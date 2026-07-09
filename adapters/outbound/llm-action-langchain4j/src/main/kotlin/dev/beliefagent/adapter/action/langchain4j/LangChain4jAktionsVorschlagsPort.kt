package dev.beliefagent.adapter.action.langchain4j

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import dev.beliefagent.application.belief.aktionsvorschlag.dto.AktionsVorschlag
import dev.beliefagent.application.belief.aktionsvorschlag.ports.AktionsVorschlagsPort
import dev.beliefagent.domain.belief.BeliefState
import dev.langchain4j.model.chat.ChatModel

/**
 * LangChain4j-Adapter hinter dem [AktionsVorschlagsPort] (ARC-08, slice-042) für
 * die abgegrenzte Modellaufgabe „Aktionen vorschlagen" (LH-FA-LLM-002). Er liefert
 * ausschließlich **rohe** [AktionsVorschlag]-Werte; Gate, Freigabe und Ausführung
 * bleiben im Kern (`LH-FA-POL-006`).
 *
 * **Schicht-Trennung (§9 F-1, spiegelt slice-041 DR-F3).** Der Adapter prüft nur
 * **Wire-/Deserialisierungs-Integrität** (erlaubte/vollständige JSON-Felder, Typ/
 * Shape, endliche Zahlen) und mappt auf die primitiven DTO-Felder. Die **Semantik**
 * (unbekannte Hypothese, gültige Wirkungsklasse, Evidenz-Auflösung/Nicht-Leere,
 * Konfidenz-Bereich `[0,1]`) validiert der Use Case `AktionsVorschlagen`; der Adapter
 * dupliziert sie **nicht** und **kann** die Evidenzprüfung nicht leisten, weil
 * [vorschlaege] keinen Evidenz-Kontext trägt.
 *
 * **Fehler-Signalisierung je Klasse (§9 F-2, `LH-QA-02`).** Leere/`[]`-Antwort →
 * `emptyList()` (legitim „kein Vorschlag", Fake-Parität); ein einzelner
 * wire-defekter Vorschlag wird **verworfen** (valide bleiben) und über den
 * Warn-Kanal des Parsers sichtbar gemeldet; eine unparsebare/falsch-geshapte Antwort oder ein
 * Provider-Ausfall wird **geworfen** ([AktionsVorschlagAntwortFehler] bzw. die
 * Transport-Exception) — „Provider unreachable" bleibt unterscheidbar von „kein
 * Vorschlag". Der Wurf propagiert außerhalb des per-Vorschlag-`runCatching` im Use
 * Case.
 */
class LangChain4jAktionsVorschlagsPort(
    private val chat: LangChain4jAktionsChatRunner,
    private val promptFactory: AktionsVorschlagPromptFactory = AktionsVorschlagPromptFactory(),
    private val parser: AktionsVorschlagParser = StrictAktionsVorschlagParser(),
) : AktionsVorschlagsPort {

    companion object {
        fun fromChatModel(
            chatModel: ChatModel,
            promptFactory: AktionsVorschlagPromptFactory = AktionsVorschlagPromptFactory(),
            parser: AktionsVorschlagParser = StrictAktionsVorschlagParser(),
        ): LangChain4jAktionsVorschlagsPort = LangChain4jAktionsVorschlagsPort(
            chat = LangChain4jAktionsChatRunner { prompt -> chatModel.chat(prompt) },
            promptFactory = promptFactory,
            parser = parser,
        )
    }

    override fun vorschlaege(belief: BeliefState): List<AktionsVorschlag> {
        val request = AktionsVorschlagRequest.from(belief)
        val antwort = chat.chat(promptFactory.prompt(request))
        return parser.parse(antwort)
    }
}

/** Transport-Grenze zum Provider; als `fun interface` injizierbar (Stub in Tests, kein Netz). */
fun interface LangChain4jAktionsChatRunner {
    fun chat(prompt: String): String
}

