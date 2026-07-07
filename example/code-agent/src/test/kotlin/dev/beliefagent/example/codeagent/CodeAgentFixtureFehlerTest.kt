package dev.beliefagent.example.codeagent

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodeAgentFixtureFehlerTest {
    @Test
    fun m0_build_env_missing_faellt_fail_closed_ohne_execute() {
        val repo = fixture("repo.fixture", validRepo())
        val result = runWith(mapOf("CODE_AGENT_REPO_FIXTURE" to repo.toString()))

        result.assertFailClosed("fixture_env_missing")
    }

    @Test
    fun m0_repo_env_missing_faellt_fail_closed_ohne_execute() {
        val build = fixture("build.fixture", validBuild())
        val result = runWith(mapOf("CODE_AGENT_BUILD_FIXTURE" to build.toString()))

        result.assertFailClosed("fixture_env_missing")
    }

    @Test
    fun m0_build_env_empty_faellt_fail_closed_ohne_execute() {
        val repo = fixture("repo.fixture", validRepo())
        val result = runWith(
            mapOf(
                "CODE_AGENT_BUILD_FIXTURE" to "",
                "CODE_AGENT_REPO_FIXTURE" to repo.toString(),
            ),
        )

        result.assertFailClosed("fixture_env_missing")
    }

    @Test
    fun m0_repo_env_empty_faellt_fail_closed_ohne_execute() {
        val build = fixture("build.fixture", validBuild())
        val result = runWith(
            mapOf(
                "CODE_AGENT_BUILD_FIXTURE" to build.toString(),
                "CODE_AGENT_REPO_FIXTURE" to "",
            ),
        )

        result.assertFailClosed("fixture_env_missing")
    }

    @Test
    fun m1_missing_faellt_fail_closed_ohne_execute() {
        val repo = fixture("repo.fixture", validRepo())
        val missing = Files.createTempDirectory("code-agent-fixture-test").resolve("missing.fixture")
        val result = runWith(
            mapOf(
                "CODE_AGENT_BUILD_FIXTURE" to missing.toString(),
                "CODE_AGENT_REPO_FIXTURE" to repo.toString(),
            ),
        )

        result.assertFailClosed("fixture_missing")
    }

    @Test
    fun m1_repo_missing_faellt_fail_closed_ohne_execute() {
        val build = fixture("build.fixture", validBuild())
        val missing = Files.createTempDirectory("code-agent-fixture-test").resolve("missing-repo.fixture")
        val result = runWith(
            mapOf(
                "CODE_AGENT_BUILD_FIXTURE" to build.toString(),
                "CODE_AGENT_REPO_FIXTURE" to missing.toString(),
            ),
        )

        result.assertFailClosed("fixture_missing")
    }

    @Test
    fun m2_unreadable_faellt_fail_closed_ohne_execute() {
        val repo = fixture("repo.fixture", validRepo())
        val directoryInsteadOfFile = Files.createTempDirectory("code-agent-fixture-directory")
        val result = runWith(
            mapOf(
                "CODE_AGENT_BUILD_FIXTURE" to directoryInsteadOfFile.toString(),
                "CODE_AGENT_REPO_FIXTURE" to repo.toString(),
            ),
        )

        result.assertFailClosed("fixture_unreadable")
    }

    @Test
    fun m2_empty_faellt_fail_closed_ohne_execute() {
        val build = fixture("build.fixture", "")
        val repo = fixture("repo.fixture", validRepo())
        val result = runWith(
            mapOf(
                "CODE_AGENT_BUILD_FIXTURE" to build.toString(),
                "CODE_AGENT_REPO_FIXTURE" to repo.toString(),
            ),
        )

        result.assertFailClosed("fixture_empty")
    }

    @Test
    fun m2_repo_empty_faellt_fail_closed_ohne_execute() {
        val build = fixture("build.fixture", validBuild())
        val repo = fixture("repo.fixture", "")
        val result = runWith(
            mapOf(
                "CODE_AGENT_BUILD_FIXTURE" to build.toString(),
                "CODE_AGENT_REPO_FIXTURE" to repo.toString(),
            ),
        )

        result.assertFailClosed("fixture_empty")
    }

    @Test
    fun m3_malformed_json_faellt_fail_closed_ohne_execute() {
        val build = fixture("build.fixture", "not-json-and-not-key-value")
        val repo = fixture("repo.fixture", validRepo())
        val result = runWith(
            mapOf(
                "CODE_AGENT_BUILD_FIXTURE" to build.toString(),
                "CODE_AGENT_REPO_FIXTURE" to repo.toString(),
            ),
        )

        result.assertFailClosed("fixture_malformed_json")
    }

    @Test
    fun m4_schema_mismatch_faellt_fail_closed_ohne_execute() {
        val build = fixture(
            "build.fixture",
            """
            status=FAILED
            summary=missing task
            """.trimIndent(),
        )
        val repo = fixture("repo.fixture", validRepo())
        val result = runWith(
            mapOf(
                "CODE_AGENT_BUILD_FIXTURE" to build.toString(),
                "CODE_AGENT_REPO_FIXTURE" to repo.toString(),
            ),
        )

        result.assertFailClosed("fixture_schema_mismatch")
    }

    @Test
    fun m5_invalid_encoding_faellt_fail_closed_ohne_execute() {
        val build = binaryFixture("build.fixture", byteArrayOf(0xC3.toByte(), 0x28.toByte()))
        val repo = fixture("repo.fixture", validRepo())
        val result = runWith(
            mapOf(
                "CODE_AGENT_BUILD_FIXTURE" to build.toString(),
                "CODE_AGENT_REPO_FIXTURE" to repo.toString(),
            ),
        )

        result.assertFailClosed("fixture_encoding_invalid")
    }

    @Test
    fun positiver_runtime_pfad_bleibt_eskalation_ohne_execute() {
        val build = fixture("build.fixture", validBuild())
        val repo = fixture("repo.fixture", validRepo())
        val result = runWith(
            mapOf(
                "CODE_AGENT_BUILD_FIXTURE" to build.toString(),
                "CODE_AGENT_REPO_FIXTURE" to repo.toString(),
            ),
        )

        assertEquals(0, result.exitCode)
        assertTrue(result.output.contains("terminal=eskaliert"))
        assertTrue(result.output.contains("executed=false"))
        assertTrue(result.output.contains("executor_boundary=closed"))
        assertFalse(result.output.any { it.startsWith("execute=") })
    }

    private fun runWith(env: Map<String, String?>): RunResult {
        val output = mutableListOf<String>()
        val exitCode = runCodeAgent(env) { line -> output += line }
        return RunResult(exitCode, output)
    }

    private fun RunResult.assertFailClosed(fehlerklasse: String) {
        assertEquals(65, exitCode)
        assertTrue(output.contains("terminal=eskaliert"))
        assertTrue(output.contains("executed=false"))
        assertTrue(output.contains("executor_boundary=closed"))
        assertTrue(output.any { it.startsWith("reason=$fehlerklasse") }, output.joinToString("\n"))
        assertFalse(output.any { it.startsWith("execute=") })
    }

    private fun fixture(name: String, content: String): Path = Files.createTempDirectory("code-agent-fixture-test")
        .resolve(name)
        .also { it.writeText(content) }

    private fun binaryFixture(name: String, content: ByteArray): Path = Files.createTempDirectory("code-agent-fixture-test")
        .resolve(name)
        .also { it.writeBytes(content) }

    private fun validBuild(): String = """
        status=FAILED
        task=:example:code-agent:test
        summary=deterministic regression signal from build fixture
        durationMillis=4242
    """.trimIndent()

    private fun validRepo(): String = """
        head=abc123fixture
        branch=main
        dirty=true
        changedFiles=src/regression/Payment.kt,README.md
    """.trimIndent()

    private data class RunResult(
        val exitCode: Int,
        val output: List<String>,
    )
}
