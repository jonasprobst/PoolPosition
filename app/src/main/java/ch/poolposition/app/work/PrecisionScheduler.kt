package ch.poolposition.app.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import ch.poolposition.app.MainActivity
import ch.poolposition.app.core.Logger
import ch.poolposition.app.model.Watch

/**
 * Schedules exact, Doze-proof checks for a precision watch using
 * [AlarmManager.setAlarmClock] — which fires on time even in Doze and, unlike
 * setExact*, needs no exact-alarm permission. Each fired check schedules the
 * next one (see CheckWorker), forming a chain that stops when the watch fires.
 * The only visible cost is an alarm-clock icon in the status bar while armed.
 */
object PrecisionScheduler {

    const val ACTION = "ch.poolposition.app.PRECISION_CHECK"
    const val EXTRA_WATCH_ID = "watch_id"

    /** Schedule the next check for [watch], its interval from now. */
    fun schedule(context: Context, watch: Watch) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = System.currentTimeMillis() + watch.intervalMinutes.toLong() * 60_000L
        val show = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, show), operation(context, watch.id))
        Logger.log(context, "Precision alarm set: ${watch.label} in ${watch.intervalMinutes} min")
    }

    /** Schedule only if no alarm is already pending (used on app start / boot). */
    fun ensureArmed(context: Context, watch: Watch) {
        val existing = PendingIntent.getBroadcast(
            context,
            0,
            intentFor(context, watch.id),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (existing == null) schedule(context, watch)
    }

    fun cancel(context: Context, watchId: String) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(operation(context, watchId))
    }

    private fun intentFor(context: Context, watchId: String): Intent =
        // A distinct data URI per watch keeps each watch's alarm independent.
        Intent(context, PrecisionAlarmReceiver::class.java).apply {
            action = ACTION
            data = Uri.parse("poolposition://precision/$watchId")
            putExtra(EXTRA_WATCH_ID, watchId)
        }

    private fun operation(context: Context, watchId: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            intentFor(context, watchId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
