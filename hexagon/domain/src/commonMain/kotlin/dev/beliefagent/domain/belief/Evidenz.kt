package dev.beliefagent.domain.belief

/**
 * Evidenz — der beobachtete Inhalt einer [Beobachtung]. Für welle-02 ein
 * schlanker Beschreibungs-Wert; verrauschte/korrelierte Bewertung erfolgt beim
 * Update (Dedup slice-006), nicht im Typ.
 */
data class Evidenz(val beschreibung: String) {
    init {
        require(beschreibung.isNotBlank()) { "Evidenz-Beschreibung darf nicht leer sein" }
    }
}
