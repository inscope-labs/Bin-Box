package com.inscopelabs.abx.binbox.core.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Abstraction layer for Coroutine dispatchers to allow clean dependency injection
 * and deterministic testing with TestDispatchers.
 */
interface CoroutineDispatchersProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}

/**
 * Default production implementation backed by standard Kotlin Coroutine dispatchers.
 */
open class DefaultCoroutineDispatchers(
    override val main: CoroutineDispatcher = Dispatchers.Main,
    override val io: CoroutineDispatcher = Dispatchers.IO,
    override val default: CoroutineDispatcher = Dispatchers.Default,
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
) : CoroutineDispatchersProvider {
    companion object Instance : DefaultCoroutineDispatchers()
}
