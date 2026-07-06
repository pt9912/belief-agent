package dev.beliefagent.application.belief.beobachtungwaehlen

import dev.beliefagent.application.belief.beobachtungwaehlen.ports.BeobachtungsAuswahlPort
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Zeitstempel
import dev.beliefagent.domain.voi.VoiKandidat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Deterministische Tests des Use-Cases *beobachtung-waehlen* (LH-FA-VOI-002,
 * LH-QA-03). Der Port wird durch einen In-Test-Stub ersetzt (application-Test hängt
 * nicht am Adapter-Modul — Abhängigkeitsrichtung nach innen, ADR-0003).
 */
class BeobachtungWaehlenTest {

    private val beobachtung = Beobachtung(Quelle.TEST, Zeitstempel(1L), Evidenz("e"))
    private fun kandidat(diskriminierung: Double, kosten: Double) =
        VoiKandidat(beobachtung, diskriminierung, kosten)

    private fun port(vararg kandidaten: VoiKandidat) = object : BeobachtungsAuswahlPort {
        override fun kandidaten(): List<VoiKandidat> = kandidaten.toList()
    }

    @Test
    fun waehlt_den_informativsten_kandidaten_aus_dem_port() { // LH-FA-VOI-002
        val schwach = kandidat(diskriminierung = 0.2, kosten = 1.0)
        val stark = kandidat(diskriminierung = 0.6, kosten = 1.0)
        assertSame(stark, BeobachtungWaehlen(port(schwach, stark)).waehle())
    }

    @Test
    fun leere_kandidaten_liefern_null() { // „keine günstige Beobachtung" -> Signal an slice-017
        assertNull(BeobachtungWaehlen(port()).waehle())
    }

    @Test
    fun auswahl_ist_deterministisch() { // LH-QA-03
        val useCase = BeobachtungWaehlen(port(kandidat(0.2, 2.0), kandidat(0.6, 2.0)))
        assertEquals(useCase.waehle(), useCase.waehle())
    }
}
