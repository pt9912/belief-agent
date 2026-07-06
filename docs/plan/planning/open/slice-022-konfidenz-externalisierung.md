# Slice slice-022: Konfidenz-Externalisierung

**Status:** open → next → in-progress → done (siehe
[Planning-README](../README.md)).

**Welle:** Roadmap-Follow-up zu `welle-05-llm-port`
([Roadmap](../in-progress/roadmap.md)); bei aktiver Umsetzung Welle explizit
wieder öffnen oder als gezielten Follow-up-Slice starten.

**Bezug:** `LH-FA-LLM-003`, `LH-FA-AUD-001`, `LH-FA-AUD-003`,
`LH-QA-04`; `ADR-0001`, `ADR-0003`; `ARC-06`, `ARC-08`, `ARC-09`.

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
  Fixtures); jedes Override erzeugt ein neues Audit-Ereignis und mutiert keinen
  bestehenden Eintrag (`LH-FA-AUD-001`, `LH-FA-AUD-003`).
- [ ] `LH-QA-04` erfüllt: Konfidenz-Contract wird in der Architektur als klarer Port
  und Mapping dokumentiert und geprüft.
- [ ] mindestens ein Golden-Set-Test / Replay-Prüfpfad existiert (deterministisch).
- [ ] `make gates` grün.
- [ ] Closure-Notiz mit Steering-Loop-Eintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/konfidenzexternalisieren/KonfidenzExtern.kt` | neu | Use-Case für Externalisierung und Übersetzungslogik; konsumiert den gemeinsamen Konfidenz-Contract |
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/ports/KonfidenzPort.kt` | neu | Business-Area-geteilter Vertrag zur Externalisierung (`ARC-07`/`ARC-08`), weil `slice-023` denselben Konfidenz-Contract für Aktionsvorschläge konsumiert |
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/ports/ExternalisierteKonfidenz.kt` | neu | Gemeinsame Contract-Typen/Value-Types für externalisierte Modell-Konfidenz; nicht unter einem lokalen Use-Case-Pfad, damit `slice-023` keine fremden lokalen DTOs importiert |
| `adapters/outbound/konfidenz-memory` / `…/konfidenz-fake` | neu | Persistenz/Replay-Pfad für Fixtures |
| `hexagon/application/src/commonTest/...` | neu | Golden-Set-/Replay-Tests |
| `spec/architecture.md` | update | `ARC-06`/`ARC-08`/`ARC-09`-Mapping für Konfidenz-Port, Audit-Ereignis und Zyklusbindung dokumentieren |
| `docs/user/integration.md` | update | Integrationskontext für Konfidenz-Override dokumentieren |
| `docs/plan/planning/in-progress/roadmap.md` | update | Wellen-`Closure` um den Golden-Set-Pfad aus `welle-05` nachziehen |

## 4. Trigger

Roadmap-Follow-up aus `welle-05-llm-port` offen + Abschluss von `slice-021`
(LLM-Hypothesen-Port) oder klares Follow-up-Need aus LLM-Adapter-Rückläufen
zur Reproduzierbarkeit. Falls keine Welle aktiv ist, wird dieser Slice als
gezielter Follow-up-Slice gestartet oder `welle-05-llm-port` explizit wieder
geöffnet.

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz + Slice in `done/`.

## 6. Risiken und offene Punkte

- Goldendaten können zu einem falschen Sicherheitsgefühl führen; Metriken müssen
  explizit als Modell-/Datensatz-abhängig geführt werden.
- Überschreibbarkeit braucht klare Governance, sonst verwischt die
  Verantwortung zwischen LLM-Port und Anwendungslogik.
- Override ohne neues Audit-Ereignis würde `LH-FA-LLM-003` verletzen und die
  Entscheidungsspur nachträglich verändern.
- Unzureichendes Failure-Diagnose-Logging (fehlende Trace-IDs) erschwert Replay.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** TODO.

**Was ist offen geblieben:** TODO.

**Steering-Loop:** TODO.

**Folge-Slices:** E2E-Validierung in `slice-024`.

## 8. Sub-Area-Modus-Begründung

Berührte Sub-Areas:

- `hexagon/application/belief/konfidenz-externalisieren` — Hybrid: neuer
  Contract/DTO und Übersetzungslogik im bestehenden Application-Core; Bindung
  an `entscheidungszyklus` und Audit-Port muss regressionssicher bleiben.
- `hexagon/application/belief/ports` — BF/Hybrid: bestehender Audit-Port wird
  angebunden; der neue `KonfidenzPort` wird als Business-Area-geteilter Vertrag
  angelegt, weil `slice-023` ihn als zweiten Use Case konsumiert. Die zugehörigen
  Contract-Typen liegen ebenfalls hier, nicht unter dem lokalen
  `konfidenzexternalisieren`-Use-Case. Importregeln aus `ARC-06`/`ARC-07`
  bleiben maßgeblich.
- `adapters/outbound/konfidenz-memory` und `adapters/outbound/konfidenz-fake`
  — GF: neue deterministische Persistenz-/Replay-Adapter vor Produktivbetrieb.
