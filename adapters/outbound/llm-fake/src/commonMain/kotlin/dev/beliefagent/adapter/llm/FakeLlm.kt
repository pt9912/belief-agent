package dev.beliefagent.adapter.llm

import dev.beliefagent.application.belief.aktualisieren.ports.LlmPort
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Likelihoods

/**
 * Deterministischer Fake-LLM-Adapter (ARC-08): steht in welle-02 für das echte
 * Sprachmodell hinter dem [LlmPort]. Regel (bewusst einfach, revidierbar): eine
 * Hypothese, deren `id` in der Evidenz-Beschreibung vorkommt, bekommt die hohe
 * Likelihood [treffer], sonst [daneben]; die Resthypothese ebenfalls [daneben].
 * Deterministisch (LH-QA-03), damit Update und Replay stabil sind; der echte
 * Anbieter-Adapter folgt in welle-05.
 */
class FakeLlm(
    private val treffer: Double = 0.9,
    private val daneben: Double = 0.1,
) : LlmPort {
    override fun likelihoods(beobachtung: Beobachtung, prior: BeliefState): Likelihoods {
        val text = beobachtung.evidenz.beschreibung.lowercase()
        val proHypothese = prior.hypothesen.associate { h ->
            h.id to if (text.contains(h.id.wert.lowercase())) treffer else daneben
        }
        return Likelihoods(proHypothese, resthypothese = daneben)
    }
}
