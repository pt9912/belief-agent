package dev.beliefagent.adapter.audit

import dev.beliefagent.domain.belief.AktionVorgeschlagen
import dev.beliefagent.domain.belief.Zeitstempel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** LH-FA-AUD-001: MemoryAudit persistiert append-only + geordnet (delegiert an EreignisProtokoll). */
class MemoryAuditTest {

    @Test
    fun anhaengen_persistiert_geordnet() {
        val audit = MemoryAudit()
        assertTrue(audit.lade().istLeer())
        audit.anhaengen(AktionVorgeschlagen(Zeitstempel(1L), "a"))
        audit.anhaengen(AktionVorgeschlagen(Zeitstempel(2L), "b"))
        assertEquals(2, audit.lade().groesse)
    }

    @Test
    fun rueck_datieren_wird_abgewiesen() { // Append-only-Ordnung (slice-007)
        val audit = MemoryAudit()
        audit.anhaengen(AktionVorgeschlagen(Zeitstempel(10L), "a"))
        assertFailsWith<IllegalArgumentException> {
            audit.anhaengen(AktionVorgeschlagen(Zeitstempel(9L), "b"))
        }
    }
}
