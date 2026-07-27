package ch.poolposition.app.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import ch.poolposition.app.core.Logger

/** Fired by an exact alarm: run a precision check for one watch off the main thread. */
class PrecisionAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val watchId = intent.getStringExtra(PrecisionScheduler.EXTRA_WATCH_ID) ?: return
        Logger.log(context, "Precision alarm fired ($watchId)")
        val request = OneTimeWorkRequestBuilder<CheckWorker>()
            .setInputData(
                workDataOf(
                    CheckWorker.KEY_WATCH_ID to watchId,
                    CheckWorker.KEY_PRECISION to true,
                ),
            )
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("poolposition-precision-$watchId", ExistingWorkPolicy.REPLACE, request)
    }
}
