package com.maurobanze.klean

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Usually, we write statements like scope.launch(Dispatchers.IO) to launch coroutines in our thread pool of choice.
 * However, this is problematic during unit testing. Unit tests should use TestCoroutineDispatcher
 * provided by the coroutines-test library (otherwise tests break). Therefore, a better pattern is to inject
 * dispatchers into our classes (e.g. ViewModels), and replace hardcoded Dispatcher.YXZ calls with
 * DispatcherProvider.xyz() calls.
 *
 * This interface helps with the above goal. [DefaultDispatcherProvider] should be used in production code,
 * while the instance returned by [createTestDispatcherProvider] should be used in unit tests.
 */
interface DispatcherProvider {
    fun default(): CoroutineDispatcher
    fun io(): CoroutineDispatcher
    fun main(): CoroutineDispatcher
    fun unconfined(): CoroutineDispatcher
}

object DefaultDispatcherProvider : DispatcherProvider {
    override fun default() = Dispatchers.Default
    override fun io() = Dispatchers.IO
    override fun main() = Dispatchers.Main
    override fun unconfined() = Dispatchers.Unconfined
}

fun createTestDispatcherProvider(testDispatcher: CoroutineDispatcher): DispatcherProvider {
    return object : DispatcherProvider {
        override fun default() = testDispatcher
        override fun io() = testDispatcher
        override fun main() = testDispatcher
        override fun unconfined() = testDispatcher
    }
}