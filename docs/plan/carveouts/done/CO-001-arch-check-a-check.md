# CO-001: arch-check ausgesetzt — a-check-KMP-Falsch-negativ, Upstream ausstehend

**Status:** Aufgelöst (a-check v0.10.0, 2026-07-04)

**Datum angelegt:** 2026-07-04. **Letzte Prüfung:** 2026-07-04.

**Betroffenes Gate:** `arch-check` (via a-check)

**Geltungsbereich:** `hexagon:domain` und künftige Kern-Module (HexSlice
domain/application/ports).

**Folge-Slice:** [`welle-01-belief-kern`](../../planning/welle-01-belief-kern.md)
(neuer Slice wird beim Auflösungs-Trigger angelegt).

---

## Begründung

Das gewählte Arch-Gate **a-check** (`ADR-0003`) verfehlt im **Kotlin-
Multiplatform-Fall** Source-Set-übergreifende Verletzungen, wenn `commonMain`
und `jvmMain` dasselbe `package_base` teilen und flache `layers`-Globs genutzt
werden (falsch-negativ, dokumentiert reproduziert: shallow→0 Befunde,
deep→1 Befund). Das ist eine **externe Abhängigkeit außerhalb unserer
Kontrolle**: der Fall wurde an den a-check-Maintainer gemeldet, die Antwort
(Fix oder dokumentiertes KMP-Rezept) steht aus. Ein blind-grünes Gate wäre
eine Harness-Lüge (Modul 13) — daher wird `arch-check` bewusst **nicht**
behauptet, statt es unwirksam zu verdrahten.

Die Kern-Reinheit ist derweil **dreifach anders** gesichert: Gradle-Modul-
Grenzen (der `hexagon:domain`-Modul hat keine Adapter-Dependency), die
KMP-Source-Set-Sichtbarkeit (`commonMain` sieht `jvmMain` nicht) und der
Review.

## Auflösungs-Trigger

Der a-check-Maintainer bestätigt den Fix **oder** dokumentiert das
KMP-Rezept (Multi-Modul mit tiefen, modul-spezifischen `layers`-Globs). Dann:
`.a-check.yml` + `a-check.mk` (Digest-gepinnt) verdrahten, `make arch-check`
grün, `arch-check` in `make gates` aufnehmen und aus dem „Nicht behauptet"-
Block in `harness/README.md` promoten.

## Geltungs-Konfiguration

| Datei | Zeile/Section | Wert |
|---|---|---|
| `harness/README.md` | §Sensors „Nicht behauptet" | `make arch-check` als geplant/ausstehend geführt |
| `Makefile` | — | kein `arch-check`-Target (bewusst nicht behauptet) |

## Verifikation (nach Auflösung)

- [x] `.a-check.yml`/`a-check.mk` verdrahtet, `make arch-check` grün (a-check v0.10.0; tech-leak-Zähne nachgewiesen).
- [x] `arch-check` in `make gates`; `make gates` grün (5 Gates).
- [x] Datei nach `docs/plan/carveouts/done/` bewegt (reiner `git mv`). <!-- d-check:ignore (done/ entsteht erst bei erster Carveout-Auflösung) -->
- [x] Kein Folge-Slice nötig — direkt durch Upstream-Fix aufgelöst; Multi-Modul-`resolution` wird in welle-02 erweitert (Guard-erzwungen).

## Geschichte

| Datum | Ereignis | Verweis |
|---|---|---|
| 2026-07-04 | Angelegt bei Welle-01-Closure; a-check-Fall an Maintainer gemeldet | `slice-001` DoD (arch-check) |
| 2026-07-04 | **Aufgelöst** — a-check v0.10.0 fail-closed-Guard + dokumentiertes KMP-Rezept; `arch-check` verdrahtet + grün | `MR-005` |
