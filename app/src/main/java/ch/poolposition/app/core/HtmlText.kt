package ch.poolposition.app.core

/**
 * Minimal HTML-to-visible-text extraction using only the standard library
 * (no Jsoup). Good enough for server-rendered pages: drops script/style/head
 * noise, strips tags, decodes the common entities, and collapses whitespace.
 */
object HtmlText {

    private val REMOVE_BLOCKS = Regex(
        "<(script|style|head|noscript)\\b[^>]*>.*?</\\1>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    // Tags that imply a line/word break so text doesn't run together.
    private val BLOCK_BREAKS = Regex(
        "<(br|/p|/div|/li|/tr|/h[1-6]|/section|/article|/header|/footer)\\b[^>]*>",
        RegexOption.IGNORE_CASE,
    )

    private val ANY_TAG = Regex("<[^>]+>")
    private val COMMENTS = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    private val WHITESPACE = Regex("\\s+")

    /** Extract normalized visible text from an HTML document. */
    fun toVisibleText(html: String): String {
        var s = html
        s = COMMENTS.replace(s, " ")
        s = REMOVE_BLOCKS.replace(s, " ")
        s = BLOCK_BREAKS.replace(s, " ")
        s = ANY_TAG.replace(s, " ")
        s = decodeEntities(s)
        s = WHITESPACE.replace(s, " ").trim()
        return s
    }

    private fun decodeEntities(input: String): String {
        var s = input
        // Named entities we actually expect to see in visible text.
        val named = mapOf(
            "&nbsp;" to " ",
            "&amp;" to "&",
            "&lt;" to "<",
            "&gt;" to ">",
            "&quot;" to "\"",
            "&#39;" to "'",
            "&apos;" to "'",
            "&auml;" to "ä", "&ouml;" to "ö", "&uuml;" to "ü",
            "&Auml;" to "Ä", "&Ouml;" to "Ö", "&Uuml;" to "Ü",
            "&szlig;" to "ß", "&eacute;" to "é", "&egrave;" to "è",
        )
        for ((entity, replacement) in named) {
            s = s.replace(entity, replacement)
        }
        // Numeric entities: &#123; and &#x1F;
        s = Regex("&#x([0-9a-fA-F]+);").replace(s) { m ->
            m.groupValues[1].toIntOrNull(16)?.let { cp ->
                runCatching { String(Character.toChars(cp)) }.getOrDefault(m.value)
            } ?: m.value
        }
        s = Regex("&#([0-9]+);").replace(s) { m ->
            m.groupValues[1].toIntOrNull()?.let { cp ->
                runCatching { String(Character.toChars(cp)) }.getOrDefault(m.value)
            } ?: m.value
        }
        return s
    }
}
