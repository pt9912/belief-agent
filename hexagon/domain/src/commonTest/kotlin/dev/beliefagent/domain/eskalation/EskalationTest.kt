package dev.beliefagent.domain.eskalation

import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Resthypothese
import dev.beliefagent.domain.belief.Zeitstempel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Deterministische Tests des Eskalations-Zustands (LH-FA-ESK-002/003): Eskalation
 * ist ein **definierter Zustand** (kein Fehler/keine Exception) mit vollem Kontext
 * — Belief, gesammelte Evidenz und Grund.
 */
class EskalationTest {

    private val belief = BeliefState.of(
        listOf(Hypothese(HypotheseId("A"), 0.4)),
        Resthypothese(0.6),
    )
    private val evidenz = listOf(Beobachtung(Quelle.TEST, Zeitstempel(1L), Evidenz("e1")))

    /** Modellhaft: der Zyklus liefert eine Eskalation als **normalen Rückgabewert** (LH-FA-ESK-002). */
    private fun eskaliere(): Eskalation =
        Eskalation(belief, evidenz, Eskalationsgrund.BeobachtungenErschoepft(0.6, 0.5, "Ablehnung: unter Schwelle"))

    @Test
    fun eskalation_ist_definierter_zustand_kein_fehler() { // LH-FA-ESK-002
        val e = eskaliere() // normaler Rückgabewert, keine Exception
        assertTrue(e is Eskalation)
    }

    @Test
    fun eskalation_traegt_kontext_belief_evidenz_grund() { // LH-FA-ESK-003 (alle drei)
        val e = eskaliere()
        assertSame(belief, e.belief)
        assertEquals(evidenz, e.evidenz)
        assertTrue(e.grund is Eskalationsgrund.BeobachtungenErschoepft)
    }

    @Test
    fun grund_beobachtungen_erschoepft_traegt_gate_schwelle_resthypothese() { // LH-FA-ESK-003
        val grund = eskaliere().grund as Eskalationsgrund.BeobachtungenErschoepft
        assertEquals(0.6, grund.resthypothese, 1e-12)
        assertEquals(0.5, grund.schwelle, 1e-12)
        assertTrue("Ablehnung" in grund.gateGrund)
    }

    @Test
    fun grund_budget_erschoepft_traegt_das_budget() { // LH-FA-ESK-004 (Kontext des Budget-Pfads)
        val budget = Budget(maxSchritte = 1, verbrauchteSchritte = 1)
        val e = Eskalation(belief, evidenz, Eskalationsgrund.BudgetErschoepft(budget))
        val grund = e.grund as Eskalationsgrund.BudgetErschoepft
        assertTrue(grund.budget.erschoepft)
    }

    @Test
    fun eskalation_mit_leerer_evidenz_ist_zulaessig() {
        val e = Eskalation(belief, emptyList(), Eskalationsgrund.BudgetErschoepft(Budget()))
        assertTrue(e.evidenz.isEmpty())
    }
}
