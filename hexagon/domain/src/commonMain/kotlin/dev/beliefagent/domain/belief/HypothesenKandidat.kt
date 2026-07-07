package dev.beliefagent.domain.belief

/**
 * Expliziter Score eines Hypothesen-Kandidaten (LH-FA-LLM-003).
 *
 * Der Score ist kein modellimpliziter Default, sondern ein strukturierter Wert:
 * Anteil der aktuellen Resthypothesen-Masse, den dieser Kandidat beansprucht.
 */
@JvmInline
value class KandidatenScore(val wert: Double) {
    init {
        require(wert > 0.0 && wert <= 1.0) {
            "KandidatenScore muss in (0,1] liegen: $wert"
        }
    }
}

/**
 * Kandidat für eine neue oder verfeinerte Hypothese (LH-FA-BEL-006).
 *
 * Jeder Kandidat muss mindestens eine stützende Evidenz referenzieren
 * (LH-FA-BEL-007). Kandidaten ohne expliziten Score sind strukturell nicht
 * konstruierbar.
 */
data class HypothesenKandidat(
    val id: HypotheseId,
    val score: KandidatenScore,
    val stuetzendeEvidenz: List<EvidenzReferenz>,
) {
    init {
        require(stuetzendeEvidenz.isNotEmpty()) {
            "HypothesenKandidat braucht mindestens eine EvidenzReferenz"
        }
    }
}
