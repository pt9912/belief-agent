package dev.beliefagent.application.belief.entscheidungszyklus

import dev.beliefagent.application.belief.aktualisieren.BeliefAktualisieren
import dev.beliefagent.application.belief.aktualisieren.BeliefAktualisierenBefehl
import dev.beliefagent.application.belief.beobachtungwaehlen.BeobachtungWaehlen
import dev.beliefagent.application.belief.gaten.AktionGaten
import dev.beliefagent.application.belief.gaten.Aktionsfreigabe
import dev.beliefagent.domain.belief.Aktion
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.GateEntscheidung
import dev.beliefagent.domain.eskalation.Budget
import dev.beliefagent.domain.eskalation.Eskalation
import dev.beliefagent.domain.eskalation.Eskalationsbedingung
import dev.beliefagent.domain.eskalation.Eskalationsgrund

/**
 * Terminales Ergebnis des Entscheidungszyklus (`ARC-09`): genau **eines von drei**.
 * Versiegelt, damit jeder Ausgang explizit behandelt werden muss.
 */
sealed interface Zyklusergebnis {
    /** Gehandelt — das Gate hat [freigabe] erteilt (ausführungsbereit); [belief] ist der Stand danach. */
    data class Gehandelt(val freigabe: Aktionsfreigabe.Freigegeben, val belief: BeliefState) : Zyklusergebnis

    /** Eskaliert — an den Menschen übergeben, mit vollem Kontext ([Eskalation]: Belief, Evidenz, Grund). */
    data class Eskaliert(val eskalation: Eskalation) : Zyklusergebnis

    /**
     * Abgelehnt — Gate geschlossen, günstige Beobachtungen erschöpft, aber die
     * Resthypothese liegt **unter** θ_esc: nicht handeln, nicht eskalieren
     * (`LH-FA-POL-002.a` — „ablehnen" ohne Eskalation).
     */
    data class Abgelehnt(val grund: String, val belief: BeliefState) : Zyklusergebnis
}

/**
 * Entscheidungszyklus (`ARC-09`, `LH-FA-VOI-001`): verbindet die Bausteine der
 * welle-04 zu **sammeln | handeln | eskalieren**. Vor jeder Aktionsausführung läuft
 * das **nicht umgehbare** Gate (`LH-FA-POL-006`); ist es geschlossen, sammelt der
 * Zyklus **zuerst Information** statt zu handeln (`LH-FA-VOI-001`) — er wählt via
 * [BeobachtungWaehlen] die informativste Beobachtung, schreibt den Belief über
 * [BeliefAktualisieren] fort und prüft erneut. Sind die günstigen Beobachtungen
 * erschöpft **und** die Resthypothese hoch **und** das Gate zu (`LH-FA-ESK-001`) —
 * **oder**, davon unabhängig, das [Budget] erschöpft (`LH-FA-ESK-004`) —, eskaliert
 * er als **definierter Zustand** mit Kontext (`LH-FA-ESK-002`/`003`).
 *
 * **Garantierte Terminierung (`LH-QA-02` fail-safe):** jeder Sammel-Schritt
 * verbraucht ein Budget; der Zyklus läuft höchstens `budget.maxSchritte` Runden und
 * eskaliert dann. Kein Endlos-Sammeln.
 *
 * Die Domäne kennt die application-`Aktionsfreigabe` nicht (Abhängigkeit nach innen,
 * `ADR-0003`); der Zyklus **mappt** die geschlossene Gate-Entscheidung zurück auf die
 * Domänen-[GateEntscheidung], damit der Eskalations-Grund den Gate-Typ trägt.
 * Deterministisch bei deterministischen Ports (`LH-QA-03`), framework-frei.
 *
 * Out-of-Scope: die reale Aktionsausführung und der produktive cli-Composition-Root
 * (der die Outbound-Adapter an die Ports bindet und den Zyklus anstößt) folgen später;
 * hier läuft der Zyklus E2E-nah im Testcode gegen Fakes.
 */
