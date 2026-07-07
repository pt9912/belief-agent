package dev.beliefagent.example.codeagent

import dev.beliefagent.adapter.audit.MemoryAudit
import dev.beliefagent.adapter.konfidenz.MemoryKonfidenzPort
import dev.beliefagent.adapter.llm.FakeLlm
import dev.beliefagent.application.belief.aktualisieren.BeliefAktualisieren
import dev.beliefagent.application.belief.aktualisieren.BeliefAktualisierenBefehl
import dev.beliefagent.application.belief.aktualisieren.ports.BeobachtungsPort
import dev.beliefagent.application.belief.aktualisieren.ports.HypothesenPort
import dev.beliefagent.application.belief.aktualisieren.ports.LlmPort
import dev.beliefagent.application.belief.aktualisieren.ports.UhrPort
import dev.beliefagent.application.belief.aktionsvorschlag.AktionsVorschlagen
import dev.beliefagent.application.belief.aktionsvorschlag.AktionsVorschlagenBefehl
import dev.beliefagent.application.belief.aktionsvorschlag.dto.AktionsVorschlag
import dev.beliefagent.application.belief.aktionsvorschlag.ports.AktionsVorschlagsPort
import dev.beliefagent.application.belief.entscheidungszyklus.Entscheidungszyklus
import dev.beliefagent.application.belief.entscheidungszyklus.KonfidenzgebundenerEntscheidungszyklus
import dev.beliefagent.application.belief.entscheidungszyklus.Zyklusergebnis
import dev.beliefagent.application.belief.beobachtungwaehlen.BeobachtungWaehlen
import dev.beliefagent.application.belief.beobachtungwaehlen.ports.BeobachtungsAuswahlPort
import dev.beliefagent.application.belief.gaten.AktionGaten
import dev.beliefagent.application.belief.gaten.ports.HumanApprovalPort
import dev.beliefagent.application.belief.ports.KonfidenzPort
import dev.beliefagent.application.ports.AuditPort
import dev.beliefagent.domain.belief.Aktion
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.EvidenzReferenz
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.HypothesenKandidat
import dev.beliefagent.domain.belief.KandidatenScore
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Resthypothese
import dev.beliefagent.domain.belief.Zeitstempel
import dev.beliefagent.domain.eskalation.Budget
import dev.beliefagent.domain.eskalation.Eskalation
import dev.beliefagent.domain.voi.VoiKandidat
import java.util.Locale

fun main() {
    println("belief-agent code-agent example")
    println("compose=CodeAgentController")

    val approvalFreigegeben = envBool("CODE_AGENT_APPROVAL_APPROVED", default = false)
    val prior = BeliefState.of(
        listOf(
            Hypothese(HypotheseId("regression"), 0.9),
            Hypothese(HypotheseId("flaky"), 0.0),
        ),
        Resthypothese(0.1),
    )

    val evidence = seedEvidence()
    val uhr = MonotoneDemoClock(start = 1L)

    val beobachtungsPort = StaticBeobachtungsPort(evidence.map { it.beobachtung })
    val voePort = StaticBeobachtungsAuswahlPort(
        candidates = listOf(
            VoiKandidat(
                observationsForVoi(evidence).first(),
                erwarteteDiskriminierung = 0.8,
                kosten = 1.0,
            ),
        ),
    )
    val llm: LlmPort = FakeLlm()
    val beliefAktualisieren = BeliefAktualisieren(
        llm = llm,
        uhr = uhr,
        hypothesen = StaticHypothesenPort(),
    )

    val konfidenzPort: KonfidenzPort = MemoryKonfidenzPort.leer()
    val audit: MemoryAudit = MemoryAudit()
    val actionPort: AktionsVorschlagsPort = StaticAktionsVorschlagsPort(seedActionSuggestions())
    val aktionsVorschlagen = AktionsVorschlagen(actionPort, konfidenzPort, audit)

    val entscheideZyklus = Entscheidungszyklus(
        BeobachtungWaehlen(voePort),
        beliefAktualisieren,
        AktionGaten(StaticApproval(approvalFreigegeben)),
    )
    val konfidenzEntscheidungsZyklus = KonfidenzgebundenerEntscheidungszyklus(entscheideZyklus, konfidenzPort)
    val controller = CodeAgentController(
        beliefAktualisieren = beliefAktualisieren,
        beobachtungsPort = beobachtungsPort,
        aktionsvorschlaege = aktionsVorschlagen,
        konfidenzEntscheidungsZyklus = konfidenzEntscheidungsZyklus,
        evidenzIndex = evidence.associate { it.referenz to it.beobachtung },
        budget = Budget(maxSchritte = 3),
        uhr = uhr,
        audit = audit,
        execute = { aktion -> println("execute=${aktion.beschreibung}") },
        escalate = { eskalation -> println("escalate reason=${eskalationsGrund(eskalation)}") },
    )

    val result = controller.step(prior)

    println("scenario=code-agent")
    printResult(result, approvalFreigegeben)
    println("audit_events=${audit.lade().groesse}")
}

