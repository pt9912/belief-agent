# LangChain4j Adapter Example

**Status:** v0 runnable example.

This example shows the LangChain4j adapter behind `LlmPort` without moving
control out of `belief-agent`: `belief-agent` orchestrates, LangChain4j returns
structured estimates.

The runnable module uses the real `LangChain4jLlmPort` with a deterministic
`LangChain4jChatRunner`, so it works without provider credentials or network
access. For a production wire-up, replace the demo runner with your configured
`ChatModel` and use `LangChain4jLlmPort.fromChatModel(chatModel)` directly.

### Configuration

Default is offline mock mode.

```sh
BELIEF_AGENT_LANGCHAIN4J_MODE=mock
```

To run with a real provider in the example process, configure:

```sh
BELIEF_AGENT_LANGCHAIN4J_MODE=real
BELIEF_AGENT_LANGCHAIN4J_PROVIDER=openai
OPENAI_API_KEY=<your-api-key>
OPENAI_MODEL=gpt-4o-mini
OPENAI_BASE_URL=https://api.openai.com/v1  # optional
```

`BELIEF_AGENT_LANGCHAIN4J_MODE=real` requires a suitable LangChain4j model class
on the runtime classpath (for OpenAI this means `langchain4j-open-ai`).

## Files

| File | Purpose |
|---|---|
| [`src/main/kotlin/dev/beliefagent/example/langchain/Main.kt`](src/main/kotlin/dev/beliefagent/example/langchain/Main.kt) | Runnable demo using the LangChain4j adapter boundary |
| [`payloads/likelihood-request.json`](payloads/likelihood-request.json) | Example request shape represented by the adapter prompt |
| [`payloads/likelihood-response.json`](payloads/likelihood-response.json) | Example structured response expected from LangChain4j |

## Run

Use the Docker-backed make target:

```sh
make example-langchain
```

Expected output contains:

```text
belief-agent LangChain4j adapter example
langchain4j_chat_response=...
result=GEHANDELT
executor_allowed=true
```

## Boundary

```text
belief-agent Core
  BeliefAktualisieren / Entscheidungszyklus
    -> LlmPort
      -> LangChain4jLlmPort
        -> LangChain4jChatRunner / ChatModel
          -> LangChain4j provider client
```

LangChain4j may estimate likelihoods. It must not execute externally effective
actions. Execution remains bound to
`Aktionsfreigabe.Freigegeben` from `belief-agent`.

## Contract

For a `LlmPort` integration, the tool receives:

- the current `Beobachtung`,
- the current `BeliefState`,
- the list of known hypothesis IDs,
- the current rest-hypothesis mass.

It returns `Likelihoods`: one non-negative, finite value per known hypothesis and
one non-negative, finite value for the rest hypothesis. These values are
likelihoods, not a probability distribution; they do not have to sum to `1.0`.

The adapter validates the response before constructing domain values:

- exact hypothesis ID set,
- no missing IDs,
- no unknown IDs,
- finite non-negative numbers,
- no fallback to action approval on invalid responses.

## Using The Runnable Module

1. Configure a LangChain4j `ChatModel` in your composition root.
2. Build `LangChain4jLlmPort.fromChatModel(chatModel)`.
3. Inject it into `BeliefAktualisieren`.
4. Let `Entscheidungszyklus` and `AktionGaten` decide whether an action can run.

The user-facing integration notes live in
[`docs/user/integration.md`](../../docs/user/integration.md).
