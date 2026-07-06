# Slice slice-024: cli-Composition-Root + produktives E2E

**Status:** open → next → in-progress → done (siehe
[Planning-README](../README.md)).

**Welle:** Roadmap-Follow-up zu `welle-05-llm-port`
([Roadmap](../in-progress/roadmap.md)); bei aktiver Umsetzung Welle explizit
wieder öffnen oder als gezielten Follow-up-Slice starten.

**Bezug:** `LH-FA-LLM-002`, `LH-FA-LLM-004`, `LH-FA-POL-004`, `LH-FA-POL-006`,
`LH-OUT-04`, `LH-QA-02`, `LH-QA-03`, `LH-QA-04`; `ADR-0001`, `ADR-0002`,
`ADR-0003`; `ARC-07`, `ARC-08`, `ARC-09`.

**Autor:** pt9912. **Datum:** 2026-07-06.

---

## 1. Ziel

Die App-Entscheidungslogik ist bisher nur test-nah verdrahtet und durch
Beispielmodule (`example:langchain`, `example:koog`) demonstriert. Dieser Slice
liefert einen produktiv gedachten `adapters/inbound/cli`-Composition-Root
(`ARC-09`) mit `Koin` (`ADR-0002`) und einem E2E-Pfad **ohne Netzabhängigkeit**
(Fake-Adapter für LLM/Ports, aber reale Komposition wie im Produktivbetrieb).

Danach bleibt die Verhaltenslogik im Kern unverändert, aber die Laufzeitverdrahtung
wird vollständig durchgängig und wiederholbar.

## 2. Definition of Done

- [ ] `make doc-check` + `make gates` grün inkl. neuer `ARC-09`-Inbound-Verbindung.
- [ ] Architekturregel für den Composition Root ist maschinell belastbar:
  entweder `a-check` trennt `adapters/inbound/cli`/Composition-Wiring von
  übrigen Outbound-Adaptern inkl. Negativ-Guard gegen fachliche
  Adapter-zu-Adapter-Kopplung, oder der Slice wird vor Code auf ein separates
  Runtime-/Composition-Modul re-geschnitten.
- [ ] Produktiver `cli`-Entrypoint startet und baut `Entscheidungszyklus` mit
  allen notwendigen Ports (`BeobachtungsAuswahl`, `Beobachtung`, `LlmPort`,
  `Audit`, `HumanApproval`) sowie einer Ausführungsgrenze/Fake-Execution-Adapter.
- [ ] Produktive Verdrahtung verhindert indirekte/fehlerhafte Ausführungspfade:
  Ausführungsadapter werden nur dann angestoßen, wenn das Ergebnis des
  `Entscheidungszyklus` `Zyklusergebnis.Gehandelt` ist und der Executor die
  darin enthaltene `Aktionsfreigabe.Freigegeben` konsumiert
  (`LH-FA-POL-006`, `LH-OUT-04`).
- [ ] Positiver Executor-Contract-Test beweist: `Zyklusergebnis.Gehandelt`
  führt genau einmal über `freigabe.aktion` aus; es gibt keinen
  `execute(aktion)`-/`GateEntscheidung.Freigabe`-Bypass.
- [ ] Negative E2E-/Contract-Tests beweisen fail-closed Executor-Verhalten:
  bei `Ablehnung`, `Eskalation` und fehlender menschlicher Freigabe wird kein
  Ausführungsadapter aufgerufen; `a-check` deckt nur Boundary-/Importregeln ab.
- [ ] `slice-023` (Aktions-Vorschlag), `slice-020` (belief-abhängige
  Beobachtung), `slice-021` (Hypothesen-Port), `slice-022`
  (Konfidenz-Externalisierung) werden in der CLI-Weg-Kette konsistent konsumiert.
- [ ] Netzfrei testbares E2E: CLI-sichtbare Terminalergebnisse
  `Gehandelt`, `Eskaliert` und `Abgelehnt` mit deterministischen Fake-Adaptern;
  mindestens ein Fall läuft zuvor durch einen Sammel-Schritt
  (`BeobachtungWaehlen` → `BeliefAktualisieren` → erneutes Gate).
- [ ] `docs/user/integration.md` aktualisiert: Produktiv-Composition-Root als
  Integrationssprungstelle dokumentiert.
