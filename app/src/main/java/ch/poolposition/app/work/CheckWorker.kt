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
 * The check job. Run modes (via input data):
 * - periodic (default): each enabled, non-precision watch whose interval elapsed.
 * - force all ([KEY_FORCE_ALL]): every enabled watch now ("Check now").
 * - single baseline ([KEY_WATCH_ID] + [KEY_BASELINE_ONLY]): fetch one watch and
 *   record its baseline without alerting (used on save).
 * - precision ([KEY_WATCH_ID] + [KEY_PRECISION]): fetch one watch, alert on a
 *   fire, then either re-arm the next exact alarm or auto-stop precision if it
 *   fired.
 *
 * Runs on WorkManager's background executor, so the blocking GET is fine.
 */
class CheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val force = inputData.getBoolean(KEY_FORCE_ALL, false)
        val onlyId = inputData.getString(KEY_WATCH_ID)
        val baselineOnly = inputData.getBoolean(KEY_BASELINE_ONLY, false)
        val precision = inputData.getBoolean(KEY_PRECISION, false)

        val store = WatchStore(applicationContext)
        val watches = store.load().toMutableList()
        val now = System.currentTimeMillis()

        val reason = when {
            precision -> "precision"
            onlyId != null -> "baseline-on-save"
            force -> "check-now"
            else -> "scheduled/background"
        }
        Logger.log(applicationContext, "Worker running ($reason) over ${watches.size} watch(es)")

        if (onlyId != null) {
            checkSingle(watches, onlyId, now, baselineOnly, precision)
        } else {
            for (i in watches.indices) {
                val w = watches[i]
                if (!w.enabled) continue
                // Precision watches run on their own exact-alarm chain, not the
                // shared periodic job (but a manual "Check now" still hits them).
                if (!force && w.precision) continue
                if (force || isDue(w, now)) watches[i] = checkOne(w, now, baselineOnly = false).first
            }
        }

        store.save(watches)
        // Reconcile the periodic job with the current watch set (e.g. a precision
        // watch that just auto-stopped now needs the normal 15-min job again).
        CheckScheduler.syncPeriodic(applicationContext, watches)
        Logger.log(applicationContext, "Worker finished ($reason)")
        return Result.success()
    }

    private fun checkSingle(
        watches: MutableList<Watch>,
        id: String,
        now: Long,
        baselineOnly: Boolean,
        precision: Boolean,
    ) {
        val idx = watches.indexOfFirst { it.id == id }
        if (idx < 0) return
        val target = watches[idx]
        if (!target.enabled && !baselineOnly) return

        val (checked, alerted) = checkOne(target, now, baselineOnly)
        var result = checked

        if (precision) {
            if (alerted) {
                result = result.copy(precision = false)
                Logger.log(applicationContext, "  ${result.label}: precision fired — auto-stopping")
                PrecisionScheduler.cancel(applicationContext, id)
            } else if (result.precision && result.enabled) {
                PrecisionScheduler.schedule(applicationContext, result)
            }
        }
        watches[idx] = result
    }

    private fun isDue(watch: Watch, now: Long): Boolean {
        if (watch.lastCheckedAt == 0L) return true
        val elapsed = now - watch.lastCheckedAt
        return elapsed >= watch.intervalMinutes.toLong() * 60_000L
    }

    /** @return the updated watch and whether it posted an alert. */
    private fun checkOne(watch: Watch, now: Long, baselineOnly: Boolean): Pair<Watch, Boolean> {
        val fetch = HttpFetcher.get(watch.url)
        if (!fetch.ok || fetch.body == null) {
            val msg = fetch.error ?: "unknown error"
            Logger.log(applicationContext, "  ${watch.label}: fetch FAILED ($msg)")
            // Keep the old baseline; advance the clock so we respect the interval.
            return watch.copy(lastCheckedAt = now, lastResult = "Fetch failed: $msg") to false
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

        val updated = watch.copy(
            lastCheckedAt = now,
            lastHash = result.newHash,
            lastKeywordPresent = result.newKeywordPresent,
            lastResult = outcome,
        )
        return updated to notify
    }

    companion object {
        /** When true, check every enabled watch immediately, ignoring its interval. */
        const val KEY_FORCE_ALL = "force_all"

        /** Check only the watch with this id. */
        const val KEY_WATCH_ID = "watch_id"

        /** When true, record the baseline but never post a notification. */
        const val KEY_BASELINE_ONLY = "baseline_only"

        /** When true, this is a precision (exact-alarm) run: re-arm or auto-stop after. */
        const val KEY_PRECISION = "precision"
    }
}
