package dev.beliefagent.adapter.audit

import dev.beliefagent.adapter.llm.FakeLlm
import dev.beliefagent.adapter.observation.FakeBeobachtungsQuelle
import dev.beliefagent.application.belief.aktualisieren.BeliefAktualisieren
import dev.beliefagent.application.belief.aktualisieren.BeliefAktualisierenBefehl
import dev.beliefagent.application.belief.aktualisieren.ports.UhrPort
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Rekonstruktion
import dev.beliefagent.domain.belief.Resthypothese
import dev.beliefagent.domain.belief.Zeitstempel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * E2E-Spur für den Welle-02-Closure-Trigger (LH-FA-OBS-001/002, LH-FA-AUD-002):
 * **Quelle → Update → Protokoll → Persistenz → Rekonstruktion**, deterministisch
 * (LH-QA-03). Verdrahtet die realen Fake-Adapter (`FakeBeobachtungsQuelle`,
 * `FakeLlm`, `MemoryAudit`) — was in Produktion die cli-Composition-Root
 * (welle-03) übernimmt; hier im Test, da Testcode arch-check-befreit ist.
 */
class E2eTest {

    private fun uhr(t: Long) = object : UhrPort { override fun jetzt() = Zeitstempel(t) }

    @Test
    fun beobachtung_zu_update_protokoll_und_rekonstruktion() {
        val prior = BeliefState.of(
            listOf(Hypothese(HypotheseId("regression"), 0.4), Hypothese(HypotheseId("flaky"), 0.4)),
            Resthypothese(0.2),
        )
        val quelle = FakeBeobachtungsQuelle(
            listOf(Beobachtung(Quelle.LOG, Zeitstempel(10L), Evidenz("regression bestätigt"))),
        )
        val audit = MemoryAudit()

        // Orchestrierung (in Produktion die cli-Composition-Root, welle-03):
        val ergebnis = BeliefAktualisieren(FakeLlm(), uhr(10L))
            .ausfuehren(BeliefAktualisierenBefehl(prior, quelle.lies()))
        ergebnis.ereignisse.forEach(audit::anhaengen)

        // Belief aus dem PERSISTIERTEN Protokoll rekonstruieren (LH-FA-AUD-002):
        val rekonstruiert = Rekonstruktion.endBelief(audit.lade())!!

        assertTrue(audit.lade().groesse >= 2) // erfasst + aktualisiert (LH-FA-AUD-001)
        assertEquals( // Rekonstruktion == Live-Posterior (driftfrei)
            ergebnis.belief.hypothesen.map { it.id to it.wahrscheinlichkeit },
            rekonstruiert.hypothesen.map { it.id to it.wahrscheinlichkeit },
        )
        assertTrue( // Evidenz nennt regression -> regression überwiegt (LH-FA-OBS-002)
            rekonstruiert.hypothesen.single { it.id.wert == "regression" }.wahrscheinlichkeit >
                rekonstruiert.hypothesen.single { it.id.wert == "flaky" }.wahrscheinlichkeit,
        )
    }
}
