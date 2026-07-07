package dev.beliefagent.domain.belief

/**
 * Ein Protokoll-Ereignis (LH-FA-AUD-001). Die versiegelte Hierarchie deckt die
 * geforderten Ereignisarten ab: Hypothese hinzugefügt, Beobachtung erfasst,
 * Belief aktualisiert, Aktion vorgeschlagen, Gate abgelehnt, Eskalation
 * angefordert. Jedes Ereignis trägt einen [Zeitstempel] (LH-FA-AUD-004).
 *
 * Die Append-only-Sequenz und die Belief-Rekonstruktion sind Out-of-Scope
 * dieses Slices (slice-007). Ereignisse, die auf noch nicht existierende
 * Wellen-Typen zeigen (Aktion, Gate, Eskalation — welle-03/04), tragen vorerst
 * eine Begründung als Wert; sie werden später angereichert.
 */
sealed interface Ereignis {
    val zeitstempel: Zeitstempel
}

data class HypotheseHinzugefuegt(
    override val zeitstempel: Zeitstempel,
    val hypothese: HypotheseId,
) : Ereignis

data class BeobachtungErfasst(
    override val zeitstempel: Zeitstempel,
    val beobachtung: Beobachtung,
) : Ereignis

data class BeliefAktualisiert(
    override val zeitstempel: Zeitstempel,
    val belief: BeliefState,
) : Ereignis

data class AktionVorgeschlagen(
    override val zeitstempel: Zeitstempel,
    val beschreibung: String,
) : Ereignis

data class GateAbgelehnt(
    override val zeitstempel: Zeitstempel,
    val grund: String,
) : Ereignis

data class EskalationAngefordert(
    override val zeitstempel: Zeitstempel,
    val grund: String,
) : Ereignis

data class KonfidenzExternalisiert(
    override val zeitstempel: Zeitstempel,
    val referenz: String,
    val wert: Double,
    val quelle: String,
    val version: Int,
) : Ereignis {
    init {
        require(referenz.isNotBlank()) { "Konfidenz-Referenz darf nicht leer sein" }
        require(wert in 0.0..1.0) { "Konfidenz-Wert muss in [0,1] liegen: $wert" }
        require(quelle.isNotBlank()) { "Konfidenz-Quelle darf nicht leer sein" }
        require(version > 0) { "Konfidenz-Version muss positiv sein: $version" }
    }
}

data class KonfidenzUeberschrieben(
    override val zeitstempel: Zeitstempel,
    val referenz: String,
    val alterWert: Double,
    val neuerWert: Double,
    val begruendung: String,
    val version: Int,
) : Ereignis {
    init {
        require(referenz.isNotBlank()) { "Konfidenz-Referenz darf nicht leer sein" }
        require(alterWert in 0.0..1.0) { "Alter Konfidenz-Wert muss in [0,1] liegen: $alterWert" }
        require(neuerWert in 0.0..1.0) { "Neuer Konfidenz-Wert muss in [0,1] liegen: $neuerWert" }
        require(begruendung.isNotBlank()) { "Konfidenz-Override braucht eine Begruendung" }
        require(version > 0) { "Konfidenz-Version muss positiv sein: $version" }
    }
}
