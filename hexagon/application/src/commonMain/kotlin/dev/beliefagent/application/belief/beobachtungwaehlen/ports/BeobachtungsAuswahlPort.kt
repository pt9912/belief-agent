package dev.beliefagent.application.belief.beobachtungwaehlen.ports

import dev.beliefagent.domain.voi.VoiKandidat

/**
 * Beobachtungs-Auswahl-Port (`ARC-07`): liefert die aktuellen **VoI-Kandidaten** —
 * Beobachtungen mit erwarteter Diskriminierung + Kosten (`LH-FA-VOI-002`) —, aus
 * denen der Use-Case *beobachtung-waehlen* via `VoiSelektor` die informativste
 * wählt. In welle-04 steht dahinter ein deterministischer Fake (`voi-fake`,
 * `LH-QA-03`); welle-05 externalisiert die Kandidaten-/Diskriminierungs-Schätzung
 * ans LLM (`ADR-0001`, `LH-FA-LLM`).
 *
 * Use-case-lokaler Port, Rolle `port`: importiert nur Domänentypen, nie einen
 * Adapter.
 */
interface BeobachtungsAuswahlPort {
    fun kandidaten(): List<VoiKandidat>
}
