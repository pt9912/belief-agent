# Slice slice-033: Code-Agent Fixture-Fehlerverifikation

**Status:** open -> next -> in-progress -> done (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung

**Bezug:** `LH-FA-OBS-001`, `LH-FA-OBS-006`, `LH-QA-02`, `LH-QA-04`; `LH-FA-POL-006`;
`ADR-0001`, `ADR-0003`; `ARC-08`.

**Autor:** pt9912. **Datum:** 2026-07-07.

---

## 1. Ziel

Fehlerhafte Build-/Repo-Fixtures im `example/code-agent`-Pfad werden deterministisch
fail-closed behandelt und mit einer expliziten Verifikationsmatrix belegt.

## 2. Definition of Done

- [ ] `example/code-agent` behandelt fehlerhafte Fixture-Inputs hart definiert:
  `fixture_env_missing`, `fixture_missing`, `fixture_unreadable`, `fixture_empty`,
  `fixture_malformed_json`, `fixture_schema_mismatch`, `fixture_encoding_invalid`.
  Jede Klasse fuehrt zu Exit-Code `65` (`EX_DATAERR`), keinem `execute`-Aufruf,
  `executed=false`, `executor_boundary=closed`, `terminal=eskaliert` und einem
  `reason`, der die Fehlerklasse enthaelt.
- [ ] Keine impliziten Fallback-Datenquellen: fehlende oder leere
  `CODE_AGENT_BUILD_FIXTURE` / `CODE_AGENT_REPO_FIXTURE` ersetzen sich im expliziten
  Negativtest-/Runner-Pfad nicht durch Standardpfade, `FakeLlm`-Seeds oder harte
  Seed-Daten. Das direkt ausfuehrbare Runtime-Image aus `slice-032` bleibt konsistent,
  weil es nicht-leere Default-ENV fuer die im Image enthaltenen Fixtures setzt.
- [ ] Dedizierte Tests oder Verification-Runs decken M0-M5 ab:
  M0 env-missing, M1 missing, M2 unreadable/empty, M3 malformed JSON,
  M4 schema mismatch, M5 invalid encoding.
- [ ] `docs/verifications/2026-07-07-slice-033-verification.md` dokumentiert Matrix,
  Kommandos/Sensoren und Ergebnis je Fehlerklasse.
- [ ] `make example-code-agent`, `docker run --rm $(IMAGE):example-code-agent`,
  `make doc-check` und `make gates` laufen gruen; der positive Runtime-Image-Pfad
  bleibt trotz Negativfall-Handling intakt.
- [ ] Closure-Notiz mit Lerneintrag vorhanden.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `example/code-agent/src/main/kotlin/dev/beliefagent/example/codeagent/*.kt` | update | Fehlerklassen in der Demo-Eingangsvalidierung fail-closed ausgeben. |
| `example/code-agent/src/test/**` | neu/update | M0-M5 als deterministische Tests oder testbare Runner-Faelle abdecken. |
| `Makefile` / `Dockerfile` | update | Nur falls fuer negative Build-/Run-Sensoren noetig; positive Defaults und `docker run --rm $(IMAGE):example-code-agent` aus `slice-032` duerfen nicht gebrochen werden. |
| `docs/verifications/2026-07-07-slice-033-verification.md` | neu | Verification-Artefakt fuer die Negativfallmatrix. |

## 4. Trigger

`slice-032` ist in `done/` und `make example-code-agent` besitzt einen stabilen
positiven Fixture-Kontrakt.

## 5. Closure-Trigger

DoD vollstaendig + Review/Verification abgeschlossen + `make gates` gruen + Slice
nach `done/` verschoben.

## 6. Risiken und offene Punkte

- Negative Docker-Build-Faelle koennen Build-Cache und Exit-Code-Erfassung verfaelschen;
  Sensoren muessen deshalb klar zwischen positivem Target und erwarteten Fail-Runs trennen.
- Fuer `fixture_env_missing` muss der Verification-Run die in `slice-032` gebackenen
  Default-ENV bewusst umgehen oder deaktivieren; der positive `docker run --rm
  $(IMAGE):example-code-agent`-Pfad darf dadurch nicht gebrochen werden.
- `fixture_unreadable` ist in Docker-Kontexten nicht immer portabel; wenn Dateirechte
  nicht stabil abbildbar sind, muss der Verification-Report die gewaehlte Ersatzpruefung
  dokumentieren.

## 7. Closure-Notiz (nach `done/`)

**Wird nach `done/` ergaenzt.**

## 8. Sub-Area-Modus-Begründung

### Sub-Area: Beispiel-Integration (code-agent demo + Beispiele)

- **Modus:** Hybrid
- **Konventionen-Dichte:** mittel. Fail-closed Safety-Regeln sind vorhanden, konkrete Fixture-Fehlerklassen fuer diese Demo sind neu.
- **Phase-Reife:** Phase 4. Der positive Demo-Pfad ist durch `slice-032` stabil, dieser Slice schaerft nur die Fehlergrenzen.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Exit-Code und Docker-Build-Verhalten koennen auseinanderlaufen und muessen explizit verifiziert werden.
- **Reconciliation-Aufwand:** gering. Ergebnis fliesst als Verification-Artefakt und ggf. README-Ergaenzung zurueck.
