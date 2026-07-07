package dev.beliefagent.adapter.cli

import dev.beliefagent.adapter.llmaction.FakeAktionsVorschlagKonfiguration
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.EvidenzReferenz
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Resthypothese
import dev.beliefagent.domain.belief.Wirkungsklasse
import dev.beliefagent.domain.belief.Zeitstempel
import dev.beliefagent.domain.eskalation.Budget
import dev.beliefagent.domain.voi.VoiKandidat

object StandardCliSzenarien {
    private val hypothese = HypotheseId("fake-hypothese-regression")
    private val evidenzRef = EvidenzReferenz("fake:evidenz:log-1")
    private val evidenz = Beobachtung(Quelle.TEST, Zeitstempel(1L), Evidenz("fake-hypothese-regression log"))

    fun gehandelt(): CliRuntimeKonfiguration =
        basis(
            szenario = "gehandelt",
            prior = belief(hypotheseP = 0.9, rest = 0.1),
            pSuccess = 0.8,
            wirkungsklasse = Wirkungsklasse.ARBEITSBEREICH_LOKAL,
            approval = false,
        )

    fun eskaliert(): CliRuntimeKonfiguration =
        basis(
            szenario = "eskaliert",
            prior = belief(hypotheseP = 0.9, rest = 0.1),
            pSuccess = 0.95,
            wirkungsklasse = Wirkungsklasse.EXTERN_WIRKSAM,
            approval = false,
        )

    fun abgelehnt(): CliRuntimeKonfiguration =
        basis(
            szenario = "abgelehnt",
            prior = belief(hypotheseP = 0.9, rest = 0.1),
            pSuccess = 0.3,
            wirkungsklasse = Wirkungsklasse.ARBEITSBEREICH_LOKAL,
            approval = false,
        )

    fun sammeltDannHandelt(): CliRuntimeKonfiguration =
        basis(
            szenario = "sammelt-dann-handelt",
            prior = belief(hypotheseP = 0.05, rest = 0.95),
            pSuccess = 0.95,
            wirkungsklasse = Wirkungsklasse.EXTERN_WIRKSAM,
            approval = true,
            budget = Budget(maxSchritte = 5),
            voiKandidaten = listOf(
                kandidat("fake-hypothese-regression signal 1", 0.5),
                kandidat("fake-hypothese-regression signal 2", 0.4),
                kandidat("fake-hypothese-regression signal 3", 0.3),
            ),
        )

    private fun basis(
        szenario: String,
        prior: BeliefState,
        pSuccess: Double,
        wirkungsklasse: Wirkungsklasse,
        approval: Boolean,
        budget: Budget = Budget(),
        voiKandidaten: List<VoiKandidat> = emptyList(),
    ) = CliRuntimeKonfiguration(
        prior = prior,
        budget = budget,
        approvalFreigegeben = approval,
        bekannteEvidenz = mapOf(evidenzRef to evidenz),
        voiKandidaten = voiKandidaten,
        szenario = szenario,
        aktionsVorschlaege = listOf(
            FakeAktionsVorschlagKonfiguration(
                beschreibung = "Fake-CLI-Aktion",
                hypotheseId = hypothese.wert,
                wirkungsklasse = wirkungsklasse.name,
                pSuccess = pSuccess,
                konfidenzReferenz = "fake:konfidenz:cli-aktion",
                stuetzendeEvidenz = listOf(evidenzRef.wert),
            ),
        ),
    )

    private fun belief(hypotheseP: Double, rest: Double): BeliefState =
        BeliefState.of(listOf(Hypothese(hypothese, hypotheseP)), Resthypothese(rest))

    private fun kandidat(text: String, diskriminierung: Double): VoiKandidat =
        VoiKandidat(Beobachtung(Quelle.TEST, Zeitstempel(1L), Evidenz(text)), diskriminierung, kosten = 1.0)
}
