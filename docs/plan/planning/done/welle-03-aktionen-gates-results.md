# Welle-03 Aktionen + Konfidenz-Gate — Ergebnisse

**Abgeschlossen:** 2026-07-05. **Zielmeilenstein:** kein direkter Meilenstein
(M2 leitet sich aus welle-02..04 ab).

Verweis: `welle-03-aktionen-gates` ([Roadmap](../in-progress/roadmap.md)).

---

## Geliefert

| Slice | Deliverable | Anforderungen |
|---|---|---|
| `slice-011` | Domäne: `Aktion` + 4 `Wirkungsklasse`n + `Erfolgswahrscheinlichkeit` + Evidenz-Ref | `LH-FA-ACT-001`..`004` |
| `slice-012` | `KonfidenzGate`-Regel (Freigabe/Ablehnung/Eskalation, Schwellen, Resthypothese-Sperre) + `ADR-0005` | `LH-FA-POL-001`/`002`/`003`/`005`/`007` |
| `slice-013` | `aktion-gaten`: nicht-umgehbares Gate + `HumanApprovalPort` + Fake-Adapter | `LH-FA-POL-004`/`006` |

## Closure-Trigger — Nachweis

- ✅ Alle Slices `011`..`013` `done`.
- ✅ `make gates` grün (5 Gates).
- ✅ Eine extern-wirksame (irreversible) Aktion wird bei hoher Resthypothese
  **oder** fehlender menschlicher Freigabe **nachweislich abgelehnt/eskaliert**
  (`AktionGatenTest`, `FakeApprovalTest`-E2E; `LH-FA-POL-004`/`005`/`006`); das
  Gate ist nicht umgehbar.
- ✅ Diese Closure-Notiz.

## Kennzahlen

- **102 deterministische Tests** (`LH-QA-03`), alle grün.
- **Line-Coverage `hexagon:domain` 97,65 %** (Kover); Gate ≥ 90 % (`ADR-0004`).
- **7 Gradle-Module** (domain, application, 5 outbound-Adapter).
- `ADR-0005` (Gate-Schwellwerte) **Accepted**.

## Architektur-/Prozess-Entscheidungen der Welle

- Trennung **Domänen-Regel** (`KonfidenzGate`, Konfidenz) ↔ **application-
  Freigabe** (`AktionGaten` + menschliche Freigabe); die verbindliche
  `Aktionsfreigabe.Freigegeben` ist nur intern konstruierbar (strukturelle
  Nicht-Umgehbarkeit, `LH-FA-POL-006`).
- `Wirkungsklasse.irreversibel` als **semantisches** Prädikat in **beiden**
  Schichten (fail-closed für künftige irreversible Klassen).
- `ADR-0005` Accepted nach Code-Review (Schwellen-Invarianten fail-closed).

## Code-Reviews (zwei Runden, Multi-Agent)

- **slice-012:** zwei config-erreichbare **Safety-Inversionen** in `GateSchwellen`
  (nicht-monotone Schwellen; per `1.0` abschaltbare Sperre) → im Konstruktor
  fail-closed nicht mehr konstruierbar.
- **welle-03-Kette + welle-02 retrospektiv:** 7 Befunde — u. a. **fail-open**-
  Prädikat (`== EXTERN_WIRKSAM`), strukturelle `LH-FA-POL-006`, welle-02
  Uhr-Monotonie-Vertrag und Ausgangs-Belief-Rekonstruierbarkeit (`LH-FA-AUD-002`)
  → alle gefixt.

## Offene Punkte / Follow-ups

- **Executor (spätere Welle):** darf nur `Aktionsfreigabe.Freigegeben` ausführen
  (nie `KonfidenzGate` direkt) — ggf. a-check-Regel.
- **Echter Approval-Adapter (welle-05):** Freigabe binden/einmal-gültig machen.
- **Coverage-Scope-ADR** (application/adapter gate-scopen) — offen aus welle-02.
- **Lifecycle-„Buffet"** (`MR-007`): delivered Slices bis Welle-Closure gebündelt
  in `in-progress/` — Konventions-Entscheidung weiter offen.

## Steering-Loop-Lernen

- **Reviews von Sicherheitsfunktionen an die Welle-Grenze legen:** die
  **Ketten-Sicht** (Aktion → Gate → Freigabe) fand ein fail-open, das die
  Einzel-Slice-Reviews strukturell nicht zeigen konnten.
- **Prädikat-Konsistenz über Schichten:** dasselbe semantische Prädikat
  (`.irreversibel`) in Domäne **und** Application; ein Enum-Vergleich in nur einer
  Schicht ist ein fail-open-Footgun.
- **Fail-closed by construction:** `GateSchwellen`/`Aktionsfreigabe` machen
  unsichere Zustände **nicht konstruierbar** — stärker als Tests allein.
- **Negativ-Test-Pflicht** (aus welle-02 bestätigt): config-erreichbare
  Safety-Inversionen sind nur über explizite Negativ-Tests + fail-closed-
  Konstruktoren fangbar.
