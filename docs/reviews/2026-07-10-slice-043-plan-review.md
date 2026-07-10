# Review-Report: slice-043 Plan-Review — 2026-07-10

**Review-Art:** Plan — geprueft *wogegen*: Spec (`spec/lastenheft.md`,
`spec/architecture.md`) und Accepted-ADRs plus die bereits **abgeschlossene**
Schicht-Trennung aus slice-042 (§9/Closure, gleiches Modul), **vor**
Implementierung, ohne Diff (Modul 10 §Drei Review-Arten).

**Gegenstand:** `docs/plan/planning/open/slice-043-aktionsvorschlag-koog-langchain4j-paritaet.md`

**Skill:** `.harness/skills/reviewer.md` @ v1.0 · <!-- d-check:ignore (Adopter-spezifischer Skill-Pfad) -->
**Modell:** Claude Opus 4.8 (1M) · **Datum:** 2026-07-10

**Eingangs-Kontext** (die Verträge, gegen die geprüft wurde — ohne diese Liste
ist der Lauf nicht reproduzierbar):

- Slice-Plan slice-043 (Autor Codex, `open/`)
- `spec/lastenheft.md`: `LH-FA-LLM-001..004` (`:301/304/311`), `LH-FA-ACT-001..004`,
  `LH-FA-POL-006`, `LH-QA-02` (`:324` fail-safe), `LH-QA-03` (`:328` Testbarkeit),
  `LH-QA-04` (`:333` Erweiterbarkeit, Prio **Soll**)
- `spec/architecture.md`: `ARC-07` (aktionsvorschlag-Use-Case, `:81`), `ARC-08`
  (Outbound-LLM-Adapter, `:42/93`), `ARC-09` (Composition-Root/Executor-Grenze, `:25/91`)
- ADRs: `docs/plan/adr/0001` (Hexagonaler LLM-Port), `0002` (Dependency am Rand),
  `0003` (HexSlice/Layer), `0006` (Coverage-Scope); `docs/plan/adr/README.md`
  (Supersedes-Kette geprüft: `0008` supersedet `0005`, für diesen Slice nicht relevant)
- `AGENTS.md §3` (Hard Rules) + `harness/README.md` (Source-Precedence)
- **Ist-Code (verankert):**
  `.../aktionsvorschlag/ports/AktionsVorschlagsPort.kt` (Port-Signatur
  `vorschlaege(belief): List<AktionsVorschlag>`, `:14` — **kein** Evidenz-Kontext),
  `.../aktionsvorschlag/dto/AktionsVorschlag.kt` (6 primitive Rohfelder, `:11-18`),
  `.../aktionsvorschlag/AktionsVorschlagen.kt` (Use-Case-Semantik: unbekannte
  Hypothese `:59`, Beschreibung `:62`, Wirkungsklasse `:63`, Evidenz-Auflösung/
  Nicht-Leere `:65-66/68`, Konfidenz-Externalisierung/`[0,1]` `:70-71`),
  `adapters/outbound/llm-action-langchain4j/` (slice-042, existiert — Parität-Referenz),
  `adapters/outbound/llm-koog/build.gradle.kts:17` (`ai.koog:koog-agents:1.0.0` bereits adoptiert)
- **vorherige Findings am gleichen Modul (letzte ~5):**
  `2026-07-09-slice-042-plan-review.md` (PR-F1 INFO Modul offen, PR-F2 LOW
  Adapter-Validierung), `2026-07-09-slice-042-design-review.md` (DR-F1 MEDIUM
  Validierungs-Schicht), sowie **Closure-Notiz + §9-Auflösung** in
  `docs/plan/planning/done/slice-042-llm-aktionsvorschlag-provider-adapter.md`
  (§9 F-1: **Adapter = Wire-/Deserialisierungs-Integrität; Semantik — unbekannte
  Hypothese, Wirkungsklasse, Evidenz-Auflösung/Nicht-Leere, pSuccess-Bereich
  `[0,1]`, Konfidenzreferenz — bleibt im Use Case `AktionsVorschlagen`**)

