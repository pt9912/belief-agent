package dev.beliefagent.adapter.audit.file

import dev.beliefagent.domain.belief.AktionVorgeschlagen
import dev.beliefagent.domain.belief.BeliefAktualisiert
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.GateAbgelehnt
import dev.beliefagent.domain.belief.HypotheseHinzugefuegt
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.Rekonstruktion
import dev.beliefagent.domain.belief.Resthypothese
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.Zeitstempel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DateiAuditTest {

    private fun tempDatei(): Path =
        Files.createTempDirectory("audit-file-test").resolve("audit.log")

    private fun schreibe(pfad: Path, inhalt: String) {
        Files.write(pfad, inhalt.toByteArray(StandardCharsets.UTF_8))
    }

    // --- Leer-/Neustart-Verhalten -----------------------------------------

    @Test
    fun nicht_existente_datei_ist_leer() {
        assertTrue(DateiAudit(tempDatei()).lade().istLeer())
    }

    @Test
    fun leere_datei_ist_leer() {
        val pfad = tempDatei()
        Files.createFile(pfad)
        assertTrue(DateiAudit(pfad).lade().istLeer())
    }

    @Test
    fun nur_header_ist_leer() {
        val pfad = tempDatei()
        schreibe(pfad, EreignisSerialisierung.HEADER + "\n")
        assertTrue(DateiAudit(pfad).lade().istLeer())
    }

    @Test
    fun header_ohne_abschluss_newline_ist_leer() {
        val pfad = tempDatei()
        schreibe(pfad, EreignisSerialisierung.HEADER)
        assertTrue(DateiAudit(pfad).lade().istLeer())
    }

    @Test
    fun anhaengen_dann_neustart_laedt_geordnet() {
        val pfad = tempDatei()
        val ereignisse = listOf(
            HypotheseHinzugefuegt(Zeitstempel(1), HypotheseId("h1")),
            AktionVorgeschlagen(Zeitstempel(2), "PR mergen"),
            GateAbgelehnt(Zeitstempel(3), "Konfidenz unter Schwelle"),
        )
        val schreiber = DateiAudit(pfad)
        ereignisse.forEach(schreiber::anhaengen)

        // Neuer Adapter (Prozess-Neustart-Simulation) auf derselben Datei.
        val protokoll = DateiAudit(pfad).lade()

        assertEquals(ereignisse, protokoll.ereignisse)
    }

    @Test
    fun append_only_ueber_zwei_instanzen_haengt_an_statt_zu_ueberschreiben() {
        val pfad = tempDatei()
        DateiAudit(pfad).anhaengen(HypotheseHinzugefuegt(Zeitstempel(1), HypotheseId("h1")))
        DateiAudit(pfad).anhaengen(GateAbgelehnt(Zeitstempel(2), "spaeter"))

        val protokoll = DateiAudit(pfad).lade()

        assertEquals(2, protokoll.groesse)
        assertEquals(HypotheseHinzugefuegt(Zeitstempel(1), HypotheseId("h1")), protokoll.ereignisse[0])
        assertEquals(GateAbgelehnt(Zeitstempel(2), "spaeter"), protokoll.ereignisse[1])
    }

    @Test
    fun rekonstruktion_nach_neustart_ergibt_letzten_belief() {
        val pfad = tempDatei()
        val belief = BeliefState.of(
            listOf(Hypothese(HypotheseId("h1"), 0.7)),
            Resthypothese(0.3),
        )
        DateiAudit(pfad).anhaengen(BeliefAktualisiert(Zeitstempel(1), belief))

        val rekonstruiert = Rekonstruktion.endBelief(DateiAudit(pfad).lade())

        assertEquals(belief.hypothesen, rekonstruiert?.hypothesen)
        assertEquals(belief.resthypothese, rekonstruiert?.resthypothese)
    }

    // --- Fehler-/Korruptionsverhalten (fail-closed, laut) ------------------

    @Test
    fun trailing_truncation_toleriert_n_minus_1_und_meldet_sichtbar() {
        val pfad = tempDatei()
        val inhalt = buildString {
            append(EreignisSerialisierung.HEADER).append('\n')
            append(EreignisSerialisierung.kodiere(HypotheseHinzugefuegt(Zeitstempel(1), HypotheseId("h1")))).append('\n')
            append(EreignisSerialisierung.kodiere(GateAbgelehnt(Zeitstempel(2), "ok"))).append('\n')
            append("GATE_ABGE") // abgeschnittener Trailing-Record ohne Newline
        }
        schreibe(pfad, inhalt)
        val warnungen = mutableListOf<String>()

        val protokoll = DateiAudit(pfad) { warnungen.add(it) }.lade()

        assertEquals(2, protokoll.groesse) // N-1 rekonstruiert
        assertEquals(1, warnungen.size)
        assertTrue(warnungen.single().contains("abgeschnitten"), "Warnung muss sichtbar sein: ${warnungen.single()}")
    }

    @Test
    fun trailing_truncation_default_warnkanal_wirft_nicht() {
        val pfad = tempDatei()
        schreibe(
            pfad,
            EreignisSerialisierung.HEADER + "\n" +
                EreignisSerialisierung.kodiere(GateAbgelehnt(Zeitstempel(1), "ok")) + "\n" +
                "TRUNC",
        )

        // Default-Warnkanal (System.err) — es darf nicht geworfen werden.
        val protokoll = DateiAudit(pfad).lade()

        assertEquals(1, protokoll.groesse)
    }

    @Test
    fun interior_korruption_wirft() {
        val pfad = tempDatei()
        val inhalt = EreignisSerialisierung.HEADER + "\n" +
            EreignisSerialisierung.kodiere(GateAbgelehnt(Zeitstempel(1), "ok")) + "\n" +
            "MUELL\n" + // defekter Datensatz IM INNEREN
            EreignisSerialisierung.kodiere(GateAbgelehnt(Zeitstempel(2), "auch ok")) + "\n"
        schreibe(pfad, inhalt)

        assertFailsWith<AuditFormatFehler> { DateiAudit(pfad).lade() }
    }

    @Test
    fun fehlender_header_wirft() {
        val pfad = tempDatei()
        schreibe(pfad, EreignisSerialisierung.kodiere(GateAbgelehnt(Zeitstempel(1), "ohne header")) + "\n")

        assertFailsWith<AuditFormatFehler> { DateiAudit(pfad).lade() }
    }

    @Test
    fun ordnungsverletzung_im_store_wirft() {
        val pfad = tempDatei()
        val inhalt = EreignisSerialisierung.HEADER + "\n" +
            EreignisSerialisierung.kodiere(GateAbgelehnt(Zeitstempel(10), "spaet")) + "\n" +
            EreignisSerialisierung.kodiere(GateAbgelehnt(Zeitstempel(5), "frueh")) + "\n" // rueckdatiert
        schreibe(pfad, inhalt)

        assertFailsWith<AuditFormatFehler> { DateiAudit(pfad).lade() }
    }

    @Test
    fun schreibfehler_wirft_auditschreibfehler() {
        // Ein Verzeichnispfad ist nicht beschreibbar → IOException → fail-closed.
        val verzeichnis = Files.createTempDirectory("audit-file-test-dir")

        assertFailsWith<AuditSchreibFehler> {
            DateiAudit(verzeichnis).anhaengen(GateAbgelehnt(Zeitstempel(1), "x"))
        }
    }

    @Test
    fun lesefehler_wirft_auditlesefehler() {
        // Ein Verzeichnispfad ist nicht als Datei lesbar → IOException → fail-closed.
        val verzeichnis = Files.createTempDirectory("audit-file-test-dir")

        assertFailsWith<AuditLeseFehler> { DateiAudit(verzeichnis).lade() }
    }

    // --- Resume nach Crash: Append darf nie über ein Fragment verkleben (HIGH-1) ---

    @Test
    fun resume_append_nach_trailing_truncation_haengt_sauber_an_und_laedt_alles() {
        val pfad = tempDatei()
        val r1 = HypotheseHinzugefuegt(Zeitstempel(1), HypotheseId("h1"))
        val r2 = GateAbgelehnt(Zeitstempel(2), "ok")
        // Store mit abgeschnittenem Trailing-Fragment (Crash waehrend anhaengen).
        schreibe(
            pfad,
            EreignisSerialisierung.HEADER + "\n" +
                EreignisSerialisierung.kodiere(r1) + "\n" +
                EreignisSerialisierung.kodiere(r2) + "\n" +
                "GATE_ABGE", // newline-loser Rest
        )

        val r3 = GateAbgelehnt(Zeitstempel(3), "resume")
        DateiAudit(pfad).anhaengen(r3) // Resume nach Neustart

        val protokoll = DateiAudit(pfad).lade()

        // Fragment fallen gelassen, sauberer Append, kein Wurf, N-1 + neu ladbar.
        assertEquals(listOf(r1, r2, r3), protokoll.ereignisse)
    }

    @Test
    fun resume_nach_mittiger_wert_truncation_fabriziert_keinen_datensatz() {
        val pfad = tempDatei()
        val r1 = HypotheseHinzugefuegt(Zeitstempel(1), HypotheseId("h1"))
        // Abgeschnitten MITTEN im Feld-Wert eines Approval-Records (=-haltige Tokens):
        // ohne Heilung wuerde der Folge-Append hier still einen Datensatz fabrizieren.
        schreibe(
            pfad,
            EreignisSerialisierung.HEADER + "\n" +
                EreignisSerialisierung.kodiere(r1) + "\n" +
                "APPROVAL_ERTEILT\tts=2\tdigest=d\tkanal=local\tnonce=n\tantwort=teilg", // kein '\n'
        )

        val r2 = GateAbgelehnt(Zeitstempel(3), "resume")
        DateiAudit(pfad).anhaengen(r2)

        val protokoll = DateiAudit(pfad).lade()

        // Exakt r1 + r2 — kein aus Fragment + neuem Record fabrizierter Approval-Datensatz.
        assertEquals(listOf(r1, r2), protokoll.ereignisse)
    }

    @Test
    fun resume_nach_abgeschnittenem_header_beginnt_frisch() {
        val pfad = tempDatei()
        schreibe(pfad, "beliefaud") // abgeschnittener Header, kein '\n'

        val r1 = GateAbgelehnt(Zeitstempel(1), "erster")
        DateiAudit(pfad).anhaengen(r1)

        val protokoll = DateiAudit(pfad).lade()

        assertEquals(listOf(r1), protokoll.ereignisse)
    }

    @Test
    fun resume_meldet_verworfenes_fragment_sichtbar() {
        val pfad = tempDatei()
        schreibe(
            pfad,
            EreignisSerialisierung.HEADER + "\n" +
                EreignisSerialisierung.kodiere(GateAbgelehnt(Zeitstempel(1), "ok")) + "\n" +
                "GATE_ABGE", // Crash-Fragment
        )
        val warnungen = mutableListOf<String>()

        DateiAudit(pfad) { warnungen.add(it) }.anhaengen(GateAbgelehnt(Zeitstempel(2), "resume"))

        assertEquals(1, warnungen.size)
        assertTrue(warnungen.single().contains("Fragment"), "Drop muss sichtbar sein: ${warnungen.single()}")
    }

    @Test
    fun sauberer_append_meldet_keine_warnung() {
        val pfad = tempDatei()
        val warnungen = mutableListOf<String>()
        val audit = DateiAudit(pfad) { warnungen.add(it) }

        audit.anhaengen(GateAbgelehnt(Zeitstempel(1), "a")) // erster Append (Datei fehlt)
        audit.anhaengen(GateAbgelehnt(Zeitstempel(2), "b")) // Folge-Append (Datei endet mit '\n')

        assertTrue(warnungen.isEmpty(), "Sauberer Append darf nicht warnen: $warnungen")
    }
}
