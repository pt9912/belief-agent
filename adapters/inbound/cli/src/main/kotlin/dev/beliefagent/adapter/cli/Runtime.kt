package dev.beliefagent.adapter.cli

import dev.beliefagent.adapter.approval.FakeApproval
import dev.beliefagent.adapter.approvallocal.ApprovalAntwort
import dev.beliefagent.adapter.approvallocal.ApprovalAusgabe
import dev.beliefagent.adapter.approvallocal.ApprovalChallenge
import dev.beliefagent.adapter.approvallocal.ApprovalEingabe
import dev.beliefagent.adapter.approvallocal.ApprovalNonce
import dev.beliefagent.adapter.approvallocal.ApprovalNonceQuelle
import dev.beliefagent.adapter.approvallocal.InMemoryApprovalNonceStore
import dev.beliefagent.adapter.approvallocal.LocalApproval
import dev.beliefagent.adapter.audit.MemoryAudit
import dev.beliefagent.adapter.konfidenz.MemoryKonfidenzPort
import dev.beliefagent.adapter.llm.FakeLlm
import dev.beliefagent.adapter.llmaction.FakeAktionsVorschlagKonfiguration
import dev.beliefagent.adapter.llmaction.FakeAktionsVorschlagsPort
import dev.beliefagent.adapter.llmhypothesen.FakeHypothesenPort
import dev.beliefagent.adapter.observation.FakeBeobachtungsQuelle
import dev.beliefagent.adapter.voi.FakeKandidatenquelle
import dev.beliefagent.application.belief.aktionsvorschlag.AktionsVorschlagen
import dev.beliefagent.application.belief.aktionsvorschlag.AktionsVorschlagenBefehl
import dev.beliefagent.application.belief.aktionsvorschlag.ports.AktionsVorschlagsPort
import dev.beliefagent.application.belief.aktualisieren.BeliefAktualisieren
import dev.beliefagent.application.belief.aktualisieren.ports.BeobachtungsPort
import dev.beliefagent.application.belief.aktualisieren.ports.HypothesenPort
import dev.beliefagent.application.belief.aktualisieren.ports.LlmPort
import dev.beliefagent.application.belief.aktualisieren.ports.UhrPort
import dev.beliefagent.application.belief.beobachtungwaehlen.BeobachtungWaehlen
import dev.beliefagent.application.belief.beobachtungwaehlen.ports.BeobachtungsAuswahlPort
import dev.beliefagent.application.belief.entscheidungszyklus.Entscheidungszyklus
import dev.beliefagent.application.belief.entscheidungszyklus.KonfidenzgebundenerEntscheidungszyklus
import dev.beliefagent.application.belief.entscheidungszyklus.Zyklusergebnis
import dev.beliefagent.application.belief.gaten.AktionGaten
import dev.beliefagent.application.belief.gaten.ports.ApprovalAnfrage
import dev.beliefagent.application.belief.gaten.ports.HumanApprovalPort
import dev.beliefagent.application.belief.ports.KonfidenzPort
import dev.beliefagent.application.ports.AuditPort
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.EvidenzReferenz
import dev.beliefagent.domain.belief.Zeitstempel
import dev.beliefagent.domain.eskalation.Budget
import dev.beliefagent.domain.eskalation.Eskalationsgrund
import dev.beliefagent.domain.voi.VoiKandidat
import java.util.Locale
import java.util.UUID
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module

enum class CliTerminal {
    GEHANDELT,
    ESKALIERT,
    ABGELEHNT,
}

data class CliRuntimeKonfiguration(
    val prior: BeliefState,
    val budget: Budget = Budget(),
    val aktionsVorschlaege: List<FakeAktionsVorschlagKonfiguration>,
    val bekannteEvidenz: Map<EvidenzReferenz, Beobachtung>,
    val voiKandidaten: List<VoiKandidat> = emptyList(),
    val approval: CliApprovalKonfiguration = CliApprovalKonfiguration.Fake(false),
    val startZeit: Long = 1L,
    val szenario: String = "custom",
) {
    fun mitApproval(approval: CliApprovalKonfiguration): CliRuntimeKonfiguration =
        copy(approval = approval)
}

sealed interface CliApprovalKonfiguration {
    val name: String

    data class Fake(val freigegeben: Boolean) : CliApprovalKonfiguration {
        override val name: String = "fake"
    }

