package dev.beliefagent.domain.belief

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Deterministische Konstruktionstests für die Domänentypen (LH-QA-03).
 * Kein LLM, keine Zufalls-/Zeitabhängigkeit.
 */
class BeliefStateTest {

    @Test
    fun belief_state_traegt_konkurrierende_hypothesen_mit_wahrscheinlichkeit() { // LH-FA-BEL-001
        val bs = BeliefState.of(
            hypothesen = listOf(
                Hypothese(HypotheseId("auth"), 0.55),
                Hypothese(HypotheseId("frontend"), 0.25),
            ),
            resthypothese = Resthypothese(0.20),
        )

        assertEquals(2, bs.hypothesen.size)
        assertEquals(listOf("auth", "frontend"), bs.hypothesen.map { it.id.wert })
    }

    @Test
    fun belief_state_hat_immer_eine_resthypothese() { // LH-FA-BEL-003
        val bs = BeliefState.of(
            hypothesen = listOf(Hypothese(HypotheseId("auth"), 0.8)),
            resthypothese = Resthypothese(0.2),
        )

        // Die Resthypothese ist Konstruktor-Pflicht: strukturell nicht weglassbar.
        assertEquals(0.2, bs.resthypothese.wahrscheinlichkeit)
    }

    @Test
    fun belief_state_ohne_hypothesen_hat_trotzdem_resthypothese() { // LH-FA-BEL-003
        val bs = BeliefState.of(hypothesen = emptyList(), resthypothese = Resthypothese(1.0))

        assertTrue(bs.hypothesen.isEmpty())
        assertEquals(1.0, bs.resthypothese.wahrscheinlichkeit)
    }

    @Test
    fun hypothese_id_darf_nicht_leer_sein() {
        assertFailsWith<IllegalArgumentException> { HypotheseId(" ") }
    }

    @Test
    fun hypothese_kann_stuetzende_evidenz_referenzieren() { // LH-FA-BEL-007
        val hypothese = Hypothese(
            id = HypotheseId("auth"),
            wahrscheinlichkeit = 0.8,
            stuetzendeEvidenz = listOf(EvidenzReferenz("obs:test-rot")),
        )

        assertEquals(listOf("obs:test-rot"), hypothese.stuetzendeEvidenz.map { it.wert })
    }
}
