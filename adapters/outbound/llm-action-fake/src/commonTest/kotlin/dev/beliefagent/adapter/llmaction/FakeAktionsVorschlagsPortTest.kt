package dev.beliefagent.adapter.llmaction

import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.Resthypothese
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** LH-FA-LLM-002/003, LH-QA-03: deterministischer Fake fuer Aktionsvorschlaege. */
class FakeAktionsVorschlagsPortTest {

    private fun belief(id: String = "fake-hypothese-regression") = BeliefState.of(
        listOf(Hypothese(HypotheseId(id), 0.7)),
        Resthypothese(0.3),
    )

    @Test
    fun liefert_deterministisch_konfigurierte_vorschlaege() {
        val port = FakeAktionsVorschlagsPort(
            listOf(
                FakeAktionsVorschlagKonfiguration(
                    beschreibung = "Fake-Deploy vorbereiten",
                    hypotheseId = "fake-hypothese-regression",
                    wirkungsklasse = "REPOSITORY_WIRKSAM",
                    pSuccess = 0.86,
                    konfidenzReferenz = "fake:konfidenz:aktion-deploy",
                    stuetzendeEvidenz = listOf("fake:evidenz:log-1", "fake:evidenz:test-2"),
                ),
            ),
        )

        val ersterLauf = port.vorschlaege(belief())
        val zweiterLauf = port.vorschlaege(belief())

        assertEquals(ersterLauf, zweiterLauf)
        assertEquals("Fake-Deploy vorbereiten", ersterLauf.single().beschreibung)
        assertEquals("REPOSITORY_WIRKSAM", ersterLauf.single().wirkungsklasse)
        assertEquals(0.86, ersterLauf.single().pSuccess)
    }

    @Test
    fun default_vorschlaege_sind_klar_als_fake_erkennbar() {
        val vorschlaege = FakeAktionsVorschlagsPort().vorschlaege(belief())

        assertTrue(vorschlaege.isNotEmpty())
        assertTrue(vorschlaege.all { it.konfidenzReferenz.startsWith("fake:konfidenz:") })
        assertTrue(vorschlaege.flatMap { it.stuetzendeEvidenz }.all { it.startsWith("fake:evidenz:") })
    }

    @Test
    fun leere_konfiguration_liefert_leere_liste() {
        assertEquals(emptyList(), FakeAktionsVorschlagsPort(emptyList()).vorschlaege(belief()))
    }

    @Test
    fun unbekannte_hypothese_wird_nicht_vorgeschlagen() {
        val port = FakeAktionsVorschlagsPort(
            listOf(konfiguration(hypotheseId = "fake-hypothese-anders")),
        )

        assertEquals(emptyList(), port.vorschlaege(belief()))
    }

    @Test
    fun ungueltige_wirkungsklasse_liefert_fail_safe_leere_liste() {
        val port = FakeAktionsVorschlagsPort(listOf(konfiguration(wirkungsklasse = "UNBEKANNT")))

        assertEquals(emptyList(), port.vorschlaege(belief()))
    }

    @Test
    fun ungueltige_p_success_liefert_fail_safe_leere_liste() {
        val port = FakeAktionsVorschlagsPort(listOf(konfiguration(pSuccess = Double.NaN)))

        assertEquals(emptyList(), port.vorschlaege(belief()))
    }

    @Test
    fun fehlende_evidenz_liefert_fail_safe_leere_liste() {
        val port = FakeAktionsVorschlagsPort(listOf(konfiguration(stuetzendeEvidenz = emptyList())))

        assertEquals(emptyList(), port.vorschlaege(belief()))
    }

    private fun konfiguration(
        beschreibung: String = "Fake-Analyse",
        hypotheseId: String = "fake-hypothese-regression",
        wirkungsklasse: String = "NUR_LESEND",
        pSuccess: Double = 0.8,
        konfidenzReferenz: String = "fake:konfidenz:aktion-analyse",
        stuetzendeEvidenz: List<String> = listOf("fake:evidenz:log-1"),
    ) = FakeAktionsVorschlagKonfiguration(
        beschreibung = beschreibung,
        hypotheseId = hypotheseId,
        wirkungsklasse = wirkungsklasse,
        pSuccess = pSuccess,
        konfidenzReferenz = konfidenzReferenz,
        stuetzendeEvidenz = stuetzendeEvidenz,
    )
}
