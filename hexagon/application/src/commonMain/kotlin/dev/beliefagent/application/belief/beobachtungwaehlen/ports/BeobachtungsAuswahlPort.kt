package dev.beliefagent.application.belief.beobachtungwaehlen.ports

import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.voi.VoiKandidat

/**
 * Beobachtungs-Auswahl-Port (`ARC-07`): liefert die aktuellen **VoI-Kandidaten** —
 * Beobachtungen mit erwarteter Diskriminierung + Kosten (`LH-FA-VOI-002`) —, aus
 * denen der Use-Case *beobachtung-waehlen* via `VoiSelektor` die informativste
 * wählt. Die Kandidaten hängen am aktuellen [BeliefState]: ein Adapter darf je
 * Top-Hypothesen andere Beobachtungen anbieten, die Entscheidungslogik bleibt aber
 * im Core.
 *
 * Ein deterministischer Fake (`voi-fake`, `LH-QA-03`) kann strukturierte
 * Kandidatenlisten liefern; spätere LLM-Bindungen dürfen höchstens explizite
 * Eingangswerte hinter diesem Port bereitstellen, keine VoI-Entscheidungslogik
 * (`ADR-0001`, `LH-FA-LLM-002`/`003`).
 *
 * Use-case-lokaler Port, Rolle `port`: importiert nur Domänentypen, nie einen
 * Adapter.
 */
interface BeobachtungsAuswahlPort {
    fun kandidaten(belief: BeliefState): List<VoiKandidat>
}
