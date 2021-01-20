package com.maurobanze.klean

/**
 * Allows the explicit declaration of specific mutations to the a [UiState]
 * These changes could be implicit, but it's good to have them declared because it
 * results in more readable, pure and thought-through code
 *
 * Changes allow us to explicitly represent WHY we want to change the state. The same target state can be reached by
 * different changes, each carrying different information.
 *
 * Changes are typically implemented as declarations inside a sealed class.
 * If the Change carries data, it is declared as a data class. Otherwise (e.g. Loading) are declared as objects.
 */
interface Change