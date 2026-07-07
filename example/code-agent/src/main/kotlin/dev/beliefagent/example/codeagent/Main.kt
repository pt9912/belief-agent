package dev.beliefagent.example.codeagent

import dev.beliefagent.adapter.audit.MemoryAudit
import dev.beliefagent.adapter.konfidenz.MemoryKonfidenzPort
import dev.beliefagent.adapter.llm.FakeLlm
import dev.beliefagent.adapter.observation.buildreport.BuildReportBeobachter
import dev.beliefagent.adapter.observation.buildreport.BuildReportDateiQuelle
import dev.beliefagent.adapter.observation.gitlocal.GitSourceConfig
import dev.beliefagent.adapter.observation.gitlocal.GitStatusBeobachter
import dev.beliefagent.adapter.observation.gitlocal.GitStatusQuellenFactory
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
import dev.beliefagent.application.belief.beobachtungwaehlen.BeobachtungWaehlen
import dev.beliefagent.application.belief.beobachtungwaehlen.ports.BeobachtungsAuswahlPort
import dev.beliefagent.application.belief.entscheidungszyklus.Entscheidungszyklus
import dev.beliefagent.application.belief.entscheidungszyklus.KonfidenzgebundenerEntscheidungszyklus
import dev.beliefagent.application.belief.entscheidungszyklus.Zyklusergebnis
import dev.beliefagent.application.belief.gaten.AktionGaten
import dev.beliefagent.application.belief.gaten.ports.HumanApprovalPort
import dev.beliefagent.application.belief.ports.KonfidenzPort
import dev.beliefagent.application.ports.AuditPort
import dev.beliefagent.domain.belief.Aktion
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Beobachtung
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
import java.io.IOException
import java.nio.charset.MalformedInputException
import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.Locale
import kotlin.system.exitProcess

private const val BUILD_FIXTURE_ENV = "CODE_AGENT_BUILD_FIXTURE"
private const val REPO_FIXTURE_ENV = "CODE_AGENT_REPO_FIXTURE"
private const val EX_DATAERR = 65

fun main() {
    val exitCode = runCodeAgent(System.getenv(), ::println)
    if (exitCode != 0) {
        exitProcess(exitCode)
    }
}

fun runCodeAgent(env: Map<String, String?>, print: (String) -> Unit): Int = try {
    runCodeAgentUnsafe(env, print)
    0
} catch (failure: FixtureInputFailure) {
    printFixtureFailure(failure, print)
    EX_DATAERR
}

