package dev.beliefagent.adapter.konfidenz

import dev.beliefagent.application.belief.ports.ExternalisierteKonfidenz
import dev.beliefagent.application.belief.ports.KonfidenzQuelle
import dev.beliefagent.application.belief.ports.KonfidenzReferenz
import dev.beliefagent.application.belief.ports.KonfidenzVersion
import dev.beliefagent.application.belief.ports.ModellKonfidenz
import dev.beliefagent.application.belief.ports.OverrideBegruendung
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** LH-FA-LLM-003 / LH-FA-AUD-001/003 / LH-QA-03: deterministischer Replay-Adapter. */
class MemoryKonfidenzPortTest {

    private val standardReferenz = KonfidenzReferenz("fake:konfidenz:aktion-1")
    private val andereReferenz = KonfidenzReferenz("fake:konfidenz:aktion-2")
    private val quelle = KonfidenzQuelle("konfidenz-memory-fake")

    @Test
    fun speichert_append_only_und_liefert_historie_in_einfuege_reihenfolge() {
        val port = MemoryKonfidenzPort.leer()
        val initial = konfidenz(0.82, 1)
        val override = konfidenz(0.55, 2, "Golden-Set-Korrektur")

        port.anhaengen(initial)
        port.anhaengen(konfidenz(0.91, 1, referenz = andereReferenz))
        port.anhaengen(override)

        assertEquals(listOf(initial, override), port.lade(standardReferenz))
        assertEquals(listOf(konfidenz(0.91, 1, referenz = andereReferenz)), port.lade(andereReferenz))
    }

    @Test
    fun lade_liefert_kopie_statt_mutierbarer_speicher_sicht() {
        val port = MemoryKonfidenzPort.leer()
        port.anhaengen(konfidenz(0.82, 1))

        val geladeneHistorie = port.lade(standardReferenz)

        assertEquals(listOf(konfidenz(0.82, 1)), geladeneHistorie)
        assertEquals(listOf(konfidenz(0.82, 1)), port.lade(standardReferenz))
    }

    @Test
    fun replay_fixtures_laden_konfidenzen_und_overrides_deterministisch() {
        val fixtures = listOf(
            KonfidenzReplayFixture("fake:konfidenz:aktion-1", 0.82, "konfidenz-memory-fake", 1),
            KonfidenzReplayFixture(
                referenz = "fake:konfidenz:aktion-1",
                wert = 0.55,
                quelle = "konfidenz-memory-fake",
                version = 2,
                overrideBegruendung = "Golden-Set-Korrektur",
            ),
        )

        val ersterPort = MemoryKonfidenzPort.ausFixtures(fixtures)
        val zweiterPort = MemoryKonfidenzPort.ausFixtures(fixtures)

        assertEquals(ersterPort.lade(standardReferenz), zweiterPort.lade(standardReferenz))
        assertEquals(
            listOf(konfidenz(0.82, 1), konfidenz(0.55, 2, "Golden-Set-Korrektur")),
            ersterPort.lade(standardReferenz),
        )
    }

    @Test
    fun kaputtes_fixture_liefert_fail_safe_keine_gate_faehige_konfidenz() {
        val port = MemoryKonfidenzPort.ausFixtures(
            listOf(KonfidenzReplayFixture("fake:konfidenz:aktion-1", Double.NaN, "konfidenz-memory-fake", 1)),
        )

        assertTrue(port.lade(standardReferenz).isEmpty())
    }

    @Test
    fun kaputte_versionsfolge_liefert_fail_safe_leeren_speicher() {
        val port = MemoryKonfidenzPort.ausFixtures(
            listOf(
                KonfidenzReplayFixture("fake:konfidenz:aktion-1", 0.82, "konfidenz-memory-fake", 2),
                KonfidenzReplayFixture("fake:konfidenz:aktion-1", 0.55, "konfidenz-memory-fake", 1),
            ),
        )

        assertTrue(port.lade(standardReferenz).isEmpty())
    }

    @Test
    fun direktes_anhaengen_akzeptiert_keine_alte_version() {
        val port = MemoryKonfidenzPort.leer()
        port.anhaengen(konfidenz(0.82, 1))

        assertFailsWith<IllegalArgumentException> {
            port.anhaengen(konfidenz(0.55, 1, "doppelte Version"))
        }

        assertEquals(listOf(konfidenz(0.82, 1)), port.lade(standardReferenz))
    }

    private fun konfidenz(
        wert: Double,
        version: Int,
        begruendung: String? = null,
        referenz: KonfidenzReferenz = standardReferenz,
    ) = ExternalisierteKonfidenz(
        referenz = referenz,
        wert = ModellKonfidenz(wert),
        quelle = quelle,
        version = KonfidenzVersion(version),
        overrideBegruendung = begruendung?.let { OverrideBegruendung(it) },
    )
}
