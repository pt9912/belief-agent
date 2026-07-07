package dev.beliefagent.adapter.konfidenz

import dev.beliefagent.application.belief.ports.ExternalisierteKonfidenz
import dev.beliefagent.application.belief.ports.KonfidenzPort
import dev.beliefagent.application.belief.ports.KonfidenzQuelle
import dev.beliefagent.application.belief.ports.KonfidenzReferenz
import dev.beliefagent.application.belief.ports.KonfidenzVersion
import dev.beliefagent.application.belief.ports.ModellKonfidenz
import dev.beliefagent.application.belief.ports.OverrideBegruendung

/**
 * Rohes Replay-/Golden-Set-Fixture fuer eine externalisierte Konfidenz.
 *
 * Die Felder bleiben primitives Fixture-Material. Erst [MemoryKonfidenzPort]
 * hebt sie in den Application-Contract; kaputte Fixtures werden fail-safe als
 * leerer Speicher behandelt, statt gate-faehige Teilwerte zu liefern.
 */
data class KonfidenzReplayFixture(
    val referenz: String,
    val wert: Double,
    val quelle: String,
    val version: Int,
    val overrideBegruendung: String? = null,
)

/**
 * Deterministischer Memory-/Replay-Adapter hinter [KonfidenzPort].
 *
 * Der Adapter speichert append-only und liefert Historien in Einfuege-Reihenfolge.
 * Er entscheidet nicht ueber Gate-Freigaben; er macht externalisierte
 * Modell-Konfidenz nur reproduzierbar ladbar (LH-FA-LLM-003, LH-QA-03).
 */
class MemoryKonfidenzPort private constructor(
    initial: List<ExternalisierteKonfidenz>,
) : KonfidenzPort {

    private val eintraege = mutableListOf<ExternalisierteKonfidenz>()

    init {
        initial.forEach(::anhaengen)
    }

    override fun anhaengen(konfidenz: ExternalisierteKonfidenz) {
        require(istNeueVersion(konfidenz)) {
            "Konfidenz-Version muss append-only wachsen: ${konfidenz.referenz.wert}#${konfidenz.version.wert}"
        }
        eintraege += konfidenz
    }

    override fun lade(referenz: KonfidenzReferenz): List<ExternalisierteKonfidenz> =
        eintraege.filter { it.referenz == referenz }

    private fun istNeueVersion(konfidenz: ExternalisierteKonfidenz): Boolean =
        eintraege
            .filter { it.referenz == konfidenz.referenz }
            .maxOfOrNull { it.version.wert }
            ?.let { konfidenz.version.wert == it + 1 }
            ?: (konfidenz.version.wert == 1)

    companion object {
        fun leer(): MemoryKonfidenzPort = MemoryKonfidenzPort(emptyList())

        fun ausFixtures(fixtures: List<KonfidenzReplayFixture>): MemoryKonfidenzPort = try {
            MemoryKonfidenzPort(fixtures.map { it.toKonfidenz() })
        } catch (_: IllegalArgumentException) {
            leer()
        }

        private fun KonfidenzReplayFixture.toKonfidenz(): ExternalisierteKonfidenz =
            ExternalisierteKonfidenz(
                referenz = KonfidenzReferenz(referenz),
                wert = ModellKonfidenz(wert),
                quelle = KonfidenzQuelle(quelle),
                version = KonfidenzVersion(version),
                overrideBegruendung = overrideBegruendung?.let { OverrideBegruendung(it) },
            )
    }
}
