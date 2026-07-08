package dev.beliefagent.application.belief.gaten.ports

import dev.beliefagent.domain.belief.Aktion
import dev.beliefagent.domain.belief.BeliefState

/**
 * Human-Approval-Port (ARC-07, LH-FA-POL-004): holt für **extern-wirksame**
 * (irreversible) Aktionen eine explizite menschliche Freigabe ein — zusätzlich
 * zur bestandenen Konfidenz-Schwelle. In welle-03 steht dahinter ein
 * deterministischer Fake-Adapter (LH-QA-03); ein echter interaktiver Adapter
 * folgt später. Use-case-lokaler Port, Rolle `port`: importiert nur Domänentypen.
 *
 * **Anforderung an den echten Adapter (welle-05, Sicherheits-relevant):** Die
 * Freigabe muss an die konkrete [ApprovalAnfrage] **gebunden** und **einmal
 * gültig** sein (Nonce/Identität + Entscheidungs-Kontext), damit eine unter
 * einem sicheren Belief erteilte Freigabe nicht später unter einem unsichereren
 * Belief für eine wert-gleiche [Aktion] **wiederverwendet** werden kann. Der
 * Fake ist bewusst deterministisch und erfüllt Nonce/Identität noch nicht.
 */
data class ApprovalAnfrage(
    val aktion: Aktion,
    val belief: BeliefState,
)

data class ApprovalAuditSnapshot(
    val anfrageKontextDigest: String,
    val kanal: String,
    val nonceReferenz: String,
    val antwortReferenz: String?,
    val identitaetsReferenz: String?,
    val ergebnisGrund: String,
) {
    init {
        require(anfrageKontextDigest.isNotBlank()) { "Approval-Kontext-Digest darf nicht leer sein" }
        require(kanal.isNotBlank()) { "Approval-Kanal darf nicht leer sein" }
        require(nonceReferenz.isNotBlank()) { "Approval-Nonce-Referenz darf nicht leer sein" }
        require(antwortReferenz == null || antwortReferenz.isNotBlank()) {
            "Approval-Antwortreferenz darf nicht leer sein"
        }
        require(identitaetsReferenz == null || identitaetsReferenz.isNotBlank()) {
            "Approval-Identitaetsreferenz darf nicht leer sein"
        }
        require(ergebnisGrund.isNotBlank()) { "Approval-Ergebnisgrund darf nicht leer sein" }
    }
}

enum class ApprovalErgebnisArt {
    FREIGEGEBEN,
    VERWEIGERT,
    FEHLER,
}

data class ApprovalErgebnis(
    val art: ApprovalErgebnisArt,
    val audit: ApprovalAuditSnapshot,
) {
    fun istFreigegeben(): Boolean = art == ApprovalErgebnisArt.FREIGEGEBEN

    companion object {
        fun freigegeben(audit: ApprovalAuditSnapshot): ApprovalErgebnis =
            ApprovalErgebnis(ApprovalErgebnisArt.FREIGEGEBEN, audit)

        fun verweigert(audit: ApprovalAuditSnapshot): ApprovalErgebnis =
            ApprovalErgebnis(ApprovalErgebnisArt.VERWEIGERT, audit)

        fun fehler(audit: ApprovalAuditSnapshot): ApprovalErgebnis =
            ApprovalErgebnis(ApprovalErgebnisArt.FEHLER, audit)
    }
}

class ApprovalAuditKontextDigestBerechner {
    fun digest(anfrage: ApprovalAnfrage): String = stableHash(anfrage.canonicalForm()).toString(16)

    private fun ApprovalAnfrage.canonicalForm(): String =
        listOf(
            aktion.canonicalForm(),
            belief.canonicalForm(),
        ).joinToString(separator = "\n")

    private fun Aktion.canonicalForm(): String =
        buildString {
            appendLine("aktion")
            appendLine("beschreibung=$beschreibung")
            appendLine("wirkungsklasse=${wirkungsklasse.name}")
            appendLine("p_success=${erfolgswahrscheinlichkeit.wert}")
            stuetzendeEvidenz
                .sortedWith(compareBy({ it.quelle.name }, { it.zeitstempel.epochMillis }, { it.evidenz.beschreibung }))
                .forEachIndexed { index, beobachtung ->
                    appendLine(
                        "evidenz[$index]=${beobachtung.quelle.name}|" +
                            "${beobachtung.zeitstempel.epochMillis}|${beobachtung.evidenz.beschreibung}",
                    )
                }
        }

    private fun BeliefState.canonicalForm(): String =
        buildString {
            appendLine("belief")
            hypothesen
                .sortedBy { it.id.wert }
                .forEachIndexed { index, hypothese ->
                    appendLine("hypothese[$index]=${hypothese.id.wert}|${hypothese.wahrscheinlichkeit}")
                    hypothese.stuetzendeEvidenz
                        .sortedBy { it.wert }
                        .forEachIndexed { evidenzIndex, evidenz ->
                            appendLine("hypothese[$index].evidenz[$evidenzIndex]=${evidenz.wert}")
                        }
                }
            append("resthypothese=${resthypothese.wahrscheinlichkeit}")
        }

    private fun stableHash(text: String): ULong {
        var hash = 14695981039346656037UL
        text.encodeToByteArray().forEach { byte ->
            hash = hash xor byte.toUByte().toULong()
            hash *= 1099511628211UL
        }
        return hash
    }
}

interface HumanApprovalPort {
    /** Adapterfreies Ergebnis fuer diese konkrete [anfrage], inklusive Audit-Snapshot. */
    fun entscheide(anfrage: ApprovalAnfrage): ApprovalErgebnis
}
