package com.maurobanze.klean

import com.maurobanze.klean.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Represents a ViewModel, a component that serves as the glue between the View layer and
 * the operations layer (Cloud apis, local storage, etc.)
 *
 * It holds the current state of the UI, and manages it's mutations.
 * It receives [UiAction]s from the user, executes work (e.g. through [UseCase]s), and dispatch a [Change]
 * as a result of task completion.
 * The [Change] and the [currentState] are used to derive a new [UiState], by calling [onReduce] on the [UiViewModel].
 * The new [UiState] is communicated to the attached [Ui], by calling [Ui.renderState]
 *
 * [UiViewModel] can trigger state updates in its initialization process. For that reason, [UiViewModel]
 * asks that a [Ui] is registered right in it's constructor, so the [Ui] never misses renderState calls.
 * For the same reason, [Ui]s should create a [UiViewModel] AFTER initializing it's
 * rendering mechanisms (it's Views and other related components). Otherwise, [Ui.renderState] can be
 * called immediately, and will try to use non initialized views, which will result in a crash.
 *
 * TODO: Consider having UiViewModel work exclusively with specific [UiAction] and [Change] subtypes,
 * similarly to how it only works with a specific [UiState]. Type safety.
 */
abstract class UiViewModel<State : UiState>(initialState: State, ui: Ui<State>? = null) {

    public var currentState: State = initialState
        private set

    private var ui: Ui<State>? = ui

    private val vmScope = CoroutineScope(Dispatchers.Default)
    private val mutex = Mutex()

    private var logger = Logger()

    /**
     * This is useful in situations in which the ViewModel does not actually emmit a state
     * update to the attached Ui, because the action received didn't actually cause a change.
     * In such cases, while testing, it's still useful to assert that the state didn't change.
     */
    public var testModeActive = false
        set(value) {
            logger = Logger(value)
            field = value
        }

    /**
     * Called when the UiViewModel receives an action from the Ui.
     * Within this method, you can identify the action, and execute work.
     *
     * Once the work is complete, you should use [dispatchChange] to trigger a state update.
     */
    protected abstract fun onActionReceived(action: UiAction)

    /**
     * Dispatches an action to this [UiViewModel]. It triggers [onActionReceived].
     */
    public fun dispatchAction(uiAction: UiAction) {
        logger.logVerbose("Ui ACTION: ${uiAction.javaClass.simpleName}")
        onActionReceived(uiAction)
    }

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
     * Allows setting [ui] as the [Ui] instance for this ViewModel, such that it can then
     * receive state updates.
     */
    public fun registerUi(ui: Ui<State>) {
        require(this.ui == null) { "A Ui is already registered. UiViewModel currently only supports a single Ui" }
        this.ui = ui
    }

    public fun unregisterUi() {
        if (ui != null) {
            ui = null
            onDetachUi()
        }
    }

    /**
     * Called when the attached [Ui] is destroyed.
     * Use this callback to cancel ongoing work (e.g. cancel coroutine scope, unregister listeners, etc.)
     */
    protected abstract fun onDetachUi()


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
    public fun dispatchChange(change: Change) {
        logger.logVerbose("CHANGE: ${change.javaClass.simpleName}")

        // executes state updates on a worker thread
        vmScope.launch {

            //synchronized block, eliminating state update race conditions
            mutex.withLock {
                val newState = onReduce(currentState, change)
                if (newState === currentState && !testModeActive) {

                    //No state transition happened, so does not dispatch state update in production mode
                    return@withLock
                }

                logger.logVerbose(
                    "State Update: \nOLD STATE: $currentState\n" +
                            "NEW STATE: $newState"
                )

                val oldState = currentState
                currentState =
                    newState //state transition. Only place in which currentState is modified

                dispatchStateUpdateToUi(currentState)
                dispatchStateTransitionToViewModel(oldState, currentState)
            }
        }
    }


    /**
     * Used on [onReduce] to indicate that it's not possible to transform the current state using the
     * supplied change. Might indicate that the client's programmers forgot to account for valid state transitions.
     * Might also arise due to unforeseen race conditions.
     * [onReduce] serves as the single source of truth, and should throw an exception right away.
     * In addition, the exception should contain the state transition log. TODO: Do this. Maybe create our own exception
     */
    protected fun throwUnsupportedStateTransition(currentState: UiState?, change: Change): State {
        throw IllegalArgumentException(
            "Unsupported state transition: $currentState, $change. " +
                    "a. Add transition to onReduce if it should be valid\n" +
                    "b. Check for race conditions if event history looks weird"
        )
    }

    protected fun throwUnknownChange(change: Change): State {
        throw IllegalArgumentException(
            "This ViewModel does not support that change:$change. Forgot to add new change to onReduce()?"
        )
    }

    /**
     * Sends a state update to the associated [Ui] (if present), on the UI thread
     */
    private suspend fun dispatchStateUpdateToUi(uiState: State) {
        withContext(Dispatchers.Main) {
            ui?.dispatchStateUpdate(uiState)
        }
    }

    /**
     * Triggers the [UiViewModel]'s [onStateTransition] callback, to notify that a state transition
     * has taken place.
     */
    private fun dispatchStateTransitionToViewModel(oldState: State, currentState: State) {
        onStateTransition(oldState, currentState)
    }
}