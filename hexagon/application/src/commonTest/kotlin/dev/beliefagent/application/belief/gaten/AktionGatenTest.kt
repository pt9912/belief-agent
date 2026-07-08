package dev.beliefagent.application.belief.gaten

import dev.beliefagent.application.belief.gaten.ports.ApprovalAnfrage
import dev.beliefagent.application.belief.gaten.ports.ApprovalAuditKontextDigestBerechner
import dev.beliefagent.application.belief.gaten.ports.ApprovalAuditSnapshot
import dev.beliefagent.application.belief.gaten.ports.ApprovalErgebnis
import dev.beliefagent.application.belief.gaten.ports.HumanApprovalPort
import dev.beliefagent.application.belief.aktualisieren.ports.UhrPort
import dev.beliefagent.application.ports.AuditPort
import dev.beliefagent.domain.belief.Aktion
import dev.beliefagent.domain.belief.ApprovalAngefragt
import dev.beliefagent.domain.belief.ApprovalErteilt
import dev.beliefagent.domain.belief.ApprovalVerweigert
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.Ereignis
import dev.beliefagent.domain.belief.EreignisProtokoll
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
import kotlin.test.assertTrue

/**
 * Deterministische Tests des aktion-gaten-Use-Cases (LH-FA-POL-004/006, LH-QA-03)
 * gegen In-Test-Approval-Fakes. Ergebnis ist die verbindliche [Aktionsfreigabe].
 */
class AktionGatenTest {

    private fun belief(rest: Double) =
        BeliefState.of(listOf(Hypothese(HypotheseId("A"), 1.0 - rest)), Resthypothese(rest))

    private fun aktion(klasse: Wirkungsklasse, erfolg: Double) = Aktion(
        "test", klasse, Erfolgswahrscheinlichkeit(erfolg),
        listOf(Beobachtung(Quelle.TEST, Zeitstempel(1L), Evidenz("e"))),
    )

    private fun approval(ja: Boolean) = object : HumanApprovalPort {
        override fun entscheide(anfrage: ApprovalAnfrage): ApprovalErgebnis =
            if (ja) {
                ApprovalErgebnis.freigegeben(snapshot(anfrage, "freigegeben"))
            } else {
                ApprovalErgebnis.verweigert(snapshot(anfrage, "verweigert"))
            }
    }

    private class RecordingApproval(private val ja: Boolean) : HumanApprovalPort {
        val anfragen = mutableListOf<ApprovalAnfrage>()

        override fun entscheide(anfrage: ApprovalAnfrage): ApprovalErgebnis {
            anfragen += anfrage
            return if (ja) {
                ApprovalErgebnis.freigegeben(snapshot(anfrage, "freigegeben"))
            } else {
                ApprovalErgebnis.verweigert(snapshot(anfrage, "verweigert"))
            }
        }
    }

