# Welle-01 Belief-Kern — Ergebnisse

**Abgeschlossen:** 2026-07-04. **Zielmeilenstein:** M1 — Belief-Kern lauffähig
(**erreicht**).

Verweis: `welle-01-belief-kern` ([Roadmap](../in-progress/roadmap.md)).

---

## Geliefert

| Slice | Deliverable | Anforderungen |
|---|---|---|
| `slice-001` | Domänentypen `Hypothese`/`BeliefState`/Pflicht-`Resthypothese` + KMP-Multi-Modul-Skelett (`hexagon:domain`) | `LH-FA-BEL-001`, `LH-FA-BEL-003` |
| `slice-002` | Normierung + Validierung (`BeliefState.of` weist ungültige Zustände zurück) | `LH-FA-BEL-002`, `LH-FA-BEL-004`, `LH-OP-05` |
| `slice-003` | Nicht-überschreibendes Bayes-Update (`Posterior ∝ Prior × Likelihood`) | `LH-FA-OBS-003`, `LH-FA-OBS-005` |
| `slice-004` | Entropie + Top-2-Abstand + Re-Hypothesen-Auslöser | `LH-FA-BEL-005`, `LH-FA-BEL-008` |

## Closure-Trigger — Nachweis

- ✅ Alle Slices `done`.
- ✅ `make gates` grün (`doc-check` + `build` + `test` + `coverage-gate`).
- ✅ Ungültiger Belief State (keine Resthypothese / nicht normiert) wird
  nachweislich zurückgewiesen (`LH-FA-BEL-004`, Tests in `NormierungTest`).
- ✅ Diese Closure-Notiz.

## Kennzahlen

- **30 deterministische Tests** (`LH-QA-03`), alle grün.
- **Line-Coverage 94,83 %** (Kover); Gate ≥ 90 % (`ADR-0004`, bootstrap-aware).
- Toolchain Docker-only (Kotlin 2.4.0 / Gradle 8.14.5 / JDK 21), digest-gepinnt.

## Architektur-/Prozess-Entscheidungen der Welle

- `ADR-0002` (KMP) angenommen; `ADR-0003` (HexSlice) + `ADR-0004`
  (Coverage-Gate) als `Proposed` ergänzt.
- d-check auf v0.37.1 gehoben (`MR-004`).

## Review

Durchgeführt; 3 Befunde adressiert: Eindeutigkeit der Hypothesen-IDs (Befund 1),
Ablehnung unbekannter Likelihood-IDs (Befund 2), Resthypothese-als-Masse
bestätigt (Befund 3).

## Offene Punkte / Carveouts

- `CO-001`: **aufgelöst 2026-07-04** — a-check v0.10.0 (fail-closed-Guard)
  verdrahtet, `arch-check` grün (zum Closure-Zeitpunkt war es noch ausgesetzt).
- `gradle.lockfile` (transitives Dependency-Locking) zurückgestellt;
  Direktabhängigkeiten sind versionsgepinnt.

## Steering-Loop-Lernen

- HexSlice + KMP + Multi-Modul früh festgelegt zahlte sich aus (Umbau war
  billig, weil nur Skelett stand).
- Werkzeug-Verifikation vor Behauptung (a-check-Falsch-negativ empirisch
  entdeckt) verhinderte ein blind-grünes Gate (Modul 13).
