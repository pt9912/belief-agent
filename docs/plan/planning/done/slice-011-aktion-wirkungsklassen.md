# Slice slice-011: Domäne — Aktion + Wirkungsklassen + Erfolgswahrscheinlichkeit

**Status:** open → next → in-progress → done (siehe [Planning-README](../README.md)).

**Welle:** `welle-03-aktionen-gates` ([Ergebnisse](welle-03-aktionen-gates-results.md)).

**Bezug:** `LH-FA-ACT-001`, `LH-FA-ACT-002`, `LH-FA-ACT-003`, `LH-FA-ACT-004`,
`LH-QA-03`; `ADR-0001`, `ADR-0003`; `ARC-01`.

**Autor:** pt9912. **Datum:** 2026-07-05.

---

## 1. Ziel

Reine Domänentypen für Aktionen (`hexagon:domain`): jede `Aktion` trägt genau
eine von vier **Wirkungsklassen** (`LH-FA-ACT-001`, eingestuft nach
Seiteneffekt-Reichweite `LH-FA-ACT-002`), eine eigene **Erfolgswahrscheinlichkeit**
`P(Aktion erreicht Ziel)` (`LH-FA-ACT-003`, getrennt vom Belief) und eine
**Referenz auf die stützende Evidenz** (`LH-FA-ACT-004`, Rückverfolgbarkeit). Pur,
framework-frei, deterministisch — das Substrat, auf dem das Gate (slice-012)
aufsetzt.

## 2. Definition of Done

- [x] `LH-FA-ACT-001` erfüllt: `Wirkungsklasse`-Enum (**nur-lesend /
      arbeitsbereich-lokal / repository-wirksam / extern-wirksam**); `AktionTest`.
- [x] `LH-FA-ACT-002` erfüllt: nach Reichweite geordnet (Enum-Ordinal); nur
      `EXTERN_WIRKSAM.irreversibel`, repository-wirksam = reversibler Checkpoint.
- [x] `LH-FA-ACT-003` erfüllt: `Erfolgswahrscheinlichkeit` in `[0,1]` validiert,
      getrennt von der Hypothesen-Wahrscheinlichkeit.
- [x] `LH-FA-ACT-004` erfüllt: `Aktion.stuetzendeEvidenz` (≥1 `Beobachtung`,
      Rückverfolgbarkeit Aktion → Evidenz); leere Evidenz wird abgewiesen.
- [x] Kern-lokal (`hexagon:domain`, `commonMain`), framework-frei, deterministisch
      (`LH-QA-03`); `make gates` grün (5 Gates; 78 Tests, Coverage 97,71 %).
- [x] Closure-Notiz (bei Welle-03-Closure).

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/domain/.../Wirkungsklasse.kt` | neu | 4 Klassen, geordnet nach Reichweite (`LH-FA-ACT-001`/`002`) |
| `hexagon/domain/.../Aktion.kt` | neu | Aktion + Erfolgswahrscheinlichkeit + Evidenz-Ref (`LH-FA-ACT-003`/`004`) |
| `hexagon/domain/.../*Test.kt` | neu | deterministische Typ-Tests (`LH-QA-03`) |

## 4. Trigger

welle-03 Start (welle-02 done). Nutzt `Beobachtung`/`Evidenz` (slice-005) für die
Rückverfolgbarkeit.

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz; Datei nach `done/`.

## 6. Risiken und offene Punkte

- Reichweiten-Ordnung als Enum-Reihenfolge vs. explizite Eigenschaft
  (reversibel/irreversibel) — im Slice entscheiden, deterministisch belegen.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** `Aktion` mit `Wirkungsklasse` (4, nach Reichweite
geordnet, nur extern-wirksam irreversibel), `Erfolgswahrscheinlichkeit` in `[0,1]`
**getrennt** vom Belief, Rückverfolgbarkeit Aktion → Evidenz (≥1 Beobachtung).
**Steering-Loop:** `Wirkungsklasse.irreversibel` als **semantisches** Prädikat am
Enum trug die fail-closed-Gate-Prüfung in beiden Schichten (statt Enum-Vergleich;
Review-Nachlauf slice-013). **Offen:** —.

## 8. Sub-Area-Modus-Begründung

Alle berührten Sub-Areas GF (frisches Repo, Doku führt; siehe Kurs Modul 5).
Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
