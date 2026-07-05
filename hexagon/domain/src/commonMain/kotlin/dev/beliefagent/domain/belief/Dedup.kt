package dev.beliefagent.domain.belief

/**
 * Dedup korrelierter Beobachtungen (LH-FA-OBS-004): mehrere Beobachtungen mit
 * gleicher [Signatur] tragen dieselbe, NICHT unabhängige Evidenz und dürfen im
 * Belief-Update nicht doppelt zählen (Schutz gegen Scheingewissheit). Reine
 * Domänen-Regel, deterministisch (LH-QA-03), framework-frei (ADR-0001/0003);
 * das Update selbst (slice-008) reduziert die Beobachtungen zuerst hierüber.
 *
 * Korrelations-Kriterium (welle-02, bewusst einfach — komplexere
 * Korrelationsmodelle sind Out-of-Scope, Folge-Slice bei Bedarf): gleiche
 * [Quelle] **und** gleiche [Evidenz]. Der [Zeitstempel] gehört NICHT zur
 * Signatur — dieselbe Evidenz aus derselben Quelle zu anderer Zeit ist eine
 * Wiederholung, keine unabhängige Beobachtung.
 */
object Dedup {

    /**
     * Reduziert [beobachtungen] auf unabhängige Evidenz: je [Signatur] bleibt
     * die **erste** Beobachtung in Eingabereihenfolge erhalten, weitere mit
     * gleicher Signatur entfallen. Reihenfolge-stabil und allein durch die
     * Eingabe determiniert (LH-QA-03); die übergebene Liste bleibt unverändert.
     */
    fun unabhaengig(beobachtungen: List<Beobachtung>): List<Beobachtung> =
        beobachtungen.distinctBy(::signatur)

    /** Korrelations-Signatur einer [Beobachtung]: [Quelle] + [Evidenz]. */
    fun signatur(beobachtung: Beobachtung): Signatur =
        Signatur(beobachtung.quelle, beobachtung.evidenz)
}

/**
 * Signatur zur Korrelations-Erkennung (LH-FA-OBS-004): [Quelle] + [Evidenz].
 * Zwei Beobachtungen mit gleicher Signatur gelten als korreliert (dieselbe
 * Evidenz aus derselben Quelle) und zählen als **eine** unabhängige Beobachtung.
 */
data class Signatur(val quelle: Quelle, val evidenz: Evidenz)
