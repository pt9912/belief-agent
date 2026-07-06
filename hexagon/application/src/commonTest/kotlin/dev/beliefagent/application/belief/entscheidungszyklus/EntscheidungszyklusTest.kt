package dev.beliefagent.application.belief.entscheidungszyklus

import dev.beliefagent.application.belief.aktualisieren.BeliefAktualisieren
import dev.beliefagent.application.belief.aktualisieren.ports.LlmPort
import dev.beliefagent.application.belief.aktualisieren.ports.UhrPort
import dev.beliefagent.application.belief.beobachtungwaehlen.BeobachtungWaehlen
import dev.beliefagent.application.belief.beobachtungwaehlen.ports.BeobachtungsAuswahlPort
import dev.beliefagent.application.belief.gaten.AktionGaten
import dev.beliefagent.application.belief.gaten.ports.HumanApprovalPort
import dev.beliefagent.domain.belief.Aktion
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.Erfolgswahrscheinlichkeit
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.Likelihoods
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Resthypothese
import dev.beliefagent.domain.belief.Wirkungsklasse
import dev.beliefagent.domain.belief.Zeitstempel
import dev.beliefagent.domain.eskalation.Budget
import dev.beliefagent.domain.eskalation.Eskalationsgrund
import dev.beliefagent.domain.voi.VoiKandidat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * E2E-Tests des Entscheidungszyklus (ARC-09, LH-FA-VOI-001, LH-FA-ESK, LH-QA-02/03)
 * gegen **Fake-Ports**: alle drei Ausgänge sammeln|handeln|eskalieren, beide
 * Eskalations-Auslöser (Beobachtungen erschöpft / Budget) und die garantierte
 * Terminierung.
 */
class EntscheidungszyklusTest {

    private fun beobachtung(text: String) = Beobachtung(Quelle.TEST, Zeitstempel(1L), Evidenz(text))
    private fun belief(a: Double, rest: Double) =
        BeliefState.of(listOf(Hypothese(HypotheseId("A"), a)), Resthypothese(rest))
    private fun aktion(klasse: Wirkungsklasse, erfolg: Double) =
        Aktion("test", klasse, Erfolgswahrscheinlichkeit(erfolg), listOf(beobachtung("stuetzt")))

    // Fake-LLM: Beobachtung, deren Text die Hypothesen-id enthält, bekommt hohe
    // Likelihood (verschiebt Masse zur Hypothese, senkt die Resthypothese).
    private val llm = object : LlmPort {
        override fun likelihoods(beobachtung: Beobachtung, prior: BeliefState): Likelihoods {
            val text = beobachtung.evidenz.beschreibung.lowercase()
            val pro = prior.hypothesen.associate { it.id to if (text.contains(it.id.wert.lowercase())) 0.9 else 0.1 }
            return Likelihoods(pro, resthypothese = 0.1)
        }
    }
    private val uhr = object : UhrPort {
        override fun jetzt(): Zeitstempel = Zeitstempel(1L)
    }
    private fun approval(ok: Boolean) = object : HumanApprovalPort {
        override fun freigegeben(aktion: Aktion): Boolean = ok
    }
    private fun auswahl(vararg kandidaten: VoiKandidat) = object : BeobachtungsAuswahlPort {
        override fun kandidaten(): List<VoiKandidat> = kandidaten.toList()
    }

    private fun zyklus(port: BeobachtungsAuswahlPort, approvalOk: Boolean = true) = Entscheidungszyklus(
        BeobachtungWaehlen(port),
        BeliefAktualisieren(llm, uhr),
        AktionGaten(approval(approvalOk)),
    )

    @Test
    fun handelt_sofort_wenn_das_gate_schon_frei_ist() { // Kontrast zu VOI-001: kein Sammeln nötig
        val ergebnis = zyklus(auswahl()).entscheide(
            aktion(Wirkungsklasse.ARBEITSBEREICH_LOKAL, erfolg = 0.9),
            belief(a = 0.9, rest = 0.1),
            Budget(),
        )
        assertTrue(ergebnis is Zyklusergebnis.Gehandelt)
    }

