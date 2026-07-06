# Slice slice-024: cli-Composition-Root + produktives E2E

**Status:** open → next → in-progress → done (siehe
[Planning-README](../README.md)).

**Welle:** `welle-05-llm-port` ([Roadmap](../in-progress/roadmap.md)).

**Bezug:** `LH-FA-LLM-002`, `LH-FA-LLM-004`, `LH-FA-POL-004`, `LH-FA-POL-006`,
`LH-QA-02`, `LH-QA-03`, `LH-QA-04`; `ADR-0001`, `ADR-0002`, `ADR-0003`;
`ARC-02`, `ARC-07`, `ARC-09`.

**Autor:** pt9912. **Datum:** 2026-07-06.

---

## 1. Ziel

Die App-Entscheidungslogik ist bisher nur test-nah verdrahtet und durch
Beispielmodule (`example:langchain`, `example:koog`) demonstriert. Dieser Slice
liefert einen produktiv gedachten `cli`-Composition-Root (`ARC-09`) mit `Koin`
(`ADR-0002`) und einem E2E-Pfad **ohne Netzabhängigkeit** (Fake-Adapter für
LLM/Ports, aber reale Komposition wie im Produktivbetrieb).

Danach bleibt die Verhaltenslogik im Kern unverändert, aber die Laufzeitverdrahtung
wird vollständig durchgängig und wiederholbar.

## 2. Definition of Done

- [ ] `make doc-check` + `make gates` grün inkl. neuer `ARC-09`-Inbound-Verbindung.
- [ ] Produktiver `cli`-Entrypoint startet und baut `Entscheidungszyklus` mit
  allen notwendigen Ports (`BeobachtungsAuswahl`, `Beobachtung`, `LlmPort`,
  `Audit`, `HumanApproval`).
- [ ] Produktive Verdrahtung verhindert indirekte/fehlerhafte Ausführungspfade:
  Ausführungsadapter werden nur dann angestoßen, wenn das Ergebnis des
  `Entscheidungszyklus`/`AktionGaten` `Aktionsfreigabe.Freigegeben` ist
  (a-check-Regel/Fail-safe).
- [ ] `slice-023` (Aktions-Vorschlag), `slice-020` (belief-abhängige
  Beobachtung), `slice-021` (Hypothesen-Port), `slice-022`
  (Konfidenz-Externalisierung) werden in der CLI-Weg-Kette konsistent konsumiert.
- [ ] Netzfrei testbares E2E: Entscheidungsweg `sammeln | handeln | eskalieren`
  im CLI-Root mit deterministischen Fake-Adaptern inklusive einem
  `Eskalation`-Pfad.
- [ ] `docs/user/integration.md` aktualisiert: Produktiv-Composition-Root als
  Integrationssprungstelle dokumentiert.
- [ ] Closure-Notiz mit Steering-Loop-Eintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `example/cli` (neu) | neu | CLI-Eingangspunkt-Modul (`jvmMain`), Koin-Composition-Root (`ARC-09`, `ADR-0002`). |
| `example/cli/src/main/kotlin/.../Runtime.kt` | neu | Zentraler Aufbau der Ports + Use-Case-Wiring. |
| `example/cli/src/test/kotlin/.../CliRuntimeE2eTest.kt` | neu | E2E ohne Netz: Fake-Adapter, Entscheidungstypen prüfen (`LH-QA-03`). |
| `example/cli/build.gradle.kts` | neu | Koin + Projektabhängigkeiten (`langchain4j`, `koog`, Fake-Ports, core). |
| `settings.gradle.kts` | update | neues Inbound-Module (`example:cli`) einhängen. |
| `Dockerfile` | update | optionaler CLI-Build-Stage / Laufzeitartefakt-Option vorbereiten. |
| `.a-check.yml` | update | Root-Aufnahme `example/cli`, `tech` ggf. für DI-Boundary-Verifikation. |
| `docs/user/integration.md` | update | produktiver Composition-Root als Integrations-API dokumentieren. |

## 4. Trigger

`slice-019` in `done/` (`LlmPort` mit realen Framework-Adaptern vorhanden) und
`slice-020` in `done/`.
`slice-021` bis `slice-023` müssen innerhalb von `welle-05` priorisiert
vorliegen (Dokumentation/Order der Ausführung in der Welle festgelegt).

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz + Slice in `done/`.

## 6. Risiken und offene Punkte

- CLI kann schnell produktive Parameter-Tuning-Muster einführen; klare
  Konfigurationsgrenzen (Model/URL/Timeout) sind Pflicht.
- Falsche Adapterwahl im Root kann `a-check`-Lecks erzeugen; neue DI-Regelung soll
  im Tech-Raster hart bleiben.
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

- `example/cli` — GF (neuer Inbound/Runtime-Bereich, Spezifikation zuerst, reine
  Verdrahtung und Laufzeitkonfiguration).
