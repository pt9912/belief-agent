# Code-Agent Composition Example

**Status:** v0 runnable example.

This example demonstrates a runnable Code-Agent wiring over all main Core-Ports:

- `AktionsVorschlagsPort` (static in-memory concrete implementation)
- `BeobachtungsAuswahlPort` (static in-memory concrete implementation)
- `HypothesenPort` (static in-memory concrete implementation)
- `HumanApprovalPort` (env-gesteuerter Schalter)
- `KonfidenzPort` (Replay/append-only: `MemoryKonfidenzPort`)
- `AuditPort` (append-only: `MemoryAudit`)
- `LlmPort` (`FakeLlm` for deterministic offline inference)

The example is intentionally deterministic and local:

- no real API keys,
- no real tools/services,
- no local Gradle runtime required in normal execution.

## Wie der Controller verdrahtet ist

```kotlin
val beliefAktualisieren = BeliefAktualisieren(FakeLlm(), MonotoneDemoClock(), StaticHypothesenPort())
val actionPort = StaticAktionsVorschlagsPort(seedActionSuggestions())
val vorschlaege = AktionsVorschlagen(actionPort, MemoryKonfidenzPort.leer(), MemoryAudit())
val entscheide = Entscheidungszyklus(
    BeobachtungWaehlen(StaticBeobachtungsAuswahlPort(...)),
    beliefAktualisieren,
    AktionGaten(StaticApproval(approvalFreigegeben)),
)
val konfidenzEntscheide = KonfidenzgebundenerEntscheidungszyklus(entscheide, konfidenzPort)
```

## Ausführung

Run via Make/Docker:

```sh
make example-code-agent
```

Erwartete Konsolenausgabe (Default: keine Freigabe):

```text
belief-agent code-agent example
compose=CodeAgentController
scenario=code-agent
terminal=eskaliert
executed=false
executor_boundary=closed
reason=GateEskalation
approval=false
resthypothese=0.000152
audit_events=7
```

Setze `CODE_AGENT_APPROVAL_APPROVED=true`, dann wird der gleiche Lauf als
`gehandelt` durchlaufen:

```sh
make example-code-agent CODE_AGENT_APPROVAL_APPROVED=true
```

Erwartete Konsolenausgabe (mit Genehmigung):

```text
belief-agent code-agent example
compose=CodeAgentController
scenario=code-agent
terminal=gehandelt
executed=true
executor_boundary=Zyklusergebnis.Gehandelt.freigabe.aktion
reason=gate_freigegeben
approval=true
resthypothese=0.001370
audit_events=7
```

## Hinweis

`CodeAgentController` in diesem Beispiel fasst die in einem Produktivagenten typischen
Schritte zusammen: Beobachtungen holen, Belief aktualisieren, Vorschlag filtern
und den `KonfidenzgebundenenEntscheidungszyklus` durchlaufen.
