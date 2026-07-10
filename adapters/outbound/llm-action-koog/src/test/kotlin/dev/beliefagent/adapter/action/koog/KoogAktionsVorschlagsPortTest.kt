package dev.beliefagent.adapter.action.koog

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import dev.beliefagent.application.belief.aktionsvorschlag.dto.AktionsVorschlag
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.Resthypothese
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Contract-Test-Matrix für den Koog-Aktionsvorschlags-Adapter. Die Wire-/Parser-
 * Fälle spiegeln bewusst `LangChain4jAktionsVorschlagsPortTest` (slice-042), damit
 * beide Framework-Pfade dieselbe Fehlerklassen-Äquivalenz belegen (§9 F-1/F-3).
 * Semantik (unbekannte Hypothese, Wirkungsklasse, Evidenz, `pSuccess`,
 * Konfidenzreferenz) bleibt im Use Case `AktionsVorschlagen` und wird hier **nicht**
 * am Adapter geprüft.
 */
class KoogAktionsVorschlagsPortTest {

    private fun belief(): BeliefState = BeliefState.of(
        listOf(Hypothese(HypotheseId("h1"), 0.6), Hypothese(HypotheseId("h2"), 0.1)),
        Resthypothese(0.3),
    )

    private fun model(): LLModel = LLModel(LLMProvider("test", "Test"), "test-model")

