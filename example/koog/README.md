# Koog Adapter Example

**Status:** v0 runnable example.

This example shows the Koog LLM adapter behind `LlmPort`: `belief-agent`
orchestrates and Koog returns structured estimates.

The runnable module uses the real `KoogLlmPort` with a deterministic
`KoogPromptRunner`, so it works without provider credentials or network access.
Production composition can switch to `KoogLlmPort.fromLlmClient(client, model)`
or `KoogLlmPort.fromPromptExecutor(executor, model)`.

### Configuration

Default is offline mock mode.

```sh
KOOG_EXAMPLE_MODE=mock
```

To run with a real Koog client in this executable, set:

```sh
KOOG_EXAMPLE_MODE=real
KOOG_CLIENT_CLASS=<fully-qualified-classname>
KOOG_PROVIDER_ID=<provider-id>
KOOG_PROVIDER_NAME=<provider-name>
KOOG_MODEL_ID=<model-id>
```

`KOOG_CLIENT_CLASS` must be loadable at runtime and expose a public no-arg
constructor so this example can build `KoogLlmPort.fromLlmClient(...)`.

## Files

| File | Purpose |
|---|---|
| [`src/main/kotlin/dev/beliefagent/example/koog/Main.kt`](src/main/kotlin/dev/beliefagent/example/koog/Main.kt) | Runnable demo using the Koog adapter boundary |
| [`payloads/likelihood-request.json`](payloads/likelihood-request.json) | Example request shape represented by the adapter prompt |
| [`payloads/likelihood-response.json`](payloads/likelihood-response.json) | Example structured response expected from Koog |

## Run

Use the Docker-backed make target:

```sh
make example-koog
```

Expected output contains:

```text
belief-agent Koog adapter example
koog_prompt_response=...
result=GEHANDELT
executor_allowed=true
```

## Boundary

```text
belief-agent Core
  BeliefAktualisieren / Entscheidungszyklus
    -> LlmPort
      -> KoogLlmPort
        -> KoogPromptRunner / PromptExecutor / LLMClient
          -> Koog provider client
```

Koog may estimate likelihoods, but it must not execute externally effective
actions. Execution remains bound to `Aktionsfreigabe.Freigegeben` from
`belief-agent`.

## Contract

For a `LlmPort` integration, Koog returns:

- one non-negative, finite likelihood per known hypothesis,
- one non-negative, finite likelihood for the rest hypothesis,
- no unknown hypothesis IDs,
- no action execution instruction.

The adapter validates the response before constructing domain values.

The user-facing integration notes live in
[`docs/user/integration.md`](../../docs/user/integration.md).
