package dev.beliefagent.application.belief.aktualisieren

import dev.beliefagent.application.belief.aktualisieren.ports.LlmPort
import dev.beliefagent.application.belief.aktualisieren.ports.UhrPort
import dev.beliefagent.domain.belief.BayesUpdate
import dev.beliefagent.domain.belief.BeliefAktualisiert
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.BeobachtungErfasst
import dev.beliefagent.domain.belief.Dedup
import dev.beliefagent.domain.belief.Ereignis

/** Befehl: aus [prior] mit [beobachtungen] einen neuen Belief State ableiten. */
data class BeliefAktualisierenBefehl(
    val prior: BeliefState,
    val beobachtungen: List<Beobachtung>,
)

/** Ergebnis: der neue Belief State und die dabei entstandenen [Ereignis]se. */
data class BeliefAktualisierenErgebnis(
    val belief: BeliefState,
    val ereignisse: List<Ereignis>,
)

/**
 * Use-Case *belief-aktualisieren* (ARC-02, LH-FA-OBS-002): die nachvollziehbare
 * Belief-Update-Pipeline. Korrelierte Beobachtungen werden zuerst auf
 * unabhängige Evidenz reduziert ([Dedup], slice-006); je unabhängiger
 * Beobachtung holt der [LlmPort] die Likelihoods, [BayesUpdate] (slice-003)
 * schreibt den Belief **nicht-überschreibend** fort, und jeder Schritt erzeugt
 * Protokoll-Ereignisse (erfasst + aktualisiert) mit [UhrPort]-Zeitstempel.
 *
 * Deterministisch bei deterministischen Ports (LH-QA-03) — **kein** direkter
 * LLM-/`Clock`-Aufruf im Kern. Das Persistieren der Ereignisse über den
 * Audit-Port ist Out-of-Scope (slice-010); hier liegen sie im Ergebnis.
 */
class BeliefAktualisieren(
    private val llm: LlmPort,
    private val uhr: UhrPort,
) {
    fun ausfuehren(befehl: BeliefAktualisierenBefehl): BeliefAktualisierenErgebnis {
        var belief = befehl.prior
        val ereignisse = mutableListOf<Ereignis>()
        for (beobachtung in Dedup.unabhaengig(befehl.beobachtungen)) {
            val t = uhr.jetzt()
            ereignisse += BeobachtungErfasst(t, beobachtung)
            belief = BayesUpdate.posterior(belief, llm.likelihoods(beobachtung, belief))
            ereignisse += BeliefAktualisiert(t, belief)
        }
        return BeliefAktualisierenErgebnis(belief, ereignisse.toList())
    }
}
