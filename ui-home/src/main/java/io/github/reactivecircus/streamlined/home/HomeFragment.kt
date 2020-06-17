package io.github.reactivecircus.streamlined.home

import com.squareup.workflow.ui.ViewEnvironment
import com.squareup.workflow.ui.ViewRegistry
import com.squareup.workflow.ui.WorkflowFragment
import com.squareup.workflow.ui.WorkflowRunner
import io.github.reactivecircus.streamlined.navigator.NavigatorProvider
import io.github.reactivecircus.streamlined.ui.ScreenForAnalytics
import javax.inject.Inject

class HomeFragment @Inject constructor(
    navigatorProvider: NavigatorProvider,
    private val homeWorkFlow: HomeWorkflow
) : WorkflowFragment<Unit, Nothing>(), ScreenForAnalytics {

    override val viewEnvironment: ViewEnvironment =
        ViewEnvironment(
            ViewRegistry(
                HomeLayoutRunner.create(navigatorProvider)
            )
        )

    override fun onCreateWorkflow(): WorkflowRunner.Config<Unit, Nothing> {
        return WorkflowRunner.Config(homeWorkFlow)
    }
}
