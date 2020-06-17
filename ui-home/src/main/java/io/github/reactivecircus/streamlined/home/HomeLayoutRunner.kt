package io.github.reactivecircus.streamlined.home

import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.squareup.workflow.ui.LayoutRunner
import com.squareup.workflow.ui.ViewEnvironment
import com.squareup.workflow.ui.ViewFactory
import io.github.reactivecircus.streamlined.design.setDefaultBackgroundColor
import io.github.reactivecircus.streamlined.home.databinding.FragmentHomeBinding
import io.github.reactivecircus.streamlined.navigator.NavigatorProvider
import io.github.reactivecircus.streamlined.ui.context
import io.github.reactivecircus.streamlined.ui.lifecycleScope
import io.github.reactivecircus.streamlined.ui.util.ItemActionListener
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.view.clicks
import reactivecircus.flowbinding.swiperefreshlayout.refreshes
import io.github.reactivecircus.streamlined.ui.R as CommonUiResource

internal class HomeLayoutRunner(
    private val binding: FragmentHomeBinding,
    private val navigatorProvider: NavigatorProvider
) : LayoutRunner<HomeRendering> {

    private val itemActionListener: ItemActionListener<FeedsListAdapter.ItemAction> = { action ->
        when (action) {
            is FeedsListAdapter.ItemAction.StoryClicked -> {
                navigatorProvider.get()?.navigateToStoryDetailsScreen(action.story.id)
            }
            is FeedsListAdapter.ItemAction.BookmarkToggled -> Unit
            is FeedsListAdapter.ItemAction.MoreButtonClicked -> Unit
            FeedsListAdapter.ItemAction.ReadMoreHeadlinesButtonClicked -> {
                navigatorProvider.get()?.navigateToHeadlinesScreen()
            }
        }
    }

    private val feedsListAdapter = FeedsListAdapter(itemActionListener).apply {
        stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    private var errorSnackbar: Snackbar? = null

    init {
        binding.recyclerView.layoutManager = LinearLayoutManager(binding.root.context)
    }

    override fun showRendering(
        rendering: HomeRendering,
        viewEnvironment: ViewEnvironment
    ) {
        binding.toolbar.title = binding.context.getString(R.string.title_home)

        binding.swipeRefreshLayout.refreshes()
            .onEach { rendering.onRefresh() }
            .launchIn(binding.lifecycleScope)

        binding.retryButton.clicks()
            .onEach { rendering.onRefresh() }
            .launchIn(binding.lifecycleScope)

        when (rendering.state) {
            is HomeWorkflow.State.InFlight -> binding.showInFlightState(
                hasContent = rendering.state.itemsOrNull != null
            )
            is HomeWorkflow.State.ShowingContent -> binding.showContentState()
            is HomeWorkflow.State.Error.Transient -> binding.showTransientErrorState()
            HomeWorkflow.State.Error.Permanent -> binding.showPermanentErrorState()
        }

        feedsListAdapter.submitList(rendering.state.itemsOrNull)

        if (binding.recyclerView.adapter == null) binding.recyclerView.adapter = feedsListAdapter
    }

    private fun FragmentHomeBinding.showContentState() {
        errorStateView.isVisible = false
        progressBar.isVisible = false
        swipeRefreshLayout.isRefreshing = false
        swipeRefreshLayout.isEnabled = true
        recyclerView.isVisible = true
        errorSnackbar?.dismiss()
    }

    private fun FragmentHomeBinding.showInFlightState(hasContent: Boolean) {
        errorStateView.isVisible = false
        progressBar.isVisible = !hasContent
        swipeRefreshLayout.isRefreshing = hasContent
        swipeRefreshLayout.isEnabled = hasContent
        recyclerView.isVisible = hasContent
        errorSnackbar?.dismiss()
    }

    private fun FragmentHomeBinding.showPermanentErrorState() {
        errorStateView.isVisible = true
        progressBar.isVisible = false
        swipeRefreshLayout.isRefreshing = false
        swipeRefreshLayout.isEnabled = false
        recyclerView.isVisible = false
        errorSnackbar?.dismiss()
    }

    private fun FragmentHomeBinding.showTransientErrorState() {
        errorStateView.isVisible = false
        progressBar.isVisible = false
        swipeRefreshLayout.isRefreshing = false
        swipeRefreshLayout.isEnabled = true
        recyclerView.isVisible = true
        if (errorSnackbar?.isShownOrQueued != true) {
            val errorMessage = context.getString(
                CommonUiResource.string.error_message_could_not_refresh_content
            )
            errorSnackbar = Snackbar
                .make(root, errorMessage, Snackbar.LENGTH_INDEFINITE)
                .setDefaultBackgroundColor()
                .apply { show() }
        }
    }

    companion object {
        fun create(navigatorProvider: NavigatorProvider): ViewFactory<HomeRendering> =
            LayoutRunner.bind(FragmentHomeBinding::inflate) { binding ->
                HomeLayoutRunner(
                    binding = binding,
                    navigatorProvider = navigatorProvider
                )
            }
    }
}
