package dev.beliefagent.adapter.action.koog

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo

/**
 * Reflektiv ladbare Stubs für die vier Ablehnungsäste von
 * `KoogAktionsVorschlagsPort.fromLlmClient(clientClass, …)` (Code-Review slice-043
 * F-1). Der „Klasse nicht ladbar"-Ast wird über einen nicht existierenden FQN
 * getestet; die drei übrigen Äste je über einen dieser Stubs. Kein Netz, kein
 * API-Key.
 */

/** Ladbar, aber **kein** [LLMClient] → `require(isAssignableFrom)`-Ast. */
class KoogAktionsClientKeinLlmClient

/** [LLMClient], aber **ohne** No-arg-Ctor → `getDeclaredConstructor()`-Ast. */
class KoogAktionsClientOhneNoArgCtor(private val markierung: String) : LLMClient() {
    override fun llmProvider(): LLMProvider = LLMProvider(markierung, markierung)

    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): Message.Assistant = Message.Assistant("[]", ResponseMetaInfo.Empty)

    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult =
        ModerationResult(isHarmful = false, categories = emptyMap())

    override fun close() = Unit
}

/** [LLMClient] mit No-arg-Ctor, der **wirft** → `newInstance()`-Ast. */
class KoogAktionsClientWirftImKonstruktor : LLMClient() {
    init {
        error("Konstruktor-Ausfall (Test)")
    }

    override fun llmProvider(): LLMProvider = LLMProvider("test", "Test")

    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): Message.Assistant = Message.Assistant("[]", ResponseMetaInfo.Empty)

    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult =
        ModerationResult(isHarmful = false, categories = emptyMap())

    override fun close() = Unit
}
