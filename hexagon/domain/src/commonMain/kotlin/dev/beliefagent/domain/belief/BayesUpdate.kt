package dev.beliefagent.domain.belief

/**
 * Bayesianisches Belief-Update als reine Domänen-Regel (LH-FA-OBS-003):
 * `Posterior ∝ Prior × Likelihood`, anschließend renormiert.
 *
 * **Nicht-überschreibend** (LH-FA-OBS-003): erzeugt einen NEUEN [BeliefState];
 * der übergebene Prior bleibt unverändert. Die Resthypothese wird ebenfalls
 * per Likelihood bewertet (LH-FA-OBS-005). Deterministisch bei gegebenen
 * Likelihoods (LH-QA-03) — **kein LLM**; die spätere application-Slice
 * *belief-aktualisieren* (ARC-02) speist die Likelihoods über den LLM-Port.
 */
object BayesUpdate {

    /**
     * Posterior-Belief aus [prior] und [likelihoods], renormiert auf Σp = 1
     * (Validierung via [BeliefState.of]).
     *
     * Wirft [IllegalArgumentException], wenn eine Likelihood fehlt, für eine
     * unbekannte Hypothese angegeben oder negativ ist, oder die Gesamtmasse
     * `Σ(Prior × Likelihood)` null ist (dann ist kein normierbarer Posterior
     * definierbar).
     */
    fun posterior(prior: BeliefState, likelihoods: Likelihoods): BeliefState {
        require(likelihoods.resthypothese >= 0.0) {
            "Resthypothesen-Likelihood ist negativ: ${likelihoods.resthypothese}"
        }
        val bekannteIds = prior.hypothesen.map { it.id }.toSet()
        val unbekannte = likelihoods.proHypothese.keys - bekannteIds
        require(unbekannte.isEmpty()) {
            "Likelihoods für unbekannte Hypothesen: ${unbekannte.map { it.wert }}"
        }

        val unnormierteHypothesen: List<Pair<HypotheseId, Double>> = prior.hypothesen.map { h ->
            val l = likelihoods.proHypothese[h.id]
                ?: throw IllegalArgumentException("Likelihood fehlt für Hypothese '${h.id.wert}'")
            require(l >= 0.0) { "Likelihood für '${h.id.wert}' ist negativ: $l" }
            h.id to h.wahrscheinlichkeit * l
        }
        val unnormierteRestmasse = prior.resthypothese.wahrscheinlichkeit * likelihoods.resthypothese

        val gesamtmasse = unnormierteHypothesen.sumOf { it.second } + unnormierteRestmasse
        require(gesamtmasse > 0.0) {
            "Posterior-Gesamtmasse ist 0 (Prior × Likelihood überall 0) — Update nicht definierbar"
        }

        val posteriorHypothesen = unnormierteHypothesen.map { (id, masse) ->
            Hypothese(id, masse / gesamtmasse)
        }
        return BeliefState.of(posteriorHypothesen, Resthypothese(unnormierteRestmasse / gesamtmasse))
    }
}
