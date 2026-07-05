package dev.beliefagent.application.belief.gaten

import dev.beliefagent.application.belief.gaten.ports.HumanApprovalPort
import dev.beliefagent.domain.belief.Aktion
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.Erfolgswahrscheinlichkeit
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.GateEntscheidung
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Resthypothese
import dev.beliefagent.domain.belief.Wirkungsklasse
import dev.beliefagent.domain.belief.Zeitstempel
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Deterministische Tests des aktion-gaten-Use-Cases (LH-FA-POL-004/006, LH-QA-03)
 * gegen In-Test-Approval-Fakes.
 */
class AktionGatenTest {

    private fun belief(rest: Double) =
        BeliefState.of(listOf(Hypothese(HypotheseId("A"), 1.0 - rest)), Resthypothese(rest))

    private fun aktion(klasse: Wirkungsklasse, erfolg: Double) = Aktion(
        "test", klasse, Erfolgswahrscheinlichkeit(erfolg),
        listOf(Beobachtung(Quelle.TEST, Zeitstempel(1L), Evidenz("e"))),
    )

    private fun approval(ja: Boolean) = object : HumanApprovalPort {
        override fun freigegeben(aktion: Aktion) = ja
    }

    @Test
    fun gate_wird_nicht_umgangen_ablehnung_bleibt_ablehnung() { // LH-FA-POL-006
        // Niedrige Erfolgs-P -> Gate lehnt ab; aktion-gaten hebt das NICHT zur Freigabe an.
        assertTrue(
            AktionGaten(approval(true)).pruefe(aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.1), belief(0.1)) is GateEntscheidung.Ablehnung,
        )
    }

    @Test
    fun extern_wirksam_ohne_freigabe_wird_eskaliert() { // LH-FA-POL-004
        val e = AktionGaten(approval(false)).pruefe(aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.95), belief(0.1))
        assertTrue(e is GateEntscheidung.Eskalation && "LH-FA-POL-004" in e.grund)
    }

    @Test
    fun extern_wirksam_mit_freigabe_wird_freigegeben() { // LH-FA-POL-004
        assertTrue(
            AktionGaten(approval(true)).pruefe(aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.95), belief(0.1)) is GateEntscheidung.Freigabe,
        )
    }

    @Test
    fun reversible_freigabe_braucht_keine_menschliche_freigabe() {
        // repository-wirksam ist reversibel -> Freigabe ohne Approval.
        assertTrue(
            AktionGaten(approval(false)).pruefe(aktion(Wirkungsklasse.REPOSITORY_WIRKSAM, 0.9), belief(0.1)) is GateEntscheidung.Freigabe,
        )
    }

    @Test
    fun gate_eskalation_bleibt_unabhaengig_von_freigabe() { // LH-FA-POL-005 dominiert
        // extern-wirksam + hohe Resthypothese -> Gate eskaliert; Approval ändert nichts.
        assertTrue(
            AktionGaten(approval(true)).pruefe(aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.95), belief(0.6)) is GateEntscheidung.Eskalation,
        )
    }
}
