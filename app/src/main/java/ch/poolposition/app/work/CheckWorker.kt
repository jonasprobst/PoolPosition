package ch.poolposition.app.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import ch.poolposition.app.core.HtmlText
import ch.poolposition.app.core.HttpFetcher
import ch.poolposition.app.core.Logger
import ch.poolposition.app.core.TriggerEngine
import ch.poolposition.app.data.WatchStore
import ch.poolposition.app.model.Watch
import ch.poolposition.app.notify.Notifier

/**
 * The check job. Three ways to run, chosen via input data:
 * - periodic (default): check each enabled watch whose interval elapsed.
 * - force all ([KEY_FORCE_ALL]): check every enabled watch now ("Check now").
 * - single baseline ([KEY_WATCH_ID] + [KEY_BASELINE_ONLY]): fetch one watch and
 *   record its baseline without ever alerting (used on save).
 *
 * Runs on WorkManager's background executor, so the blocking GET is fine.
 */
class CheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val force = inputData.getBoolean(KEY_FORCE_ALL, false)
        val onlyId = inputData.getString(KEY_WATCH_ID)
        val baselineOnly = inputData.getBoolean(KEY_BASELINE_ONLY, false)

        val store = WatchStore(applicationContext)
        val watches = store.load()
        val now = System.currentTimeMillis()

        val reason = when {
            onlyId != null -> "baseline-on-save"
            force -> "check-now"
            else -> "periodic"
        }
        Logger.log(applicationContext, "Worker running ($reason) over ${watches.size} watch(es)")

        val updated = watches.map { watch ->
            when {
                onlyId != null -> if (watch.id == onlyId) checkOne(watch, now, baselineOnly) else watch
                !watch.enabled -> watch
                force || isDue(watch, now) -> checkOne(watch, now, baselineOnly = false)
                else -> watch
            }
        }

        store.save(updated)
        Logger.log(applicationContext, "Worker finished ($reason)")
        return Result.success()
    }

    private fun isDue(watch: Watch, now: Long): Boolean {
        if (watch.lastCheckedAt == 0L) return true
        val elapsed = now - watch.lastCheckedAt
        return elapsed >= watch.intervalMinutes.toLong() * 60_000L
    }

    private fun checkOne(watch: Watch, now: Long, baselineOnly: Boolean): Watch {
        val fetch = HttpFetcher.get(watch.url)
        if (!fetch.ok || fetch.body == null) {
            val msg = fetch.error ?: "unknown error"
            Logger.log(applicationContext, "  ${watch.label}: fetch FAILED ($msg)")
            // Keep the old baseline; advance the clock so we respect the interval.
            return watch.copy(lastCheckedAt = now, lastResult = "Fetch failed: $msg")
        }

        val text = HtmlText.toVisibleText(fetch.body)
        Logger.log(applicationContext, "  ${watch.label}: fetched OK, ${text.length} chars of text")

        val result = TriggerEngine.evaluate(
            mode = watch.mode,
            keyword = watch.keyword,
            visibleText = text,
            prevHash = watch.lastHash,
            prevKeywordPresent = watch.lastKeywordPresent,
        )

        val outcome: String
        val notify: Boolean
        when {
            result.isFirstBaseline -> { outcome = "Baseline set"; notify = false }
            result.triggered && baselineOnly -> { outcome = "Changed (baseline reset)"; notify = false }
            result.triggered -> { outcome = "TRIGGERED"; notify = true }
            else -> { outcome = "No change"; notify = false }
        }
        Logger.log(applicationContext, "  ${watch.label}: $outcome")

        if (notify) {
            Notifier.notifyTriggered(applicationContext, watch)
            Logger.log(applicationContext, "  ${watch.label}: notification posted")
        }

        return watch.copy(
            lastCheckedAt = now,
            lastHash = result.newHash,
            lastKeywordPresent = result.newKeywordPresent,
            lastResult = outcome,
        )
    }

    companion object {
        /** When true, check every enabled watch immediately, ignoring its interval. */
        const val KEY_FORCE_ALL = "force_all"

        /** Check only the watch with this id (used to baseline one watch on save). */
        const val KEY_WATCH_ID = "watch_id"

        /** When true, record the baseline but never post a notification. */
        const val KEY_BASELINE_ONLY = "baseline_only"
    }
}
