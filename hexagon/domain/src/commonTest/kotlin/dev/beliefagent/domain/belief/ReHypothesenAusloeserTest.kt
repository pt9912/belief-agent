package dev.beliefagent.domain.belief

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Deterministische Tests für den Re-Hypothesen-Auslöser (LH-FA-BEL-005,
 * LH-QA-03).
 */
class ReHypothesenAusloeserTest {

    private val a = HypotheseId("A")

    private fun mitRest(rest: Double): BeliefState = BeliefState.of(
        hypothesen = listOf(Hypothese(a, 1.0 - rest)),
        resthypothese = Resthypothese(rest),
    )

    @Test
    fun ausgeloest_wenn_resthypothese_ueber_schwellwert() { // LH-FA-BEL-005
        assertTrue(ReHypothesenAusloeser().ausgeloest(mitRest(0.6)))
    }

    @Test
    fun nicht_ausgeloest_wenn_resthypothese_unter_schwellwert() { // LH-FA-BEL-005 (θ_rehyp=0,30, ADR-0008)
        assertFalse(ReHypothesenAusloeser().ausgeloest(mitRest(0.2)))
    }

    @Test
    fun nicht_ausgeloest_genau_am_schwellwert() { // LH-FA-BEL-005 (echt >)
        assertFalse(ReHypothesenAusloeser(schwellwert = 0.5).ausgeloest(mitRest(0.5)))
    }

    @Test
    fun konfigurierbarer_schwellwert_wirkt() { // LH-FA-BEL-005
        // Rest 0.2 löst bei Standard (0.30) nicht aus, bei 0.1 schon.
        assertFalse(ReHypothesenAusloeser().ausgeloest(mitRest(0.2)))
        assertTrue(ReHypothesenAusloeser(schwellwert = 0.1).ausgeloest(mitRest(0.2)))
    }

    @Test
    fun ungueltiger_schwellwert_wird_abgelehnt() {
        assertFailsWith<IllegalArgumentException> { ReHypothesenAusloeser(schwellwert = 1.5) }
        assertFailsWith<IllegalArgumentException> { ReHypothesenAusloeser(schwellwert = -0.1) }
    }
}
