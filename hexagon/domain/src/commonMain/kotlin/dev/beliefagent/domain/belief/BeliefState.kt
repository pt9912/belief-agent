package dev.beliefagent.domain.belief

/**
 * Pflicht-Resthypothese (LH-FA-BEL-003): eigene Wahrscheinlichkeitsmasse für
 * „keine der genannten / unbekannt". Eigener Typ, damit sie im [BeliefState]
 * strukturell verlangt werden kann und nicht mit einer normalen [Hypothese]
 * verwechselbar ist.
 */
data class Resthypothese(
    val wahrscheinlichkeit: Double,
)

/**
 * Belief State über konkurrierende Hypothesen (LH-FA-BEL-001) mit
 * **struktureller Pflicht-Resthypothese** (LH-FA-BEL-003): der Konstruktor
 * verlangt sie, ein Belief State ohne Resthypothese ist damit nicht
 * konstruierbar.
 *
 * Out-of-Scope dieses Slices: Normierung (Σp = 1) und Validierung
 * (slice-002, LH-FA-BEL-002/004) sowie das bayesianische Update
 * (slice-003, LH-FA-OBS-003). Der private Konstruktor hält den Typ offen für
 * diese späteren Invarianten, ohne heute eine unvollständige Prüfung
 * vorzutäuschen.
 */
class BeliefState private constructor(
    val hypothesen: List<Hypothese>,
    val resthypothese: Resthypothese,
) {
    companion object {
        /** Erzeugt einen Belief State; die Resthypothese ist Pflicht (LH-FA-BEL-003). */
        fun of(hypothesen: List<Hypothese>, resthypothese: Resthypothese): BeliefState =
            BeliefState(hypothesen.toList(), resthypothese)
    }

    override fun toString(): String =
        "BeliefState(hypothesen=$hypothesen, resthypothese=$resthypothese)"
}
