package dev.beliefagent.adapter.observation.gitlocal

import dev.beliefagent.application.belief.aktualisieren.ports.BeobachtungsPort
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Zeitstempel
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.util.io.DisabledOutputStream

/**
 * Konkreter Git-Beobachter (ARC-08): liest ausschliesslich lokale Git-Daten
 * ueber explizit konfigurierte Quellen. Replay-Fixture, Git-CLI und JGit sind
 * getrennte Strategien; unbekannte Modi oder fehlende Parameter fallen nicht
 * still auf eine andere Strategie zurueck.
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

data class GitSourceConfig(
    val source: String = GitSource.FIXTURE.mode,
    val fixturePath: Path? = null,
    val repoRoot: Path? = null,
    val gitBinary: String = "git",
)

object GitStatusQuellenFactory {
    fun create(config: GitSourceConfig): GitStatusQuelle = when (GitSource.from(config.source)) {
        GitSource.FIXTURE -> GitStatusFixtureQuelle(
            requirePath(config.fixturePath, "fixturePath", GitSource.FIXTURE),
        )
        GitSource.CLI -> GitCliStatusQuelle(
            repoRoot = requirePath(config.repoRoot, "repoRoot", GitSource.CLI),
            runner = ProcessGitCommandRunner(config.gitBinary),
        )
        GitSource.JGIT -> JGitStatusQuelle(
            repoRoot = requirePath(config.repoRoot, "repoRoot", GitSource.JGIT),
        )
    }

    private fun requirePath(path: Path?, name: String, source: GitSource): Path = requireNotNull(path) {
        "Git-Source ${source.mode} braucht Parameter $name"
    }
}

enum class GitSource(val mode: String) {
    FIXTURE("fixture"),
    CLI("cli"),
    JGIT("jgit"),
    ;

    companion object {
        fun from(raw: String): GitSource {
            val mode = raw.trim().lowercase()
            return entries.firstOrNull { it.mode == mode }
                ?: throw IllegalArgumentException("Unbekannter Git-Source-Modus: $raw")
        }
    }
}

class GitCliStatusQuelle(
    private val repoRoot: Path,
    private val runner: GitCommandRunner = ProcessGitCommandRunner(),
) : GitStatusQuelle {
    override fun lies(): GitStatusSnapshot {
        val version = runner.run(repoRoot, "--version").trim()
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
            diagnose = GitStatusDiagnose(source = GitSource.CLI.mode, gitVersion = version),
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

class ProcessGitCommandRunner(
    private val gitBinary: String = "git",
) : GitCommandRunner {
    override fun run(repoRoot: Path, vararg args: String): String {
        val command = listOf(gitBinary, "-C", repoRoot.absolutePathString()) + args
        val process = try {
            ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
        } catch (error: IOException) {
            throw IllegalStateException("Git-CLI nicht verfuegbar ($gitBinary): ${error.message}", error)
        }
        val output = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()
        require(exit == 0) {
            "Git-Kommando fehlgeschlagen (${args.joinToString(" ")}): ${output.trim()}"
        }
        return output
    }
}

class JGitStatusQuelle(
    private val repoRoot: Path,
    private val reader: JGitSnapshotReader = EclipseJGitSnapshotReader(),
) : GitStatusQuelle {
    override fun lies(): GitStatusSnapshot = reader.read(repoRoot)
}

fun interface JGitSnapshotReader {
    fun read(repoRoot: Path): GitStatusSnapshot
}

class EclipseJGitSnapshotReader : JGitSnapshotReader {
    override fun read(repoRoot: Path): GitStatusSnapshot = openRepository(repoRoot).use { repository ->
        val status = Git(repository).use { git -> git.status().call() }
        val changedFiles = status.changedFiles(repository, repoRoot)
        GitStatusSnapshot(
            head = requireNotNull(repository.resolve(Constants.HEAD)) {
                "JGit-Repository hat keinen HEAD: ${repoRoot.absolutePathString()}"
            }.name,
            branch = repository.branchName(),
            dirty = changedFiles.isNotEmpty(),
            changedFiles = changedFiles,
            diagnose = GitStatusDiagnose(source = GitSource.JGIT.mode, gitVersion = jgitVersion()),
        )
    }

    private fun openRepository(repoRoot: Path): Repository = FileRepositoryBuilder()
        .setWorkTree(repoRoot.toFile())
        .findGitDir(repoRoot.toFile())
        .build()

    private fun org.eclipse.jgit.api.Status.changedFiles(repository: Repository, repoRoot: Path): List<String> {
        val files = linkedSetOf<String>()
        files += added
        files += changed
        files += modified
        files += missing
        files += removed
        files += untracked
        files += untrackedFolders
        files += conflicting

        for (rename in repository.renamesAgainstWorkingTree(repoRoot)) {
            files -= rename.oldPath
            files -= rename.newPath
            files += rename.newPath
        }

        return files.filter { it.isNotBlank() }.sorted()
    }

    private fun Repository.renamesAgainstWorkingTree(repoRoot: Path): List<DiffEntry> {
        val headTree = resolve("${Constants.HEAD}^{tree}") ?: return emptyList()
        newObjectReader().use { objectReader ->
            val oldTree = CanonicalTreeParser().apply { reset(objectReader, headTree) }
            val newTree = FileTreeIterator(this)
            DiffFormatter(DisabledOutputStream.INSTANCE).use { formatter ->
                formatter.setRepository(this)
                formatter.setDetectRenames(true)
                return formatter.scan(oldTree, newTree)
                    .filter { it.changeType == DiffEntry.ChangeType.RENAME }
            }
        }
    }

    private fun Repository.branchName(): String {
        val full = fullBranch.orEmpty()
        return if (full.startsWith(Constants.R_HEADS)) {
            Repository.shortenRefName(full)
        } else {
            "DETACHED"
        }
    }

    private fun jgitVersion(): String = Git::class.java.`package`?.implementationVersion ?: "unknown"
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
    val diagnose: GitStatusDiagnose? = null,
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
            "git head=$head; branch=$branch; dirty=$dirty; changedFiles=${changedFiles.joinToString(",")}" + diagnoseText(),
        ),
    )

    private fun diagnoseText(): String = diagnose?.let { diagnose ->
        val version = diagnose.gitVersion?.let { "; gitVersion=$it" }.orEmpty()
        "; source=${diagnose.source}$version"
    }.orEmpty()
}

data class GitStatusDiagnose(
    val source: String,
    val gitVersion: String? = null,
)

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
            diagnose = GitStatusDiagnose(source = GitSource.FIXTURE.mode),
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
