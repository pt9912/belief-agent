package dev.beliefagent.domain.belief

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Deterministische Tests für das nicht-überschreibende Bayes-Update
 * (LH-FA-OBS-003, LH-FA-OBS-005, LH-QA-03). Likelihoods sind feste Eingaben,
 * kein LLM.
 */
class BayesUpdateTest {

    private val a = HypotheseId("A")
    private val b = HypotheseId("B")

    private fun prior() = BeliefState.of(
        hypothesen = listOf(Hypothese(a, 0.5), Hypothese(b, 0.3)),
        resthypothese = Resthypothese(0.2),
    )

    @Test
    fun update_verschiebt_masse_zur_gestuetzten_hypothese() { // LH-FA-OBS-003
        val p = prior()
        val post = BayesUpdate.posterior(
            p, Likelihoods(mapOf(a to 0.9, b to 0.1), resthypothese = 0.1),
        )
        val aPrior = p.hypothesen.first { it.id == a }.wahrscheinlichkeit
        val aPost = post.hypothesen.first { it.id == a }.wahrscheinlichkeit
        assertTrue(aPost > aPrior, "A sollte an Masse gewinnen: prior=$aPrior post=$aPost")
    }

    @Test
    fun update_ist_nicht_ueberschreibend() { // LH-FA-OBS-003
        val p = prior()
        BayesUpdate.posterior(p, Likelihoods(mapOf(a to 0.9, b to 0.1), resthypothese = 0.1))
        // Der Prior bleibt unveraendert (immutable, kein Ueberschreiben).
        assertEquals(0.5, p.hypothesen.first { it.id == a }.wahrscheinlichkeit)
        assertEquals(0.2, p.resthypothese.wahrscheinlichkeit)
    }

    @Test
    fun resthypothese_wird_per_likelihood_aktualisiert() { // LH-FA-OBS-005
        val p = prior()
        val post = BayesUpdate.posterior(
            p, Likelihoods(mapOf(a to 0.1, b to 0.1), resthypothese = 0.9),
        )
        assertTrue(
            post.resthypothese.wahrscheinlichkeit > p.resthypothese.wahrscheinlichkeit,
            "Resthypothese sollte an Masse gewinnen",
        )
    }

    @Test
    fun gleiche_likelihoods_erhalten_die_verteilung() {
        val p = prior()
        val post = BayesUpdate.posterior(
            p, Likelihoods(mapOf(a to 0.5, b to 0.5), resthypothese = 0.5),
        )
        assertEquals(0.5, post.hypothesen.first { it.id == a }.wahrscheinlichkeit, 1e-9)
        assertEquals(0.3, post.hypothesen.first { it.id == b }.wahrscheinlichkeit, 1e-9)
        assertEquals(0.2, post.resthypothese.wahrscheinlichkeit, 1e-9)
    }

    @Test
    fun posterior_ist_normiert() { // LH-FA-BEL-002 (wiederverwendet)
        val p = prior()
        val post = BayesUpdate.posterior(
            p, Likelihoods(mapOf(a to 0.7, b to 0.2), resthypothese = 0.4),
        )
        val summe = post.hypothesen.sumOf { it.wahrscheinlichkeit } + post.resthypothese.wahrscheinlichkeit
        assertEquals(1.0, summe, 1e-9)
    }

    @Test
    fun posterior_erhaelt_evidenzreferenzen_der_hypothesen() { // LH-FA-BEL-007
        val p = BeliefState.of(
            hypothesen = listOf(
                Hypothese(a, 0.5, listOf(EvidenzReferenz("obs:test-rot"))),
                Hypothese(b, 0.3),
            ),
            resthypothese = Resthypothese(0.2),
        )

        val post = BayesUpdate.posterior(
            p,
            Likelihoods(mapOf(a to 0.9, b to 0.1), resthypothese = 0.1),
        )

        assertEquals(listOf("obs:test-rot"), post.hypothesen.first { it.id == a }.stuetzendeEvidenz.map { it.wert })
    }

    @Test
    fun deterministisch_bei_gegebenen_likelihoods() { // LH-QA-03
        val p = prior()
        val l = Likelihoods(mapOf(a to 0.6, b to 0.25), resthypothese = 0.3)
        val erste = BayesUpdate.posterior(p, l)
        val zweite = BayesUpdate.posterior(p, l)
        assertEquals(
            erste.hypothesen.map { it.wahrscheinlichkeit },
            zweite.hypothesen.map { it.wahrscheinlichkeit },
        )
        assertEquals(erste.resthypothese.wahrscheinlichkeit, zweite.resthypothese.wahrscheinlichkeit)
    }

    @Test
    fun fehlende_likelihood_wird_abgelehnt() {
        val p = prior()
        assertFailsWith<IllegalArgumentException> {
            // Likelihood für B fehlt
            BayesUpdate.posterior(p, Likelihoods(mapOf(a to 0.5), resthypothese = 0.5))
        }
    }

    @Test
    fun null_gesamtmasse_wird_abgelehnt() {
        val p = prior()
        assertFailsWith<IllegalArgumentException> {
            BayesUpdate.posterior(p, Likelihoods(mapOf(a to 0.0, b to 0.0), resthypothese = 0.0))
        }
    }

    @Test
    fun unbekannte_likelihood_id_wird_abgelehnt() { // Review-Befund 2
        val p = prior()
        assertFailsWith<IllegalArgumentException> {
            BayesUpdate.posterior(
                p,
                Likelihoods(mapOf(a to 0.5, b to 0.3, HypotheseId("X") to 0.1), resthypothese = 0.1),
            )
        }
    }
}
