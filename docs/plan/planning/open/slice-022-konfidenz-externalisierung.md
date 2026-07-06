# Slice slice-022: Konfidenz-Externalisierung

**Status:** open → next → in-progress → done (siehe
[Planning-README](../README.md)).

**Welle:** `welle-05-llm-port` ([Roadmap](../in-progress/roadmap.md)).

**Bezug:** `LH-FA-LLM-003`, `LH-QA-04`; `ADR-0001`, `ADR-0003`; `ARC-08`, `ARC-09`.

**Autor:** pt9912. **Datum:** 2026-07-06.

---

## 1. Ziel

Modell-Kennzahlen aus LLM-Interaktionen sollen nicht implizit im Entscheidungsfluss
verweilen, sondern als explizite, protokollierte und überschreibbare Konfidenz
vorliegen (Golden-Set-fähig). Dieser Slice führt eine Konfidenz-Externalisierung
ein (Datensatz + Persistenzschnittstelle + Evaluations-Hook), ohne Entscheidungslogik
zu verwischen.

## 2. Definition of Done

- [ ] `LH-FA-LLM-003` erfüllt: rohe LLM-Confidence wird in explizite,
  protokollierbare Konfidenz-Strukturen überführt und an den `Entscheidungszyklus`/
  Gate-Pfad (nicht an `AktionGaten` direkt) gebunden.
- [ ] Konfidenz-Werte sind überschreibbar/reproduzierbar (Golden-Set-Pfad + feste
  Fixtures).
- [ ] `LH-QA-04` erfüllt: Konfidenz-Contract wird in der Architektur als klarer Port
  und Mapping dokumentiert und geprüft.
- [ ] mindestens ein Golden-Set-Test / Replay-Prüfpfad existiert (deterministisch).
- [ ] `make gates` grün.
- [ ] Closure-Notiz mit Steering-Loop-Eintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/application/src/commonMain/.../KonfidenzExtern` | neu | Explizites Modell-Konfidenzmodell + Übersetzungslogik |
| `hexagon/application/src/commonMain/.../ports/KonfidenzPort.kt` | neu | Vertrag zur Externalisierung (`ARC-08`) |
| `adapters/outbound/konfidenz-memory` / `…/konfidenz-fake` | neu | Persistenz/Replay-Pfad für Fixtures |
| `hexagon/application/src/commonTest/...` | neu | Golden-Set-/Replay-Tests |
| `docs/user/integration.md` | update | Integrationskontext für Konfidenz-Override dokumentieren |
| `docs/plan/planning/in-progress/roadmap.md` | update | Wellen-`Closure` um den Golden-Set-Pfad aus `welle-05` nachziehen |

## 4. Trigger

Abschluss von `slice-021` (LLM-Hypothesen-Port) oder klares Follow-up-Need aus
LLM-Adapter-Rückläufen zur Reproduzierbarkeit.

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz + Slice in `done/`.

## 6. Risiken und offene Punkte

- Goldendaten können zu einem falschen Sicherheitsgefühl führen; Metriken müssen
  explizit als Modell-/Datensatz-abhängig geführt werden.
- Überschreibbarkeit braucht klare Governance, sonst verwischt die
  Verantwortung zwischen LLM-Port und Anwendungslogik.
- Unzureichendes Failure-Diagnose-Logging (fehlende Trace-IDs) erschwert Replay.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** TODO.

**Was ist offen geblieben:** TODO.

**Steering-Loop:** TODO.

**Folge-Slices:** E2E-Validierung in `slice-024`.

## 8. Sub-Area-Modus-Begründung

Neue Sub-Areas:

- `hexagon/application` (Konfidenz-Contract + DTO) — BF/Hybrid: neue
  Verträge im Kern erfordern präzise Traceability.
- `adapters/outbound` (Golden-Set-/Replay-Adapter) — GF: deterministische
  Infrastruktur vor Produktivbetrieb.
