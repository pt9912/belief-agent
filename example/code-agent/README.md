# Code-Agent Composition Example

**Status:** runnable demo with fixture-backed Build/Repo observations.

This example demonstrates a runnable Code-Agent wiring over the main Core-Ports
and concrete observation adapters:

- `BuildReportBeobachter` from `adapters/outbound/observation-build-report`
- `GitStatusBeobachter` via `GitStatusQuellenFactory(source=fixture)` from
  `adapters/outbound/observation-git-local`
- `AktionsVorschlagsPort` (static in-memory concrete implementation)
- `BeobachtungsAuswahlPort` (static in-memory concrete implementation)
- `HypothesenPort` (static in-memory concrete implementation)
- `HumanApprovalPort` (env-gesteuerter Schalter)
- `KonfidenzPort` (Replay/append-only: `MemoryKonfidenzPort`)
- `AuditPort` (append-only: `MemoryAudit`)
- `LlmPort` (`FakeLlm` for deterministic offline inference)

The example is deterministic and local:

- no real API keys,
- no remote Git access,
- no networked build/repo observation,
- fixture files are read from the image or from explicit env overrides.

## Fixture contract

Default paths used by `make example-code-agent`:

```text
CODE_AGENT_BUILD_FIXTURE=example/code-agent/fixtures/build.fixture
CODE_AGENT_REPO_FIXTURE=example/code-agent/fixtures/repo.fixture
```

Both paths are passed as Docker build args and baked as non-empty `ENV` values
into the runtime image `$(IMAGE):example-code-agent`.

Explicit override example:

```sh
make example-code-agent \
  CODE_AGENT_BUILD_FIXTURE=/tmp/my-build.fixture \
  CODE_AGENT_REPO_FIXTURE=/tmp/my-repo.fixture
```

The build fixture uses the `BuildReportParser` key-value format:

```text
status=FAILED
task=:example:code-agent:test
summary=deterministic regression signal from build fixture
durationMillis=4242
```

The repo fixture uses the `GitStatusFixtureParser` key-value format:

```text
head=abc123fixture
branch=main
dirty=true
changedFiles=src/regression/Payment.kt,README.md
```

Missing, empty or malformed fixtures are not normalized in this slice. The
negative error-class matrix is handled by `slice-033`; this slice only defines
and exercises the positive fixture-backed runtime path.

## Ausfuehrung

Build and run the image creation sensor:

```sh
make example-code-agent
```

Run the produced runtime image directly:

```sh
docker run --rm belief-agent:example-code-agent
```

Expected output contains the concrete observation sources, timestamps and the
terminal decision fields:

```text
belief-agent code-agent example
compose=CodeAgentController
scenario=code-agent
observation source=BUILD; timestamp=10; evidence=build status=FAILED; task=:example:code-agent:test; durationMillis=4242; summary=deterministic regression signal from build fixture
observation source=REPO; timestamp=11; evidence=git head=abc123fixture; branch=main; dirty=true; changedFiles=src/regression/Payment.kt,README.md; source=fixture
terminal=eskaliert
executed=false
executor_boundary=closed
reason=GateEskalation
audit_events=7
```

Set `CODE_AGENT_APPROVAL_APPROVED=true`, then the same observation-backed run can
reach the handled path after the confidence gate:

```sh
make example-code-agent CODE_AGENT_APPROVAL_APPROVED=true
docker run --rm -e CODE_AGENT_APPROVAL_APPROVED=true belief-agent:example-code-agent
```

## Boundary

`CodeAgentController` in this example composes the existing use cases. It does
not add a new productive Composition-Root and does not alter
`adapters/inbound/cli`. Externally effective execution still only happens after
`Zyklusergebnis.Gehandelt.freigabe.aktion`; negative paths print
`executed=false` and keep `executor_boundary=closed`.
