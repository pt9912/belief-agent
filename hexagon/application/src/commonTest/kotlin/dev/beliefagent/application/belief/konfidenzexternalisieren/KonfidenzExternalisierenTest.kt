package dev.beliefagent.application.belief.konfidenzexternalisieren

import dev.beliefagent.application.belief.ports.ExternalisierteKonfidenz
import dev.beliefagent.application.belief.ports.KonfidenzPort
import dev.beliefagent.application.belief.ports.KonfidenzQuelle
import dev.beliefagent.application.belief.ports.KonfidenzReferenz
import dev.beliefagent.application.belief.ports.KonfidenzVersion
import dev.beliefagent.application.belief.ports.ModellKonfidenz
import dev.beliefagent.application.belief.ports.OverrideBegruendung
import dev.beliefagent.application.ports.AuditPort
import dev.beliefagent.domain.belief.Ereignis
import dev.beliefagent.domain.belief.EreignisProtokoll
import dev.beliefagent.domain.belief.KonfidenzExternalisiert
import dev.beliefagent.domain.belief.KonfidenzUeberschrieben
import dev.beliefagent.domain.belief.Zeitstempel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** LH-FA-LLM-003 / LH-FA-AUD-001/003: Konfidenz wird explizit und append-only. */
class KonfidenzExternalisierenTest {

    private val referenz = KonfidenzReferenz("llm:aktion:deploy")
    private val quelle = KonfidenzQuelle("llm-action-fake")

    @Test
    fun externalisiert_rohe_modell_konfidenz_in_expliziten_contract() {
        val port = SpeichernderKonfidenzPort()
        val audit = SpeichernderAuditPort()
        val useCase = KonfidenzExternalisieren(port, audit)

        val ergebnis = useCase.externalisieren(
            KonfidenzExternalisierenBefehl(referenz, roheKonfidenz = 0.82, quelle, Zeitstempel(10L)),
        )

        assertEquals(ExternalisierteKonfidenz(referenz, ModellKonfidenz(0.82), quelle, KonfidenzVersion(1)), ergebnis)
        assertEquals(listOf(ergebnis), port.lade(referenz))
        assertEquals(
            KonfidenzExternalisiert(Zeitstempel(10L), "llm:aktion:deploy", 0.82, "llm-action-fake", 1),
            audit.ereignisse.single(),
        )
    }

    @Test
    fun override_erzeugt_neue_version_und_neues_audit_ereignis() {
        val port = SpeichernderKonfidenzPort()
        val audit = SpeichernderAuditPort()
        val useCase = KonfidenzExternalisieren(port, audit)
        val initial = useCase.externalisieren(
            KonfidenzExternalisierenBefehl(referenz, roheKonfidenz = 0.82, quelle, Zeitstempel(10L)),
        )

        val override = useCase.ueberschreiben(
            KonfidenzOverrideBefehl(
                referenz,
                neueKonfidenz = 0.55,
                begruendung = OverrideBegruendung("Golden-Set-Korrektur"),
                zeitstempel = Zeitstempel(11L),
            ),
        )

        assertEquals(ModellKonfidenz(0.82), initial.wert)
        assertEquals(ModellKonfidenz(0.55), override.wert)
        assertEquals(KonfidenzVersion(2), override.version)
        assertEquals(listOf(initial, override), port.lade(referenz))
        assertEquals(2, audit.ereignisse.size)
        assertEquals(
            KonfidenzUeberschrieben(
                Zeitstempel(11L),
                "llm:aktion:deploy",
                alterWert = 0.82,
                neuerWert = 0.55,
                begruendung = "Golden-Set-Korrektur",
                version = 2,
            ),
            audit.ereignisse.last(),
        )
    }

    @Test
    fun ungueltige_roh_konfidenz_wird_abgelehnt_ohne_seiteneffekt() {
        val port = SpeichernderKonfidenzPort()
        val audit = SpeichernderAuditPort()
        val useCase = KonfidenzExternalisieren(port, audit)

        assertFailsWith<IllegalArgumentException> {
            useCase.externalisieren(
                KonfidenzExternalisierenBefehl(referenz, roheKonfidenz = Double.NaN, quelle, Zeitstempel(10L)),
            )
        }

        assertTrue(port.lade(referenz).isEmpty())
        assertTrue(audit.ereignisse.isEmpty())
    }

    @Test
    fun override_ohne_bestehenden_eintrag_wird_abgelehnt() {
        val port = SpeichernderKonfidenzPort()
        val audit = SpeichernderAuditPort()
        val useCase = KonfidenzExternalisieren(port, audit)

        assertFailsWith<IllegalArgumentException> {
            useCase.ueberschreiben(
                KonfidenzOverrideBefehl(
                    referenz,
                    neueKonfidenz = 0.55,
                    begruendung = OverrideBegruendung("Golden-Set-Korrektur"),
                    zeitstempel = Zeitstempel(11L),
                ),
            )
        }

        assertTrue(port.lade(referenz).isEmpty())
        assertTrue(audit.ereignisse.isEmpty())
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
        val ereignisse = mutableListOf<Ereignis>()

        override fun anhaengen(ereignis: Ereignis) {
            ereignisse += ereignis
        }

        override fun lade(): EreignisProtokoll = EreignisProtokoll.von(ereignisse)
    }
}
