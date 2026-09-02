package com.inscopelabs.abx.binbox.terminal.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.inscopelabs.abx.binbox.MainActivity
import com.inscopelabs.abx.binbox.R
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger

/**
 * Foreground service providing a persistent notification for terminal sessions,
 * mirroring the battle-tested background session lifecycle pattern from Termux.
 * This guarantees terminal sessions and background execution threads are not killed by
 * Android's low-memory killer when the UI is minimized or backgrounded.
 */
class TerminalForegroundService : Service() {

    companion object {
        private const val TAG = "TerminalForegroundService"
        const val CHANNEL_ID = "binbox_terminal_service_channel"
        const val NOTIFICATION_ID = 1337

        const val ACTION_START_OR_UPDATE = "com.inscopelabs.abx.binbox.action.START_OR_UPDATE"
        const val ACTION_STOP = "com.inscopelabs.abx.binbox.action.STOP"

        const val EXTRA_SHELL_TITLE = "extra_shell_title"
        const val EXTRA_SESSION_COUNT = "extra_session_count"

        fun startService(context: Context, activeShellTitle: String? = null, sessionCount: Int = 1) {
            BinBoxLogger.i(TAG, "Requesting TerminalForegroundService start (shell=$activeShellTitle, count=$sessionCount)")
            val intent = Intent(context, TerminalForegroundService::class.java).apply {
                action = ACTION_START_OR_UPDATE
                putExtra(EXTRA_SHELL_TITLE, activeShellTitle)
                putExtra(EXTRA_SESSION_COUNT, sessionCount)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (t: Throwable) {
                BinBoxLogger.e(TAG, "Failed to start TerminalForegroundService", t)
            }
        }

        fun stopService(context: Context) {
            BinBoxLogger.i(TAG, "Requesting TerminalForegroundService stop")
            val intent = Intent(context, TerminalForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (t: Throwable) {
                BinBoxLogger.e(TAG, "Failed to stop TerminalForegroundService", t)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        BinBoxLogger.i(TAG, "TerminalForegroundService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_OR_UPDATE
        BinBoxLogger.d(TAG, "onStartCommand received action: $action")

        if (action == ACTION_STOP) {
            BinBoxLogger.i(TAG, "Stopping foreground service and removing persistent notification")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
            return START_NOT_STICKY
        }

        val shellTitle = intent?.getStringExtra(EXTRA_SHELL_TITLE)
        val sessionCount = intent?.getIntExtra(EXTRA_SESSION_COUNT, 1) ?: 1

        val notification = buildPersistentNotification(shellTitle, sessionCount)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            BinBoxLogger.i(TAG, "Persistent notification posted successfully (foreground active)")
        } catch (t: Throwable) {
            BinBoxLogger.e(TAG, "Error in startForeground", t)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        BinBoxLogger.i(TAG, "TerminalForegroundService destroyed")
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager?.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "BinBox Terminal Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Keeps active BinBox terminal sessions alive in background"
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                notificationManager?.createNotificationChannel(channel)
                BinBoxLogger.d(TAG, "Created notification channel: $CHANNEL_ID")
            }
        }
    }

    private fun buildPersistentNotification(shellTitle: String?, sessionCount: Int): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TerminalForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = when {
            !shellTitle.isNullOrBlank() -> "Connected: ${shellTitle.uppercase()}"
            sessionCount > 1 -> "$sessionCount active sessions running"
            else -> "Terminal session running in background"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BinBox")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Exit",
                stopPendingIntent
            )
            .build()
    }
}
