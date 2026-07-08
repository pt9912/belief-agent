package dev.beliefagent.adapter.approvallocal

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

class LocalApprovalTest {

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
        nonce: String = "nonce-1",
        store: InMemoryApprovalNonceStore = InMemoryApprovalNonceStore(),
        antwort: (ApprovalChallenge) -> ApprovalAntwort?,
    ) = LocalApproval(
        nonceQuelle = ApprovalNonceQuelle { ApprovalNonce(nonce) },
        eingabe = ApprovalEingabe(antwort),
        nonceStore = store,
    )

    private fun passendeAntwort(challenge: ApprovalChallenge) = ApprovalAntwort(
        nonce = challenge.nonce.wert,
        identitaet = "alice",
        kontextDigest = challenge.kontextDigest.wert,
        bestaetigung = LocalApproval.BESTAETIGUNG,
    )

    @Test
    fun passende_eingabe_gibt_genau_eine_anfrage_frei() { // LH-FA-POL-004
        val store = InMemoryApprovalNonceStore()
        val approval = approval(store = store) { passendeAntwort(it) }

        val ergebnis = approval.entscheide(anfrage())
        assertTrue(ergebnis.istFreigegeben())
        assertEquals(LocalApproval.KANAL, ergebnis.audit.kanal)
        assertEquals("alice", ergebnis.audit.identitaetsReferenz)
        assertFalse(approval.entscheide(anfrage()).istFreigegeben(), "Nonce darf nicht wiederverwendet werden")
    }

    @Test
    fun falsche_nonce_verweigert_fail_closed() {
        val approval = approval { challenge ->
            passendeAntwort(challenge).copy(nonce = "andere-nonce")
        }

        assertFalse(approval.entscheide(anfrage()).istFreigegeben())
    }

    @Test
    fun fehlende_identitaet_verweigert_fail_closed() {
        val approval = approval { challenge ->
            passendeAntwort(challenge).copy(identitaet = " ")
        }

        assertFalse(approval.entscheide(anfrage()).istFreigegeben())
    }

    @Test
    fun kontext_digest_mismatch_verweigert_fail_closed() {
        val approval = approval { challenge ->
            passendeAntwort(challenge).copy(kontextDigest = "anderer-kontext")
        }

        assertFalse(approval.entscheide(anfrage()).istFreigegeben())
    }

    @Test
    fun eof_oder_abbruch_verweigert_fail_closed() {
        val approval = approval { null }

        assertFalse(approval.entscheide(anfrage()).istFreigegeben())
    }

    @Test
    fun falsche_bestaetigung_verweigert_fail_closed() {
        val approval = approval { challenge ->
            passendeAntwort(challenge).copy(bestaetigung = "ja")
        }

        assertFalse(approval.entscheide(anfrage()).istFreigegeben())
    }

    @Test
    fun wertgleiche_aktion_unter_anderem_belief_hat_anderen_digest() {
        val digest = ApprovalKontextDigestBerechner()
        val sicher = digest.digest(anfrage(rest = 0.1))
        val unsicher = digest.digest(anfrage(rest = 0.2))

        assertNotEquals(sicher, unsicher)
    }

    @Test
    fun ausgabe_rendert_nonce_digest_und_kontext() {
        val challenges = mutableListOf<ApprovalChallenge>()
        val approval = LocalApproval(
            nonceQuelle = ApprovalNonceQuelle { ApprovalNonce("nonce-1") },
            eingabe = ApprovalEingabe { passendeAntwort(it) },
            ausgabe = ApprovalAusgabe { challenges += it },
        )

        assertTrue(approval.entscheide(anfrage()).istFreigegeben())

        val challenge = challenges.single()
        assertEquals("nonce-1", challenge.nonce.wert)
        assertTrue("context_digest=${challenge.kontextDigest.wert}" in challenge.text)
        assertTrue("action=Deploy" in challenge.text)
        assertTrue("resthypothese=0.1" in challenge.text)
    }
}
