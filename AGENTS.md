# AGENTS.md — Briefing für AI-Coding-Agenten

## 1. Was diese Datei ist

Onboarding-Briefing für jede AI-Session, die in `belief-agent` Code oder
Dokumentation ändert. Sie verweist auf die kanonischen Quellen und
formuliert die Hard Rules, die der Implementation-Agent immer einhalten
muss.

**Bei Konflikt zwischen dieser Datei und einer kanonischen Quelle gilt die
kanonische Quelle** (Source Precedence — siehe
[`harness/README.md`](harness/README.md)).

Strukturregeln (ID-Schemata, Verzeichniskonvention, Adaptionen ggü.
Baseline, Modus-Deklarationen pro Sub-Area, Zusatzklassen für
Sensors-Bindung) leben in [`harness/conventions.md`](harness/conventions.md).

Das **Betriebsregelwerk der adoptierten Baseline** liegt als ZIP-Bundle
(Regelwerk pro Modul, self-navigierbar) als Release-Asset bereit:
<https://github.com/pt9912/ai-harness-course/releases/download/v1.3.0/lab-regelwerk.zip>
— pro Session das aufgaben-relevante Modul lesen, bevor der Workflow (§6)
startet. Derivativ: bei
Konflikt gelten die kanonischen Quellen; adoptierter Stand in
[`harness/conventions.md`](harness/conventions.md) §Baseline.

Die **Skelett-Vorlagen** der Baseline liegen als ZIP bereit:
<https://github.com/pt9912/ai-harness-course/releases/download/v1.3.0/lab-templates.zip>
— beim Anlegen neuer Artefakte das passende Template kopieren statt frei zu
formulieren. Wiederkehrende Vorlagen liegen co-located im Repo
(`docs/plan/adr/NNNN-titel.template.md`, `docs/plan/planning/slice.template.md`,
`docs/plan/planning/welle.template.md`, `docs/plan/carveouts/carveout.template.md`,
`docs/reviews/review-report.template.md`).

## 2. Kanonische Quellen (Source Precedence)

In dieser Reihenfolge:

1. [`spec/lastenheft.md`](spec/lastenheft.md) — vertraglich abnahmebindend.
2. [`spec/spezifikation.md`](spec/spezifikation.md) — technisch verbindlich, fortschreibbar (3. Spec-Stratum, `MR-001`).
3. [`spec/architecture.md`](spec/architecture.md) — Komponenten- und Sequenzsicht.
4. [`docs/plan/adr/`](docs/plan/adr/) — ADR-Verzeichnis und -Index.
5. [`docs/plan/planning/in-progress/roadmap.md`](docs/plan/planning/in-progress/roadmap.md) — aktuelle Welle.
6. `docs/user/*` (noch nicht vorhanden) — Operations, Quality, Releasing.
7. [`README.md`](README.md) — Projekt-Überblick.
8. **AGENTS.md (diese Datei).**
9. [`harness/README.md`](harness/README.md) — Harness-Einstieg.

## 3. Harte Regeln

### 3.1 Gates über `make`

Gates laufen über `make` (das Docker nutzt, z. B. `make doc-check`). Der
Host braucht Docker und GNU `make`. Die Code-Toolchain-Politik
(Docker-only o. ä.) wird mit dem Sprach-ADR (`LH-RB-04`) festgelegt.

### 3.2 Suppression-Verbot

Inline-Suppression von Lint-/Typecheck-Findings ist verboten; Ausnahmen
leben in zentraler Konfiguration mit Begründung (verschärft mit dem ersten
Code-Slice gemäß Sprach-ADR).

### 3.3 git mv + Inhaltsänderung = zwei Commits

Wird eine Datei verschoben **und** ihr Inhalt umgeschrieben: erst
`git mv source target` (reiner Move), dann zweiter Commit mit der
Inhaltsänderung — sonst wird `git log --follow` unzuverlässig.

### 3.4 Architektur ist sprach- und meilensteinfrei

`spec/architecture.md` referenziert ADRs (aufwärts über deren `Schärft:`)
und Modul-Pfade, aber **keine** Wellen, Slices, Commit-Hashes oder
Closure-Daten. Die zeitliche Schicht lebt in `docs/plan/planning/`.

### 3.5 ADRs sind nach `Accepted` immutable

Eine ADR mit Status `Accepted` wird nicht inhaltlich überschrieben.
Korrekturen entstehen als neue ADR mit `Supersedes ADR-NNNN`.

### 3.6 Gates dürfen nicht ohne ADR gelockert werden

Jede Schwellen-Senkung (Coverage, Linter-Strenge, Architekturregel) ist ein
ADR, kein PR-Kommentar.

### 3.7 Konfidenz-Gate ist nicht umgehbar (Safety/Control)

Das Konfidenz-Gate liegt außerhalb der Aktion; keine Aktion erhält einen
Pfad, der es auslässt (`LH-FA-POL-006`). Im Zweifel wird **nicht** gehandelt,
sondern Information gesammelt oder eskaliert (`LH-QA-02`).

### 3.8 Keine irreversible Aktion ohne harte Schwelle + Freigabe

Extern-wirksame Aktionen sind nur nach harter Schwelle *und* expliziter
menschlicher Freigabe zulässig (`LH-FA-POL-004`, `LH-OUT-04`); bei hoher
Resthypothese gesperrt (`LH-FA-POL-005`). Modell-Konfidenz wird in explizite,
protokollierte Zahlen überführt (`LH-FA-LLM-003`).

## 4. Quality Gates

Nur Targets, die im `Makefile` existieren und laufen (keine halluzinierten
Gates, Modul 13):

| Target | Zweck |
|---|---|
| `make doc-check` | Doku-Referenzen prüfen (d-check: links, anchors) |
| `make gates` | alle aktuell lauffähigen Gates (derzeit `doc-check`) |
| `make help` | verfügbare Targets anzeigen |

Geplant (entstehen mit dem ersten Code-Slice, dann hier ergänzen):
`make lint`, `make test`, `make arch-check`, `make coverage-gate`,
`make ci`, `make fullbuild`.

## 5. Dokumentations-Regeln

- Requirement- und Architektur-IDs müssen in PRs/Commits referenziert sein.
  IDs werden beim Spec-/ADR-Schreiben nach dem in
  [`harness/conventions.md`](harness/conventions.md) deklarierten Schema
  vergeben (`LH-FA-<BEREICH>-<NNN>` / `LH-QA-<NN>`, `ARC-<NN>`, `SPEC-<NN>`, ADR-Nummern über
  den Index) — nie ad hoc im PR.
- Neue ADRs müssen den ADR-Index
  ([`docs/plan/adr/README.md`](docs/plan/adr/README.md)) aktualisieren.
- Roadmap/Status-Geschichte lebt in `docs/plan/planning/`, nicht in
  `spec/architecture.md`.

## 6. Minimal Agent Workflow

Pro Slice:

1. [`harness/README.md`](harness/README.md) lesen.
2. Relevante kanonische Quelle lesen (Source Precedence beachten).
3. Betroffene `LH-*`/`ADR-*`-IDs identifizieren.
4. Kleinste sinnvolle Änderung planen.
5. Engsten nützlichen Sensor laufen lassen.
6. Repo-weiten Gate-Lauf vor Handoff (`make gates`).
7. Doku/Indizes aktualisieren, falls ein öffentlicher Vertrag berührt.
8. Ausgeführte Sensors und verbleibende Risiken berichten — keine
   Erfolgsmeldung ohne Gate-Ausführung.
