package dev.beliefagent.domain.belief

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Deterministische Tests für Beobachtung/Quelle/Zeitstempel/Evidenz
 * (LH-FA-OBS-001, LH-FA-OBS-006, LH-QA-03).
 */
class BeobachtungTest {

    @Test
    fun beobachtung_traegt_quelle_und_zeitstempel() { // LH-FA-OBS-006
        val b = Beobachtung(Quelle.BUILD, Zeitstempel(42L), Evidenz("Build grün"))
        assertEquals(Quelle.BUILD, b.quelle)
        assertEquals(42L, b.zeitstempel.epochMillis)
        assertEquals("Build grün", b.evidenz.beschreibung)
    }

    @Test
    fun alle_geforderten_quellen_existieren() { // LH-FA-OBS-001
        assertEquals(
            setOf(Quelle.TEST, Quelle.BUILD, Quelle.LOG, Quelle.MENSCH, Quelle.REPO),
            Quelle.entries.toSet(),
        )
    }

    @Test
    fun zeitstempel_ist_ordnend() {
        assertTrue(Zeitstempel(1L) < Zeitstempel(2L))
    }

    @Test
    fun evidenz_darf_nicht_leer_sein() {
        assertFailsWith<IllegalArgumentException> { Evidenz(" ") }
    }
}
