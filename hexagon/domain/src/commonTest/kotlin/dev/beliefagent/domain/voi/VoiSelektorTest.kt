package dev.beliefagent.domain.voi

import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Zeitstempel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Deterministische Tests des VoI-Selektors (LH-FA-VOI-002/003/004, LH-QA-03).
 * Reine Domänen-Regel — geprüft werden das Auswahlkriterium (Gewinn je Kosten,
 * nicht roher Gewinn), der deterministische Tie-Break und die Grenzfälle inkl.
 * der fail-closed-Invarianten des [VoiKandidat]en (`MR-003`).
 */
class VoiSelektorTest {

    private val beobachtung = Beobachtung(Quelle.TEST, Zeitstempel(1L), Evidenz("e"))

    private fun kandidat(diskriminierung: Double, kosten: Double) =
        VoiKandidat(beobachtung, diskriminierung, kosten)

    @Test
    fun waehlt_bei_gleichen_kosten_die_staerkste_top2_trennung() { // LH-FA-VOI-002
        val schwach = kandidat(diskriminierung = 0.2, kosten = 1.0)
        val stark = kandidat(diskriminierung = 0.5, kosten = 1.0)
        val mittel = kandidat(diskriminierung = 0.3, kosten = 1.0)
        assertSame(stark, VoiSelektor.waehle(listOf(schwach, stark, mittel)))
    }

    @Test
    fun waegt_gewinn_je_kosten_ab_nicht_rohen_gewinn() { // LH-FA-VOI-003
        // teuer-aber-informativ: roher Gewinn 0,9 höher, aber nur 0,1 je Kosten.
        val teuerInformativ = kandidat(diskriminierung = 0.9, kosten = 9.0)
        // günstig-aber-schwach: roher Gewinn 0,3, aber 0,3 je Kosten -> gewinnt.
        val guenstigSchwach = kandidat(diskriminierung = 0.3, kosten = 1.0)
        assertSame(
            guenstigSchwach,
            VoiSelektor.waehle(listOf(teuerInformativ, guenstigSchwach)),
            "Auswahl muss Gewinn je Kosten maximieren, nicht den rohen Gewinn",
        )
    }

    @Test
    fun tie_break_bevorzugt_hoehere_absolute_diskriminierung() { // LH-FA-VOI-004 (deterministisch)
        // Beide 0,2 je Kosten; B steht zuerst -> es entscheidet NICHT die Reihenfolge,
        // sondern der Tie-Break: höhere absolute Diskriminierung (A, 0,4).
        val a = kandidat(diskriminierung = 0.4, kosten = 2.0)
        val b = kandidat(diskriminierung = 0.2, kosten = 1.0)
        assertSame(a, VoiSelektor.waehle(listOf(b, a)))
    }

    @Test
    fun voller_gleichstand_waehlt_stabil_den_ersten_kandidaten() { // LH-QA-03 (letzter Tie-Break)
        val erster = kandidat(diskriminierung = 0.3, kosten = 1.0)
        val zweiter = kandidat(diskriminierung = 0.3, kosten = 1.0)
        assertSame(erster, VoiSelektor.waehle(listOf(erster, zweiter)))
    }

    @Test
    fun auswahl_ist_unabhaengig_von_der_eingabereihenfolge() { // LH-QA-03 (Determinismus)
        val kandidaten = listOf(
            kandidat(diskriminierung = 0.2, kosten = 2.0), // 0,10
            kandidat(diskriminierung = 0.6, kosten = 2.0), // 0,30 (Sieger)
            kandidat(diskriminierung = 0.3, kosten = 3.0), // 0,10
        )
        assertEquals(VoiSelektor.waehle(kandidaten), VoiSelektor.waehle(kandidaten.reversed()))
    }

    @Test
    fun leere_kandidatenliste_liefert_null() { // Grenzfall: kein günstiger Zug -> Signal an slice-016
        assertNull(VoiSelektor.waehle(emptyList()))
    }

    @Test
    fun einzelner_kandidat_wird_gewaehlt() { // Grenzfall
        val nur = kandidat(diskriminierung = 0.1, kosten = 5.0)
        assertSame(nur, VoiSelektor.waehle(listOf(nur)))
    }

    @Test
    fun gewinn_je_kosten_wird_korrekt_berechnet() { // LH-FA-VOI-003
        assertEquals(0.2, kandidat(diskriminierung = 0.6, kosten = 3.0).gewinnJeKosten, 1e-12)
    }

    @Test
    fun diskriminierung_null_ist_zulaessig() { // 0 = keine Trennung, aber gültiger Kandidat
        assertEquals(0.0, kandidat(diskriminierung = 0.0, kosten = 1.0).gewinnJeKosten, 1e-12)
    }

    @Test
    fun kosten_null_werden_abgewiesen() { // fail-closed: Division durch Kosten
        assertFailsWith<IllegalArgumentException> { kandidat(diskriminierung = 0.5, kosten = 0.0) }
    }

    @Test
    fun negative_kosten_werden_abgewiesen() { // fail-closed
        assertFailsWith<IllegalArgumentException> { kandidat(diskriminierung = 0.5, kosten = -1.0) }
    }

    @Test
    fun negative_diskriminierung_wird_abgewiesen() { // fail-closed: keine „negative" Trennung
        assertFailsWith<IllegalArgumentException> { kandidat(diskriminierung = -0.1, kosten = 1.0) }
    }

    @Test
    fun nan_diskriminierung_wird_abgewiesen() { // Sicherheit: NaN gilt in Double-Ordnung als größer-als-alles
        assertFailsWith<IllegalArgumentException> { kandidat(diskriminierung = Double.NaN, kosten = 1.0) }
    }

    @Test
    fun nan_kosten_werden_abgewiesen() { // fail-closed
        assertFailsWith<IllegalArgumentException> { kandidat(diskriminierung = 0.5, kosten = Double.NaN) }
    }
}
