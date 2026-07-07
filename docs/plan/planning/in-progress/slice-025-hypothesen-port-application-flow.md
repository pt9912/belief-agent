# Slice slice-025: Hypothesen-Port im Application-Flow

**Status:** open → next → in-progress → done (siehe
[Planning-README](../README.md)).

**Welle:** `welle-05-llm-port` ([Roadmap](../in-progress/roadmap.md)).

**Bezug:** `LH-FA-LLM-002`, `LH-FA-LLM-003`, `LH-FA-BEL-005`,
`LH-FA-BEL-006`, `LH-FA-BEL-007`, `LH-QA-03`, `LH-QA-04`; `ADR-0003`;
`ARC-07`, `ARC-08`.

**Autor:** pt9912. **Datum:** 2026-07-07.

---

## 1. Ziel

Dieser Slice bindet die in `slice-021` gelieferte Hypothesen-Kandidatenregel an
einen application-lokalen Hypothesen-Port an, sodass hohe Resthypothese neue
oder verfeinerte Kandidaten anfordern kann, ohne Gate-, VoI- oder Aktionslogik
zu koppeln.

## 2. Definition of Done

- [ ] `LH-FA-BEL-005` ist im Anwendungsfluss angebunden: der bestehende
  domänenseitige Schwellwert löst den Hypothesen-Port nur bei hoher
  Resthypothese aus.
- [ ] `LH-FA-LLM-002` ist erfüllt: der Port ist auf Hypothesen
  erzeugen/verfeinern beschränkt und getrennt vom bestehenden Likelihood-Port,
  von Gate-/VoI-/Aktionslogik und von Adaptern.
- [ ] Gültige Kandidaten werden über die Domänenregel aus `slice-021` in einen
  neuen normierten `BeliefState` übernommen; ungültige oder leere Ergebnisse
  bleiben fail-safe und deterministisch getestet.
- [ ] `make gates` grün; Closure-Notiz mit Steering-Loop-Eintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/aktualisieren/ports/HypothesenPort.kt` | neu | Lokaler Outbound-Port für hypothesenbezogene LLM-Aufgaben |
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/aktualisieren/BeliefAktualisieren.kt` | update | Re-Hypothesen-Auslöser führt optional an den neuen Port |
| `hexagon/application/src/commonTest/...` | neu / update | Trigger, Nicht-Trigger, leere Kandidaten und ungültige Kandidaten deterministisch prüfen |
| `spec/architecture.md` | update, falls öffentlicher Architekturvertrag betroffen | Port-Verantwortung tracebar halten, ohne Wellen-/Slice-Bezug in Architektur |

## 4. Trigger

`slice-021` ist abgeschlossen und liefert Kandidatenvertrag samt
Übernahme-Regel.

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz + Slice in `done/`.

## 6. Risiken und offene Punkte

- Gefahr: Der bestehende `LlmPort` für Likelihoods wird mit
  Hypothesen-Erzeugung vermischt; der neue Port bleibt getrennt und lokal im
  Use-Case.
- Gefahr: Re-Hypothesen werden zu früh oder mehrfach im Update-Ablauf
  ausgelöst; Tests müssen Trigger und Nicht-Trigger absichern.
- Offen: Ob die Architektur textlich geschärft werden muss, entscheidet die
  Implementierung. `spec/architecture.md` darf dabei keine Slice- oder
  Wellen-Historie aufnehmen.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** TODO.

**Was ist offen geblieben:** TODO.

**Steering-Loop:** TODO.

**Folge-Slices:** `slice-026` (`llm-hypothesen-fake` Adapter).

## 8. Sub-Area-Modus-Begründung

### Sub-Area: `hexagon/application/belief-aktualisieren`

- **Modus:** Hybrid
- **Konventionen-Dichte:** Hoch für Layering und lokale Ports (`ARC-07`,
  `ADR-0003`), mittel für den konkreten Re-Hypothesen-Pfad: der bestehende Use
  Case nutzt bereits `LlmPort` für Likelihoods, aber noch keinen getrennten Port
  für Hypothesen-Erzeugung/Verfeinerung.
- **Phase-Reife:** Phase 4 für die bestehende Update-Pipeline, Phase 3 für die
  neue Port-Verflechtung. Die Architektur sagt „Ports so lokal wie möglich",
  der neue Auslöser muss aber ohne Adapter-Import und ohne Gate-/VoI-Kopplung in
  den Use Case.
- **Evidenz-/Diskrepanz-Risiko:** Mittel. Die Änderung kann unabsichtlich den
  bestehenden Likelihood-Port mit Hypothesen-Erzeugung vermischen oder
  Re-Hypothesen zu früh/spät im Update-Ablauf auslösen.
- **Reconciliation-Aufwand:** Teil dieses Slice: getrennte Port-Signatur und
  Orchestrierungstests für Schwellwert-Auslösung, Nicht-Auslösung,
  Kandidaten-Übernahme und Negativfälle gegen Gate-/VoI-/Aktionskopplung.
  Graduation-Trigger: wenn `make gates` grün bleibt und die Closure-Notiz keine
  Drift benennt, gilt der Pfad als GF-Konvention für weitere
  Hypothesen-Adapter.
