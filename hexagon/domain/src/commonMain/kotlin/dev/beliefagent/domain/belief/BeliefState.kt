package dev.beliefagent.domain.belief

import kotlin.math.abs

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
 * **struktureller Pflicht-Resthypothese** (LH-FA-BEL-003) und **erzwungener
 * Normierung** (LH-FA-BEL-002): der validierende Fabrik-Konstruktor [of]
 * weist ungültige Zustände zurück (LH-FA-BEL-004), sodass ein ungültiger
 * Belief State nicht konstruierbar ist.
 *
 * Out-of-Scope dieses Slices: das bayesianische Update (slice-003,
 * LH-FA-OBS-003) und die Dedup korrelierter Evidenz (Welle-02).
 */
class BeliefState private constructor(
    val hypothesen: List<Hypothese>,
    val resthypothese: Resthypothese,
) {
    companion object {
        /**
         * Zulässige Abweichung von Σp = 1 (LH-OP-05). Default, revidierbar:
         * eng genug, um echte Nicht-Normierung zu fangen, weit genug für
         * Fließkomma-Rauschen beim Summieren weniger Hypothesen.
         */
        const val NORMIERUNGS_TOLERANZ: Double = 1e-9

        /**
         * Erzeugt einen **gültigen, normierten** Belief State und weist
         * ungültige Zustände zurück (LH-FA-BEL-004):
         *  - jede Wahrscheinlichkeit ≥ 0,
         *  - eindeutige Hypothesen-IDs (Menge, LH-FA-BEL-001),
         *  - Σ(Hypothesen) + Resthypothese = 1 innerhalb [NORMIERUNGS_TOLERANZ]
         *    (LH-FA-BEL-002).
         * Die Resthypothese ist ohnehin Konstruktor-Pflicht (LH-FA-BEL-003).
         */
        fun of(hypothesen: List<Hypothese>, resthypothese: Resthypothese): BeliefState {
            require(resthypothese.wahrscheinlichkeit >= 0.0) {
                "Resthypothese-Wahrscheinlichkeit ist negativ: ${resthypothese.wahrscheinlichkeit}"
            }
            hypothesen.forEach { h ->
                require(h.wahrscheinlichkeit >= 0.0) {
                    "Hypothese '${h.id.wert}' hat negative Wahrscheinlichkeit: ${h.wahrscheinlichkeit}"
                }
            }
            val ids = hypothesen.map { it.id }
            require(ids.toSet().size == ids.size) {
                "Hypothesen-IDs müssen eindeutig sein (Menge, LH-FA-BEL-001): ${ids.map { it.wert }}"
            }
            val summe = hypothesen.sumOf { it.wahrscheinlichkeit } + resthypothese.wahrscheinlichkeit
            require(abs(summe - 1.0) <= NORMIERUNGS_TOLERANZ) {
                "Belief State nicht normiert: Σp = $summe (Toleranz $NORMIERUNGS_TOLERANZ)"
            }
            return BeliefState(hypothesen.toList(), resthypothese)
        }
    }

    override fun toString(): String =
        "BeliefState(hypothesen=$hypothesen, resthypothese=$resthypothese)"
}
