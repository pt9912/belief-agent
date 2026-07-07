package dev.beliefagent.application.belief.beobachtungwaehlen

import dev.beliefagent.application.belief.beobachtungwaehlen.ports.BeobachtungsAuswahlPort
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.voi.VoiKandidat
import dev.beliefagent.domain.voi.VoiSelektor

/**
 * Use-Case *beobachtung-waehlen* (`ARC-04`, `LH-FA-VOI-002`): wählt die informativste
 * nächste Beobachtung. Holt die Kandidaten für den aktuellen [BeliefState] über den
 * [BeobachtungsAuswahlPort] und wendet die reine Domänen-Regel [VoiSelektor] an
 * (max Gewinn/Kosten über die Top-2-Trennung).
 *
 * [bereitsGewaehlt] schließt bereits **verbrauchte** Kandidaten aus, damit der
 * Entscheidungszyklus (slice-017) über mehrere Runden **verschiedene** Beobachtungen
 * sammelt und nicht dieselbe wiederholt (sonst zählte ein Beweis mehrfach —
 * Scheingewissheit, `LH-FA-OBS-004`).
 *
 * Liefert die Wahl oder `null` — **„keine günstige Beobachtung"** (leere/erschöpfte
 * Kandidaten). Das ist das Signal, mit dem der Zyklus *sammeln* von *eskalieren*
 * unterscheidet (`LH-FA-ESK-001`), kein Fehler.
 *
 * Reine Orchestrierung Port → Domäne; die Auswahl-Logik lebt in der Domäne, nicht
 * hier. Deterministisch bei deterministischem Port (`LH-QA-03`), framework-frei. Der
 * **belief-abhängige** Zuschnitt der Kandidaten (welche Beobachtungen der aktuelle
 * Zustand gerade nahelegt) liegt hinter dem Port; LLM-beeinflusste Zahlen müssen dort
 * explizit strukturiert übergeben werden (`ADR-0001`, `LH-FA-LLM-003`).
 */
class BeobachtungWaehlen(
    private val port: BeobachtungsAuswahlPort,
) {
    fun waehle(belief: BeliefState, bereitsGewaehlt: Set<VoiKandidat> = emptySet()): VoiKandidat? =
        VoiSelektor.waehle(port.kandidaten(belief).filterNot { it in bereitsGewaehlt })
}
