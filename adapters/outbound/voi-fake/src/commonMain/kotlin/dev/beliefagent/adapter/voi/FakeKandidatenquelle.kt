package dev.beliefagent.adapter.voi

import dev.beliefagent.application.belief.beobachtungwaehlen.ports.BeobachtungsAuswahlPort
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.voi.VoiKandidat

/**
 * Geordneter Top-2-Schlüssel des aktuellen Beliefs. Der Fake nutzt ihn, um
 * deterministisch verschiedene Kandidatenlisten für verschiedene Hypothesen-Paare
 * zu liefern, ohne VoI-Logik oder Providerlogik zu enthalten.
 */
data class TopHypothesenSchluessel(
    val erste: HypotheseId,
    val zweite: HypotheseId?,
) {
    companion object {
        fun aus(belief: BeliefState): TopHypothesenSchluessel {
            require(belief.hypothesen.isNotEmpty()) {
                "Belief-aware VoI-Kandidaten brauchen mindestens eine Hypothese"
            }
            val top = belief.hypothesen
                .withIndex()
                .sortedWith(
                    compareByDescending<IndexedValue<Hypothese>> { it.value.wahrscheinlichkeit }
                        .thenBy { it.index },
                )
                .map { it.value.id }
            return TopHypothesenSchluessel(top[0], top.getOrNull(1))
        }
    }
}

/**
 * Fake-Kandidatenquelle (`ARC-08`): liefert deterministische [VoiKandidat]en
 * (`LH-FA-VOI-002`, `LH-QA-03`) belief-aware über [TopHypothesenSchluessel].
 *
 * Der alte feste Listen-Konstruktor bleibt erhalten und ignoriert den Belief
 * bewusst; neue Tests/Integrationen können stattdessen ein Mapping nach Top-2
 * Hypothesen übergeben. Der Adapter erzeugt keine stillen Defaults für
 * `erwarteteDiskriminierung`: alle Kandidatenwerte werden explizit konfiguriert.
 */
class FakeKandidatenquelle(
    private val fallback: List<VoiKandidat>,
    private val kandidatenNachTop2: Map<TopHypothesenSchluessel, List<VoiKandidat>> = emptyMap(),
) : BeobachtungsAuswahlPort {
    constructor(kandidaten: List<VoiKandidat>) : this(
        fallback = kandidaten,
        kandidatenNachTop2 = emptyMap(),
    )

    override fun kandidaten(belief: BeliefState): List<VoiKandidat> {
        if (kandidatenNachTop2.isEmpty() || belief.hypothesen.isEmpty()) {
            return fallback.toList()
        }
        return (kandidatenNachTop2[TopHypothesenSchluessel.aus(belief)] ?: fallback).toList()
    }
}
