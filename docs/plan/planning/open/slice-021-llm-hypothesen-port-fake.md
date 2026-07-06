# Slice slice-021: LLM-Hypothesen-Port + Fake

**Status:** open → next → in-progress → done (siehe
[Planning-README](../README.md)).

**Welle:** `welle-05-llm-port` ([Roadmap](../in-progress/roadmap.md)).

**Bezug:** `LH-FA-LLM-002`, `LH-FA-BEL-005`, `LH-QA-03`, `LH-QA-04`; `ADR-0001`,
`ADR-0003`; `ARC-07`, `ARC-08`.

**Autor:** pt9912. **Datum:** 2026-07-06.

---

## 1. Ziel

Bei hoher Resthypothese wird heute nur zwischen vorhandenen Hypothesen operiert;
die eigentliche Hypothesen-Erzeugung/Verfeinerung über LLM bleibt offen. Dieser
Slice definiert einen dedizierten LLM-Hypothesen-Port samt Fake-Adapter, damit
`LH-FA-BEL-005` technisch umsetzbar wird, ohne die Domänenlogik zu verunreinigen.

## 2. Definition of Done

- [ ] `LH-FA-BEL-005` ist im Anwendungsfluss an den neuen Hypothesen-Port angebunden
  (Auslöser bleibt domänenseitig über den bestehenden Schwellwert).
- [ ] `LH-FA-LLM-002` erfüllt: der Port ist auf die Aufgabe „Hypothesen
  erzeugen/verfeinern“ beschränkt und entkoppelt von Gate-/VoI-/Aktionslogik.
- [ ] `LH-QA-03` erfüllt: deterministische Testfälle für Fake-Output, leere und
  inkonsistente Kandidatenfälle.
- [ ] `LH-QA-04` erfüllt: neue Schnittstelle und Tests sind strukturell/tracebar
  in der Architektur hinterlegt.
- [ ] `make gates` grün.
- [ ] Doku-Update, falls `docs/user/integration.md` den Hypothesen-Flow für
  Integratoren ergänzt.
- [ ] Closure-Notiz mit Steering-Loop-Eintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/aktualisieren/ports/HypothesenPort.kt` | neu | Vertrag für hypothesenbezogene LLM-Aufgaben |
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/aktualisieren/BeliefAktualisieren.kt` | update | Re-Hypothesen-Auslöser führt optional an Port |
| `adapters/outbound/llm-hypothesen-fake/src/...` | neu | Deterministischer Fake für reproduzierbare Revision |
| `adapters/outbound/llm-hypothesen-fake` (Build/Settings) | neu | Adapter-Modul als eigene, port-spezifische Quelle |
| `hexagon/application/src/commonTest/...` | update | Test der Orchestrierung bei `LH-FA-BEL-005` |
| `adapters/outbound/llm-hypothesen-fake/src/commonTest/...` | neu | Fail-safe-Fälle (unbekannte Hypothesen/NaN/negativ) |

## 4. Trigger

Abschluss von `slice-020` **und** `slice-019`.
Alternativ startet der Slice frühestens, wenn in der Welle ein beobachtbares,
veröffentlichtes Spezifikations-Kriterium für entkoppelte
Re-Hypothesen-Erzeugung vorliegt.

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz + Slice in `done/`.

## 6. Risiken und offene Punkte

- Gefahr: Hypothesen-Erweiterung drängt in Domänenlogik — klare Grenzlinie auf
  `ARC-08`/`ADR-0003` nötig.
- Gefahr: Unbounded Hypothesen-Erweiterung kann Prior-Verteilung bremsen; Caps für
  Neuankunft/Änderung definieren.
- Gefahr: Fake kann echte Pipeline-Anforderungen maskieren; Fake-Daten müssen
  als solche gekennzeichnet bleiben.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** TODO.

**Was ist offen geblieben:** TODO.

**Steering-Loop:** TODO.

**Folge-Slices:** bei Kalibrierungsbedarf an `slice-022` anschließen.

## 8. Sub-Area-Modus-Begründung

Neue Sub-Areas:

- `hexagon/application/aktualisieren` — BF/Hybrid: neue Use-Case-Verflechtung im
  Aktualisieren erfordert zusätzliche Port-Disziplin.
- `adapters/outbound/llm-hypothesen-fake` — GF: testbarer Fake für die neue
  Abhängigkeit.
