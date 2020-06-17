package io.github.reactivecircus.streamlined.home

data class HomeRendering(
    val state: HomeWorkflow.State, // TODO move State to HomeState file / class?
    val onRefresh: () -> Unit
)
