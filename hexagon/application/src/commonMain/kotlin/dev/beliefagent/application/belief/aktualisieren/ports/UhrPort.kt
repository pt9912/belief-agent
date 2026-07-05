package dev.beliefagent.application.belief.aktualisieren.ports

import dev.beliefagent.domain.belief.Zeitstempel

/**
 * Uhr-Port (ARC-07): liefert den aktuellen [Zeitstempel] als reinen Wert — die
 * Zeitquelle liegt außerhalb des Kerns (kein `Clock.systemUTC()` in Domäne oder
 * Application), damit Läufe deterministisch replaybar sind (LH-QA-03). In Tests
 * eine Fake-Uhr; ein System-Uhr-Adapter folgt bei Bedarf.
 */
interface UhrPort {
    fun jetzt(): Zeitstempel
}
