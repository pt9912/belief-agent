package dev.beliefagent.application.belief.konfidenzexternalisieren

import dev.beliefagent.application.belief.ports.ExternalisierteKonfidenz
import dev.beliefagent.application.belief.ports.KonfidenzPort
import dev.beliefagent.application.belief.ports.KonfidenzQuelle
import dev.beliefagent.application.belief.ports.KonfidenzReferenz
import dev.beliefagent.application.belief.ports.KonfidenzVersion
import dev.beliefagent.application.belief.ports.ModellKonfidenz
import dev.beliefagent.application.belief.ports.OverrideBegruendung
import dev.beliefagent.application.ports.AuditPort
import dev.beliefagent.domain.belief.KonfidenzExternalisiert
import dev.beliefagent.domain.belief.KonfidenzUeberschrieben
import dev.beliefagent.domain.belief.Zeitstempel

data class KonfidenzExternalisierenBefehl(
    val referenz: KonfidenzReferenz,
    val roheKonfidenz: Double,
    val quelle: KonfidenzQuelle,
    val zeitstempel: Zeitstempel,
)

data class KonfidenzOverrideBefehl(
    val referenz: KonfidenzReferenz,
    val neueKonfidenz: Double,
    val begruendung: OverrideBegruendung,
    val zeitstempel: Zeitstempel,
)

/**
 * Use-Case zur Externalisierung impliziter Modell-Konfidenz (LH-FA-LLM-003).
 *
 * Der Use Case erzeugt explizite Contract-Werte, schreibt sie append-only ueber
 * [KonfidenzPort] und protokolliert jede Externalisierung bzw. jeden Override
 * als neues Audit-Ereignis. Er trifft keine Gate-Entscheidung.
 */
class KonfidenzExternalisieren(
    private val konfidenzen: KonfidenzPort,
    private val audit: AuditPort,
) {

    fun externalisieren(befehl: KonfidenzExternalisierenBefehl): ExternalisierteKonfidenz {
        val eintrag = ExternalisierteKonfidenz(
            referenz = befehl.referenz,
            wert = ModellKonfidenz(befehl.roheKonfidenz),
            quelle = befehl.quelle,
            version = naechsteVersion(befehl.referenz),
        )
        konfidenzen.anhaengen(eintrag)
        audit.anhaengen(
            KonfidenzExternalisiert(
                zeitstempel = befehl.zeitstempel,
                referenz = eintrag.referenz.wert,
                wert = eintrag.wert.wert,
                quelle = eintrag.quelle.wert,
                version = eintrag.version.wert,
            ),
        )
        return eintrag
    }

    fun ueberschreiben(befehl: KonfidenzOverrideBefehl): ExternalisierteKonfidenz {
        val bisher = konfidenzen.lade(befehl.referenz)
        require(bisher.isNotEmpty()) {
            "Konfidenz-Override braucht einen bestehenden Eintrag: ${befehl.referenz.wert}"
        }
        val alter = bisher.maxBy { it.version.wert }
        val neuer = ExternalisierteKonfidenz(
            referenz = befehl.referenz,
            wert = ModellKonfidenz(befehl.neueKonfidenz),
            quelle = alter.quelle,
            version = alter.version.naechste(),
            overrideBegruendung = befehl.begruendung,
        )
        konfidenzen.anhaengen(neuer)
        audit.anhaengen(
            KonfidenzUeberschrieben(
                zeitstempel = befehl.zeitstempel,
                referenz = neuer.referenz.wert,
                alterWert = alter.wert.wert,
                neuerWert = neuer.wert.wert,
                begruendung = befehl.begruendung.wert,
                version = neuer.version.wert,
            ),
        )
        return neuer
    }

    private fun naechsteVersion(referenz: KonfidenzReferenz): KonfidenzVersion =
        konfidenzen.lade(referenz)
            .maxByOrNull { it.version.wert }
            ?.version
            ?.naechste()
            ?: KonfidenzVersion(1)
}
