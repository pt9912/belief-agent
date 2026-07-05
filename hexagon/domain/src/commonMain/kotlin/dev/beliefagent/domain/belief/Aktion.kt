package dev.beliefagent.domain.belief

/**
 * Erfolgswahrscheinlichkeit einer Aktion: `P(Aktion erreicht Ziel | aktueller
 * Belief)` (LH-FA-ACT-003) — bewusst **getrennt** von der Wahrscheinlichkeit der
 * zugrunde liegenden Hypothese. In `[0,1]`.
 */
@JvmInline
value class Erfolgswahrscheinlichkeit(val wert: Double) {
    init {
        require(wert in 0.0..1.0) { "Erfolgswahrscheinlichkeit muss in [0,1] liegen: $wert" }
    }
}

/**
 * Eine vorgeschlagene Aktion (LH-FA-ACT): trägt ihre [Wirkungsklasse]
 * (LH-FA-ACT-001/002), eine eigene [Erfolgswahrscheinlichkeit] (LH-FA-ACT-003)
 * und die sie **stützende Evidenz** (LH-FA-ACT-004, Rückverfolgbarkeit
 * Aktion → Evidenz — mindestens eine [Beobachtung]). Reiner Domänentyp; das Gate
 * (slice-012) und die menschliche Freigabe (slice-013) sind Out-of-Scope.
 */
data class Aktion(
    val beschreibung: String,
    val wirkungsklasse: Wirkungsklasse,
    val erfolgswahrscheinlichkeit: Erfolgswahrscheinlichkeit,
    val stuetzendeEvidenz: List<Beobachtung>,
) {
    init {
        require(beschreibung.isNotBlank()) { "Aktions-Beschreibung darf nicht leer sein" }
        require(stuetzendeEvidenz.isNotEmpty()) {
            "Aktion muss stützende Evidenz referenzieren (LH-FA-ACT-004)"
        }
    }
}
