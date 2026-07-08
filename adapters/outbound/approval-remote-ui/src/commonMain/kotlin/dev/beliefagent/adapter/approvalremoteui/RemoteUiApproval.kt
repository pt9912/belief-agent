package dev.beliefagent.adapter.approvalremoteui

import dev.beliefagent.application.belief.gaten.ports.ApprovalAnfrage
import dev.beliefagent.application.belief.gaten.ports.HumanApprovalPort
import dev.beliefagent.domain.belief.Aktion
import dev.beliefagent.domain.belief.BeliefState

@JvmInline
value class RemoteApprovalNonce(val wert: String) {
    init {
        require(wert.isNotBlank()) { "Remote-Approval-Nonce darf nicht leer sein" }
    }
}

@JvmInline
value class RemoteApprovalKontextDigest(val wert: String) {
    init {
        require(wert.isNotBlank()) { "Remote-Approval-Kontext-Digest darf nicht leer sein" }
    }
}

data class RemoteApprovalPayload(
    val aktion: String,
    val wirkungsklasse: String,
    val erfolgswahrscheinlichkeit: Double,
    val resthypothese: Double,
)

data class RemoteApprovalAuftrag(
    val anfrage: ApprovalAnfrage,
    val nonce: RemoteApprovalNonce,
    val kontextDigest: RemoteApprovalKontextDigest,
    val payload: RemoteApprovalPayload,
)

data class RemoteApprovalAntwort(
    val nonce: String,
    val identitaet: String,
    val kontextDigest: String,
    val bestaetigung: String,
)

fun interface RemoteApprovalNonceQuelle {
    fun naechsteNonce(): RemoteApprovalNonce
}

fun interface RemoteApprovalTransport {
    fun frage(auftrag: RemoteApprovalAuftrag): List<RemoteApprovalAntwort>
}

class InMemoryRemoteApprovalNonceStore {
    private val verbraucht = mutableSetOf<RemoteApprovalNonce>()

    fun verbrauche(nonce: RemoteApprovalNonce): Boolean = verbraucht.add(nonce)
}

/**
 * Remote/UI-Human-Approval-Adapter (ARC-08).
 *
 * Der Adapter kapselt nur die entfernte Bediengrenze hinter [RemoteApprovalTransport].
 * Er erzeugt keine Aktion und bleibt fail-closed: Transportfehler, fehlende,
 * doppelte oder kontextfalsche Antworten liefern `false`.
 */
class RemoteUiApproval(
    private val nonceQuelle: RemoteApprovalNonceQuelle,
    private val transport: RemoteApprovalTransport,
    private val erlaubteIdentitaeten: Set<String>,
    private val nonceStore: InMemoryRemoteApprovalNonceStore = InMemoryRemoteApprovalNonceStore(),
    private val digestBerechner: RemoteApprovalKontextDigestBerechner = RemoteApprovalKontextDigestBerechner(),
) : HumanApprovalPort {

    init {
        require(erlaubteIdentitaeten.isNotEmpty()) { "Remote-Approval braucht mindestens eine erlaubte Identitaet" }
        require(erlaubteIdentitaeten.none { it.isBlank() }) { "Remote-Approval-Identitaeten duerfen nicht leer sein" }
    }

    override fun freigegeben(anfrage: ApprovalAnfrage): Boolean {
        val nonce = nonceQuelle.naechsteNonce()
        val digest = digestBerechner.digest(anfrage)
        val auftrag = RemoteApprovalAuftrag(
            anfrage = anfrage,
            nonce = nonce,
            kontextDigest = digest,
            payload = RemoteApprovalPayload(
                aktion = anfrage.aktion.beschreibung,
                wirkungsklasse = anfrage.aktion.wirkungsklasse.name,
                erfolgswahrscheinlichkeit = anfrage.aktion.erfolgswahrscheinlichkeit.wert,
                resthypothese = anfrage.belief.resthypothese.wahrscheinlichkeit,
            ),
        )

        val antworten = try {
            transport.frage(auftrag)
        } catch (_: Exception) {
            return false
        }
        if (antworten.size != 1) return false

        val antwort = antworten.single()
        if (antwort.nonce != nonce.wert) return false
        if (antwort.identitaet !in erlaubteIdentitaeten) return false
        if (antwort.kontextDigest != digest.wert) return false
        if (antwort.bestaetigung != BESTAETIGUNG) return false
        if (!nonceStore.verbrauche(nonce)) return false

        return true
    }

    companion object {
        const val BESTAETIGUNG: String = "FREIGEBEN"
    }
}

class RemoteApprovalKontextDigestBerechner {
    fun digest(anfrage: ApprovalAnfrage): RemoteApprovalKontextDigest =
        RemoteApprovalKontextDigest(stableHash(anfrage.canonicalForm()).toString(16))

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
