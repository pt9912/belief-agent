# Slice slice-004: Unsicherheitsmaße + Re-Hypothesen-Auslöser

**Status:** open → next → in-progress → done (Verzeichniswechsel, siehe
[Planning-README](../README.md)).

**Welle:** [`welle-01-belief-kern`](../welle-01-belief-kern.md).

**Bezug:** `LH-FA-BEL-005`, `LH-FA-BEL-008`, `LH-QA-03`; `ADR-0001`,
`ADR-0002`, `ADR-0003`; `ARC-01`, `ARC-02`.

**Autor:** pt9912. **Datum:** 2026-07-04.

---

## 1. Ziel

Unsicherheitsmaße über den `BeliefState` bereitstellen (`LH-FA-BEL-008`:
z. B. Entropie, Abstand der zwei wahrscheinlichsten Hypothesen) und den
**Re-Hypothesen-Auslöser** definieren: Überschreitet die Resthypothese einen
**konfigurierbaren Schwellwert**, stößt das System die Erzeugung neuer bzw.
verfeinerter Hypothesen an (`LH-FA-BEL-005`). Der Auslöser meldet den Bedarf
als Kern-Signal; die *inhaltliche* Hypothesen-Erzeugung (über den LLM-Port)
ist Sache späterer Wellen.

## 2. Definition of Done

- [x] `LH-FA-BEL-008` erfüllt: **zwei** Maße bereitgestellt —
      `BeliefState.entropie()` (Shannon, nats) und `top2Abstand()`; Tests mit
      bekannten Verteilungen (`UnsicherheitTest`, deterministisch `LH-QA-03`).
- [x] `LH-FA-BEL-005` erfüllt: `ReHypothesenAusloeser.ausgeloest(belief)`
      feuert bei Resthypothese echt über Schwellwert; Tests unter/über/genau
      (`ReHypothesenAusloeserTest`).
- [x] Schwellwert konfigurierbar (`ReHypothesenAusloeser(schwellwert)`,
      Default `STANDARD_SCHWELLWERT = 0.5` begründet dokumentiert, `[0,1]`
      validiert).
- [x] Maße/Auslöser im Domain-Modul `hexagon:domain`, framework-frei
      (`ADR-0001`/`ADR-0003`), deterministisch (`LH-QA-03`).
- [x] `make gates` grün (`make build`/`make test` im Docker).
- [ ] Closure-Notiz mit Steering-Loop-Lerneintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `src/commonMain/kotlin/**` (Unsicherheitsmaße) | neu | Entropie / Top-2-Abstand (`ARC-02`, `LH-FA-BEL-008`) |
| `src/commonMain/kotlin/**` (Re-Hypothesen-Auslöser) | neu | Schwellwert-Signal (`LH-FA-BEL-005`) |
| Schwellwert-Konstante/Config | neu | konfigurierbarer Resthypothesen-Schwellwert |
| `src/commonTest/kotlin/**` | neu | deterministische Maß-/Schwellwert-Tests (`LH-QA-03`) |

## 4. Trigger

`slice-003`-Bayes-Update geliefert (fortschreibbarer Belief State inkl.
Resthypothesen-Masse). `slice-003` ist `in-progress`; blockiert `slice-004`
nicht.

## 5. Closure-Trigger

DoD vollständig + PR gemerged + Closure-Notiz geschrieben; Datei nach
`done/`. Letzter Slice der Welle → danach Welle-Closure-Notiz in
`done/welle-01-belief-kern-results.md`.

## 6. Risiken und offene Punkte

- Nur das **Signal** wird erzeugt; die tatsächliche Hypothesen-Erzeugung
  über den LLM-Port ist Out-of-Scope dieser Welle (Welle-05).
- `LH-FA-BEL-008` ist `Prio: Soll` — bei Zeitdruck ist der Re-Hypothesen-
  Auslöser (`Muss`) vorrangig.

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss füllen. -->

## 8. Sub-Area-Modus-Begründung

Alle berührten Sub-Areas GF (frisches Repo, Doku führt, kein Bestandscode;
siehe Kurs Modul 5 §Worked Mini-Example). Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
