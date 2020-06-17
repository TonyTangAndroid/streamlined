package io.github.reactivecircus.streamlined.home

import com.dropbox.android.external.store4.StoreResponse
import com.squareup.workflow.RenderContext
import com.squareup.workflow.Snapshot
import com.squareup.workflow.StatefulWorkflow
import com.squareup.workflow.Worker
import com.squareup.workflow.WorkflowAction
import com.squareup.workflow.asWorker
import io.github.reactivecircus.streamlined.domain.interactor.FetchHeadlineStories
import io.github.reactivecircus.streamlined.domain.interactor.FetchPersonalizedStories
import io.github.reactivecircus.streamlined.domain.interactor.StreamHeadlineStories
import io.github.reactivecircus.streamlined.domain.interactor.StreamPersonalizedStories
import io.github.reactivecircus.streamlined.home.HomeWorkflow.State
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import reactivecircus.blueprint.interactor.EmptyParams
import javax.inject.Inject
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class HomeWorkflow @Inject constructor(
    streamHeadlineStories: StreamHeadlineStories,
    streamPersonalizedStories: StreamPersonalizedStories,
    private val fetchHeadlineStories: FetchHeadlineStories,
    private val fetchPersonalizedStories: FetchPersonalizedStories,
    private val homeUiConfigs: HomeUiConfigs
) : StatefulWorkflow<Unit, State, Nothing, HomeRendering>() {

    // TODO replace with currently applied filters / configs
    private val query = "android"

    sealed class State {
        sealed class InFlight : State() {
            object Initial : InFlight()
            data class FetchWithCache(val items: List<FeedItem>) : InFlight()
            data class Refresh(val items: List<FeedItem>?) : InFlight()
        }

        data class ShowingContent(val items: List<FeedItem>) : State()

        sealed class Error : State() {
            data class Transient(val items: List<FeedItem>) : Error()
            object Permanent : Error()
        }

        internal val itemsOrNull: List<FeedItem>?
            get() = when (this) {
                is InFlight.FetchWithCache -> items
                is InFlight.Refresh -> items
                is ShowingContent -> items
                is Error.Transient -> items
                else -> null
            }
    }

    private sealed class Action : WorkflowAction<State, Nothing> {

        class HandleStreamStoriesResponse(
            val combinedResponses: CombinedStoryResponses,
            val homeUiConfigs: HomeUiConfigs
        ) : Action()

        object RefreshStories : Action()

        class HandleRefreshStoriesResponse(val successful: Boolean) : Action()

        object DismissTransientError : Action()

        override fun WorkflowAction.Updater<State, Nothing>.apply() {
            val currentState = nextState
            nextState = when (this@Action) {
                is HandleStreamStoriesResponse -> {
                    val headlines = combinedResponses.first
                    val personalized = combinedResponses.second
                    when {
                        // When either response is loading and currently showing content,
                        // transition to InFlight.FetchWithCache state.
                        // This is needed when cached content has been displayed,
                        // but the story streams continue to fetch fresh data from network after
                        // emitting the cached content.
                        // The sequence of responses looks like this:
                        // Data response cache / disk -> Loading -> Error or Data response from network
                        headlines is StoreResponse.Loading || personalized is StoreResponse.Loading -> {
                            when (currentState) {
                                is State.ShowingContent -> {
                                    State.InFlight.FetchWithCache(currentState.items)
                                }
                                else -> currentState
                            }
                        }
                        // When either response is an error, transition to Error.Permanent state
                        // if we are either loading for the first time;
                        // or transition to Error.Transient state if we are fetching from network
                        // while displaying cached content.
                        headlines is StoreResponse.Error || personalized is StoreResponse.Error -> {
                            when (currentState) {
                                is State.InFlight.Initial -> {
                                    State.Error.Permanent
                                }
                                is State.InFlight.FetchWithCache -> {
                                    State.Error.Transient(currentState.items)
                                }
                                else -> currentState
                            }
                        }
                        // Generate feed items and transition to ShowingContent state
                        // when both responses have data available
                        headlines is StoreResponse.Data && personalized is StoreResponse.Data -> {
                            val feedItems = generateFeedItems(
                                maxNumberOfHeadlines = homeUiConfigs.numberOfHeadlinesDisplayed,
                                headlineStories = headlines.requireData(),
                                personalizedStories = personalized.requireData()
                            )
                            State.ShowingContent(feedItems)
                        }
                        else -> currentState
                    }
                }

                RefreshStories -> State.InFlight.Refresh(currentState.itemsOrNull)

                // When refresh fails, transition to Error.Permanent state if no existing data is available,
                // otherwise transition to Error.Transient state if data exists.
                is HandleRefreshStoriesResponse -> {
                    if (!successful && currentState is State.InFlight.Refresh) {
                        if (currentState.items == null) {
                            State.Error.Permanent
                        } else {
                            State.Error.Transient(currentState.items)
                        }
                    } else {
                        currentState
                    }
                }

                DismissTransientError -> {
                    when (currentState) {
                        is State.Error.Transient -> {
                            State.ShowingContent(currentState.items)
                        }
                        else -> currentState
                    }
                }
            }
        }
    }

    override fun initialState(props: Unit, snapshot: Snapshot?): State {
        return State.InFlight.Initial
    }

    override fun render(
        props: Unit,
        state: State,
        context: RenderContext<State, Nothing>
    ): HomeRendering {
        context.runningWorker(streamCombinedStoriesWorker) {
            Action.HandleStreamStoriesResponse(it, homeUiConfigs)
        }

        when (state) {
            is State.InFlight.Refresh -> {
                context.runningWorker(refreshStoriesWorker) {
                    Action.HandleRefreshStoriesResponse(it)
                }
            }
            is State.Error.Transient -> {
                context.runningWorker(
                    Worker.timer(homeUiConfigs.transientErrorDisplayDuration.inMilliseconds.toLong())
                ) {
                    Action.DismissTransientError
                }
            }
            else -> Unit
        }

        return HomeRendering(state, onRefresh = { context.actionSink.send(Action.RefreshStories) })
    }

    override fun snapshotState(state: State): Snapshot {
        return Snapshot.EMPTY
    }

    /**
     * A [Worker] that combines headline and personalized story streams.
     */
    private val streamCombinedStoriesWorker: Worker<CombinedStoryResponses> = combine(
        streamHeadlineStories.buildFlow(EmptyParams),
        streamPersonalizedStories.buildFlow(StreamPersonalizedStories.Params(query))
    ) { source1, source2 -> source1 to source2 }.asWorker()

    /**
     * A [Worker] that fetch both headlines and personalized stories from network.
     * We only care if the fetching fails and don't care about the result which will be emitted
     * through the combined stories stream.
     */
    private val refreshStoriesWorker: Worker<Boolean> = Worker.from {
        runCatching {
            coroutineScope {
                val headlineStoriesDeferred = async {
                    fetchHeadlineStories.execute(EmptyParams)
                }
                val personalizedStoriesDeferred = async {
                    fetchPersonalizedStories.execute(
                        FetchPersonalizedStories.Params(query)
                    )
                }
                awaitAll(headlineStoriesDeferred, personalizedStoriesDeferred)
            }
        }.isSuccess
    }
}
