package dev.beliefagent.adapter.approval

import dev.beliefagent.application.belief.gaten.ports.HumanApprovalPort
import dev.beliefagent.domain.belief.Aktion

/**
 * Deterministischer Fake-Approval-Adapter (ARC-08): steht in welle-03 für die
 * menschliche Freigabe hinter dem [HumanApprovalPort]. Feste Antwort — **Default
 * verweigert** (fail-safe, LH-QA-02): ohne explizite Setzung wird keine
 * extern-wirksame Aktion freigegeben. Deterministisch (LH-QA-03); ein echter
 * interaktiver Adapter folgt später.
 */
class FakeApproval(private val freigabe: Boolean = false) : HumanApprovalPort {
    override fun freigegeben(aktion: Aktion): Boolean = freigabe
}
