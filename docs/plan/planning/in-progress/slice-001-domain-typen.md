# Slice slice-001: Domain-Typen — Hypothese, Belief State, Resthypothese

**Status:** open → next → in-progress → done (Verzeichniswechsel, siehe
[Planning-README](../README.md)).

**Welle:** [`welle-01-belief-kern`](../welle-01-belief-kern.md).

**Bezug:** `LH-FA-BEL-001`, `LH-FA-BEL-003`, `LH-QA-03`, `LH-QA-04`; `ADR-0001`,
`ADR-0002`, `ADR-0003`; `ARC-01`.

**Autor:** pt9912. **Datum:** 2026-07-04.

---

## 1. Ziel

Die puren Domänentypen des Belief-Kerns (`ARC-01`) als framework-freie
Kotlin-Typen im Modul `hexagon:domain` (`commonMain`, Paket
`dev.beliefagent.domain.belief`): `Hypothese` (Identität + zugeordnete
Wahrscheinlichkeit + Evidenz-Referenz) und `BeliefState` (Menge
konkurrierender Hypothesen) mit der **Pflicht-Resthypothese** als
strukturellem Erstklasse-Konzept — noch **ohne** Normierungs-/Update-Logik
(folgt in `slice-002`/`slice-003`). Als **erster Code-Slice** stellt dieser
Slice zugleich das minimale **KMP-Gradle-Multi-Modul-Skelett** (`ADR-0003`)
bereit, das die Folge-Slices zum Kompilieren und Testen brauchen
(`ADR-0002`-Folgepflicht).

## 2. Definition of Done

- [x] `LH-FA-BEL-001` erfüllt: `BeliefState` trägt eine Menge konkurrierender
      `Hypothese`n mit zugeordneter Wahrscheinlichkeit; Test referenziert.
- [x] `LH-FA-BEL-003` erfüllt: die Resthypothese ist struktureller
      Pflichtbestandteil (Konstruktor-Pflicht); Test referenziert.
- [x] Typen im Modul `hexagon:domain` (`commonMain`), framework-/DI-frei
      (kein `org.koin.*`, kein Adapter) — `ADR-0001`, `ADR-0002`, `ADR-0003`.
- [x] KMP-Gradle-Multi-Modul-Skelett steht; `make build` und `make test`
      grün und deterministisch (`LH-QA-03`) — über multi-stage Dockerfile,
      kein Host-JDK/-Gradle (AGENTS.md §3.1).
- [x] Reproduzierbarkeit (Modul 14): Base-Image digest-gepinnt
      (`gradle:8.14-jdk21@sha256:…`); Build-Image-Hash via `--iidfile`
      erfasst; `build`-Target Docker-getrieben.
- [x] `make gates` grün.
- [x] Doku-Update: `make build`/`make test` in `AGENTS.md` §4 und
      `harness/README.md` von „geplant" nach „laufend" promotet.
- [ ] **Zurückgestellt:** Gradle-Dependency-Locking (`gradle.lockfile`) —
      Direktabhängigkeiten sind versionsgepinnt (Kotlin 2.4.0); volles
      transitives Locking folgt (Replay/Modul 12).
- [ ] **Ausgesetzt:** `make arch-check` via `a-check` — KMP-Falsch-negativ an
      den a-check-Maintainer gemeldet, Antwort ausstehend; Reinheit derweil
      über Gradle-Modul-Grenzen + KMP-Source-Set-Sicht.
- [ ] Closure-Notiz mit Steering-Loop-Lerneintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `Dockerfile` (multi-stage) | neu | Toolchain (Gradle + JDK 21 + Kotlin) im Build-Stage; Host nutzt nur Docker + make (AGENTS.md §3.1, `ADR-0002` JDK-21-Pin). Kein Host-JDK/-Gradle. |
| `settings.gradle.kts`, `build.gradle.kts` | neu | KMP-Build, `jvm()`-Ziel |
| `src/commonMain/kotlin/**/Hypothese.kt` | neu | Hypothesen-Typ (`ARC-01`) |
| `src/commonMain/kotlin/**/BeliefState.kt` | neu | Belief-State-Typ inkl. Pflicht-Resthypothese (`ARC-01`) |
| `src/commonTest/kotlin/**` | neu | deterministische Konstruktionstests (`LH-QA-03`) |
| `gradle.lockfile` | neu | Dependency-Locking, reproduzierbarer Dep-Tree (Modul 14 Lock-File) |
| image-hash.txt (in `harness/`, generiert) | neu | Build-Image-Hash via `make build` (`--iidfile`); lokaler Anker (Modul 14) |
| `a-check.mk`, `.a-check.yml` | neu | Arch-Gate analog `d-check`: GHCR-Image `ghcr.io/pt9912/a-check`, Digest-Pin, `--network none` |
| `Makefile` | update | `include a-check.mk`; `build`, `test`, `arch-check` ergänzen (Einstiegspunkt für alle Aktionen) |

## 4. Trigger

Welle-01-Start-Trigger erfüllt (`ADR-0001` & `ADR-0002` Accepted). Erster
Code-Slice der Welle → keine Slice-Vorbedingung.

## 5. Closure-Trigger

DoD vollständig + PR gemerged + Closure-Notiz geschrieben; Datei nach
`done/`.

## 6. Risiken und offene Punkte

- `arch-check` via `a-check` ist **ausgesetzt**: a-check verfehlt bei
  KMP-Multi-Source-Set mit flachen `layers`-Globs Source-Set-übergreifende
  Verletzungen (falsch-negativ) — an den a-check-Maintainer gemeldet, Antwort
  ausstehend. Bis dahin tragen Gradle-Modul-Grenzen + KMP-Source-Set-Sicht die
  Reinheit; scharfgeschaltet mit tiefen Modul-Globs, sobald die Antwort da ist.
- Normierung/Validierung bewusst **nicht** hier (`slice-002`); Bayes-Update
  bewusst **nicht** hier (`slice-003`).
- Runtime-/Distroless-Stage (Modul 14) ist für diesen Slice
  **zurückgestellt**: `slice-001` liefert eine Bibliothek + Tests, noch keine
  lauffähige App (`ARC-09`). Das Dockerfile deckt hier `deps` + `build`/Test
  ab; die gehärtete Runtime-Stage entsteht mit dem ersten lauffähigen
  Orchestrator.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** KMP-Multi-Modul-Skelett + Domänentypen sauber; `make
build`/`make test` im Docker grün. **Anders als geplant:** Architektur auf
HexSlice (`ADR-0003`) umgestellt, plain-Single-Modul → **KMP-Multi-Modul**,
Paket `dev.beliefagent.domain.belief`. **Steering-Loop:** a-check-KMP-Falsch-
negativ entdeckt → `CO-001`; d-check auf v0.37.1 gehoben. **Offen/Folge:**
`arch-check` via `CO-001` (a-check-Antwort ausstehend); `gradle.lockfile`
zurückgestellt.

## 8. Sub-Area-Modus-Begründung

Alle berührten Sub-Areas GF (frisches Repo, Doku führt, kein Bestandscode;
siehe Kurs Modul 5 §Worked Mini-Example). Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
