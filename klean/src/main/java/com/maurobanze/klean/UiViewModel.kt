package com.maurobanze.klean

import com.maurobanze.klean.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Represents a ViewModel, a component that serves as the glue between the View layer and
 * the operations layer (Cloud apis, local storage, etc.)
 *
 * It holds the current state of the UI, and manages it's mutations.
 * It receives [UiAction]s from the user, executes work (e.g. through [UseCase]s), and dispatch a [Change]
 * as a result of task completion.
 * The [Change] and the [currentState] are used to derive a new [UiState], by calling [onReduce] on the [UiViewModel].
 * The new [UiState] is emitted to the UI via [uiStateFlow]
 *
 */
abstract class UiViewModel<State : UiState, Action : UiAction>(
    initialState: State,
    private val testModeActive: Boolean = false,
    protected val dispatcherProvider: DefaultDispatcherProvider = DefaultDispatcherProvider
) {

    public var currentState: State = initialState
        private set

    private val uiStateFlow = MutableSharedFlow<State>(replay = 1)

    private val vmScope = CoroutineScope(dispatcherProvider.default())

    private val mutex = Mutex()

    init {
        uiStateFlow.tryEmit(initialState)
    }

    /**
     *
     */
    public fun uiStateAsFlow(): SharedFlow<State> = uiStateFlow

    /**
     * Dispatches an action to this [UiViewModel]. It triggers [onActionReceived].
     */
    public fun dispatchAction(uiAction: Action) {
        onActionReceived(uiAction)
    }

    /**
     * Called when the UiViewModel receives an action from the Ui.
     * Within this method, you can identify the action, and execute work.
     *
     * Once the work is complete, you should use [dispatchChange] to trigger a state update.
     */
    protected abstract fun onActionReceived(action: Action)


    /**
     * Method in which this [UiViewModel] makes a state transition.
     * The purpose of this method is to use the inputted [change] and [currentState] to arrive at another state,
     * which is returned by this function.
     *
     * This function should be implemented as a pure function. In order to achieve that,[UiViewModel]
     * implementations are recommended to:
     *  - Implement [UiState] as a data class
     *  - Use immutable variables within the [UiState], for instance by declaring them val or [List] instead
     *   of var or [MutableList]/[ArrayList].
     *  - Use dataclass.copy() or copy the List within [onReduce], and return the copy.
     *  - Not perform actions with side effects within this method. E.g. network call.
     *  - Use only the function parameters as input, and function return as output.
     *
     * The above recommendations help eliminate edge cases in which objects are
     * unknowingly modified elsewhere in concurrent code. It also guarantees that users of the
     * values (i.e. [Ui]) contained in the state can be sure that it won't be suddenly modified while
     * they are still using them, introducing subtle, hard-to-identify bugs.
     *
     * Note: return [currentState] if you want to signal that no transition happened. That way,
     * the ViewModel won't emmit a state update event to the [Ui], unless test mode is active.
     *
     * Some more notes:
     *
     * - This method always runs on a worker thread.
     * - This method is synchronized, meaning that it can not execute in parallel. This design eliminates
     * race conditions.
     *
     * @return the new state, or [currentState] if no state transition should be performed
     */
    protected abstract fun onReduce(currentState: State, change: Change): State

    /**
     * Callback triggered every time a state transition takes place.
     *
     * The [UiViewModel] may use this callback as an opportunity to detect events of interest, for instance
     * by comparing proprieties of the old and new states.
     * This can be useful, for instance, for logging Analytics events (e.g. selected
     * list item went from item A to item B). The existence of this method helps keep [onReduce] free
     * from side-effect operations.
     *
     * PLEASE NOTE:
     *
     * - This callback is triggered AFTER [Ui.renderState].
     * - This callback is triggered AFTER the transition takes place. That is, after [UiViewModel.currentState] assumes
     * the new value.
     * - This callback is triggered ONLY when there is an actual state transition. Therefore, [oldState] and [currentState] are
     * guaranteed to be different objects.
     * - New state transitions can only take place after this method returns. For this reason, this callback's
     * implementations should be lightweight (i.e. avoid long-running operations on the same thread).
     *
     * Implementing this callback is optional.
     */
    protected open fun onStateTransition(oldState: State, currentState: State) {}

    /**
     * TODO
     */
    protected open fun onClear() {}

    /**
     * Use this method to request a change to the current state based on the outcome of a task.
     * It updates the current state, and dispatches:
     *  - a state update event to the attached [Ui].
     *  - a state transition event to this [UiViewModel], through [onStateTransition]
     *
     * This is the ONLY way to trigger a [currentState] transition.
     * Does not emmit a state update event if the state doesn't actually change, except if test mode
     * is active.
     */
    protected fun dispatchChange(change: Change) {

        // executes state updates on a worker thread
        vmScope.launch {

            //synchronized block, eliminating state update race conditions
            mutex.withLock {
                val newState = onReduce(currentState, change)
                if (newState === currentState && !testModeActive) {

                    //No state transition happened, so does not dispatch state update in production mode
                    return@withLock
                }

                val oldState = currentState
                currentState =
                    newState //state transition. Only place in which currentState is modified

                uiStateFlow.emit(currentState)
                dispatchStateTransitionToViewModel(oldState, currentState)
            }
        }
    }

    protected fun throwUnknownChange(change: Change): State {
        throw IllegalArgumentException(
            "This ViewModel does not support that change:$change. Forgot to add new change to onReduce()?"
        )
    }

    /**
     * Triggers the [UiViewModel]'s [onStateTransition] callback, to notify that a state transition
     * has taken place.
     */
    private fun dispatchStateTransitionToViewModel(oldState: State, currentState: State) {
        onStateTransition(oldState, currentState)
    }
}