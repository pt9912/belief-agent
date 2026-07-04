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
