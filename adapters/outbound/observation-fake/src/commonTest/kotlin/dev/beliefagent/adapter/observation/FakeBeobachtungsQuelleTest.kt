package dev.beliefagent.adapter.observation

import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Zeitstempel
import kotlin.test.Test
import kotlin.test.assertEquals

/** LH-FA-OBS-001 / LH-QA-03: die Fake-Quelle liefert ihre Beobachtungen deterministisch. */
class FakeBeobachtungsQuelleTest {

    @Test
    fun liefert_die_konfigurierten_beobachtungen() {
        val bs = listOf(
            Beobachtung(Quelle.TEST, Zeitstempel(1L), Evidenz("17 grün")),
            Beobachtung(Quelle.BUILD, Zeitstempel(2L), Evidenz("Build grün")),
        )
        assertEquals(bs, FakeBeobachtungsQuelle(bs).lies())
    }
}
