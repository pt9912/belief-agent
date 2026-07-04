package dev.beliefagent.domain.belief

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Deterministische Tests für die Unsicherheitsmaße (LH-FA-BEL-008, LH-QA-03).
 */
class UnsicherheitTest {

    private val a = HypotheseId("A")
    private val b = HypotheseId("B")
    private val c = HypotheseId("C")

    @Test
    fun entropie_ist_null_bei_voller_konzentration() { // LH-FA-BEL-008
        val konzentriert = BeliefState.of(
            hypothesen = listOf(Hypothese(a, 1.0)),
            resthypothese = Resthypothese(0.0),
        )
        assertEquals(0.0, konzentriert.entropie(), 1e-12)
    }

    @Test
    fun entropie_ist_hoeher_bei_gleichverteilung_als_bei_konzentration() { // LH-FA-BEL-008
        val gleich = BeliefState.of(
            hypothesen = listOf(Hypothese(a, 0.25), Hypothese(b, 0.25), Hypothese(c, 0.25)),
            resthypothese = Resthypothese(0.25),
        )
        val konzentriert = BeliefState.of(
            hypothesen = listOf(Hypothese(a, 0.9), Hypothese(b, 0.05)),
            resthypothese = Resthypothese(0.05),
        )
        assertTrue(
            gleich.entropie() > konzentriert.entropie(),
            "Gleichverteilung sollte unsicherer sein: gleich=${gleich.entropie()} konz=${konzentriert.entropie()}",
        )
    }

    @Test
    fun top2_abstand_ist_differenz_der_zwei_groessten_massen() { // LH-FA-BEL-008
        val bs = BeliefState.of(
            hypothesen = listOf(Hypothese(a, 0.5), Hypothese(b, 0.3)),
            resthypothese = Resthypothese(0.2),
        )
        assertEquals(0.2, bs.top2Abstand(), 1e-12) // 0.5 - 0.3
    }

    @Test
    fun top2_abstand_ist_null_bei_zwei_gleich_starken_massen() { // LH-FA-BEL-008
        val bs = BeliefState.of(
            hypothesen = listOf(Hypothese(a, 0.5)),
            resthypothese = Resthypothese(0.5),
        )
        assertEquals(0.0, bs.top2Abstand(), 1e-12)
    }

    @Test
    fun top2_abstand_ist_maximal_bei_voller_konzentration() { // LH-FA-BEL-008
        val bs = BeliefState.of(
            hypothesen = listOf(Hypothese(a, 1.0)),
            resthypothese = Resthypothese(0.0),
        )
        assertEquals(1.0, bs.top2Abstand(), 1e-12)
    }
}
