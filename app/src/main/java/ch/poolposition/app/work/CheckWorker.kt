package ch.poolposition.app.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import ch.poolposition.app.core.HtmlText
import ch.poolposition.app.core.HttpFetcher
import ch.poolposition.app.core.TriggerEngine
import ch.poolposition.app.data.WatchStore
import ch.poolposition.app.model.Watch
import ch.poolposition.app.notify.Notifier

/**
 * Periodic worker: for each enabled watch whose interval has elapsed, fetch the
 * page, strip to text, evaluate the trigger, and alert on a fire. The first
 * successful check of a watch only records a baseline. Runs on WorkManager's
 * background executor, so the blocking HTTP GET is fine here.
 */
class CheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val store = WatchStore(applicationContext)
        val watches = store.load()
        val now = System.currentTimeMillis()

        val updated = watches.map { watch ->
            if (!watch.enabled || !isDue(watch, now)) return@map watch
            checkOne(watch, now)
        }

        store.save(updated)
        return Result.success()
    }

    private fun isDue(watch: Watch, now: Long): Boolean {
        if (watch.lastCheckedAt == 0L) return true
        val elapsed = now - watch.lastCheckedAt
        return elapsed >= watch.intervalMinutes.toLong() * 60_000L
    }

    private fun checkOne(watch: Watch, now: Long): Watch {
        val fetch = HttpFetcher.get(watch.url)
        if (!fetch.ok || fetch.body == null) {
            // Keep the old baseline; advance the clock so we respect the interval.
            return watch.copy(lastCheckedAt = now)
        }

        val text = HtmlText.toVisibleText(fetch.body)
        val result = TriggerEngine.evaluate(
            mode = watch.mode,
            keyword = watch.keyword,
            visibleText = text,
            prevHash = watch.lastHash,
            prevKeywordPresent = watch.lastKeywordPresent,
        )

        if (result.triggered) {
            Notifier.notifyTriggered(applicationContext, watch)
        }

        return watch.copy(
            lastCheckedAt = now,
            lastHash = result.newHash,
            lastKeywordPresent = result.newKeywordPresent,
        )
    }
}
