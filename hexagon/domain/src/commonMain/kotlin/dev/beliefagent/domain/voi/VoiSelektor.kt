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
 * Rein rechnend und framework-frei (`ADR-0001`/`ADR-0003`): die Schätzung der
 * erwarteten Diskriminierung trägt der [VoiKandidat] (welle-04 Fake, welle-05 LLM),
 * nicht diese Regel.
 */
object VoiSelektor {

    /**
     * Wählt den informativsten Kandidaten oder `null`, wenn keiner vorliegt —
     * erschöpfte/leere Kandidaten bedeuten „kein günstiger Zug" und sind ein Signal
     * an den Entscheidungszyklus/die Eskalation (slice-016, `LH-FA-ESK-001`), kein
     * Fehler dieser Regel.
     */
    fun waehle(kandidaten: List<VoiKandidat>): VoiKandidat? =
        kandidaten.maxWithOrNull(
            // maxWithOrNull liefert das größte Element dieser Ordnung und hält bei
            // vollem Gleichstand den zuerst gesehenen Kandidaten (aktualisiert nur bei
            // echt größer) -> stabile Eingabe-Reihenfolge als letzter Tie-Break.
            // Beide Schlüssel aufsteigend: größter Gewinn/Kosten, bei Gleichstand
            // größte absolute Diskriminierung = das Maximum.
            compareBy<VoiKandidat> { it.gewinnJeKosten }.thenBy { it.erwarteteDiskriminierung },
        )
}
