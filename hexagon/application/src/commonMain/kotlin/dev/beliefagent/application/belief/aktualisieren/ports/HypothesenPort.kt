package dev.beliefagent.application.belief.aktualisieren.ports

import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.HypothesenKandidat

/**
 * Hypothesen-Port (ARC-07): fordert neue oder verfeinerte
 * [HypothesenKandidat]en für einen [belief] an, wenn die Domäne
 * Re-Hypothesenbildung signalisiert (LH-FA-BEL-005/006).
 *
 * Der Vertrag ist bewusst auf die abgegrenzte LLM-Aufgabe
 * "Hypothesen erzeugen/verfeinern" beschränkt (LH-FA-LLM-002) und getrennt vom
 * [LlmPort], der ausschließlich Likelihoods schätzt. Use-case-lokaler Port,
 * Rolle `port`: importiert nur Domänentypen, nie einen Adapter.
 */
interface HypothesenPort {
    fun kandidaten(belief: BeliefState): List<HypothesenKandidat>
}
