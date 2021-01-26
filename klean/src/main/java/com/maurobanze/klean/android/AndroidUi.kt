package com.maurobanze.klean.android

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.maurobanze.klean.Ui
import com.maurobanze.klean.UiState
import com.maurobanze.klean.UiViewModel

/**
 * [Ui] implementation for use with Android.
 * It uses Android-specific tools such as [Lifecycle] to make it easier to integrate
 * with Activities and Fragments, etc.
 */
interface AndroidUi<State : UiState> : Ui<State>, LifecycleObserver {

    /**
     * Attaches [lifecycle] to this [Ui] instance.
     * This allows the [Ui] to receive lifecycle events such ON_DESTROY.
     * The [Ui] then can detach itself from the [UiViewModel], to prevent issues such as
     * memory leaks.
     */
    fun attachLifecycleToUi(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
    }

    fun detachLifecycleFromUi(lifecycle: Lifecycle) {
        lifecycle.removeObserver(this)
    }

    /**
     * INTERNAL API
     * Receives the "ON_DESTROY" callback from the attached lifecycle, and detaches this Ui from
     * the ViewModel
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroyUi() {
        viewModel?.unregisterUi()
    }
}