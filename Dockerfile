# syntax=docker/dockerfile:1
# Reproduzierbarer KMP-Build (Regelwerk Modul 14): deps -> build -> test.
# Toolchain digest-gepinnt (ADR-0002, JDK 21). Einstiegspunkt ist das Makefile
# (make build / make test) — kein Host-JDK/-Gradle (AGENTS.md §3.1).
#
# Die CLI-Stage unten startet den ARC-09-Composition-Root netzfrei gegen
# deterministische Adapter. Eine gehaertete Runtime-Image-Stage folgt separat.

# --- deps: gepinnte Toolchain + Dependency-Resolve (cache-freundlich) -------
FROM gradle:8.14-jdk21@sha256:dae150d9066fc04a791ec7f0adc1a0eb4e867f11d76d03063ee0a60e5da56149 AS deps
WORKDIR /src
# Nur Build-Metadaten zuerst: der Layer-Cache greift, solange sie unveraendert
# sind (kein Neu-Resolve bei reinen Code-Aenderungen).
COPY settings.gradle.kts build.gradle.kts ./
COPY hexagon/domain/build.gradle.kts ./hexagon/domain/build.gradle.kts
COPY hexagon/application/build.gradle.kts ./hexagon/application/build.gradle.kts
COPY adapters/outbound/llm-fake/build.gradle.kts ./adapters/outbound/llm-fake/build.gradle.kts
COPY adapters/outbound/llm-langchain4j/build.gradle.kts ./adapters/outbound/llm-langchain4j/build.gradle.kts
COPY adapters/outbound/llm-koog/build.gradle.kts ./adapters/outbound/llm-koog/build.gradle.kts
COPY adapters/outbound/observation-fake/build.gradle.kts ./adapters/outbound/observation-fake/build.gradle.kts
COPY adapters/outbound/observation-build-report/build.gradle.kts ./adapters/outbound/observation-build-report/build.gradle.kts
COPY adapters/outbound/observation-git-local/build.gradle.kts ./adapters/outbound/observation-git-local/build.gradle.kts
COPY adapters/outbound/audit-memory/build.gradle.kts ./adapters/outbound/audit-memory/build.gradle.kts
COPY adapters/outbound/approval-fake/build.gradle.kts ./adapters/outbound/approval-fake/build.gradle.kts
COPY adapters/outbound/voi-fake/build.gradle.kts ./adapters/outbound/voi-fake/build.gradle.kts
COPY adapters/outbound/llm-hypothesen-fake/build.gradle.kts ./adapters/outbound/llm-hypothesen-fake/build.gradle.kts
COPY adapters/outbound/konfidenz-memory/build.gradle.kts ./adapters/outbound/konfidenz-memory/build.gradle.kts
COPY adapters/outbound/llm-action-fake/build.gradle.kts ./adapters/outbound/llm-action-fake/build.gradle.kts
COPY adapters/inbound/cli/build.gradle.kts ./adapters/inbound/cli/build.gradle.kts
COPY example/langchain/build.gradle.kts ./example/langchain/build.gradle.kts
COPY example/koog/build.gradle.kts ./example/koog/build.gradle.kts
COPY example/code-agent/build.gradle.kts ./example/code-agent/build.gradle.kts
RUN gradle --no-daemon --console=plain :hexagon:domain:dependencies :hexagon:application:dependencies :adapters:outbound:llm-fake:dependencies :adapters:outbound:llm-langchain4j:dependencies :adapters:outbound:llm-koog:dependencies :adapters:outbound:observation-fake:dependencies :adapters:outbound:observation-build-report:dependencies :adapters:outbound:observation-git-local:dependencies :adapters:outbound:audit-memory:dependencies :adapters:outbound:approval-fake:dependencies :adapters:outbound:voi-fake:dependencies :adapters:outbound:llm-hypothesen-fake:dependencies :adapters:outbound:konfidenz-memory:dependencies :adapters:outbound:llm-action-fake:dependencies :adapters:inbound:cli:dependencies :example:langchain:dependencies :example:koog:dependencies :example:code-agent:dependencies

# --- build: Quellcode kompilieren (alle Module) ----------------------------
FROM deps AS build
COPY hexagon ./hexagon
COPY adapters ./adapters
COPY example ./example
RUN gradle --no-daemon --console=plain assemble

