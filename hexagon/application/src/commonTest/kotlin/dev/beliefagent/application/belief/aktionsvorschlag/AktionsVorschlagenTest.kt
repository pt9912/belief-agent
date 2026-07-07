package dev.beliefagent.application.belief.aktionsvorschlag

import dev.beliefagent.application.belief.aktionsvorschlag.dto.AktionsVorschlag
import dev.beliefagent.application.belief.aktionsvorschlag.ports.AktionsVorschlagsPort
import dev.beliefagent.application.belief.ports.ExternalisierteKonfidenz
import dev.beliefagent.application.belief.ports.KonfidenzPort
import dev.beliefagent.application.belief.ports.KonfidenzReferenz
import dev.beliefagent.application.ports.AuditPort
import dev.beliefagent.domain.belief.AktionVorgeschlagen
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.Ereignis
import dev.beliefagent.domain.belief.EreignisProtokoll
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.EvidenzReferenz
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.KonfidenzExternalisiert
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Resthypothese
import dev.beliefagent.domain.belief.Wirkungsklasse
import dev.beliefagent.domain.belief.Zeitstempel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** LH-FA-LLM-002/003, LH-FA-ACT-001..004: Aktionsvorschlaege bleiben vor dem Gate. */
class AktionsVorschlagenTest {

    private val zeit = Zeitstempel(10L)
    private val evidenzReferenz = EvidenzReferenz("fake:evidenz:regression")
    private val evidenz = Beobachtung(Quelle.LOG, Zeitstempel(9L), Evidenz("regression bestaetigt"))
    private val konfidenzReferenz = KonfidenzReferenz("fake:konfidenz:aktion-deploy")

    @Test
    fun gueltiger_vorschlag_wird_konfidenzgebunden_und_externalisiert() {
        val konfidenzen = SpeichernderKonfidenzPort()
        val audit = SpeichernderAuditPort()
        val useCase = useCase(
            vorschlag(
                beschreibung = "Deploy ausloesen",
                wirkungsklasse = "EXTERN_WIRKSAM",
                pSuccess = 0.96,
            ),
            konfidenzen = konfidenzen,
            audit = audit,
        )

        val ergebnis = useCase.ausfuehren(befehl())

        val vorschlag = ergebnis.single()
        assertEquals(HypotheseId("regression"), vorschlag.hypotheseId)
        assertEquals("Deploy ausloesen", vorschlag.aktion.beschreibung)
        assertEquals(Wirkungsklasse.EXTERN_WIRKSAM, vorschlag.aktion.wirkungsklasse)
        assertEquals(listOf(evidenz), vorschlag.aktion.stuetzendeEvidenz)
        assertEquals(konfidenzReferenz, vorschlag.aktion.konfidenzReferenz)
        assertEquals(0.96, konfidenzen.lade(konfidenzReferenz).single().wert.wert)
        assertTrue(audit.lade().ereignisse[0] is KonfidenzExternalisiert)
        assertEquals(AktionVorgeschlagen(zeit, "Deploy ausloesen"), audit.lade().ereignisse[1])
    }

    @Test
    fun leere_port_rueckgabe_liefert_keine_gate_faehigen_vorschlaege() {
        val ergebnis = useCase().ausfuehren(befehl())

        assertEquals(emptyList(), ergebnis)
    }

    @Test
    fun unbekannte_hypothese_wird_verworfen_ohne_konfidenz_zu_externalisieren() {
        val konfidenzen = SpeichernderKonfidenzPort()
        val audit = SpeichernderAuditPort()
        val ergebnis = useCase(
            vorschlag(hypotheseId = "flaky"),
            konfidenzen = konfidenzen,
            audit = audit,
        ).ausfuehren(befehl())

        assertEquals(emptyList(), ergebnis)
        assertTrue(konfidenzen.lade(konfidenzReferenz).isEmpty())
        assertTrue(audit.lade().istLeer())
    }

    @Test
    fun ungueltige_wirkungsklasse_wird_verworfen_ohne_gate_freigabe() {
        val konfidenzen = SpeichernderKonfidenzPort()
        val ergebnis = useCase(vorschlag(wirkungsklasse = "UNBEKANNT"), konfidenzen = konfidenzen)
            .ausfuehren(befehl())

        assertEquals(emptyList(), ergebnis)
        assertTrue(konfidenzen.lade(konfidenzReferenz).isEmpty())
    }