class Entscheidungszyklus(
    private val beobachtungWaehlen: BeobachtungWaehlen,
    private val beliefAktualisieren: BeliefAktualisieren,
    private val aktionGaten: AktionGaten,
    private val eskalationsSchwelle: Double = Eskalationsbedingung.STANDARD_ESKALATIONS_SCHWELLE,
) {
    fun entscheide(aktion: Aktion, prior: BeliefState, budget: Budget): Zyklusergebnis {
        var belief = prior
        var restbudget = budget
        val gesammelt = mutableListOf<Beobachtung>()
        val bereitsGesammelt = mutableSetOf<Beobachtung>() // verbrauchte Beobachtungen -> keine Wiederholung
        while (true) {
            // 1. Gate (nicht umgehbar, LH-FA-POL-006): freigegeben -> handeln. Sonst
            //    die geschlossene Entscheidung als Domänen-GateEntscheidung festhalten.
            val gate: GateEntscheidung = when (val freigabe = aktionGaten.pruefe(aktion, belief)) {
                is Aktionsfreigabe.Freigegeben -> return Zyklusergebnis.Gehandelt(freigabe, belief)
                is Aktionsfreigabe.Abgelehnt -> GateEntscheidung.Ablehnung(freigabe.grund)
                is Aktionsfreigabe.Eskaliert -> GateEntscheidung.Eskalation(freigabe.grund)
            }
            // 2. Budget erschöpft -> eigenständige Eskalation (LH-FA-ESK-004, getrennter Pfad).
            if (restbudget.erschoepft) {
                return Zyklusergebnis.Eskaliert(
                    Eskalation(belief, gesammelt.toList(), Eskalationsgrund.BudgetErschoepft(restbudget)),
                )
            }
            // 3. Sammeln: die informativste **noch nicht verbrauchte** günstige Beobachtung
            //    wählen (LH-FA-VOI-001/002). Keine mehr -> erschöpft: eskalieren oder
            //    ablehnen (LH-FA-ESK-001). Der Ausschluss verhindert, dieselbe Beobachtung
            //    mehrfach zu zählen (Scheingewissheit, LH-FA-OBS-004).
            val kandidat = beobachtungWaehlen.waehle(belief, bereitsGesammelt)
                ?: return abschlussOhneBeobachtung(belief, gesammelt.toList(), gate)
            // beobachten -> Belief fortschreiben -> Budget verbrauchen -> Zyklus wiederholen.
            belief = beliefAktualisieren.ausfuehren(
                BeliefAktualisierenBefehl(belief, listOf(kandidat.beobachtung)),
            ).belief
            restbudget = restbudget.verbrauche(kandidat.kosten)
            gesammelt += kandidat.beobachtung
            bereitsGesammelt += kandidat.beobachtung
        }
    }

    /**
     * Günstige Beobachtungen erschöpft — Abschluss je nach **Grund** der
     * Gate-Schließung:
     *  - Das Gate hat **selbst eskaliert** (Resthypothese-Sperre `LH-FA-POL-005`
     *    **oder** fehlende menschliche Freigabe `LH-FA-POL-004`) → bindend
     *    eskalieren, **nicht** über die Resthypothese neu bewerten. Sonst würde eine
     *    konfident-fertige, aber unfreigegebene irreversible Aktion still abgelehnt,
     *    statt an den Menschen zu gehen.
     *  - Das Gate hat **abgelehnt** (niedrige Erfolgswahrscheinlichkeit) →
     *    `LH-FA-ESK-001`: eskalieren nur bei hoher Resthypothese (zu unsicher), sonst
     *    ablehnen (nicht handeln, nicht eskalieren).
     */
    private fun abschlussOhneBeobachtung(
        belief: BeliefState,
        gesammelt: List<Beobachtung>,
        gate: GateEntscheidung,
    ): Zyklusergebnis = when (gate) {
        is GateEntscheidung.Eskalation ->
            Zyklusergebnis.Eskaliert(Eskalation(belief, gesammelt, Eskalationsgrund.GateEskalation(gate)))

        is GateEntscheidung.Ablehnung ->
            if (Eskalationsbedingung.erfuellt(beobachtungenErschoepft = true, belief, gate, eskalationsSchwelle)) {
                Zyklusergebnis.Eskaliert(
                    Eskalation(
                        belief,
                        gesammelt,
                        Eskalationsgrund.BeobachtungenErschoepft(
                            resthypothese = belief.resthypothese.wahrscheinlichkeit,
                            schwelle = eskalationsSchwelle,
                            gate = gate,
                        ),
                    ),
                )
            } else {
                Zyklusergebnis.Abgelehnt(
                    "günstige Beobachtungen erschöpft, Resthypothese unter θ_esc — kein günstiger Zug",
                    belief,
                )
            }

        // Freigabe wird bereits in Schritt 1 (am Gate) behandelt und erreicht diese Stelle nie.
        is GateEntscheidung.Freigabe -> error("Freigabe wird am Gate behandelt, nicht bei Erschöpfung")
    }
}
