package ch.poolposition.app.core

import ch.poolposition.app.model.TriggerMode
import java.security.MessageDigest

/**
 * Pure, side-effect-free evaluation of a watch's trigger against freshly
 * fetched page text. No Android or network dependencies, so it is fully
 * unit-testable on the JVM.
 */
object TriggerEngine {

    /**
     * Result of evaluating one check.
     *
     * @param triggered whether an alert should fire.
     * @param newHash the content hash to persist as the new baseline (CHANGED).
     * @param newKeywordPresent the keyword presence to persist (keyword modes).
     * @param isFirstBaseline true when this check only established a baseline.
     */
    data class Result(
        val triggered: Boolean,
        val newHash: String?,
        val newKeywordPresent: Boolean?,
        val isFirstBaseline: Boolean,
    )

    /**
     * Evaluate a check.
     *
     * @param visibleText already stripped-to-text page content.
     * @param prevHash previously stored hash baseline (null if never checked).
     * @param prevKeywordPresent previously stored keyword presence (null if never checked).
     */
    fun evaluate(
        mode: TriggerMode,
        keyword: String,
        visibleText: String,
        prevHash: String?,
        prevKeywordPresent: Boolean?,
    ): Result = when (mode) {
        TriggerMode.CHANGED -> {
            val hash = sha256(visibleText)
            val first = prevHash == null
            Result(
                triggered = !first && hash != prevHash,
                newHash = hash,
                newKeywordPresent = null,
                isFirstBaseline = first,
            )
        }

        TriggerMode.APPEARS -> {
            val present = containsKeyword(visibleText, keyword)
            val first = prevKeywordPresent == null
            // Fire on the absent -> present transition.
            Result(
                triggered = !first && prevKeywordPresent == false && present,
                newHash = null,
                newKeywordPresent = present,
                isFirstBaseline = first,
            )
        }

        TriggerMode.DISAPPEARS -> {
            val present = containsKeyword(visibleText, keyword)
            val first = prevKeywordPresent == null
            // Fire on the present -> absent transition.
            Result(
                triggered = !first && prevKeywordPresent == true && !present,
                newHash = null,
                newKeywordPresent = present,
                isFirstBaseline = first,
            )
        }
    }

    private fun containsKeyword(text: String, keyword: String): Boolean {
        if (keyword.isBlank()) return false
        return text.contains(keyword, ignoreCase = true)
    }

    fun sha256(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }
}
