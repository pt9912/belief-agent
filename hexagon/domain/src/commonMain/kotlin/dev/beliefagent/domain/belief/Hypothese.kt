package dev.beliefagent.domain.belief

/**
 * Stabile Identität einer Hypothese (Value Object). Trennt die fachliche
 * Identität von der (veränderlichen) Wahrscheinlichkeit.
 */
@JvmInline
value class HypotheseId(val wert: String) {
    init {
        require(wert.isNotBlank()) { "HypotheseId darf nicht leer sein" }
    }
}

/**
 * Eine konkurrierende Hypothese mit zugeordneter Wahrscheinlichkeit
 * (LH-FA-BEL-001): Das System führt eine Menge solcher Hypothesen statt einer
 * einzelnen angenommenen Wahrheit.
 *
 * Bewusst noch ohne Normierungs-/Validierungslogik (slice-002) und ohne
 * Bayes-Update (slice-003) — dieser Slice liefert nur den puren Domain-Typ.
 */
data class Hypothese(
    val id: HypotheseId,
    val wahrscheinlichkeit: Double,
)