private class CodeAgentController(
    private val beliefAktualisieren: BeliefAktualisieren,
    private val beobachtungsPort: BeobachtungsPort,
    private val aktionsvorschlaege: AktionsVorschlagen,
    private val konfidenzEntscheidungsZyklus: KonfidenzgebundenerEntscheidungszyklus,
    private val evidenzIndex: Map<EvidenzReferenz, Beobachtung>,
    private val budget: Budget,
    private val uhr: UhrPort,
    private val audit: AuditPort,
    private val execute: (Aktion) -> Unit,
    private val escalate: (Eskalation) -> Unit,
) {
    fun step(prior: BeliefState): Zyklusergebnis {
        val beobachtungen = beobachtungsPort.lies()
        val updated = beliefAktualisieren.ausfuehren(BeliefAktualisierenBefehl(prior, beobachtungen))
        updated.ereignisse.forEach(audit::anhaengen)

        val vorschlaege = aktionsvorschlaege.ausfuehren(
            AktionsVorschlagenBefehl(
                belief = updated.belief,
                bekannteEvidenz = evidenzIndex,
                zeitstempel = uhr.jetzt(),
            ),
        )
        val vorschlag = vorschlaege.firstOrNull() ?: return Zyklusergebnis.Abgelehnt(
            "kein gate-faehiger Vorschlag",
            updated.belief,
        )

        return when (val result = konfidenzEntscheidungsZyklus.entscheide(vorschlag.aktion, updated.belief, budget)) {
            is Zyklusergebnis.Gehandelt -> {
                execute(result.freigabe.aktion)
                result
            }
            is Zyklusergebnis.Eskaliert -> {
                escalate(result.eskalation)
                result
            }
            is Zyklusergebnis.Abgelehnt -> result
        }
    }
}

private data class EvidenceSeed(
    val referenz: EvidenzReferenz,
    val beobachtung: Beobachtung,
)

private fun seedEvidence() = listOf(
    EvidenceSeed(
        EvidenzReferenz("code-agent:evidence:build"),
        Beobachtung(
            Quelle.BUILD,
            Zeitstempel(10L),
            Evidenz("build test output: regression suspicion"),
        ),
    ),
    EvidenceSeed(
        EvidenzReferenz("code-agent:evidence:repo"),
        Beobachtung(
            Quelle.REPO,
            Zeitstempel(11L),
            Evidenz("commit touches regression-prone code"),
        ),
    ),
)

private fun seedActionSuggestions() = listOf(
    StaticActionSuggestionConfig(
        beschreibung = "Release-Branch stoppen",
        hypotheseId = "regression",
        wirkungsklasse = "EXTERN_WIRKSAM",
        pSuccess = 0.95,
        konfidenzReferenz = "code-agent:konfidenz:rollback-check",
        stuetzendeEvidenz = listOf("code-agent:evidence:build", "code-agent:evidence:repo"),
    ),
)

private fun observationsForVoi(evidence: List<EvidenceSeed>): List<Beobachtung> =
    evidence.map { it.beobachtung }.distinctBy { it.hashCode() }

private data class StaticActionSuggestionConfig(
    val beschreibung: String,
    val hypotheseId: String,
    val wirkungsklasse: String,
    val pSuccess: Double,
    val konfidenzReferenz: String,
    val stuetzendeEvidenz: List<String>,
) {
    init {
        require(beschreibung.isNotBlank()) { "beschreibung darf nicht leer sein" }
        require(hypotheseId.isNotBlank()) { "hypotheseId darf nicht leer sein" }
        require(wirkungsklasse.isNotBlank()) { "wirkungsklasse darf nicht leer sein" }
        require(pSuccess in 0.0..1.0) { "pSuccess muss in [0,1] liegen: $pSuccess" }
    }
}

