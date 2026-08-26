package com.inscopelabs.abx.binbox.core.diagnostics

interface CrashReporter {
    fun initialize()
    fun reportCrash(thread: Thread, throwable: Throwable)
    fun setEnabled(enabled: Boolean)
}
