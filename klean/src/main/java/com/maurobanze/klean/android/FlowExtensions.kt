package com.maurobanze.klean.android

import androidx.lifecycle.Lifecycle
import com.maurobanze.klean.Ui
import com.maurobanze.klean.UiState
import com.maurobanze.klean.UiViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow



@OptIn(ExperimentalCoroutinesApi::class)
public fun <State : UiState> UiViewModel<State>.uiStateAsFlow(): Flow<State> {

    return callbackFlow {

        val ui = object : Ui<State> {
            override var viewModel: UiViewModel<State>? = null
            override fun renderState(uiState: State) {
                sendBlocking(uiState)
            }
        }

        registerUi(ui)

        awaitClose {
            unregisterUi()
        }
    }
}


@OptIn(ExperimentalCoroutinesApi::class)
public fun <State : UiState> UiViewModel<State>.uiStateAsFlow(lifecycle: Lifecycle): Flow<State> {

    return callbackFlow {

        val ui = object : AndroidUi<State> {
            override var viewModel: UiViewModel<State>? = null
            override fun renderState(uiState: State) {
                sendBlocking(uiState)
            }
        }

        ui.attachLifecycleToUi(lifecycle)
        registerUi(ui)

        awaitClose {
            unregisterUi()
            ui.detachLifecycleFromUi(lifecycle)
        }
    }
}