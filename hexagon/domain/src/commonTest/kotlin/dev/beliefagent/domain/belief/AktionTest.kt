package dev.beliefagent.domain.belief

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Deterministische Tests für Aktion / Wirkungsklasse / Erfolgswahrscheinlichkeit
 * (LH-FA-ACT-001..004, LH-QA-03).
 */
class AktionTest {

    private fun beob(text: String) = Beobachtung(Quelle.TEST, Zeitstempel(1L), Evidenz(text))

    @Test
    fun vier_wirkungsklassen_existieren() { // LH-FA-ACT-001
        assertEquals(
            setOf(
                Wirkungsklasse.NUR_LESEND, Wirkungsklasse.ARBEITSBEREICH_LOKAL,
                Wirkungsklasse.REPOSITORY_WIRKSAM, Wirkungsklasse.EXTERN_WIRKSAM,
            ),
            Wirkungsklasse.entries.toSet(),
        )
    }

    @Test
    fun nur_extern_wirksam_ist_irreversibel() { // LH-FA-ACT-002
        assertTrue(Wirkungsklasse.EXTERN_WIRKSAM.irreversibel)
        assertFalse(Wirkungsklasse.REPOSITORY_WIRKSAM.irreversibel) // reversibler Checkpoint
        assertFalse(Wirkungsklasse.ARBEITSBEREICH_LOKAL.irreversibel)
        assertFalse(Wirkungsklasse.NUR_LESEND.irreversibel)
    }

    @Test
    fun wirkungsklassen_sind_nach_reichweite_geordnet() { // LH-FA-ACT-002
        assertTrue(Wirkungsklasse.NUR_LESEND < Wirkungsklasse.ARBEITSBEREICH_LOKAL)
        assertTrue(Wirkungsklasse.ARBEITSBEREICH_LOKAL < Wirkungsklasse.REPOSITORY_WIRKSAM)
        assertTrue(Wirkungsklasse.REPOSITORY_WIRKSAM < Wirkungsklasse.EXTERN_WIRKSAM)
    }

    @Test
    fun erfolgswahrscheinlichkeit_muss_in_einheitsintervall_liegen() { // LH-FA-ACT-003
        Erfolgswahrscheinlichkeit(0.0)
        Erfolgswahrscheinlichkeit(1.0)
        assertFailsWith<IllegalArgumentException> { Erfolgswahrscheinlichkeit(-0.1) }
        assertFailsWith<IllegalArgumentException> { Erfolgswahrscheinlichkeit(1.1) }
    }

    @Test
    fun aktion_traegt_klasse_erfolg_und_evidenz() { // LH-FA-ACT-001/003/004
        val e = beob("Test rot")
        val a = Aktion("Deploy", Wirkungsklasse.EXTERN_WIRKSAM, Erfolgswahrscheinlichkeit(0.8), listOf(e))
        assertEquals(Wirkungsklasse.EXTERN_WIRKSAM, a.wirkungsklasse)
        assertEquals(0.8, a.erfolgswahrscheinlichkeit.wert)
        assertEquals(listOf(e), a.stuetzendeEvidenz) // Rückverfolgbarkeit (LH-FA-ACT-004)
    }

    @Test
    fun aktion_ohne_stuetzende_evidenz_wird_abgewiesen() { // LH-FA-ACT-004
        assertFailsWith<IllegalArgumentException> {
            Aktion("x", Wirkungsklasse.NUR_LESEND, Erfolgswahrscheinlichkeit(0.5), emptyList())
        }
    }

    @Test
    fun aktion_beschreibung_darf_nicht_leer_sein() {
        assertFailsWith<IllegalArgumentException> {
            Aktion(" ", Wirkungsklasse.NUR_LESEND, Erfolgswahrscheinlichkeit(0.5), listOf(beob("e")))
        }
    }
}
