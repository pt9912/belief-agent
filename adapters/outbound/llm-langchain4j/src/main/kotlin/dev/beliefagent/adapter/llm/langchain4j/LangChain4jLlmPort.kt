package dev.beliefagent.adapter.llm.langchain4j

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import dev.beliefagent.application.belief.aktualisieren.ports.LlmPort
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.Likelihoods
import dev.langchain4j.model.chat.ChatModel

/**
 * LangChain4j-Adapter hinter dem [LlmPort] (ARC-08). Er fragt nur strukturierte
 * Likelihoods ab; Gate, Freigabe und Aktionsausfuehrung bleiben im Core.
 */
class LangChain4jLlmPort(
    private val chat: LangChain4jChatRunner,
    private val promptFactory: LangChain4jLikelihoodPromptFactory = LangChain4jLikelihoodPromptFactory(),
    private val parser: LangChain4jLikelihoodParser = StrictLangChain4jLikelihoodParser(),
) : LlmPort {

    companion object {
        fun fromChatModel(
            chatModel: ChatModel,
            promptFactory: LangChain4jLikelihoodPromptFactory = LangChain4jLikelihoodPromptFactory(),
            parser: LangChain4jLikelihoodParser = StrictLangChain4jLikelihoodParser(),
        ): LangChain4jLlmPort = LangChain4jLlmPort(
            chat = LangChain4jChatRunner { prompt -> chatModel.chat(prompt) },
            promptFactory = promptFactory,
            parser = parser,
        )
    }

    override fun likelihoods(beobachtung: Beobachtung, prior: BeliefState): Likelihoods {
        val request = LlmLikelihoodRequest.from(beobachtung, prior)
        val antwort = chat.chat(promptFactory.prompt(request))
        return parser.parse(antwort).toLikelihoods(prior)
    }
}

fun interface LangChain4jChatRunner {
    fun chat(prompt: String): String
}

class LangChain4jLikelihoodPromptFactory {
    fun prompt(request: LlmLikelihoodRequest): String = buildString {
        appendLine("Schaetze Likelihoods P(Evidenz | Hypothese) fuer belief-agent.")
        appendLine("belief-agent orchestriert; LangChain4j liefert nur strukturierte Einschaetzungen.")
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

data class LlmLikelihoodRequest(
    val quelle: String,
    val zeitstempel: Long,
    val evidenz: String,
    val hypothesen: List<HypotheseSnapshot>,
    val resthypothese: Double,
) {
    companion object {
        fun from(beobachtung: Beobachtung, prior: BeliefState): LlmLikelihoodRequest = LlmLikelihoodRequest(
            quelle = beobachtung.quelle.name,
            zeitstempel = beobachtung.zeitstempel.epochMillis,
            evidenz = beobachtung.evidenz.beschreibung,
            hypothesen = prior.hypothesen.map { HypotheseSnapshot(it.id.wert, it.wahrscheinlichkeit) },
            resthypothese = prior.resthypothese.wahrscheinlichkeit,
        )
    }
}

data class HypotheseSnapshot(
    val id: String,
    val wahrscheinlichkeit: Double,
)

data class LlmLikelihoodResponse(
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

interface LangChain4jLikelihoodParser {
    fun parse(raw: String): LlmLikelihoodResponse
}

class StrictLangChain4jLikelihoodParser : LangChain4jLikelihoodParser {
    private val objectMapper = ObjectMapper(
        JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build(),
    )

    override fun parse(raw: String): LlmLikelihoodResponse {
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
        return LlmLikelihoodResponse(
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
