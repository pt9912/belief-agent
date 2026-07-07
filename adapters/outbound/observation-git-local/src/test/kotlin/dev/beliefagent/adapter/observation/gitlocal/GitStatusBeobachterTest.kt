package dev.beliefagent.adapter.observation.gitlocal

import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Zeitstempel
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.eclipse.jgit.api.Git

class GitStatusBeobachterTest {

    @Test
    fun liest_replay_snapshot_als_repo_beobachtung() {
        val beobachter = GitStatusBeobachter(
            quelle = GitStatusQuelle {
                GitStatusSnapshot(
                    head = "abc123",
                    branch = "main",
                    dirty = true,
                    changedFiles = listOf("src/Main.kt", "README.md"),
                )
            },
            zeitstempel = { Zeitstempel(99L) },
        )

        val beobachtung = beobachter.lies().single()

        assertEquals(Quelle.REPO, beobachtung.quelle)
        assertEquals(Zeitstempel(99L), beobachtung.zeitstempel)
        assertEquals("git head=abc123; branch=main; dirty=true; changedFiles=src/Main.kt,README.md", beobachtung.evidenz.beschreibung)
    }

    @Test
    fun factory_nutzt_fixture_als_default_und_faellt_nicht_auf_cli_zurueck() {
        val fixture = writeFixture(
            """
            head=def456
            branch=main
            dirty=false
            changedFiles=
            """.trimIndent(),
        )

        val snapshot = GitStatusQuellenFactory.create(
            GitSourceConfig(fixturePath = fixture, repoRoot = Path.of("/nicht/genutzt")),
        ).lies()

        assertEquals("def456", snapshot.head)
        assertEquals("fixture", snapshot.diagnose!!.source)
        assertFalse(snapshot.dirty)
    }

    @Test
    fun factory_verwirft_unbekannten_modus_fail_closed() {
        val fehler = assertFailsWith<IllegalArgumentException> {
            GitStatusQuellenFactory.create(GitSourceConfig(source = "auto", fixturePath = Path.of("fixture")))
        }

        assertTrue(fehler.message!!.contains("Unbekannter Git-Source-Modus"))
    }

    @Test
    fun factory_verlangt_pflichtparameter_pro_modus() {
        val fixtureFehler = assertFailsWith<IllegalArgumentException> {
            GitStatusQuellenFactory.create(GitSourceConfig(source = "fixture"))
        }
        val cliFehler = assertFailsWith<IllegalArgumentException> {
            GitStatusQuellenFactory.create(GitSourceConfig(source = "cli"))
        }
        val jgitFehler = assertFailsWith<IllegalArgumentException> {
            GitStatusQuellenFactory.create(GitSourceConfig(source = "jgit"))
        }

        assertTrue(fixtureFehler.message!!.contains("fixturePath"))
        assertTrue(cliFehler.message!!.contains("repoRoot"))
        assertTrue(jgitFehler.message!!.contains("repoRoot"))
    }

    @Test
    fun git_cli_prueft_version_und_nutzt_nur_lokale_status_kommandos() {
        val commands = mutableListOf<List<String>>()
        val quelle = GitCliStatusQuelle(
            repoRoot = Path.of("/repo"),
            runner = GitCommandRunner { _, args ->
                commands += args.toList()
                when (args.toList()) {
                    listOf("--version") -> "git version 2.45.0\n"
                    listOf("rev-parse", "HEAD") -> "abc123\n"
                    listOf("branch", "--show-current") -> "main\n"
                    listOf("status", "--porcelain=v1") -> " M src/Main.kt\n?? docs/new.md\n"
                    else -> error("unerwartetes Kommando: ${args.toList()}")
                }
            },
        )

        val snapshot = quelle.lies()

        assertEquals("abc123", snapshot.head)
        assertEquals("main", snapshot.branch)
        assertTrue(snapshot.dirty)
        assertEquals(listOf("src/Main.kt", "docs/new.md"), snapshot.changedFiles)
        assertEquals(GitStatusDiagnose(source = "cli", gitVersion = "git version 2.45.0"), snapshot.diagnose)
        assertFalse(commands.any { it.firstOrNull() == "fetch" || it.firstOrNull() == "pull" })
    }

