package dev.beliefagent.adapter.observation

import dev.beliefagent.application.belief.aktualisieren.ports.BeobachtungsPort
import dev.beliefagent.domain.belief.Beobachtung

/**
 * Fake-Beobachtungsquelle (ARC-08): liefert eine feste, deterministische Liste
 * von [Beobachtung]en (LH-FA-OBS-001, LH-QA-03) — steht in welle-02 für echte
 * Quellen-Adapter (Test-/Build-Runner, Log-Scraper, …), die in welle-05 folgen.
 */
class FakeBeobachtungsQuelle(
    private val beobachtungen: List<Beobachtung>,
) : BeobachtungsPort {
    override fun lies(): List<Beobachtung> = beobachtungen
}
