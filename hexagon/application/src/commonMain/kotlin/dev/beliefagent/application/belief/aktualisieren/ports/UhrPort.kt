package dev.beliefagent.application.belief.aktualisieren.ports

import dev.beliefagent.domain.belief.Zeitstempel

/**
 * Uhr-Port (ARC-07): liefert den aktuellen [Zeitstempel] als reinen Wert — die
 * Zeitquelle liegt außerhalb des Kerns (kein `Clock.systemUTC()` in Domäne oder
 * Application), damit Läufe deterministisch replaybar sind (LH-QA-03). In Tests
 * eine Fake-Uhr; ein System-Uhr-Adapter folgt bei Bedarf.
 *
 * **Vertrag — monoton nicht-fallend:** aufeinanderfolgende [jetzt]-Aufrufe müssen
 * nicht-fallende Zeitstempel liefern. Das append-only `EreignisProtokoll`
 * (slice-007) weist Rück-Datieren zurück; eine rückwärts laufende Uhr (z. B.
 * Wanduhr bei NTP-Korrektur/Leap-Second) würde die Pipeline beim Persistieren
 * abreißen. Ein System-Uhr-Adapter muss daher eine **monotone** Quelle nutzen
 * (bzw. das Maximum mit dem zuletzt gelieferten Wert bilden).
 */
interface UhrPort {
    fun jetzt(): Zeitstempel
}
