package dev.beliefagent.application.belief.gaten.ports

import dev.beliefagent.domain.belief.Aktion

/**
 * Human-Approval-Port (ARC-07, LH-FA-POL-004): holt für **extern-wirksame**
 * (irreversible) Aktionen eine explizite menschliche Freigabe ein — zusätzlich
 * zur bestandenen Konfidenz-Schwelle. In welle-03 steht dahinter ein
 * deterministischer Fake-Adapter (LH-QA-03); ein echter interaktiver Adapter
 * folgt später. Use-case-lokaler Port, Rolle `port`: importiert nur Domänentypen.
 */
interface HumanApprovalPort {
    /** True gdw. ein Mensch die extern-wirksame [aktion] explizit freigegeben hat. */
    fun freigegeben(aktion: Aktion): Boolean
}
