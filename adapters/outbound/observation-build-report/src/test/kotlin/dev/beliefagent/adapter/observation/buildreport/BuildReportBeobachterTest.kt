package dev.beliefagent.adapter.observation.buildreport

import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Zeitstempel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuildReportBeobachterTest {

    @Test
    fun liest_replay_report_als_build_beobachtung() {
        val beobachter = BuildReportBeobachter(
            quelle = BuildReportQuelle {
                BuildReport(
                    status = BuildStatus.SUCCESS,
                    task = ":test",
                    summary = "151 tests gruen",
                    durationMillis = 1200L,
                )
            },
            zeitstempel = { Zeitstempel(42L) },
        )

        val beobachtung = beobachter.lies().single()

        assertEquals(Quelle.BUILD, beobachtung.quelle)
        assertEquals(Zeitstempel(42L), beobachtung.zeitstempel)
        assertEquals("build status=SUCCESS; task=:test; durationMillis=1200; summary=151 tests gruen", beobachtung.evidenz.beschreibung)
    }

    @Test
    fun parser_liest_key_value_fixture_deterministisch() {
        val report = BuildReportParser.parse(
            """
            status=failed
            task=:coverage-gate
            durationMillis=99
            summary=coverage below threshold
            """.trimIndent(),
        )

        assertEquals(BuildStatus.FAILED, report.status)
        assertEquals(":coverage-gate", report.task)
        assertEquals(99L, report.durationMillis)
        assertEquals("coverage below threshold", report.summary)
    }

    @Test
    fun build_report_erzeugt_immer_build_quelle() {
        val beschreibung = BuildReport(BuildStatus.UNKNOWN, ":build", "kein status").alsBeobachtung(Zeitstempel(7L))

        assertEquals(Quelle.BUILD, beschreibung.quelle)
        assertTrue(beschreibung.evidenz.beschreibung.contains("status=UNKNOWN"))
    }
}
