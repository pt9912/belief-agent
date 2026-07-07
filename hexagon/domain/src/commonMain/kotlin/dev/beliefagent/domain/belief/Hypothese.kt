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
 * Referenz auf stützende Evidenz einer Hypothese (LH-FA-BEL-007).
 *
 * Der Wert ist bewusst nur eine stabile Referenz, kein eingebetteter
 * Beobachtungsinhalt: Beobachtungen/Ereignisse bleiben eigenständige
 * Protokollobjekte, Hypothesen machen deren Beleg nur referenzierbar.
 */
@JvmInline
value class EvidenzReferenz(val wert: String) {
    init {
        require(wert.isNotBlank()) { "EvidenzReferenz darf nicht leer sein" }
    }
}

/**
 * Eine konkurrierende Hypothese mit zugeordneter Wahrscheinlichkeit
 * (LH-FA-BEL-001): Das System führt eine Menge solcher Hypothesen statt einer
 * einzelnen angenommenen Wahrheit.
 */
data class Hypothese(
    val id: HypotheseId,
    val wahrscheinlichkeit: Double,
    val stuetzendeEvidenz: List<EvidenzReferenz> = emptyList(),
)
