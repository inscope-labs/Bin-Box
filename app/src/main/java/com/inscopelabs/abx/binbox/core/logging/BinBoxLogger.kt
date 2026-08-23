package com.inscopelabs.abx.binbox.core.logging

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger

enum class LogLevel {
    VERBOSE, DEBUG, INFO, WARN, ERROR
}

data class LogEntry(
    val id: Int,
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
}

object BinBoxLogger {
    private const val MAX_RING_BUFFER_ENTRIES = 500
    private val idCounter = AtomicInteger(1)
    private val ringBuffer = ConcurrentLinkedDeque<LogEntry>()

    var minLogLevel: LogLevel = LogLevel.DEBUG

    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        if (level.ordinal < minLogLevel.ordinal) return

        val entry = LogEntry(
            id = idCounter.getAndIncrement(),
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable
        )

        ringBuffer.addLast(entry)
        while (ringBuffer.size > MAX_RING_BUFFER_ENTRIES) {
            ringBuffer.pollFirst()
        }

        // Print to Android Logcat with JVM fallback
        try {
            when (level) {
                LogLevel.VERBOSE -> Log.v(tag, message, throwable)
                LogLevel.DEBUG -> Log.d(tag, message, throwable)
                LogLevel.INFO -> Log.i(tag, message, throwable)
                LogLevel.WARN -> Log.w(tag, message, throwable)
                LogLevel.ERROR -> Log.e(tag, message, throwable)
            }
        } catch (_: Throwable) {
            println("[$level][$tag] $message")
            throwable?.printStackTrace()
        }
    }

    fun v(tag: String, message: String) = log(LogLevel.VERBOSE, tag, message)
    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.WARN, tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, tag, message, throwable)

    fun getLogs(): List<LogEntry> = ringBuffer.toList()

    fun clear() {
        ringBuffer.clear()
    }
}
