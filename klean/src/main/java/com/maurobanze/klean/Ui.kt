package com.maurobanze.klean

/**
 * Describes a UI component that wants to be plugged into our logic system.
 * These would typically be Fragments/Activities in Android, UIController (I think?) in iOS,
 * but could literally be anything such as a command line interface.
 * A Ui should do only rendering states, and dispatching newUser and system events. Ui's should
 * not contain business logic.
 */
interface Ui<State : UiState> {

    var viewModel: UiViewModel<State>?

    fun renderState(uiState: State)

    fun dispatchStateUpdate(uiState: State) {
        renderState(uiState)
    }

    fun dispatchAction(uiAction: UiAction) {
        viewModel?.dispatchAction(uiAction)
    }
}