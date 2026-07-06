package dev.beliefagent.adapter.llm.koog

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import dev.beliefagent.application.belief.aktualisieren.ports.LlmPort
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.Likelihoods
import kotlinx.coroutines.runBlocking

/**
 * Koog-Adapter hinter dem [LlmPort] (ARC-08). Er kann entweder mit einem Koog
 * [LLMClient] oder einem [PromptExecutor] gebaut werden. Koog liefert die
 * strukturierte Einschaetzung; belief-agent orchestriert und gated.
 */
class KoogLlmPort(
    private val runner: KoogPromptRunner,
    private val promptFactory: KoogLikelihoodPromptFactory = KoogLikelihoodPromptFactory(),
    private val parser: KoogLikelihoodParser = StrictKoogLikelihoodParser(),
) : LlmPort {

    companion object {
        fun fromPromptExecutor(
            executor: PromptExecutor,
            model: LLModel,
            promptFactory: KoogLikelihoodPromptFactory = KoogLikelihoodPromptFactory(),
            parser: KoogLikelihoodParser = StrictKoogLikelihoodParser(),
        ): KoogLlmPort = KoogLlmPort(
            runner = KoogPromptRunner { promptText ->
                runBlocking {
                    executor.execute(prompt("belief-agent-likelihoods") { user(promptText) }, model).textContent()
                }
            },
            promptFactory = promptFactory,
            parser = parser,
        )

        fun fromLlmClient(
            client: LLMClient,
            model: LLModel,
            promptFactory: KoogLikelihoodPromptFactory = KoogLikelihoodPromptFactory(),
            parser: KoogLikelihoodParser = StrictKoogLikelihoodParser(),
        ): KoogLlmPort = KoogLlmPort(
            runner = KoogPromptRunner { promptText ->
                runBlocking {
                    client.execute(prompt("belief-agent-likelihoods") { user(promptText) }, model).textContent()
                }
            },
            promptFactory = promptFactory,
            parser = parser,
        )
    }

    override fun likelihoods(beobachtung: Beobachtung, prior: BeliefState): Likelihoods {
        val request = KoogLikelihoodRequest.from(beobachtung, prior)
        val antwort = runner.execute(promptFactory.prompt(request))
        return parser.parse(antwort).toLikelihoods(prior)
    }
}

fun interface KoogPromptRunner {
    fun execute(prompt: String): String
}

class KoogLikelihoodPromptFactory {
    fun prompt(request: KoogLikelihoodRequest): String = buildString {
        appendLine("Schaetze Likelihoods P(Evidenz | Hypothese) fuer belief-agent.")
        appendLine("belief-agent orchestriert; Koog liefert nur strukturierte Einschaetzungen.")
        appendLine("Antworte ausschliesslich mit gueltigem JSON in dieser Form:")
        appendLine("""{"proHypothese":{"hypothese-id":0.0},"resthypothese":0.0}""")
        appendLine("Regeln:")
        appendLine("- genau eine Zahl >= 0 fuer jede bekannte Hypothese liefern")
        appendLine("- keine unbekannten Hypothesen hinzufuegen")
        appendLine("- resthypothese als eigene Zahl >= 0 liefern")
        appendLine("- keine Aktion vorschlagen oder ausfuehren")
        appendLine("Beobachtung:")
        appendLine("quelle=${request.quelle}")
        appendLine("zeitstempel=${request.zeitstempel}")
        appendLine("evidenz=${request.evidenz}")
        appendLine("Bekannte Hypothesen mit Prior:")
        request.hypothesen.forEach { h ->
            appendLine("- ${h.id}: ${h.wahrscheinlichkeit}")
        }
        appendLine("Resthypothese-Prior: ${request.resthypothese}")
    }
}

