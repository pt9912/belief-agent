package dev.beliefagent.adapter.approvalremoteui

import dev.beliefagent.application.belief.gaten.ports.ApprovalAnfrage
import dev.beliefagent.domain.belief.Aktion
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.Erfolgswahrscheinlichkeit
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Resthypothese
import dev.beliefagent.domain.belief.Wirkungsklasse
import dev.beliefagent.domain.belief.Zeitstempel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RemoteUiApprovalTest {

    private fun aktion(beschreibung: String = "Deploy") = Aktion(
        beschreibung = beschreibung,
        wirkungsklasse = Wirkungsklasse.EXTERN_WIRKSAM,
        erfolgswahrscheinlichkeit = Erfolgswahrscheinlichkeit(0.95),
        stuetzendeEvidenz = listOf(Beobachtung(Quelle.TEST, Zeitstempel(1L), Evidenz("green build"))),
    )

    private fun belief(rest: Double) =
        BeliefState.of(listOf(Hypothese(HypotheseId("A"), 1.0 - rest)), Resthypothese(rest))

    private fun anfrage(rest: Double = 0.1, beschreibung: String = "Deploy") =
        ApprovalAnfrage(aktion(beschreibung), belief(rest))

    private fun approval(
        nonce: String = "nonce-remote",
        store: InMemoryRemoteApprovalNonceStore = InMemoryRemoteApprovalNonceStore(),
        transport: RemoteApprovalTransport,
    ) = RemoteUiApproval(
        nonceQuelle = RemoteApprovalNonceQuelle { RemoteApprovalNonce(nonce) },
        transport = transport,
        erlaubteIdentitaeten = setOf("operator"),
        nonceStore = store,
    )

    private fun passendeAntwort(auftrag: RemoteApprovalAuftrag) = RemoteApprovalAntwort(
        nonce = auftrag.nonce.wert,
        identitaet = "operator",
        kontextDigest = auftrag.kontextDigest.wert,
        bestaetigung = RemoteUiApproval.BESTAETIGUNG,
    )

    @Test
    fun passende_remote_antwort_gibt_genau_eine_anfrage_frei() {
        val store = InMemoryRemoteApprovalNonceStore()
        val approval = approval(store = store) { listOf(passendeAntwort(it)) }

        assertTrue(approval.freigegeben(anfrage()))
        assertFalse(approval.freigegeben(anfrage()), "Nonce darf nicht wiederverwendet werden")
    }

    @Test
    fun timeout_oder_eof_verweigert_fail_closed() {
        val approval = approval { emptyList() }

        assertFalse(approval.freigegeben(anfrage()))
    }

    @Test
    fun transportfehler_verweigert_fail_closed() {
        val approval = approval {
            error("transport unavailable")
        }

        assertFalse(approval.freigegeben(anfrage()))
    }

    @Test
    fun unbekannte_identitaet_verweigert_fail_closed() {
        val approval = approval { auftrag ->
            listOf(passendeAntwort(auftrag).copy(identitaet = "mallory"))
        }

        assertFalse(approval.freigegeben(anfrage()))
    }

    @Test
    fun falsche_nonce_verweigert_fail_closed() {
        val approval = approval { auftrag ->
            listOf(passendeAntwort(auftrag).copy(nonce = "andere-nonce"))
        }

        assertFalse(approval.freigegeben(anfrage()))
    }

    @Test
    fun kontext_digest_mismatch_verweigert_fail_closed() {
        val approval = approval { auftrag ->
            listOf(passendeAntwort(auftrag).copy(kontextDigest = "anderer-kontext"))
        }

        assertFalse(approval.freigegeben(anfrage()))
    }

    @Test
    fun falsche_bestaetigung_verweigert_fail_closed() {
        val approval = approval { auftrag ->
            listOf(passendeAntwort(auftrag).copy(bestaetigung = "ja"))
        }

        assertFalse(approval.freigegeben(anfrage()))
    }

    @Test
    fun doppelte_antwort_verweigert_fail_closed() {
        val approval = approval { auftrag ->
            listOf(passendeAntwort(auftrag), passendeAntwort(auftrag))
        }

        assertFalse(approval.freigegeben(anfrage()))
    }

    @Test
    fun wertgleiche_aktion_unter_anderem_belief_hat_anderen_digest() {
        val digest = RemoteApprovalKontextDigestBerechner()
        val sicher = digest.digest(anfrage(rest = 0.1))
        val unsicher = digest.digest(anfrage(rest = 0.2))

        assertNotEquals(sicher, unsicher)
    }

    @Test
    fun transportauftrag_enthaelt_serialisierte_anfrage() {
        val auftraege = mutableListOf<RemoteApprovalAuftrag>()
        val approval = approval { auftrag ->
            auftraege += auftrag
            listOf(passendeAntwort(auftrag))
        }

        assertTrue(approval.freigegeben(anfrage()))

        val auftrag = auftraege.single()
        assertEquals("nonce-remote", auftrag.nonce.wert)
        assertEquals("Deploy", auftrag.payload.aktion)
        assertEquals("EXTERN_WIRKSAM", auftrag.payload.wirkungsklasse)
        assertEquals(0.95, auftrag.payload.erfolgswahrscheinlichkeit)
        assertEquals(0.1, auftrag.payload.resthypothese)
    }
}
