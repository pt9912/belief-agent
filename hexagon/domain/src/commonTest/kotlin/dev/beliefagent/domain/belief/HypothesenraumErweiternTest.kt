package dev.beliefagent.domain.belief

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Deterministische Übernahme-Regel für dynamische Hypothesenräume
 * (LH-FA-BEL-006, LH-FA-BEL-007, LH-QA-03).
 */
class HypothesenraumErweiternTest {

    private val auth = HypotheseId("auth")
    private val frontend = HypotheseId("frontend")
    private val gateway = HypotheseId("gateway")

    @Test
    fun uebernimmt_neue_kandidaten_aus_der_resthypothese_normiert() {
        val prior = BeliefState.of(
            hypothesen = listOf(Hypothese(auth, 0.50), Hypothese(frontend, 0.20)),
            resthypothese = Resthypothese(0.30),
        )

        val erweitert = HypothesenraumErweitern.mitKandidaten(
            prior,
            listOf(
                HypothesenKandidat(gateway, KandidatenScore(0.50), listOf(EvidenzReferenz("obs:log-gateway"))),
            ),
        )

        assertProbability(0.50, erweitert.hypothese(auth).wahrscheinlichkeit)
        assertProbability(0.20, erweitert.hypothese(frontend).wahrscheinlichkeit)
        assertProbability(0.15, erweitert.hypothese(gateway).wahrscheinlichkeit)
        assertProbability(0.15, erweitert.resthypothese.wahrscheinlichkeit)
        assertEquals(listOf("obs:log-gateway"), erweitert.hypothese(gateway).stuetzendeEvidenz.map { it.wert })
    }

    @Test
    fun verfeinert_bestehende_hypothese_und_erhaelt_evidenzreferenzen() {
        val prior = BeliefState.of(
            hypothesen = listOf(
                Hypothese(auth, 0.50, listOf(EvidenzReferenz("obs:test-rot"))),
                Hypothese(frontend, 0.20),
            ),
            resthypothese = Resthypothese(0.30),
        )

        val erweitert = HypothesenraumErweitern.mitKandidaten(
            prior,
            listOf(
                HypothesenKandidat(auth, KandidatenScore(0.25), listOf(EvidenzReferenz("obs:stacktrace"))),
            ),
        )

        assertProbability(0.575, erweitert.hypothese(auth).wahrscheinlichkeit)
        assertProbability(0.20, erweitert.hypothese(frontend).wahrscheinlichkeit)
        assertProbability(0.225, erweitert.resthypothese.wahrscheinlichkeit)
        assertEquals(
            listOf("obs:test-rot", "obs:stacktrace"),
            erweitert.hypothese(auth).stuetzendeEvidenz.map { it.wert },
        )
    }

    @Test
    fun lehnt_kandidaten_ab_die_mehr_als_die_restmasse_beanspruchen() {
        val prior = BeliefState.of(
            hypothesen = listOf(Hypothese(auth, 0.60)),
            resthypothese = Resthypothese(0.40),
        )

        assertFailsWith<IllegalArgumentException> {
            HypothesenraumErweitern.mitKandidaten(
                prior,
                listOf(
                    HypothesenKandidat(frontend, KandidatenScore(0.70), listOf(EvidenzReferenz("obs:ui"))),
                    HypothesenKandidat(gateway, KandidatenScore(0.40), listOf(EvidenzReferenz("obs:gateway"))),
                ),
            )
        }
    }

    @Test
    fun leere_kandidatenliste_laesst_prior_unveraendert() {
        val prior = BeliefState.of(
            hypothesen = listOf(Hypothese(auth, 0.60)),
            resthypothese = Resthypothese(0.40),
        )

        assertEquals(prior, HypothesenraumErweitern.mitKandidaten(prior, emptyList()))
    }

    private fun BeliefState.hypothese(id: HypotheseId): Hypothese =
        hypothesen.single { it.id == id }

    private fun assertProbability(expected: Double, actual: Double) {
        assertEquals(expected, actual, BeliefState.NORMIERUNGS_TOLERANZ)
    }
}
