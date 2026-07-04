# Welle welle-01-belief-kern: Belief-Kern

**Status:** done

**Zielmeilenstein:** M1 — Belief-Kern lauffähig.

**Verantwortlich:** pt9912. **Datum:** 2026-06-22.

---

## 1. Welle-Ziel

Ein gültiger, normierter **Belief State** über konkurrierende Hypothesen
mit Pflicht-Resthypothese und ein deterministisch testbares
**bayesianisches Belief-Update** (`LH-FA-BEL-001`..`LH-FA-BEL-004`, `LH-FA-OBS-003`,
`LH-QA-03`). Damit existiert das Substrat, auf dem Gate, VoI und Eskalation
der Folge-Wellen aufsetzen.

## 2. Trigger (Welle startet)

- ADR-0001 `Accepted`.
- Implementierungssprache via eigenem ADR entschieden (`LH-RB-04`).

## 3. Closure-Trigger (Welle schließt)

- Alle Slices der Welle done.
- `make gates` grün.
- Ein ungültiger Belief State (keine Resthypothese / nicht normiert) wird
  nachweislich zurückgewiesen (`LH-FA-BEL-004`).
- Closure-Notiz in `done/welle-01-belief-kern-results.md`.

## 4. Slices in dieser Welle

Alle Slices geliefert, reviewt und nach `done/` geschlossen (2026-07-04).
`slice-001` brachte zusätzlich das KMP-Gradle-Multi-Modul-Skelett
(`ADR-0002`/`ADR-0003`). Zuschnitt:

| Slice | Titel | Status | Bezug |
|---|---|---|---|
| `slice-001` | Domain-Typen: Hypothese, Belief State, Resthypothese | done | `LH-FA-BEL-001`, `LH-FA-BEL-003` |
| `slice-002` | Normierung + Validierung (Resthypothese-Pflicht, Toleranz) | done | `LH-FA-BEL-002`, `LH-FA-BEL-004` |
| `slice-003` | Bayes-Update (Posterior ∝ Prior × Likelihood), nicht-überschreibend | done | `LH-FA-OBS-003`, `LH-FA-OBS-005` |
| `slice-004` | Unsicherheitsmaße + Re-Hypothesen-Auslöser | done | `LH-FA-BEL-005`, `LH-FA-BEL-008` |

## 5. Abhängigkeiten

- Blockiert: welle-02 (Evidenz/Audit setzt auf dem Belief State auf).
- Wird blockiert von: Sprach-/Setup-ADR (`LH-RB-04`).

## 6. Out-of-Scope für diese Welle

- Konfidenz-Gate und Wirkungsklassen (welle-03).
- VoI und Eskalation (welle-04).
- LLM-Anbindung (welle-05) — diese Welle nutzt Fake-/Test-Likelihoods für
  Determinismus (`LH-QA-03`).

## 7. Closure-Notiz

Welle-01 abgeschlossen (2026-07-04): gültiger, normierter Belief State +
deterministisches, nicht-überschreibendes Bayes-Update, plus Unsicherheitsmaße
und Re-Hypothesen-Auslöser. 30 deterministische Tests, Line-Coverage 94,83 %
(`ADR-0004`-Gate ≥ 90 %), `make gates` grün. Review durchgeführt (3 Befunde
adressiert). **Rest:** `arch-check` via `CO-001` ausgesetzt (a-check-Antwort
ausstehend). Ergebnisse: `done/welle-01-belief-kern-results.md`.

## 8. Sub-Area-Modus-Begründung

Alle berührten Sub-Areas GF (siehe Kurs Modul 5 §Worked Mini-Example):
frisches Repo, Doku (Lastenheft/Spec/Architektur) führt, Code folgt; keine
Bestandsinventur nötig. Modus-Deklaration siehe
[`../../../harness/conventions.md`](../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
