package com.dark.darknama.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dark.darknama.MainActivity
import com.dark.darknama.R

/** Progress remains visible when navigating away from the update dialog. */
class UpdateNotifier(private val context: Context) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL_ID, context.getString(R.string.update_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ))
        }
    }

    fun progress(percent: Int?) {
        show(NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(if (percent == null) context.getString(R.string.update_downloading)
                else context.getString(R.string.update_download_progress, percent))
            .setContentIntent(openApp())
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, percent ?: 0, percent == null))
    }

    fun complete() {
        show(NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.update_notification_ready))
            .setContentIntent(openApp())
            .setAutoCancel(true))
    }

    fun failed() {
        show(NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.update_notification_failed))
            .setContentIntent(openApp())
            .setAutoCancel(true))
    }

    fun cancel() = manager.cancel(NOTIFICATION_ID)

    private fun show(builder: NotificationCompat.Builder) {
        // Android 13+ notifications are optional; the in-app dialog works even if denied.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        try {
            if (manager.areNotificationsEnabled()) manager.notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Permission may be revoked while the APK is downloading.
        }
    }

    private fun openApp(): PendingIntent = PendingIntent.getActivity(context, NOTIFICATION_ID,
        Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_UPDATE, true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    companion object {
        private const val CHANNEL_ID = "darknama_updates"
        private const val NOTIFICATION_ID = 1019
    }
}
