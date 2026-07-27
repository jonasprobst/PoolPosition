package ch.poolposition.app.model

/**
 * How a watch decides whether to fire an alert.
 *
 * All modes are transition-based: the first check only records a baseline and
 * never alerts. Subsequent checks compare against the stored baseline.
 */
enum class TriggerMode {
    /** Fire when the page's visible text changes from the baseline. */
    CHANGED,

    /** Fire when [Watch.keyword] goes from absent to present. */
    APPEARS,

    /** Fire when [Watch.keyword] goes from present to absent. */
    DISAPPEARS,
}

/**
 * A single watched page.
 *
 * The `last*` fields hold the baseline captured on the most recent successful
 * check. They are null/0 until the first check runs.
 */
data class Watch(
    val id: String,
    val label: String,
    val url: String,
    val intervalMinutes: Int,
    val mode: TriggerMode = TriggerMode.CHANGED,
    /** Keyword for [TriggerMode.APPEARS] / [TriggerMode.DISAPPEARS]; ignored for CHANGED. */
    val keyword: String = "",
    val enabled: Boolean = true,
    /**
     * When true, this watch is driven by exact alarms (Doze-proof) instead of
     * the shared 15-minute job, allowing sub-15-minute intervals. Higher battery
     * use; auto-disables once the watch fires.
     */
    val precision: Boolean = false,
    /** Epoch millis of the last successful check; 0 means never checked. */
    val lastCheckedAt: Long = 0L,
    /** Baseline content hash for [TriggerMode.CHANGED]; null until first check. */
    val lastHash: String? = null,
    /** Baseline keyword presence for keyword modes; null until first check. */
    val lastKeywordPresent: Boolean? = null,
    /** Human-readable outcome of the most recent check; null until first check. */
    val lastResult: String? = null,
) {
    companion object {
        const val MIN_INTERVAL_MINUTES = 15
        const val PRECISION_MIN_INTERVAL_MINUTES = 1
    }
}
