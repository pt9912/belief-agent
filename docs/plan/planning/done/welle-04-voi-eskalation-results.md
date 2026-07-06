# Welle-04 VoI + Eskalation — Ergebnisse

**Abgeschlossen:** 2026-07-06. **Zielmeilenstein:** kein direkter Meilenstein
(M2 leitet sich aus welle-02..04 ab).

Verweis: `welle-04-voi-eskalation` ([Roadmap](../in-progress/roadmap.md)).

---

## Geliefert

| Slice | Deliverable | Anforderungen |
|---|---|---|
| `slice-014` | Domäne: `VoiSelektor` + `VoiKandidat` (informativste Beobachtung, max Gewinn/Kosten über Top-2-Trennung) | `LH-FA-VOI-002`/`003`/`004` |
| `slice-015` | Domäne: `Eskalation` (Zustand + Kontext), `Eskalationsbedingung`, `Budget`; `ADR-0007` | `LH-FA-ESK-001`..`004` |
| `slice-016` | application `beobachtung-waehlen` + `BeobachtungsAuswahlPort` + neues Adapter-Modul `voi-fake` | `LH-FA-VOI-002`, `LH-QA-03` |
| `slice-017` | application `Entscheidungszyklus` (`ARC-09`): sammeln \| handeln \| eskalieren, budget-terminiert | `LH-FA-VOI-001`, `LH-FA-ESK` |

## Closure-Trigger — Nachweis

- ✅ Alle Slices `014`..`017` `done`.
- ✅ `make gates` grün (5 Gates).
- ✅ Der Entscheidungszyklus **sammelt** bei hoher Unsicherheit + irreversibler
  Aktion Information statt zu handeln (`LH-FA-VOI-001`) und **eskaliert** als
  definierten Zustand mit Kontext bei erschöpften Beobachtungen + hoher
  Resthypothese/geschlossenem Gate **oder** — davon unabhängig — erschöpftem Budget
  (`LH-FA-ESK-001`/`002`/`003`/`004`); E2E gegen Fakes (`EntscheidungszyklusTest`,
  7 Fälle inkl. fehlende Freigabe → Eskalation).
- ✅ Diese Closure-Notiz.

## Kennzahlen

- **151 deterministische Tests** (`LH-QA-03`), alle grün.
- **Line-Coverage** `hexagon:domain` 98,17 %, `hexagon:application` 98,73 % (Kover);
  Gate ≥ 90 % (`ADR-0004`/`ADR-0006`).
- **7 Gradle-Module** (domain, application, 5 outbound-Adapter; neu: `voi-fake`).
- `ADR-0007` (Eskalations-Schwelle θ_esc) **Accepted**.

## Architektur-/Prozess-Entscheidungen der Welle

- **Domäne rein rechnend, Schätzung externalisiert:** `VoiSelektor`/`Eskalation`
  sind framework-frei; die VoI-Diskriminierung + Kandidaten trägt der Port/Fake
  (welle-05: LLM, `ADR-0001`).
- **`ARC-09`-Größenschnitt (Modul 5):** der Zyklus war zu groß → `beobachtung-waehlen`
  (Modul-/Build-Risiko zuerst isoliert) + `entscheidungszyklus` (reine
  Orchestrierung); Präzedenz slice-008.
- **Schichtgrenze gewahrt:** die Domäne kennt die application-`Aktionsfreigabe`
  nicht; der Zyklus mappt sie auf die Domänen-`GateEntscheidung` zurück.
- **`ADR-0007`:** θ_esc = 0,30/`≥` spec-konform, **entkoppelt** von der Gate-Sperre
  (`ADR-0005`, 0,5 — anderer Zweck: Eskalation greift *früher* als die Sperre blockt).

## Code-Reviews (rollierend + Ketten)

- **Sequentiell slice-014/015 (Fail-safe):** 5 Befunde — θ_esc un-ADR'd +
  spec-abweichend (→ `ADR-0007`), `schwelle` fail-closed, `Eskalationsgrund` trägt
  `GateEntscheidung`, `VoiSelektor` Kreuz-Multiplikation statt Float-Division.
- **Ketten-Review slice-016/017 (Komposition):** 5 Befunde — fehlende menschliche
  Freigabe wurde still **abgelehnt statt eskaliert** (`LH-FA-POL-004`); **Scheingewissheit**
  durch Wiederholung derselben Beobachtung (`LH-FA-OBS-004`) → Kandidaten-Konsumption;
  `Eskalationsgrund.GateEskalation`; Approval-Pfad-Test; `ARC-09`-Diagramm reconcilt.

## Offene Punkte / Follow-ups

- **Schwellwert-Reconciliation** (`ADR-0007`): `STANDARD_SCHWELLWERT`/θ_rehyp/
  θ_other_block (Code/`ADR-0005` = 0,5/0,5) vs. Spec-Tabelle (0,30/0,10) — eigener
  Slice/ADR.
- **Belief-abhängige Kandidaten-Generierung (F4b):** der Auswahl-Port liefert eine
  feste Menge; der LLM erzeugt Kandidaten belief-abhängig (welle-05).
- **Produktiver cli-Composition-Root** (`ARC-09`-Verdrahtung, welle-05).
- Aus welle-03 weiter offen: Executor darf nur `Aktionsfreigabe.Freigegeben`;
  echter Approval-Adapter mit Binding (welle-05).

## Steering-Loop-Lernen

- **Ketten-Sicht schlägt Einzel-Slice (bestätigt):** die schwersten Befunde
  (fehlende Freigabe still abgelehnt; Scheingewissheit durch Wiederholung) waren
  **Kompositions**-Fehler, unsichtbar in den Einzel-Slice-Reviews — nur das
  Ketten-Review an der Welle-Grenze fand sie.
- **Safety-Schwellen sind ADR-pflichtig + spec-abzugleichen:** θ_esc war eine
  un-ADR'te, spec-abweichende Schwelle auf dem Fail-safe-Pfad (`ADR-0007`); der
  breitere Schwellwert-Drift ist als benannte **Spec-Lücke** getrackt.
- **Fakes können Zustand nicht ersetzen:** ein statischer Kandidaten-Port lässt den
  Zyklus dieselbe Beobachtung mehrfach zählen — Konsumption im Zyklus + belief-
  abhängige Generierung (welle-05) statt „ein Fake modelliert alles".
- **Verhältnisse nie per Division vergleichen:** Kreuz-Multiplikation (Nenner > 0)
  ist robust gegen ULP-Rundung + Overflow.
