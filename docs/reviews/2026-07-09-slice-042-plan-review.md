# Review-Report: slice-042 Plan-Review — 2026-07-09

**Review-Art:** Plan — geprueft *wogegen*: Spec (`spec/lastenheft.md`) und
Accepted-ADRs, **vor** Implementierung, ohne Diff (Modul 10).

**Gegenstand:** `docs/plan/planning/open/slice-042-llm-aktionsvorschlag-provider-adapter.md`

**Skill:** `.harness/skills/reviewer.md` @ v1.0
**Modell:** Claude Opus 4.8 (1M) · **Datum:** 2026-07-09

**Eingangs-Kontext** (Skill §Kontext-Eingang):

- Slice-Plan slice-042
- `spec/lastenheft.md`: `LH-FA-LLM-001..004`, `LH-FA-ACT-001..004`,
  `LH-FA-POL-006`, `LH-QA-02/03/04`
- `spec/architecture.md`: `ARC-03`, `ARC-07`, `ARC-08`, `ARC-09`
- `docs/plan/adr/0001`, `0003`, `0006`; `AGENTS.md §3`; Roadmap (welle-05)
- **Ist-Code:** `AktionsVorschlagsPort.kt`, `AktionsVorschlag.kt`,
  `AktionsVorschlagen.kt`, `FakeAktionsVorschlagsPort.kt`,
  `adapters/outbound/llm-langchain4j` / `llm-koog` (Provider-Adapter-Praezedenz)
- **vorherige Findings am gleichen Modul:** slice-041-Review-Kette (v. a. der vom
  Frischkontext gefundene Coverage-Gate-/`ADR-0006`-Punkt und die
  Validierungs-Schicht-Trennung DR-F3)

> **Hinweis (`claude-api`-Skill):** nicht anwendbar — Skip-Grep trifft
> (`example/langchain` referenziert Provider); das Projekt arbeitet mit
> LangChain4j/Koog, und dieser Lauf prueft Architektur/Spec, keine Claude-API-Nutzung.

> **Rollentrennung (Modul 8):** kategorisiert gegen Spec/ADR. Der tragende
> (blockierende) Befund ist ein Schicht-/Validierungs-Design-Punkt und liegt im
> **Design-Review** gleichen Datums.

---

## Findings

### F-1 — Konkreter Provider-/Framework-Pfad und Modulname noch offen

- `kategorie`: INFO
- `quelle`: Modul 1 (Plan konvergiert Spec+ADR auf einen konkreten Diff)
- `pfad`: `docs/plan/planning/open/slice-042-llm-aktionsvorschlag-provider-adapter.md:26`
- `befund`: Modulname und Framework sind disjunktiv gehalten
  („`llm-action-langchain4j` oder nach Design-Review gleichwertig", `:26`/`:52`)
  und die Pfadwahl ist auf „vor Start" verschoben (`:68`). Damit ist der Plan
  gegen einen konkreten Diff noch nicht voll prueffaehig — dieselbe Konstellation
  wie slice-041 PR-F3, dort vor Code auf ein konkretes Modul + `.a-check`-Rolle/
  Root konvergiert. Legitimes Routing; hier festgehalten, damit das Verdikt keine
  voll fixierte Planung behauptet.
- `verifizierbar`: nein — Prozess-/Routing-Hinweis.

### F-2 — DoD 2 weist dem Adapter Validierungen zu, die der Use Case bereits fuehrt

- `kategorie`: LOW
- `quelle`: Maintainability (Plan-interne Konsistenz), `ARC-07`; Detail im
  Design-Review F-1
- `pfad`: `docs/plan/planning/open/slice-042-llm-aktionsvorschlag-provider-adapter.md:29`
- `befund`: DoD 2 verlangt vom Adapter fail-closed u. a. bei „unbekannten
  Hypothesen, ungueltigen Wirkungsklassen, fehlender Evidenz, Werten ausserhalb
  `[0,1]`" — genau die Semantik, die `AktionsVorschlagen` bereits validiert
  (`AktionsVorschlagen.kt:59/63/66/68/71`). §8 (`:124`) schliesst zugleich eine
  Port-Aenderung aus. Zwischen „Adapter validiert Semantik" (DoD 2) und „kein
  Port-Wechsel" (§8) besteht eine Spannung, die der Design-Review aufloesen muss
  (dort F-1, MEDIUM).
- `verifizierbar`: ja — Code-Review Adapter vs. `AktionsVorschlagen`; Port-Signatur.

## Negativbefunde

- geprueft, ohne Befund: **`ADR-0006`/per-Modul-Coverage** ist korrekt im Bezug
  und in DoD 4 verankert (`build.gradle.kts` + Kover-Gate) — die im slice-041
  Frischkontext-Review gefundene Luecke (IPR-2) ist hier **von vornherein
  vermieden**.
- geprueft, ohne Befund: **LH-ID-Abdeckung** ist stark und sachgerecht
  (`LH-FA-LLM-002` Modellaufgabe „Aktionen vorschlagen", `LH-FA-ACT-003` `pSuccess`,
  `LH-FA-ACT-004` `stuetzendeEvidenz`, `LH-FA-POL-006` Gate/Executor ausserhalb).
- geprueft, ohne Befund: **Executor-/Gate-Grenze** sauber gezogen — Adapter
  erzeugt keine `Aktionsfreigabe`, kein CLI-Default, keine Ausfuehrung (§6, `:79`).
- geprueft, ohne Befund: **Scope-Abgrenzung** (Provider-/Modellwahl, Secrets,
  Live-Netztests, zweiter Framework-Pfad als Folgeslice) klar (§6).
- geprueft, ohne Befund: **`LH-FA-LLM-003`** wird korrekt nicht dem Adapter
  zugeschrieben — `pSuccess` bleibt Rohwert, Externalisierung/Gate-Faehigkeit
  entsteht im bestehenden Use Case (§6, `:84`).
- geprueft, ohne Befund: **WIP=0** (`in-progress/` nur `roadmap.md`) und
  **Wellen-Zuordnung** (welle-05) roadmap-konsistent.

## Ausgefuehrte Sensoren

- `Read`/`grep` ueber Plan, Lastenheft, ADRs, Architektur, Port/DTO/Use-Case/Fake,
  Provider-Adapter-Praezedenz; WIP- und Framework-Dep-Check.
- `make`-Gates: nicht ausgefuehrt (Plan-Review ohne Diff). Doc-Check des Reports
  siehe Design-Review gleichen Datums (gemeinsamer Lauf).

## Summary

| Kategorie | Anzahl |
|---|---:|
| HIGH | 0 |
| MEDIUM | 0 |
| LOW | 1 |
| INFO | 1 |

## Verdikt

**Merge-blockierend (Plan-Ebene):** nein — kein HIGH/MEDIUM gegen Spec/ADR; F-2
(LOW) und F-1 (INFO) sind vor Closure zu klaeren. Der Plan hat die slice-041-
Lehren (Coverage/`ADR-0006`, `src/main`-Source-Set) sichtbar eingearbeitet.

**Vorbehalt:** Freigabe unter dem abgeschlossenen Design-Review — dort liegt **1
MEDIUM** (Validierungs-Schicht-Trennung), vor Implementierungsstart zu klaeren.

**Uebergabe:** F-1/F-2 an die Planung. Keine Verifikation (Modul 11).
