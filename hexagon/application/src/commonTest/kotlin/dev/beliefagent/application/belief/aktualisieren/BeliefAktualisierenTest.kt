package dev.beliefagent.application.belief.aktualisieren

import dev.beliefagent.application.belief.aktualisieren.ports.LlmPort
import dev.beliefagent.application.belief.aktualisieren.ports.UhrPort
import dev.beliefagent.domain.belief.BeliefAktualisiert
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.BeobachtungErfasst
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.Likelihoods
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Resthypothese
import dev.beliefagent.domain.belief.Zeitstempel
import kotlin.test.Test
import kotlin.test.assertEquals
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

    private fun uhr(t: Long) = object : UhrPort { override fun jetzt() = Zeitstempel(t) }

    @Test
    fun beobachtung_verschiebt_belief_und_erzeugt_ereignisse() { // LH-FA-OBS-002
        val ergebnis = BeliefAktualisieren(llmFavorisiertA, uhr(100L))
            .ausfuehren(BeliefAktualisierenBefehl(prior(), listOf(beob(100L, "Test rot"))))
        assertTrue(ergebnis.belief.hypothesen.single { it.id.wert == "A" }.wahrscheinlichkeit > 0.5)
        assertEquals(2, ergebnis.ereignisse.size)
        assertTrue(ergebnis.ereignisse[0] is BeobachtungErfasst)
        assertTrue(ergebnis.ereignisse[1] is BeliefAktualisiert)
    }

    @Test
    fun korrelierte_beobachtungen_zaehlen_einmal() { // Dedup (slice-006) in der Pipeline
        val doppelt = listOf(beob(1L, "gleich"), beob(2L, "gleich")) // gleiche Quelle + Evidenz
        val ergebnis = BeliefAktualisieren(llmFavorisiertA, uhr(1L))
            .ausfuehren(BeliefAktualisierenBefehl(prior(), doppelt))
        assertEquals(2, ergebnis.ereignisse.size) // nur eine unabhängige Beobachtung
    }

    @Test
    fun prior_bleibt_unveraendert() { // nicht-überschreibend (LH-FA-OBS-003)
        val p = prior()
        BeliefAktualisieren(llmFavorisiertA, uhr(1L))
            .ausfuehren(BeliefAktualisierenBefehl(p, listOf(beob(1L, "x"))))
        assertEquals(0.5, p.hypothesen.single { it.id.wert == "A" }.wahrscheinlichkeit)
    }

    @Test
    fun leere_beobachtungen_ergeben_prior_ohne_ereignisse() {
        val p = prior()
        val ergebnis = BeliefAktualisieren(llmFavorisiertA, uhr(1L))
            .ausfuehren(BeliefAktualisierenBefehl(p, emptyList()))
        assertTrue(ergebnis.ereignisse.isEmpty())
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
}
