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
import java.util.concurrent.TimeUnit

/** Schedules the single periodic check job. */
object CheckScheduler {

    private const val WORK_NAME = "poolposition-periodic-check"
    private const val CHECK_NOW_NAME = "poolposition-check-now"

    /**
     * Ensure the 15-minute periodic check is scheduled. Uses KEEP so an existing
     * schedule survives app restarts; call [reschedule] to force a refresh.
     */
    fun ensureScheduled(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            buildRequest(),
        )
    }

    /** Kick off a check now-ish and reset the schedule (used after edits). */
    fun reschedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            buildRequest(),
        )
    }

    /**
     * Run a check right now for every enabled watch, ignoring per-watch
     * intervals. Used by the "Check now" action to test without waiting.
     */
    fun checkNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<CheckWorker>()
            .setInputData(workDataOf(CheckWorker.KEY_FORCE_ALL to true))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(CHECK_NOW_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    private fun buildRequest() =
        PeriodicWorkRequestBuilder<CheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
}
