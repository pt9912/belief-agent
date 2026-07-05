package dev.beliefagent.adapter.llm

import dev.beliefagent.application.belief.aktualisieren.BeliefAktualisieren
import dev.beliefagent.application.belief.aktualisieren.BeliefAktualisierenBefehl
import dev.beliefagent.application.belief.aktualisieren.ports.UhrPort
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Resthypothese
import dev.beliefagent.domain.belief.Zeitstempel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests des deterministischen Fake-LLM (LH-QA-03) und ein **E2E-naher** Lauf der
 * Pipeline mit dem echten Fake-Adapter (LH-FA-OBS-002) — dieses Adapter-Modul
 * sieht sowohl den Use-Case (application) als auch den Adapter.
 */
class FakeLlmTest {

    private fun prior() = BeliefState.of(
        listOf(Hypothese(HypotheseId("flaky"), 0.4), Hypothese(HypotheseId("regression"), 0.4)),
        Resthypothese(0.2),
    )

    @Test
    fun evidenz_die_hypothese_nennt_bekommt_hohe_likelihood() { // LH-QA-03
        val l = FakeLlm().likelihoods(
            Beobachtung(Quelle.LOG, Zeitstempel(1L), Evidenz("Ursache ist regression im Modul X")),
            prior(),
        )
        assertEquals(0.9, l.proHypothese[HypotheseId("regression")])
        assertEquals(0.1, l.proHypothese[HypotheseId("flaky")])
        assertEquals(0.1, l.resthypothese)
    }

    @Test
    fun deterministisch_gleiche_eingabe_gleiche_likelihoods() { // LH-QA-03
        val b = Beobachtung(Quelle.LOG, Zeitstempel(1L), Evidenz("regression"))
        assertEquals(FakeLlm().likelihoods(b, prior()), FakeLlm().likelihoods(b, prior()))
    }

    @Test
    fun e2e_pipeline_mit_fake_llm_verschiebt_belief_zur_genannten_hypothese() { // LH-FA-OBS-002
        val uhr = object : UhrPort { override fun jetzt() = Zeitstempel(42L) }
        val ergebnis = BeliefAktualisieren(FakeLlm(), uhr).ausfuehren(
            BeliefAktualisierenBefehl(
                prior(),
                listOf(Beobachtung(Quelle.LOG, Zeitstempel(42L), Evidenz("regression bestätigt"))),
            ),
        )
        val regression = ergebnis.belief.hypothesen.single { it.id.wert == "regression" }
        val flaky = ergebnis.belief.hypothesen.single { it.id.wert == "flaky" }
        assertTrue(regression.wahrscheinlichkeit > flaky.wahrscheinlichkeit) // Evidenz nennt regression
        assertEquals(3, ergebnis.ereignisse.size) // Initial-Snapshot + erfasst + aktualisiert
    }
}
