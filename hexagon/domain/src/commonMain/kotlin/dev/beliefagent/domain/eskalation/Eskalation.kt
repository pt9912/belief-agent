package dev.beliefagent.domain.eskalation

import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.GateEntscheidung

/**
 * Grund einer [Eskalation] (`LH-FA-ESK-003`): **warum** eskaliert wurde. Versiegelt,
 * damit die zwei **unabhängigen** Auslöser explizit unterschieden werden — die
 * [Eskalationsbedingung] (`LH-FA-ESK-001`) und die Budget-Erschöpfung
 * (`LH-FA-ESK-004`).
 */
sealed interface Eskalationsgrund {
    /**
     * `LH-FA-ESK-001`: günstige Beobachtungen erschöpft, [resthypothese] ≥ der
     * [schwelle] und das Aktions-Gate geschlossen — [gate] trägt die **volle**
     * [GateEntscheidung] (Ablehnung/Eskalation, nicht vor-gestringt), sodass der
     * Grund den Gate-Typ behält. Deckt `LH-FA-ESK-003` („welches Gate, welche
     * Schwelle, Stand der Resthypothese") mit gate + schwelle + resthypothese ab.
     */
    data class BeobachtungenErschoepft(
        val resthypothese: Double,
        val schwelle: Double,
        val gate: GateEntscheidung,
    ) : Eskalationsgrund

    /** `LH-FA-ESK-004`: das [budget] der Informationssammlung ist erschöpft (eigenständiger Auslöser). */
    data class BudgetErschoepft(val budget: Budget) : Eskalationsgrund
}

/**
 * Eskalation als **definierter Zustand** (`LH-FA-ESK-002`): das *erwartete* Ergebnis
 * „an einen Menschen übergeben" — **kein Fehler, keine Exception**, sondern ein
 * normaler Rückgabewert des Entscheidungszyklus (slice-016).
 *
 * Trägt den vollen **Kontext** (`LH-FA-ESK-003`): den aktuellen [belief], die
 * gesammelte [evidenz] und den [grund]. Reiner Domänen-Zustand (`ARC-05`),
 * framework-frei (`ADR-0001`/`ADR-0003`).
 *
 * **Abgrenzung** (slice-015 §6): [dev.beliefagent.domain.belief.GateEntscheidung.Eskalation]
 * ist nur das **Regel-Signal** des Konfidenz-Gates (slice-012); *diese* Eskalation
 * ist der **Zustands-Kontext**, den der Zyklus daraus **oder** aus Budget-Erschöpfung
 * produziert — ein kohärenter Eskalations-Begriff.
 */
data class Eskalation(
    val belief: BeliefState,
    val evidenz: List<Beobachtung>,
    val grund: Eskalationsgrund,
)