    data class Kanalwahl(
        val kanal: CliApprovalKanalName,
        val kanaele: Map<CliApprovalKanalName, HumanApprovalPort>,
    ) : CliApprovalKonfiguration {
        override val name: String = kanal.anzeigeName()

        companion object {
            fun local(
                nonceQuelle: ApprovalNonceQuelle = JvmApprovalNonceQuelle(),
                eingabe: ApprovalEingabe = ConsoleApprovalEingabe(),
                ausgabe: ApprovalAusgabe = ConsoleApprovalAusgabe(),
                nonceStore: InMemoryApprovalNonceStore = InMemoryApprovalNonceStore(),
            ): Kanalwahl = Kanalwahl(
                kanal = CliApprovalKanalName.LOCAL,
                kanaele = mapOf(
                    CliApprovalKanalName.LOCAL to LocalApproval(
                        nonceQuelle = nonceQuelle,
                        eingabe = eingabe,
                        ausgabe = ausgabe,
                        nonceStore = nonceStore,
                    ),
                ),
            )

            fun auswahl(kanal: String): Kanalwahl =
                Kanalwahl(kanal = CliApprovalKanalName(kanal), kanaele = local().kanaele)
        }
    }
}

data class CliApprovalKanalName(val wert: String) {
    fun anzeigeName(): String = wert.ifBlank { "leer" }

    companion object {
        val LOCAL = CliApprovalKanalName("local")
    }
}

class CliApprovalKanalDispatcher(
    private val kanal: CliApprovalKanalName,
    private val kanaele: Map<CliApprovalKanalName, HumanApprovalPort>,
) : HumanApprovalPort {
    override fun freigegeben(anfrage: ApprovalAnfrage): Boolean {
        val ausgewaehlt = kanaele[kanal] ?: return false
        return try {
            ausgewaehlt.freigegeben(anfrage)
        } catch (_: Exception) {
            false
        }
    }
}

data class CliLaufErgebnis(
    val terminal: CliTerminal,
    val zyklus: Zyklusergebnis,
    val executor: ExecutorErgebnis,
    val sichtbareAusgabe: String,
)

class CliRuntime private constructor(
    private val app: KoinApplication,
) {
    private val koin: Koin = app.koin

    fun starte(): CliLaufErgebnis {
        val config = koin.get<CliRuntimeKonfiguration>()
        val vorschlag = koin.get<AktionsVorschlagen>().ausfuehren(
            AktionsVorschlagenBefehl(
                belief = config.prior,
                bekannteEvidenz = config.bekannteEvidenz,
                zeitstempel = koin.get<UhrPort>().jetzt(),
            ),
        ).firstOrNull()

        val zyklusErgebnis = if (vorschlag == null) {
            Zyklusergebnis.Abgelehnt("kein gate-faehiger Aktionsvorschlag", config.prior)
        } else {
            koin.get<KonfidenzgebundenerEntscheidungszyklus>().entscheide(
                aktion = vorschlag.aktion,
                prior = config.prior,
                budget = config.budget,
            )
        }
        val executorErgebnis = koin.get<CliExecutor>().verarbeite(zyklusErgebnis)
        return CliLaufErgebnis(
            terminal = executorErgebnis.terminal,
            zyklus = zyklusErgebnis,
            executor = executorErgebnis,
            sichtbareAusgabe = sichtbareAusgabe(config, zyklusErgebnis, executorErgebnis),
        )
    }

    fun ausgefuehrteAktionen() = koin.get<RecordingAktionsAusfuehrungsAdapter>().ausgefuehrteAktionen()

    companion object {
        fun ausKonfiguration(config: CliRuntimeKonfiguration): CliRuntime =
            CliRuntime(koinApplication { modules(cliModule(config)) })
    }
}

private fun sichtbareAusgabe(
    config: CliRuntimeKonfiguration,
    zyklus: Zyklusergebnis,
    executor: ExecutorErgebnis,
): String = buildString {
    appendLine("scenario=${config.szenario}")
    appendLine("approval=${config.approval.name}")
    appendLine("terminal=${executor.terminal.name.lowercase()}")
    appendLine("executed=${executor.ausgefuehrt}")
    when (zyklus) {
        is Zyklusergebnis.Gehandelt -> {
            appendLine("reason=gate_freigegeben")
            appendLine("executor_boundary=Zyklusergebnis.Gehandelt.freigabe.aktion")
            append("resthypothese=${formatWahrscheinlichkeit(zyklus.belief.resthypothese.wahrscheinlichkeit)}")
        }
        is Zyklusergebnis.Eskaliert -> {
            appendLine("reason=${eskalationsgrundName(zyklus.eskalation.grund)}")
            appendLine("executor_boundary=closed")
            append("resthypothese=${formatWahrscheinlichkeit(zyklus.eskalation.belief.resthypothese.wahrscheinlichkeit)}")
        }
        is Zyklusergebnis.Abgelehnt -> {
            appendLine("reason=${zyklus.grund}")
            appendLine("executor_boundary=closed")
            append("resthypothese=${formatWahrscheinlichkeit(zyklus.belief.resthypothese.wahrscheinlichkeit)}")
        }
    }
}

