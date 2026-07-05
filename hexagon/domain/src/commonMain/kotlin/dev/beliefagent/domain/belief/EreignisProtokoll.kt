package dev.beliefagent.domain.belief

/**
 * Unveränderliches, geordnetes Ereignisprotokoll (LH-FA-AUD-001): append-only
 * Sequenz von [Ereignis], aus der der Belief State rekonstruierbar ist
 * ([Rekonstruktion], LH-FA-AUD-002). Die Entscheidungsspur ist damit ein
 * prüfbares Protokoll, kein modellinternes Reasoning (LH-FA-AUD-003).
 *
 * **Append-only in der Zeit:** [append] hängt hinten an und weist ein Ereignis
 * zurück, dessen [Zeitstempel] VOR dem letzten liegt — die Vergangenheit ist
 * nicht mutierbar (kein Rück-Datieren; gleicher Zeitstempel bleibt erlaubt).
 * Jede Operation liefert ein NEUES Protokoll; das bestehende bleibt unverändert.
 * Reiner Domänentyp, framework-frei (ADR-0001/0003), deterministisch (LH-QA-03).
 *
 * Der Audit-Port (Persistenz-Vertrag) lebt in der application-Schicht und wird
 * mit deren Ausbau eingeführt (slice-008); er gehört nicht in die Domäne.
 */
class EreignisProtokoll private constructor(
    val ereignisse: List<Ereignis>,
) {
    val groesse: Int get() = ereignisse.size

    fun istLeer(): Boolean = ereignisse.isEmpty()

    /**
     * Hängt [ereignis] an und liefert ein NEUES Protokoll. Wirft
     * [IllegalArgumentException], wenn [ereignis] zeitlich vor dem letzten liegt
     * (Append-only-Ordnung, LH-FA-AUD-001) — gleicher Zeitstempel ist zulässig.
     */
    fun append(ereignis: Ereignis): EreignisProtokoll {
        val letzter = ereignisse.lastOrNull()
        if (letzter != null) {
            require(ereignis.zeitstempel >= letzter.zeitstempel) {
                "Append-only verletzt: Ereignis @${ereignis.zeitstempel.epochMillis} " +
                    "liegt vor dem letzten @${letzter.zeitstempel.epochMillis}"
            }
        }
        return EreignisProtokoll(ereignisse + ereignis)
    }

    companion object {
        /** Das leere Protokoll — Startpunkt jeder append-Kette. */
        val LEER: EreignisProtokoll = EreignisProtokoll(emptyList())

        /**
         * Baut ein Protokoll aus [ereignisse] in gegebener Reihenfolge und prüft
         * dabei die Append-only-Ordnung (jedes Element ≥ Vorgänger, LH-FA-AUD-001).
         */
        fun von(ereignisse: List<Ereignis>): EreignisProtokoll =
            ereignisse.fold(LEER) { protokoll, e -> protokoll.append(e) }
    }
}
