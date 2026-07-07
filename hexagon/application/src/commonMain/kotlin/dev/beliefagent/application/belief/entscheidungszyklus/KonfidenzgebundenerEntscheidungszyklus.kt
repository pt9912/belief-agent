package dev.beliefagent.application.belief.entscheidungszyklus

import dev.beliefagent.application.belief.ports.ExternalisierteKonfidenz
import dev.beliefagent.application.belief.ports.KonfidenzPort
import dev.beliefagent.application.belief.ports.KonfidenzReferenz
import dev.beliefagent.domain.belief.Aktion
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Erfolgswahrscheinlichkeit
import dev.beliefagent.domain.belief.Wirkungsklasse
import dev.beliefagent.domain.eskalation.Budget

/**
 * Aktionsabsicht, deren gate-faehige Erfolgswahrscheinlichkeit aus einer
 * externalisierten Modell-Konfidenz stammt (LH-FA-LLM-003).
 *
 * Der Typ ist bewusst noch kein LLM-Aktionsvorschlags-Port: er beschreibt nur
 * die Application-Wiring-Grenze zwischen Konfidenz-Contract und bestehendem
 * Gate-Pfad.
 */
data class KonfidenzgebundeneAktion(
    val beschreibung: String,
    val wirkungsklasse: Wirkungsklasse,
    val stuetzendeEvidenz: List<Beobachtung>,
    val konfidenzReferenz: KonfidenzReferenz,
) {
    init {
        require(beschreibung.isNotBlank()) { "Aktions-Beschreibung darf nicht leer sein" }
        require(stuetzendeEvidenz.isNotEmpty()) {
            "Konfidenzgebundene Aktion muss stuetzende Evidenz referenzieren"
        }
    }
}

/**
 * Application-Wiring fuer `LH-FA-LLM-003`: bindet externalisierte Modell-
 * Konfidenz an den bestehenden `ARC-09`-Zyklus, ohne `AktionGaten` oder den
 * Domain-`KonfidenzGate` um LLM-/Adapterwissen zu erweitern.
 */
class KonfidenzgebundenerEntscheidungszyklus(
    private val zyklus: Entscheidungszyklus,
    private val konfidenzen: KonfidenzPort,
) {

    fun entscheide(
        aktion: KonfidenzgebundeneAktion,
        prior: BeliefState,
        budget: Budget,
    ): Zyklusergebnis {
        val neuesteKonfidenz = neuesteGueltigeKonfidenz(aktion.konfidenzReferenz)
            ?: return Zyklusergebnis.Abgelehnt(
                "externalisierte Konfidenz fehlt oder ist nicht append-only gueltig " +
                    "(LH-FA-LLM-003): ${aktion.konfidenzReferenz.wert}",
                prior,
            )

        return zyklus.entscheide(aktion.mitKonfidenz(neuesteKonfidenz), prior, budget)
    }

    private fun KonfidenzgebundeneAktion.mitKonfidenz(konfidenz: ExternalisierteKonfidenz): Aktion =
        Aktion(
            beschreibung = beschreibung,
            wirkungsklasse = wirkungsklasse,
            erfolgswahrscheinlichkeit = Erfolgswahrscheinlichkeit(konfidenz.wert.wert),
            stuetzendeEvidenz = stuetzendeEvidenz,
        )

    private fun neuesteGueltigeKonfidenz(referenz: KonfidenzReferenz): ExternalisierteKonfidenz? {
        val historie = konfidenzen.lade(referenz)
        if (historie.isEmpty()) return null

        val sortiert = historie.sortedBy { it.version.wert }
        val appendOnlyGueltig = sortiert.withIndex().all { (index, eintrag) ->
            eintrag.referenz == referenz && eintrag.version.wert == index + 1
        }
        return if (appendOnlyGueltig) sortiert.last() else null
    }
}
