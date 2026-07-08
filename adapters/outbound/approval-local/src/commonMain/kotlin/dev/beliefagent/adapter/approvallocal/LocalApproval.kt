package dev.beliefagent.adapter.approvallocal

import dev.beliefagent.application.belief.gaten.ports.ApprovalAnfrage
import dev.beliefagent.application.belief.gaten.ports.HumanApprovalPort
import dev.beliefagent.domain.belief.Aktion
import dev.beliefagent.domain.belief.BeliefState

@JvmInline
value class ApprovalNonce(val wert: String) {
    init {
        require(wert.isNotBlank()) { "Approval-Nonce darf nicht leer sein" }
    }
}

@JvmInline
value class ApprovalKontextDigest(val wert: String) {
    init {
        require(wert.isNotBlank()) { "Approval-Kontext-Digest darf nicht leer sein" }
    }
}

data class ApprovalChallenge(
    val anfrage: ApprovalAnfrage,
    val nonce: ApprovalNonce,
    val kontextDigest: ApprovalKontextDigest,
    val text: String,
)

data class ApprovalAntwort(
    val nonce: String,
    val identitaet: String,
    val kontextDigest: String,
    val bestaetigung: String,
)

fun interface ApprovalNonceQuelle {
    fun naechsteNonce(): ApprovalNonce
}

fun interface ApprovalEingabe {
    fun lese(challenge: ApprovalChallenge): ApprovalAntwort?
}

fun interface ApprovalAusgabe {
    fun schreibe(challenge: ApprovalChallenge)
}

class InMemoryApprovalNonceStore {
    private val verbraucht = mutableSetOf<ApprovalNonce>()

    fun verbrauche(nonce: ApprovalNonce): Boolean = verbraucht.add(nonce)
}

/**
 * Lokaler Human-Approval-Adapter (ARC-08).
 *
 * Der Adapter erzeugt keine Aktion und keine Executor-Freigabe. Er beantwortet
 * nur den [HumanApprovalPort] und bleibt fail-closed: jede fehlende, falsche
 * oder wiederverwendete Eingabe liefert `false`.
 */
class LocalApproval(
    private val nonceQuelle: ApprovalNonceQuelle,
    private val eingabe: ApprovalEingabe,
    private val ausgabe: ApprovalAusgabe = ApprovalAusgabe {},
    private val nonceStore: InMemoryApprovalNonceStore = InMemoryApprovalNonceStore(),
    private val digestBerechner: ApprovalKontextDigestBerechner = ApprovalKontextDigestBerechner(),
) : HumanApprovalPort {

    override fun freigegeben(anfrage: ApprovalAnfrage): Boolean {
        val nonce = nonceQuelle.naechsteNonce()
        val digest = digestBerechner.digest(anfrage)
        val challenge = ApprovalChallenge(
            anfrage = anfrage,
            nonce = nonce,
            kontextDigest = digest,
            text = render(anfrage, nonce, digest),
        )

        ausgabe.schreibe(challenge)
        val antwort = eingabe.lese(challenge) ?: return false

        if (antwort.nonce != nonce.wert) return false
        if (antwort.identitaet.isBlank()) return false
        if (antwort.kontextDigest != digest.wert) return false
        if (antwort.bestaetigung != BESTAETIGUNG) return false
        if (!nonceStore.verbrauche(nonce)) return false

        return true
    }

    private fun render(
        anfrage: ApprovalAnfrage,
        nonce: ApprovalNonce,
        digest: ApprovalKontextDigest,
    ): String = buildString {
        appendLine("Human approval required")
        appendLine("nonce=${nonce.wert}")
        appendLine("context_digest=${digest.wert}")
        appendLine("action=${anfrage.aktion.beschreibung}")
        appendLine("wirkungsklasse=${anfrage.aktion.wirkungsklasse.name}")
        appendLine("p_success=${anfrage.aktion.erfolgswahrscheinlichkeit.wert}")
        appendLine("resthypothese=${anfrage.belief.resthypothese.wahrscheinlichkeit}")
        append("confirm=$BESTAETIGUNG")
    }

    companion object {
        const val BESTAETIGUNG: String = "FREIGEBEN"
    }
}

class ApprovalKontextDigestBerechner {
    fun digest(anfrage: ApprovalAnfrage): ApprovalKontextDigest =
        ApprovalKontextDigest(stableHash(anfrage.canonicalForm()).toString(16))

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
