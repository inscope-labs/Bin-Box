package com.inscopelabs.abx.binbox.core

import com.inscopelabs.abx.binbox.core.dispatcher.DefaultCoroutineDispatchers
import com.inscopelabs.abx.binbox.core.error.AppError
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.logging.LogLevel
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.core.result.getOrDefault
import com.inscopelabs.abx.binbox.core.result.getOrNull
import com.inscopelabs.abx.binbox.core.result.map
import com.inscopelabs.abx.binbox.core.result.onError
import com.inscopelabs.abx.binbox.core.result.onSuccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CoreArchitectureTest {

    @Test
    fun testAppResultSuccessAndTransformations() {
        val success: AppResult<String> = AppResult.Success("connected")

        assertTrue(success.isSuccess)
        assertFalse(success.isError)
        assertFalse(success.isLoading)
        assertEquals("connected", success.getOrNull())
        assertEquals("connected", success.getOrDefault("fallback"))

        val mapped = success.map { it.length }
        assertTrue(mapped is AppResult.Success)
        assertEquals(9, (mapped as AppResult.Success<Int>).data)

        var captured = ""
        success.onSuccess { captured = it }
        assertEquals("connected", captured)
    }

    @Test
    fun testAppResultErrorHandling() {
        val errorObj = AppError.NetworkError.HostUnreachable("10.0.0.1")
        val errorResult: AppResult<String> = AppResult.Error(errorObj)

        assertFalse(errorResult.isSuccess)
        assertTrue(errorResult.isError)
        assertNull(errorResult.getOrNull())
        assertEquals("default_val", errorResult.getOrDefault("default_val"))

        var receivedError: AppError? = null
        errorResult.onError { receivedError = it }
        assertNotNull(receivedError)
        assertEquals("Host 10.0.0.1 is unreachable.", receivedError?.userMessage)
    }

    @Test
    fun testCoroutineDispatchersInstantiation() {
        val dispatchers = DefaultCoroutineDispatchers()
        assertNotNull(dispatchers.main)
        assertNotNull(dispatchers.io)
        assertNotNull(dispatchers.default)
        assertNotNull(dispatchers.unconfined)
    }

    @Test
    fun testBinBoxLoggerRingBuffer() {
        BinBoxLogger.clear()
        BinBoxLogger.minLogLevel = LogLevel.DEBUG

        BinBoxLogger.d("CoreTest", "Debug message")
        BinBoxLogger.i("CoreTest", "Info message")
        BinBoxLogger.w("CoreTest", "Warning message")
        BinBoxLogger.e("CoreTest", "Error message")

        val logs = BinBoxLogger.getLogs()
        assertEquals(4, logs.size)
        assertEquals(LogLevel.DEBUG, logs[0].level)
        assertEquals("Debug message", logs[0].message)
        assertEquals(LogLevel.ERROR, logs[3].level)
    }
}
