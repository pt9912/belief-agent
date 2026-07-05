package dev.beliefagent.domain.belief

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Deterministische Tests für die Belief-Rekonstruktion aus dem Protokoll
 * (LH-FA-AUD-002, LH-QA-03).
 */
class RekonstruktionTest {

    private fun belief(pA: Double): BeliefState =
        BeliefState.of(listOf(Hypothese(HypotheseId("A"), pA)), Resthypothese(1.0 - pA))

    private fun aktualisiert(t: Long, pA: Double): Ereignis =
        BeliefAktualisiert(Zeitstempel(t), belief(pA))

    @Test
    fun leeres_protokoll_hat_keinen_belief() {
        assertNull(Rekonstruktion.endBelief(EreignisProtokoll.LEER))
    }

    @Test
    fun ohne_belief_ereignis_bleibt_null() {
        val p = EreignisProtokoll.von(
            listOf(
                HypotheseHinzugefuegt(Zeitstempel(1), HypotheseId("A")),
                AktionVorgeschlagen(Zeitstempel(2), "x"),
            ),
        )
        assertNull(Rekonstruktion.endBelief(p))
    }

    @Test
    fun end_belief_ist_der_letzte_snapshot() { // LH-FA-AUD-002
        val p = EreignisProtokoll.von(listOf(aktualisiert(1, 0.6), aktualisiert(2, 0.9)))
        val end = Rekonstruktion.endBelief(p)!!
        assertEquals(0.9, end.hypothesen.single().wahrscheinlichkeit)
    }

    @Test
    fun vergangener_zustand_ist_rekonstruierbar() { // LH-FA-AUD-002 — Replay bis Zeitpunkt
        val p = EreignisProtokoll.von(listOf(aktualisiert(10, 0.6), aktualisiert(20, 0.9)))
        val beiT15 = Rekonstruktion.rekonstruiereBis(p, Zeitstempel(15))!!
        assertEquals(0.6, beiT15.hypothesen.single().wahrscheinlichkeit) // nur das erste Update zählt
    }

    @Test
    fun rekonstruktion_vor_erstem_belief_ist_null() {
        val p = EreignisProtokoll.von(listOf(aktualisiert(10, 0.6)))
        assertNull(Rekonstruktion.rekonstruiereBis(p, Zeitstempel(5)))
    }

    @Test
    fun deterministisch() { // LH-QA-03
        val eingabe = listOf(aktualisiert(1, 0.7), aktualisiert(2, 0.8))
        val a = Rekonstruktion.endBelief(EreignisProtokoll.von(eingabe))!!
        val b = Rekonstruktion.endBelief(EreignisProtokoll.von(eingabe))!!
        assertEquals(a.hypothesen.single().wahrscheinlichkeit, b.hypothesen.single().wahrscheinlichkeit)
    }
}
