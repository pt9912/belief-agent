package dev.beliefagent.domain.eskalation

import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.GateEntscheidung
import dev.beliefagent.domain.belief.ReHypothesenAusloeser

/**
 * Eskalationsbedingung als reine Domänen-Regel (`ARC-05`, `LH-FA-ESK-001`): das
 * System hält an und eskaliert **genau dann, wenn** alle drei Teilbedingungen
 * zugleich gelten:
 *  1. die verfügbaren **günstigen Beobachtungen erschöpft** sind
 *     ([beobachtungenErschoepft]),
 *  2. die **Resthypothese hoch** bleibt (echt über der [schwelle]) und
 *  3. das **Aktions-Gate geschlossen** bleibt (die [gate]-Entscheidung ist **keine**
 *     [GateEntscheidung.Freigabe]).
 *
 * Fehlt eine der drei, wird **nicht** eskaliert (dann sammeln oder handeln —
 * slice-016). Deterministisch (`LH-QA-03`), framework-frei.
 *
 * Die [schwelle] ist θ_esc; ihr Startwert ist an θ_rehyp gekoppelt
 * ([ReHypothesenAusloeser.STANDARD_SCHWELLWERT], Spezifikation §3): Eskalation
 * greift dort, wo Re-Hypothesenbildung indiziert wäre, die günstigen Beobachtungen
 * aber erschöpft sind.
 *
 * Der **Budget-Auslöser** (`LH-FA-ESK-004`) ist bewusst **nicht** Teil dieser
 * Bedingung — er ist ein getrennter Pfad ([Budget.erschoepft]).
 */
object Eskalationsbedingung {

    /** θ_esc — Startwert an θ_rehyp gekoppelt (Spezifikation §3, `LH-FA-ESK-001`). */
    const val STANDARD_ESKALATIONS_SCHWELLE: Double = ReHypothesenAusloeser.STANDARD_SCHWELLWERT

    fun erfuellt(
        beobachtungenErschoepft: Boolean,
        belief: BeliefState,
        gate: GateEntscheidung,
        schwelle: Double = STANDARD_ESKALATIONS_SCHWELLE,
    ): Boolean =
        beobachtungenErschoepft &&
            belief.resthypothese.wahrscheinlichkeit > schwelle &&
            gate !is GateEntscheidung.Freigabe
}
