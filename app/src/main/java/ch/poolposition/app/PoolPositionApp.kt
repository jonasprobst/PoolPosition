package ch.poolposition.app

import android.app.Application
import ch.poolposition.app.data.WatchStore
import ch.poolposition.app.notify.Notifier
import ch.poolposition.app.work.CheckScheduler

class PoolPositionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannel(this)
        // Touch the store so the example watch is seeded on first launch.
        WatchStore(this).load()
        CheckScheduler.ensureScheduled(this)
    }
}