/** Sichtbarer Adapter-Fehler für eine unparsebare/falsch-geshapte Provider-Antwort (`LH-QA-02`). */
class AktionsVorschlagAntwortFehler(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class AktionsVorschlagPromptFactory {
    fun prompt(request: AktionsVorschlagRequest): String = buildString {
        appendLine("Schlage Aktionen für belief-agent vor (nur Vorschläge, keine Freigabe/Ausführung).")
        appendLine("belief-agent orchestriert und validiert; du lieferst nur strukturierte Rohvorschläge.")
        appendLine("Antworte ausschließlich mit einem gültigen JSON-Array in dieser Form:")
        appendLine(
            """[{"beschreibung":"...","hypotheseId":"...","wirkungsklasse":"...",""" +
                """"pSuccess":0.0,"konfidenzReferenz":"...","stuetzendeEvidenz":["..."]}]""",
        )
        appendLine("Regeln:")
        appendLine("- jedes Objekt trägt genau diese sechs Felder, keine weiteren")
        appendLine("- schlage Aktionen nur für die unten genannten bekannten Hypothesen vor")
        appendLine("- pSuccess ist eine Zahl; keine Aktion freigeben oder ausführen")
        appendLine("- ohne sinnvollen Vorschlag ein leeres Array [] liefern")
        appendLine("Bekannte Hypothesen mit Prior:")
        request.hypothesen.forEach { h ->
            appendLine("- ${h.id}: ${h.wahrscheinlichkeit}")
        }
        appendLine("Resthypothese-Prior: ${request.resthypothese}")
    }
}

data class AktionsVorschlagRequest(
    val hypothesen: List<HypotheseSnapshot>,
    val resthypothese: Double,
) {
    companion object {
        fun from(belief: BeliefState): AktionsVorschlagRequest = AktionsVorschlagRequest(
            hypothesen = belief.hypothesen.map { HypotheseSnapshot(it.id.wert, it.wahrscheinlichkeit) },
            resthypothese = belief.resthypothese.wahrscheinlichkeit,
        )
    }
}

data class HypotheseSnapshot(
    val id: String,
    val wahrscheinlichkeit: Double,
)

interface AktionsVorschlagParser {
    fun parse(raw: String): List<AktionsVorschlag>
}

/**
 * Strikter Wire-Parser: erzwingt exakt die erlaubten JSON-Felder, Typ/Shape und
 * endliche Zahlen; keine Semantik. Verwirft einen einzelnen wire-defekten Vorschlag
 * (sichtbar über [warnung]) und wirft nur bei einer strukturell kaputten Gesamtantwort.
 */
class StrictAktionsVorschlagParser(
    private val warnung: (String) -> Unit = { System.err.println(it) },
) : AktionsVorschlagParser {

    // Doppelte JSON-Felder sind ein tokenizer-seitiges Problem der GESAMTANTWORT
    // und fallen unter „unparsebar → Wurf" (§9 F-1) — nicht unter den per-Vorschlag-
    // Verwurf: Jacksons Duplikaterkennung ist Whole-Stream, und nach einer
    // Element-Exception ist die Parser-Position nicht mehr wiederaufsetzbar.
    private val objectMapper = ObjectMapper(
        JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build(),
    )

    override fun parse(raw: String): List<AktionsVorschlag> {
        if (raw.isBlank()) return emptyList() // leere Antwort → kein Vorschlag (legitim)
        val root = parseJson(raw)
        if (!root.isArray) {
            throw AktionsVorschlagAntwortFehler("Aktionsvorschlags-Antwort muss ein JSON-Array sein")
        }
        return root.mapNotNull { element -> vorschlagOderVerwerfen(element) }
    }

    private fun parseJson(raw: String): JsonNode = try {
        objectMapper.createParser(raw.trim()).use { parser ->
            val node: JsonNode = objectMapper.readTree(parser)
                ?: throw AktionsVorschlagAntwortFehler("Aktionsvorschlags-Antwort ist leer")
            // Bytes hinter dem ersten JSON-Wert nicht still ignorieren: eine Antwort
            // mit gültigem Präfix + Trailing-Müll ist strukturell defekt → sichtbarer
            // Wurf (Code-Safety-Review F1), nicht „kein Vorschlag" (LH-QA-02).
            if (parser.nextToken() != null) {
                throw AktionsVorschlagAntwortFehler("Aktionsvorschlags-Antwort enthält Tokens nach dem JSON-Wert")
            }
            node
        }
    } catch (e: JsonProcessingException) {
        val message = if (e.message.orEmpty().contains("Duplicate field")) {
            "Aktionsvorschlags-Antwort enthält doppelte JSON-Felder"
        } else {
            "Aktionsvorschlags-Antwort muss gültiges JSON sein"
        }
        throw AktionsVorschlagAntwortFehler(message, e)
    }

    private fun vorschlagOderVerwerfen(element: JsonNode): AktionsVorschlag? {
        val defekt = wireDefekt(element)
        if (defekt != null) {
            warnung("Aktionsvorschlag verworfen (Wire-Defekt): $defekt")
            return null
        }
        return AktionsVorschlag(
            beschreibung = element.get("beschreibung").asText(),
            hypotheseId = element.get("hypotheseId").asText(),
            wirkungsklasse = element.get("wirkungsklasse").asText(),
            pSuccess = element.get("pSuccess").asDouble(),
            konfidenzReferenz = element.get("konfidenzReferenz").asText(),
            stuetzendeEvidenz = element.get("stuetzendeEvidenz").map { it.asText() },
        )
    }

    /** Gibt den Grund zurück, warum [element] wire-defekt ist, oder `null`, wenn es sauber ist. */
    private fun wireDefekt(element: JsonNode): String? {
        if (!element.isObject) return "Vorschlag ist kein JSON-Objekt"
        val felder = fieldNames(element)
        if (felder != ERLAUBTE_FELDER) return "Feldmenge $felder != erlaubt $ERLAUBTE_FELDER"
        if (!element.get("beschreibung").isTextual) return "beschreibung ist kein String"
        if (!element.get("hypotheseId").isTextual) return "hypotheseId ist kein String"
        if (!element.get("wirkungsklasse").isTextual) return "wirkungsklasse ist kein String"
        val pSuccess = element.get("pSuccess")
        if (!pSuccess.isNumber || !pSuccess.asDouble().isFinite()) return "pSuccess ist keine endliche Zahl"
        if (!element.get("konfidenzReferenz").isTextual) return "konfidenzReferenz ist kein String"
        val evidenz = element.get("stuetzendeEvidenz")
        if (!evidenz.isArray) return "stuetzendeEvidenz ist kein Array"
        if (evidenz.any { !it.isTextual }) return "stuetzendeEvidenz enthält Nicht-Strings"
        return null
    }

    private fun fieldNames(node: JsonNode): Set<String> {
        val result = linkedSetOf<String>()
        val fields = node.fieldNames()
        while (fields.hasNext()) result += fields.next()
        return result
    }

    private companion object {
        val ERLAUBTE_FELDER = setOf(
            "beschreibung", "hypotheseId", "wirkungsklasse", "pSuccess", "konfidenzReferenz", "stuetzendeEvidenz",
        )
    }
}
