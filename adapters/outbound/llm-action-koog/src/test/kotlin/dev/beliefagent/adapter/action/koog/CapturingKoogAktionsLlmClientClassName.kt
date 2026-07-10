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
 * Reflektiv instanziierbarer Stub-Client (öffentlicher No-arg-Ctor) für den
 * `fromLlmClient(clientClass, …)`-Pfad. Liefert eine gültige Aktionsvorschlags-
 * Antwort; kein Netz, kein API-Key.
 */
class CapturingKoogAktionsLlmClientClassName : LLMClient() {
    override fun llmProvider(): LLMProvider = LLMProvider("acme-llm", "ACME")

    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): Message.Assistant = Message.Assistant(
        """[{"beschreibung":"Log pruefen","hypotheseId":"h1","wirkungsklasse":"NUR_LESEND",""" +
            """"pSuccess":0.8,"konfidenzReferenz":"k1","stuetzendeEvidenz":["e1","e2"]}]""",
        ResponseMetaInfo.Empty,
    )

    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult =
        ModerationResult(isHarmful = false, categories = emptyMap())

    override fun close() = Unit
}
