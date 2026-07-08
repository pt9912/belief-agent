package dev.beliefagent.application.belief.entscheidungszyklus

import dev.beliefagent.application.belief.aktualisieren.BeliefAktualisieren
import dev.beliefagent.application.belief.aktualisieren.ports.LlmPort
import dev.beliefagent.application.belief.aktualisieren.ports.UhrPort
import dev.beliefagent.application.belief.beobachtungwaehlen.BeobachtungWaehlen
import dev.beliefagent.application.belief.beobachtungwaehlen.ports.BeobachtungsAuswahlPort
import dev.beliefagent.application.belief.gaten.AktionGaten
import dev.beliefagent.application.belief.gaten.ports.ApprovalAnfrage
import dev.beliefagent.application.belief.gaten.ports.HumanApprovalPort
import dev.beliefagent.application.belief.ports.ExternalisierteKonfidenz
import dev.beliefagent.application.belief.ports.KonfidenzPort
import dev.beliefagent.application.belief.ports.KonfidenzQuelle
import dev.beliefagent.application.belief.ports.KonfidenzReferenz
import dev.beliefagent.application.belief.ports.KonfidenzVersion
import dev.beliefagent.application.belief.ports.ModellKonfidenz
import dev.beliefagent.application.belief.ports.OverrideBegruendung
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * E2E-Tests des Entscheidungszyklus (ARC-09, LH-FA-VOI-001, LH-FA-ESK, LH-QA-02/03)
 * gegen **Fake-Ports**: alle Ausgänge sammeln|handeln|eskalieren|ablehnen, alle drei
 * Eskalations-Gründe (Gate-Eskalation inkl. fehlende Freigabe / Beobachtungen
 * erschöpft / Budget) und die garantierte Terminierung. Jeder Kandidat wird
 * höchstens **einmal** beobachtet (keine Scheingewissheit).
 */
class EntscheidungszyklusTest {

    private fun beobachtung(text: String) = Beobachtung(Quelle.TEST, Zeitstempel(1L), Evidenz(text))
    private fun belief(a: Double, rest: Double) =
        BeliefState.of(listOf(Hypothese(HypotheseId("A"), a)), Resthypothese(rest))
    private fun aktion(klasse: Wirkungsklasse, erfolg: Double) =
        Aktion("test", klasse, Erfolgswahrscheinlichkeit(erfolg), listOf(beobachtung("stuetzt")))
    private fun kandidat(text: String, disk: Double) = VoiKandidat(beobachtung(text), disk, kosten = 1.0)
    private val konfidenzReferenz = KonfidenzReferenz("fake:konfidenz:aktion")
    private val konfidenzQuelle = KonfidenzQuelle("zyklus-test")

