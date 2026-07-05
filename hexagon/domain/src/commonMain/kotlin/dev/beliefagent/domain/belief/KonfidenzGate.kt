package dev.beliefagent.domain.belief

/**
 * Ergebnis des Konfidenz-Gates (LH-FA-POL-001): genau **eine** von drei
 * Entscheidungen. Versiegelt, damit jeder Ausgang explizit behandelt werden muss.
 */
sealed interface GateEntscheidung {
    /** Aktion freigegeben — Konfidenz reicht und keine Sperre greift. */
    object Freigabe : GateEntscheidung

    /** Aktion abgelehnt — Erfolgswahrscheinlichkeit unter der Wirkungsklassen-Schwelle. */
    data class Ablehnung(val grund: String) : GateEntscheidung

    /** Aktion eskaliert — zu unsicher (Resthypothese über Sperr-Schwelle) für Irreversibles. */
    data class Eskalation(val grund: String) : GateEntscheidung
}

/**
 * Konfigurierbare Schwellen des Konfidenz-Gates (LH-FA-POL-003/007). Default-Werte
 * begründet in `ADR-0005`: Mindest-Erfolgswahrscheinlichkeit je [Wirkungsklasse]
 * (nur-lesend ohne wirksame Schwelle), **monoton steigend mit der Reichweite**,
 * plus die [resthypotheseSperrschwelle] für irreversible Aktionen (LH-FA-POL-005).
 *
 * Der Konstruktor erzwingt die **Sicherheits-Invarianten** fail-closed (`MR-003`):
 * alle Werte in `[0,1]`; die Erfolgs-Schwellen **monoton nicht-fallend**
 * (nur-lesend ≤ arbeitsbereich-lokal ≤ repository-wirksam ≤ extern-wirksam) — sonst
 * ließe sich die *gefährlichste* Klasse laxer konfigurieren als eine reversible;
 * und die [resthypotheseSperrschwelle] **echt < 1** — sonst wäre die POL-005-
 * Sperre (Vergleich `> Schwelle`) stumm abgeschaltet. Der Default ist an
 * [ReHypothesenAusloeser.STANDARD_SCHWELLWERT] gekoppelt (eine Quelle für „0,5").
 */
data class GateSchwellen(
    val nurLesend: Double = 0.0,
    val arbeitsbereichLokal: Double = 0.5,
    val repositoryWirksam: Double = 0.7,
    val externWirksam: Double = 0.9,
    val resthypotheseSperrschwelle: Double = ReHypothesenAusloeser.STANDARD_SCHWELLWERT,
) {
    init {
        listOf(nurLesend, arbeitsbereichLokal, repositoryWirksam, externWirksam, resthypotheseSperrschwelle)
            .forEach { require(it in 0.0..1.0) { "Gate-Schwelle muss in [0,1] liegen: $it" } }
        require(nurLesend <= arbeitsbereichLokal && arbeitsbereichLokal <= repositoryWirksam &&
            repositoryWirksam <= externWirksam) {
            "Erfolgs-Schwellen müssen monoton mit der Reichweite steigen " +
                "(nur-lesend ≤ arbeitsbereich-lokal ≤ repository-wirksam ≤ extern-wirksam): " +
                "$nurLesend/$arbeitsbereichLokal/$repositoryWirksam/$externWirksam"
        }
        require(resthypotheseSperrschwelle < 1.0) {
            "resthypotheseSperrschwelle muss < 1 sein, sonst ist die POL-005-Sperre " +
                "abgeschaltet: $resthypotheseSperrschwelle"
        }
    }

    /** Mindest-Erfolgswahrscheinlichkeit für die Freigabe einer Aktion dieser [klasse]. */
    fun mindestErfolg(klasse: Wirkungsklasse): Double = when (klasse) {
        Wirkungsklasse.NUR_LESEND -> nurLesend
        Wirkungsklasse.ARBEITSBEREICH_LOKAL -> arbeitsbereichLokal
        Wirkungsklasse.REPOSITORY_WIRKSAM -> repositoryWirksam
        Wirkungsklasse.EXTERN_WIRKSAM -> externWirksam
    }
}

/**
 * Konfidenz-Gate als reine Domänen-Regel (`ARC-03`, LH-FA-POL): gibt eine [Aktion]
 * frei, lehnt sie ab oder eskaliert (LH-FA-POL-001), geprüft gegen die
 * **Erfolgswahrscheinlichkeit der Aktion** (LH-FA-POL-002 — *nicht* die Top-
 * Hypothese) und die wirkungsklassen-abhängige Schwelle (LH-FA-POL-003/007).
 *
 * **Fail-safe (LH-FA-POL-005, LH-QA-02):** Für irreversible (extern-wirksame)
 * Aktionen wird die Resthypothese-Sperre **zuerst** geprüft — liegt die
 * Resthypothese echt über der Sperr-Schwelle, wird **eskaliert statt
 * freigegeben**, unabhängig von einer noch so hohen Erfolgswahrscheinlichkeit.
 * Deterministisch (LH-QA-03), Default-Schwellen `ADR-0005`.
 *
 * Die **Nicht-Umgehbarkeit** (LH-FA-POL-006) und die **menschliche Freigabe** für
 * extern-wirksame Aktionen (LH-FA-POL-004) sind Sache des application-Schritts
 * *aktion-gaten* (slice-013), nicht dieser Regel.
 */
object KonfidenzGate {

    fun bewerte(
        aktion: Aktion,
        belief: BeliefState,
        schwellen: GateSchwellen = GateSchwellen(),
    ): GateEntscheidung {
        // LH-FA-POL-005 (fail-safe, ZUERST — bewusst vor der Schwellenprüfung):
        // irreversible Aktion bei hoher Resthypothese wird nie freigegeben. Greift
        // auch bei niedriger Erfolgs-P, dann überschattet Eskalation die Ablehnung
        // (Unsicherheit → Mensch; Vorrang durch Test gepinnt).
        if (aktion.wirkungsklasse.irreversibel &&
            belief.resthypothese.wahrscheinlichkeit > schwellen.resthypotheseSperrschwelle
        ) {
            return GateEntscheidung.Eskalation(
                "Resthypothese ${belief.resthypothese.wahrscheinlichkeit} über Sperr-Schwelle " +
                    "${schwellen.resthypotheseSperrschwelle}: irreversible Aktion gesperrt (LH-FA-POL-005)",
            )
        }
        // LH-FA-POL-002/003: Erfolgswahrscheinlichkeit gegen die Wirkungsklassen-Schwelle.
        val schwelle = schwellen.mindestErfolg(aktion.wirkungsklasse)
        return if (aktion.erfolgswahrscheinlichkeit.wert >= schwelle) {
            GateEntscheidung.Freigabe
        } else {
            GateEntscheidung.Ablehnung(
                "Erfolgswahrscheinlichkeit ${aktion.erfolgswahrscheinlichkeit.wert} unter Schwelle " +
                    "$schwelle für ${aktion.wirkungsklasse} (LH-FA-POL-002/003)",
            )
        }
    }
}
