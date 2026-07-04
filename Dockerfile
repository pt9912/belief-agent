# syntax=docker/dockerfile:1
# Reproduzierbarer KMP-Build (Regelwerk Modul 14): deps -> build -> test.
# Toolchain digest-gepinnt (ADR-0002, JDK 21). Einstiegspunkt ist das Makefile
# (make build / make test) — kein Host-JDK/-Gradle (AGENTS.md §3.1).
#
# Runtime-Stage bewusst zurueckgestellt: slice-001 liefert eine Bibliothek +
# Tests, noch keine lauffaehige App (ARC-09). Die gehaertete Runtime-Stage
# entsteht mit dem ersten Orchestrator.

# --- deps: gepinnte Toolchain + Dependency-Resolve (cache-freundlich) -------
FROM gradle:8.14-jdk21@sha256:dae150d9066fc04a791ec7f0adc1a0eb4e867f11d76d03063ee0a60e5da56149 AS deps
WORKDIR /src
# Nur Build-Metadaten zuerst: der Layer-Cache greift, solange sie unveraendert
# sind (kein Neu-Resolve bei reinen Code-Aenderungen).
COPY settings.gradle.kts build.gradle.kts ./
COPY hexagon/domain/build.gradle.kts ./hexagon/domain/build.gradle.kts
RUN gradle --no-daemon --console=plain :hexagon:domain:dependencies

# --- build: Quellcode kompilieren ------------------------------------------
FROM deps AS build
COPY hexagon ./hexagon
RUN gradle --no-daemon --console=plain :hexagon:domain:assemble

# --- test: deterministische Tests (LH-QA-03) -------------------------------
FROM build AS test
RUN gradle --no-daemon --console=plain :hexagon:domain:allTests
