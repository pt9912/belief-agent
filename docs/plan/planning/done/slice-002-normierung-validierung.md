# Slice slice-002: Normierung + Validierung (Resthypothese-Pflicht, Toleranz)

**Status:** open → next → in-progress → done (Verzeichniswechsel, siehe
[Planning-README](../README.md)).

**Welle:** [`welle-01-belief-kern`](../welle-01-belief-kern.md).

**Bezug:** `LH-FA-BEL-002`, `LH-FA-BEL-004`, `LH-OP-05`, `LH-QA-03`;
`ADR-0001`, `ADR-0002`, `ADR-0003`; `ARC-01`.

**Autor:** pt9912. **Datum:** 2026-07-04.

---

## 1. Ziel

Die Invarianten des `BeliefState` durchsetzen: **Normierung** (Summe der
Wahrscheinlichkeiten = 1 innerhalb einer definierten Toleranz, `LH-FA-BEL-002`)
und **Validierung**, die einen Belief State ohne Resthypothese oder ohne
Normierung **zurückweist** (`LH-FA-BEL-004`). Die Konstruktion eines
ungültigen Belief States ist damit nicht möglich (Abnahme aus `LH-FA-BEL-004`).

## 2. Definition of Done

- [x] `LH-FA-BEL-002` erfüllt: Normierung im validierenden `BeliefState.of`,
      Toleranz `NORMIERUNGS_TOLERANZ = 1e-9`; Grenzwert-Tests
      (innerhalb/außerhalb) referenziert (`NormierungTest`).
- [x] `LH-FA-BEL-004` erfüllt: ungültiger Belief State (nicht normiert oder
      negative Wahrscheinlichkeit) wird nachweislich zurückgewiesen — negative
      Tests referenziert. (Resthypothese-Pflicht ist bereits strukturell,
      slice-001.)
- [x] Toleranzwert als benannte Konstante verankert (`NORMIERUNGS_TOLERANZ`,
      `1e-9`); `LH-OP-05`-Default begründet dokumentiert (revidierbar).
- [x] Validierung deterministisch (`LH-QA-03`), im Domain-Modul
      `hexagon:domain`, framework-frei (`ADR-0001`/`ADR-0003`).
- [x] `make gates` grün (`make build`/`make test` im Docker).
- [ ] Closure-Notiz mit Steering-Loop-Lerneintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `src/commonMain/kotlin/**` (Normierung/Validierung) | neu | Invarianten des `BeliefState` (`ARC-01`/`ARC-02`) |
| Toleranz-Konstante/Config | neu | `LH-FA-BEL-002`-Toleranz (`LH-OP-05`) |
| `src/commonTest/kotlin/**` | neu | positive Normierungs- und negative Validierungstests |

## 4. Trigger

`slice-001`-Domänentypen geliefert (`hexagon:domain` gebaut/getestet).
`slice-001` ist `in-progress` (formale Closure wartet extern auf `arch-check`/
a-check); das blockiert die Substrat-Nutzung durch `slice-002` nicht.

## 5. Closure-Trigger

DoD vollständig + PR gemerged + Closure-Notiz geschrieben; Datei nach
`done/`. Trägt maßgeblich zum Welle-Closure-Trigger bei (ungültiger Belief
State wird nachweislich zurückgewiesen, `LH-FA-BEL-004`).

## 6. Risiken und offene Punkte

- `LH-OP-05` (konkreter Toleranzwert) ist im Lastenheft offen; Slice legt
  einen begründeten Default fest und dokumentiert ihn — keine stille Wahl.
- Update-Logik bewusst **nicht** hier (`slice-003`).

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** Validierung im `BeliefState.of` (Normierung +
Nicht-Negativität), Toleranz `1e-9` als `LH-OP-05`-Default. **Review-Nachlauf:**
Eindeutigkeit der Hypothesen-IDs ergänzt (Befund 1, `LH-FA-BEL-001` „Menge").
**Steering-Loop:** Domänen-Invarianten am Typ erzwingen — kein ungültiger
Belief State konstruierbar. **Offen:** —

## 8. Sub-Area-Modus-Begründung

Alle berührten Sub-Areas GF (frisches Repo, Doku führt, kein Bestandscode;
siehe Kurs Modul 5 §Worked Mini-Example). Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