    @Test
    fun sammelt_dann_handelt_bei_hoher_unsicherheit_und_irreversibler_aktion() { // LH-FA-VOI-001
        // Start: Resthypothese hoch (0,95) -> Gate zu (irreversibel). Beobachtung "a"
        // senkt die Resthypothese, bis das Gate freigibt -> gesammelt, dann gehandelt.
        val kandidatA = VoiKandidat(beobachtung("a"), erwarteteDiskriminierung = 0.5, kosten = 1.0)
        val ergebnis = zyklus(auswahl(kandidatA), approvalOk = true).entscheide(
            aktion(Wirkungsklasse.EXTERN_WIRKSAM, erfolg = 0.95),
            belief(a = 0.05, rest = 0.95),
            Budget(maxSchritte = 5),
        )
        assertTrue(ergebnis is Zyklusergebnis.Gehandelt, "sollte nach Sammeln handeln")
        assertTrue(
            ergebnis.belief.resthypothese.wahrscheinlichkeit < 0.95,
            "der Zyklus hat vor dem Handeln Information gesammelt (Resthypothese gesenkt)",
        )
    }

    @Test
    fun eskaliert_bei_erschoepftem_budget_unabhaengig() { // LH-FA-ESK-004
        // Beobachtung "x" verschiebt nichts -> Gate bleibt zu -> Budget (2 Schritte) läuft aus.
        val kandidatX = VoiKandidat(beobachtung("x"), erwarteteDiskriminierung = 0.5, kosten = 1.0)
        val ergebnis = zyklus(auswahl(kandidatX)).entscheide(
            aktion(Wirkungsklasse.EXTERN_WIRKSAM, erfolg = 0.95),
            belief(a = 0.05, rest = 0.95),
            Budget(maxSchritte = 2),
        )
        assertTrue(ergebnis is Zyklusergebnis.Eskaliert)
        assertTrue(ergebnis.eskalation.grund is Eskalationsgrund.BudgetErschoepft)
    }

    @Test
    fun eskaliert_bei_erschoepften_beobachtungen_und_hoher_resthypothese() { // LH-FA-ESK-001
        val ergebnis = zyklus(auswahl()).entscheide( // keine Kandidaten
            aktion(Wirkungsklasse.EXTERN_WIRKSAM, erfolg = 0.95),
            belief(a = 0.05, rest = 0.95),
            Budget(),
        )
        assertTrue(ergebnis is Zyklusergebnis.Eskaliert)
        val grund = ergebnis.eskalation.grund
        assertTrue(grund is Eskalationsgrund.BeobachtungenErschoepft && grund.resthypothese == 0.95)
    }

    @Test
    fun lehnt_ab_bei_erschoepften_beobachtungen_und_niedriger_resthypothese() { // LH-FA-POL-002.a
        // Reversible Aktion, Erfolg unter Schwelle -> Gate lehnt ab; keine Beobachtung;
        // Resthypothese niedrig (0,1 < θ_esc) -> weder handeln noch eskalieren.
        val ergebnis = zyklus(auswahl()).entscheide(
            aktion(Wirkungsklasse.ARBEITSBEREICH_LOKAL, erfolg = 0.3),
            belief(a = 0.9, rest = 0.1),
            Budget(),
        )
        assertTrue(ergebnis is Zyklusergebnis.Abgelehnt)
    }

    @Test
    fun terminiert_garantiert_ueber_das_budget() { // LH-QA-02 (kein Endlos-Sammeln)
        // Gate nie frei (approval verweigert für irreversibel), Kandidat immer vorhanden:
        // nur das Budget beendet den Lauf.
        val kandidatA = VoiKandidat(beobachtung("a"), erwarteteDiskriminierung = 0.5, kosten = 1.0)
        val ergebnis = zyklus(auswahl(kandidatA), approvalOk = false).entscheide(
            aktion(Wirkungsklasse.EXTERN_WIRKSAM, erfolg = 0.99),
            belief(a = 0.05, rest = 0.95),
            Budget(maxSchritte = 3),
        )
        assertEquals(true, ergebnis is Zyklusergebnis.Eskaliert)
    }
}
