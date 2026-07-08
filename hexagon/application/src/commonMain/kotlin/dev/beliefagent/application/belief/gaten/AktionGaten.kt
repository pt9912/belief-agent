package dev.beliefagent.application.belief.gaten

import dev.beliefagent.application.belief.gaten.ports.ApprovalAnfrage
import dev.beliefagent.application.belief.gaten.ports.ApprovalAuditKontextDigestBerechner
import dev.beliefagent.application.belief.gaten.ports.ApprovalAuditSnapshot
import dev.beliefagent.application.belief.gaten.ports.ApprovalErgebnis
import dev.beliefagent.application.belief.gaten.ports.ApprovalErgebnisArt
import dev.beliefagent.application.belief.gaten.ports.HumanApprovalPort
import dev.beliefagent.application.belief.aktualisieren.ports.UhrPort
import dev.beliefagent.application.ports.AuditPort
import dev.beliefagent.domain.belief.Aktion
import dev.beliefagent.domain.belief.ApprovalAngefragt
import dev.beliefagent.domain.belief.ApprovalErteilt
import dev.beliefagent.domain.belief.ApprovalFehler
import dev.beliefagent.domain.belief.ApprovalVerweigert
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Ereignis
import dev.beliefagent.domain.belief.GateEntscheidung
import dev.beliefagent.domain.belief.GateSchwellen
import dev.beliefagent.domain.belief.KonfidenzGate

/**
 * Use-Case *aktion-gaten* (ARC-03, LH-FA-POL-006): das Konfidenz-Gate als
 * **nicht umgehbarer** Schritt vor jeder Aktion. Es gibt **keinen** Pfad, der
 * eine Aktion zur Ausführung freigibt, ohne [KonfidenzGate] durchlaufen zu haben:
 * die einzige öffentliche Operation ist [pruefe], sie ruft das Gate immer, hebt
 * nie eine Nicht-Freigabe an, und die zur Ausführung freigegebene
 * [Aktionsfreigabe.Freigegeben] ist **nur hier** konstruierbar (internal) — ein
 * direkter `KonfidenzGate.bewerte`-Aufruf liefert einen anderen Typ und taugt
 * nicht als Ausführungs-Freigabe.
 *
 * Für **irreversible** Aktionen (aktuell extern-wirksam; das Prädikat ist
 * [dev.beliefagent.domain.belief.Wirkungsklasse.irreversibel], konsistent mit der
 * Resthypothese-Sperre und **fail-closed** für künftige irreversible Klassen) ist
 * die Konfidenz-Freigabe NICHT hinreichend (LH-FA-POL-004): zusätzlich muss der
 * [HumanApprovalPort] eine explizite menschliche Freigabe liefern, sonst wird
 * eskaliert. Fail-safe (LH-QA-02): fehlt die Freigabe, gibt es keine Freigabe.
 *
 * Deterministisch bei deterministischen Ports/Schwellen (LH-QA-03), framework-frei.
 */
class AktionGaten(
    private val approval: HumanApprovalPort,
    private val audit: AuditPort,
    private val uhr: UhrPort,
    private val schwellen: GateSchwellen = GateSchwellen(),
    private val fallbackDigestBerechner: ApprovalAuditKontextDigestBerechner = ApprovalAuditKontextDigestBerechner(),
) {
    fun pruefe(aktion: Aktion, belief: BeliefState): Aktionsfreigabe =
        when (val entscheidung = KonfidenzGate.bewerte(aktion, belief, schwellen)) {
            is GateEntscheidung.Ablehnung -> Aktionsfreigabe.Abgelehnt(entscheidung.grund)
            is GateEntscheidung.Eskalation -> Aktionsfreigabe.Eskaliert(entscheidung.grund)
            GateEntscheidung.Freigabe ->
                // LH-FA-POL-004: irreversible Aktion braucht zusätzlich menschliche Freigabe.
                if (aktion.wirkungsklasse.irreversibel) {
                    pruefeIrreversibleAktionMitApproval(ApprovalAnfrage(aktion, belief))
                } else {
                    Aktionsfreigabe.Freigegeben(aktion)
                }
        }

    private fun pruefeIrreversibleAktionMitApproval(anfrage: ApprovalAnfrage): Aktionsfreigabe {
        val ergebnis = try {
            approval.entscheide(anfrage)
        } catch (_: Exception) {
            ApprovalErgebnis.fehler(
                ApprovalAuditSnapshot(
                    anfrageKontextDigest = fallbackDigestBerechner.digest(anfrage),
                    kanal = "unknown",
                    nonceReferenz = "unavailable",
                    antwortReferenz = null,
                    identitaetsReferenz = null,
                    ergebnisGrund = "approval-port-fehler",
                ),
            )
        }

        return if (!auditApprovalEntscheidung(ergebnis)) {
            Aktionsfreigabe.Eskaliert("Approval-Audit fehlgeschlagen (LH-FA-AUD-001/LH-FA-POL-004)")
        } else if (ergebnis.istFreigegeben()) {
            Aktionsfreigabe.Freigegeben(anfrage.aktion)
        } else {
            Aktionsfreigabe.Eskaliert(
                "irreversible Aktion ohne menschliche Freigabe (LH-FA-POL-004): " +
                    ergebnis.audit.ergebnisGrund,
            )
        }
    }

    private fun auditApprovalEntscheidung(ergebnis: ApprovalErgebnis): Boolean =
        try {
            audit.anhaengen(
                ApprovalAngefragt(
                    zeitstempel = uhr.jetzt(),
                    anfrageKontextDigest = ergebnis.audit.anfrageKontextDigest,
                    kanal = ergebnis.audit.kanal,
                    nonceReferenz = ergebnis.audit.nonceReferenz,
                ),
            )
            audit.anhaengen(ergebnis.zuEreignis())
            true
        } catch (_: Exception) {
            false
        }

    private fun ApprovalErgebnis.zuEreignis(): Ereignis =
        when (art) {
            ApprovalErgebnisArt.FREIGEGEBEN -> ApprovalErteilt(
                zeitstempel = uhr.jetzt(),
                anfrageKontextDigest = audit.anfrageKontextDigest,
                kanal = audit.kanal,
                nonceReferenz = audit.nonceReferenz,
                antwortReferenz = requireNotNull(audit.antwortReferenz) {
                    "Freigegebenes Approval braucht eine Antwortreferenz"
                },
                identitaetsReferenz = requireNotNull(audit.identitaetsReferenz) {
                    "Freigegebenes Approval braucht eine Identitaetsreferenz"
                },
                ergebnisGrund = audit.ergebnisGrund,
            )
            ApprovalErgebnisArt.VERWEIGERT -> ApprovalVerweigert(
                zeitstempel = uhr.jetzt(),
                anfrageKontextDigest = audit.anfrageKontextDigest,
                kanal = audit.kanal,
                nonceReferenz = audit.nonceReferenz,
                antwortReferenz = audit.antwortReferenz,
                identitaetsReferenz = audit.identitaetsReferenz,
                ergebnisGrund = audit.ergebnisGrund,
            )
            ApprovalErgebnisArt.FEHLER -> ApprovalFehler(
                zeitstempel = uhr.jetzt(),
                anfrageKontextDigest = audit.anfrageKontextDigest,
                kanal = audit.kanal,
                nonceReferenz = audit.nonceReferenz,
                antwortReferenz = audit.antwortReferenz,
                identitaetsReferenz = audit.identitaetsReferenz,
                ergebnisGrund = audit.ergebnisGrund,
            )
        }
}
