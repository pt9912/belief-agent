package dev.beliefagent.application.belief.gaten

import dev.beliefagent.domain.belief.Aktion

/**
 * Verbindliche Aktions-Entscheidung des application-Gate-Schritts [AktionGaten]
 * (ARC-03, LH-FA-POL-006). **Provenienz-tragend und nicht umgehbar:**
 * [Freigegeben] hat einen `internal`-Konstruktor — es ist **nur** innerhalb des
 * `hexagon:application`-Moduls (also praktisch nur durch [AktionGaten])
 * konstruierbar, nicht durch einen Adapter/Executor, der die Domänen-Regel
 * [dev.beliefagent.domain.belief.KonfidenzGate] direkt aufruft. Eine
 * [dev.beliefagent.domain.belief.GateEntscheidung.Freigabe] (Konfidenz-only,
 * ohne menschliche Freigabe) ist ein **anderer Typ** und lässt sich nicht als
 * `Freigegeben` ausgeben — so kann die POL-004-Freigabe nicht umgangen werden.
 */
sealed interface Aktionsfreigabe {
    /**
     * Zur Ausführung freigegeben — Konfidenz **und** (bei irreversibler Aktion)
     * menschliche Freigabe liegen vor. Trägt die freigegebene [aktion] als
     * Provenienz. Nur von [AktionGaten] konstruierbar (internal).
     */
    class Freigegeben internal constructor(val aktion: Aktion) : Aktionsfreigabe

    /** Abgelehnt — Erfolgswahrscheinlichkeit unter der Wirkungsklassen-Schwelle. */
    data class Abgelehnt(val grund: String) : Aktionsfreigabe

    /** Eskaliert — zu unsicher (Resthypothese) oder fehlende menschliche Freigabe. */
    data class Eskaliert(val grund: String) : Aktionsfreigabe
}
