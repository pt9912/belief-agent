# Slice slice-007: Ereignisprotokoll + Belief-Rekonstruktion

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
Reasoning (`LH-FA-AUD-003`). Der **Audit-Port** (Persistenz-Vertrag) ist als
**anwendungsweiter Port** nach slice-008 verschoben (Weg C, siehe DoD) — er
gehört in die application-Schicht, nicht in die Domäne.

## 2. Definition of Done

- [x] `LH-FA-AUD-001` erfüllt: `EreignisProtokoll` ist append-only + geordnet
      (monotone Zeitstempel); Rück-Datieren wird abgewiesen, jede Operation
      liefert ein neues Protokoll (Vergangenheit nicht mutierbar);
      `EreignisProtokollTest`.
- [x] `LH-FA-AUD-002` erfüllt: Belief State ist aus dem Protokoll rekonstruierbar
      (`Rekonstruktion.endBelief`/`rekonstruiereBis`, Replay der Ereignisse);
      `RekonstruktionTest` mit erwartetem End-Belief und vergangenem Zustand.
- [x] `LH-FA-AUD-003` erfüllt: die Spur liegt als prüfbares append-only Protokoll
      vor (nicht modellintern) — durch die Typen belegt.
- [x] Kern-lokal (`hexagon:domain`, `commonMain`), framework-frei
      (`ADR-0001`/`ADR-0003`), deterministisch (`LH-QA-03`).
- [x] `make gates` grün (5 Gates; 59 Tests, Line-Coverage 97,37 %).
- [x] Closure-Notiz (bei Welle-02-Closure).

**Umschnitt (Weg C):** Der ursprüngliche DoD-Punkt „Audit-Port als Interface"
ist nach **slice-008** verschoben. Der Audit-Port ist ein **anwendungsweiter
Port** (`hexagon/application/ports/`, Rolle `port`, `ARC-06`), **kein**
Domänentyp — `architecture.md` §2 verbietet der Domain den Import von Ports. Er
landet in slice-008, wo `hexagon:application` gebaut und a-check um die
`port`-Rolle erweitert wird — dort sofort im korrekten Zuhause statt
provisorisch in der Domäne. Das hält slice-007 als reinen domain-Slice
(Regelwerk Modul 5: Schnitt nach Lieferwert, ≤ drei Schichten).

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/domain/.../EreignisProtokoll.kt` | neu | append-only Sequenz (`ARC-06`, `LH-FA-AUD-001`) |
| `hexagon/domain/.../Rekonstruktion.kt` | neu | Replay → Belief (`LH-FA-AUD-002`) |
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

**Was funktionierte:** `EreignisProtokoll` append-only mit monotonen
Zeitstempeln (Rück-Datieren abgewiesen); `Rekonstruktion` als **driftfreier**
Replay über `BeliefAktualisiert`-Snapshots. **Steering-Loop / Entscheidung:**
Weg C — der Audit-Port ist ein **anwendungsweiter Port** (application-Schicht),
kein Domänentyp; nach slice-008 verschoben (`architecture.md` §2: Domain darf
Ports nicht importieren). **Offen:** —.

## 8. Sub-Area-Modus-Begründung

Alle berührten Sub-Areas GF (frisches Repo, Doku führt; siehe Kurs Modul 5).
Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
