# Verification — slice-033 Code-Agent Fixture-Fehlerverifikation

**Datum:** 2026-07-07
**Scope:** M0-M5 Fixture-Negativmatrix fuer `example/code-agent` plus positiver Runtime-Image-Pfad.
**Bezug:** `LH-FA-OBS-001`, `LH-FA-OBS-006`, `LH-QA-02`, `LH-QA-04`, `LH-FA-POL-006`, `ADR-0001`, `ADR-0003`, `ARC-08`.

## DoD-Verifikation

| DoD | Ergebnis |
|---|---|
| Fehlerhafte Fixtures liefern definierte Klassen und Exit-Code 65 | Erfuellt durch `CodeAgentFixtureFehlerTest` und erwartete `runCodeAgent`-Rueckgabe `65`. |
| Keine impliziten Fallbacks im Negativpfad | Erfuellt: Tests pruefen fehlende und leere `CODE_AGENT_BUILD_FIXTURE`/`CODE_AGENT_REPO_FIXTURE` explizit und erwarten `fixture_env_missing`. |
| M0-M5 abgedeckt | Erfuellt: Tests fuer env missing, missing file, unreadable, empty, malformed fixture, schema mismatch, invalid encoding. |
| Verification-Artefakt vorhanden | Erfuellt: dieses Dokument. |
| Positive Sensoren gruen | Erfuellt nach finalem Sensorlauf: `make example-code-agent-run`, `make doc-check`, `make gates`. |
| Closure-Notiz vorhanden | Erfuellt im Slice-Dokument nach finalem `done/`-Move. |

## Negativmatrix

| Matrix | Fixture-Fall | Erwartung | Sensor |
|---|---|---|---|
| M0a | fehlende `CODE_AGENT_BUILD_FIXTURE` | `65`, `fixture_env_missing`, `terminal=eskaliert`, `executed=false`, `executor_boundary=closed` | `CodeAgentFixtureFehlerTest.m0_build_env_missing_faellt_fail_closed_ohne_execute` |
| M0b | fehlende `CODE_AGENT_REPO_FIXTURE` | `65`, `fixture_env_missing`, `terminal=eskaliert`, `executed=false`, `executor_boundary=closed` | `CodeAgentFixtureFehlerTest.m0_repo_env_missing_faellt_fail_closed_ohne_execute` |
| M0c | leere `CODE_AGENT_BUILD_FIXTURE` | `65`, `fixture_env_missing`, `terminal=eskaliert`, `executed=false`, `executor_boundary=closed` | `CodeAgentFixtureFehlerTest.m0_build_env_empty_faellt_fail_closed_ohne_execute` |
| M0d | leere `CODE_AGENT_REPO_FIXTURE` | `65`, `fixture_env_missing`, `terminal=eskaliert`, `executed=false`, `executor_boundary=closed` | `CodeAgentFixtureFehlerTest.m0_repo_env_empty_faellt_fail_closed_ohne_execute` |
| M1a | nicht existierende Build-Fixture | `65`, `fixture_missing`, `terminal=eskaliert`, `executed=false`, `executor_boundary=closed` | `CodeAgentFixtureFehlerTest.m1_missing_faellt_fail_closed_ohne_execute` |
| M1b | nicht existierende Repo-Fixture | `65`, `fixture_missing`, `terminal=eskaliert`, `executed=false`, `executor_boundary=closed` | `CodeAgentFixtureFehlerTest.m1_repo_missing_faellt_fail_closed_ohne_execute` |
| M2a | Verzeichnis statt Datei | `65`, `fixture_unreadable`, `terminal=eskaliert`, `executed=false`, `executor_boundary=closed` | `CodeAgentFixtureFehlerTest.m2_unreadable_faellt_fail_closed_ohne_execute` |
| M2b | leere Build-Fixture | `65`, `fixture_empty`, `terminal=eskaliert`, `executed=false`, `executor_boundary=closed` | `CodeAgentFixtureFehlerTest.m2_empty_faellt_fail_closed_ohne_execute` |
| M2c | leere Repo-Fixture | `65`, `fixture_empty`, `terminal=eskaliert`, `executed=false`, `executor_boundary=closed` | `CodeAgentFixtureFehlerTest.m2_repo_empty_faellt_fail_closed_ohne_execute` |
| M3 | formal unparsebare Fixture-Zeile | `65`, `fixture_malformed_json`, `terminal=eskaliert`, `executed=false`, `executor_boundary=closed` | `CodeAgentFixtureFehlerTest.m3_malformed_json_faellt_fail_closed_ohne_execute` |
| M4 | fehlendes Pflichtfeld | `65`, `fixture_schema_mismatch`, `terminal=eskaliert`, `executed=false`, `executor_boundary=closed` | `CodeAgentFixtureFehlerTest.m4_schema_mismatch_faellt_fail_closed_ohne_execute` |
| M5 | ungueltige UTF-8-Bytes | `65`, `fixture_encoding_invalid`, `terminal=eskaliert`, `executed=false`, `executor_boundary=closed` | `CodeAgentFixtureFehlerTest.m5_invalid_encoding_faellt_fail_closed_ohne_execute` |

## Sensoren

| Kommando | Ergebnis |
|---|---|
| `make test` | gruen; enthaelt `:example:code-agent:test` in der Docker-Teststage. Initialer roter Lauf war Kotlin-Compiler-OOM; behoben durch `--max-workers=1` in der Teststage. |
| `make example-code-agent` | gruen |
| `docker run --rm belief-agent:example-code-agent` | gruen via `make example-code-agent-run`; Runtime-Ausgabe ohne Gradle-Tasks |
| `make doc-check` | gruen |
| `make gates` | gruen |

## Hinweis zu `fixture_malformed_json`

Die konkreten Adapterfixtures sind seit `slice-031`/`slice-032` `key=value`-Dateien. Der Klassenname `fixture_malformed_json` bleibt aus dem Slice-Vertrag erhalten und bezeichnet in diesem Example formal unparsebare Fixture-Syntax, nicht ein neues JSON-Format.
