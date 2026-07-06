package dev.beliefagent.adapter.llm.koog

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo

class CapturingKoogLlmClientClassName : LLMClient() {
    override fun llmProvider(): LLMProvider = LLMProvider("acme-llm", "ACME")

    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): Message.Assistant = Message.Assistant(
        """{"proHypothese":{"regression":0.85,"flaky":0.15},"resthypothese":0.2}""",
        ResponseMetaInfo.Empty,
    )

    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult =
        ModerationResult(isHarmful = false, categories = emptyMap())

    override fun close() = Unit
}
