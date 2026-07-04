package dev.beliefagent.domain.belief

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Deterministische Tests für Normierung + Validierung (LH-FA-BEL-002,
 * LH-FA-BEL-004, LH-OP-05, LH-QA-03).
 */
class NormierungTest {

    @Test
    fun normierter_belief_state_wird_akzeptiert() { // LH-FA-BEL-002
        val bs = BeliefState.of(
            hypothesen = listOf(
                Hypothese(HypotheseId("auth"), 0.5),
                Hypothese(HypotheseId("frontend"), 0.3),
            ),
            resthypothese = Resthypothese(0.2),
        )
        assertEquals(2, bs.hypothesen.size)
    }

    @Test
    fun summe_ungleich_eins_wird_zurueckgewiesen() { // LH-FA-BEL-004
        // 0.5 + 0.3 + 0.1 = 0.9 -> nicht normiert
        assertFailsWith<IllegalArgumentException> {
            BeliefState.of(
                hypothesen = listOf(
                    Hypothese(HypotheseId("auth"), 0.5),
                    Hypothese(HypotheseId("frontend"), 0.3),
                ),
                resthypothese = Resthypothese(0.1),
            )
        }
    }

    @Test
    fun summe_ueber_eins_wird_zurueckgewiesen() { // LH-FA-BEL-004
        assertFailsWith<IllegalArgumentException> {
            BeliefState.of(
                hypothesen = listOf(Hypothese(HypotheseId("auth"), 0.9)),
                resthypothese = Resthypothese(0.9),
            )
        }
    }

    @Test
    fun negative_wahrscheinlichkeit_wird_zurueckgewiesen() { // LH-FA-BEL-004
        assertFailsWith<IllegalArgumentException> {
            BeliefState.of(
                hypothesen = listOf(Hypothese(HypotheseId("auth"), 1.2)),
                resthypothese = Resthypothese(-0.2),
            )
        }
    }

    @Test
    fun abweichung_innerhalb_der_toleranz_wird_akzeptiert() { // LH-FA-BEL-002, LH-OP-05
        // Σ = 1 + 1e-10 < Toleranz 1e-9 -> gültig
        val bs = BeliefState.of(
            hypothesen = listOf(Hypothese(HypotheseId("auth"), 0.5 + 1e-10)),
            resthypothese = Resthypothese(0.5),
        )
        assertEquals(1, bs.hypothesen.size)
    }

    @Test
    fun abweichung_ausserhalb_der_toleranz_wird_zurueckgewiesen() { // LH-FA-BEL-002, LH-OP-05
        // Σ = 1 + 1e-6 > Toleranz 1e-9 -> ungültig
        assertFailsWith<IllegalArgumentException> {
            BeliefState.of(
                hypothesen = listOf(Hypothese(HypotheseId("auth"), 0.5 + 1e-6)),
                resthypothese = Resthypothese(0.5),
            )
        }
    }
}
