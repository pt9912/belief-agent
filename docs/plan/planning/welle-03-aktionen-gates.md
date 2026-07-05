# Welle welle-03-aktionen-gates: Aktionen + Konfidenz-Gate

**Status:** in-progress

**Zielmeilenstein:** kein direkter Meilenstein (M2 leitet sich aus welle-02..04 ab).

**Verantwortlich:** pt9912. **Datum:** 2026-07-05.

---

## 1. Welle-Ziel

Jede Aktion wird genau einer von vier **Wirkungsklassen** zugeordnet
(`LH-FA-ACT-001`) und trägt eine eigene **Erfolgswahrscheinlichkeit**
(`LH-FA-ACT-003`) samt stützender Evidenz (`LH-FA-ACT-004`). Davor liegt das
**nicht umgehbare Konfidenz-Gate** (`LH-FA-POL-006`): es gibt eine Aktion frei,
lehnt sie ab oder eskaliert (`LH-FA-POL-001`) — geprüft gegen die
Erfolgswahrscheinlichkeit (`LH-FA-POL-002`) und die wirkungsklassen-abhängige,
konfigurierbare Schwelle (`LH-FA-POL-003`/`007`). Extern-wirksame Aktionen sind
bei hoher Resthypothese gesperrt (`LH-FA-POL-005`) und brauchen zusätzlich eine
explizite **menschliche Freigabe** (`LH-FA-POL-004`). Das ist die **Sicherheits-
funktion** des Systems (`MR-003`).

## 2. Trigger (Welle startet)

- welle-02 done (Belief State, Resthypothese, Evidenz/Beobachtung vorhanden).

## 3. Closure-Trigger (Welle schließt)

- Alle Slices der Welle done.
- `make gates` grün.
- Eine **extern-wirksame** Aktion wird bei hoher Resthypothese **oder** fehlender
  menschlicher Freigabe **nachweislich abgelehnt/eskaliert** (`LH-FA-POL-004`/
  `005`/`006`); das Gate ist nicht umgehbar.
- Closure-Notiz in `done/welle-03-aktionen-gates-results.md`.

## 4. Slices in dieser Welle

Aufgesetzt 2026-07-05 (`open/`). Zuschnitt nach Lieferwert (Regelwerk Modul 5):

| Slice | Titel | Status | Bezug |
|---|---|---|---|
| `slice-011` | Domäne: Aktion + Wirkungsklassen + Erfolgswahrscheinlichkeit | in-progress (geliefert) | `LH-FA-ACT-001`..`004` |
| `slice-012` | Konfidenz-Gate-Regel (Freigabe/Ablehnung/Eskalation, Schwellen, Resthypothese-Sperre) | open | `LH-FA-POL-001`/`002`/`003`/`005`/`007` |
| `slice-013` | aktion-gaten: nicht-umgehbares Gate + Human-Approval-Port | open | `LH-FA-POL-004`/`006` |

## 5. Abhängigkeiten

- Blockiert: welle-04 (VoI/Eskalation setzt auf den Gate-Entscheidungen auf) und
  den vollen Entscheidungszyklus (`ARC-09`, Composition-Root/cli).
- Wird blockiert von: welle-02 (Belief/Resthypothese/Evidenz).

## 6. Out-of-Scope für diese Welle

- VoI-Selektor, Eskalations-Manager, Budget (welle-04).
- LLM-Anbindung (welle-05) — Erfolgswahrscheinlichkeiten/Freigaben kommen
  deterministisch über Fake-Adapter (`LH-QA-03`).
- **Ausführung** extern-wirksamer Aktionen: diese Welle liefert Einstufung +
  Gate + Freigabe-Vertrag, **nicht** die reale externe Aktionsausführung.
- Voller Entscheidungszyklus / cli-Composition-Root (`ARC-09`, welle-03-Folge).

## 7. Closure-Notiz

<!-- Erst nach Welle-Abschluss füllen; Verweis auf done/welle-03-aktionen-gates-results.md. -->

## 8. Sub-Area-Modus-Begründung

Alle berührten Sub-Areas GF (frisches Repo, Doku führt, Code folgt; keine
Bestandsinventur). Modus-Deklaration siehe
[`../../../harness/conventions.md`](../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