private fun runCodeAgentUnsafe(env: Map<String, String?>, print: (String) -> Unit) {
    print("belief-agent code-agent example")
    print("compose=CodeAgentController")

    val approvalFreigegeben = envBool(env, "CODE_AGENT_APPROVAL_APPROVED", default = false)
    val prior = BeliefState.of(
        listOf(
            Hypothese(HypotheseId("regression"), 0.9),
            Hypothese(HypotheseId("flaky"), 0.0),
        ),
        Resthypothese(0.1),
    )

    val observations = codeAgentObservations(env)
    val uhr = MonotoneDemoClock(start = 1L)

    val beobachtungsPort = StaticBeobachtungsPort(observations)
    val voePort = StaticBeobachtungsAuswahlPort(
        candidates = listOf(
            VoiKandidat(
                observationsForVoi(observations).first(),
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
        budget = Budget(maxSchritte = 3),
        uhr = uhr,
        audit = audit,
        execute = { aktion -> print("execute=${aktion.beschreibung}") },
        escalate = { eskalation -> print("escalate reason=${eskalationsGrund(eskalation)}") },
    )

    val result = controller.step(prior)

    print("scenario=code-agent")
    printObservations(observations, print)
    printResult(result, approvalFreigegeben, print)
    print("audit_events=${audit.lade().groesse}")
}

private fun codeAgentObservations(env: Map<String, String?>): List<Beobachtung> {
    val beobachtungsZeit = MonotoneDemoClock(start = 10L)
    val build = BuildReportBeobachter(
        quelle = BuildReportDateiQuelle(requiredEnvPath(env, BUILD_FIXTURE_ENV)),
        zeitstempel = beobachtungsZeit::jetzt,
    )
    val repo = GitStatusBeobachter(
        quelle = GitStatusQuellenFactory.create(
            GitSourceConfig(
                source = "fixture",
                fixturePath = requiredEnvPath(env, REPO_FIXTURE_ENV),
            ),
        ),
        zeitstempel = beobachtungsZeit::jetzt,
    )
    return try {
        build.lies() + repo.lies()
    } catch (error: Throwable) {
        throw classifyFixtureFailure(error) ?: error
    }
}

private class CodeAgentController(
    private val beliefAktualisieren: BeliefAktualisieren,
    private val beobachtungsPort: BeobachtungsPort,
    private val aktionsvorschlaege: AktionsVorschlagen,
    private val konfidenzEntscheidungsZyklus: KonfidenzgebundenerEntscheidungszyklus,
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
                bekannteEvidenz = evidenceIndex(beobachtungen),
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

private fun observationsForVoi(observations: List<Beobachtung>): List<Beobachtung> =
    observations.distinctBy { it.quelle to it.evidenz.beschreibung }

private fun evidenceIndex(observations: List<Beobachtung>): Map<EvidenzReferenz, Beobachtung> =
    observations.associateBy { evidenceReferenceFor(it.quelle) }

private fun evidenceReferenceFor(quelle: Quelle): EvidenzReferenz = when (quelle) {
    Quelle.BUILD -> EvidenzReferenz("code-agent:evidence:build")
    Quelle.REPO -> EvidenzReferenz("code-agent:evidence:repo")
    else -> EvidenzReferenz("code-agent:evidence:${quelle.name.lowercase(Locale.ROOT)}")
}

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

private fun printObservations(observations: List<Beobachtung>, print: (String) -> Unit) {
    observations.forEach { beobachtung ->
        print(
            "observation source=${beobachtung.quelle.name}; timestamp=${beobachtung.zeitstempel.epochMillis}; " +
                "evidence=${beobachtung.evidenz.beschreibung}",
        )
    }
}

private fun printResult(result: Zyklusergebnis, approvalFreigegeben: Boolean, print: (String) -> Unit) {
    when (result) {
        is Zyklusergebnis.Gehandelt -> {
            print("terminal=gehandelt")
            print("executed=true")
            print("executor_boundary=Zyklusergebnis.Gehandelt.freigabe.aktion")
            print("reason=gate_freigegeben")
            print("approval=$approvalFreigegeben")
            print("resthypothese=${format(result.belief.resthypothese.wahrscheinlichkeit)}")
        }
        is Zyklusergebnis.Eskaliert -> {
            print("terminal=eskaliert")
            print("executed=false")
            print("executor_boundary=closed")
            print("reason=GateEskalation")
            print("resthypothese=${format(result.eskalation.belief.resthypothese.wahrscheinlichkeit)}")
        }
        is Zyklusergebnis.Abgelehnt -> {
            print("terminal=abgelehnt")
            print("executed=false")
            print("executor_boundary=closed")
            print("reason=${result.grund}")
            print("resthypothese=${format(result.belief.resthypothese.wahrscheinlichkeit)}")
        }
    }
}

private fun eskalationsGrund(eskalation: Eskalation): String = when (val grund = eskalation.grund) {
    is dev.beliefagent.domain.eskalation.Eskalationsgrund.BeobachtungenErschoepft -> "BeobachtungenErschoepft"
    is dev.beliefagent.domain.eskalation.Eskalationsgrund.GateEskalation -> "GateEskalation"
    is dev.beliefagent.domain.eskalation.Eskalationsgrund.BudgetErschoepft -> "BudgetErschoepft"
}

private class FixtureInputFailure(
    val fehlerklasse: String,
    detail: String,
    cause: Throwable? = null,
) : RuntimeException(detail, cause)

private fun requiredEnvPath(env: Map<String, String?>, name: String): Path {
    val value = env[name]?.trim()
    if (value.isNullOrEmpty()) {
        throw FixtureInputFailure("fixture_env_missing", "$name fehlt oder ist leer")
    }
    val resolved = resolveFixturePath(value)
    when {
        !Files.exists(resolved) -> throw FixtureInputFailure("fixture_missing", "$name zeigt auf fehlende Datei: $resolved")
        !Files.isRegularFile(resolved) || !Files.isReadable(resolved) -> throw FixtureInputFailure(
            "fixture_unreadable",
            "$name ist keine lesbare regulaere Datei: $resolved",
        )
        Files.size(resolved) == 0L -> throw FixtureInputFailure("fixture_empty", "$name ist leer: $resolved")
    }
    return resolved
}

private fun resolveFixturePath(value: String): Path {
    val configured = Path.of(value)
    if (configured.isAbsolute) {
        return configured
    }
    val fromWorkingDirectory = Path.of("").toAbsolutePath().resolve(configured).normalize()
    if (Files.exists(fromWorkingDirectory)) {
        return fromWorkingDirectory
    }
    val repoRoot = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
        .firstOrNull { Files.exists(it.resolve("settings.gradle.kts")) }
        ?: Path.of("").toAbsolutePath()
    return repoRoot.resolve(configured).normalize()
}

private fun classifyFixtureFailure(error: Throwable): FixtureInputFailure? {
    val cause = rootCause(error)
    val message = cause.message.orEmpty()
    return when (cause) {
        is MalformedInputException -> FixtureInputFailure("fixture_encoding_invalid", "Fixture ist nicht gueltig UTF-8", cause)
        is NoSuchFileException -> FixtureInputFailure("fixture_missing", "Fixture-Datei fehlt: ${cause.file}", cause)
        is AccessDeniedException -> FixtureInputFailure("fixture_unreadable", "Fixture-Datei ist nicht lesbar: ${cause.file}", cause)
        is IOException -> FixtureInputFailure("fixture_unreadable", "Fixture-Datei konnte nicht gelesen werden: $message", cause)
        is IllegalArgumentException -> {
            val fehlerklasse = if (message.contains("key=value")) {
                "fixture_malformed_json"
            } else {
                "fixture_schema_mismatch"
            }
            FixtureInputFailure(fehlerklasse, message.ifBlank { cause::class.simpleName.orEmpty() }, cause)
        }
        is IllegalStateException -> FixtureInputFailure(
            "fixture_schema_mismatch",
            message.ifBlank { cause::class.simpleName.orEmpty() },
            cause,
        )
        else -> null
    }
}

private fun rootCause(error: Throwable): Throwable = error.cause?.let(::rootCause) ?: error

private fun printFixtureFailure(failure: FixtureInputFailure, print: (String) -> Unit) {
    print("scenario=code-agent")
    print("terminal=eskaliert")
    print("executed=false")
    print("executor_boundary=closed")
    print("reason=${failure.fehlerklasse}: ${failure.message}")
}

private fun format(value: Double): String = String.format(Locale.ROOT, "%.6f", value)

private fun envBool(env: Map<String, String?>, name: String, default: Boolean): Boolean {
    val value = env[name]?.trim()
    if (value.isNullOrEmpty()) {
        return default
    }
    return value.equals("true", ignoreCase = true)
}
