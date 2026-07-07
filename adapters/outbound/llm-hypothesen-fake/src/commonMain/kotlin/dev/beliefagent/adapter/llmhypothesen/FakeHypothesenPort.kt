package dev.beliefagent.adapter.llmhypothesen

import dev.beliefagent.application.belief.aktualisieren.ports.HypothesenPort
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.EvidenzReferenz
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.HypothesenKandidat
import dev.beliefagent.domain.belief.KandidatenScore

/**
 * Rohe Fake-Konfiguration fuer einen Hypothesen-Kandidaten.
 *
 * Die Werte bleiben absichtlich primitives Konfigurationsmaterial, damit Tests
 * ungueltige Scores oder fehlende Evidenz ohne Umgehung der Domain-Guards
 * abbilden koennen. Das Mapping in Domain-Typen passiert erst im Adapter.
 */
data class FakeHypotheseKonfiguration(
    val id: String,
    val score: Double,
    val evidenzReferenzen: List<String>,
)

/**
 * Deterministischer Fake-Hypothesen-Port (ARC-08, LH-FA-LLM-002).
 *
 * Der Adapter erzeugt keine Modellqualitaet, sondern klar als Fake erkennbare
 * Kandidaten mit explizitem [KandidatenScore] und Evidenzreferenzen
 * (LH-FA-LLM-003). Ungueltige Konfigurationen liefern fail-safe eine leere
 * Kandidatenliste, statt halbvalide Kandidaten in den Use-Case zu geben.
 */
class FakeHypothesenPort(
    private val konfiguration: List<FakeHypotheseKonfiguration> = DEFAULT_KANDIDATEN,
) : HypothesenPort {

    override fun kandidaten(belief: BeliefState): List<HypothesenKandidat> = try {
        konfiguration.map { it.toKandidat() }
    } catch (_: IllegalArgumentException) {
        emptyList()
    }

    private fun FakeHypotheseKonfiguration.toKandidat(): HypothesenKandidat =
        HypothesenKandidat(
            id = HypotheseId(id.also {
                require(it.startsWith("fake-")) { "Fake-HypothesenId muss mit fake- beginnen: $it" }
            }),
            score = KandidatenScore(score),
            stuetzendeEvidenz = evidenzReferenzen.map {
                require(it.startsWith("fake:")) { "Fake-EvidenzReferenz muss mit fake: beginnen: $it" }
                EvidenzReferenz(it)
            },
        )

    companion object {
        val DEFAULT_KANDIDATEN: List<FakeHypotheseKonfiguration> = listOf(
            FakeHypotheseKonfiguration(
                id = "fake-hypothese-unbekannte-ursache",
                score = 0.25,
                evidenzReferenzen = listOf("fake:evidenz:synthetisch-1"),
            ),
        )
    }
}
