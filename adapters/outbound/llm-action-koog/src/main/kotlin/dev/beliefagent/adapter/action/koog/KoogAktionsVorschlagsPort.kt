package dev.beliefagent.adapter.action.koog

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import dev.beliefagent.application.belief.aktionsvorschlag.dto.AktionsVorschlag
import dev.beliefagent.application.belief.aktionsvorschlag.ports.AktionsVorschlagsPort
import dev.beliefagent.domain.belief.BeliefState
import kotlinx.coroutines.runBlocking

/**
 * Koog-Adapter hinter dem [AktionsVorschlagsPort] (ARC-08, slice-043) für die
 * abgegrenzte Modellaufgabe „Aktionen vorschlagen" (LH-FA-LLM-002). Er ist der
 * **zweite** echte Framework-Pfad neben dem LangChain4j-Adapter aus slice-042 und
 * liefert wie dieser ausschließlich **rohe** [AktionsVorschlag]-Werte; Gate,
 * Freigabe und Ausführung bleiben im Kern (`LH-FA-POL-006`).
 *
 * **Bewusste Duplikation je Framework-Pfad (§9 F-2).** Parität zwischen Koog und
 * LangChain4j entsteht nicht über ein geteiltes Produktivmodul (das eine
 * Adapter→Adapter-Kante `ARC-08` oder eine Framework-Dep im Core `ADR-0001`/
 * `ADR-0003` erzwänge), sondern über die je Pfad eigene Prompt-Factory, den
 * eigenen strikten Parser und die gemeinsame Contract-Test-Matrix. Dieser Adapter
 * duppliziert daher bewusst Prompt-Factory und Parser des LangChain4j-Pfads —
 * Repo-Präzedenz `llm-koog`/`llm-langchain4j` für den `LlmPort`.
 *
 * **Schicht-Trennung (§9 F-1, spiegelt slice-042).** Der Adapter prüft nur
 * **Wire-/Deserialisierungs-Integrität** (erlaubte/vollständige JSON-Felder, Typ/
 * Shape, endliche Zahlen) und mappt auf die primitiven DTO-Felder. Die **Semantik**
 * (unbekannte Hypothese, gültige Wirkungsklasse, Evidenz-Auflösung/Nicht-Leere,
 * Konfidenz-Bereich `[0,1]`) validiert der Use Case `AktionsVorschlagen`; der Adapter
 * dupliziert sie **nicht** und **kann** die Evidenzprüfung nicht leisten, weil
 * [vorschlaege] keinen Evidenz-Kontext trägt.
 *
 * **Fail-closed-Parität (§9 F-3, `LH-QA-02`).** Leere/`[]`-Antwort → `emptyList()`
 * (legitim „kein Vorschlag"); ein einzelner wire-defekter Vorschlag wird
 * **verworfen** (valide bleiben) und über den Warn-Kanal sichtbar gemeldet; eine
 * unparsebare/falsch-geshapte Antwort, **Tokens hinter dem ersten JSON-Wert** oder
 * ein Provider-Ausfall werden **geworfen** — „Provider unreachable" bleibt von „kein
 * Vorschlag" unterscheidbar. Der [StrictAktionsVorschlagParser] übernimmt den
 * Trailing-Token-Guard (`nextToken()==null`) aus slice-042 und spiegelt **nicht**
 * die schwächere `readTree(raw.trim())`-Vorlage aus `KoogLlmPort.kt`.
 */
