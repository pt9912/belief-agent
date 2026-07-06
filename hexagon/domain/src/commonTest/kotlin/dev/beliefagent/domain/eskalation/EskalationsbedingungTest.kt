package dev.beliefagent.domain.eskalation

import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.GateEntscheidung
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.Resthypothese
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Deterministische Tests der Eskalationsbedingung (LH-FA-ESK-001, LH-QA-03): die
 * Bedingung greift **gdw.** alle drei Teilbedingungen gelten — daher jede einzeln
 * negativ. Der Budget-Auslöser (LH-FA-ESK-004) ist ein **getrennter** Pfad.
 */
class EskalationsbedingungTest {

    private fun belief(rest: Double) =
        BeliefState.of(listOf(Hypothese(HypotheseId("A"), 1.0 - rest)), Resthypothese(rest))

    private val zu: GateEntscheidung = GateEntscheidung.Ablehnung("unter Schwelle")
    private val offen: GateEntscheidung = GateEntscheidung.Freigabe

    @Test
    fun eskaliert_wenn_alle_drei_teilbedingungen_gelten() { // LH-FA-ESK-001
        assertTrue(Eskalationsbedingung.erfuellt(beobachtungenErschoepft = true, belief = belief(0.6), gate = zu))
    }

    @Test
    fun beobachtungen_nicht_erschoepft_keine_eskalation() { // LH-FA-ESK-001 (Teilbedingung 1 negativ)
        assertFalse(Eskalationsbedingung.erfuellt(beobachtungenErschoepft = false, belief = belief(0.6), gate = zu))
    }

    @Test
    fun resthypothese_nicht_hoch_keine_eskalation() { // LH-FA-ESK-001 (Teilbedingung 2 negativ)
        assertFalse(Eskalationsbedingung.erfuellt(beobachtungenErschoepft = true, belief = belief(0.3), gate = zu))
    }

    @Test
    fun gate_offen_keine_eskalation() { // LH-FA-ESK-001 (Teilbedingung 3 negativ)
        assertFalse(Eskalationsbedingung.erfuellt(beobachtungenErschoepft = true, belief = belief(0.6), gate = offen))
    }

    @Test
    fun resthypothese_genau_auf_schwelle_eskaliert_nicht() { // Grenzfall: echt > (nicht >=)
        assertFalse(Eskalationsbedingung.erfuellt(beobachtungenErschoepft = true, belief = belief(0.5), gate = zu))
    }

    @Test
    fun gate_eskalation_zaehlt_als_geschlossen() { // "geschlossen" = keine Freigabe
        val gateEsk = GateEntscheidung.Eskalation("Resthypothese über Sperr-Schwelle")
        assertTrue(Eskalationsbedingung.erfuellt(beobachtungenErschoepft = true, belief = belief(0.6), gate = gateEsk))
    }

    @Test
    fun schwelle_ist_konfigurierbar() {
        // Strengere Schwelle 0,7: rest 0,6 gilt nicht mehr als "hoch".
        assertFalse(Eskalationsbedingung.erfuellt(true, belief(0.6), zu, schwelle = 0.7))
        // θ_esc-Default an θ_rehyp gekoppelt.
        assertTrue(Eskalationsbedingung.STANDARD_ESKALATIONS_SCHWELLE == 0.5)
    }

    @Test
    fun budget_ist_getrennter_pfad_von_der_bedingung() { // LH-FA-ESK-004 (Unabhängigkeit)
        // Gate-Bedingung NICHT erfüllt (Freigabe, niedrige Resthypothese) ...
        assertFalse(Eskalationsbedingung.erfuellt(beobachtungenErschoepft = false, belief = belief(0.1), gate = offen))
        // ... trotzdem eskaliert ein erschöpftes Budget eigenständig.
        assertTrue(Budget(maxSchritte = 1, verbrauchteSchritte = 1).erschoepft)
    }
}
