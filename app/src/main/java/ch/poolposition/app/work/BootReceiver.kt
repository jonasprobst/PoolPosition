package ch.poolposition.app.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ch.poolposition.app.core.Logger
import ch.poolposition.app.data.WatchStore

/**
 * AlarmManager alarms are cleared on reboot, so re-arm every enabled precision
 * watch after the device restarts. Normal watches are handled by WorkManager,
 * which persists across reboots on its own.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val precisionWatches = WatchStore(context).load().filter { it.enabled && it.precision }
        precisionWatches.forEach { PrecisionScheduler.ensureArmed(context, it) }
        if (precisionWatches.isNotEmpty()) {
            Logger.log(context, "Boot: re-armed ${precisionWatches.size} precision watch(es)")
        }
    }
}
