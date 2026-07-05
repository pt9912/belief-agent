# Welle welle-04-voi-eskalation: Value of Information + Eskalation

**Status:** open

**Zielmeilenstein:** kein direkter Meilenstein (M2 leitet sich aus welle-02..04 ab).

**Verantwortlich:** pt9912. **Datum:** 2026-07-05.

---

## 1. Welle-Ziel

Der Agent handelt bei Unsicherheit nicht blind, sondern **sammelt zuerst
Information** (`LH-FA-VOI-001`): er wählt die günstige Beobachtung, die die zwei
wahrscheinlichsten Hypothesen am stärksten trennt (`LH-FA-VOI-002`), abgewogen
nach Gewinn je Kosten (`LH-FA-VOI-003`), lokal/heuristisch (`LH-FA-VOI-004`).
Sind die günstigen Beobachtungen erschöpft, die Resthypothese hoch **und** das
Gate geschlossen — **oder** ist, davon unabhängig, das Budget erschöpft
(`LH-FA-ESK-004`) —, hält er an und **eskaliert als definierten Zustand**
(`LH-FA-ESK-001`/`002`) mit vollem Kontext: Belief, Evidenz, Grund
(`LH-FA-ESK-003`). Damit schließt sich der **Entscheidungszyklus** (`ARC-09`):
Belief → Gate → (sammeln | handeln | eskalieren).

## 2. Trigger (Welle startet)

- welle-03 done (Belief-Update, Konfidenz-Gate, Aktionen/Wirkungsklassen vorhanden).

## 3. Closure-Trigger (Welle schließt)

- Alle Slices der Welle done.
- `make gates` grün.
- Der Entscheidungszyklus **sammelt** bei hoher Unsicherheit + teurer/irreversibler
  Aktion eine informationssammelnde Beobachtung statt zu handeln (`LH-FA-VOI-001`);
  bei erschöpften Beobachtungen + hoher Resthypothese + geschlossenem Gate —
  **oder**, davon unabhängig, bei erschöpftem Budget (`LH-FA-ESK-004`) —
  **eskaliert** er als definierten Zustand mit Kontext, nicht als Fehler
  (`LH-FA-ESK-001`/`002`/`003`). E2E-Test gegen Fakes.
- Closure-Notiz in `done/welle-04-voi-eskalation-results.md`.

## 4. Slices in dieser Welle

Aufgesetzt 2026-07-05 (`open/`). Zuschnitt nach Lieferwert (Regelwerk Modul 5):

| Slice | Titel | Status | Bezug |
|---|---|---|---|
| `slice-014` | VoI-Selektor (Domäne): informativste Beobachtung wählen | open | `LH-FA-VOI-002`/`003`/`004` |
| `slice-015` | Eskalation-Zustand + Bedingung + Budget (Domäne) | open | `LH-FA-ESK-001`..`004` |
| `slice-016` | Entscheidungszyklus (application): sammeln-statt-handeln + eskalieren | open | `LH-FA-VOI-001` |

## 5. Abhängigkeiten

- Blockiert: den vollen produktiven Entscheidungszyklus / cli-Composition-Root
  und welle-05 (LLM externalisiert VoI-Diskriminierung/Likelihoods).
- Wird blockiert von: welle-02 (Belief/Unsicherheit) und welle-03 (Gate).

## 6. Out-of-Scope für diese Welle

- Echtes LLM (welle-05) — VoI-Diskriminierung und Beobachtungs-Kandidaten kommen
  deterministisch über Fake-Adapter (`LH-QA-03`).
- Global optimale VoI-Policy — bewusst lokal/heuristisch (`LH-FA-VOI-004`).
- Reale Beobachtungs-**Ausführung** (nur Auswahl + Zyklus-Entscheidung).
- Produktiver cli-Composition-Root (`ARC-09`-Verdrahtung; E2E läuft im Testcode).

## 7. Closure-Notiz

<!-- Erst nach Welle-Abschluss füllen; Verweis auf done/welle-04-voi-eskalation-results.md. -->

## 8. Sub-Area-Modus-Begründung

Alle berührten Sub-Areas GF (frisches Repo, Doku führt, Code folgt; keine
Bestandsinventur). Modus-Deklaration siehe
[`../../../harness/conventions.md`](../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
