package dev.beliefagent.application.belief.aktualisieren

import dev.beliefagent.application.belief.aktualisieren.ports.HypothesenPort
import dev.beliefagent.application.belief.aktualisieren.ports.LlmPort
import dev.beliefagent.application.belief.aktualisieren.ports.UhrPort
import dev.beliefagent.domain.belief.BeliefAktualisiert
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.BeobachtungErfasst
import dev.beliefagent.domain.belief.EvidenzReferenz
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.HypothesenKandidat
import dev.beliefagent.domain.belief.KandidatenScore
import dev.beliefagent.domain.belief.Likelihoods
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Resthypothese
import dev.beliefagent.domain.belief.Zeitstempel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Deterministische Tests der Belief-Update-Pipeline gegen In-Test-Fakes
 * (LH-FA-OBS-002, LH-QA-03). Der Produktions-Fake-LLM wird im Adapter-Modul
 * getestet; hier steht ein minimaler Fake-Port für die reine Pipeline-Logik.
 */
class BeliefAktualisierenTest {

    private fun prior() = BeliefState.of(
        listOf(Hypothese(HypotheseId("A"), 0.5), Hypothese(HypotheseId("B"), 0.3)),
        Resthypothese(0.2),
    )

    private fun beob(t: Long, text: String) = Beobachtung(Quelle.TEST, Zeitstempel(t), Evidenz(text))

    // Fake-Port: A hohe, alle anderen niedrige Likelihood.
    private val llmFavorisiertA = object : LlmPort {
        override fun likelihoods(beobachtung: Beobachtung, prior: BeliefState) =
            Likelihoods(prior.hypothesen.associate { it.id to if (it.id.wert == "A") 0.9 else 0.1 }, 0.1)
    }

    private val llmErhoehtRest = object : LlmPort {
        override fun likelihoods(beobachtung: Beobachtung, prior: BeliefState) =
            Likelihoods(prior.hypothesen.associate { it.id to 0.1 }, resthypothese = 0.9)
    }

    private fun uhr(t: Long) = object : UhrPort { override fun jetzt() = Zeitstempel(t) }

    @Test
    fun beobachtung_verschiebt_belief_und_erzeugt_ereignisse() { // LH-FA-OBS-002
        val ergebnis = BeliefAktualisieren(llmFavorisiertA, uhr(100L))
            .ausfuehren(BeliefAktualisierenBefehl(prior(), listOf(beob(100L, "Test rot"))))
        assertTrue(ergebnis.belief.hypothesen.single { it.id.wert == "A" }.wahrscheinlichkeit > 0.5)
        assertEquals(3, ergebnis.ereignisse.size) // Initial-Snapshot + erfasst + aktualisiert
        assertTrue(ergebnis.ereignisse[0] is BeliefAktualisiert) // Ausgangs-Belief (LH-FA-AUD-002)
        assertTrue(ergebnis.ereignisse[1] is BeobachtungErfasst)
        assertTrue(ergebnis.ereignisse[2] is BeliefAktualisiert)
    }

    @Test
    fun korrelierte_beobachtungen_zaehlen_einmal() { // Dedup (slice-006) in der Pipeline
        val doppelt = listOf(beob(1L, "gleich"), beob(2L, "gleich")) // gleiche Quelle + Evidenz
        val ergebnis = BeliefAktualisieren(llmFavorisiertA, uhr(1L))
            .ausfuehren(BeliefAktualisierenBefehl(prior(), doppelt))
        assertEquals(3, ergebnis.ereignisse.size) // Initial-Snapshot + eine unabhängige Beobachtung
    }

    @Test
    fun prior_bleibt_unveraendert() { // nicht-überschreibend (LH-FA-OBS-003)
        val p = prior()
        BeliefAktualisieren(llmFavorisiertA, uhr(1L))
            .ausfuehren(BeliefAktualisierenBefehl(p, listOf(beob(1L, "x"))))
        assertEquals(0.5, p.hypothesen.single { it.id.wert == "A" }.wahrscheinlichkeit)
    }

    @Test
    fun leere_beobachtungen_ergeben_prior_mit_initial_snapshot() { // LH-FA-AUD-002
        val p = prior()
        val ergebnis = BeliefAktualisieren(llmFavorisiertA, uhr(1L))
            .ausfuehren(BeliefAktualisierenBefehl(p, emptyList()))
        // Kein Update, aber der Ausgangs-Belief ist protokolliert (rekonstruierbar).
        assertEquals(1, ergebnis.ereignisse.size)
        assertTrue(ergebnis.ereignisse.single() is BeliefAktualisiert)
        assertEquals(
            p.hypothesen.map { it.wahrscheinlichkeit },
            ergebnis.belief.hypothesen.map { it.wahrscheinlichkeit },
        )
    }

