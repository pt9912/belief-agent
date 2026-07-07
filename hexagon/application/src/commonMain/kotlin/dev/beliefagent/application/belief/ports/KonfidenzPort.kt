package dev.beliefagent.application.belief.ports

/**
 * Business-Area-geteilter Konfidenz-Port (ARC-07/ARC-08).
 *
 * Der Port speichert externalisierte Modell-Konfidenzen append-only. Adapter
 * duerfen spaeter persistieren oder Fixtures laden; der Core konsumiert nur den
 * Contract und importiert keinen Adapter.
 */
interface KonfidenzPort {

    /** Haengt [konfidenz] append-only an die Historie der Referenz an. */
    fun anhaengen(konfidenz: ExternalisierteKonfidenz)

    /** Liefert die Historie zu [referenz] in Einfuege-Reihenfolge. */
    fun lade(referenz: KonfidenzReferenz): List<ExternalisierteKonfidenz>
}
