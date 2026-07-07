package dev.beliefagent.domain.belief

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Deterministische Kandidaten-Invarianten (LH-FA-BEL-007, LH-FA-LLM-003,
 * LH-QA-03).
 */
class HypothesenKandidatTest {

    @Test
    fun kandidat_traegt_score_und_evidenzreferenz() {
        val kandidat = HypothesenKandidat(
            id = HypotheseId("gateway"),
            score = KandidatenScore(0.40),
            stuetzendeEvidenz = listOf(EvidenzReferenz("obs:test-rot")),
        )

        assertEquals("gateway", kandidat.id.wert)
        assertEquals(0.40, kandidat.score.wert)
        assertEquals(listOf("obs:test-rot"), kandidat.stuetzendeEvidenz.map { it.wert })
    }

    @Test
    fun kandidat_ohne_evidenzreferenz_ist_ungueltig() {
        assertFailsWith<IllegalArgumentException> {
            HypothesenKandidat(HypotheseId("gateway"), KandidatenScore(0.40), emptyList())
        }
    }

    @Test
    fun score_muss_explizit_im_gueltigen_intervall_liegen() {
        assertFailsWith<IllegalArgumentException> { KandidatenScore(0.0) }
        assertFailsWith<IllegalArgumentException> { KandidatenScore(-0.1) }
        assertFailsWith<IllegalArgumentException> { KandidatenScore(1.1) }
        assertFailsWith<IllegalArgumentException> { KandidatenScore(Double.NaN) }
    }

    @Test
    fun evidenzreferenz_darf_nicht_leer_sein() {
        assertFailsWith<IllegalArgumentException> { EvidenzReferenz(" ") }
    }
}
