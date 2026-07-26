package ch.poolposition.app.core

import ch.poolposition.app.model.TriggerMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerEngineTest {

    // --- CHANGED ---

    @Test
    fun changed_firstCheck_setsBaseline_noAlert() {
        val r = TriggerEngine.evaluate(
            TriggerMode.CHANGED, keyword = "", visibleText = "hello",
            prevHash = null, prevKeywordPresent = null,
        )
        assertTrue(r.isFirstBaseline)
        assertFalse(r.triggered)
        assertNotNull(r.newHash)
    }

    @Test
    fun changed_sameContent_doesNotFire() {
        val hash = TriggerEngine.sha256("same content")
        val r = TriggerEngine.evaluate(
            TriggerMode.CHANGED, keyword = "", visibleText = "same content",
            prevHash = hash, prevKeywordPresent = null,
        )
        assertFalse(r.triggered)
        assertEquals(hash, r.newHash)
    }

    @Test
    fun changed_differentContent_fires() {
        val oldHash = TriggerEngine.sha256("old")
        val r = TriggerEngine.evaluate(
            TriggerMode.CHANGED, keyword = "", visibleText = "new",
            prevHash = oldHash, prevKeywordPresent = null,
        )
        assertTrue(r.triggered)
    }

    // --- APPEARS ---

    @Test
    fun appears_firstCheck_setsBaseline_noAlert() {
        val r = TriggerEngine.evaluate(
            TriggerMode.APPEARS, keyword = "open now", visibleText = "closed",
            prevHash = null, prevKeywordPresent = null,
        )
        assertTrue(r.isFirstBaseline)
        assertFalse(r.triggered)
        assertEquals(false, r.newKeywordPresent)
    }

    @Test
    fun appears_absentThenPresent_fires() {
        val r = TriggerEngine.evaluate(
            TriggerMode.APPEARS, keyword = "open now", visibleText = "it is open now!",
            prevHash = null, prevKeywordPresent = false,
        )
        assertTrue(r.triggered)
        assertEquals(true, r.newKeywordPresent)
    }

    @Test
    fun appears_alreadyPresent_doesNotFireAgain() {
        val r = TriggerEngine.evaluate(
            TriggerMode.APPEARS, keyword = "open now", visibleText = "open now",
            prevHash = null, prevKeywordPresent = true,
        )
        assertFalse(r.triggered)
    }

    // --- DISAPPEARS (the Bern swim case) ---

    @Test
    fun disappears_firstCheck_present_setsBaseline_noAlert() {
        val r = TriggerEngine.evaluate(
            TriggerMode.DISAPPEARS,
            keyword = "Anmeldung noch nicht möglich",
            visibleText = "... Anmeldung noch nicht möglich ...",
            prevHash = null, prevKeywordPresent = null,
        )
        assertTrue(r.isFirstBaseline)
        assertFalse(r.triggered)
        assertEquals(true, r.newKeywordPresent)
    }

    @Test
    fun disappears_presentThenGone_fires() {
        val r = TriggerEngine.evaluate(
            TriggerMode.DISAPPEARS,
            keyword = "Anmeldung noch nicht möglich",
            visibleText = "Jetzt anmelden!",
            prevHash = null, prevKeywordPresent = true,
        )
        assertTrue(r.triggered)
        assertEquals(false, r.newKeywordPresent)
    }

    @Test
    fun disappears_stillPresent_doesNotFire() {
        val r = TriggerEngine.evaluate(
            TriggerMode.DISAPPEARS,
            keyword = "Anmeldung noch nicht möglich",
            visibleText = "Anmeldung noch nicht möglich",
            prevHash = null, prevKeywordPresent = true,
        )
        assertFalse(r.triggered)
    }

    @Test
    fun keyword_matchIsCaseInsensitive() {
        val r = TriggerEngine.evaluate(
            TriggerMode.APPEARS, keyword = "OPEN", visibleText = "the doors are open",
            prevHash = null, prevKeywordPresent = false,
        )
        assertTrue(r.triggered)
    }
}
