package dev.beliefagent.adapter.observation.gitlocal

import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Zeitstempel
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun git_cli_nutzt_nur_lokale_status_kommandos() {
        val commands = mutableListOf<List<String>>()
        val quelle = GitCliStatusQuelle(
            repoRoot = Path.of("/repo"),
            runner = GitCommandRunner { _, args ->
                commands += args.toList()
                when (args.toList()) {
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
        assertFalse(commands.any { it.firstOrNull() == "fetch" })
    }

    @Test
    fun git_cli_markiert_detached_head_und_rename_status() {
        val quelle = GitCliStatusQuelle(
            repoRoot = Path.of("/repo"),
            runner = GitCommandRunner { _, args ->
                when (args.toList()) {
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
        assertFalse(snapshot.dirty)
        assertEquals(emptyList(), snapshot.changedFiles)
    }

    @Test
    fun fixture_quelle_liest_snapshot_von_datei() {
        val temp = Files.createTempFile("git-status", ".fixture")
        Files.writeString(
            temp,
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
    fun process_runner_meldet_fehlgeschlagenes_lokales_git_kommando() {
        val fehler = assertFailsWith<IllegalArgumentException> {
            ProcessGitCommandRunner().run(Path.of("/definitely/not/a/repo"), "rev-parse", "HEAD")
        }

        assertTrue(fehler.message!!.contains("Git-Kommando fehlgeschlagen"))
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
}
