package dev.beliefagent.adapter.llmaction

import dev.beliefagent.application.belief.aktionsvorschlag.dto.AktionsVorschlag
import dev.beliefagent.application.belief.aktionsvorschlag.ports.AktionsVorschlagsPort
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Wirkungsklasse

/**
 * Rohe Fake-Konfiguration fuer einen Aktionsvorschlag.
 *
 * Die Werte bleiben primitives Konfigurationsmaterial, damit Tests ungueltige
 * Modellantworten ohne Umgehung der Application-Validierung abbilden koennen.
 */
data class FakeAktionsVorschlagKonfiguration(
    val beschreibung: String,
    val hypotheseId: String,
    val wirkungsklasse: String,
    val pSuccess: Double,
    val konfidenzReferenz: String,
    val stuetzendeEvidenz: List<String>,
)

/**
 * Deterministischer Fake-Aktionsvorschlags-Port (ARC-08, LH-FA-LLM-002).
 *
 * Der Adapter erzeugt keine Modellqualitaet und keine Freigabe. Er liefert nur
 * strukturierte Rohvorschlaege fuer bekannte Hypothesen; kaputte
 * Konfigurationen fuehren fail-safe zu einer leeren Liste.
 */
class FakeAktionsVorschlagsPort(
    private val konfiguration: List<FakeAktionsVorschlagKonfiguration> = DEFAULT_VORSCHLAEGE,
) : AktionsVorschlagsPort {

    override fun vorschlaege(belief: BeliefState): List<AktionsVorschlag> = try {
        konfiguration
            .filter { vorschlag -> belief.hypothesen.any { it.id.wert == vorschlag.hypotheseId } }
            .map { it.toVorschlag() }
    } catch (_: IllegalArgumentException) {
        emptyList()
    }

    private fun FakeAktionsVorschlagKonfiguration.toVorschlag(): AktionsVorschlag {
        require(beschreibung.isNotBlank()) { "Fake-Aktionsbeschreibung darf nicht leer sein" }
        require(Wirkungsklasse.entries.any { it.name == wirkungsklasse }) {
            "Fake-Wirkungsklasse ist unbekannt: $wirkungsklasse"
        }
        require(pSuccess in 0.0..1.0) { "Fake-pSuccess muss in [0,1] liegen: $pSuccess" }
        require(konfidenzReferenz.startsWith("fake:konfidenz:")) {
            "Fake-Konfidenzreferenz muss mit fake:konfidenz: beginnen: $konfidenzReferenz"
        }
        require(stuetzendeEvidenz.isNotEmpty()) { "Fake-Aktionsvorschlag braucht Evidenz" }
        require(stuetzendeEvidenz.all { it.startsWith("fake:evidenz:") }) {
            "Fake-Evidenzreferenzen muessen mit fake:evidenz: beginnen: $stuetzendeEvidenz"
        }
        return AktionsVorschlag(
            beschreibung = beschreibung,
            hypotheseId = hypotheseId,
            wirkungsklasse = wirkungsklasse,
            pSuccess = pSuccess,
            konfidenzReferenz = konfidenzReferenz,
            stuetzendeEvidenz = stuetzendeEvidenz,
        )
    }

    companion object {
        val DEFAULT_VORSCHLAEGE: List<FakeAktionsVorschlagKonfiguration> = listOf(
            FakeAktionsVorschlagKonfiguration(
                beschreibung = "Fake-Loganalyse ausfuehren",
                hypotheseId = "fake-hypothese-regression",
                wirkungsklasse = "NUR_LESEND",
                pSuccess = 0.8,
                konfidenzReferenz = "fake:konfidenz:aktion-loganalyse",
                stuetzendeEvidenz = listOf("fake:evidenz:log-1"),
            ),
        )
    }
}