    @Test
    fun fehlende_evidenz_wird_verworfen_ohne_konfidenz_zu_externalisieren() {
        val konfidenzen = SpeichernderKonfidenzPort()
        val ergebnis = useCase(
            vorschlag(stuetzendeEvidenz = listOf("fake:evidenz:unbekannt")),
            konfidenzen = konfidenzen,
        ).ausfuehren(befehl())

        assertEquals(emptyList(), ergebnis)
        assertTrue(konfidenzen.lade(konfidenzReferenz).isEmpty())
    }

    @Test
    fun ungueltige_p_success_wird_nicht_gate_faehig() {
        val konfidenzen = SpeichernderKonfidenzPort()
        val ergebnis = useCase(vorschlag(pSuccess = Double.NaN), konfidenzen = konfidenzen)
            .ausfuehren(befehl())

        assertEquals(emptyList(), ergebnis)
        assertTrue(konfidenzen.lade(konfidenzReferenz).isEmpty())
    }

    @Test
    fun doppelte_konfidenzreferenz_wird_fail_safe_verworfen() {
        val konfidenzen = SpeichernderKonfidenzPort()
        val ergebnis = useCase(
            vorschlag(beschreibung = "erste Aktion"),
            vorschlag(beschreibung = "zweite Aktion"),
            konfidenzen = konfidenzen,
        ).ausfuehren(befehl())

        assertEquals(emptyList(), ergebnis)
        assertTrue(konfidenzen.lade(konfidenzReferenz).isEmpty())
    }

    private fun useCase(
        vararg vorschlaege: AktionsVorschlag,
        konfidenzen: SpeichernderKonfidenzPort = SpeichernderKonfidenzPort(),
        audit: SpeichernderAuditPort = SpeichernderAuditPort(),
    ) = AktionsVorschlagen(
        port = FesterAktionsVorschlagsPort(vorschlaege.toList()),
        konfidenzen = konfidenzen,
        audit = audit,
    )

    private fun befehl() = AktionsVorschlagenBefehl(
        belief = BeliefState.of(
            listOf(Hypothese(HypotheseId("regression"), 0.8)),
            Resthypothese(0.2),
        ),
        bekannteEvidenz = mapOf(evidenzReferenz to evidenz),
        zeitstempel = zeit,
    )

    private fun vorschlag(
        beschreibung: String = "Arbeitsbaum pruefen",
        hypotheseId: String = "regression",
        wirkungsklasse: String = "ARBEITSBEREICH_LOKAL",
        pSuccess: Double = 0.8,
        konfidenzReferenz: String = this.konfidenzReferenz.wert,
        stuetzendeEvidenz: List<String> = listOf(evidenzReferenz.wert),
    ) = AktionsVorschlag(
        beschreibung = beschreibung,
        hypotheseId = hypotheseId,
        wirkungsklasse = wirkungsklasse,
        pSuccess = pSuccess,
        konfidenzReferenz = konfidenzReferenz,
        stuetzendeEvidenz = stuetzendeEvidenz,
    )

    private class FesterAktionsVorschlagsPort(
        private val vorschlaege: List<AktionsVorschlag>,
    ) : AktionsVorschlagsPort {
        override fun vorschlaege(belief: BeliefState): List<AktionsVorschlag> = vorschlaege
    }

    private class SpeichernderKonfidenzPort : KonfidenzPort {
        private val eintraege = mutableListOf<ExternalisierteKonfidenz>()

        override fun anhaengen(konfidenz: ExternalisierteKonfidenz) {
            eintraege += konfidenz
        }

        override fun lade(referenz: KonfidenzReferenz): List<ExternalisierteKonfidenz> =
            eintraege.filter { it.referenz == referenz }
    }

    private class SpeichernderAuditPort : AuditPort {
        private var protokoll = EreignisProtokoll.LEER

        override fun anhaengen(ereignis: Ereignis) {
            protokoll = protokoll.append(ereignis)
        }

        override fun lade(): EreignisProtokoll = protokoll
    }
}
