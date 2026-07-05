# Slice slice-006: Dedup korrelierter Beobachtungen

**Status:** open → next → in-progress → done (siehe [Planning-README](../README.md)).

**Welle:** [`welle-02-evidenz-audit`](../welle-02-evidenz-audit.md).

**Bezug:** `LH-FA-OBS-004`, `LH-QA-03`; `ADR-0001`, `ADR-0003`; `ARC-01`, `ARC-02`.

**Autor:** pt9912. **Datum:** 2026-07-04.

---

## 1. Ziel

Korrelierte oder redundante Beobachtungen erkennen und **nicht** als
unabhängige Evidenz mehrfach zählen (`LH-FA-OBS-004`, Dedup gegen
Scheingewissheit). Reine Domänen-Regel in `hexagon:domain`: eine Menge von
Beobachtungen wird auf unabhängige Evidenz reduziert, bevor sie ins Update geht.

## 2. Definition of Done

- [x] `LH-FA-OBS-004` erfüllt: identische/korrelierte Beobachtungen werden
      dedupliziert (gleiche `Quelle` + `Evidenz`, Zeitstempel-unabhängig);
      `DedupTest` referenziert Duplikat **und** Korrelat.
- [x] Deterministisch (`LH-QA-03`), Kern-lokal (`Dedup` in `hexagon:domain`,
      `commonMain`), framework-frei (`ADR-0001`/`ADR-0003`).
- [x] `make gates` grün (5 Gates; 46 Tests, Line-Coverage 96,81 %).
- [x] Closure-Notiz (bei Welle-02-Closure).

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/domain/.../Dedup.kt` (o. ä.) | neu | Dedup-Regel (`ARC-02`, `LH-FA-OBS-004`) |
| `hexagon/domain/.../*Test.kt` | neu | Duplikat-/Korrelat-Tests (`LH-QA-03`) |

## 4. Trigger

`slice-005` done (Beobachtungs-Typ vorhanden).

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz; Datei nach `done/`.

## 6. Risiken und offene Punkte

- Korrelations-Kriterium: für welle-02 ein einfaches, deterministisches
  Kriterium (z. B. gleiche Quelle+Signatur); komplexere Korrelationsmodelle
  sind Out-of-Scope (Folge-Slice bei Bedarf).

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** Dedup-Signatur = `Quelle` + `Evidenz` (Zeitstempel bewusst
ausgeklammert) — idiomatisch via `distinctBy`, reihenfolge-stabil. **Steering-
Loop:** Die `Signatur` als benannter Typ macht das Korrelations-Kriterium
explizit und testbar. **Offen:** komplexere Korrelationsmodelle (Folge-Slice bei
Bedarf).

## 8. Sub-Area-Modus-Begründung

Alle berührten Sub-Areas GF (frisches Repo, Doku führt; siehe Kurs Modul 5).
Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
