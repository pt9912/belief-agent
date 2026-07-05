# Slice slice-012: Konfidenz-Gate-Regel (Freigabe/Ablehnung/Eskalation)

**Status:** open → next → in-progress → done (siehe [Planning-README](../README.md)).

**Welle:** [`welle-03-aktionen-gates`](../welle-03-aktionen-gates.md).

**Bezug:** `LH-FA-POL-001`, `LH-FA-POL-002`, `LH-FA-POL-003`, `LH-FA-POL-005`,
`LH-FA-POL-007`, `LH-QA-03`; `ADR-0001`, `ADR-0003`, `ADR-0005`; `ARC-03`.

**Autor:** pt9912. **Datum:** 2026-07-05.

---

## 1. Ziel

Das **Konfidenz-Gate** als reine Domänen-Regel (`hexagon:domain`, `ARC-03`):
Aus der **Erfolgswahrscheinlichkeit** einer `Aktion` (slice-011, `LH-FA-POL-002`
— nicht der Top-Hypothese) und der **wirkungsklassen-abhängigen, konfigurierbaren
Schwelle** (`LH-FA-POL-003`/`007`) ergibt sich eine von drei Entscheidungen:
**Freigabe / Ablehnung / Eskalation** (`LH-FA-POL-001`). Zusätzlich: **Sperre**
extern-wirksamer Aktionen, solange die Resthypothese über ihrer Schwelle liegt
(`LH-FA-POL-005`) — unabhängig davon, wie zugespitzt die Top-Hypothese ist.
Deterministisch (`LH-QA-03`).

## 2. Definition of Done

- [x] `LH-FA-POL-001` erfüllt: `KonfidenzGate.bewerte` liefert genau eine von drei
      `GateEntscheidung` (`Freigabe`/`Ablehnung`/`Eskalation`); `KonfidenzGateTest`
      je Ausgang.
- [x] `LH-FA-POL-002` erfüllt: geprüft wird `aktion.erfolgswahrscheinlichkeit`,
      nicht die Top-Hypothese (Test: niedrige Erfolgs-P → Ablehnung).
- [x] `LH-FA-POL-003`/`007` erfüllt: Mindest-Konfidenz je Wirkungsklasse via
      `GateSchwellen` (konfigurierbar); Default-Werte in `ADR-0005`; nur-lesend
      ohne wirksame Schwelle.
- [x] `LH-FA-POL-005` erfüllt: irreversible Aktion bei Resthypothese > Sperr-
      Schwelle → **Eskalation** (fail-safe, **zuerst** geprüft); Grenzfall
      `==`-Schwelle getestet, hohe Erfolgs-P überstimmt die Sperre **nicht**.
- [x] Kern-lokal, deterministisch (`LH-QA-03`); `make gates` grün (5 Gates;
      88 Tests, Coverage 98,1 %).
- [x] Closure-Notiz (bei Welle-03-Closure).

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/domain/.../KonfidenzGate.kt` | neu | Gate-Regel (`ARC-03`, `LH-FA-POL-001`/`002`/`005`) |
| `hexagon/domain/.../GateEntscheidung.kt` | neu | Freigabe/Ablehnung/Eskalation (sealed) |
| Schwellen-Konfiguration | neu | konfigurierbare Schwellen je Wirkungsklasse (`LH-FA-POL-003`/`007`) |
| `hexagon/domain/.../*Test.kt` | neu | Ausgang-/Schwellen-/Sperr-Tests (`LH-QA-03`) |

## 4. Trigger

`slice-011` done (Aktion/Wirkungsklasse/Erfolgswahrscheinlichkeit vorhanden).
Nutzt Resthypothese/Unsicherheit (slice-004) und `BeliefState` (slice-001).

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz; Datei nach `done/`.

## 6. Risiken und offene Punkte

- **Default-Schwellen sind ADR-pflichtig** (Regelwerk Modul 13, analog `ADR-0004`
  Coverage): die konkreten Schwellwerte je Wirkungsklasse + Resthypothese-Schwelle
  brauchen eine begründete, terminierte ADR-Entscheidung (ggf. `ADR-0005` im Slice).

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** `KonfidenzGate` (Freigabe/Ablehnung/Eskalation), fail-safe-
Ordnung (Resthypothese-Sperre **zuerst**), konfigurierbare `GateSchwellen`,
`ADR-0005`. **Steering-Loop (Kern-Lehre):** das Code-Review fand **zwei
config-erreichbare Safety-Inversionen** (nicht-monotone Schwellen; per `1.0`
abschaltbare Sperre) — jetzt **fail-closed im Konstruktor** erzwungen (nicht
konstruierbar). **Regel geschärft:** Review von Sicherheitsfunktionen ist Pflicht;
Negativ-Tests für böse Configs. **Offen:** —.

## 8. Sub-Area-Modus-Begründung

Alle berührten Sub-Areas GF (frisches Repo, Doku führt; siehe Kurs Modul 5).
Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
