package dev.beliefagent.application.belief.ports

/**
 * Stabile Referenz auf eine externalisierte Modell-Konfidenz (LH-FA-LLM-003).
 *
 * Die Referenz bleibt bei Overrides stabil, damit spaetere Gate-/Replay-Pfade
 * nicht auf modellinternes Reasoning oder fragile Payload-Positionen zeigen.
 */
@JvmInline
value class KonfidenzReferenz(val wert: String) {
    init {
        require(wert.isNotBlank()) { "KonfidenzReferenz darf nicht leer sein" }
    }
}

/**
 * Explizite, gate-faehige Modell-Konfidenz als Zahl in [0,1].
 *
 * Sie ist bewusst ein Application-Contract-Wert, keine Gate-Entscheidung: welche
 * Schwelle gilt, entscheidet erst der Gate-Pfad.
 */
@JvmInline
value class ModellKonfidenz(val wert: Double) {
    init {
        require(wert in 0.0..1.0) { "ModellKonfidenz muss in [0,1] liegen: $wert" }
    }
}

@JvmInline
value class KonfidenzQuelle(val wert: String) {
    init {
        require(wert.isNotBlank()) { "KonfidenzQuelle darf nicht leer sein" }
    }
}

@JvmInline
value class KonfidenzVersion(val wert: Int) {
    init {
        require(wert > 0) { "KonfidenzVersion muss positiv sein: $wert" }
    }

    fun naechste(): KonfidenzVersion = KonfidenzVersion(wert + 1)
}

@JvmInline
value class OverrideBegruendung(val wert: String) {
    init {
        require(wert.isNotBlank()) { "OverrideBegruendung darf nicht leer sein" }
    }
}

/**
 * Externalisierte Modell-Konfidenz (LH-FA-LLM-003).
 *
 * Jede Instanz ist ein eigener, append-only speicherbarer Eintrag. Ein Override
 * wird als neue Version mit gleicher [referenz] und gesetzter
 * [overrideBegruendung] geschrieben, nicht als Mutation eines alten Eintrags.
 */
data class ExternalisierteKonfidenz(
    val referenz: KonfidenzReferenz,
    val wert: ModellKonfidenz,
    val quelle: KonfidenzQuelle,
    val version: KonfidenzVersion,
    val overrideBegruendung: OverrideBegruendung? = null,
)
