package dev.beliefagent.adapter.approval

import dev.beliefagent.application.belief.gaten.ports.ApprovalAnfrage
import dev.beliefagent.application.belief.gaten.ports.ApprovalAuditKontextDigestBerechner
import dev.beliefagent.application.belief.gaten.ports.ApprovalAuditSnapshot
import dev.beliefagent.application.belief.gaten.ports.ApprovalErgebnis
import dev.beliefagent.application.belief.gaten.ports.HumanApprovalPort

/**
 * Deterministischer Fake-Approval-Adapter (ARC-08): steht in welle-03 für die
 * menschliche Freigabe hinter dem [HumanApprovalPort]. Feste Antwort — **Default
 * verweigert** (fail-safe, LH-QA-02): ohne explizite Setzung wird keine
 * extern-wirksame Aktion freigegeben. Deterministisch (LH-QA-03); ein echter
 * interaktiver Adapter folgt später.
 */
class FakeApproval(
    private val freigabe: Boolean = false,
    private val digestBerechner: ApprovalAuditKontextDigestBerechner = ApprovalAuditKontextDigestBerechner(),
) : HumanApprovalPort {
    override fun entscheide(anfrage: ApprovalAnfrage): ApprovalErgebnis {
        val snapshot = ApprovalAuditSnapshot(
            anfrageKontextDigest = digestBerechner.digest(anfrage),
            kanal = "fake",
            nonceReferenz = "fake-static",
            antwortReferenz = "fake-static-response",
            identitaetsReferenz = "fake-operator",
            ergebnisGrund = if (freigabe) "fake-freigegeben" else "fake-verweigert",
        )
        return if (freigabe) ApprovalErgebnis.freigegeben(snapshot) else ApprovalErgebnis.verweigert(snapshot)
    }
}
