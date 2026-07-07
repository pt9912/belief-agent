package dev.beliefagent.application.belief.aktionsvorschlag

import dev.beliefagent.application.belief.aktionsvorschlag.dto.AktionsVorschlag
import dev.beliefagent.application.belief.aktionsvorschlag.ports.AktionsVorschlagsPort
import dev.beliefagent.application.belief.entscheidungszyklus.KonfidenzgebundeneAktion
import dev.beliefagent.application.belief.konfidenzexternalisieren.KonfidenzExternalisieren
import dev.beliefagent.application.belief.konfidenzexternalisieren.KonfidenzExternalisierenBefehl
import dev.beliefagent.application.belief.ports.KonfidenzPort
import dev.beliefagent.application.belief.ports.KonfidenzQuelle
import dev.beliefagent.application.belief.ports.KonfidenzReferenz
import dev.beliefagent.application.ports.AuditPort
import dev.beliefagent.domain.belief.AktionVorgeschlagen
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.EvidenzReferenz
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.Wirkungsklasse
import dev.beliefagent.domain.belief.Zeitstempel

data class AktionsVorschlagenBefehl(
    val belief: BeliefState,
    val bekannteEvidenz: Map<EvidenzReferenz, Beobachtung>,
    val zeitstempel: Zeitstempel,
)

data class GateFaehigerAktionsVorschlag(
    val hypotheseId: HypotheseId,
    val aktion: KonfidenzgebundeneAktion,
)

/**
 * Use-Case-Rand fuer LLM-Aktionsvorschlaege (LH-FA-LLM-002/003).
 *
 * Der Use Case normalisiert Rohvorschlaege in gate-faehige,
 * konfidenzgebundene Aktionen. Er erzeugt keine `Aktionsfreigabe`, ruft kein
 * Gate auf und fuehrt keine Aktion aus.
 */
class AktionsVorschlagen(
    private val port: AktionsVorschlagsPort,
    konfidenzen: KonfidenzPort,
    private val audit: AuditPort,
    private val konfidenzQuelle: KonfidenzQuelle = KonfidenzQuelle("aktionsvorschlag"),
) {
    private val konfidenzExternalisieren = KonfidenzExternalisieren(konfidenzen, audit)

    fun ausfuehren(befehl: AktionsVorschlagenBefehl): List<GateFaehigerAktionsVorschlag> {
        val rohVorschlaege = port.vorschlaege(befehl.belief)
        val referenzAnzahlen = rohVorschlaege.groupingBy { it.konfidenzReferenz }.eachCount()
        return rohVorschlaege.mapNotNull { vorschlag ->
            if (referenzAnzahlen[vorschlag.konfidenzReferenz] != 1) return@mapNotNull null
            vorschlag.gateFaehigOderNull(befehl)
        }
    }

    private fun AktionsVorschlag.gateFaehigOderNull(
        befehl: AktionsVorschlagenBefehl,
    ): GateFaehigerAktionsVorschlag? = runCatching {
        val hypothese = HypotheseId(hypotheseId)
        require(befehl.belief.hypothesen.any { it.id == hypothese }) {
            "Aktionsvorschlag referenziert unbekannte Hypothese: $hypotheseId"
        }
        require(beschreibung.isNotBlank()) { "Aktionsvorschlag braucht eine Beschreibung" }
        val klasse = Wirkungsklasse.valueOf(wirkungsklasse)

        val evidenz = stuetzendeEvidenz.map { referenz ->
            befehl.bekannteEvidenz.getValue(EvidenzReferenz(referenz))
        }
        require(evidenz.isNotEmpty()) { "Aktionsvorschlag braucht stuetzende Evidenz" }

        val referenz = KonfidenzReferenz(konfidenzReferenz)
        konfidenzExternalisieren.externalisieren(
            KonfidenzExternalisierenBefehl(
                referenz = referenz,
                roheKonfidenz = pSuccess,
                quelle = konfidenzQuelle,
                zeitstempel = befehl.zeitstempel,
            ),
        )

        val gateFaehigerVorschlag = GateFaehigerAktionsVorschlag(
            hypotheseId = hypothese,
            aktion = KonfidenzgebundeneAktion(
                beschreibung = beschreibung,
                wirkungsklasse = klasse,
                stuetzendeEvidenz = evidenz,
                konfidenzReferenz = referenz,
            ),
        )
        audit.anhaengen(AktionVorgeschlagen(befehl.zeitstempel, gateFaehigerVorschlag.aktion.beschreibung))
        gateFaehigerVorschlag
    }.getOrNull()
}
