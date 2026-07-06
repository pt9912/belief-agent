package dev.beliefagent.adapter.llm.koog

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Resthypothese
import dev.beliefagent.domain.belief.Zeitstempel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KoogLlmPortTest {

    private fun prior() = BeliefState.of(
        listOf(
            Hypothese(HypotheseId("regression"), 0.4),
            Hypothese(HypotheseId("flaky"), 0.4),
        ),
        Resthypothese(0.2),
    )

    private fun beobachtung() = Beobachtung(
        Quelle.LOG,
        Zeitstempel(7L),
        Evidenz("regression im Zahlungsmodul wahrscheinlich"),
    )

    private fun model() = LLModel(
        provider = LLMProvider("test", "Test"),
        id = "test-model",
    )

    @Test
    fun fragt_koog_runner_mit_beobachtung_und_hypothesen() {
        var prompt = ""
        val port = KoogLlmPort(
            runner = KoogPromptRunner {
                prompt = it
                """{"proHypothese":{"regression":0.85,"flaky":0.15},"resthypothese":0.2}"""
            },
        )

        val likelihoods = port.likelihoods(beobachtung(), prior())

        assertTrue(prompt.contains("regression im Zahlungsmodul wahrscheinlich"))
        assertTrue(prompt.contains("regression"))
        assertTrue(prompt.contains("flaky"))
        assertEquals(0.85, likelihoods.proHypothese[HypotheseId("regression")])
        assertEquals(0.15, likelihoods.proHypothese[HypotheseId("flaky")])
        assertEquals(0.2, likelihoods.resthypothese)
    }

    @Test
    fun factory_nutzt_prompt_executor() {
        val executor = CapturingPromptExecutor(
            """{"proHypothese":{"regression":0.85,"flaky":0.15},"resthypothese":0.2}""",
        )
        val port = KoogLlmPort.fromPromptExecutor(executor, model())

        val likelihoods = port.likelihoods(beobachtung(), prior())

        assertEquals("belief-agent-likelihoods", executor.promptId)
        assertEquals("test-model", executor.modelId)
        assertEquals(0.85, likelihoods.proHypothese[HypotheseId("regression")])
    }

    @Test
    fun factory_nutzt_llm_client() {
        val client = CapturingLlmClient(
            """{"proHypothese":{"regression":0.85,"flaky":0.15},"resthypothese":0.2}""",
        )
        val port = KoogLlmPort.fromLlmClient(client, model())

        val likelihoods = port.likelihoods(beobachtung(), prior())

        assertEquals("belief-agent-likelihoods", client.promptId)
        assertEquals("test-model", client.modelId)
        assertEquals(0.15, likelihoods.proHypothese[HypotheseId("flaky")])
    }

    @Test
    fun weist_fehlende_hypothese_zurueck() {
        val port = KoogLlmPort(
            runner = KoogPromptRunner {
                """{"proHypothese":{"regression":0.85},"resthypothese":0.2}"""
            },
        )

        val fehler = assertFailsWith<IllegalArgumentException> {
            port.likelihoods(beobachtung(), prior())
        }

        assertTrue(fehler.message!!.contains("fehlend=[flaky]"))
    }

    @Test
    fun weist_negative_resthypothese_zurueck() {
        val port = KoogLlmPort(
            runner = KoogPromptRunner {
                """{"proHypothese":{"regression":0.85,"flaky":0.15},"resthypothese":-0.2}"""
            },
        )

        val fehler = assertFailsWith<IllegalArgumentException> {
            port.likelihoods(beobachtung(), prior())
        }

        assertTrue(fehler.message!!.contains("Resthypothesen-Likelihood"))
    }

    @Test
    fun weist_unbekannte_hypothese_zurueck() {
        val port = KoogLlmPort(
            runner = KoogPromptRunner {
                """{"proHypothese":{"regression":0.85,"flaky":0.15,"anderes":0.1},"resthypothese":0.2}"""
            },
        )

        val fehler = assertFailsWith<IllegalArgumentException> {
            port.likelihoods(beobachtung(), prior())
        }

        assertTrue(fehler.message!!.contains("unbekannt=[anderes]"))
    }

    @Test
    fun weist_negative_werte_zurueck() {
        val port = KoogLlmPort(
            runner = KoogPromptRunner {
                """{"proHypothese":{"regression":0.85,"flaky":-0.15},"resthypothese":0.2}"""
            },
        )

        val fehler = assertFailsWith<IllegalArgumentException> {
            port.likelihoods(beobachtung(), prior())
        }

        assertTrue(fehler.message!!.contains("flaky"))
    }

    @Test
    fun parser_verbietet_doppelte_hypothesen_ids() {
        val parser = StrictKoogLikelihoodParser()

        val fehler = assertFailsWith<IllegalArgumentException> {
            parser.parse("""{"proHypothese":{"regression":0.85,"regression":0.75},"resthypothese":0.2}""")
        }

        assertTrue(fehler.message!!.contains("doppelte"))
    }

    @Test
    fun parser_verlangt_striktes_json_objekt() {
        val parser = StrictKoogLikelihoodParser()

        assertFailsWith<IllegalArgumentException> {
            parser.parse("```json\n{\"proHypothese\":{},\"resthypothese\":0.1}\n```")
        }
    }

    @Test
    fun parser_verlangt_exakte_top_level_felder() {
        val parser = StrictKoogLikelihoodParser()

        val fehler = assertFailsWith<IllegalArgumentException> {
            parser.parse("""{"proHypothese":{},"resthypothese":0.1,"aktion":"none"}""")
        }

        assertTrue(fehler.message!!.contains("exakt"))
    }

    @Test
    fun parser_verlangt_pro_hypothese_objekt() {
        val parser = StrictKoogLikelihoodParser()

        val fehler = assertFailsWith<IllegalArgumentException> {
            parser.parse("""{"proHypothese":[],"resthypothese":0.1}""")
        }

        assertTrue(fehler.message!!.contains("proHypothese"))
    }

    @Test
    fun parser_verlangt_numerische_likelihoods() {
        val parser = StrictKoogLikelihoodParser()

        val fehler = assertFailsWith<IllegalArgumentException> {
            parser.parse("""{"proHypothese":{"regression":"hoch"},"resthypothese":0.1}""")
        }

        assertTrue(fehler.message!!.contains("regression"))
    }

    private class CapturingPromptExecutor(
        private val response: String,
    ) : PromptExecutor() {
        var promptId: String? = null
        var modelId: String? = null

        override suspend fun execute(
            prompt: Prompt,
            model: LLModel,
            tools: List<ToolDescriptor>,
        ): Message.Assistant {
            promptId = prompt.id
            modelId = model.id
            return Message.Assistant(response, ResponseMetaInfo.Empty)
        }

        override fun executeStreaming(
            prompt: Prompt,
            model: LLModel,
            tools: List<ToolDescriptor>,
        ): Flow<StreamFrame> = emptyFlow()

        override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult =
            ModerationResult(isHarmful = false, categories = emptyMap())

        override fun close() = Unit
    }

    private class CapturingLlmClient(
        private val response: String,
    ) : LLMClient() {
        var promptId: String? = null
        var modelId: String? = null

        override fun llmProvider(): LLMProvider = LLMProvider("test", "Test")

        override suspend fun execute(
            prompt: Prompt,
            model: LLModel,
            tools: List<ToolDescriptor>,
        ): Message.Assistant {
            promptId = prompt.id
            modelId = model.id
            return Message.Assistant(response, ResponseMetaInfo.Empty)
        }

        override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult =
            ModerationResult(isHarmful = false, categories = emptyMap())

        override fun close() = Unit
    }
}
