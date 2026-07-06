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
     * [schwelle] und das Gate **abgelehnt** (niedrige Erfolgswahrscheinlichkeit) —
     * der Zustand ist zu unsicher, um die Ablehnung hinzunehmen, aber nicht mehr
     * verbesserbar. [gate] trägt die volle [GateEntscheidung] (nicht vor-gestringt).
     * Deckt `LH-FA-ESK-003` („welches Gate, welche Schwelle, Stand der Resthypothese").
     */
    data class BeobachtungenErschoepft(
        val resthypothese: Double,
        val schwelle: Double,
        val gate: GateEntscheidung,
    ) : Eskalationsgrund

    /**
     * Das **Gate selbst** hat Eskalation verlangt — Resthypothese-Sperre für
     * irreversible Aktionen (`LH-FA-POL-005`) **oder** fehlende menschliche Freigabe
     * (`LH-FA-POL-004`). Unabhängig von der Beobachtungs-Erschöpfung: [gate] trägt
     * die Gate-Entscheidung samt Grund. Wird **nicht** über die Resthypothese neu
     * bewertet — die Gate-Forderung nach einem Menschen ist bindend.
     */
    data class GateEskalation(val gate: GateEntscheidung) : Eskalationsgrund

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
