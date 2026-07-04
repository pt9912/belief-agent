# ADR-0004: Coverage-Gate — bootstrap-aware Line-Coverage-Schwelle (Kover)

**Status:** Accepted

**Datum:** 2026-07-04

**Autor:** belief-agent

**Bezug:** [`LH-QA-03`](../../../spec/lastenheft.md#lh-qa-03--testbarkeit)

**Schärft:** — (Prozess-/Gate-Entscheidung ohne eigenes Spec-Stratum; die
Schwelle lebt in der Build-Datei und im `make coverage-gate`-Target.)

---

## Kontext

Der Belief-Kern trägt jetzt echte Logik (Validierung, Bayes-Update,
Unsicherheitsmaße). Ohne Coverage-Gate kann eine spätere Änderung Testabdeckung
still absenken. Regelwerk Modul 13: **Schwellen sind ADR-pflichtig** — ein
Coverage-Gate braucht eine begründete, terminierte Schwelle, kein PR-Kommentar.

Gemessene Ausgangs-Line-Coverage von `hexagon:domain`: **94,83 %** (`make
coverage`, Kover). Die unabgedeckten Zeilen sind generierte `data class`-Member
(`copy`/`componentN`/`toString`), keine Logik.

## Entscheidung

Wir führen ein **bootstrap-aware Coverage-Gate** über **Kover** ein
(`make coverage-gate` → `koverVerify`), mit terminierter Reifestufe:

| Stufe | Trigger | Line-Coverage-Minimum |
|---|---|---|
| M1 (jetzt) | dieser ADR | **90 %** |
| M2 | vollständiger Entscheidungszyklus | **95 %** |

Generierte `data class`-Member werden **mitgezählt** (kein Exclude-Kosmetik) —
die Schwelle trägt der Realität Rechnung statt sie wegzufiltern. Die 90 %-Stufe
lässt bewusst Luft über der generierten-Member-Grundlast, ohne echte
Logik-Lücken zu erlauben (die Logik-Zweige sind vollständig abgedeckt).

## Verglichene Alternativen

- **Kein Gate:** Regressions-Risiko unsichtbar — verworfen.
- **JaCoCo:** funktioniert, aber Kover ist KMP-nativ und passt zu `ADR-0002`.
- **Schwelle sofort 95 %:** unter der aktuellen 94,83 % → würde sofort rot durch
  generierte Member; erst nach Exclude-/Test-Kosmetik tragfähig (M2-Stufe).

## Konsequenzen

- Positiv: Coverage-Regressionen werden im Gate sichtbar (`make gates`).
- Negativ: generierte Member drücken die Zahl; die M2-Stufe erfordert ggf.
  Exclude-Regeln oder `toString`-Tests.
- Folgepflicht: Schwelle in `build.gradle.kts` (`kover { … verify }`),
  Dockerfile-Stage `coverage-gate`, `make coverage-gate` in `make gates`,
  Doku-Promotion in `AGENTS.md` §4 / `harness/README.md`.

## Fitness Function (falls maschinell prüfbar)

| Tooling | Regel | Make-Target |
|---|---|---|
| Kover `koverVerify` | Line-Coverage `hexagon:domain` ≥ Stufen-Minimum | `make coverage-gate` |

## Re-Evaluierungs-Trigger

Hochschaltung auf 95 % bei M2; erneut bewerten, falls generierte-Member-Rauschen
die Aussagekraft dominiert (dann Exclude-Regel dokumentieren).

## Geschichte

| Datum | Ereignis | Verweis |
|---|---|---|
| 2026-07-04 | Proposed — bootstrap-aware Coverage-Gate (90 % → 95 % bei M2), Kover | Review-Nachlauf Welle-01 |
| 2026-07-04 | **Accepted** — Coverage-Schwelle final; ab hier immutable (Korrekturen nur als neue ADR mit `Supersedes ADR-0004`) | Freigabe |
