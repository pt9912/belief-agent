package dev.beliefagent.adapter.voi

import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Zeitstempel
import dev.beliefagent.domain.voi.VoiKandidat
import kotlin.test.Test
import kotlin.test.assertEquals

/** LH-FA-VOI-002 / LH-QA-03: die Fake-Kandidatenquelle liefert ihre Kandidaten deterministisch. */
class FakeKandidatenquelleTest {

    @Test
    fun liefert_die_konfigurierten_kandidaten() {
        val beobachtung = Beobachtung(Quelle.TEST, Zeitstempel(1L), Evidenz("e"))
        val kandidaten = listOf(
            VoiKandidat(beobachtung, erwarteteDiskriminierung = 0.5, kosten = 1.0),
            VoiKandidat(beobachtung, erwarteteDiskriminierung = 0.3, kosten = 2.0),
        )
        assertEquals(kandidaten, FakeKandidatenquelle(kandidaten).kandidaten())
    }
}
