package ch.poolposition.app.data

import android.content.Context
import ch.poolposition.app.model.TriggerMode
import ch.poolposition.app.model.Watch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Persists the watch list as a single JSON file in the app's private storage.
 * Uses the platform's built-in org.json, so it adds no dependencies and no
 * database. Not thread-safe; callers serialize access (WorkManager runs one
 * worker at a time, the UI touches it on the main thread).
 */
class WatchStore(context: Context) {

    private val file = File(context.filesDir, FILE_NAME)

    /** Load all watches, seeding the example watch on first run. */
    fun load(): List<Watch> {
        if (!file.exists()) {
            val seeded = listOf(seedWatch())
            save(seeded)
            return seeded
        }
        return runCatching {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    fun save(watches: List<Watch>) {
        val arr = JSONArray()
        watches.forEach { arr.put(toJson(it)) }
        file.writeText(arr.toString())
    }

    private fun toJson(w: Watch): JSONObject = JSONObject().apply {
        put("id", w.id)
        put("label", w.label)
        put("url", w.url)
        put("intervalMinutes", w.intervalMinutes)
        put("mode", w.mode.name)
        put("keyword", w.keyword)
        put("enabled", w.enabled)
        put("lastCheckedAt", w.lastCheckedAt)
        put("lastHash", w.lastHash ?: JSONObject.NULL)
        when (val p = w.lastKeywordPresent) {
            null -> put("lastKeywordPresent", JSONObject.NULL)
            else -> put("lastKeywordPresent", p)
        }
    }

    private fun fromJson(o: JSONObject): Watch = Watch(
        id = o.optString("id", UUID.randomUUID().toString()),
        label = o.optString("label", ""),
        url = o.optString("url", ""),
        intervalMinutes = o.optInt("intervalMinutes", Watch.MIN_INTERVAL_MINUTES),
        mode = runCatching { TriggerMode.valueOf(o.optString("mode", "CHANGED")) }
            .getOrDefault(TriggerMode.CHANGED),
        keyword = o.optString("keyword", ""),
        enabled = o.optBoolean("enabled", true),
        lastCheckedAt = o.optLong("lastCheckedAt", 0L),
        lastHash = if (o.isNull("lastHash")) null else o.optString("lastHash"),
        lastKeywordPresent = if (o.isNull("lastKeywordPresent")) null
        else o.optBoolean("lastKeywordPresent"),
    )

    companion object {
        private const val FILE_NAME = "watches.json"

        fun newId(): String = UUID.randomUUID().toString()

        /** The pre-seeded Bern swim registration watch. */
        fun seedWatch(): Watch = Watch(
            id = UUID.randomUUID().toString(),
            label = "Bern swim",
            url = "https://anmeldung.bernschwimmt.ch/",
            intervalMinutes = 15,
            mode = TriggerMode.DISAPPEARS,
            keyword = "Anmeldung noch nicht möglich",
            enabled = true,
        )
    }
}
