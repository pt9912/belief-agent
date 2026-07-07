package dev.beliefagent.example.langchain

import dev.beliefagent.adapter.llm.langchain4j.LangChain4jChatRunner
import dev.beliefagent.adapter.llm.langchain4j.LangChain4jLlmPort
import dev.beliefagent.application.belief.aktualisieren.BeliefAktualisieren
import dev.beliefagent.application.belief.aktualisieren.ports.UhrPort
import dev.beliefagent.application.belief.beobachtungwaehlen.BeobachtungWaehlen
import dev.beliefagent.application.belief.beobachtungwaehlen.ports.BeobachtungsAuswahlPort
import dev.beliefagent.application.belief.entscheidungszyklus.Entscheidungszyklus
import dev.beliefagent.application.belief.entscheidungszyklus.Zyklusergebnis
import dev.beliefagent.application.belief.gaten.AktionGaten
import dev.beliefagent.application.belief.gaten.ports.HumanApprovalPort
import dev.beliefagent.domain.belief.Aktion
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Erfolgswahrscheinlichkeit
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Resthypothese
import dev.beliefagent.domain.belief.Wirkungsklasse
import dev.beliefagent.domain.belief.Zeitstempel
import dev.beliefagent.domain.eskalation.Budget
import dev.beliefagent.domain.voi.VoiKandidat

fun main() {
    println("belief-agent LangChain4j adapter example")
    val mode = env("BELIEF_AGENT_LANGCHAIN4J_MODE") ?: "mock"
    println("belief-agent orchestrates; LangChain4j returns structured estimates only.")
    println("belief-agent LLM mode=$mode")
    println("production_composition_root=adapters:inbound:cli")
    println("example_scope=llm_port_boundary_only")

    val prior = BeliefState.of(
        listOf(
            Hypothese(HypotheseId("regression"), 0.4),
            Hypothese(HypotheseId("flaky"), 0.4),
        ),
        Resthypothese(0.2),
    )

    val supportingEvidence = Beobachtung(
        Quelle.REPO,
        Zeitstempel(1L),
        Evidenz("regression possible after dependency update"),
    )
    val nextObservation = Beobachtung(
        Quelle.LOG,
        Zeitstempel(2L),
        Evidenz("regression confirmed by checkout error log"),
    )

    val action = Aktion(
        beschreibung = "Trigger production deploy",
        wirkungsklasse = Wirkungsklasse.EXTERN_WIRKSAM,
        erfolgswahrscheinlichkeit = Erfolgswahrscheinlichkeit(0.96),
        stuetzendeEvidenz = listOf(supportingEvidence),
    )

    val langChainChatPort = when (mode.lowercase()) {
        "mock" -> LangChain4jLlmPort(langChainMockRunner())
        "real" -> buildRealLangChain4jLlmPort()
        else -> {
            println("unsupported mode '$mode', falling back to mock")
            LangChain4jLlmPort(langChainMockRunner())
        }
    }
    val cycle = Entscheidungszyklus(
        beobachtungWaehlen = BeobachtungWaehlen(BeliefAwareCandidatePort(nextObservation)),
        beliefAktualisieren = BeliefAktualisieren(langChainChatPort, MonotoneDemoClock()),
        aktionGaten = AktionGaten(AllowingHumanApproval),
    )

    when (val result = cycle.entscheide(action, prior, Budget(maxSchritte = 3))) {
        is Zyklusergebnis.Gehandelt -> {
            println("result=GEHANDELT")
            println("executor_allowed=true")
            println("executor_boundary=Zyklusergebnis.Gehandelt.freigabe.aktion")
            println("approved_action=${result.freigabe.aktion.beschreibung}")
            println("posterior=${formatBelief(result.belief)}")
        }
        is Zyklusergebnis.Eskaliert -> {
            println("result=ESKALIERT")
            println("executor_allowed=false")
            println("executor_boundary=closed")
            println("reason=${result.eskalation.grund}")
            println("belief=${formatBelief(result.eskalation.belief)}")
        }
        is Zyklusergebnis.Abgelehnt -> {
            println("result=ABGELEHNT")
            println("executor_allowed=false")
            println("executor_boundary=closed")
            println("reason=${result.grund}")
            println("belief=${formatBelief(result.belief)}")
        }
    }
}

