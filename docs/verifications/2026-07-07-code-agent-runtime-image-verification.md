# Verification — Code-Agent Runtime-Image-Fix

**Datum:** 2026-07-07
**Scope:** Runtime-Image fuer `belief-agent:example-code-agent` ohne Gradle-ENTRYPOINT.
**Bezug:** `slice-032`, `slice-033`, `LH-FA-OBS-001`, `LH-FA-OBS-006`, `LH-QA-02`, `LH-QA-04`, `LH-FA-POL-006`, `ARC-08`.

## Verifizierte Eigenschaften

| Eigenschaft | Ergebnis |
|---|---|
| Runtime-Base ohne Gradle | Erfuellt: finale Stage `FROM eclipse-temurin:21-jre@sha256:d2b9f8f12212cadcfdf889461531784e8fd097feade954d65b31ee7a71c473ec`. |
| Runtime-Entrypoint ohne Gradle | Erfuellt: `ENTRYPOINT ["/app/bin/code-agent"]`. |
| Make-Run-Target | Erfuellt: `make example-code-agent-run`. |
| Positive Runtime-Ausgabe | Erfuellt: `docker run --rm belief-agent:example-code-agent` gibt nur Demo-Ausgabe aus, keine Gradle-Tasks. |
| Safety Default | Erfuellt: Default endet kontrolliert mit `terminal=eskaliert`, `executed=false`, `executor_boundary=closed`. |

## Sensoren

| Kommando | Ergebnis |
|---|---|
| `docker buildx imagetools inspect eclipse-temurin:21-jre` | gruen; Digest `sha256:d2b9f8f12212cadcfdf889461531784e8fd097feade954d65b31ee7a71c473ec` ermittelt. |
| `make example-code-agent-run` | gruen; baut Runtime-Image und startet `/app/bin/code-agent`. |
| `make gates` | gruen nach Runtime-Umstellung auf installDist/JRE-Stage. |


## Fixture-Pfadvertrag

Runtime-Defaults sind Image-interne Pfade: `/app/fixtures/build.fixture` und `/app/fixtures/repo.fixture`. Repo-relative Host-Pfade sind keine gueltigen Runtime-ENV-Defaults, sofern sie nicht in das Image kopiert oder in den Container gemountet werden.

## Offene Demo-Grenze

Dieser Fix macht das Image runtime-faehig, aber noch keine realistische LLM-Demo. Prompt-Eingabe und echter LLM-Adapter sind separater Scope und muessen vor einer entsprechenden Demo-Closure geplant, reviewed und verifiziert werden.