    @Test
    fun git_cli_markiert_detached_head_und_rename_status() {
        val quelle = GitCliStatusQuelle(
            repoRoot = Path.of("/repo"),
            runner = GitCommandRunner { _, args ->
                when (args.toList()) {
                    listOf("--version") -> "git version 2.45.0\n"
                    listOf("rev-parse", "HEAD") -> "abc123\n"
                    listOf("branch", "--show-current") -> "\n"
                    listOf("status", "--porcelain=v1") -> "R  old.kt -> new.kt\n"
                    else -> error("unerwartetes Kommando: ${args.toList()}")
                }
            },
        )

        val snapshot = quelle.lies()

        assertEquals("DETACHED", snapshot.branch)
        assertEquals(listOf("new.kt"), snapshot.changedFiles)
    }

    @Test
    fun process_runner_meldet_fehlende_git_cli_fail_closed() {
        val fehler = assertFailsWith<IllegalStateException> {
            ProcessGitCommandRunner("git-binary-die-es-nicht-gibt-${System.nanoTime()}")
                .run(Path.of("/repo"), "--version")
        }

        assertTrue(fehler.message!!.contains("Git-CLI nicht verfuegbar"))
    }

    @Test
    fun process_runner_meldet_fehlgeschlagenes_lokales_git_kommando() {
        val fehler = assertFailsWith<IllegalArgumentException> {
            ProcessGitCommandRunner().run(Path.of("/definitely/not/a/repo"), "rev-parse", "HEAD")
        }

        assertTrue(fehler.message!!.contains("Git-Kommando fehlgeschlagen"))
    }

    @Test
    fun parser_liest_key_value_fixture_deterministisch() {
        val snapshot = GitStatusFixtureParser.parse(
            """
            head=def456
            branch=feature/demo
            dirty=false
            changedFiles=
            """.trimIndent(),
        )

        assertEquals("def456", snapshot.head)
        assertEquals("feature/demo", snapshot.branch)
        assertEquals("fixture", snapshot.diagnose!!.source)
        assertFalse(snapshot.dirty)
        assertEquals(emptyList(), snapshot.changedFiles)
    }

    @Test
    fun fixture_quelle_liest_snapshot_von_datei() {
        val temp = writeFixture(
            """
            head=789abc
            branch=main
            dirty=true
            changedFiles=a.kt,b.kt
            """.trimIndent(),
        )

        val snapshot = GitStatusFixtureQuelle(temp).lies()

        assertEquals("789abc", snapshot.head)
        assertEquals(listOf("a.kt", "b.kt"), snapshot.changedFiles)
    }

    @Test
    fun parser_verlangt_dirty_feld() {
        val fehler = assertFailsWith<IllegalStateException> {
            GitStatusFixtureParser.parse(
                """
                head=abc
                branch=main
                """.trimIndent(),
            )
        }

        assertTrue(fehler.message!!.contains("dirty"))
    }

    @Test
    fun alle_strategien_erfuellen_clean_contract() {
        assertContract(
            snapshot = fixtureSnapshot(dirty = false, changedFiles = emptyList()),
            branch = "main",
            dirty = false,
            changedFiles = emptyList(),
        )
        assertContract(
            snapshot = cliSnapshot(branchOutput = "main\n", porcelain = ""),
            branch = "main",
            dirty = false,
            changedFiles = emptyList(),
        )
        assertContract(
            snapshot = jgitSnapshot { repo -> repo.commitFile("clean.kt", "fun clean() = Unit") },
            branch = "master",
            dirty = false,
            changedFiles = emptyList(),
        )
    }

    @Test
    fun alle_strategien_erfuellen_dirty_contract() {
        assertContract(
            snapshot = fixtureSnapshot(dirty = true, changedFiles = listOf("dirty.kt")),
            branch = "main",
            dirty = true,
            changedFiles = listOf("dirty.kt"),
        )
        assertContract(
            snapshot = cliSnapshot(branchOutput = "main\n", porcelain = " M dirty.kt\n"),
            branch = "main",
            dirty = true,
            changedFiles = listOf("dirty.kt"),
        )
        assertContract(
            snapshot = jgitSnapshot { repo ->
                repo.commitFile("dirty.kt", "old")
                Files.writeString(repo.resolve("dirty.kt"), "new")
            },
            branch = "master",
            dirty = true,
            changedFiles = listOf("dirty.kt"),
        )
    }

