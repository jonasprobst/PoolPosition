package ch.poolposition.app

import android.app.Application
import ch.poolposition.app.core.Logger
import ch.poolposition.app.data.WatchStore
import ch.poolposition.app.notify.Notifier
import ch.poolposition.app.work.CheckScheduler
import ch.poolposition.app.work.PrecisionScheduler

class PoolPositionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Logger.log(this, "App started")
        Notifier.ensureChannel(this)
        // Touch the store so the example watch is seeded on first launch.
        val watches = WatchStore(this).load()
        CheckScheduler.syncPeriodic(this, watches)
        // Re-arm any precision watches whose exact-alarm chain isn't pending
        // (e.g. after the app was force-stopped and reopened).
        watches.filter { it.enabled && it.precision }
            .forEach { PrecisionScheduler.ensureArmed(this, it) }
    }
}
