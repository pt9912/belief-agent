package dev.beliefagent.application.belief.gaten.ports

import dev.beliefagent.domain.belief.Aktion

/**
 * Human-Approval-Port (ARC-07, LH-FA-POL-004): holt für **extern-wirksame**
 * (irreversible) Aktionen eine explizite menschliche Freigabe ein — zusätzlich
 * zur bestandenen Konfidenz-Schwelle. In welle-03 steht dahinter ein
 * deterministischer Fake-Adapter (LH-QA-03); ein echter interaktiver Adapter
 * folgt später. Use-case-lokaler Port, Rolle `port`: importiert nur Domänentypen.
 *
 * **Anforderung an den echten Adapter (welle-05, Sicherheits-relevant):** Die
 * Freigabe muss an die konkrete Entscheidung **gebunden** und **einmal gültig**
 * sein (Nonce/Identität + Entscheidungs-Kontext), damit eine unter einem
 * sicheren Belief erteilte Freigabe nicht später unter einem unsichereren Belief
 * für eine wert-gleiche [Aktion] **wiederverwendet** werden kann. Der Fake ist
 * bewusst kontextfrei-deterministisch und erfüllt das nicht.
 */
interface HumanApprovalPort {
    /** True gdw. ein Mensch die irreversible [aktion] explizit freigegeben hat. */
    fun freigegeben(aktion: Aktion): Boolean
}
