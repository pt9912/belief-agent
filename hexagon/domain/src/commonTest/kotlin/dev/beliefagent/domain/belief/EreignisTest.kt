package dev.beliefagent.domain.belief

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Deterministische Tests für die Ereignis-Typen (LH-FA-AUD-001, LH-FA-AUD-004,
 * LH-QA-03).
 */
class EreignisTest {

    private val t = Zeitstempel(1000L)

    @Test
    fun protokoll_deckt_die_geforderten_ereignisarten_ab() { // LH-FA-AUD-001
        val belief = BeliefState.of(
            hypothesen = listOf(Hypothese(HypotheseId("A"), 0.7)),
            resthypothese = Resthypothese(0.3),
        )
        val beobachtung = Beobachtung(Quelle.TEST, t, Evidenz("Test rot"))

        val ereignisse: List<Ereignis> = listOf(
            HypotheseHinzugefuegt(t, HypotheseId("A")),
            BeobachtungErfasst(t, beobachtung),
            BeliefAktualisiert(t, belief),
            AktionVorgeschlagen(t, "Deploy"),
            GateAbgelehnt(t, "Konfidenz unter Schwelle"),
            EskalationAngefordert(t, "Resthypothese hoch"),
        )

        assertEquals(6, ereignisse.size)
    }

    @Test
    fun jedes_ereignis_traegt_einen_zeitstempel() { // LH-FA-AUD-004
        val ereignisse: List<Ereignis> = listOf(
            HypotheseHinzugefuegt(t, HypotheseId("A")),
            EskalationAngefordert(Zeitstempel(2000L), "Grund"),
        )
        assertTrue(ereignisse.all { it.zeitstempel.epochMillis > 0L })
    }
}
