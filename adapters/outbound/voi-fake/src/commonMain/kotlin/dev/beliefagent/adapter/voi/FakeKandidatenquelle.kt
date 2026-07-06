package dev.beliefagent.adapter.voi

import dev.beliefagent.application.belief.beobachtungwaehlen.ports.BeobachtungsAuswahlPort
import dev.beliefagent.domain.voi.VoiKandidat

/**
 * Fake-Kandidatenquelle (`ARC-08`): liefert eine feste, deterministische Liste von
 * [VoiKandidat]en (`LH-FA-VOI-002`, `LH-QA-03`) — steht in welle-04 für die
 * LLM-gestützte Kandidaten-/Diskriminierungs-Schätzung, die in welle-05 folgt
 * (`ADR-0001`).
 */
class FakeKandidatenquelle(
    private val kandidaten: List<VoiKandidat>,
) : BeobachtungsAuswahlPort {
    override fun kandidaten(): List<VoiKandidat> = kandidaten
}
