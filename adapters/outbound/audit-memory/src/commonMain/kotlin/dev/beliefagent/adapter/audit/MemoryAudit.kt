package dev.beliefagent.adapter.audit

import dev.beliefagent.application.ports.AuditPort
import dev.beliefagent.domain.belief.Ereignis
import dev.beliefagent.domain.belief.EreignisProtokoll

/**
 * In-Memory-Audit-Adapter (ARC-08): persistiert das append-only
 * [EreignisProtokoll] im Speicher (LH-FA-AUD-001) — deterministischer Stand-in
 * für welle-02; ein dauerhafter Adapter (DB/Datei) folgt später. Die
 * Append-only-Ordnung (kein Rück-Datieren) erzwingt [EreignisProtokoll] selbst.
 */
class MemoryAudit : AuditPort {
    private var protokoll = EreignisProtokoll.LEER

    override fun anhaengen(ereignis: Ereignis) {
        protokoll = protokoll.append(ereignis)
    }

    override fun lade(): EreignisProtokoll = protokoll
}
