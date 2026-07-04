# Slice slice-007: Ereignisprotokoll + Belief-Rekonstruktion + Audit-Port

**Status:** open → next → in-progress → done (siehe [Planning-README](../README.md)).

**Welle:** [`welle-02-evidenz-audit`](../welle-02-evidenz-audit.md).

**Bezug:** `LH-FA-AUD-001`, `LH-FA-AUD-002`, `LH-FA-AUD-003`, `LH-QA-03`;
`ADR-0001`, `ADR-0003`; `ARC-06`.

**Autor:** pt9912. **Datum:** 2026-07-04.

---

## 1. Ziel

Ein **unveränderliches, geordnetes Ereignisprotokoll** (`LH-FA-AUD-001`) im
Kern: append-only Sequenz von `Ereignis`, aus der der Belief State zu jedem
vergangenen Zeitpunkt **rekonstruierbar** ist (`LH-FA-AUD-002`). Die
Entscheidungsspur ist damit ein auditierbares Protokoll, kein verstecktes
Reasoning (`LH-FA-AUD-003`). Plus der **Audit-Port** (Vertrag zum Persistieren
des Protokolls; Adapter folgt in slice-008/später).

## 2. Definition of Done

- [ ] `LH-FA-AUD-001` erfüllt: `EreignisProtokoll` ist append-only + geordnet;
      Mutation der Vergangenheit ist nicht möglich; Test.
- [ ] `LH-FA-AUD-002` erfüllt: Belief State ist aus dem Protokoll rekonstruierbar
      (Replay der Ereignisse); Test mit erwartetem End-Belief.
- [ ] `LH-FA-AUD-003` erfüllt: die Spur liegt als prüfbares Protokoll vor (nicht
      modellintern) — durch die Typen belegt.
- [ ] Audit-Port als Interface im Core; framework-frei (`ADR-0001`).
- [ ] `make gates` grün.
- [ ] Closure-Notiz.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/domain/.../EreignisProtokoll.kt` | neu | append-only Sequenz (`ARC-06`, `LH-FA-AUD-001`) |
| `hexagon/domain/.../Rekonstruktion.kt` | neu | Replay → Belief (`LH-FA-AUD-002`) |
| Audit-Port (Interface) | neu | Vertrag zum Persistieren (`ARC-07`) |
| `hexagon/domain/.../*Test.kt` | neu | Append-/Replay-Tests (`LH-QA-03`) |

## 4. Trigger

`slice-005` done (Ereignis-Typen vorhanden).

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz; Datei nach `done/`. Trägt maßgeblich zum
Welle-Closure-Trigger bei (Rekonstruierbarkeit).

## 6. Risiken und offene Punkte

- Rekonstruktion braucht denselben deterministischen Update-Pfad wie das
  Live-Update (Wiederverwendung von `BayesUpdate`) — sonst driften Live und
  Replay.

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss füllen. -->

## 8. Sub-Area-Modus-Begründung

Alle berührten Sub-Areas GF (frisches Repo, Doku führt; siehe Kurs Modul 5).
Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
