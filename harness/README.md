# Harness

## Purpose

Dieser Harness verbindet die Spezifikationen, ADRs, Planning-Dokumente und
Gates von `belief-agent`. Er ist **kein Ersatz** für `spec/` oder `docs/`,
sondern ein **Einstiegspunkt** für Menschen und AI-Code-Agenten.

Wenn diese Datei einer kanonischen Quelle widerspricht, **gewinnt die
kanonische Quelle**, und diese Datei wird angepasst.

Strukturregeln (Verzeichniskonvention, ID-Schemata, Modus-Deklarationen pro
Sub-Area, Zusatzklassen für Sensors-Bindung) sowie Adaptionen ggü. der
adoptierten Baseline leben in [`conventions.md`](conventions.md). Diese
Datei dupliziert sie nicht.

## Source precedence

| Rang | Datei | Charakter |
|---|---|---|
| 1 | [`spec/lastenheft.md`](../spec/lastenheft.md) | vertraglich abnahmebindend |
| 2 | [`spec/spezifikation.md`](../spec/spezifikation.md) | technisch fortschreibbar (3. Spec-Stratum, `MR-001`) |
| 3 | [`spec/architecture.md`](../spec/architecture.md) | Komponenten/Sequenzen, meilensteinfrei |
| 4 | [`docs/plan/adr/`](../docs/plan/adr/) | Architekturentscheidungen |
| 5 | [`docs/plan/planning/in-progress/roadmap.md`](../docs/plan/planning/in-progress/roadmap.md) | aktuelle Welle |
| 6 | `docs/user/*` (noch nicht vorhanden) | Operations, Quality, Releasing |
| 7 | [`README.md`](../README.md) | Projekt-Überblick |
| 8 | [`AGENTS.md`](../AGENTS.md) | Agent-Briefing |
| 9 | diese Datei | Harness-Einstieg |

**Spec-Stratifizierung.** Innerhalb der Spec gilt: Lastenheft (1) →
Spezifikation (2) → Architektur (3). Eine ADR darf die Spezifikation
schärfen, **niemals** das Lastenheft. Die konkrete Rangwahl ist in
[`conventions.md`](conventions.md) §Adaptions-Block (`MR-001`) begründet.

## Guides (Feedforward-Quellen)

| Quelle | Inhalt |
|---|---|
| [`spec/lastenheft.md`](../spec/lastenheft.md) | Anforderungen, IDs, Akzeptanzkriterien |
| [`spec/spezifikation.md`](../spec/spezifikation.md) | Algorithmen, Defaults, Schwellwerte |
| [`spec/architecture.md`](../spec/architecture.md) | Komponenten, Schichten, Constraints |
| [`docs/plan/adr/`](../docs/plan/adr/) | Architekturentscheidungen |
| [`docs/plan/planning/`](../docs/plan/planning/) | Slice-Pläne und Roadmap |
| [`AGENTS.md`](../AGENTS.md) | Hard Rules, Source Precedence, Workflow |
| [`conventions.md`](conventions.md) | repo-lokale Strukturregeln, Adaptions-Block (`MR-*`), Modus-Deklarationen |
| [Agenten-Regelwerk v1.4.0 (committet vendored)](../.harness/baseline/v1.4.0/regelwerk/README.md) | adoptiertes Betriebsregelwerk, Regelwerk pro Modul (`MR-007`); netzlos unter `.harness/baseline/v1.4.0/regelwerk/`, offline verifizierbar (`tools/harness/fetch-baseline-cache.sh --verify`); derivativ, Stand siehe [`conventions.md`](conventions.md) §Baseline |

## Sensors (Feedback-Gates)

Nur Targets, die im `Makefile` / `d-check.mk` **existieren** und laufen.
Kein Lauf-Status (der lebt in CI). Strukturell rote Gates → Carveout in
`docs/plan/carveouts/` mit Auflösungs-Trigger.

