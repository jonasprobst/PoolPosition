package ch.poolposition.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Schedules the single periodic check job. */
object CheckScheduler {

    private const val WORK_NAME = "poolposition-periodic-check"

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

    private fun buildRequest() =
        PeriodicWorkRequestBuilder<CheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
}