private fun eskalationsgrundName(grund: Eskalationsgrund): String = when (grund) {
    is Eskalationsgrund.BeobachtungenErschoepft -> "BeobachtungenErschoepft"
    is Eskalationsgrund.GateEskalation -> "GateEskalation"
    is Eskalationsgrund.BudgetErschoepft -> "BudgetErschoepft"
}

private fun formatWahrscheinlichkeit(wert: Double): String =
    String.format(Locale.ROOT, "%.6f", wert)

fun cliModule(config: CliRuntimeKonfiguration) = module {
    single { config }
    single<AuditPort> { MemoryAudit() }
    single<KonfidenzPort> { MemoryKonfidenzPort.leer() }
    single<UhrPort> { MonotoneFakeUhr(config.startZeit) }
    single<LlmPort> { FakeLlm() }
    single<HypothesenPort> { FakeHypothesenPort() }
    single<BeobachtungsPort> { FakeBeobachtungsQuelle(emptyList()) }
    single<BeobachtungsAuswahlPort> { FakeKandidatenquelle(config.voiKandidaten) }
    single<AktionsVorschlagsPort> { FakeAktionsVorschlagsPort(config.aktionsVorschlaege) }
    single<HumanApprovalPort> { config.approval.toHumanApprovalPort() }
    single { BeliefAktualisieren(get(), get(), get()) }
    single { BeobachtungWaehlen(get()) }
    single { AktionGaten(get()) }
    single { Entscheidungszyklus(get(), get(), get()) }
    single { KonfidenzgebundenerEntscheidungszyklus(get(), get()) }
    single { AktionsVorschlagen(get(), get(), get()) }
    single(named("recordingExecution")) { RecordingAktionsAusfuehrungsAdapter() }
    single<RecordingAktionsAusfuehrungsAdapter> { get(named("recordingExecution")) }
    single<AktionsAusfuehrungsAdapter> { get<RecordingAktionsAusfuehrungsAdapter>() }
    single { CliExecutor(get()) }
}

class MonotoneFakeUhr(start: Long = 1L) : UhrPort {
    private var naechster = start

    override fun jetzt(): Zeitstempel = Zeitstempel(naechster++)
}

private fun CliApprovalKonfiguration.toHumanApprovalPort(): HumanApprovalPort = when (this) {
    is CliApprovalKonfiguration.Fake -> FakeApproval(freigegeben)
    is CliApprovalKonfiguration.Kanalwahl -> CliApprovalKanalDispatcher(
        kanal = kanal,
        kanaele = kanaele,
    )
}

class JvmApprovalNonceQuelle : ApprovalNonceQuelle {
    override fun naechsteNonce(): ApprovalNonce = ApprovalNonce(UUID.randomUUID().toString())
}

class ConsoleApprovalAusgabe(
    private val schreibeZeile: (String) -> Unit = ::println,
) : ApprovalAusgabe {
    override fun schreibe(challenge: ApprovalChallenge) {
        schreibeZeile(challenge.text)
    }
}

class ConsoleApprovalEingabe(
    private val leseZeile: () -> String? = ::readlnOrNull,
) : ApprovalEingabe {
    override fun lese(challenge: ApprovalChallenge): ApprovalAntwort? {
        println("nonce:")
        val nonce = leseZeile() ?: return null
        println("identity:")
        val identitaet = leseZeile() ?: return null
        println("context_digest:")
        val kontextDigest = leseZeile() ?: return null
        println("confirmation (${LocalApproval.BESTAETIGUNG}):")
        val bestaetigung = leseZeile() ?: return null
        return ApprovalAntwort(
            nonce = nonce,
            identitaet = identitaet,
            kontextDigest = kontextDigest,
            bestaetigung = bestaetigung,
        )
    }
}
