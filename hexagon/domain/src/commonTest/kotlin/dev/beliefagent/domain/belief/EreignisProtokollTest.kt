package dev.beliefagent.domain.belief

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Deterministische Tests für das append-only Ereignisprotokoll
 * (LH-FA-AUD-001, LH-QA-03).
 */
class EreignisProtokollTest {

    private fun ev(t: Long): Ereignis = AktionVorgeschlagen(Zeitstempel(t), "aktion@$t")

    @Test
    fun leeres_protokoll_ist_leer() {
        assertTrue(EreignisProtokoll.LEER.istLeer())
        assertEquals(0, EreignisProtokoll.LEER.groesse)
    }

    @Test
    fun append_haengt_an_und_zaehlt() { // LH-FA-AUD-001
        val p = EreignisProtokoll.LEER.append(ev(1)).append(ev(2))
        assertEquals(2, p.groesse)
        assertEquals(listOf(ev(1), ev(2)), p.ereignisse)
    }

    @Test
    fun append_ist_nicht_ueberschreibend() { // Vergangenheit unveränderlich
        val basis = EreignisProtokoll.LEER.append(ev(1))
        val erweitert = basis.append(ev(2))
        assertEquals(1, basis.groesse) // Original unverändert
        assertEquals(2, erweitert.groesse)
    }

    @Test
    fun gleicher_zeitstempel_ist_erlaubt() {
        val p = EreignisProtokoll.LEER.append(ev(5)).append(ev(5))
        assertEquals(2, p.groesse)
    }

    @Test
    fun rueck_datieren_wird_abgewiesen() { // LH-FA-AUD-001 — kein Mutieren der Vergangenheit
        val p = EreignisProtokoll.LEER.append(ev(10))
        assertFailsWith<IllegalArgumentException> { p.append(ev(9)) }
    }

    @Test
    fun von_baut_geordnet() {
        val p = EreignisProtokoll.von(listOf(ev(1), ev(2), ev(3)))
        assertEquals(3, p.groesse)
        assertFalse(p.istLeer())
    }

    @Test
    fun von_weist_ungeordnete_sequenz_ab() {
        assertFailsWith<IllegalArgumentException> {
            EreignisProtokoll.von(listOf(ev(2), ev(1)))
        }
    }
}
