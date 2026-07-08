package dev.beliefagent.adapter.approvallocal

import dev.beliefagent.application.belief.gaten.ports.ApprovalAnfrage
import dev.beliefagent.application.belief.gaten.ports.ApprovalAuditSnapshot
import dev.beliefagent.application.belief.gaten.ports.ApprovalErgebnis
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

    override fun entscheide(anfrage: ApprovalAnfrage): ApprovalErgebnis {
        val nonce = nonceQuelle.naechsteNonce()
        val digest = digestBerechner.digest(anfrage)
        val challenge = ApprovalChallenge(
            anfrage = anfrage,
            nonce = nonce,
            kontextDigest = digest,
            text = render(anfrage, nonce, digest),
        )

        try {
            ausgabe.schreibe(challenge)
        } catch (_: Exception) {
            return fehler(digest, nonce, null, null, "ausgabe-fehler")
        }
        val antwort = try {
            eingabe.lese(challenge)
        } catch (_: Exception) {
            return fehler(digest, nonce, null, null, "eingabe-fehler")
        } ?: return verweigert(digest, nonce, null, null, "keine-antwort")

        if (antwort.nonce != nonce.wert) return verweigert(digest, nonce, antwort, "falsche-nonce")
        if (antwort.identitaet.isBlank()) return verweigert(digest, nonce, antwort, "leere-identitaet")
        if (antwort.kontextDigest != digest.wert) return verweigert(digest, nonce, antwort, "digest-mismatch")
        if (antwort.bestaetigung != BESTAETIGUNG) return verweigert(digest, nonce, antwort, "bestaetigung-fehlt")
        if (!nonceStore.verbrauche(nonce)) return verweigert(digest, nonce, antwort, "nonce-replay")

        return ApprovalErgebnis.freigegeben(snapshot(digest, nonce, antwort, "freigegeben"))
    }

    private fun verweigert(
        digest: ApprovalKontextDigest,
        nonce: ApprovalNonce,
        antwort: ApprovalAntwort?,
        grund: String,
    ): ApprovalErgebnis = ApprovalErgebnis.verweigert(snapshot(digest, nonce, antwort, grund))

    private fun verweigert(
        digest: ApprovalKontextDigest,
        nonce: ApprovalNonce,
        antwortReferenz: String?,
        identitaetsReferenz: String?,
        grund: String,
    ): ApprovalErgebnis = ApprovalErgebnis.verweigert(
        snapshot(digest, nonce, antwortReferenz, identitaetsReferenz, grund),
    )

    private fun fehler(
        digest: ApprovalKontextDigest,
        nonce: ApprovalNonce,
        antwortReferenz: String?,
        identitaetsReferenz: String?,
        grund: String,
    ): ApprovalErgebnis = ApprovalErgebnis.fehler(
        snapshot(digest, nonce, antwortReferenz, identitaetsReferenz, grund),
    )

    private fun snapshot(
        digest: ApprovalKontextDigest,
        nonce: ApprovalNonce,
        antwort: ApprovalAntwort?,
        grund: String,
    ): ApprovalAuditSnapshot = snapshot(
        digest = digest,
        nonce = nonce,
        antwortReferenz = antwort?.nonce?.let { "local-response:$it" },
        identitaetsReferenz = antwort?.identitaet?.takeIf { it.isNotBlank() },
        grund = grund,
    )

    private fun snapshot(
        digest: ApprovalKontextDigest,
        nonce: ApprovalNonce,
        antwortReferenz: String?,
        identitaetsReferenz: String?,
        grund: String,
    ): ApprovalAuditSnapshot = ApprovalAuditSnapshot(
        anfrageKontextDigest = digest.wert,
        kanal = KANAL,
        nonceReferenz = "local:${nonce.wert}",
        antwortReferenz = antwortReferenz,
        identitaetsReferenz = identitaetsReferenz,
        ergebnisGrund = grund,
    )

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
        const val KANAL: String = "local"
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
