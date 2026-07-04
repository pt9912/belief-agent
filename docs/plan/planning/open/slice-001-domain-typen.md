# Slice slice-001: Domain-Typen — Hypothese, Belief State, Resthypothese

**Status:** open → next → in-progress → done (Verzeichniswechsel, siehe
[Planning-README](../README.md)).

**Welle:** [`welle-01-belief-kern`](../welle-01-belief-kern.md).

**Bezug:** `LH-FA-BEL-001`, `LH-FA-BEL-003`, `LH-QA-03`; `ADR-0001`,
`ADR-0002`; `ARC-01`.

**Autor:** pt9912. **Datum:** 2026-07-04.

---

## 1. Ziel

Die puren Domänentypen des Belief-Kerns (`ARC-01`) als framework-freie
Kotlin-Typen in `commonMain`: `Hypothese` (Identität + zugeordnete
Wahrscheinlichkeit + Evidenz-Referenz) und `BeliefState` (Menge
konkurrierender Hypothesen) mit der **Pflicht-Resthypothese** als
strukturellem Erstklasse-Konzept — noch **ohne** Normierungs-/Update-Logik
(folgt in `slice-002`/`slice-003`). Als **erster Code-Slice** stellt dieser
Slice zugleich das minimale **KMP-Gradle-Skelett** bereit, das die
Folge-Slices zum Kompilieren und Testen brauchen (`ADR-0002`-Folgepflicht).

## 2. Definition of Done

- [ ] `LH-FA-BEL-001` erfüllt: `BeliefState` trägt eine Menge konkurrierender
      `Hypothese`n mit zugeordneter Wahrscheinlichkeit; Test referenziert.
- [ ] `LH-FA-BEL-003` erfüllt: die Resthypothese ist struktureller
      Pflichtbestandteil (nicht weglassbar konstruierbar); Test referenziert.
- [ ] Typen liegen in `commonMain`, framework-/DI-frei (kein `org.koin.*`,
      kein Adapter-Paket) — `ADR-0001`, `ADR-0002`.
- [ ] KMP-Gradle-Skelett steht: `commonMain`/`commonTest`/`jvmMain`/`jvmTest`,
      JDK-21-Pin, `jvm()`-Ziel; `make build` und `make test` grün und
      deterministisch (`LH-QA-03`).
- [ ] `make arch-check` über `a-check` (GHCR-Image, digest-gepinnt, offline
      `--network none`) verdrahtet und grün.
- [ ] `make gates` grün.
- [ ] Doku-Update: `make build`/`make test`/`make arch-check` in `AGENTS.md`
      §4 und `harness/README.md` von „geplant" nach „laufend" verschoben.
- [ ] Closure-Notiz mit Steering-Loop-Lerneintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `settings.gradle.kts`, `build.gradle.kts`, Gradle-Wrapper | neu | KMP-Build, `jvm()`-Ziel, JDK-21-Pin (`ADR-0002`) |
| `src/commonMain/kotlin/**/Hypothese.kt` | neu | Hypothesen-Typ (`ARC-01`) |
| `src/commonMain/kotlin/**/BeliefState.kt` | neu | Belief-State-Typ inkl. Pflicht-Resthypothese (`ARC-01`) |
| `src/commonTest/kotlin/**` | neu | deterministische Konstruktionstests (`LH-QA-03`) |
| `a-check.mk`, `.a-check.yml` | neu | Arch-Gate analog `d-check`: GHCR-Image `ghcr.io/pt9912/a-check`, Digest-Pin, `--network none` |
| `Makefile` | update | `include a-check.mk`; `build`, `test`, `arch-check` ergänzen |

## 4. Trigger

Welle-01-Start-Trigger erfüllt (`ADR-0001` & `ADR-0002` Accepted). Erster
Code-Slice der Welle → keine Slice-Vorbedingung.

## 5. Closure-Trigger

DoD vollständig + PR gemerged + Closure-Notiz geschrieben; Datei nach
`done/`.

## 6. Risiken und offene Punkte

- `arch-check` wird in diesem Slice über das GHCR-Image
  `ghcr.io/pt9912/a-check` (digest-gepinnt, offline `--network none`)
  verdrahtet — analog `d-check.mk`/`.d-check.yml`. Rest-Risiko: exakten
  Digest aus den a-check-Release-Notes ziehen; bis dahin ist die Kern-Reinheit
  strukturell durch die `commonMain`/`jvmMain`-Trennung gesichert.
- Normierung/Validierung bewusst **nicht** hier (`slice-002`); Bayes-Update
  bewusst **nicht** hier (`slice-003`).

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss füllen. -->

## 8. Sub-Area-Modus-Begründung

Alle berührten Sub-Areas GF (frisches Repo, Doku führt, kein Bestandscode;
siehe Kurs Modul 5 §Worked Mini-Example). Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
