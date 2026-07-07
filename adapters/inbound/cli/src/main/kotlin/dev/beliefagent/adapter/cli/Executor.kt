package dev.beliefagent.adapter.cli

import dev.beliefagent.application.belief.entscheidungszyklus.Zyklusergebnis
import dev.beliefagent.domain.belief.Aktion

interface AktionsAusfuehrungsAdapter {
    fun ausfuehren(aktion: Aktion)
}

class RecordingAktionsAusfuehrungsAdapter : AktionsAusfuehrungsAdapter {
    private val ausgefuehrte = mutableListOf<Aktion>()

    override fun ausfuehren(aktion: Aktion) {
        ausgefuehrte += aktion
    }

    fun ausgefuehrteAktionen(): List<Aktion> = ausgefuehrte.toList()
}

data class ExecutorErgebnis(
    val ausgefuehrt: Boolean,
    val terminal: CliTerminal,
)

/**
 * Ausfuehrungsgrenze fuer `LH-FA-POL-006`/`LH-OUT-04`.
 *
 * Der Executor akzeptiert keinen Roh-`Aktion`-Pfad. Ausgefuehrt wird nur die
 * Aktion, die in `Zyklusergebnis.Gehandelt.freigabe` steckt; alle anderen
 * terminalen Ergebnisse bleiben fail-closed.
 */
class CliExecutor(
    private val ausfuehrung: AktionsAusfuehrungsAdapter,
) {
    fun verarbeite(ergebnis: Zyklusergebnis): ExecutorErgebnis = when (ergebnis) {
        is Zyklusergebnis.Gehandelt -> {
            ausfuehrung.ausfuehren(ergebnis.freigabe.aktion)
            ExecutorErgebnis(ausgefuehrt = true, terminal = CliTerminal.GEHANDELT)
        }
        is Zyklusergebnis.Eskaliert -> ExecutorErgebnis(ausgefuehrt = false, terminal = CliTerminal.ESKALIERT)
        is Zyklusergebnis.Abgelehnt -> ExecutorErgebnis(ausgefuehrt = false, terminal = CliTerminal.ABGELEHNT)
    }
}
