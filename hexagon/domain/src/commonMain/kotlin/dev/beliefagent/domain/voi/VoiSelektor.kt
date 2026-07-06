package dev.beliefagent.domain.voi

/**
 * VoI-Selektor als reine, **bewusst heuristische** Domänen-Regel (`ARC-04`, `LH-FA-VOI`):
 * wählt aus [VoiKandidat]en die informativste Beobachtung — die mit dem höchsten
 * erwarteten **Gewinn je Kosten** (`LH-FA-VOI-003`), wobei der Gewinn die erwartete
 * Diskriminierung der zwei wahrscheinlichsten Hypothesen ist (`LH-FA-VOI-002`).
 *
 * **Lokal/heuristisch (`LH-FA-VOI-004`, `LH-OUT-01`):** greedy je Zug über die
 * aktuellen Kandidaten-Schätzungen — **keine** global optimale Beobachtungs-Policy und
 * keine Vorausschau über mehrere Beobachtungen. Das ist bewusst so und genügt der
 * Anforderung.
 *
 * **Deterministisch (`LH-QA-03`):** bei gleichem Gewinn je Kosten entscheidet ein
 * fixierter Tie-Break, nie Zufall — zuerst die höhere **absolute** Diskriminierung
 * (bei gleicher Effizienz löst die Beobachtung mit dem größeren Einzelschritt die
 * Mehrdeutigkeit schneller auf), danach die stabile Eingabe-Reihenfolge (erster
 * Kandidat gewinnt).
 *
 * Der Effizienz-Vergleich läuft über **Kreuz-Multiplikation**
 * (`diskA·kostenB` vs. `diskB·kostenA`, Kosten `> 0` per [VoiKandidat]-Invariante)
 * statt über die Division [VoiKandidat.gewinnJeKosten]: kein Divisions-Rundungsfehler
 * (der den Tie-Break bei mathematisch gleicher Effizienz unterlaufen könnte) und kein
 * Overflow bei winzigen Kosten. [VoiKandidat.gewinnJeKosten] bleibt als lesbare Kennzahl.
 *
 * Rein rechnend und framework-frei (`ADR-0001`/`ADR-0003`): die Schätzung der
 * erwarteten Diskriminierung trägt der [VoiKandidat] (welle-04 Fake, welle-05 LLM),
 * nicht diese Regel.
 */
object VoiSelektor {

    // Größter Gewinn/Kosten via Kreuz-Multiplikation; bei Gleichstand größte absolute
    // Diskriminierung. maxWithOrNull liefert das Maximum und hält bei vollem
    // Gleichstand den zuerst gesehenen Kandidaten (stabile Eingabe-Reihenfolge).
    private val vergleich: Comparator<VoiKandidat> = Comparator { a, b ->
        val kreuz = (a.erwarteteDiskriminierung * b.kosten).compareTo(b.erwarteteDiskriminierung * a.kosten)
        if (kreuz != 0) kreuz else a.erwarteteDiskriminierung.compareTo(b.erwarteteDiskriminierung)
    }

    /**
     * Wählt den informativsten Kandidaten oder `null`, wenn keiner vorliegt —
     * erschöpfte/leere Kandidaten bedeuten „kein günstiger Zug" und sind ein Signal
     * an den Entscheidungszyklus/die Eskalation (slice-017, `LH-FA-ESK-001`), kein
     * Fehler dieser Regel.
     */
    fun waehle(kandidaten: List<VoiKandidat>): VoiKandidat? = kandidaten.maxWithOrNull(vergleich)
}
