package dev.beliefagent.domain.belief

import kotlin.math.ln

/**
 * Unsicherheitsmaße über einen [BeliefState] (LH-FA-BEL-008). Reine
 * Domänen-Funktionen. Die Resthypothese zählt als eigene Masse mit: sie ist
 * laut LH-FA-BEL-003 selbst eine Hypothese („keine der genannten / unbekannt")
 * und damit ein legitimer Kandidat für „die wahrscheinlichsten Hypothesen".
 */

/**
 * Shannon-Entropie über die volle Verteilung (Hypothesen + Resthypothese),
 * in **nats** (natürlicher Logarithmus). 0 bei voller Konzentration auf eine
 * Masse, maximal bei Gleichverteilung. Nullmassen tragen nichts bei
 * (0·ln 0 := 0).
 */
fun BeliefState.entropie(): Double {
    val massen = hypothesen.map { it.wahrscheinlichkeit } + resthypothese.wahrscheinlichkeit
    return -massen.filter { it > 0.0 }.sumOf { it * ln(it) }
}

/**
 * Abstand der zwei wahrscheinlichsten Massen (Hypothesen inkl. Resthypothese).
 * Groß = klar diskriminiert, ~0 = zwei gleich starke Kandidaten. Bei nur einer
 * Masse ist die zweite 0 (maximale Diskriminierung).
 */
fun BeliefState.top2Abstand(): Double {
    val sortiert = (hypothesen.map { it.wahrscheinlichkeit } + resthypothese.wahrscheinlichkeit)
        .sortedDescending()
    val erste = sortiert.getOrElse(0) { 0.0 }
    val zweite = sortiert.getOrElse(1) { 0.0 }
    return erste - zweite
}