- [ ] Closure-Notiz mit Steering-Loop-Eintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `adapters/inbound/cli` (neu) | neu | CLI-Eingangspunkt-Modul (`jvmMain`), Koin-Composition-Root (`ARC-09`, `ADR-0002`). |
| `adapters/inbound/cli/src/main/kotlin/.../Runtime.kt` | neu | Zentraler Aufbau der Ports + Use-Case-Wiring. |
| `adapters/inbound/cli/src/main/kotlin/.../Executor.kt` | neu | Ausführungsgrenze: führt nur `Aktionsfreigabe.Freigegeben` aus, sonst fail-closed. |
| `adapters/inbound/cli/src/test/kotlin/.../CliRuntimeE2eTest.kt` | neu | E2E ohne Netz: Fake-Adapter, Entscheidungstypen und Executor-Negativpfade prüfen (`LH-QA-03`). |
| `adapters/inbound/cli/build.gradle.kts` | neu | Koin + Core-/Port-Abhängigkeiten; Outbound-/Fake-Adapter nur über die in diesem Slice definierte Composition-Root-Regel. |
| `settings.gradle.kts` | update | neues Inbound-Module (`adapters:inbound:cli`) einhängen. |
| `Dockerfile` | update | optionaler CLI-Build-Stage / Laufzeitartefakt-Option vorbereiten. |
| `.a-check.yml` | update | Root-Aufnahme `adapters/inbound/cli`; Rollen so schneiden, dass `cli`/Composition-Wiring nur für DI-Bindings ausgewählte Outbound-Adapter an Ports binden darf. Negativ-Guard beweist, dass fachliche Adapter-zu-Adapter-Kopplung weiter rot ist; falls a-check das nicht ausdrücken kann, vor Code re-schneiden. |
| `spec/architecture.md` | update | `ARC-09`/`ARC-08` reconciliieren: `adapters/inbound/cli` bleibt Inbound-Entrypoint und ist zugleich der einzige Composition Root, der ausgewählte Outbound-Adapter an Ports binden darf; falls a-check diese Sonderrolle nicht ausdrücken kann, vor Code auf separates Runtime-/Composition-Modul re-schneiden. |
| `docs/user/integration.md` | update | produktiver Composition-Root als Integrations-API dokumentieren. |

## 4. Trigger

`slice-019` bis `slice-023` in `done/`. `slice-020` bis `slice-023` sind harte
Voraussetzungen, weil dieser Slice deren Contracts in der CLI-Weg-Kette
konsumiert (belief-abhängige Beobachtung, Hypothesen-Port,
Konfidenz-Externalisierung, Aktions-Vorschlag).

Falls keine Welle aktiv ist, wird dieser Slice als gezielter Follow-up-Slice
gestartet oder `welle-05-llm-port` explizit wieder geöffnet.

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz + Slice in `done/`.

## 6. Risiken und offene Punkte

- CLI kann schnell produktive Parameter-Tuning-Muster einführen; klare
  Konfigurationsgrenzen (Model/URL/Timeout) sind Pflicht.
- Falsche Adapterwahl im Root kann `a-check`-Lecks erzeugen; neue DI-Regelung soll
  im Tech-Raster hart bleiben.
- Inbound-CLI und Outbound-Adapter-Wiring stehen unter Architekturspannung:
  Zielzustand dieses Slice ist eine explizite, eng begrenzte
  Composition-Root-Sonderrolle für `adapters/inbound/cli`: Outbound-Adapter dürfen
  dort nur an Ports gebunden werden; fachliche Adapter-zu-Adapter-Kopplung bleibt
  verboten. Wenn Architektur + a-check diese Sonderrolle nicht belastbar abbilden,
  wird vor Code auf ein separates Runtime-/Composition-Modul re-geschnitten.
- `sammeln` ist im aktuellen `ARC-09`-Modell ein interner Loop-Schritt, kein
  terminales `Zyklusergebnis`; CLI-/E2E-Tests müssen Terminalergebnisse prüfen
  und den Sammel-Pfad nur als durchlaufenen Zwischenschritt absichern.
- Executor-Safety ist Laufzeitverhalten, keine reine Importregel; fehlende
  Negativtests würden `LH-FA-POL-006`/`LH-OUT-04` nicht belastbar absichern.
- E2E ohne Netz darf nicht als „Produktivmodus“ missverstanden werden; klarer
  Übergang zu externen Bindungen im späteren Stabilisierungsschritt.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** TODO.

**Was ist offen geblieben:** TODO.

**Steering-Loop:** TODO.

**Folge-Slices:** Echte `HumanApproval`- und Ausführungsadapter-Bindung sowie
Persistenz-Adapter
  (Ausbau des CLI-Bundles ohne Mock).

## 8. Sub-Area-Modus-Begründung

Neue Sub-Area:

- `adapters/inbound/cli` — GF (neuer Inbound/Runtime-Bereich, Spezifikation
  zuerst, Verdrahtung, Executor-Grenze und Laufzeitkonfiguration).
