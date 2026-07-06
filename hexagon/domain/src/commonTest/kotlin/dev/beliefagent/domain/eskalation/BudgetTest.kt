package dev.beliefagent.domain.eskalation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Deterministische Tests des Budgets (LH-FA-ESK-004, LH-QA-03): Erschöpfung über
 * Schritte **und** Kosten als eigenständiger Eskalations-Auslöser; fail-closed
 * (`MR-003`).
 */
class BudgetTest {

    @Test
    fun default_budget_ist_frisch_nicht_erschoepft() {
        assertFalse(Budget().erschoepft)
        assertEquals(20, Budget.STANDARD_SCHRITTE)
    }

    @Test
    fun verbrauche_erhoeht_schritte_und_kosten_unveraenderlich() {
        val b = Budget(maxSchritte = 3)
        val b1 = b.verbrauche(kosten = 2.0)
        assertEquals(0, b.verbrauchteSchritte, "Original bleibt unverändert")
        assertEquals(1, b1.verbrauchteSchritte)
        assertEquals(2.0, b1.verbrauchteKosten, 1e-12)
    }

    @Test
    fun erschoepft_wenn_schritte_grenze_erreicht() { // LH-FA-ESK-004
        assertTrue(Budget(maxSchritte = 2, verbrauchteSchritte = 2).erschoepft)
        assertFalse(Budget(maxSchritte = 2, verbrauchteSchritte = 1).erschoepft)
    }

    @Test
    fun erschoepft_wenn_kosten_grenze_erreicht() { // LH-FA-ESK-004 (Kosten-Dimension)
        // Schritte nicht erschöpft (1/20), aber Kosten erreichen die Grenze.
        val b = Budget(maxKosten = 5.0).verbrauche(kosten = 5.0)
        assertTrue(b.erschoepft)
    }

    @Test
    fun ohne_kostengrenze_zaehlen_nur_schritte() {
        // maxKosten = null -> beliebige Kosten erschöpfen nicht, solange Schritte reichen.
        val b = Budget(maxSchritte = 5, maxKosten = null).verbrauche(kosten = 1000.0)
        assertFalse(b.erschoepft)
    }

    @Test
    fun schritt_fuer_schritt_bis_erschoepft() {
        var b = Budget(maxSchritte = 3)
        repeat(3) { b = b.verbrauche() }
        assertTrue(b.erschoepft)
    }

    @Test
    fun ungueltiges_budget_wird_abgewiesen() { // fail-closed
        assertFailsWith<IllegalArgumentException> { Budget(maxSchritte = 0) }
        assertFailsWith<IllegalArgumentException> { Budget(verbrauchteSchritte = -1) }
        assertFailsWith<IllegalArgumentException> { Budget(maxKosten = 0.0) }
        assertFailsWith<IllegalArgumentException> { Budget(maxKosten = Double.NaN) }
        assertFailsWith<IllegalArgumentException> { Budget(verbrauchteKosten = -1.0) }
    }

    @Test
    fun verbrauche_mit_negativen_kosten_wird_abgewiesen() { // fail-closed
        assertFailsWith<IllegalArgumentException> { Budget().verbrauche(kosten = -0.5) }
    }
}
