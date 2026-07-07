# Slice slice-026: LLM-Hypothesen-Fake-Adapter

**Status:** open → next → in-progress → done (siehe
[Planning-README](../README.md)).

**Welle:** `welle-05-llm-port` ([Roadmap](../in-progress/roadmap.md)).

**Bezug:** `LH-FA-LLM-002`, `LH-FA-LLM-003`, `LH-FA-BEL-006`,
`LH-FA-BEL-007`, `LH-QA-03`, `LH-QA-04`; `ADR-0003`; `ARC-08`.

**Autor:** pt9912. **Datum:** 2026-07-07.

---

## 1. Ziel

Dieser Slice liefert den deterministischen Fake-Adapter hinter dem
application-lokalen Hypothesen-Port aus `slice-025` und nimmt das neue
Adapter-Modul in Build, Architekturprüfung und Tests auf.

## 2. Definition of Done

- [x] `adapters/outbound/llm-hypothesen-fake` implementiert den
  Hypothesen-Port deterministisch und liefert klar als Fake erkennbare
  Kandidaten.
- [x] `LH-FA-LLM-003` bleibt abgesichert: Fake-Daten enthalten explizite Scores
  und Evidenzreferenzen; ungültige Konfigurationen werden fail-safe getestet.
- [x] Build- und Architekturkonfigurationen nehmen das neue Adapter-Modul auf,
  ohne Domain/Application-Abhängigkeiten auf Adapter zu erzeugen.
- [x] `make gates` grün; Closure-Notiz mit Steering-Loop-Eintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `adapters/outbound/llm-hypothesen-fake/src/...` | neu | Deterministischer Fake für reproduzierbare Revision |
| `adapters/outbound/llm-hypothesen-fake/build.gradle.kts` | neu | Neues Adapter-Modul in den KMP-Build aufnehmen |
| `settings.gradle.kts` / Build-Container | update | Modul im reproduzierbaren Build sichtbar machen |
| `.a-check.yml` | update | Neuen Adapter-Root in die Architekturprüfung aufnehmen (`ARC-08`) |
| `adapters/outbound/llm-hypothesen-fake/src/commonTest/...` | neu | Fake-Output, leere Kandidaten, fehlende Evidenz und ungültige Scores prüfen |

## 4. Trigger

`slice-025` ist abgeschlossen und der application-lokale Hypothesen-Port steht
stabil.

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz + Slice in `done/`.

## 6. Risiken und offene Punkte

- Gefahr: Fake-Qualität wird mit Modellqualität verwechselt; der Adapter bleibt
  deterministisch und klar als Fake gekennzeichnet.
- Gefahr: Build- oder Architekturkonfiguration blendet das neue Modul aus;
  `arch-check` und `make gates` sind explizite DoD-Kriterien.
- Offen: Echte Provider-Anforderungen entstehen nicht in diesem Slice, sondern
  in späteren Adapter-Slices.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** Der Adapter-Schnitt blieb klein: rohe
Fake-Konfiguration wird erst im Adapter in Domain-Werte gemappt, wodurch
ungueltige Scores, leere Evidenz und nicht als Fake markierte IDs fail-safe zu
`emptyList()` werden.

**Was ist offen geblieben:** Echte Hypothesen-Provider, Prompt-/Schema-Vertrag
und produktive Provider-Konfiguration bleiben bewusst ausserhalb dieses Slices.

**Steering-Loop:** Die DoD-Formulierung "klar als Fake erkennbar" wurde beim
Review in eine pruefbare Adapter-Guard uebersetzt (`fake-`/`fake:`), statt nur
Default-Testdaten so zu benennen.

**Folge-Slices:** echte Hypothesen-Provider nur nach separatem Planning-Slice.

## 8. Sub-Area-Modus-Begründung

### Sub-Area: `adapters/outbound/llm-hypothesen-fake`

- **Modus:** GF
- **Konventionen-Dichte:** Hoch. Vorbilder sind die bestehenden
  port-spezifischen Fake-Adapter (`llm-fake`, `voi-fake`) hinter
  application-lokalen Ports; Layering wird über `ADR-0003`/`ARC-08` und
  `arch-check` abgesichert.
- **Phase-Reife:** Phase 3. Der Adapter wird neu aus dem Port-Vertrag heraus
  gebaut; Spezifikation und Architektur führen, Code folgt.
- **Evidenz-/Diskrepanz-Risiko:** Niedrig. Keine Alt-Implementierung; Risiko
  liegt vor allem darin, Fake-Qualität mit Modellqualität zu verwechseln.
- **Reconciliation-Aufwand:** Keiner im Slice beyond Implementierung und Tests.
  Graduation-Trigger: bleibt GF, solange der Fake deterministisch bleibt und
  echte Provider-Anforderungen in separaten Adapter-Slices landen.
