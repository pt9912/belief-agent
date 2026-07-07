package dev.beliefagent.adapter.voi

import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Resthypothese
import dev.beliefagent.domain.belief.Zeitstempel
import dev.beliefagent.domain.voi.VoiKandidat
import kotlin.test.Test
import kotlin.test.assertEquals

/** LH-FA-VOI-002 / LH-QA-03: die Fake-Kandidatenquelle liefert ihre Kandidaten deterministisch. */
class FakeKandidatenquelleTest {

    private val regression = HypotheseId("regression")
    private val flaky = HypotheseId("flaky")
    private val config = HypotheseId("config")

    private fun belief(
        regressionP: Double,
        flakyP: Double,
        configP: Double,
        rest: Double,
    ) = BeliefState.of(
        listOf(
            Hypothese(regression, regressionP),
            Hypothese(flaky, flakyP),
            Hypothese(config, configP),
        ),
        Resthypothese(rest),
    )

    private fun kandidat(text: String, diskriminierung: Double = 0.5) = VoiKandidat(
        Beobachtung(Quelle.TEST, Zeitstempel(1L), Evidenz(text)),
        erwarteteDiskriminierung = diskriminierung,
        kosten = 1.0,
    )

    private fun restOnlyBelief() = BeliefState.of(emptyList(), Resthypothese(1.0))

    @Test
    fun liefert_die_konfigurierten_kandidaten() {
        val kandidaten = listOf(kandidat("a", 0.5), kandidat("b", 0.3))
        val quelle = FakeKandidatenquelle(kandidaten)

        assertEquals(kandidaten, quelle.kandidaten(belief(0.5, 0.3, 0.1, 0.1)))
        assertEquals(kandidaten, quelle.kandidaten(belief(0.1, 0.5, 0.3, 0.1)))
        assertEquals(kandidaten, quelle.kandidaten(restOnlyBelief()))
    }

    @Test
    fun liefert_belief_abhaengige_kandidaten_nach_top2_hypothesen() { // F4b / LH-FA-VOI-002
        val regressionKandidaten = listOf(kandidat("regression log pruefen", 0.7))
        val flakyKandidaten = listOf(kandidat("flaky test history pruefen", 0.6))
        val quelle = FakeKandidatenquelle(
            fallback = emptyList(),
            kandidatenNachTop2 = mapOf(
                TopHypothesenSchluessel(regression, flaky) to regressionKandidaten,
                TopHypothesenSchluessel(flaky, regression) to flakyKandidaten,
            ),
        )

        assertEquals(regressionKandidaten, quelle.kandidaten(belief(0.5, 0.3, 0.1, 0.1)))
        assertEquals(flakyKandidaten, quelle.kandidaten(belief(0.3, 0.5, 0.1, 0.1)))
    }

    @Test
    fun liefert_fallback_wenn_top2_nicht_konfiguriert_sind() { // negative Kandidaten / leerer Pfad
        val fallback = listOf(kandidat("generische repo-inspektion", 0.2))
        val quelle = FakeKandidatenquelle(
            fallback = fallback,
            kandidatenNachTop2 = mapOf(
                TopHypothesenSchluessel(regression, flaky) to listOf(kandidat("regression log pruefen", 0.7)),
            ),
        )

        assertEquals(fallback, quelle.kandidaten(belief(0.1, 0.3, 0.5, 0.1)))
    }

    @Test
    fun kann_erschoepfte_kandidaten_deterministisch_abbilden() { // leerer Kandidatenpfad
        val quelle = FakeKandidatenquelle(
            fallback = emptyList(),
            kandidatenNachTop2 = emptyMap(),
        )

        assertEquals(emptyList(), quelle.kandidaten(belief(0.5, 0.3, 0.1, 0.1)))
    }
}
