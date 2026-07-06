package dev.beliefagent.application.belief.beobachtungwaehlen

import dev.beliefagent.application.belief.beobachtungwaehlen.ports.BeobachtungsAuswahlPort
import dev.beliefagent.domain.voi.VoiKandidat
import dev.beliefagent.domain.voi.VoiSelektor

/**
 * Use-Case *beobachtung-waehlen* (`ARC-04`, `LH-FA-VOI-002`): wählt die informativste
 * nächste Beobachtung. Holt die Kandidaten über den [BeobachtungsAuswahlPort] und
 * wendet die reine Domänen-Regel [VoiSelektor] an (max Gewinn/Kosten über die
 * Top-2-Trennung).
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
 * **belief-abhängige** Zuschnitt der Kandidaten (welche Beobachtungen der Belief-
 * Zustand gerade nahelegt) externalisiert welle-05 ans LLM (`ADR-0001`); hier liefert
 * der Port eine feste Menge.
 */
class BeobachtungWaehlen(
    private val port: BeobachtungsAuswahlPort,
) {
    fun waehle(bereitsGewaehlt: Set<VoiKandidat> = emptySet()): VoiKandidat? =
        VoiSelektor.waehle(port.kandidaten().filterNot { it in bereitsGewaehlt })
}
