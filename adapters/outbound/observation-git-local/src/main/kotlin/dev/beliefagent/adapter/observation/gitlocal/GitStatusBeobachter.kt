package dev.beliefagent.adapter.observation.gitlocal

import dev.beliefagent.application.belief.aktualisieren.ports.BeobachtungsPort
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Zeitstempel
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString

/**
 * Konkreter Git-Beobachter (ARC-08): liest ausschliesslich lokale Git-Daten
 * (`rev-parse`, `branch --show-current`, `status --porcelain`) oder ein
 * Replay-Fixture. Remote-Kommandos wie `fetch` sind nicht Teil dieses Adapters.
 */
class GitStatusBeobachter(
    private val quelle: GitStatusQuelle,
    private val zeitstempel: () -> Zeitstempel,
) : BeobachtungsPort {
    override fun lies(): List<Beobachtung> = listOf(quelle.lies().alsBeobachtung(zeitstempel()))
}

fun interface GitStatusQuelle {
    fun lies(): GitStatusSnapshot
}

class GitCliStatusQuelle(
    private val repoRoot: Path,
    private val runner: GitCommandRunner = ProcessGitCommandRunner(),
) : GitStatusQuelle {
    override fun lies(): GitStatusSnapshot {
        val head = runner.run(repoRoot, "rev-parse", "HEAD").trim()
        val branch = runner.run(repoRoot, "branch", "--show-current").trim().ifBlank { "DETACHED" }
        val changedFiles = runner.run(repoRoot, "status", "--porcelain=v1")
            .lineSequence()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
            .map(::changedFileFromPorcelain)
            .toList()
        return GitStatusSnapshot(
            head = head,
            branch = branch,
            dirty = changedFiles.isNotEmpty(),
            changedFiles = changedFiles,
        )
    }

    private fun changedFileFromPorcelain(line: String): String {
        require(line.length >= 4) { "Git-Porcelain-Zeile zu kurz: $line" }
        return line.substring(3).substringAfter(" -> ").trim()
    }
}

fun interface GitCommandRunner {
    fun run(repoRoot: Path, vararg args: String): String
}

class ProcessGitCommandRunner : GitCommandRunner {
    override fun run(repoRoot: Path, vararg args: String): String {
        val command = listOf("git", "-C", repoRoot.absolutePathString()) + args
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()
        require(exit == 0) {
            "Git-Kommando fehlgeschlagen (${args.joinToString(" ")}): ${output.trim()}"
        }
        return output
    }
}

class GitStatusFixtureQuelle(
    private val pfad: Path,
) : GitStatusQuelle {
    override fun lies(): GitStatusSnapshot = GitStatusFixtureParser.parse(Files.readString(pfad))
}

data class GitStatusSnapshot(
    val head: String,
    val branch: String,
    val dirty: Boolean,
    val changedFiles: List<String>,
) {
    init {
        require(head.isNotBlank()) { "Git-HEAD darf nicht leer sein" }
        require(branch.isNotBlank()) { "Git-Branch darf nicht leer sein" }
        require(changedFiles.none { it.isBlank() }) { "Git-Changed-Files duerfen nicht leer sein" }
    }

    fun alsBeobachtung(zeitstempel: Zeitstempel): Beobachtung = Beobachtung(
        Quelle.REPO,
        zeitstempel,
        Evidenz(
            "git head=$head; branch=$branch; dirty=$dirty; changedFiles=${changedFiles.joinToString(",")}",
        ),
    )
}

object GitStatusFixtureParser {
    fun parse(raw: String): GitStatusSnapshot {
        val felder = keyValue(raw)
        val changedFiles = felder["changedFiles"]
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        return GitStatusSnapshot(
            head = felder["head"] ?: error("Git-Fixture braucht head"),
            branch = felder["branch"] ?: error("Git-Fixture braucht branch"),
            dirty = felder["dirty"]?.toBooleanStrict() ?: error("Git-Fixture braucht dirty"),
            changedFiles = changedFiles,
        )
    }

    private fun keyValue(raw: String): Map<String, String> = raw
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .associate { line ->
            val separator = line.indexOf('=')
            require(separator > 0) { "Git-Fixture-Zeile braucht key=value: $line" }
            line.substring(0, separator).trim() to line.substring(separator + 1).trim()
        }
}
