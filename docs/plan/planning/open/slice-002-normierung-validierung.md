# Slice slice-002: Normierung + Validierung (Resthypothese-Pflicht, Toleranz)

**Status:** open → next → in-progress → done (Verzeichniswechsel, siehe
[Planning-README](../README.md)).

**Welle:** [`welle-01-belief-kern`](../welle-01-belief-kern.md).

**Bezug:** `LH-FA-BEL-002`, `LH-FA-BEL-004`, `LH-OP-05`, `LH-QA-03`;
`ADR-0001`; `ARC-01`, `ARC-02`.

**Autor:** offen. **Datum:** 2026-07-04.

---

## 1. Ziel

Die Invarianten des `BeliefState` durchsetzen: **Normierung** (Summe der
Wahrscheinlichkeiten = 1 innerhalb einer definierten Toleranz, `LH-FA-BEL-002`)
und **Validierung**, die einen Belief State ohne Resthypothese oder ohne
Normierung **zurückweist** (`LH-FA-BEL-004`). Die Konstruktion eines
ungültigen Belief States ist damit nicht möglich (Abnahme aus `LH-FA-BEL-004`).

## 2. Definition of Done

- [ ] `LH-FA-BEL-002` erfüllt: Normierung mit definierter Toleranz; Test mit
      Grenzwerten (innerhalb/außerhalb Toleranz) referenziert.
- [ ] `LH-FA-BEL-004` erfüllt: ein Belief State ohne Resthypothese **oder**
      ohne Normierung wird nachweislich zurückgewiesen — negativer Test
      referenziert (Closure-Trigger der Welle).
- [ ] Toleranzwert als benannte Konstante/Konfiguration verankert; offener
      Punkt `LH-OP-05` in der Slice-Closure adressiert oder als bleibend
      offen markiert.
- [ ] Validierung ist deterministisch (`LH-QA-03`), Kern-lokal (`ARC-02`),
      framework-frei (`ADR-0001`).
- [ ] `make gates` grün.
- [ ] Closure-Notiz mit Steering-Loop-Lerneintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `src/commonMain/kotlin/**` (Normierung/Validierung) | neu | Invarianten des `BeliefState` (`ARC-01`/`ARC-02`) |
| Toleranz-Konstante/Config | neu | `LH-FA-BEL-002`-Toleranz (`LH-OP-05`) |
| `src/commonTest/kotlin/**` | neu | positive Normierungs- und negative Validierungstests |

## 4. Trigger

`slice-001` done (Domain-Typen vorhanden).

## 5. Closure-Trigger

DoD vollständig + PR gemerged + Closure-Notiz geschrieben; Datei nach
`done/`. Trägt maßgeblich zum Welle-Closure-Trigger bei (ungültiger Belief
State wird nachweislich zurückgewiesen, `LH-FA-BEL-004`).

## 6. Risiken und offene Punkte

- `LH-OP-05` (konkreter Toleranzwert) ist im Lastenheft offen; Slice legt
  einen begründeten Default fest und dokumentiert ihn — keine stille Wahl.
- Update-Logik bewusst **nicht** hier (`slice-003`).

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss füllen. -->

## 8. Sub-Area-Modus-Begründung

Alle berührten Sub-Areas GF (frisches Repo, Doku führt, kein Bestandscode;
siehe Kurs Modul 5 §Worked Mini-Example). Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
