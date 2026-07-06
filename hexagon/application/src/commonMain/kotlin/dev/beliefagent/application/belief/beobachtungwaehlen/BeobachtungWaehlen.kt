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
 * Liefert die Wahl oder `null` — **„keine günstige Beobachtung"** (leere/erschöpfte
 * Kandidaten). Das ist das Signal, mit dem der Entscheidungszyklus (slice-017)
 * *sammeln* von *eskalieren* unterscheidet (`LH-FA-ESK-001`), kein Fehler.
 *
 * Reine Orchestrierung Port → Domäne; die Auswahl-Logik lebt in der Domäne, nicht
 * hier. Deterministisch bei deterministischem Port (`LH-QA-03`), framework-frei.
 */
class BeobachtungWaehlen(
    private val port: BeobachtungsAuswahlPort,
) {
    fun waehle(): VoiKandidat? = VoiSelektor.waehle(port.kandidaten())
}