    @Test
    fun alle_strategien_erfuellen_detached_head_contract() {
        assertContract(
            snapshot = fixtureSnapshot(branch = "DETACHED", dirty = false, changedFiles = emptyList()),
            branch = "DETACHED",
            dirty = false,
            changedFiles = emptyList(),
        )
        assertContract(
            snapshot = cliSnapshot(branchOutput = "\n", porcelain = ""),
            branch = "DETACHED",
            dirty = false,
            changedFiles = emptyList(),
        )
        assertContract(
            snapshot = jgitSnapshot { repo ->
                val commit = repo.commitFile("detached.kt", "fun detached() = Unit")
                Git.open(repo.toFile()).use { git -> git.checkout().setName(commit.name).call() }
            },
            branch = "DETACHED",
            dirty = false,
            changedFiles = emptyList(),
        )
    }

    @Test
    fun alle_strategien_erfuellen_rename_contract() {
        assertContract(
            snapshot = fixtureSnapshot(dirty = true, changedFiles = listOf("new.kt")),
            branch = "main",
            dirty = true,
            changedFiles = listOf("new.kt"),
        )
        assertContract(
            snapshot = cliSnapshot(branchOutput = "main\n", porcelain = "R  old.kt -> new.kt\n"),
            branch = "main",
            dirty = true,
            changedFiles = listOf("new.kt"),
        )
        assertContract(
            snapshot = jgitSnapshot { repo ->
                repo.commitFile("old.kt", "fun same() = 1")
                Files.move(repo.resolve("old.kt"), repo.resolve("new.kt"))
            },
            branch = "master",
            dirty = true,
            changedFiles = listOf("new.kt"),
        )
    }

    private fun fixtureSnapshot(
        branch: String = "main",
        dirty: Boolean,
        changedFiles: List<String>,
    ): GitStatusSnapshot = GitStatusFixtureParser.parse(
        """
        head=abc123
        branch=$branch
        dirty=$dirty
        changedFiles=${changedFiles.joinToString(",")}
        """.trimIndent(),
    )

    private fun cliSnapshot(branchOutput: String, porcelain: String): GitStatusSnapshot = GitCliStatusQuelle(
        repoRoot = Path.of("/repo"),
        runner = GitCommandRunner { _, args ->
            when (args.toList()) {
                listOf("--version") -> "git version 2.45.0\n"
                listOf("rev-parse", "HEAD") -> "abc123\n"
                listOf("branch", "--show-current") -> branchOutput
                listOf("status", "--porcelain=v1") -> porcelain
                else -> error("unerwartetes Kommando: ${args.toList()}")
            }
        },
    ).lies()

    private fun jgitSnapshot(mutator: (Path) -> Unit): GitStatusSnapshot {
        val repo = Files.createTempDirectory("jgit-contract")
        Git.init().setDirectory(repo.toFile()).call().close()
        mutator(repo)
        return JGitStatusQuelle(repo).lies()
    }

    private fun Path.commitFile(name: String, content: String): org.eclipse.jgit.revwalk.RevCommit {
        Files.writeString(resolve(name), content)
        Git.open(toFile()).use { git ->
            git.add().addFilepattern(name).call()
            return git.commit()
                .setAuthor("Slice Test", "slice-test@example.invalid")
                .setCommitter("Slice Test", "slice-test@example.invalid")
                .setMessage("commit ${Path.of(name).name}")
                .call()
        }
    }

    private fun assertContract(
        snapshot: GitStatusSnapshot,
        branch: String,
        dirty: Boolean,
        changedFiles: List<String>,
    ) {
        assertEquals("abc123".isNotBlank(), snapshot.head.isNotBlank())
        assertEquals(branch, snapshot.branch)
        assertEquals(dirty, snapshot.dirty)
        assertEquals(changedFiles, snapshot.changedFiles)
    }

    private fun writeFixture(content: String): Path {
        val temp = Files.createTempFile("git-status", ".fixture")
        Files.writeString(temp, content)
        return temp
    }
}