    @Test
    fun deterministisch_bei_gleicher_eingabe() { // LH-QA-03
        val befehl = BeliefAktualisierenBefehl(prior(), listOf(beob(1L, "Test"), beob(2L, "Build")))
        val a = BeliefAktualisieren(llmFavorisiertA, uhr(5L)).ausfuehren(befehl)
        val b = BeliefAktualisieren(llmFavorisiertA, uhr(5L)).ausfuehren(befehl)
        assertEquals(
            a.belief.hypothesen.map { it.wahrscheinlichkeit },
            b.belief.hypothesen.map { it.wahrscheinlichkeit },
        )
    }

    @Test
    fun hohe_resthypothese_fordert_hypothesen_an_und_uebernimmt_gueltige_kandidaten() { // LH-FA-BEL-005/006
        val port = MerkenderHypothesenPort(
            listOf(
                HypothesenKandidat(
                    HypotheseId("C"),
                    KandidatenScore(0.5),
                    listOf(EvidenzReferenz("obs:rest-hoch")),
                ),
            ),
        )

        val ergebnis = BeliefAktualisieren(llmErhoehtRest, uhr(10L), hypothesen = port)
            .ausfuehren(BeliefAktualisierenBefehl(prior(), listOf(beob(10L, "unerklaert"))))

        assertEquals(1, port.aufrufe)
        assertTrue(port.beliefs.single().resthypothese.wahrscheinlichkeit > 0.30)
        val c = ergebnis.belief.hypothesen.single { it.id.wert == "C" }
        assertTrue(c.wahrscheinlichkeit > 0.0)
        assertEquals(listOf("obs:rest-hoch"), c.stuetzendeEvidenz.map { it.wert })
        assertEquals(4, ergebnis.ereignisse.size) // Initial + Beobachtung + Bayes-Update + Re-Hypothesen-Update
        assertTrue(ergebnis.ereignisse.last() is BeliefAktualisiert)
    }

    @Test
    fun resthypothese_am_schwellwert_fordert_keine_hypothesen_an() { // LH-FA-BEL-005
        val port = MerkenderHypothesenPort(emptyList())
        val beliefAmSchwellwert = BeliefState.of(
            listOf(Hypothese(HypotheseId("A"), 0.70)),
            Resthypothese(0.30),
        )

        val ergebnis = BeliefAktualisieren(llmFavorisiertA, uhr(1L), hypothesen = port)
            .ausfuehren(BeliefAktualisierenBefehl(beliefAmSchwellwert, emptyList()))

        assertEquals(0, port.aufrufe)
        assertEquals(beliefAmSchwellwert, ergebnis.belief)
        assertEquals(1, ergebnis.ereignisse.size)
    }

    @Test
    fun leere_kandidaten_bei_hoher_resthypothese_bleiben_fail_safe_unveraendert() {
        val port = MerkenderHypothesenPort(emptyList())
        val hoherRest = BeliefState.of(
            listOf(Hypothese(HypotheseId("A"), 0.60)),
            Resthypothese(0.40),
        )

        val ergebnis = BeliefAktualisieren(llmFavorisiertA, uhr(1L), hypothesen = port)
            .ausfuehren(BeliefAktualisierenBefehl(hoherRest, emptyList()))

        assertEquals(1, port.aufrufe)
        assertEquals(hoherRest, ergebnis.belief)
        assertEquals(1, ergebnis.ereignisse.size)
    }

    @Test
    fun inkonsistente_kandidaten_werden_nicht_uebernommen() { // LH-FA-LLM-003
        val port = MerkenderHypothesenPort(
            listOf(
                HypothesenKandidat(HypotheseId("C"), KandidatenScore(0.7), listOf(EvidenzReferenz("obs:c"))),
                HypothesenKandidat(HypotheseId("D"), KandidatenScore(0.4), listOf(EvidenzReferenz("obs:d"))),
            ),
        )
        val hoherRest = BeliefState.of(
            listOf(Hypothese(HypotheseId("A"), 0.60)),
            Resthypothese(0.40),
        )

        val ergebnis = BeliefAktualisieren(llmFavorisiertA, uhr(1L), hypothesen = port)
            .ausfuehren(BeliefAktualisierenBefehl(hoherRest, emptyList()))

        assertEquals(1, port.aufrufe)
        assertEquals(hoherRest, ergebnis.belief)
        assertFalse(ergebnis.belief.hypothesen.any { it.id.wert == "C" || it.id.wert == "D" })
        assertEquals(1, ergebnis.ereignisse.size)
    }

    private class MerkenderHypothesenPort(
        private val rueckgabe: List<HypothesenKandidat>,
    ) : HypothesenPort {
        var aufrufe = 0
            private set
        val beliefs = mutableListOf<BeliefState>()

        override fun kandidaten(belief: BeliefState): List<HypothesenKandidat> {
            aufrufe += 1
            beliefs += belief
            return rueckgabe
        }
    }
}
