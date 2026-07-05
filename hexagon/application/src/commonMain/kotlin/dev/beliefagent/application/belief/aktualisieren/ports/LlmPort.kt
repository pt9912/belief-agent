package dev.beliefagent.application.belief.aktualisieren.ports

import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Likelihoods

/**
 * LLM-Port (ARC-07): schätzt die [Likelihoods] `P(Evidenz | Hypothese)` für eine
 * [Beobachtung] gegen die Hypothesen des [prior] (LH-FA-LLM-001 — das
 * Sprachmodell ist ein austauschbares Modul hinter diesem Port, nicht der Kern).
 *
 * In welle-02 steht dahinter ein deterministischer Fake-Adapter (LH-QA-03); der
 * echte Anbieter-Adapter folgt in welle-05. Use-case-lokaler Port, Rolle `port`:
 * importiert nur Domänentypen, nie einen Adapter.
 */
interface LlmPort {
    fun likelihoods(beobachtung: Beobachtung, prior: BeliefState): Likelihoods
}
