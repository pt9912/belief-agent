package dev.beliefagent.domain.voi

import dev.beliefagent.domain.belief.Beobachtung

/**
 * Ein Kandidat für die nächste informationssammelnde Beobachtung (LH-FA-VOI-002):
 * die [beobachtung] selbst plus ihre **erwartete Diskriminierung** der zwei
 * wahrscheinlichsten Hypothesen und ihre [kosten].
 *
 * Die [erwarteteDiskriminierung] ist die geschätzte Trennschärfe *dieser* Beobachtung
 * für die beiden aktuell wahrscheinlichsten Hypothesen (erwarteter Zuwachs des
 * Top-2-Abstands, vgl. `BeliefState.top2Abstand`). Sie wird hier **nicht** berechnet:
 * in welle-04 liefert sie ein deterministischer Fake, welle-05 externalisiert sie ans
 * LLM (`ADR-0001`, `LH-FA-LLM`). Dadurch bleibt der [VoiSelektor] rein rechnend und
 * framework-frei (`ADR-0003`).
 *
 * **Invarianten (fail-closed, `MR-003`):**
 *  - [erwarteteDiskriminierung] endlich und `>= 0` — eine Beobachtung kann die
 *    Hypothesen nicht „negativ" trennen; `NaN` würde die Auswahl vergiften, weil
 *    `NaN` in Kotlins `Double`-Ordnung als größer-als-alles gilt und ein solcher
 *    Kandidat den [VoiSelektor] unverdient gewinnen ließe.
 *  - [kosten] endlich und **echt `> 0`** — [gewinnJeKosten] teilt durch die Kosten;
 *    eine kostenlose Beobachtung ist ein degenerierter Sonderfall (Division bzw.
 *    „unendlicher" Gewinn) und wird bewusst ausgeschlossen.
 */
data class VoiKandidat(
    val beobachtung: Beobachtung,
    val erwarteteDiskriminierung: Double,
    val kosten: Double,
) {
    init {
        require(erwarteteDiskriminierung.isFinite() && erwarteteDiskriminierung >= 0.0) {
            "erwartete Diskriminierung muss endlich und >= 0 sein: $erwarteteDiskriminierung"
        }
        require(kosten.isFinite() && kosten > 0.0) {
            "Kosten müssen endlich und > 0 sein: $kosten"
        }
    }

    /**
     * Erwarteter Informationsgewinn **je Kosten** (`LH-FA-VOI-003`) — das
     * Auswahlkriterium des [VoiSelektor]s. Nicht der rohe Gewinn: eine teure,
     * stark trennende Beobachtung kann schlechter sein als eine günstige, schwächere.
     */
    val gewinnJeKosten: Double get() = erwarteteDiskriminierung / kosten
}
