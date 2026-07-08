package dev.beliefagent.adapter.approval

import dev.beliefagent.application.belief.gaten.AktionGaten
import dev.beliefagent.application.belief.gaten.Aktionsfreigabe
import dev.beliefagent.application.belief.gaten.ports.ApprovalAnfrage
import dev.beliefagent.application.belief.aktualisieren.ports.UhrPort
import dev.beliefagent.application.ports.AuditPort
import dev.beliefagent.domain.belief.Aktion
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

    private fun anfrage() = ApprovalAnfrage(aktion(0.9), belief(0.1))

    @Test
    fun default_verweigert_freigabe() { // fail-safe
        assertFalse(FakeApproval().entscheide(anfrage()).istFreigegeben())
    }

    @Test
    fun konfiguriert_freigegeben() {
        assertTrue(FakeApproval(freigabe = true).entscheide(anfrage()).istFreigegeben())
    }

    @Test
    fun e2e_extern_wirksam_nur_mit_freigabe_frei() { // LH-FA-POL-004/006 (E2E gegen echten Fake-Adapter)
        val aktion = aktion(0.95)
        val belief = belief(0.1)
        assertTrue(gaten(FakeApproval(freigabe = false)).pruefe(aktion, belief) is Aktionsfreigabe.Eskaliert)
        assertTrue(gaten(FakeApproval(freigabe = true)).pruefe(aktion, belief) is Aktionsfreigabe.Freigegeben)
    }

    private fun gaten(approval: FakeApproval): AktionGaten =
        AktionGaten(approval, SpeichernderAuditPort(), FakeUhr())

    private class SpeichernderAuditPort : AuditPort {
        private var protokoll = EreignisProtokoll.LEER

        override fun anhaengen(ereignis: Ereignis) {
            protokoll = protokoll.append(ereignis)
        }

        override fun lade(): EreignisProtokoll = protokoll
    }

    private class FakeUhr : UhrPort {
        private var naechster = 1L

        override fun jetzt(): Zeitstempel = Zeitstempel(naechster++)
    }
}
