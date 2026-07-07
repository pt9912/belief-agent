package dev.beliefagent.adapter.observation.buildreport

import dev.beliefagent.application.belief.aktualisieren.ports.BeobachtungsPort
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Zeitstempel
import java.nio.file.Files
import java.nio.file.Path

/**
 * Konkreter Build-Beobachter (ARC-08): liest einen lokalen Build-/Test-Report
 * oder dessen Replay-Fixture und liefert daraus eine `Quelle.BUILD`-Beobachtung.
 * Der Adapter fuehrt selbst keinen Build aus.
 */
class BuildReportBeobachter(
    private val quelle: BuildReportQuelle,
    private val zeitstempel: () -> Zeitstempel,
) : BeobachtungsPort {
    override fun lies(): List<Beobachtung> = listOf(quelle.lies().alsBeobachtung(zeitstempel()))
}

fun interface BuildReportQuelle {
    fun lies(): BuildReport
}

class BuildReportDateiQuelle(
    private val pfad: Path,
) : BuildReportQuelle {
    override fun lies(): BuildReport = BuildReportParser.parse(Files.readString(pfad))
}

data class BuildReport(
    val status: BuildStatus,
    val task: String,
    val summary: String,
    val durationMillis: Long? = null,
) {
    init {
        require(task.isNotBlank()) { "Build-Task darf nicht leer sein" }
        require(summary.isNotBlank()) { "Build-Summary darf nicht leer sein" }
        require(durationMillis == null || durationMillis >= 0L) { "Build-Dauer darf nicht negativ sein" }
    }

    fun alsBeobachtung(zeitstempel: Zeitstempel): Beobachtung = Beobachtung(
        Quelle.BUILD,
        zeitstempel,
        Evidenz(
            buildString {
                append("build status=${status.name}; task=$task")
                durationMillis?.let { append("; durationMillis=$it") }
                append("; summary=$summary")
            },
        ),
    )
}

enum class BuildStatus {
    SUCCESS,
    FAILED,
    UNKNOWN,
}

object BuildReportParser {
    fun parse(raw: String): BuildReport {
        val felder = keyValue(raw)
        val status = felder["status"]?.let { BuildStatus.valueOf(it.uppercase()) }
            ?: error("Build-Report braucht status")
        val task = felder["task"] ?: error("Build-Report braucht task")
        val summary = felder["summary"] ?: error("Build-Report braucht summary")
        val durationMillis = felder["durationMillis"]?.toLong()
        return BuildReport(status, task, summary, durationMillis)
    }

    private fun keyValue(raw: String): Map<String, String> = raw
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .associate { line ->
            val separator = line.indexOf('=')
            require(separator > 0) { "Build-Report-Zeile braucht key=value: $line" }
            line.substring(0, separator).trim() to line.substring(separator + 1).trim()
        }
}