private class BeliefAwareCandidatePort(
    beobachtung: Beobachtung,
) : BeobachtungsAuswahlPort {
    private val candidates = listOf(
        VoiKandidat(
            beobachtung = beobachtung,
            erwarteteDiskriminierung = 0.7,
            kosten = 1.0,
        ),
    )

    override fun kandidaten(belief: BeliefState): List<VoiKandidat> =
        if (belief.hypothesen.any { it.id.wert == "regression" }) candidates else emptyList()
}

private object AllowingHumanApproval : HumanApprovalPort {
    override fun freigegeben(aktion: Aktion): Boolean = true
}

private class MonotoneDemoClock : UhrPort {
    private var next = 2L

    override fun jetzt(): Zeitstempel = Zeitstempel(next++)
}

private fun buildRealLangChain4jLlmPort(): LangChain4jLlmPort {
    val provider = env("BELIEF_AGENT_LANGCHAIN4J_PROVIDER") ?: "openai"
    return when (provider.lowercase()) {
        "openai" -> buildOpenAiLangChain4jPort()
        else -> throw IllegalArgumentException(
            "Unsupported LangChain4j provider='$provider'. " +
                "Use BELIEF_AGENT_LANGCHAIN4J_PROVIDER=openai or set BELIEF_AGENT_LANGCHAIN4J_MODE=mock.",
        )
    }
}

private fun buildOpenAiLangChain4jPort(): LangChain4jLlmPort {
    val apiKey = envRequired("OPENAI_API_KEY")
    val model = env("OPENAI_MODEL") ?: "gpt-4o-mini"
    val baseUrl = env("OPENAI_BASE_URL")

    val chatModelClass = runCatching {
        Class.forName("dev.langchain4j.model.openai.OpenAiChatModel")
    }.getOrElse { cause ->
        throw IllegalStateException(
            "OpenAI ChatModel dependency not on classpath. " +
                "Add dev.langchain4j:langchain4j-open-ai to this example module " +
                "or keep BELIEF_AGENT_LANGCHAIN4J_MODE=mock.",
            cause,
        )
    }

    val builder = chatModelClass.getMethod("builder").invoke(null)
    val withApiKey = invokeBuilderMethodIfAvailable(builder, "apiKey", apiKey)
    val withModel = invokeBuilderMethodIfAvailable(withApiKey, "modelName", model)
    val withBaseUrl = baseUrl?.let { invokeBuilderMethodIfAvailable(withModel, "baseUrl", it) } ?: withModel

    val chatModel = withBaseUrl.javaClass.getMethod("build").invoke(withBaseUrl)

    return LangChain4jLlmPort(
        LangChain4jChatRunner { prompt ->
            runCatching {
                chatModel.javaClass.getMethod("chat", String::class.java).invoke(chatModel, prompt)
            }.getOrElse { cause ->
                throw IllegalStateException(
                    "LangChain4j ChatModel does not expose a usable chat(String) method in runtime. ",
                    cause,
                )
            }.let { result ->
                result?.toString() ?: throw IllegalStateException(
                    "LangChain4j ChatModel returned null for prompt.",
                )
            }
        },
    )
}

private fun invokeBuilderMethodIfAvailable(builder: Any, method: String, value: String): Any {
    return runCatching { builder.javaClass.getMethod(method, String::class.java).invoke(builder, value) }
        .getOrElse { e ->
            throw IllegalArgumentException(
                "Could not configure LangChain4j OpenAI builder using method '$method'. " +
                    "Keep BELIEF_AGENT_LANGCHAIN4J_MODE=mock for offline execution.",
                e,
            )
        }
}

private fun langChainMockRunner(): LangChain4jChatRunner = LangChain4jChatRunner {
    val response = """{"proHypothese":{"regression":0.85,"flaky":0.15},"resthypothese":0.05}"""
    println("langchain4j_chat_response=$response")
    response
}

private fun formatBelief(belief: BeliefState): String {
    val hypotheses = belief.hypothesen.joinToString(prefix = "[", postfix = "]") {
        "${it.id.wert}=${"%.6f".format(it.wahrscheinlichkeit)}"
    }
    return "$hypotheses, rest=${"%.6f".format(belief.resthypothese.wahrscheinlichkeit)}"
}

private fun env(name: String): String? = System.getenv(name)?.trim()?.takeIf { it.isNotBlank() }

private fun envRequired(name: String): String =
    env(name) ?: throw IllegalArgumentException(
        "Missing environment variable '$name' for real mode. " +
            "Set BELIEF_AGENT_LANGCHAIN4J_MODE=mock if you want the offline example.",
    )