data class KoogLikelihoodRequest(
    val quelle: String,
    val zeitstempel: Long,
    val evidenz: String,
    val hypothesen: List<KoogHypotheseSnapshot>,
    val resthypothese: Double,
) {
    companion object {
        fun from(beobachtung: Beobachtung, prior: BeliefState): KoogLikelihoodRequest = KoogLikelihoodRequest(
            quelle = beobachtung.quelle.name,
            zeitstempel = beobachtung.zeitstempel.epochMillis,
            evidenz = beobachtung.evidenz.beschreibung,
            hypothesen = prior.hypothesen.map { KoogHypotheseSnapshot(it.id.wert, it.wahrscheinlichkeit) },
            resthypothese = prior.resthypothese.wahrscheinlichkeit,
        )
    }
}

data class KoogHypotheseSnapshot(
    val id: String,
    val wahrscheinlichkeit: Double,
)

data class KoogLikelihoodResponse(
    val proHypothese: Map<String, Double>,
    val resthypothese: Double,
) {
    fun toLikelihoods(prior: BeliefState): Likelihoods {
        val erwarteteIds = prior.hypothesen.map { it.id.wert }.toSet()
        val gelieferteIds = proHypothese.keys
        require(gelieferteIds == erwarteteIds) {
            val fehlend = erwarteteIds - gelieferteIds
            val unbekannt = gelieferteIds - erwarteteIds
            "LLM-Antwort muss exakt die bekannten Hypothesen bewerten; fehlend=$fehlend, unbekannt=$unbekannt"
        }
        require(resthypothese.isFinite() && resthypothese >= 0.0) {
            "Resthypothesen-Likelihood ungueltig: $resthypothese"
        }
        return Likelihoods(
            proHypothese = prior.hypothesen.associate { h ->
                val wert = proHypothese.getValue(h.id.wert)
                require(wert.isFinite() && wert >= 0.0) {
                    "Likelihood fuer '${h.id.wert}' ungueltig: $wert"
                }
                HypotheseId(h.id.wert) to wert
            },
            resthypothese = resthypothese,
        )
    }
}

interface KoogLikelihoodParser {
    fun parse(raw: String): KoogLikelihoodResponse
}

class StrictKoogLikelihoodParser : KoogLikelihoodParser {
    private val objectMapper = ObjectMapper(
        JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build(),
    )

    override fun parse(raw: String): KoogLikelihoodResponse {
        val root = parseJson(raw)
        require(root.isObject) {
            "LLM-Antwort muss ein JSON-Objekt sein"
        }
        val topLevel = fieldNames(root)
        require(topLevel == setOf("proHypothese", "resthypothese")) {
            "LLM-Antwort muss exakt proHypothese und resthypothese enthalten: $topLevel"
        }

        val hypothesen = root.get("proHypothese")
        require(hypothesen.isObject) {
            "proHypothese muss ein JSON-Objekt sein"
        }
        val rest = root.get("resthypothese")
        require(rest.isNumber) {
            "resthypothese muss eine Zahl sein"
        }
        return KoogLikelihoodResponse(
            proHypothese = readLikelihoodMap(hypothesen),
            resthypothese = rest.asDouble(),
        )
    }

    private fun parseJson(raw: String): JsonNode = try {
        objectMapper.readTree(raw.trim())
    } catch (e: JsonProcessingException) {
        val message = if (e.message.orEmpty().contains("Duplicate field")) {
            "LLM-Antwort enthaelt doppelte JSON-Felder"
        } else {
            "LLM-Antwort muss gueltiges JSON sein"
        }
        throw IllegalArgumentException(message, e)
    }

    private fun fieldNames(root: JsonNode): Set<String> {
        val result = linkedSetOf<String>()
        val fields = root.fieldNames()
        while (fields.hasNext()) result += fields.next()
        return result
    }

    private fun readLikelihoodMap(hypothesen: JsonNode): Map<String, Double> {
        val result = linkedMapOf<String, Double>()
        fieldNames(hypothesen).forEach { hypotheseId ->
            val likelihood = hypothesen.get(hypotheseId)
            require(likelihood.isNumber) {
                "Likelihood fuer '$hypotheseId' muss eine Zahl sein"
            }
            result[hypotheseId] = likelihood.asDouble()
        }
        return result
    }
}
