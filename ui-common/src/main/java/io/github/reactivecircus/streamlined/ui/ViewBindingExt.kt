package io.github.reactivecircus.streamlined.ui

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.viewbinding.ViewBinding
import com.squareup.workflow.ui.lifecycleOrNull
import kotlinx.coroutines.CoroutineScope

/**
 * Returns the lifecycle [CoroutineScope] associated with the
 * [Lifecycle] of the [ViewBinding]'s context.
 */
val ViewBinding.lifecycleScope: CoroutineScope
    get() = context.lifecycleOrNull()!!.coroutineScope

/**
 * Returns the [Context] in which the root view of the [ViewBinding] is running in.
 */
val ViewBinding.context: Context
    get() = root.context
