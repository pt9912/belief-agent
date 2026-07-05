package dev.beliefagent.domain.belief

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Deterministische Tests des Konfidenz-Gates (LH-FA-POL-001/002/003/005/007,
 * LH-QA-03). Sicherheitskritisch (`MR-003`) — daher jeder Ausgang und die
 * Grenzfälle der Resthypothese-Sperre.
 */
class KonfidenzGateTest {

    private fun belief(rest: Double) =
        BeliefState.of(listOf(Hypothese(HypotheseId("A"), 1.0 - rest)), Resthypothese(rest))

    private fun aktion(klasse: Wirkungsklasse, erfolg: Double) = Aktion(
        "test", klasse, Erfolgswahrscheinlichkeit(erfolg),
        listOf(Beobachtung(Quelle.TEST, Zeitstempel(1L), Evidenz("e"))),
    )

    @Test
    fun nur_lesend_wird_immer_freigegeben() { // LH-FA-POL-003 (keine wirksame Schwelle)
        assertEquals(GateEntscheidung.Freigabe, KonfidenzGate.bewerte(aktion(Wirkungsklasse.NUR_LESEND, 0.0), belief(0.1)))
    }

    @Test
    fun hohe_erfolgswahrscheinlichkeit_wird_freigegeben() {
        assertEquals(GateEntscheidung.Freigabe, KonfidenzGate.bewerte(aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.95), belief(0.1)))
    }

    @Test
    fun niedrige_erfolgswahrscheinlichkeit_wird_abgelehnt() { // LH-FA-POL-002
        assertTrue(KonfidenzGate.bewerte(aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.5), belief(0.1)) is GateEntscheidung.Ablehnung)
    }

    @Test
    fun schwelle_haengt_von_wirkungsklasse_ab() { // LH-FA-POL-003
        // Erfolg 0,6: über arbeitsbereich-lokal (0,5), unter repository-wirksam (0,7).
        assertEquals(GateEntscheidung.Freigabe, KonfidenzGate.bewerte(aktion(Wirkungsklasse.ARBEITSBEREICH_LOKAL, 0.6), belief(0.1)))
        assertTrue(KonfidenzGate.bewerte(aktion(Wirkungsklasse.REPOSITORY_WIRKSAM, 0.6), belief(0.1)) is GateEntscheidung.Ablehnung)
    }

    @Test
    fun extern_wirksam_bei_hoher_resthypothese_wird_eskaliert_trotz_hoher_erfolgs_p() { // LH-FA-POL-005 (Sicherheitskern)
        val e = KonfidenzGate.bewerte(aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.99), belief(0.6)) // 0,6 > 0,5
        assertTrue(e is GateEntscheidung.Eskalation, "hohe Erfolgs-P darf die Resthypothese-Sperre nicht überstimmen")
    }

    @Test
    fun resthypothese_sperre_gilt_nur_fuer_irreversible() { // LH-FA-POL-005
        // repository-wirksam ist reversibel -> keine Sperre, normale Schwellenprüfung (0,9 >= 0,7).
        assertEquals(GateEntscheidung.Freigabe, KonfidenzGate.bewerte(aktion(Wirkungsklasse.REPOSITORY_WIRKSAM, 0.9), belief(0.6)))
    }

    @Test
    fun resthypothese_genau_auf_schwelle_sperrt_nicht() { // Grenzfall: echt > (nicht >=)
        assertEquals(GateEntscheidung.Freigabe, KonfidenzGate.bewerte(aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.95), belief(0.5)))
    }

    @Test
    fun gate_liefert_genau_eine_von_drei_entscheidungen() { // LH-FA-POL-001
        assertTrue(KonfidenzGate.bewerte(aktion(Wirkungsklasse.NUR_LESEND, 0.0), belief(0.1)) is GateEntscheidung.Freigabe)
        assertTrue(KonfidenzGate.bewerte(aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.1), belief(0.1)) is GateEntscheidung.Ablehnung)
        assertTrue(KonfidenzGate.bewerte(aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.99), belief(0.9)) is GateEntscheidung.Eskalation)
    }

    @Test
    fun schwellen_sind_konfigurierbar() { // LH-FA-POL-007
        val streng = GateSchwellen(externWirksam = 0.99)
        assertTrue(KonfidenzGate.bewerte(aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.95), belief(0.1), streng) is GateEntscheidung.Ablehnung)
    }

    @Test
    fun ungueltige_schwelle_wird_abgewiesen() {
        assertFailsWith<IllegalArgumentException> { GateSchwellen(externWirksam = 1.5) }
    }

    @Test
    fun nicht_monotone_schwellen_werden_abgewiesen() { // Review #1: Safety-Inversion
        // extern-wirksam laxer als repository-wirksam -> gefährlichste Klasse zu leicht.
        assertFailsWith<IllegalArgumentException> {
            GateSchwellen(repositoryWirksam = 0.7, externWirksam = 0.1)
        }
    }

    @Test
    fun sperrschwelle_von_eins_wird_abgewiesen() { // Review #2: POL-005-Sperre nicht abschaltbar
        assertFailsWith<IllegalArgumentException> { GateSchwellen(resthypotheseSperrschwelle = 1.0) }
    }

    @Test
    fun irreversibel_hohe_resthypothese_niedrige_erfolgs_p_wird_eskaliert() { // Review #3: Vorrang
        // Beide Bedingungen greifen (Sperre UND unter Schwelle) -> Eskalation hat Vorrang.
        assertTrue(
            KonfidenzGate.bewerte(aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.05), belief(0.6)) is GateEntscheidung.Eskalation,
        )
    }

    @Test
    fun gruende_belegen_die_entscheidung() { // Review #4: Audit-Grund geprüft
        val ablehnung = KonfidenzGate.bewerte(aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.1), belief(0.1))
        assertTrue(ablehnung is GateEntscheidung.Ablehnung && "LH-FA-POL-002/003" in ablehnung.grund)
        val eskalation = KonfidenzGate.bewerte(aktion(Wirkungsklasse.EXTERN_WIRKSAM, 0.99), belief(0.9))
        assertTrue(eskalation is GateEntscheidung.Eskalation && "LH-FA-POL-005" in eskalation.grund)
    }
}
