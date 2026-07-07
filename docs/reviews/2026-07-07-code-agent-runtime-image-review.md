# Code Review — Code-Agent Runtime-Image-Fix

**Datum:** 2026-07-07
**Scope:** `example-code-agent` Docker-/Make-Runtime-Pfad nach Drift aus `slice-032`.
**Bezug:** `slice-032`, `slice-033`, `LH-FA-OBS-001`, `LH-FA-OBS-006`, `LH-QA-02`, `LH-QA-04`, `LH-FA-POL-006`, `ARC-08`.

## Findings

Keine Findings fuer den Runtime-Image-Fix.

## Gepruefte Punkte

| Kategorie | Quelle | Pfad | Ergebnis | Verifizierbar |
|---|---|
| Runtime-Basis | Dockerfile | `Dockerfile` | Finale `example-code-agent`-Stage nutzt `eclipse-temurin:21-jre` mit Digest-Pin, nicht mehr `gradle`. | ja |
| EntryPoint | Dockerfile | `Dockerfile` | Runtime-Image startet `/app/bin/code-agent` aus `installDist`, nicht `gradle :example:code-agent:run`. | ja |
| Fixtures | Dockerfile/Makefile | `Dockerfile`, `Makefile` | Runtime-Image enthaelt `/app/fixtures/build.fixture` und `/app/fixtures/repo.fixture`; Make-Defaults zeigen auf diese Pfade. | ja |
| Fixture-Overrides | Makefile/README | `Makefile`, `example/code-agent/README.md` | Build-Ziel nutzt image-interne `CODE_AGENT_IMAGE_*`-Make-Variablen; Host-Fixtures sind nur als Runtime-Mount plus Env-Override dokumentiert. | ja |
| Make-Vertrag | Makefile | `Makefile` | `make example-code-agent-run` baut und startet das Runtime-Image. | ja |
| Safety | Runtime-Sensor | `docker run --rm belief-agent:example-code-agent` | Default-Pfad bleibt fail-closed: `terminal=eskaliert`, `executed=false`, `executor_boundary=closed`. | ja |


## Unabhaengiger Review ohne Verlaufskontext

Reviewer: `Chandrasekhar` (separate Agent-Instanz, ohne Kontext-Fork). Datum: 2026-07-07.

| Schwere | Kategorie | Quelle | Pfad | Befund | Status |
|---|---|---|---|---|---|
| Medium | Testabdeckung | Slice-033 DoD / Modul 11 | `example/code-agent/src/test/kotlin/dev/beliefagent/example/codeagent/CodeAgentFixtureFehlerTest.kt` | Repo-Fixture-Negativabdeckung fuer `fixture_missing` und `fixture_empty` fehlte; Build-Fixture war abgedeckt, Repo-Fixture jeweils valide gesetzt. | Eingearbeitet durch `m1_repo_missing_faellt_fail_closed_ohne_execute` und `m2_repo_empty_faellt_fail_closed_ohne_execute`; erneute Sensoren erforderlich. |
| Medium | Artefakt-Konsistenz | Modul 10/11 | `docs/verifications/2026-07-07-slice-033-verification.md`, `docs/reviews/2026-07-07-slice-033-code-review.md` | Artefakte behaupteten Sensor-/Review-Status zu stark fuer einen offenen Slice; dadurch Closure-/DoD-Risiko. | Eingearbeitet: Verification als Zwischenstand formuliert; slice-033-Review als Zwischen-Review mit offenen Folge-Sensoren markiert. |

## Residuale Risiken / offene Punkte

Der Demo-Pfad nutzt weiterhin `FakeLlm()` und hat noch keine Prompt-Eingabe. Das ist kein Finding gegen den Runtime-Image-Fix, bleibt aber ein separater Demo-Scope und darf nicht als realistische LLM-Demo abgeschlossen werden.
