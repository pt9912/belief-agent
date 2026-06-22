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
| [Agenten-Regelwerk v1.3.0](https://github.com/pt9912/ai-harness-course/releases/download/v1.3.0/agents-regelwerk.md) | adoptiertes Betriebsregelwerk in Agenten-Kurzform; derivativ, Stand siehe [`conventions.md`](conventions.md) §Baseline |

## Sensors (Feedback-Gates)

Nur Targets, die im `Makefile` / `d-check.mk` **existieren** und laufen.
Kein Lauf-Status (der lebt in CI). Strukturell rote Gates → Carveout in
`docs/plan/carveouts/` mit Auflösungs-Trigger.

| Target | Vertrag | Bindung |
|---|---|---|
| `make doc-check` | Doku-Referenzen: lokale Links + Heading-Anker auflösbar (d-check `links`/`anchors`) | Reproduzierbarkeit: Image-Digest `sha256:68951f5a…` (v0.23.0) |
| `make gates` | bündelt alle aktuell lauffähigen Gates (derzeit `doc-check`) | — |

**Aktueller Lauf-Status:** lokal `make gates` (CI-Badge folgt mit
CI-Slice).
**Nicht behauptet** (geplant, entstehen mit dem ersten Code-Slice):
`make lint`, `make test`, `make arch-check` (Bindung ADR-0001 —
Kern importiert kein Adapter-Paket), `make coverage-gate` (Welle 1+).

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
