package ch.poolposition.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlTextTest {

    @Test
    fun stripsTags_keepsVisibleText() {
        val html = "<html><body><h1>Hello</h1><p>World</p></body></html>"
        assertEquals("Hello World", HtmlText.toVisibleText(html))
    }

    @Test
    fun dropsScriptAndStyleContent() {
        val html = """
            <html><head><style>.x{color:red}</style></head>
            <body>Visible<script>var noise = 1;</script></body></html>
        """.trimIndent()
        val text = HtmlText.toVisibleText(html)
        assertEquals("Visible", text)
        assertFalse(text.contains("noise"))
        assertFalse(text.contains("color"))
    }

    @Test
    fun decodesCommonEntities() {
        val html = "<p>Tom &amp; Jerry&nbsp;&#39;s caf&eacute;</p>"
        assertEquals("Tom & Jerry 's café", HtmlText.toVisibleText(html))
    }

    @Test
    fun collapsesWhitespace() {
        val html = "<p>a</p>\n\n   <p>b</p>\t<p>c</p>"
        assertEquals("a b c", HtmlText.toVisibleText(html))
    }

    @Test
    fun preservesKeywordAcrossTags() {
        val html = "<div>Anmeldung noch <b>nicht</b> m&ouml;glich</div>"
        val text = HtmlText.toVisibleText(html)
        assertTrue(text.contains("Anmeldung noch nicht möglich"))
    }
}
