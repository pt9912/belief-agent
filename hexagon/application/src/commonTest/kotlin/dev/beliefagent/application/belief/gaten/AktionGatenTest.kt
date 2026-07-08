package dev.beliefagent.application.belief.gaten

import dev.beliefagent.application.belief.gaten.ports.ApprovalAnfrage
import dev.beliefagent.application.belief.gaten.ports.HumanApprovalPort
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
        override fun freigegeben(anfrage: ApprovalAnfrage) = ja
    }

    private class RecordingApproval(private val ja: Boolean) : HumanApprovalPort {
        val anfragen = mutableListOf<ApprovalAnfrage>()

        override fun freigegeben(anfrage: ApprovalAnfrage): Boolean {
            anfragen += anfrage
            return ja
        }
    }

    @Test
    fun gate_wird_nicht_umgangen_ablehnung_bleibt_ablehnung() { // LH-FA-POL-006
        // Niedrige Erfolgs-P -> Gate lehnt ab; aktion-gaten hebt das NICHT zur Freigabe an.
        val approval = RecordingApproval(ja = true)
        assertTrue(
            AktionGaten(approval).pruefe(aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.1), belief(0.1)) is Aktionsfreigabe.Abgelehnt,
        )
        assertEquals(0, approval.anfragen.size)
    }

    @Test
    fun irreversibel_ohne_freigabe_wird_eskaliert() { // LH-FA-POL-004
        val e = AktionGaten(approval(false)).pruefe(aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.95), belief(0.1))
        assertTrue(e is Aktionsfreigabe.Eskaliert && "LH-FA-POL-004" in e.grund)
    }

    @Test
    fun irreversibel_mit_freigabe_wird_freigegeben() { // LH-FA-POL-004
        assertTrue(
            AktionGaten(approval(true)).pruefe(aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.95), belief(0.1)) is Aktionsfreigabe.Freigegeben,
        )
    }

    @Test
    fun approval_anfrage_enthaelt_aktion_und_aktuellen_belief() { // LH-FA-POL-004 / LH-FA-POL-006
        val approval = RecordingApproval(ja = true)
        val aktion = aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.95)
        val belief = belief(0.1)

        assertTrue(AktionGaten(approval).pruefe(aktion, belief) is Aktionsfreigabe.Freigegeben)

        assertEquals(1, approval.anfragen.size)
        assertEquals(aktion, approval.anfragen.single().aktion)
        assertTrue(approval.anfragen.single().belief === belief)
        assertEquals(0.1, approval.anfragen.single().belief.resthypothese.wahrscheinlichkeit)
    }

    @Test
    fun reversible_freigabe_braucht_keine_menschliche_freigabe() {
        // repository-wirksam ist reversibel -> Freigegeben ohne Approval.
        assertTrue(
            AktionGaten(approval(false)).pruefe(aktion(Wirkungsklasse.REPOSITORY_WIRKSAM, 0.9), belief(0.1)) is Aktionsfreigabe.Freigegeben,
        )
    }

    @Test
    fun gate_eskalation_bleibt_unabhaengig_von_freigabe() { // LH-FA-POL-005 dominiert
        // extern-wirksam + hohe Resthypothese -> Gate eskaliert; Approval ändert nichts.
        val approval = RecordingApproval(ja = true)
        assertTrue(
            AktionGaten(approval).pruefe(aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.95), belief(0.6)) is Aktionsfreigabe.Eskaliert,
        )
        assertEquals(0, approval.anfragen.size)
    }
}
