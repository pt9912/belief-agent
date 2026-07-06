package dev.beliefagent.domain.eskalation

/**
 * Budget der Informationssammlung (`LH-FA-ESK-004`): begrenzt **Schritte** und
 * optional **Kosten**, damit die Sammlung nicht in eine Endlosschleife läuft. Seine
 * Erschöpfung ([erschoepft]) ist ein **eigenständiger** Eskalations-Auslöser —
 * bewusst **getrennt** von der [Eskalationsbedingung] (`LH-FA-ESK-001`), nicht an
 * deren Gate-Teilbedingung gekoppelt.
 *
 * Unveränderlich: [verbrauche] gibt ein **neues** Budget mit einem weiteren
 * verbrauchten Schritt (und addierten Kosten) zurück. Deterministisch (`LH-QA-03`),
 * framework-frei (`ADR-0001`/`ADR-0003`).
 *
 * Invarianten (fail-closed, `MR-003`): [maxSchritte] echt `> 0` (sonst wäre das
 * Budget von Beginn an erschöpft bzw. sinnlos); [maxKosten] `null` (keine
 * Kostengrenze) oder endlich `> 0`; verbrauchte Werte endlich und `>= 0`.
 */
data class Budget(
    val maxSchritte: Int = STANDARD_SCHRITTE,
    val verbrauchteSchritte: Int = 0,
    val maxKosten: Double? = null,
    val verbrauchteKosten: Double = 0.0,
) {
    init {
        require(maxSchritte > 0) { "maxSchritte muss > 0 sein: $maxSchritte" }
        require(verbrauchteSchritte >= 0) { "verbrauchteSchritte darf nicht negativ sein: $verbrauchteSchritte" }
        require(maxKosten == null || (maxKosten.isFinite() && maxKosten > 0.0)) {
            "maxKosten muss null oder endlich > 0 sein: $maxKosten"
        }
        require(verbrauchteKosten.isFinite() && verbrauchteKosten >= 0.0) {
            "verbrauchteKosten muss endlich und >= 0 sein: $verbrauchteKosten"
        }
    }

    /**
     * Erschöpft, sobald die verbrauchten **Schritte** die Grenze erreichen **oder**
     * — falls [maxKosten] gesetzt — die verbrauchten **Kosten** die Kostengrenze
     * erreichen.
     */
    val erschoepft: Boolean
        get() = verbrauchteSchritte >= maxSchritte || (maxKosten != null && verbrauchteKosten >= maxKosten)

    /** Verbraucht einen Schritt und die angegebenen [kosten]; liefert das fortgeschriebene Budget. */
    fun verbrauche(kosten: Double = 0.0): Budget {
        require(kosten.isFinite() && kosten >= 0.0) { "kosten muss endlich und >= 0 sein: $kosten" }
        return copy(
            verbrauchteSchritte = verbrauchteSchritte + 1,
            verbrauchteKosten = verbrauchteKosten + kosten,
        )
    }

    companion object {
        /** Default-Budget der Informationssammlung vor Eskalation (`BUDGET_STEPS`, Spezifikation §3). */
        const val STANDARD_SCHRITTE: Int = 20
    }
}
