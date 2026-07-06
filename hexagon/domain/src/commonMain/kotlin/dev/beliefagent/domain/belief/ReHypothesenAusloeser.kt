package dev.beliefagent.domain.belief

/**
 * Re-Hypothesen-Auslöser (LH-FA-BEL-005): signalisiert, dass neue bzw.
 * verfeinerte Hypothesen erzeugt werden müssen, sobald die Resthypothese den
 * **konfigurierbaren** [schwellwert] überschreitet.
 *
 * Erzeugt nur das **Signal** ([ausgeloest]); die inhaltliche Hypothesen-
 * Erzeugung über den LLM-Port ist Out-of-Scope dieser Welle (Welle-05). Reine,
 * deterministische Domänen-Regel (LH-QA-03).
 */
class ReHypothesenAusloeser(
    val schwellwert: Double = STANDARD_SCHWELLWERT,
) {
    init {
        require(schwellwert in 0.0..1.0) {
            "Schwellwert muss in [0,1] liegen: $schwellwert"
        }
    }

    /** True, wenn die Resthypothesen-Masse den Schwellwert **echt** überschreitet. */
    fun ausgeloest(belief: BeliefState): Boolean =
        belief.resthypothese.wahrscheinlichkeit > schwellwert

    companion object {
        /**
         * Default-Schwellwert θ_rehyp (`LH-FA-BEL-005`, `spezifikation.md` §3,
         * `ADR-0008`): ab **0,30** Resthypothese-Masse ist der Hypothesenraum
         * erweiterungs-bedürftig. Deckt sich mit θ_esc (`ADR-0007`) — Spec:
         * „θ_esc Startwert = θ_rehyp".
         */
        const val STANDARD_SCHWELLWERT: Double = 0.30
    }
}
