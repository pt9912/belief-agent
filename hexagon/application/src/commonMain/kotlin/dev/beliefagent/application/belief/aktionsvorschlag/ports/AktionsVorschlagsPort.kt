package dev.beliefagent.application.belief.aktionsvorschlag.ports

import dev.beliefagent.application.belief.aktionsvorschlag.dto.AktionsVorschlag
import dev.beliefagent.domain.belief.BeliefState

/**
 * Use-case-lokaler Port fuer die abgegrenzte LLM-Aufgabe "Aktionen
 * vorschlagen" (LH-FA-LLM-002).
 *
 * Der Port liefert nur strukturierte Rohvorschlaege. Er aktualisiert keinen
 * Belief State, trifft keine Gate-Entscheidung und fuehrt keine Aktion aus.
 */
interface AktionsVorschlagsPort {
    fun vorschlaege(belief: BeliefState): List<AktionsVorschlag>
}