    @Test
    fun gate_wird_nicht_umgangen_ablehnung_bleibt_ablehnung() { // LH-FA-POL-006
        // Niedrige Erfolgs-P -> Gate lehnt ab; aktion-gaten hebt das NICHT zur Freigabe an.
        val approval = RecordingApproval(ja = true)
        assertTrue(
            gaten(approval).pruefe(aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.1), belief(0.1)) is Aktionsfreigabe.Abgelehnt,
        )
        assertEquals(0, approval.anfragen.size)
    }

    @Test
    fun irreversibel_ohne_freigabe_wird_eskaliert() { // LH-FA-POL-004
        val e = gaten(approval(false)).pruefe(aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.95), belief(0.1))
        assertTrue(e is Aktionsfreigabe.Eskaliert && "LH-FA-POL-004" in e.grund)
    }

    @Test
    fun irreversibel_mit_freigabe_wird_freigegeben() { // LH-FA-POL-004
        assertTrue(
            gaten(approval(true)).pruefe(aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.95), belief(0.1)) is Aktionsfreigabe.Freigegeben,
        )
    }

    @Test
    fun approval_anfrage_enthaelt_aktion_und_aktuellen_belief() { // LH-FA-POL-004 / LH-FA-POL-006
        val approval = RecordingApproval(ja = true)
        val aktion = aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.95)
        val belief = belief(0.1)

        assertTrue(gaten(approval).pruefe(aktion, belief) is Aktionsfreigabe.Freigegeben)

        assertEquals(1, approval.anfragen.size)
        assertEquals(aktion, approval.anfragen.single().aktion)
        assertTrue(approval.anfragen.single().belief === belief)
        assertEquals(0.1, approval.anfragen.single().belief.resthypothese.wahrscheinlichkeit)
    }

    @Test
    fun reversible_freigabe_braucht_keine_menschliche_freigabe() {
        // repository-wirksam ist reversibel -> Freigegeben ohne Approval.
        assertTrue(
            gaten(approval(false)).pruefe(aktion(Wirkungsklasse.REPOSITORY_WIRKSAM, 0.9), belief(0.1)) is Aktionsfreigabe.Freigegeben,
        )
    }

    @Test
    fun gate_eskalation_bleibt_unabhaengig_von_freigabe() { // LH-FA-POL-005 dominiert
        // extern-wirksam + hohe Resthypothese -> Gate eskaliert; Approval ändert nichts.
        val approval = RecordingApproval(ja = true)
        assertTrue(
            gaten(approval).pruefe(aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.95), belief(0.6)) is Aktionsfreigabe.Eskaliert,
        )
        assertEquals(0, approval.anfragen.size)
    }

    @Test
    fun approval_audit_spur_wird_append_only_in_reihenfolge_geschrieben() {
        val audit = SpeichernderAuditPort()

        val ergebnis = gaten(approval(true), audit).pruefe(
            aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.95),
            belief(0.1),
        )

        assertTrue(ergebnis is Aktionsfreigabe.Freigegeben)
        assertEquals(2, audit.ereignisse.size)
        assertTrue(audit.ereignisse[0] is ApprovalAngefragt)
        assertTrue(audit.ereignisse[1] is ApprovalErteilt)
        assertEquals(1L, audit.ereignisse[0].zeitstempel.epochMillis)
        assertEquals(2L, audit.ereignisse[1].zeitstempel.epochMillis)
        assertEquals(audit.ereignisse, audit.lade().ereignisse)
    }

    @Test
    fun verweigerte_freigabe_bleibt_auditierbar() {
        val audit = SpeichernderAuditPort()

        val ergebnis = gaten(approval(false), audit).pruefe(
            aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.95),
            belief(0.1),
        )

        assertTrue(ergebnis is Aktionsfreigabe.Eskaliert)
        assertEquals(2, audit.ereignisse.size)
        assertTrue(audit.ereignisse[0] is ApprovalAngefragt)
        assertTrue(audit.ereignisse[1] is ApprovalVerweigert)
    }

    @Test
    fun audit_ausfall_fuer_extern_wirksame_aktion_bleibt_fail_closed() {
        val ergebnis = gaten(approval(true), FehlernderAuditPort()).pruefe(
            aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.95),
            belief(0.1),
        )

        assertTrue(ergebnis is Aktionsfreigabe.Eskaliert)
        assertTrue("Approval-Audit" in ergebnis.grund)
    }

    private fun gaten(
        approval: HumanApprovalPort,
        audit: AuditPort = SpeichernderAuditPort(),
    ): AktionGaten = AktionGaten(approval, audit, FakeUhr())

    private class SpeichernderAuditPort : AuditPort {
        val ereignisse = mutableListOf<Ereignis>()

        override fun anhaengen(ereignis: Ereignis) {
            ereignisse += ereignis
        }

        override fun lade(): EreignisProtokoll = EreignisProtokoll.von(ereignisse)
    }

    private class FehlernderAuditPort : AuditPort {
        override fun anhaengen(ereignis: Ereignis) {
            error("audit failed")
        }

        override fun lade(): EreignisProtokoll = EreignisProtokoll.LEER
    }

    private class FakeUhr : UhrPort {
        private var naechster = 1L

        override fun jetzt(): Zeitstempel = Zeitstempel(naechster++)
    }

    companion object {
        private val digestBerechner = ApprovalAuditKontextDigestBerechner()

        private fun snapshot(anfrage: ApprovalAnfrage, grund: String): ApprovalAuditSnapshot =
            ApprovalAuditSnapshot(
                anfrageKontextDigest = digestBerechner.digest(anfrage),
                kanal = "test",
                nonceReferenz = "test-nonce",
                antwortReferenz = "test-response",
                identitaetsReferenz = "test-operator",
                ergebnisGrund = grund,
            )
    }
}
