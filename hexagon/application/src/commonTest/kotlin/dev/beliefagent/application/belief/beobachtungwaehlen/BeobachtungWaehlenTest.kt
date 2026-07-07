package dev.beliefagent.application.belief.beobachtungwaehlen

import dev.beliefagent.application.belief.beobachtungwaehlen.ports.BeobachtungsAuswahlPort
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Resthypothese
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
    private val belief = belief(a = 0.6, b = 0.3, rest = 0.1)

    private fun kandidat(diskriminierung: Double, kosten: Double) =
        VoiKandidat(beobachtung, diskriminierung, kosten)

    private fun port(vararg kandidaten: VoiKandidat) = object : BeobachtungsAuswahlPort {
        override fun kandidaten(belief: BeliefState): List<VoiKandidat> = kandidaten.toList()
    }

    private fun belief(a: Double, b: Double, rest: Double) = BeliefState.of(
        listOf(
            Hypothese(HypotheseId("A"), a),
            Hypothese(HypotheseId("B"), b),
        ),
        Resthypothese(rest),
    )

    private fun beliefAwarePort(
        kandidatenFuerBelief: (BeliefState) -> List<VoiKandidat>,
    ) = object : BeobachtungsAuswahlPort {
        override fun kandidaten(belief: BeliefState): List<VoiKandidat> = kandidatenFuerBelief(belief)
    }

    @Test
    fun waehlt_den_informativsten_kandidaten_aus_dem_port() { // LH-FA-VOI-002
        val schwach = kandidat(diskriminierung = 0.2, kosten = 1.0)
        val stark = kandidat(diskriminierung = 0.6, kosten = 1.0)
        assertSame(stark, BeobachtungWaehlen(port(schwach, stark)).waehle(belief))
    }

    @Test
    fun leere_kandidaten_liefern_null() { // „keine günstige Beobachtung" -> Signal an slice-017
        assertNull(BeobachtungWaehlen(port()).waehle(belief))
    }

    @Test
    fun auswahl_ist_deterministisch() { // LH-QA-03
        val useCase = BeobachtungWaehlen(port(kandidat(0.2, 2.0), kandidat(0.6, 2.0)))
        assertEquals(useCase.waehle(belief), useCase.waehle(belief))
    }

    @Test
    fun schliesst_bereits_gewaehlte_kandidaten_aus() { // Konsumption: keine Wiederholung (Scheingewissheit)
        val a = kandidat(diskriminierung = 0.6, kosten = 1.0)
        val b = kandidat(diskriminierung = 0.4, kosten = 1.0)
        val useCase = BeobachtungWaehlen(port(a, b))
        assertSame(a, useCase.waehle(belief))
        assertSame(b, useCase.waehle(belief, bereitsGewaehlt = setOf(a)))
        assertNull(useCase.waehle(belief, bereitsGewaehlt = setOf(a, b)))
    }

    @Test
    fun nutzt_belief_abhaengige_kandidatenlisten() { // F4b / LH-FA-VOI-002
        val kandidatFuerA = kandidat(diskriminierung = 0.7, kosten = 1.0)
        val kandidatFuerB = kandidat(diskriminierung = 0.5, kosten = 1.0)
        val useCase = BeobachtungWaehlen(
            beliefAwarePort { aktuellerBelief ->
                if (aktuellerBelief.hypothesen.first { it.id.wert == "A" }.wahrscheinlichkeit >
                    aktuellerBelief.hypothesen.first { it.id.wert == "B" }.wahrscheinlichkeit
                ) {
                    listOf(kandidatFuerA)
                } else {
                    listOf(kandidatFuerB)
                }
            },
        )

        assertSame(kandidatFuerA, useCase.waehle(belief(a = 0.6, b = 0.3, rest = 0.1)))
        assertSame(kandidatFuerB, useCase.waehle(belief(a = 0.3, b = 0.6, rest = 0.1)))
    }
}
