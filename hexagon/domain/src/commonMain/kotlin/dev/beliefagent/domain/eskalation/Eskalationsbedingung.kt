package dev.beliefagent.domain.eskalation

import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.GateEntscheidung

/**
 * Eskalationsbedingung als reine Domänen-Regel (`ARC-05`, `LH-FA-ESK-001`): das
 * System hält an und eskaliert **genau dann, wenn** alle drei Teilbedingungen
 * zugleich gelten:
 *  1. die verfügbaren **günstigen Beobachtungen erschöpft** sind
 *     ([beobachtungenErschoepft]),
 *  2. die **Resthypothese hoch** bleibt (**≥** der [schwelle], `LH-FA-POL-002.a`) und
 *  3. das **Aktions-Gate geschlossen** bleibt (die [gate]-Entscheidung ist **keine**
 *     [GateEntscheidung.Freigabe]).
 *
 * Fehlt eine der drei, wird **nicht** eskaliert (dann sammeln oder handeln —
 * slice-017). Deterministisch (`LH-QA-03`), framework-frei.
 *
 * Die [schwelle] ist **θ_esc = 0,30** (`ADR-0007`, spec-konform, `spezifikation.md`
 * §3). Sie deckt sich seit `ADR-0008` mit θ_rehyp
 * ([dev.beliefagent.domain.belief.ReHypothesenAusloeser.STANDARD_SCHWELLWERT] = 0,30;
 * Spec: „θ_esc Startwert = θ_rehyp") und ist **verschieden** von der Irreversibel-Sperre
 * θ_other_block (`ADR-0008`, 0,10). Als **eigener** Default gehalten, damit die
 * `≥`-Semantik (`LH-FA-POL-002.a`) explizit bleibt — θ_rehyp nutzt striktes `>`.
 *
 * Der **Budget-Auslöser** (`LH-FA-ESK-004`) ist bewusst **nicht** Teil dieser
 * Bedingung — er ist ein getrennter Pfad ([Budget.erschoepft]).
 */
object Eskalationsbedingung {

    /** θ_esc = 0,30 (`ADR-0007`, spec-konform, `spezifikation.md` §3); Vergleich `≥` (`LH-FA-POL-002.a`). */
    const val STANDARD_ESKALATIONS_SCHWELLE: Double = 0.30

    fun erfuellt(
        beobachtungenErschoepft: Boolean,
        belief: BeliefState,
        gate: GateEntscheidung,
        schwelle: Double = STANDARD_ESKALATIONS_SCHWELLE,
    ): Boolean {
        require(schwelle in 0.0..1.0) { "Eskalations-Schwelle muss in [0,1] liegen: $schwelle" }
        return beobachtungenErschoepft &&
            belief.resthypothese.wahrscheinlichkeit >= schwelle &&
            gate !is GateEntscheidung.Freigabe
    }
}
