package dev.beliefagent.example.koog

import dev.beliefagent.adapter.llm.koog.KoogLlmPort
import dev.beliefagent.adapter.llm.koog.KoogPromptRunner
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
    println("belief-agent Koog adapter example")
    val mode = env("KOOG_EXAMPLE_MODE") ?: "mock"
    println("belief-agent orchestrates; Koog returns structured estimates only.")
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

    val koogLlmPort = when (mode.lowercase()) {
        "mock" -> KoogLlmPort(koogMockRunner())
        "real" -> buildRealKoogLlmPort()
        else -> {
            println("unsupported mode '$mode', falling back to mock")
            KoogLlmPort(koogMockRunner())
        }
    }
    val cycle = Entscheidungszyklus(
        beobachtungWaehlen = BeobachtungWaehlen(BeliefAwareCandidatePort(nextObservation)),
        beliefAktualisieren = BeliefAktualisieren(koogLlmPort, MonotoneDemoClock()),
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

private fun buildRealKoogLlmPort(): KoogLlmPort {
    val clientClass = envRequired("KOOG_CLIENT_CLASS")
    val providerId = env("KOOG_PROVIDER_ID") ?: "llm-provider"
    val providerName = env("KOOG_PROVIDER_NAME") ?: "LLM"
    val modelId = env("KOOG_MODEL_ID") ?: "default-model"

    return KoogLlmPort.fromLlmClient(
        clientClass = clientClass,
        providerId = providerId,
        providerName = providerName,
        modelId = modelId,
    )
}

private fun koogMockRunner(): KoogPromptRunner = KoogPromptRunner {
    val response = """{"proHypothese":{"regression":0.85,"flaky":0.15},"resthypothese":0.05}"""
    println("koog_prompt_response=$response")
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
            "Set KOOG_EXAMPLE_MODE=mock if you want the offline example.",
    )
