package dev.beliefagent.domain.belief

/**
 * Rekonstruktion des Belief State aus einem [EreignisProtokoll] durch Replay
 * (LH-FA-AUD-002): der Zustand zu einem Zeitpunkt ergibt sich aus dem Abspielen
 * der Ereignisse, nicht aus verstecktem Reasoning (LH-FA-AUD-003). Reine
 * Domänen-Regel, deterministisch (LH-QA-03), framework-frei (ADR-0001/0003).
 *
 * In welle-02 trägt allein [BeliefAktualisiert] den Belief-Zustand (Snapshot des
 * Live-Updates via [BayesUpdate]); andere Ereignisarten verschieben den Belief
 * nicht. Der Replay ist **driftfrei**, weil er den bereits protokollierten
 * Update-Ausgang wiedergibt statt neu herzuleiten. Das erneute Herleiten aus
 * Beobachtungen (Likelihoods über den LLM-Port) ist Out-of-Scope (slice-008 /
 * welle-05).
 */
object Rekonstruktion {

    /**
     * Belief State nach Replay **aller** Ereignisse; `null`, falls das Protokoll
     * noch keinen Belief etabliert hat (kein [BeliefAktualisiert]).
     */
    fun endBelief(protokoll: EreignisProtokoll): BeliefState? =
        falte(protokoll.ereignisse)

    /**
     * Belief State nach Replay aller Ereignisse **bis einschließlich** [bis];
     * `null`, falls bis dahin kein Belief etabliert wurde. Erlaubt die
     * Rekonstruktion eines vergangenen Zustands (LH-FA-AUD-002).
     *
     * Tie-Break bei **gleichem** Zeitstempel: es zählt der **letzte** Belief mit
     * `zeitstempel ≤ bis` (mehrere Updates teilen oft denselben `epochMillis`).
     * Für sequenz-genaue Zwischenzustände siehe [zustandNach].
     */
    fun rekonstruiereBis(protokoll: EreignisProtokoll, bis: Zeitstempel): BeliefState? =
        falte(protokoll.ereignisse.filter { it.zeitstempel <= bis })

    /**
     * Belief State nach Replay der **ersten [anzahl] Ereignisse** (Einfüge-
     * reihenfolge, 0-basiert: `zustandNach(p, 2)` = Zustand nach den ersten zwei).
     * Isoliert Zwischenzustände **sequenz-genau** auch bei gleichem Zeitstempel,
     * wo [rekonstruiereBis] nur zeitstempel-genau auflöst (LH-FA-AUD-002). `null`,
     * falls unter den ersten [anzahl] kein Belief etabliert wurde; [anzahl] wird
     * auf `[0, Größe]` geklemmt.
     */
    fun zustandNach(protokoll: EreignisProtokoll, anzahl: Int): BeliefState? =
        falte(protokoll.ereignisse.take(anzahl.coerceAtLeast(0)))

    private fun falte(ereignisse: List<Ereignis>): BeliefState? =
        ereignisse.fold(null as BeliefState?) { belief, ereignis ->
            when (ereignis) {
                is BeliefAktualisiert -> ereignis.belief
                else -> belief
            }
        }
}