| Target | Vertrag | Bindung |
|---|---|---|
| `make doc-check` | Doku-Referenzen + Hygiene (d-check `links`/`anchors`/`ids`/`matrix`/`codepaths`/`spans`/`hostpaths`/`tracked`/`planning`) | Digest `sha256:3bbdb19b…` (v0.37.1, `MR-004`/`MR-006`) |
| `make build` | Reproduzierbarer KMP-Build aller Module (multi-stage Dockerfile, Base digest-gepinnt) | `ADR-0002`/`ADR-0003`; Modul 14 |
| `make test` | Deterministische Tests (`LH-QA-03`) im Docker-Build | `LH-FA-BEL-001`/`LH-FA-BEL-003` |
| `make coverage-gate` | Line-Coverage ≥ Stufen-Minimum (Kover `koverVerify`) | Schwelle `ADR-0004` (bootstrap-aware 90 % → 95 % bei M2) |
| `make arch-check` | Kern importiert kein Adapter/Framework; Multi-Modul-Kanten `domain`←`application`←`adapters` (a-check Rollen + `tech`-leak) | `ADR-0001`/`ADR-0003`; a-check v0.11.0 (`MR-005`) |
| `make gates` | bündelt alle aktuell lauffähigen Gates (`doc-check` + `build` + `test` + `coverage-gate` + `arch-check`) | — |

**CI-/Range-Gates** (`MR-006`, brauchen `RANGE=base..head`, laufen **nicht** im
lokalen `make gates`): `make doc-immutable` (`vcs` — Accepted-ADRs immutabel,
maschinelle Hard Rule 3.5), `make doc-commits` (`commits` — Traceability-Kennung
je Commit).

**Aktueller Lauf-Status:** lokal `make gates` (CI-Badge folgt mit
CI-Slice).
**Nicht behauptet** (geplant): `make lint`.

> Sobald ein Code-Gate real im `Makefile` existiert und läuft, wird seine
> Zeile aus dem „Nicht behauptet"-Block in die Haupt-Tabelle promotet
> (Promotion-Trigger).

## Traceability rules

- PRs/Commits **müssen** mindestens eine `LH-*`- oder `ADR-*`-ID nennen.
- Neue oder geänderte Anforderungen brauchen einen Beleg: Test, Gate, Demo
  oder ADR.
- Neue ADRs müssen im ADR-Index ([`docs/plan/adr/README.md`](../docs/plan/adr/README.md))
  ergänzt werden.
- Änderungen an Planning-Dokumenten müssen die Lifecycle-Regeln beachten
  (open → next → in-progress → done; reine `git mv`-Commits siehe
  [`AGENTS.md`](../AGENTS.md) §3.3).
- Abdeckung sichtbar machen: `make doc-trace` rendert die Requirements
  Traceability Matrix (Anforderung → ADRs/Slices, Waisen) — Report, kein
  Gate (`--json`/`--yaml` via `TRACE_FLAGS`).

## Safety and scope boundaries

`belief-agent` ist ein **Safety/Control**-Repo (`MR-003`): die
Sicherheitsfunktion ist das Konfidenz-Gate.

- **Fail-safe-Default:** Im Zweifel handelt das System nicht, sondern
  sammelt Information oder eskaliert (`LH-QA-02`).
- **Gate nicht umgehbar:** Das Konfidenz-Gate liegt außerhalb der Aktion;
  keine Aktion erhält einen Pfad, der es auslässt (`LH-FA-POL-006`).
- **Keine irreversible Aktion ohne harte Schwelle + menschliche Freigabe:**
  extern-wirksame Aktionen (Deploy, E-Mail, Zahlung, DB-Migration,
  externer API-Aufruf) sind nur nach harter Schwelle *und* expliziter
  menschlicher Freigabe zulässig (`LH-FA-POL-004`, `LH-OUT-04`); bei hoher
  Resthypothese sind sie unabhängig von der Top-Hypothese gesperrt
  (`LH-FA-POL-005`).
- **Konfidenz ist explizit und protokolliert:** modell-implizite Konfidenz
  wird in prüfbare, gate-fähige Zahlen überführt (`LH-FA-LLM-003`); die
  Entscheidungsspur ist ein auditierbares Protokoll (`LH-FA-AUD-003`).

## Minimal agent workflow

1. Diese Datei lesen.
2. Relevante kanonische Quelle lesen (Source Precedence beachten).
3. Betroffene `LH-*`/`ADR-*`-IDs identifizieren.
4. Kleinste sinnvolle Änderung planen.
5. Engsten nützlichen Sensor laufen lassen.
6. Repo-weiten Gate-Lauf vor Handoff (`make gates`).
7. Doku/Indizes aktualisieren, falls ein öffentlicher Vertrag berührt.
8. Ausgeführte Sensors und verbleibende Risiken berichten — keine
   Erfolgsmeldung ohne Gate-Ausführung.
