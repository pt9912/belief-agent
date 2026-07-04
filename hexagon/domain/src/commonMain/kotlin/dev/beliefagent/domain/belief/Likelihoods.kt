package dev.beliefagent.domain.belief

/**
 * Likelihoods P(Evidenz | Hypothese) für ein Belief-Update — je Hypothese
 * (über ihre [HypotheseId]) und für die Resthypothese (LH-FA-OBS-005).
 *
 * Nicht-negativ, aber **nicht** notwendig normiert: Likelihoods sind keine
 * Wahrscheinlichkeitsverteilung über Hypothesen, sondern bewerten dieselbe
 * Beobachtung unter jeder Hypothese. Die Herkunft (z. B. LLM-Port) ist
 * Out-of-Scope dieses Slices — hier kommen sie als deterministische Eingabe
 * (LH-QA-03).
 */
data class Likelihoods(
    val proHypothese: Map<HypotheseId, Double>,
    val resthypothese: Double,
)