> **Rollentrennung (Modul 8):** Reviewer kategorisiert gegen Spec/ADR + etablierte
> Modul-Verträge; er entscheidet nicht und schlägt keine Lösung vor. Layer-Besitz
> ist nominell Design-Thema — hier aber Plan-Sache, weil DoD 3 einer **bereits
> abgeschlossenen** (Closure-verankerten) Schicht-Entscheidung desselben Moduls
> widerspricht (slice-042 §9 F-1).

> **Hinweis (`claude-api`-Skill):** nicht anwendbar — Skip-Grep trifft
> (Projekt arbeitet mit LangChain4j/Koog); dieser Lauf prüft Architektur/Spec/
> Schicht-Besitz, keine Claude-API-Nutzung.

---

## Findings

### F-1 — DoD 3 zieht Use-Case-Semantik zurück auf die Adapter-/Contract-Testebene, die slice-042 gerade herausgelöst hat

- `kategorie`: MEDIUM
- `quelle`: `ARC-07` (Port-Vertrag/Use-Case-Lokalität), `ADR-0003`
  (Layer-Platzierung), slice-042 §9 F-1/DR-F1-Präzedenz, Maintainability
  (Skill §Klassifikation „Unklarer Schicht-Besitz" / „Domain-Invariante dupliziert")
- `pfad`: `docs/plan/planning/open/slice-043-aktionsvorschlag-koog-langchain4j-paritaet.md:37`
  (DoD 3); vgl. `:54` (§3, Matrix in `src/test/kotlin/** beider Adapter`),
  `AktionsVorschlagen.kt:59/63/66/68/71`, `AktionsVorschlagsPort.kt:14`
- `befund`: DoD 3 verlangt „Contract-Tests" — per §3 (`:54`) als „Gemeinsame
  Contract-Matrix fuer Parser, Prompt und fail-closed Mapping" in
  `src/test/kotlin/** beider Adapter` — u. a. für „unbekannte Hypothese, ungueltige
  Wirkungsklasse, fehlende Evidenz, ungueltiges pSuccess und fehlende
  Konfidenzreferenz". Das sind genau die **semantischen** Regeln, die slice-042 §9
  F-1/Closure bewusst dem Use Case `AktionsVorschlagen` zuwies (`:59/63/66/68/71`);
  für „fehlende Evidenz" trägt der Port `vorschlaege(belief)`
  (`AktionsVorschlagsPort.kt:14`) keinen Evidenz-Kontext, sodass ein
  Adapter-/Contract-Test diese Ablehnung nicht erzeugen kann. Beobachtbar
  unentschieden (nicht vorgeschrieben): ob diese Matrix am Adapter, am
  Use-Case-Validierungsrand oder als Voll-Pfad-Integration liegt.
- `verifizierbar`: ja — Code-Review, ob die Adapter-Tests nur Wire-Format prüfen
  und Semantik dem Use Case überlassen; die Port-Signatur belegt den fehlenden
  Evidenz-Kontext; `make arch-check` · `make test`.

### F-2 — Modul/Framework disjunktiv gehalten, obwohl der Repo-Zustand die Wahl auf `koog` determiniert

- `kategorie`: LOW
- `quelle`: Modul 1 (Plan konvergiert Spec+ADR auf einen konkreten Diff),
  Maintainability (Plan-interne Inkonsistenz); Mirror slice-042 PR-F1