    private fun port(
        antwort: String,
        warnungen: MutableList<String>? = null,
    ): KoogAktionsVorschlagsPort = KoogAktionsVorschlagsPort(
        runner = KoogAktionsPromptRunner { antwort },
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

    // --- Fehler-Signalisierung je Klasse (§9 F-3) --------------------------

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
        // Gültiges Präfix + Trailing-Objekt darf nicht still verworfen werden
        // (slice-042 SR-F1; der Koog-Parser übernimmt den Trailing-Token-Guard).
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
        val port = KoogAktionsVorschlagsPort(
            runner = KoogAktionsPromptRunner { error("provider unreachable") },
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

    // --- §9 F-1: semantisch offene Rohwerte werden DURCHGEREICHT (nicht re-validiert) ---

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
        val port = KoogAktionsVorschlagsPort(
            runner = KoogAktionsPromptRunner { prompt -> gesehenerPrompt = prompt; "[]" },
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

    // --- Koog-Fabriken: Runner-Verdrahtung (spiegelt KoogLlmPortTest) ------

    @Test
    fun factory_nutzt_prompt_executor() {
        val executor = CapturingPromptExecutor("[$gueltig]")
        val port = KoogAktionsVorschlagsPort.fromPromptExecutor(executor, model())

        val result = port.vorschlaege(belief())

        assertEquals("belief-agent-aktionsvorschlaege", executor.promptId)
        assertEquals("test-model", executor.modelId)
        assertEquals(listOf("Log pruefen"), result.map { it.beschreibung })
    }

    @Test
    fun factory_nutzt_llm_client() {
        val client = CapturingLlmClient("[$gueltig]")
        val port = KoogAktionsVorschlagsPort.fromLlmClient(client, model())

        val result = port.vorschlaege(belief())

        assertEquals("belief-agent-aktionsvorschlaege", client.promptId)
        assertEquals("test-model", client.modelId)
        assertEquals(listOf("Log pruefen"), result.map { it.beschreibung })
    }

    @Test
    fun factory_nutzt_llm_client_mit_provider_daten() {
        val client = CapturingLlmClient("[$gueltig]")
        val port = KoogAktionsVorschlagsPort.fromLlmClient(
            client = client,
            providerId = "acme-llm",
            providerName = "ACME",
            modelId = "acme-fast-1",
        )

        val result = port.vorschlaege(belief())

        assertEquals("belief-agent-aktionsvorschlaege", client.promptId)
        assertEquals("acme-fast-1", client.modelId)
        assertEquals(listOf("Log pruefen"), result.map { it.beschreibung })
    }

    @Test
    fun factory_liest_klassenamen_fuer_client() {
        val port = KoogAktionsVorschlagsPort.fromLlmClient(
            clientClass = "dev.beliefagent.adapter.action.koog.CapturingKoogAktionsLlmClientClassName",
            providerId = "acme-llm",
            providerName = "ACME",
            modelId = "acme-fast-2",
        )

        val result = port.vorschlaege(belief())

        assertEquals(listOf("Log pruefen"), result.map { it.beschreibung })
    }

    @Test
    fun factory_weist_unbekannte_klasse_zurueck() {
        // Ablehnungsast 1/4: Klasse nicht ladbar.
        assertFailsWith<IllegalArgumentException> {
            KoogAktionsVorschlagsPort.fromLlmClient(
                clientClass = "dev.beliefagent.adapter.action.koog.GibtEsNicht",
                providerId = "acme-llm",
                providerName = "ACME",
                modelId = "acme-fast-2",
            )
        }
    }

    @Test
    fun factory_weist_nicht_llm_client_klasse_zurueck() {
        // Ablehnungsast 2/4: ladbar, aber kein LLMClient.
        val fehler = assertFailsWith<IllegalArgumentException> {
            KoogAktionsVorschlagsPort.fromLlmClient(
                clientClass = "dev.beliefagent.adapter.action.koog.KoogAktionsClientKeinLlmClient",
                providerId = "acme-llm",
                providerName = "ACME",
                modelId = "acme-fast-2",
            )
        }
        assertTrue(fehler.message!!.contains("is not an"), fehler.message!!)
    }

    @Test
    fun factory_weist_klasse_ohne_no_arg_ctor_zurueck() {
        // Ablehnungsast 3/4: LLMClient, aber kein No-arg-Konstruktor.
        val fehler = assertFailsWith<IllegalArgumentException> {
            KoogAktionsVorschlagsPort.fromLlmClient(
                clientClass = "dev.beliefagent.adapter.action.koog.KoogAktionsClientOhneNoArgCtor",
                providerId = "acme-llm",
                providerName = "ACME",
                modelId = "acme-fast-2",
            )
        }
        assertTrue(fehler.message!!.contains("no-arg constructor"), fehler.message!!)
    }

    @Test
    fun factory_weist_fehlschlagende_instanziierung_zurueck() {
        // Ablehnungsast 4/4: Konstruktor wirft bei der Instanziierung.
        val fehler = assertFailsWith<IllegalArgumentException> {
            KoogAktionsVorschlagsPort.fromLlmClient(
                clientClass = "dev.beliefagent.adapter.action.koog.KoogAktionsClientWirftImKonstruktor",
                providerId = "acme-llm",
                providerName = "ACME",
                modelId = "acme-fast-2",
            )
        }
        assertTrue(fehler.message!!.contains("Could not instantiate"), fehler.message!!)
    }

    private class CapturingPromptExecutor(
        private val response: String,
    ) : PromptExecutor() {
        var promptId: String? = null
        var modelId: String? = null

        override suspend fun execute(
            prompt: Prompt,
            model: LLModel,
            tools: List<ToolDescriptor>,
        ): Message.Assistant {
            promptId = prompt.id
            modelId = model.id
            return Message.Assistant(response, ResponseMetaInfo.Empty)
        }

        override fun executeStreaming(
            prompt: Prompt,
            model: LLModel,
            tools: List<ToolDescriptor>,
        ): Flow<StreamFrame> = emptyFlow()

        override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult =
            ModerationResult(isHarmful = false, categories = emptyMap())

        override fun close() = Unit
    }

    private class CapturingLlmClient(
        private val response: String,
    ) : LLMClient() {
        var promptId: String? = null
        var modelId: String? = null

        override fun llmProvider(): LLMProvider = LLMProvider("test", "Test")

        override suspend fun execute(
            prompt: Prompt,
            model: LLModel,
            tools: List<ToolDescriptor>,
        ): Message.Assistant {
            promptId = prompt.id
            modelId = model.id
            return Message.Assistant(response, ResponseMetaInfo.Empty)
        }

        override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult =
            ModerationResult(isHarmful = false, categories = emptyMap())

        override fun close() = Unit
    }
}
