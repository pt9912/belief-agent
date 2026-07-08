package dev.beliefagent.application.belief.gaten

import dev.beliefagent.application.belief.gaten.ports.ApprovalAnfrage
import dev.beliefagent.application.belief.gaten.ports.HumanApprovalPort
import dev.beliefagent.domain.belief.Aktion
import dev.beliefagent.domain.belief.BeliefState
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
    private val schwellen: GateSchwellen = GateSchwellen(),
) {
    fun pruefe(aktion: Aktion, belief: BeliefState): Aktionsfreigabe =
        when (val entscheidung = KonfidenzGate.bewerte(aktion, belief, schwellen)) {
            is GateEntscheidung.Ablehnung -> Aktionsfreigabe.Abgelehnt(entscheidung.grund)
            is GateEntscheidung.Eskalation -> Aktionsfreigabe.Eskaliert(entscheidung.grund)
            GateEntscheidung.Freigabe ->
                // LH-FA-POL-004: irreversible Aktion braucht zusätzlich menschliche Freigabe.
                if (aktion.wirkungsklasse.irreversibel && !approval.freigegeben(ApprovalAnfrage(aktion, belief))) {
                    Aktionsfreigabe.Eskaliert(
                        "irreversible Aktion ohne menschliche Freigabe (LH-FA-POL-004)",
                    )
                } else {
                    Aktionsfreigabe.Freigegeben(aktion)
                }
        }
}
