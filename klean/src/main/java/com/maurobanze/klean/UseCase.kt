package com.maurobanze.klean

/**
 * [UseCase]s represent operations such as fetching a piece of data from the network.
 */
interface UseCase

/**
 * Allows Usecases to report errors to their callers. [code] can be used to encode the type of error
 */
class UseCaseError(val code: String?) : Exception()