# --- test: deterministische Tests aller Module (LH-QA-03) ------------------
FROM build AS test
RUN gradle --no-daemon --console=plain --max-workers=1 \
    allTests \
    :adapters:inbound:cli:test \
    :adapters:outbound:llm-langchain4j:test \
    :adapters:outbound:llm-koog:test \
    :adapters:outbound:observation-build-report:test \
    :adapters:outbound:observation-git-local:test \
    :example:code-agent:test

# --- coverage: Kover Line-Coverage-Report je Modul (Modul 13; Report) -------
# Report über alle logik-tragenden Module (per-Modul kover, ADR-0006).
FROM build AS coverage
RUN gradle --no-daemon --console=plain \
    :hexagon:domain:koverLog :hexagon:application:koverLog \
    :adapters:outbound:llm-fake:koverLog :adapters:outbound:llm-langchain4j:koverLog \
    :adapters:outbound:llm-koog:koverLog :adapters:outbound:observation-fake:koverLog \
    :adapters:outbound:observation-build-report:koverLog :adapters:outbound:observation-git-local:koverLog \
    :adapters:outbound:audit-memory:koverLog :adapters:outbound:approval-fake:koverLog \
    :adapters:outbound:voi-fake:koverLog :adapters:outbound:llm-hypothesen-fake:koverLog \
    :adapters:outbound:konfidenz-memory:koverLog :adapters:outbound:llm-action-fake:koverLog \
    :adapters:inbound:cli:koverLog

# --- coverage-gate: Kover Schwellen-Verifikation (ADR-0004/ADR-0006) --------
# Gate über domain + application + alle Adapter (Schwellen je Modul, ADR-0006).
FROM build AS coverage-gate
RUN gradle --no-daemon --console=plain --max-workers=1 \
    :hexagon:domain:koverVerify :hexagon:application:koverVerify \
    :adapters:outbound:llm-fake:koverVerify :adapters:outbound:llm-langchain4j:koverVerify \
    :adapters:outbound:llm-koog:koverVerify :adapters:outbound:observation-fake:koverVerify \
    :adapters:outbound:observation-build-report:koverVerify :adapters:outbound:observation-git-local:koverVerify \
    :adapters:outbound:audit-memory:koverVerify :adapters:outbound:approval-fake:koverVerify \
    :adapters:outbound:voi-fake:koverVerify :adapters:outbound:llm-hypothesen-fake:koverVerify \
    :adapters:outbound:konfidenz-memory:koverVerify :adapters:outbound:llm-action-fake:koverVerify \
    :adapters:inbound:cli:koverVerify

# --- cli-demo: produktiver Composition-Root, netzfrei gegen Fakes -----------
FROM build AS cli-demo
RUN gradle --no-daemon --console=plain :adapters:inbound:cli:run

# --- cli-demo-scenarios: alle deterministischen CLI-Szenarien ---------------
FROM build AS cli-demo-scenarios
RUN gradle --no-daemon --console=plain :adapters:inbound:cli:run --args='all'

# --- example-langchain: lauffaehiges Integrationsbeispiel -------------------
FROM build AS example-langchain
RUN gradle --no-daemon --console=plain :example:langchain:run

# --- example-koog: lauffaehiges Integrationsbeispiel ------------------------
FROM build AS example-koog
RUN gradle --no-daemon --console=plain :example:koog:run

# --- example-code-agent: direkt ausfuehrbares Runtime-Image ---------------
FROM build AS example-code-agent-dist
RUN gradle --no-daemon --console=plain :example:code-agent:installDist

FROM eclipse-temurin:21-jre@sha256:d2b9f8f12212cadcfdf889461531784e8fd097feade954d65b31ee7a71c473ec AS example-code-agent
WORKDIR /app
COPY --from=example-code-agent-dist /src/example/code-agent/build/install/code-agent/ /app/
COPY example/code-agent/fixtures ./fixtures
ARG CODE_AGENT_APPROVAL_APPROVED=false
ARG CODE_AGENT_BUILD_FIXTURE=/app/fixtures/build.fixture
ARG CODE_AGENT_REPO_FIXTURE=/app/fixtures/repo.fixture
ENV CODE_AGENT_APPROVAL_APPROVED=${CODE_AGENT_APPROVAL_APPROVED}
ENV CODE_AGENT_BUILD_FIXTURE=${CODE_AGENT_BUILD_FIXTURE}
ENV CODE_AGENT_REPO_FIXTURE=${CODE_AGENT_REPO_FIXTURE}
ENTRYPOINT ["/app/bin/code-agent"]
