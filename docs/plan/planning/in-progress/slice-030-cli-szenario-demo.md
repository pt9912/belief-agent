# Slice slice-030: CLI-Szenario-Demo fuer Unsicherheitsgrenzen

**Status:** open -> next -> in-progress -> done (siehe
[Planning-README](../README.md)).

**Welle:** ohne Welle; gezielter Demo-Slice nach `slice-029`.

**Bezug:** `LH-FA-POL-001`, `LH-FA-POL-004`, `LH-FA-POL-005`,
`LH-FA-POL-006`, `LH-FA-VOI-001`, `LH-FA-ESK-001`, `LH-FA-ESK-002`,
`LH-FA-ESK-003`, `LH-QA-02`, `LH-QA-03`, `LH-OUT-04`; `ADR-0001`,
`ADR-0002`, `ADR-0003`.

**Autor:** pt9912. **Datum:** 2026-07-07.

---

## 1. Ziel

Die CLI-Demo zeigt nicht nur den positiven `gehandelt`-Pfad, sondern auch
sichtbar, dass der Agent bei unzureichender Sicherheit nicht ausfuehrt:
`eskaliert`, `abgelehnt` und `sammelt-dann-handelt` werden als deterministische
Szenarien am CLI-Composition-Root vorzeigbar.

## 2. Definition of Done

- [x] CLI akzeptiert eine Szenario-Auswahl fuer `gehandelt`, `eskaliert`,
  `abgelehnt`, `sammelt-dann-handelt` und `all`; ohne Argument bleibt
  `gehandelt` der Default.
- [x] Demo-Ausgabe zeigt pro Szenario mindestens `scenario`, `terminal`,
  `executed` und bei negativen Pfaden einen beobachtbaren Grund.
- [x] `make cli-demo` bleibt gruen; ein enger Demo-Sensor zeigt alle Szenarien
  und enthaelt `terminal=eskaliert`, `terminal=abgelehnt` und `executed=false`.
- [x] README/Doku macht die Repo-About-Aussage "weiss, wann er nicht genug
  weiss" anhand der CLI-Demo belegbar.
- [x] `make doc-check` und `make gates` gruen.
- [x] Review- und Verification-Harness-Berichte ohne offene Findings bzw.
  DoD-Verletzungen.
- [x] Closure-Notiz mit Steering-Loop-Eintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `adapters/inbound/cli/src/main/.../Main.kt` | update | CLI-Argumente fuer Szenario-Auswahl und Mehrszenario-Ausgabe. |
| `adapters/inbound/cli/src/main/.../Runtime.kt` | update | Sichtbare Ausgabe um Szenario, Executor-Status und Gruende erweitern. |
| `adapters/inbound/cli/src/test/.../CliRuntimeE2eTest.kt` | update | Default, Negativpfade und Mehrszenario-Ausgabe deterministisch absichern. |
| `Dockerfile` / `Makefile` | update | Enger Demo-Sensor fuer alle CLI-Szenarien. |
| `README.md` / ggf. `docs/user/integration.md` | update | Vorzeigbare Demo zur Unsicherheitsgrenze dokumentieren. |
| `docs/reviews/*slice-030*` | neu | Review-Harness-Artefakt. |
| `docs/verifications/*slice-030*` | neu | Verification-Harness-Artefakt. |
| `docs/plan/planning/in-progress/roadmap.md` | update | Demo-Slice aktivieren und abschliessen. |

## 4. Trigger

`slice-024` und `slice-029` sind in `done/`: der CLI-Composition-Root existiert
und die Beispiele verweisen bereits auf ihn.

## 5. Closure-Trigger

DoD vollstaendig + Review/Verification + `make gates` gruen + Slice in
`done/`.

## 6. Risiken und offene Punkte

- Demo darf keine zweite Runtime neben `adapters:inbound:cli` erzeugen.
- Szenario-Auswahl darf keinen Executor-Bypass einfuehren; Ausfuehrung bleibt
  an `Zyklusergebnis.Gehandelt.freigabe.aktion` gebunden.
- Keine echten Provider-/Approval-/Persistenzadapter in diesem Slice.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** Der Slice musste keine neue Kontrolllogik bauen. Die
vorhandenen CLI-Szenarien reichten aus; durch `scenario`, `terminal`,
`executed`, `reason` und `executor_boundary` ist die Sicherheitsgrenze jetzt
als Demo-Ausgabe beobachtbar. `make cli-demo` bleibt der positive Default,
`make cli-demo-scenarios` zeigt die negativen Pfade.

**Was ist offen geblieben:** Die Szenarien laufen weiter gegen deterministische
Fake-Adapter. Echte Provider-, Approval-, Ausfuehrungs- und Persistenzadapter
sind weiterhin separate Stabilisierung und werden hier nicht vorweggenommen.

**Steering-Loop:** Wenn eine README-/About-Aussage ein Sicherheitsverhalten
verspricht, braucht sie einen direkt zitierbaren Demo-Sensor oder Testpfad. Nur
interne Tests reichen fuer eine Repo-Beschreibung nicht als vorzeigbarer Beleg.

**Folge-Slices:** Echte Approval-, Ausfuehrungs- und Persistenzadapter fuer das
CLI-Bundle bleiben separat.

## 8. Sub-Area-Modus-Begründung

### Sub-Area: CLI-Composition-Root

- **Modus:** GF
- **Konventionen-Dichte:** hoch. `spec/architecture.md` legt den Inbound-Adapter
  `cli` als Composition-Root fest; `AGENTS.md` und `harness/README.md` binden
  Gates und Docker-/Make-Einstieg.
- **Phase-Reife:** Phase 4. Der CLI-Root existiert seit `slice-024`, ist mit
  E2E-Tests und `make cli-demo` abgesichert und wird in `slice-029` als
  produktiver Einstieg referenziert.
- **Evidenz-/Diskrepanz-Risiko:** niedrig. Der Slice macht vorhandene
  Runtime-Szenarien sichtbar und erweitert keine fachliche Gate-Regel.
- **Reconciliation-Aufwand:** keiner fuer diesen Slice; echte Runtime-Adapter
  bleiben Folge-Slices.

### Sub-Area: README/Demo-Dokumentation

- **Modus:** GF
- **Konventionen-Dichte:** mittel. README ist Source-Precedence-Rang 7 und darf
  nur behaupten, was Spec/Architektur und Gates bereits tragen.
- **Phase-Reife:** Phase 3. Die About-Aussage existiert; die neue Demo liefert
  den beobachtbaren CLI-Beleg.
- **Evidenz-/Diskrepanz-Risiko:** niedrig bis mittel. Risiko ist eine zu starke
  Demo-Behauptung ohne Sensor; DoD bindet deshalb README an konkrete
  CLI-Ausgabe.
- **Reconciliation-Aufwand:** erledigt in diesem Slice ueber Demo-Sensor und
  README-Abgleich.
