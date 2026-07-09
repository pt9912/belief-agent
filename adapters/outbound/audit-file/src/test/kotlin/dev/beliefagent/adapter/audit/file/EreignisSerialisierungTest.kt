package dev.beliefagent.adapter.audit.file

import dev.beliefagent.domain.belief.AktionVorgeschlagen
import dev.beliefagent.domain.belief.ApprovalAngefragt
import dev.beliefagent.domain.belief.ApprovalErteilt
import dev.beliefagent.domain.belief.ApprovalFehler
import dev.beliefagent.domain.belief.ApprovalVerweigert
import dev.beliefagent.domain.belief.BeliefAktualisiert
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.BeobachtungErfasst
import dev.beliefagent.domain.belief.EskalationAngefordert
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.EvidenzReferenz
import dev.beliefagent.domain.belief.GateAbgelehnt
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseHinzugefuegt
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.KonfidenzExternalisiert
import dev.beliefagent.domain.belief.KonfidenzUeberschrieben
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Resthypothese
import dev.beliefagent.domain.belief.Zeitstempel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class EreignisSerialisierungTest {

    private fun belief(): BeliefState =
        BeliefState.of(
            listOf(
                Hypothese(HypotheseId("h1"), 0.5, listOf(EvidenzReferenz("e1"), EvidenzReferenz("e2"))),
                Hypothese(HypotheseId("h2"), 0.3),
            ),
            Resthypothese(0.2),
        )

    /** Alle Nicht-Belief-Ereignistypen; Data-Class-Gleichheit trägt den Vergleich. */
    private fun nichtBeliefEreignisse() = listOf(
        HypotheseHinzugefuegt(Zeitstempel(1), HypotheseId("hypo-1")),
        BeobachtungErfasst(
            Zeitstempel(2),
            Beobachtung(Quelle.BUILD, Zeitstempel(2), Evidenz("151 tests gruen")),
        ),
        AktionVorgeschlagen(Zeitstempel(3), "PR mergen"),
        GateAbgelehnt(Zeitstempel(4), "Konfidenz unter Schwelle"),
        EskalationAngefordert(Zeitstempel(5), "Mensch entscheidet"),
        ApprovalAngefragt(Zeitstempel(6), "digest-abc", "local", "nonce-1"),
        ApprovalErteilt(Zeitstempel(7), "digest-abc", "local", "nonce-1", "antw-1", "ident-1", "ok"),
        ApprovalVerweigert(Zeitstempel(8), "digest-abc", "local", "nonce-1", "antw-2", "ident-2", "nein"),
        ApprovalVerweigert(Zeitstempel(9), "digest-abc", "local", "nonce-1", null, null, "eof"),
        ApprovalFehler(Zeitstempel(10), "digest-abc", "local", "nonce-1", null, null, "kanalfehler"),
        KonfidenzExternalisiert(Zeitstempel(11), "ref-1", 0.75, "replay", 3),
        KonfidenzUeberschrieben(Zeitstempel(12), "ref-1", 0.75, 0.4, "manuelle Korrektur", 4),
    )

    @Test
    fun round_trip_erhaelt_alle_nicht_belief_ereignisse() {
        for (ereignis in nichtBeliefEreignisse()) {
            val zeile = EreignisSerialisierung.kodiere(ereignis)
            assertFalse(zeile.contains('\n'), "Kodierte Zeile darf kein Newline enthalten: $zeile")
            assertEquals(ereignis, EreignisSerialisierung.dekodiere(zeile), "Round-Trip für $ereignis")
        }
    }

    @Test
    fun round_trip_erhaelt_verschachtelten_belief_state() {
        val original = BeliefAktualisiert(Zeitstempel(20), belief())

        val dekodiert = EreignisSerialisierung.dekodiere(EreignisSerialisierung.kodiere(original))

        assertEquals(BeliefAktualisiert::class, dekodiert::class)
        dekodiert as BeliefAktualisiert
        assertEquals(original.zeitstempel, dekodiert.zeitstempel)
        // BeliefState hat keine Wert-Gleichheit → feldweise vergleichen.
        assertEquals(original.belief.hypothesen, dekodiert.belief.hypothesen)
        assertEquals(original.belief.resthypothese, dekodiert.belief.resthypothese)
    }

    @Test
    fun escaping_erhaelt_tab_newline_backslash_und_gleichheitszeichen() {
        val original = AktionVorgeschlagen(Zeitstempel(1), "a\tb\nc\\d=e\rf")

        val zeile = EreignisSerialisierung.kodiere(original)

        assertFalse(zeile.contains('\n'), "Newline im Wert darf die Record-Grenze nicht brechen")
        assertFalse(zeile.contains('\r'), "CR im Wert muss escaped sein")
        assertEquals(original, EreignisSerialisierung.dekodiere(zeile))
    }

    @Test
    fun nullable_felder_werden_als_null_zurueckgegeben() {
        val original = ApprovalVerweigert(Zeitstempel(1), "d", "local", "n", null, null, "eof")

        val dekodiert = EreignisSerialisierung.dekodiere(EreignisSerialisierung.kodiere(original))

        dekodiert as ApprovalVerweigert
        assertNull(dekodiert.antwortReferenz)
        assertNull(dekodiert.identitaetsReferenz)
    }

    @Test
    fun unbekannter_tag_wirft() {
        assertFailsWith<AuditFormatFehler> { EreignisSerialisierung.dekodiere("UNBEKANNT\tts=1") }
    }

    @Test
    fun fehlendes_pflichtfeld_wirft() {
        assertFailsWith<AuditFormatFehler> { EreignisSerialisierung.dekodiere("GATE_ABGELEHNT\tts=1") }
    }

    @Test
    fun ungueltiger_zeitstempel_wirft() {
        assertFailsWith<AuditFormatFehler> {
            EreignisSerialisierung.dekodiere("GATE_ABGELEHNT\tts=abc\tgrund=x")
        }
    }

    @Test
    fun verletzte_domaeneninvariante_wird_zu_format_fehler() {
        // Leerer Digest verletzt die Approval-Konstruktor-Invariante (require).
        assertFailsWith<AuditFormatFehler> {
            EreignisSerialisierung.dekodiere("APPROVAL_ANGEFRAGT\tts=1\tdigest=\tkanal=k\tnonce=n")
        }
    }

    @Test
    fun nicht_normierter_belief_wird_zu_format_fehler() {
        // Σp = 0.9 ≠ 1 → BeliefState.of wirft → sichtbarer Format-Fehler.
        assertFailsWith<AuditFormatFehler> {
            EreignisSerialisierung.dekodiere(
                "BELIEF_AKTUALISIERT\tts=1\trestp=0.4\thn=1\th0.id=h1\th0.p=0.5\th0.evn=0",
            )
        }
    }

    @Test
    fun feld_ohne_gleichheitszeichen_wirft() {
        assertFailsWith<AuditFormatFehler> { EreignisSerialisierung.dekodiere("GATE_ABGELEHNT\tkaputt") }
    }

    @Test
    fun leere_zeile_wirft() {
        assertFailsWith<AuditFormatFehler> { EreignisSerialisierung.dekodiere("") }
    }

    @Test
    fun unbekannte_escape_sequenz_wirft() {
        assertFailsWith<AuditFormatFehler> {
            EreignisSerialisierung.dekodiere("GATE_ABGELEHNT\tts=1\tgrund=a\\xb")
        }
    }

    @Test
    fun unvollstaendige_escape_sequenz_am_ende_wirft() {
        assertFailsWith<AuditFormatFehler> {
            EreignisSerialisierung.dekodiere("GATE_ABGELEHNT\tts=1\tgrund=abc\\")
        }
    }

    @Test
    fun optionales_feld_ohne_praesenz_marker_wirft() {
        assertFailsWith<AuditFormatFehler> {
            EreignisSerialisierung.dekodiere(
                "APPROVAL_VERWEIGERT\tts=1\tdigest=d\tkanal=k\tnonce=n\tantwort=roh\tidentitaet=null\tgrund=g",
            )
        }
    }

    @Test
    fun negative_hypothesen_anzahl_wirft() {
        assertFailsWith<AuditFormatFehler> {
            EreignisSerialisierung.dekodiere("BELIEF_AKTUALISIERT\tts=1\trestp=1.0\thn=-1")
        }
    }
}
