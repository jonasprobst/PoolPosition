package ch.poolposition.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ch.poolposition.app.R
import ch.poolposition.app.model.Watch

/** Builds and posts the high-priority "watch triggered" notification. */
object Notifier {

    private const val CHANNEL_ID = "watch_alerts"

    /** Create the alert channel. Safe to call repeatedly; a no-op below API 26. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Watch alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Fires when a watched page triggers"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    /**
     * Post an alert for [watch]. Tapping opens the watched URL in the browser.
     * Caller is responsible for holding POST_NOTIFICATIONS on Android 13+.
     */
    fun notifyTriggered(context: Context, watch: Watch) {
        val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(watch.url))
        val pending = PendingIntent.getActivity(
            context,
            watch.id.hashCode(),
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(watch.label)
            .setContentText(triggerText(watch))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(watch.id.hashCode(), notification)
        }
    }

    private fun triggerText(watch: Watch): String = when (watch.mode) {
        ch.poolposition.app.model.TriggerMode.CHANGED -> "The page changed — tap to open"
        ch.poolposition.app.model.TriggerMode.APPEARS ->
            "“${watch.keyword}” appeared — tap to open"
        ch.poolposition.app.model.TriggerMode.DISAPPEARS ->
            "“${watch.keyword}” is gone — tap to open"
    }
}
