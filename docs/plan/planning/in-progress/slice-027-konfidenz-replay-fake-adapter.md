# Slice slice-027: Konfidenz-Replay-Fake-Adapter

**Status:** open → next → in-progress → done (siehe
[Planning-README](../README.md)).

**Welle:** Roadmap-Follow-up zu `welle-05-llm-port`
([Roadmap](../in-progress/roadmap.md)); gezielter Follow-up nach
`slice-022`.

**Bezug:** `LH-FA-LLM-003`, `LH-FA-AUD-001`, `LH-FA-AUD-003`,
`LH-QA-03`, `LH-QA-04`; `ADR-0001`, `ADR-0003`, `ADR-0006`; `ARC-06`,
`ARC-08`.

**Autor:** pt9912. **Datum:** 2026-07-07.

---

## 1. Ziel

Dieser Slice liefert den deterministischen Speicher-/Replay-Adapter hinter dem
in `slice-022` eingeführten Konfidenz-Contract, damit Golden-Set-Fixtures
netzfrei und reproduzierbar gegen externalisierte Modell-Konfidenz laufen.

## 2. Definition of Done

- [x] `adapters/outbound/konfidenz-memory` implementiert den Konfidenz-Port
  deterministisch und append-only; vorhandene Einträge werden nicht mutiert.
- [x] Golden-Set-/Replay-Fixtures können feste externalisierte Konfidenzen und
  Overrides reproduzierbar laden; kaputte Fixtures führen fail-safe zu keiner
  gate-fähigen Konfidenz.
- [x] Build-, Coverage- und Architekturkonfiguration nehmen das neue
  Adapter-Modul explizit auf (`ADR-0006`, `ARC-08`), ohne Kern-Abhängigkeiten
  auf Adapter zu erzeugen.
- [x] `make gates` grün.
- [x] Closure-Notiz mit Steering-Loop-Eintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `adapters/outbound/konfidenz-memory/build.gradle.kts` | neu | Eigenes Adapter-Modul mit Kover-Gate wie bestehende Fake-/Memory-Adapter |
| `adapters/outbound/konfidenz-memory/src/commonMain/...` | neu | Deterministischer In-Memory-/Fixture-Adapter hinter dem Konfidenz-Port |
| `adapters/outbound/konfidenz-memory/src/commonTest/...` | neu | Append-only, Replay, kaputte Fixtures und Determinismus prüfen |
| `settings.gradle.kts` / `Dockerfile` | update | Modul in Build, Test, Coverage und Dependency-Resolve sichtbar machen |
| `.a-check.yml` | update | Adapter-Root in die Architekturprüfung aufnehmen |

## 4. Trigger

`slice-022` liegt in `done/` und liefert den Business-Area-Konfidenz-Contract
samt Port-Typen.

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz + Slice in `done/`.

## 6. Risiken und offene Punkte

- Fixture-Replay darf nicht als Modellqualität missverstanden werden; der
  Adapter bleibt als Memory/Fake gekennzeichnet.
- Adapter darf keine Gate-Entscheidung treffen; er speichert und liefert nur
  externalisierte Konfidenzwerte.
- Build-/Coverage-/Arch-Listen sind explizit und können das neue Modul
  versehentlich ausblenden; `make gates` ist deshalb DoD.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** Der vorhandene `KonfidenzPort` aus `slice-022` war
schmal genug, um einen reinen Memory-/Replay-Adapter ohne neue Application-
Schnittstelle zu liefern. Die expliziten Build-/Coverage-/a-check-Listen haben
das neue Modul sichtbar in alle Gates gezogen.

**Was ist offen geblieben:** Der Adapter trifft bewusst keine Gate-
Entscheidung und bindet die Konfidenz noch nicht an den Entscheidungszyklus.
Diese Laufzeitbindung bleibt in `slice-028`.

**Steering-Loop:** Bei weiteren Fake-/Replay-Adaptern sollte die Versionsfolge
aus dem Port-Vertrag direkt im Adapter getestet werden; kaputte Fixtures
muessen fail-safe leer bleiben, nicht teilweise geladen werden.

**Folge-Slices:** `slice-028` bindet den Contract an Entscheidungszyklus/Gate-Pfad.

## 8. Sub-Area-Modus-Begründung

### Sub-Area: `adapters/outbound/konfidenz-memory`

- **Modus:** GF
- **Konventionen-Dichte:** Hoch. Bestehende Adapter (`audit-memory`,
  `llm-hypothesen-fake`, `voi-fake`) geben KMP-Modul, Test-Layout,
  Kover-Block und a-check-Root vor.
- **Phase-Reife:** Phase 3. Der Adapter entsteht neu aus dem in `slice-022`
  gelieferten Port-Vertrag; Code folgt Spec/Architektur.
- **Evidenz-/Diskrepanz-Risiko:** Niedrig. Keine Alt-Implementierung; Risiko
  liegt in unvollständiger Build-/Arch-Integration.
- **Reconciliation-Aufwand:** Keiner beyond Modulverdrahtung und Tests.
  Graduation-Trigger: `make gates` grün mit Adapter im Scope.
