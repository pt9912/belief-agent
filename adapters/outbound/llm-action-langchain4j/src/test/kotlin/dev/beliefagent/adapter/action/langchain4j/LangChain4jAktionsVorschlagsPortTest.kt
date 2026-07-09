package dev.beliefagent.adapter.action.langchain4j

import dev.beliefagent.application.belief.aktionsvorschlag.dto.AktionsVorschlag
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.Resthypothese
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LangChain4jAktionsVorschlagsPortTest {

    private fun belief(): BeliefState = BeliefState.of(
        listOf(Hypothese(HypotheseId("h1"), 0.6), Hypothese(HypotheseId("h2"), 0.1)),
        Resthypothese(0.3),
    )

    private fun port(
        antwort: String,
        warnungen: MutableList<String>? = null,
    ): LangChain4jAktionsVorschlagsPort = LangChain4jAktionsVorschlagsPort(
        chat = { antwort },
        parser = if (warnungen == null) {
            StrictAktionsVorschlagParser()
        } else {
            StrictAktionsVorschlagParser { warnungen.add(it) }
        },
    )

    private val gueltig =
        """{"beschreibung":"Log pruefen","hypotheseId":"h1","wirkungsklasse":"NUR_LESEND",""" +
            """"pSuccess":0.8,"konfidenzReferenz":"k1","stuetzendeEvidenz":["e1","e2"]}"""

    // --- Erfolgreiche Normalisierung + Wire→DTO-Mapping ---------------------

    @Test
    fun normalisiert_gueltiges_json_array() {
        val result = port("[$gueltig]").vorschlaege(belief())

        assertEquals(
            listOf(AktionsVorschlag("Log pruefen", "h1", "NUR_LESEND", 0.8, "k1", listOf("e1", "e2"))),
            result,
        )
    }

    // --- Fehler-Signalisierung je Klasse (§9 F-2) --------------------------

    @Test
    fun leere_antwort_ist_leer() {
        assertTrue(port("   ").vorschlaege(belief()).isEmpty())
    }

    @Test
    fun leeres_array_ist_leer() {
        assertTrue(port("[]").vorschlaege(belief()).isEmpty())
    }

    @Test
    fun unparsebare_antwort_wirft() {
        assertFailsWith<AktionsVorschlagAntwortFehler> { port("kein json {{{").vorschlaege(belief()) }
    }

    @Test
    fun nicht_array_antwort_wirft() {
        assertFailsWith<AktionsVorschlagAntwortFehler> { port(gueltig).vorschlaege(belief()) }
    }

    @Test
    fun trailing_tokens_nach_json_wert_werfen() {
        // Gültiges Präfix + Trailing-Objekt darf nicht still verworfen werden.
        assertFailsWith<AktionsVorschlagAntwortFehler> {
            port("[$gueltig] {\"leak\":1}").vorschlaege(belief())
        }
    }

    @Test
    fun trailing_muell_nach_leerem_array_wirft() {
        // [] + Müll ist strukturell defekt → Wurf, nicht "kein Vorschlag".
        assertFailsWith<AktionsVorschlagAntwortFehler> { port("[] GARBAGE").vorschlaege(belief()) }
    }

    @Test
    fun json_null_feldwert_wird_verworfen() {
        val nullFeld =
            """{"beschreibung":null,"hypotheseId":"h1","wirkungsklasse":"NUR_LESEND","pSuccess":0.5,""" +
                """"konfidenzReferenz":"k1","stuetzendeEvidenz":["e1"]}"""
        assertTrue(port("[$nullFeld]").vorschlaege(belief()).isEmpty())
    }

    @Test
    fun doppelte_json_felder_werfen() {
        val doppelt =
            """[{"beschreibung":"a","beschreibung":"b","hypotheseId":"h1","wirkungsklasse":"NUR_LESEND",""" +
                """"pSuccess":0.5,"konfidenzReferenz":"k1","stuetzendeEvidenz":["e1"]}]"""
        assertFailsWith<AktionsVorschlagAntwortFehler> { port(doppelt).vorschlaege(belief()) }
    }

    @Test
    fun provider_ausfall_propagiert_sichtbar() {
        val port = LangChain4jAktionsVorschlagsPort(
            chat = { error("provider unreachable") },
        )
        assertFailsWith<IllegalStateException> { port.vorschlaege(belief()) }
    }

    // --- Wire-defekter Einzelvorschlag: verworfen, valide bleiben (sichtbar) ---

    @Test
    fun unbekanntes_feld_wird_verworfen_valide_bleiben() {
        val extra =
            """{"beschreibung":"x","hypotheseId":"h1","wirkungsklasse":"NUR_LESEND","pSuccess":0.5,""" +
                """"konfidenzReferenz":"k1","stuetzendeEvidenz":["e1"],"extra":"boese"}"""
        val warnungen = mutableListOf<String>()

        val result = port("[$extra,$gueltig]", warnungen).vorschlaege(belief())

        assertEquals(1, result.size)
        assertEquals("Log pruefen", result.single().beschreibung)
        assertEquals(1, warnungen.size)
        assertTrue(warnungen.single().contains("verworfen"), warnungen.single())
    }

    @Test
    fun fehlendes_pflichtfeld_wird_verworfen() {
        val ohneEvidenz =
            """{"beschreibung":"x","hypotheseId":"h1","wirkungsklasse":"NUR_LESEND",""" +
                """"pSuccess":0.5,"konfidenzReferenz":"k1"}"""
        val result = port("[$ohneEvidenz,$gueltig]").vorschlaege(belief())
        assertEquals(listOf("Log pruefen"), result.map { it.beschreibung })
    }

    @Test
    fun falscher_typ_wird_verworfen() {
        val pSuccessString =
            """{"beschreibung":"x","hypotheseId":"h1","wirkungsklasse":"NUR_LESEND",""" +
                """"pSuccess":"hoch","konfidenzReferenz":"k1","stuetzendeEvidenz":["e1"]}"""
        assertTrue(port("[$pSuccessString]").vorschlaege(belief()).isEmpty())
    }

    @Test
    fun nicht_endliche_zahl_wird_verworfen() {
        // 1e400 ist syntaktisch gueltiges JSON, ueberlaeuft aber zu Infinity.
        val ueberlauf =
            """{"beschreibung":"x","hypotheseId":"h1","wirkungsklasse":"NUR_LESEND",""" +
                """"pSuccess":1e400,"konfidenzReferenz":"k1","stuetzendeEvidenz":["e1"]}"""
        assertTrue(port("[$ueberlauf]").vorschlaege(belief()).isEmpty())
    }

    @Test
    fun nicht_objekt_element_wird_verworfen() {
        val result = port("""["nur ein string", $gueltig, 42]""").vorschlaege(belief())
        assertEquals(listOf("Log pruefen"), result.map { it.beschreibung })
    }

    @Test
    fun evidenz_mit_nicht_string_wird_verworfen() {
        val evidenzZahl =
            """{"beschreibung":"x","hypotheseId":"h1","wirkungsklasse":"NUR_LESEND","pSuccess":0.5,""" +
                """"konfidenzReferenz":"k1","stuetzendeEvidenz":[1,2]}"""
        assertTrue(port("[$evidenzZahl]").vorschlaege(belief()).isEmpty())
    }

    // --- DR-F1: semantisch offene Rohwerte werden DURCHGEREICHT (nicht re-validiert) ---

    @Test
    fun reicht_semantisch_offene_rohwerte_an_den_use_case_durch() {
        // Unbekannte Hypothese, ungueltige Wirkungsklasse, pSuccess ausserhalb [0,1],
        // leere Evidenz — alles wire-korrekt, semantisch offen: der Adapter darf NICHT
        // ablehnen (das ist Sache von AktionsVorschlagen). Nur wire-korrekt => Pass-Through.
        val offen =
            """{"beschreibung":"","hypotheseId":"unbekannt","wirkungsklasse":"QUATSCH","pSuccess":5.0,""" +
                """"konfidenzReferenz":"k1","stuetzendeEvidenz":[]}"""

        val result = port("[$offen]").vorschlaege(belief())

        assertEquals(
            listOf(AktionsVorschlag("", "unbekannt", "QUATSCH", 5.0, "k1", emptyList())),
            result,
        )
    }

    // --- Prompt-Inhalt ------------------------------------------------------

    @Test
    fun prompt_enthaelt_hypothesen_und_regeln() {
        var gesehenerPrompt = ""
        val port = LangChain4jAktionsVorschlagsPort(
            chat = { prompt -> gesehenerPrompt = prompt; "[]" },
        )

        port.vorschlaege(belief())

        assertTrue(gesehenerPrompt.contains("h1"), gesehenerPrompt)
        assertTrue(gesehenerPrompt.contains("genau diese sechs Felder"), gesehenerPrompt)
        assertTrue(gesehenerPrompt.contains("keine Aktion freigeben oder ausführen"), gesehenerPrompt)
    }

    @Test
    fun sauberer_pass_meldet_keine_warnung() {
        val warnungen = mutableListOf<String>()
        port("[$gueltig]", warnungen).vorschlaege(belief())
        assertTrue(warnungen.isEmpty(), warnungen.toString())
    }
}
