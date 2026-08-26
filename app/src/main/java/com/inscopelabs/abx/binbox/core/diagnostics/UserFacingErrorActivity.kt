package com.inscopelabs.abx.binbox.core.diagnostics

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.inscopelabs.abx.binbox.R
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger

/**
 * Release-build counterpart to CrashActivity. Shown to end users after an
 * uncaught exception instead of the bare OS "App has stopped" dialog, or
 * CrashActivity's developer-facing stack trace view.
 *
 * Deliberately does NOT render exception type, message, or stack trace —
 * only a short opaque reference code the user can quote to support, which
 * correlates with the matching entry already written to crash_logs.txt by
 * GlobalExceptionHandler.
 */
class UserFacingErrorActivity : ComponentActivity() {
    private companion object {
        const val TAG = "BINBOX_ERROR_UI"

        const val EXTRA_REFERENCE_CODE = "extra_reference_code"
        const val EXTRA_FULL_REPORT = "extra_full_report"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_user_facing_error)
            setupUI()
        } catch (t: Throwable) {
            BinBoxLogger.e(TAG, "Failed to initialize UserFacingErrorActivity UI safely", t)
            try {
                Toast.makeText(this, getString(R.string.error_fallback_toast), Toast.LENGTH_LONG).show()
            } catch (_: Throwable) {}
            finish()
        }
    }

    private fun setupUI() {
        val tvReferenceCode: TextView = findViewById(R.id.tvReferenceCode)
        val btnRestart: Button = findViewById(R.id.btnErrorRestart)
        val btnShare: Button = findViewById(R.id.btnErrorShare)

        val referenceCode = intent.getStringExtra(EXTRA_REFERENCE_CODE)
            ?: getString(R.string.error_unknown_reference)
        val fullReport = intent.getStringExtra(EXTRA_FULL_REPORT) ?: ""

        tvReferenceCode.text = getString(R.string.error_reference_format, referenceCode)

        btnRestart.setOnClickListener {
            try {
                val relaunch = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (relaunch != null) {
                    startActivity(relaunch)
                }
                finishAffinity()
            } catch (t: Throwable) {
                BinBoxLogger.e(TAG, "Failed to restart application", t)
                Toast.makeText(this, R.string.error_restart_failed, Toast.LENGTH_SHORT).show()
            }
        }

        btnShare.setOnClickListener {
            try {
                val shareText = getString(R.string.error_share_body, referenceCode, fullReport)
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.error_share_subject, referenceCode))
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                startActivity(Intent.createChooser(sendIntent, getString(R.string.error_share_chooser_title)))
            } catch (t: Throwable) {
                BinBoxLogger.e(TAG, "Failed to share diagnostic report", t)
                Toast.makeText(this, R.string.error_share_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
