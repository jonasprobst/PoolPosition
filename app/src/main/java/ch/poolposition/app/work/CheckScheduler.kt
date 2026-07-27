package ch.poolposition.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import ch.poolposition.app.core.Logger
import ch.poolposition.app.model.Watch
import java.util.concurrent.TimeUnit

/** Schedules the periodic check job and the on-demand one-shot runs. */
object CheckScheduler {

    private const val WORK_NAME = "poolposition-periodic-check"
    private const val CHECK_NOW_NAME = "poolposition-check-now"

    /**
     * Keep the 15-minute periodic job scheduled only while there is at least one
     * enabled, non-precision watch to check. When there's nothing to watch, the
     * job is cancelled so the app does no background work at all. (Precision
     * watches run on their own exact-alarm chain, so they don't need it.)
     */
    fun syncPeriodic(context: Context, watches: List<Watch>) {
        val needed = watches.any { it.enabled && !it.precision }
        val wm = WorkManager.getInstance(context)
        if (needed) {
            wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, buildRequest())
        } else {
            wm.cancelUniqueWork(WORK_NAME)
        }
    }

    /**
     * Run a check right now for every enabled watch, ignoring per-watch
     * intervals. Used by the "Check now" action to test without waiting.
     */
    fun checkNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<CheckWorker>()
            .setInputData(workDataOf(CheckWorker.KEY_FORCE_ALL to true))
            .setConstraints(networkConstraint())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(CHECK_NOW_NAME, ExistingWorkPolicy.REPLACE, request)
        Logger.log(context, "Check-now requested")
    }

    /** Fetch a single watch and record its baseline (no alert). Used on save. */
    fun baselineNow(context: Context, watchId: String) {
        val request = OneTimeWorkRequestBuilder<CheckWorker>()
            .setInputData(
                workDataOf(
                    CheckWorker.KEY_WATCH_ID to watchId,
                    CheckWorker.KEY_BASELINE_ONLY to true,
                ),
            )
            .setConstraints(networkConstraint())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("poolposition-baseline-$watchId", ExistingWorkPolicy.REPLACE, request)
        Logger.log(context, "Baseline-on-save requested")
    }

    private fun networkConstraint() =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    private fun buildRequest() =
        PeriodicWorkRequestBuilder<CheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraint())
            .build()
}
