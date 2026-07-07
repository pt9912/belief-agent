package dev.beliefagent.adapter.llmhypothesen

import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.EvidenzReferenz
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.KandidatenScore
import dev.beliefagent.domain.belief.Resthypothese
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** LH-FA-LLM-002/003, LH-QA-03: deterministischer Fake fuer Hypothesen-Kandidaten. */
class FakeHypothesenPortTest {

    private fun belief() = BeliefState.of(
        listOf(Hypothese(HypotheseId("bekannt"), 0.6)),
        Resthypothese(0.4),
    )

    @Test
    fun liefert_deterministisch_konfigurierte_kandidaten() {
        val port = FakeHypothesenPort(
            listOf(
                FakeHypotheseKonfiguration(
                    id = "fake-hypothese-cache-drift",
                    score = 0.4,
                    evidenzReferenzen = listOf("fake:evidenz:log-1", "fake:evidenz:trace-2"),
                ),
            ),
        )

        val ersterLauf = port.kandidaten(belief())
        val zweiterLauf = port.kandidaten(belief())

        assertEquals(ersterLauf, zweiterLauf)
        assertEquals(HypotheseId("fake-hypothese-cache-drift"), ersterLauf.single().id)
        assertEquals(KandidatenScore(0.4), ersterLauf.single().score)
        assertEquals(
            listOf(EvidenzReferenz("fake:evidenz:log-1"), EvidenzReferenz("fake:evidenz:trace-2")),
            ersterLauf.single().stuetzendeEvidenz,
        )
    }

    @Test
    fun default_kandidaten_sind_klar_als_fake_erkennbar() {
        val kandidaten = FakeHypothesenPort().kandidaten(belief())

        assertTrue(kandidaten.isNotEmpty())
        assertTrue(kandidaten.all { it.id.wert.startsWith("fake-") })
        assertTrue(kandidaten.flatMap { it.stuetzendeEvidenz }.all { it.wert.startsWith("fake:") })
    }

    @Test
    fun leere_konfiguration_liefert_leere_kandidatenliste() {
        val port = FakeHypothesenPort(emptyList())

        assertEquals(emptyList(), port.kandidaten(belief()))
    }

    @Test
    fun ungueltiger_score_liefert_fail_safe_leere_liste() {
        val port = FakeHypothesenPort(
            listOf(FakeHypotheseKonfiguration("fake-hypothese-x", Double.NaN, listOf("fake:evidenz:x"))),
        )

        assertEquals(emptyList(), port.kandidaten(belief()))
    }

    @Test
    fun fehlende_evidenz_liefert_fail_safe_leere_liste() {
        val port = FakeHypothesenPort(
            listOf(FakeHypotheseKonfiguration("fake-hypothese-x", 0.2, emptyList())),
        )

        assertEquals(emptyList(), port.kandidaten(belief()))
    }

    @Test
    fun leere_hypothesen_id_liefert_fail_safe_leere_liste() {
        val port = FakeHypothesenPort(
            listOf(FakeHypotheseKonfiguration("", 0.2, listOf("fake:evidenz:x"))),
        )

        assertEquals(emptyList(), port.kandidaten(belief()))
    }

    @Test
    fun nicht_als_fake_markierte_konfiguration_liefert_fail_safe_leere_liste() {
        val port = FakeHypothesenPort(
            listOf(FakeHypotheseKonfiguration("echte-hypothese", 0.2, listOf("evidenz:x"))),
        )

        assertEquals(emptyList(), port.kandidaten(belief()))
    }
}