    // Fake-LLM: Beobachtung, deren Text die Hypothesen-id ("a") enthält, verschiebt
    // Masse zur Hypothese (senkt die Resthypothese); sonst bleibt der Belief gleich.
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
        override fun freigegeben(anfrage: ApprovalAnfrage): Boolean = ok
    }
    private fun auswahl(vararg kandidaten: VoiKandidat) = object : BeobachtungsAuswahlPort {
        override fun kandidaten(belief: BeliefState): List<VoiKandidat> = kandidaten.toList()
    }

    private fun zyklus(port: BeobachtungsAuswahlPort, approvalOk: Boolean = true) = Entscheidungszyklus(
        BeobachtungWaehlen(port),
        BeliefAktualisieren(llm, uhr),
        AktionGaten(approval(approvalOk)),
    )

    private fun konfidenzgebundenerZyklus(
        vararg eintraege: ExternalisierteKonfidenz,
        approvalOk: Boolean = true,
    ) = KonfidenzgebundenerEntscheidungszyklus(
        zyklus(auswahl(), approvalOk),
        SpeichernderKonfidenzPort(eintraege.toList()),
    )

    private fun konfidenz(wert: Double, version: Int, begruendung: String? = null) =
        ExternalisierteKonfidenz(
            referenz = konfidenzReferenz,
            wert = ModellKonfidenz(wert),
            quelle = konfidenzQuelle,
            version = KonfidenzVersion(version),
            overrideBegruendung = begruendung?.let { OverrideBegruendung(it) },
        )

    private fun konfidenzgebundeneAktion(klasse: Wirkungsklasse = Wirkungsklasse.ARBEITSBEREICH_LOKAL) =
        KonfidenzgebundeneAktion(
            beschreibung = "konfidenzgebundene Aktion",
            wirkungsklasse = klasse,
            stuetzendeEvidenz = listOf(beobachtung("stuetzt")),
            konfidenzReferenz = konfidenzReferenz,
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
        // Resthypothese hoch (0,95) -> Gate zu. Zwei **verschiedene** "a"-Beobachtungen
        // senken sie, bis das Gate freigibt -> gesammelt, dann gehandelt.
        val ergebnis = zyklus(
            auswahl(kandidat("a1", disk = 0.5), kandidat("a2", disk = 0.4), kandidat("a3", disk = 0.3)),
            approvalOk = true,
        ).entscheide(aktion(Wirkungsklasse.EXTERN_WIRKSAM, erfolg = 0.95), belief(a = 0.05, rest = 0.95), Budget(maxSchritte = 5))
        assertTrue(ergebnis is Zyklusergebnis.Gehandelt, "sollte nach Sammeln handeln")
        assertTrue(ergebnis.belief.resthypothese.wahrscheinlichkeit < 0.95, "Information gesammelt (Resthypothese gesenkt)")
    }

    @Test
    fun eskaliert_bei_erschoepften_beobachtungen_und_resthypothese_sperre() { // LH-FA-ESK-001 / POL-005
        // Uninformative Beobachtungen ("x") senken die Resthypothese nicht -> Gate bleibt
        // per Resthypothese-Sperre eskaliert; nach Verbrauch aller Kandidaten -> Eskalation.
        val ergebnis = zyklus(auswahl(kandidat("x1", 0.5), kandidat("x2", 0.4))).entscheide(
            aktion(Wirkungsklasse.EXTERN_WIRKSAM, erfolg = 0.95), belief(a = 0.05, rest = 0.95), Budget(maxSchritte = 5),
        )
        assertTrue(ergebnis is Zyklusergebnis.Eskaliert && ergebnis.eskalation.grund is Eskalationsgrund.GateEskalation)
    }

    @Test
    fun eskaliert_bei_fehlender_menschlicher_freigabe_trotz_niedriger_resthypothese() { // F1: LH-FA-POL-004
        // Konfidenz bestanden (Resthypothese 0,1), aber keine Freigabe -> Gate eskaliert.
        // Darf NICHT still abgelehnt werden, obwohl die Resthypothese unter θ_esc liegt.
        val ergebnis = zyklus(auswahl(), approvalOk = false).entscheide(
            aktion(Wirkungsklasse.EXTERN_WIRKSAM, erfolg = 0.95), belief(a = 0.9, rest = 0.1), Budget(),
        )
        assertTrue(ergebnis is Zyklusergebnis.Eskaliert && ergebnis.eskalation.grund is Eskalationsgrund.GateEskalation)
    }

    @Test
    fun eskaliert_bei_erschoepften_beobachtungen_und_hoher_resthypothese_nach_ablehnung() { // LH-FA-ESK-001
        val ergebnis = zyklus(auswahl()).entscheide( // keine Kandidaten; Gate lehnt ab (reversibel, Erfolg unter Schwelle)
            aktion(Wirkungsklasse.ARBEITSBEREICH_LOKAL, erfolg = 0.3), belief(a = 0.4, rest = 0.6), Budget(),
        )
        assertTrue(ergebnis is Zyklusergebnis.Eskaliert && ergebnis.eskalation.grund is Eskalationsgrund.BeobachtungenErschoepft)
    }

    @Test
    fun lehnt_ab_bei_erschoepften_beobachtungen_und_niedriger_resthypothese() { // LH-FA-POL-002.a
        val ergebnis = zyklus(auswahl()).entscheide(
            aktion(Wirkungsklasse.ARBEITSBEREICH_LOKAL, erfolg = 0.3), belief(a = 0.9, rest = 0.1), Budget(),
        )
        assertTrue(ergebnis is Zyklusergebnis.Abgelehnt)
    }

    @Test
    fun eskaliert_und_terminiert_bei_erschoepftem_budget() { // LH-FA-ESK-004 / LH-QA-02
        // Mehr Kandidaten als Budget, Gate nie frei -> nur das Budget beendet den Lauf.
        val ergebnis = zyklus(auswahl(kandidat("x1", 0.5), kandidat("x2", 0.4), kandidat("x3", 0.3))).entscheide(
            aktion(Wirkungsklasse.EXTERN_WIRKSAM, erfolg = 0.95), belief(a = 0.05, rest = 0.95), Budget(maxSchritte = 2),
        )
        assertTrue(ergebnis is Zyklusergebnis.Eskaliert && ergebnis.eskalation.grund is Eskalationsgrund.BudgetErschoepft)
    }

    @Test
    fun externalisierte_konfidenz_wird_im_gate_pfad_konsumiert() { // LH-FA-LLM-003 / POL-006
        val ergebnis = konfidenzgebundenerZyklus(konfidenz(0.8, 1)).entscheide(
            konfidenzgebundeneAktion(),
            belief(a = 0.9, rest = 0.1),
            Budget(),
        )

        val gehandelt = assertIs<Zyklusergebnis.Gehandelt>(ergebnis)
        assertEquals(0.8, gehandelt.freigabe.aktion.erfolgswahrscheinlichkeit.wert)
    }

    @Test
    fun override_steuert_den_neuesten_gate_wert_ohne_historie_zu_mutieren() { // LH-FA-AUD-001/003
        val port = SpeichernderKonfidenzPort(
            listOf(
                konfidenz(0.9, 1),
                konfidenz(0.3, 2, "Golden-Set-Korrektur"),
            ),
        )
        val ergebnis = KonfidenzgebundenerEntscheidungszyklus(zyklus(auswahl()), port).entscheide(
            konfidenzgebundeneAktion(),
            belief(a = 0.9, rest = 0.1),
            Budget(),
        )

        assertTrue(ergebnis is Zyklusergebnis.Abgelehnt)
        assertEquals(listOf(konfidenz(0.9, 1), konfidenz(0.3, 2, "Golden-Set-Korrektur")), port.lade(konfidenzReferenz))
    }

    @Test
    fun fehlende_externalisierte_konfidenz_ist_fail_safe_nicht_gate_faehig() { // LH-FA-LLM-003
        val ergebnis = konfidenzgebundenerZyklus().entscheide(
            konfidenzgebundeneAktion(),
            belief(a = 0.9, rest = 0.1),
            Budget(),
        )

        val abgelehnt = assertIs<Zyklusergebnis.Abgelehnt>(ergebnis)
        assertTrue(abgelehnt.grund.contains("externalisierte Konfidenz fehlt"))
    }

    @Test
    fun ungueltige_konfidenz_historie_ist_fail_safe_nicht_gate_faehig() { // LH-FA-AUD-001
        val ergebnis = konfidenzgebundenerZyklus(konfidenz(0.9, 2)).entscheide(
            konfidenzgebundeneAktion(),
            belief(a = 0.9, rest = 0.1),
            Budget(),
        )

        val abgelehnt = assertIs<Zyklusergebnis.Abgelehnt>(ergebnis)
        assertTrue(abgelehnt.grund.contains("nicht append-only gueltig"))
    }

    private class SpeichernderKonfidenzPort(
        private val eintraege: List<ExternalisierteKonfidenz>,
    ) : KonfidenzPort {
        override fun anhaengen(konfidenz: ExternalisierteKonfidenz) {
            error("Zyklusbindung darf Konfidenzen nicht mutieren")
        }

        override fun lade(referenz: KonfidenzReferenz): List<ExternalisierteKonfidenz> =
            eintraege.filter { it.referenz == referenz }
    }
}
