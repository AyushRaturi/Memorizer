package com.example.recall

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val CHANNEL_UNLOCK = "recall_unlock"
private const val CHANNEL_SERVICE = "recall_service"
private const val NOTIF_UNLOCK = 1001
private const val NOTIF_SERVICE = 1002

object RecallActions {
    /** Called on every unlock: rotate the note, refresh widget, optionally pop a notification. */
    fun onUnlock(context: Context, pending: BroadcastReceiver.PendingResult? = null) {
        val app = context.applicationContext
        val note = NotesStore.pickNext(app)
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (note.isNotBlank()) {
                    refreshRecallWidgets(app)
                    if (NotesStore.headsUp(app)) showNotification(app, note)
                }
            } finally {
                pending?.finish()
            }
        }
    }

    fun startListener(context: Context) {
        try {
            ContextCompat.startForegroundService(context, Intent(context, UnlockService::class.java))
        } catch (_: Exception) {
            // Background-start restrictions; the listener will start next time the app is opened.
        }
    }

    private fun showNotification(context: Context, note: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_UNLOCK, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName("Unlock reminders")
                .setVibrationEnabled(false)
                .build()
        )
        val open = android.app.PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_UNLOCK)
            .setSmallIcon(R.drawable.ic_stat_recall)
            .setContentTitle("Recall")
            .setContentText(note)
            .setStyle(NotificationCompat.BigTextStyle().bigText(note))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setTimeoutAfter(15_000)
            .build()
        try {
            manager.notify(NOTIF_UNLOCK, notification)
        } catch (_: SecurityException) {
        }
    }

    fun buildServiceNotification(context: Context): Notification {
        NotificationManagerCompat.from(context).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_SERVICE, NotificationManagerCompat.IMPORTANCE_MIN)
                .setName("Recall background listener")
                .build()
        )
        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_recall)
            .setContentTitle("Recall is active")
            .setContentText("Rotating your reminder on unlock")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    const val SERVICE_NOTIFICATION_ID = NOTIF_SERVICE
}

/**
 * Foreground service whose only job is to hold a runtime-registered USER_PRESENT receiver,
 * because that broadcast is not delivered to manifest-declared receivers on Android 8+.
 */
class UnlockService : Service() {
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_USER_PRESENT) {
                RecallActions.onUnlock(context, goAsync())
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(
            this, RecallActions.SERVICE_NOTIFICATION_ID,
            RecallActions.buildServiceNotification(this), type
        )
        ContextCompat.registerReceiver(
            this, receiver, IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        runCatching { unregisterReceiver(receiver) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            RecallActions.startListener(context)
        }
    }
}
