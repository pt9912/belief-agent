package dev.beliefagent.application.belief.gaten

import dev.beliefagent.application.belief.gaten.ports.HumanApprovalPort
import dev.beliefagent.domain.belief.Aktion
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.GateEntscheidung
import dev.beliefagent.domain.belief.GateSchwellen
import dev.beliefagent.domain.belief.KonfidenzGate
import dev.beliefagent.domain.belief.Wirkungsklasse

/**
 * Use-Case *aktion-gaten* (ARC-03, LH-FA-POL-006): das Konfidenz-Gate als
 * **nicht umgehbarer** Schritt vor jeder Aktion. Es gibt **keinen** Pfad, der
 * eine Aktion freigibt, ohne [KonfidenzGate] durchlaufen zu haben — die einzige
 * öffentliche Operation ist [pruefe], sie ruft das Gate immer, und sie **hebt
 * nie** eine Nicht-Freigabe zur Freigabe an.
 *
 * Für **extern-wirksame** (irreversible) Aktionen ist die Konfidenz-Freigabe
 * NICHT hinreichend (LH-FA-POL-004): zusätzlich muss der [HumanApprovalPort] eine
 * explizite menschliche Freigabe liefern, sonst wird zu [GateEntscheidung.Eskalation]
 * herabgestuft. Fail-safe (LH-QA-02): fehlt die Freigabe, gibt es keine Freigabe.
 *
 * Deterministisch bei deterministischen Ports/Schwellen (LH-QA-03). Framework-frei
 * (ADR-0001/0003); DI verdrahtet Adapter am Rand (Composition-Root folgt).
 */
class AktionGaten(
    private val approval: HumanApprovalPort,
    private val schwellen: GateSchwellen = GateSchwellen(),
) {
    fun pruefe(aktion: Aktion, belief: BeliefState): GateEntscheidung {
        val entscheidung = KonfidenzGate.bewerte(aktion, belief, schwellen)
        // LH-FA-POL-004: extern-wirksame Freigabe braucht zusätzlich menschliche
        // Freigabe; fehlt sie, wird eskaliert (nie freigegeben).
        if (entscheidung is GateEntscheidung.Freigabe &&
            aktion.wirkungsklasse == Wirkungsklasse.EXTERN_WIRKSAM &&
            !approval.freigegeben(aktion)
        ) {
            return GateEntscheidung.Eskalation(
                "extern-wirksame Aktion ohne menschliche Freigabe (LH-FA-POL-004)",
            )
        }
        return entscheidung
    }
}
