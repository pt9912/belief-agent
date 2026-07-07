package dev.beliefagent.domain.belief

/**
 * Reine Domänen-Regel für dynamische Hypothesen-Erweiterung (LH-FA-BEL-006).
 *
 * Kandidaten beanspruchen über ihren [KandidatenScore] explizit Anteile der
 * aktuellen Resthypothesen-Masse. Bestehende Hypothesen behalten ihre bisherige
 * Masse; übernommene Kandidaten verschieben nur Masse aus der Resthypothese in
 * neue oder bestehende Hypothesen. Damit bleibt der resultierende [BeliefState]
 * normiert und konservativ.
 */
object HypothesenraumErweitern {

    fun mitKandidaten(prior: BeliefState, kandidaten: List<HypothesenKandidat>): BeliefState {
        if (kandidaten.isEmpty()) return prior

        val scoreSumme = kandidaten.sumOf { it.score.wert }
        require(scoreSumme <= 1.0 + BeliefState.NORMIERUNGS_TOLERANZ) {
            "Hypothesen-Kandidaten beanspruchen mehr als die Resthypothese: ΣScore=$scoreSumme"
        }

        val restmasse = prior.resthypothese.wahrscheinlichkeit
        val zusatzMasseNachId = kandidaten
            .groupBy { it.id }
            .mapValues { (_, gruppe) -> gruppe.sumOf { restmasse * it.score.wert } }
        val evidenzNachId = kandidaten
            .groupBy { it.id }
            .mapValues { (_, gruppe) -> gruppe.flatMap { it.stuetzendeEvidenz }.distinct() }

        val bekannteIds = prior.hypothesen.map { it.id }.toSet()
        val aktualisierte = prior.hypothesen.map { hypothese ->
            hypothese.copy(
                wahrscheinlichkeit = hypothese.wahrscheinlichkeit + (zusatzMasseNachId[hypothese.id] ?: 0.0),
                stuetzendeEvidenz = (hypothese.stuetzendeEvidenz + (evidenzNachId[hypothese.id] ?: emptyList())).distinct(),
            )
        }
        val neue = kandidaten
            .map { it.id }
            .distinct()
            .filterNot { it in bekannteIds }
            .map { id ->
                Hypothese(
                    id = id,
                    wahrscheinlichkeit = zusatzMasseNachId.getValue(id),
                    stuetzendeEvidenz = evidenzNachId.getValue(id),
                )
            }

        val restAnteil = (1.0 - scoreSumme).coerceAtLeast(0.0)
        val rest = Resthypothese(restmasse * restAnteil)
        return BeliefState.of(aktualisierte + neue, rest)
    }
}
