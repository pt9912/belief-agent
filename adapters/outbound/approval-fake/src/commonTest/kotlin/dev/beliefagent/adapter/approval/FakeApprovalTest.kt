package dev.beliefagent.adapter.approval

import dev.beliefagent.application.belief.gaten.AktionGaten
import dev.beliefagent.application.belief.gaten.Aktionsfreigabe
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests des Fake-Approval-Adapters (LH-QA-03) + ein E2E-naher Lauf der
 * Gate-Kette mit dem echten Fake-Adapter (LH-FA-POL-004/006).
 */
class FakeApprovalTest {

    private fun aktion(erfolg: Double) = Aktion(
        "Deploy", Wirkungsklasse.EXTERN_WIRKSAM, Erfolgswahrscheinlichkeit(erfolg),
        listOf(Beobachtung(Quelle.TEST, Zeitstempel(1L), Evidenz("e"))),
    )

    private fun belief(rest: Double) =
        BeliefState.of(listOf(Hypothese(HypotheseId("A"), 1.0 - rest)), Resthypothese(rest))

    @Test
    fun default_verweigert_freigabe() { // fail-safe
        assertFalse(FakeApproval().freigegeben(aktion(0.9)))
    }

    @Test
    fun konfiguriert_freigegeben() {
        assertTrue(FakeApproval(freigabe = true).freigegeben(aktion(0.9)))
    }

    @Test
    fun e2e_extern_wirksam_nur_mit_freigabe_frei() { // LH-FA-POL-004/006 (E2E gegen echten Fake-Adapter)
        val aktion = aktion(0.95)
        val belief = belief(0.1)
        assertTrue(AktionGaten(FakeApproval(freigabe = false)).pruefe(aktion, belief) is Aktionsfreigabe.Eskaliert)
        assertTrue(AktionGaten(FakeApproval(freigabe = true)).pruefe(aktion, belief) is Aktionsfreigabe.Freigegeben)
    }
}
