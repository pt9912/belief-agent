package dev.beliefagent.application.belief.aktualisieren.ports

import dev.beliefagent.domain.belief.Beobachtung

/**
 * Beobachtungs-Port (ARC-07): liest Beobachtungen aus einer heterogenen Quelle
 * (LH-FA-OBS-001 — Test/Build/Log/Mensch/Repo) und speist die Update-Pipeline.
 * In welle-02 steht dahinter ein deterministischer Fake-Quelle-Adapter
 * (LH-QA-03); echte Quellen-Adapter folgen in welle-05. Use-case-lokaler Port,
 * Rolle `port`: importiert nur Domänentypen, nie einen Adapter.
 *
 * Der VoI-gesteuerte Auswahl-Aspekt (welche Beobachtung als Nächstes?) ist
 * Out-of-Scope (`beobachtung-waehlen`, welle-04) — hier nur das Lesen.
 */
interface BeobachtungsPort {
    fun lies(): List<Beobachtung>
}
