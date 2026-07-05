package dev.beliefagent.application.ports

import dev.beliefagent.domain.belief.Ereignis
import dev.beliefagent.domain.belief.EreignisProtokoll

/**
 * Audit-Port (ARC-06): **anwendungsweiter** Vertrag zum Persistieren des
 * unveränderlichen Ereignisprotokolls (LH-FA-AUD-001). Der Kern führt den
 * Vertrag; ein Outbound-Adapter (`adapters/outbound/audit-*`, slice-010)
 * implementiert ihn — der Kern importiert nie den Adapter (ADR-0001/0003).
 *
 * **Append-only:** [anhaengen] fügt hinten an, die Vergangenheit wird nie
 * überschrieben (LH-FA-AUD-001); [lade] liefert das persistierte Protokoll als
 * Grundlage der Rekonstruktion (LH-FA-AUD-002, `Rekonstruktion`).
 *
 * Rolle `port`: importiert nur Domänentypen ([Ereignis], [EreignisProtokoll]),
 * nie einen Adapter oder Application-Handler.
 */
interface AuditPort {

    /** Hängt [ereignis] append-only an das persistierte Protokoll an (LH-FA-AUD-001). */
    fun anhaengen(ereignis: Ereignis)

    /** Lädt das persistierte, geordnete Protokoll (Grundlage der Rekonstruktion, LH-FA-AUD-002). */
    fun lade(): EreignisProtokoll
}
