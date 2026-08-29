package com.inscopelabs.abx.binbox

import android.app.Application
import com.inscopelabs.abx.binbox.core.diagnostics.CrashReporterManager
import com.inscopelabs.abx.binbox.core.diagnostics.GlobalExceptionHandler
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.oci.diagnostics.OciCallTraceStore

/**
 * Custom Application class for BinBox.
 * Initializes logging, remote crash reporting managers, and global exception handlers.
 */
class BinBoxApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        BinBoxLogger.i("BinBoxApplication", "Initializing BinBox application services")
        CrashReporterManager.initialize(this)
        Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler(this))
        OciCallTraceStore.initialize(this)
        BinBoxLogger.i("BinBoxApplication", "Global exception handler, crash reporting, and OCI trace store initialized")
    }
}