class KoogAktionsVorschlagsPort(
    private val runner: KoogAktionsPromptRunner,
    private val promptFactory: AktionsVorschlagPromptFactory = AktionsVorschlagPromptFactory(),
    private val parser: AktionsVorschlagParser = StrictAktionsVorschlagParser(),
) : AktionsVorschlagsPort {

    companion object {
        private const val PROMPT_ID = "belief-agent-aktionsvorschlaege"

        fun fromPromptExecutor(
            executor: PromptExecutor,
            model: LLModel,
            promptFactory: AktionsVorschlagPromptFactory = AktionsVorschlagPromptFactory(),
            parser: AktionsVorschlagParser = StrictAktionsVorschlagParser(),
        ): KoogAktionsVorschlagsPort = KoogAktionsVorschlagsPort(
            runner = KoogAktionsPromptRunner { promptText ->
                runBlocking {
                    executor.execute(prompt(PROMPT_ID) { user(promptText) }, model).textContent()
                }
            },
            promptFactory = promptFactory,
            parser = parser,
        )

        fun fromLlmClient(
            client: LLMClient,
            model: LLModel,
            promptFactory: AktionsVorschlagPromptFactory = AktionsVorschlagPromptFactory(),
            parser: AktionsVorschlagParser = StrictAktionsVorschlagParser(),
        ): KoogAktionsVorschlagsPort = KoogAktionsVorschlagsPort(
            runner = KoogAktionsPromptRunner { promptText ->
                runBlocking {
                    client.execute(prompt(PROMPT_ID) { user(promptText) }, model).textContent()
                }
            },
            promptFactory = promptFactory,
            parser = parser,
        )

        fun fromLlmClient(
            client: LLMClient,
            providerId: String,
            providerName: String,
            modelId: String,
            promptFactory: AktionsVorschlagPromptFactory = AktionsVorschlagPromptFactory(),
            parser: AktionsVorschlagParser = StrictAktionsVorschlagParser(),
        ): KoogAktionsVorschlagsPort = fromLlmClient(
            client = client,
            model = LLModel(LLMProvider(providerId, providerName), modelId),
            promptFactory = promptFactory,
            parser = parser,
        )

        fun fromLlmClient(
            clientClass: String,
            providerId: String,
            providerName: String,
            modelId: String,
            promptFactory: AktionsVorschlagPromptFactory = AktionsVorschlagPromptFactory(),
            parser: AktionsVorschlagParser = StrictAktionsVorschlagParser(),
        ): KoogAktionsVorschlagsPort = fromLlmClient(
            client = instantiateLlmClient(clientClass),
            providerId = providerId,
            providerName = providerName,
            modelId = modelId,
            promptFactory = promptFactory,
            parser = parser,
        )

        private fun instantiateLlmClient(clientClass: String): LLMClient {
            val loadedClass = runCatching { Class.forName(clientClass) as Class<*> }
                .getOrElse { cause -> throw IllegalArgumentException("Could not load KOOG_CLIENT_CLASS='$clientClass'.", cause) }

            require(LLMClient::class.java.isAssignableFrom(loadedClass)) {
                "KOOG_CLIENT_CLASS='$clientClass' is not an ai.koog.prompt.executor.clients.LLMClient."
            }

            val ctor = runCatching { loadedClass.getDeclaredConstructor() }
                .getOrElse { cause ->
                    throw IllegalArgumentException("KOOG_CLIENT_CLASS='$clientClass' needs a public no-arg constructor.", cause)
                }

            return runCatching { ctor.newInstance() as LLMClient }
                .getOrElse { cause -> throw IllegalArgumentException("Could not instantiate KOOG_CLIENT_CLASS='$clientClass'.", cause) }
        }
    }

    override fun vorschlaege(belief: BeliefState): List<AktionsVorschlag> {
        val request = AktionsVorschlagRequest.from(belief)
        val antwort = runner.execute(promptFactory.prompt(request))
        return parser.parse(antwort)
    }
}

/** Transport-Grenze zum Koog-Runner; als `fun interface` injizierbar (Stub in Tests, kein Netz). */
fun interface KoogAktionsPromptRunner {
    fun execute(prompt: String): String
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
 * Strikter Wire-Parser (bewusste Duplikation des slice-042-Parsers, §9 F-2/F-3):
 * erzwingt exakt die erlaubten JSON-Felder, Typ/Shape und endliche Zahlen; keine
 * Semantik. Verwirft einen einzelnen wire-defekten Vorschlag (sichtbar über
 * [warnung]) und wirft nur bei einer strukturell kaputten Gesamtantwort.
 *
 * **Trailing-Token-Guard ist Paritäts-Pflicht:** der Parser liest exakt einen
 * JSON-Wert und wirft, sobald danach weitere Tokens folgen. Er spiegelt damit
 * **nicht** die schwächere `readTree(raw.trim())`-Vorlage aus `KoogLlmPort.kt`,
 * die die in slice-042 (SR-F1) geschlossene Trailing-Token-Nachsicht zurückbrächte.
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
            // Wurf (slice-042 SR-F1), nicht „kein Vorschlag" (LH-QA-02).
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
