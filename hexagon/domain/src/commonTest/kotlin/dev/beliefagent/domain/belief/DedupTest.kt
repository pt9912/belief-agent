package dev.beliefagent.domain.belief

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Deterministische Tests für die Dedup-Regel korrelierter Beobachtungen
 * (LH-FA-OBS-004, LH-QA-03).
 */
class DedupTest {

    private fun beob(quelle: Quelle, beschreibung: String, t: Long) =
        Beobachtung(quelle, Zeitstempel(t), Evidenz(beschreibung))

    @Test
    fun korrelierte_beobachtungen_zaehlen_einmal() { // LH-FA-OBS-004
        // Gleiche Quelle + Evidenz, verschiedene Zeit => eine unabhängige Beobachtung.
        val frueh = beob(Quelle.BUILD, "Build grün", 10L)
        val spaet = beob(Quelle.BUILD, "Build grün", 20L)
        assertEquals(listOf(frueh), Dedup.unabhaengig(listOf(frueh, spaet)))
    }

    @Test
    fun exakte_duplikate_zaehlen_einmal() { // LH-FA-OBS-004
        val b = beob(Quelle.TEST, "17 grün", 5L)
        assertEquals(listOf(b), Dedup.unabhaengig(listOf(b, b, b)))
    }

    @Test
    fun verschiedene_quellen_bleiben_unabhaengig() {
        // Dieselbe Evidenz aus zwei Quellen ist echte unabhängige Bestätigung.
        val ausTest = beob(Quelle.TEST, "grün", 1L)
        val ausBuild = beob(Quelle.BUILD, "grün", 1L)
        assertEquals(listOf(ausTest, ausBuild), Dedup.unabhaengig(listOf(ausTest, ausBuild)))
    }

    @Test
    fun verschiedene_evidenz_bleibt_unabhaengig() {
        val a = beob(Quelle.LOG, "OOM in Modul A", 1L)
        val b = beob(Quelle.LOG, "Timeout in Modul B", 2L)
        assertEquals(listOf(a, b), Dedup.unabhaengig(listOf(a, b)))
    }

    @Test
    fun erste_beobachtung_bleibt_und_reihenfolge_stabil() { // LH-QA-03
        val a1 = beob(Quelle.MENSCH, "Freigabe erteilt", 30L)
        val b = beob(Quelle.REPO, "HEAD verschoben", 40L)
        val a2 = beob(Quelle.MENSCH, "Freigabe erteilt", 99L) // Korrelat von a1
        assertEquals(listOf(a1, b), Dedup.unabhaengig(listOf(a1, b, a2)))
    }

    @Test
    fun leere_liste_bleibt_leer() {
        assertEquals(emptyList(), Dedup.unabhaengig(emptyList()))
    }

    @Test
    fun eingabe_bleibt_unveraendert() { // nicht-überschreibend
        val a = beob(Quelle.BUILD, "grün", 1L)
        val b = beob(Quelle.BUILD, "grün", 2L)
        val eingabe = listOf(a, b)
        Dedup.unabhaengig(eingabe)
        assertEquals(listOf(a, b), eingabe)
    }

    @Test
    fun deterministisch_bei_gleicher_eingabe() { // LH-QA-03
        val eingabe = listOf(
            beob(Quelle.TEST, "grün", 1L),
            beob(Quelle.TEST, "grün", 2L),
            beob(Quelle.BUILD, "grün", 3L),
        )
        assertEquals(Dedup.unabhaengig(eingabe), Dedup.unabhaengig(eingabe))
    }

    @Test
    fun signatur_ignoriert_zeitstempel() { // LH-FA-OBS-004
        val frueh = beob(Quelle.LOG, "gleiche Zeile", 1L)
        val spaet = beob(Quelle.LOG, "gleiche Zeile", 2L)
        assertEquals(Dedup.signatur(frueh), Dedup.signatur(spaet))
    }

    @Test
    fun signatur_trennt_quelle_und_evidenz() {
        val a = beob(Quelle.LOG, "x", 1L)
        assertNotEquals(Dedup.signatur(a), Dedup.signatur(beob(Quelle.TEST, "x", 1L)))
        assertNotEquals(Dedup.signatur(a), Dedup.signatur(beob(Quelle.LOG, "y", 1L)))
    }
}
