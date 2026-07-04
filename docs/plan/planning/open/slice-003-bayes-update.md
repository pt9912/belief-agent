# Slice slice-003: Bayes-Update (Posterior ∝ Prior × Likelihood), nicht-überschreibend

**Status:** open → next → in-progress → done (Verzeichniswechsel, siehe
[Planning-README](../README.md)).

**Welle:** [`welle-01-belief-kern`](../welle-01-belief-kern.md).

**Bezug:** `LH-FA-OBS-003`, `LH-FA-OBS-005`, `LH-QA-03`; `ADR-0001`;
`ARC-02`.

**Autor:** offen. **Datum:** 2026-07-04.

---

## 1. Ziel

Ein **bayesianisches Belief-Update** in der Belief-Engine (`ARC-02`):
`Posterior ∝ Prior × Likelihood` (`LH-FA-OBS-003`), das den bisherigen
Belief **nicht überschreibt/verwirft**, sondern fortschreibt, und das bei
jeder Beobachtung **auch die Resthypothese** über deren Likelihood bewertet
(`LH-FA-OBS-005`). Likelihoods werden als Eingabe entgegengenommen
(Fake-/Test-Likelihoods für Determinismus, `LH-QA-03`) — **kein** LLM in
dieser Welle.

## 2. Definition of Done

- [ ] `LH-FA-OBS-003` erfüllt: Update ist `Posterior ∝ Prior × Likelihood`,
      nicht-überschreibend; Test mit bekanntem Prior/Likelihood → erwarteter,
      wieder normierter Posterior (deterministisch, `LH-QA-03`).
- [ ] `LH-FA-OBS-005` erfüllt: die Resthypothese erhält bei jeder
      Beobachtung eine Likelihood und Masse; Test referenziert.
- [ ] Ergebnis-Belief ist gültig und normiert (Wiederverwendung der
      Validierung aus `slice-002`).
- [ ] Update ist Kern-lokal (`ARC-02`), framework-frei (`ADR-0001`),
      deterministisch bei gegebenen Likelihoods (`LH-QA-03`).
- [ ] `make gates` grün.
- [ ] Closure-Notiz mit Steering-Loop-Lerneintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `src/commonMain/kotlin/**` (Belief-Engine/Update) | neu | Bayes-Update (`ARC-02`) |
| Likelihood-Eingabetyp (Port-nah, aber Kern-Vertrag) | neu | Likelihood je Hypothese inkl. Resthypothese (`LH-FA-OBS-005`) |
| `src/commonTest/kotlin/**` | neu | deterministische Update-Tests mit Fake-Likelihoods (`LH-QA-03`) |

## 4. Trigger

`slice-002` done (gültiger, normierter Belief State + Validierung vorhanden).

## 5. Closure-Trigger

DoD vollständig + PR gemerged + Closure-Notiz geschrieben; Datei nach
`done/`. Erfüllt zusammen mit `slice-002` den Welle-Closure-Trigger
(deterministisch testbares Bayes-Update).

## 6. Risiken und offene Punkte

- Numerik: Rundung/Unterlauf bei kleinen Likelihoods — Toleranz aus
  `slice-002` wiederverwenden, ggf. Log-Raum erwägen (Carveout, falls es
  slice-sprengend wird).
- Dedup korrelierter Evidenz (`LH-FA-OBS-004`) ist **nicht** hier — Welle-02.

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss füllen. -->

## 8. Sub-Area-Modus-Begründung

Alle berührten Sub-Areas GF (frisches Repo, Doku führt, kein Bestandscode;
siehe Kurs Modul 5 §Worked Mini-Example). Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