private class StaticAktionsVorschlagsPort(
    private val configs: List<StaticActionSuggestionConfig>,
) : AktionsVorschlagsPort {
    override fun vorschlaege(belief: BeliefState): List<AktionsVorschlag> =
        configs.filter { config -> belief.hypothesen.any { it.id.wert == config.hypotheseId } }.map {
            AktionsVorschlag(
                beschreibung = it.beschreibung,
                hypotheseId = it.hypotheseId,
                wirkungsklasse = it.wirkungsklasse,
                pSuccess = it.pSuccess,
                konfidenzReferenz = it.konfidenzReferenz,
                stuetzendeEvidenz = it.stuetzendeEvidenz,
            )
        }
}

private class StaticBeobachtungsPort(
    private val beobachtungen: List<Beobachtung>,
) : BeobachtungsPort {
    override fun lies(): List<Beobachtung> = beobachtungen.toList()
}

private class StaticBeobachtungsAuswahlPort(
    private val candidates: List<VoiKandidat>,
) : BeobachtungsAuswahlPort {
    override fun kandidaten(belief: BeliefState): List<VoiKandidat> = candidates
}

private class StaticHypothesenPort : HypothesenPort {
    private val kandidaten = listOf(
        HypothesenKandidat(
            id = HypotheseId("regression"),
            score = KandidatenScore(0.25),
            stuetzendeEvidenz = listOf(EvidenzReferenz("code-agent:evidence:repo")),
        ),
    )

    override fun kandidaten(belief: BeliefState): List<HypothesenKandidat> = if (belief.resthypothese.wahrscheinlichkeit > 0.2) {
        kandidaten
    } else {
        emptyList()
    }
}

private class StaticApproval(private val freigabe: Boolean) : HumanApprovalPort {
    override fun freigegeben(aktion: Aktion): Boolean = freigabe
}

private class MonotoneDemoClock(start: Long = 1L) : UhrPort {
    private var now = start
    override fun jetzt(): Zeitstempel = Zeitstempel(now++)
}

private fun printResult(result: Zyklusergebnis, approvalFreigegeben: Boolean) {
    when (result) {
        is Zyklusergebnis.Gehandelt -> {
            println("terminal=gehandelt")
            println("executed=true")
            println("executor_boundary=Zyklusergebnis.Gehandelt.freigabe.aktion")
            println("reason=gate_freigegeben")
            println("approval=$approvalFreigegeben")
            println("resthypothese=${format(result.belief.resthypothese.wahrscheinlichkeit)}")
        }
        is Zyklusergebnis.Eskaliert -> {
            println("terminal=eskaliert")
            println("executed=false")
            println("executor_boundary=closed")
            println("reason=GateEskalation")
            println("resthypothese=${format(result.eskalation.belief.resthypothese.wahrscheinlichkeit)}")
        }
        is Zyklusergebnis.Abgelehnt -> {
            println("terminal=abgelehnt")
            println("executed=false")
            println("executor_boundary=closed")
            println("reason=${result.grund}")
            println("resthypothese=${format(result.belief.resthypothese.wahrscheinlichkeit)}")
        }
    }
}

private fun eskalationsGrund(eskalation: Eskalation): String = when (val grund = eskalation.grund) {
    is dev.beliefagent.domain.eskalation.Eskalationsgrund.BeobachtungenErschoepft -> "BeobachtungenErschoepft"
    is dev.beliefagent.domain.eskalation.Eskalationsgrund.GateEskalation -> "GateEskalation"
    is dev.beliefagent.domain.eskalation.Eskalationsgrund.BudgetErschoepft -> "BudgetErschoepft"
}

private fun format(value: Double): String = String.format(Locale.ROOT, "%.6f", value)

private fun envBool(name: String, default: Boolean): Boolean {
    val value = System.getenv(name)?.trim()
    if (value.isNullOrEmpty()) {
        return default
    }
    return value.equals("true", ignoreCase = true)
}