- `pfad`: `docs/plan/planning/open/slice-043-aktionsvorschlag-koog-langchain4j-paritaet.md:25`
  (DoD 1 „`llm-action-koog` oder `llm-action-langchain4j`"), `:52` (§3), `:67-68`
  (§4 „Vor Start wird festgestellt, welcher Framework-Pfad … fehlt")
- `befund`: DoD 1/§3/§4 halten Modul + Framework disjunktiv und verschieben die
  Wahl auf „vor Start", obwohl der Repo-Zustand sie bereits auflöst:
  `adapters/outbound/llm-action-langchain4j` existiert (slice-042,
  `settings.gradle.kts:30`), `llm-action-koog` fehlt. Der konkrete Diff (koog) ist
  damit jetzt bestimmbar; der Plan hält die Disjunktion dennoch offen — anders als
  slice-042, wo die Framework-Wahl real offen war (PR-F1).
- `verifizierbar`: nein — Konkretheits-/Prozess-Hinweis (Repo-Zustand über
  `settings.gradle.kts` belegbar).

### F-3 — `LH-QA-04` steht im Kopf-Bezug, ist aber keiner DoD-Zeile zugeordnet

- `kategorie`: LOW
- `quelle`: `LH-QA-04` (Soll), `AGENTS.md §5` (ID-Referenz); Skill §Klassifikation
  („unvollständige ID-Abdeckung bei Soll-Anforderungen")
- `pfad`: `docs/plan/planning/open/slice-043-aktionsvorschlag-koog-langchain4j-paritaet.md:9`
  (Kopf-Bezug `LH-QA-04`) vs. `:37` (DoD 3 nennt nur `LH-QA-02`/`03`), `:41`
  (DoD 4 nennt nur `ADR-0003`/`0006`)
- `befund`: `LH-QA-04` (Erweiterbarkeit ohne Kernänderung, `lastenheft.md:333`)
  steht im Kopf-Bezug, wird aber in keiner DoD-Zeile zitiert; DoD 4 (Build-/Arch-/
  Coverage-Integration, der plausible Erweiterbarkeits-Ort) referenziert nur
  `ADR-0003`/`0006`. Die Kopf-ID-Abdeckung ist damit gegenüber den DoD-Zeilen
  unvollständig.
- `verifizierbar`: nein — Doku-/ID-Kohärenz (kein Gate erzwingt Kopf↔DoD-Mapping).

### F-4 — `ADR-0002`-Dependency-Guard und Koog-Adoptionsstand nicht benannt; `ADR-0002` fehlt im Kopf-Bezug

- `kategorie`: INFO
- `quelle`: `ADR-0002` (Dependency am Rand); Mirror slice-042 DR-F3
- `pfad`: `docs/plan/planning/open/slice-043-aktionsvorschlag-koog-langchain4j-paritaet.md:10`
  (Kopf-Bezug ohne `ADR-0002`); §6 `:84-85`
- `befund`: slice-043 führt Koog als zweites Framework für Aktionsvorschläge ein;
  `ai.koog:koog-agents:1.0.0` ist bereits adoptiert
  (`adapters/outbound/llm-koog/build.gradle.kts:17`), sodass der neue
  `llm-action-koog`-Adapter keine neue Toolchain-Fläche und keinen Folge-ADR
  erzeugt. Der Plan benennt weder diese Rückfallebene (`ADR-0002`-Guard, wie
  slice-042 nach DR-F3) noch führt er `ADR-0002` im Kopf-Bezug (`:10`).
- `verifizierbar`: ja — `build.gradle.kts`-Diff zeigt, ob eine **neue**
  Framework-Dep entsteht; `make arch-check`.

### F-5 — Besitz des gemeinsamen Parser-/Contract-Codes bleibt auf Plan-Ebene unbestimmt

- `kategorie`: INFO (Rollen-Verweis: Design-Review)
- `quelle`: `ARC-07`/`ADR-0003` (Layer-/Modul-Besitz), Maintainability; Skill
  §Was-NICHT (Design-Detail → Design-Review)
- `pfad`: `docs/plan/planning/open/slice-043-aktionsvorschlag-koog-langchain4j-paritaet.md:53`
  (§3 „Paritaets-Contract … gemeinsam nutzbar machen"), `:80-83` (§6)
- `befund`: Der Plan will den Paritäts-Contract „gemeinsam nutzbar" machen (`:53`)
  und hält den Besitz disjunktiv offen („klares Adapter-internes Modul ODER
  bewusst duplizierte Tests", `:80-83`); ein solches gemeinsames Modul ist weder
  in §3 noch in DoD 4 (`settings.gradle.kts`/`.a-check.yml`) registriert. Der Plan
  benennt das Risiko selbst — auf Plan-Ebene bleibt die Schicht-/Modul-Zuordnung
  des gemeinsamen Codes damit deklariert-offen (Auflösung im Design-Review).
- `verifizierbar`: nein — Schnitt-/Design-Frage (Rolle: Design-Review).

## Negativbefunde

- geprüft, ohne Befund: **`spec/`** — `ARC-07` (aktionsvorschlag-Use-Case),
  `ARC-08` (Outbound-LLM-Adapter), `ARC-09` (Composition-Root/Executor-Grenze) und
  die `LH-FA-*`/`LH-QA-*`-IDs sind sachgerecht adressiert; `LH-QA-02` (fail-safe)
  und `LH-QA-03` (Testbarkeit) korrekt an die Contract-Matrix gebunden.
- geprüft, ohne Befund: **`docs/plan/adr/`** — Supersedes-Kette geprüft
  (`0008`↦`0005`, hier irrelevant); `ADR-0001/0003/0006` sachgerecht referenziert;
  einzige ADR-Auffälligkeit ist der fehlende `ADR-0002`-Bezug (F-4).
- geprüft, ohne Befund: **`hexagon/application/.../aktionsvorschlag`** — Port
  (`:14`), DTO (`:11-18`) und Use-Case-Validierung (`:59-71`) belegen die
  slice-042-Schicht-Trennung; der Plan plant **keine** Port-Änderung (konsistent),
  wodurch F-1 (Semantik am Adapter) zur beobachtbaren Spannung wird.
- geprüft, ohne Befund: **`adapters/outbound/llm-action-*` / `llm-koog`** —
  `llm-action-langchain4j` existiert (slice-042), `llm-action-koog` fehlt; Koog-Dep
  bereits adoptiert (`llm-koog/build.gradle.kts:17`); `hexagon:*`-Reinheit als
  DoD 1 (`:28`) adressiert.
- geprüft, ohne Befund: **Build-/Arch-Dateien** — `settings.gradle.kts`,
  `.a-check.yml`, `Dockerfile`, Modul-`build.gradle.kts` + Kover-Gate sind in DoD 4
  (`:39-41`) für den neuen Pfad benannt (slice-041/042-Lehre angewandt).
- geprüft, ohne Befund: **Scope-Abgrenzung** — keine CLI-Default-Umbindung, keine
  Live-Provider-Tests, keine Secrets, keine Gate-/Approval-/Executor-Änderung
  (`LH-FA-POL-006`) sauber gezogen (DoD 5 `:42-44`, §6 `:84-85`).
- geprüft, ohne Befund: **WIP-Limit** = 0 (`in-progress/` nur `roadmap.md`) und
  **Wellen-Zuordnung** welle-05 roadmap-konsistent (`roadmap.md:67/184`).

## Ausgeführte Sensoren

- `Read`/`grep` über: Plan slice-043; `lastenheft.md` (`LH-QA-02/03/04`,
  `LH-FA-LLM-003`); `architecture.md` (`ARC-07/08/09`); ADRs + `README.md`
  (Supersedes-Kette); Port/DTO/Use-Case (Zeilenanker verifiziert); slice-042
  Plan-/Design-Review + Closure/§9; Koog-Dep-Adoption (`llm-koog/build.gradle.kts`);
  `settings.gradle.kts` (vorhandene/fehlende Adaptermodule); `roadmap.md` (Welle/WIP).
- `make`-Gates: **nicht ausgeführt** (Plan-Review ohne Diff, Modul 10). Doc-Check
  dieses Reports erfolgt im gemeinsamen Doku-Lauf.

## Summary

| Kategorie | Anzahl |
|---|---:|
| HIGH | 0 |
| MEDIUM | 1 |
| LOW | 2 |
| INFO | 2 |

## Verdikt

**Merge-blockierend:** ja — **1 MEDIUM** (F-1). DoD 3 zieht die von slice-042
§9 F-1/Closure herausgelöste Use-Case-Semantik (unbekannte Hypothese,
Wirkungsklasse, Evidenz, `[0,1]`, Konfidenzreferenz) zurück auf die
Adapter-/Contract-Testebene und verlangt für „fehlende Evidenz" eine Prüfung, die
der unveränderte Port (`AktionsVorschlagsPort.kt:14`, kein Evidenz-Kontext) nicht
tragen kann. F-2/F-3 (LOW) und F-4/F-5 (INFO) sind vor Closure zu klären, nicht
eigenständig blockierend.

**Übergabe:** Findings gehen an die Planung (Rückkante Review → Plan, Modul 8);
die Auflösung von F-1 gehört in einen §9-Block wie in slice-042. Der Reviewer
kategorisiert — Entscheidung/Umsetzung liegt bei Architect/Implementer. Der Report
ersetzt keine Verifikation (DoD-/Spec-Konformität prüft der Verifier separat,
Modul 11).
