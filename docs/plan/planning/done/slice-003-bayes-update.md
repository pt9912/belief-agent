# Slice slice-003: Bayes-Update (Posterior ∝ Prior × Likelihood), nicht-überschreibend

**Status:** open → next → in-progress → done (Verzeichniswechsel, siehe
[Planning-README](../README.md)).

**Welle:** `welle-01-belief-kern` ([Ergebnisse](welle-01-belief-kern-results.md)).

**Bezug:** `LH-FA-OBS-003`, `LH-FA-OBS-005`, `LH-QA-03`; `ADR-0001`,
`ADR-0002`, `ADR-0003`; `ARC-01`, `ARC-02`.

**Autor:** pt9912. **Datum:** 2026-07-04.

---

## 1. Ziel

Ein **bayesianisches Belief-Update** als reine Domänen-Regel in
`hexagon:domain` (`ARC-01`; die application-Slice *belief-aktualisieren*,
`ARC-02`, speist die Likelihoods später über den LLM-Port):
`Posterior ∝ Prior × Likelihood` (`LH-FA-OBS-003`), das den bisherigen
Belief **nicht überschreibt/verwirft**, sondern fortschreibt, und das bei
jeder Beobachtung **auch die Resthypothese** über deren Likelihood bewertet
(`LH-FA-OBS-005`). Likelihoods werden als Eingabe entgegengenommen
(Fake-/Test-Likelihoods für Determinismus, `LH-QA-03`) — **kein** LLM in
dieser Welle.

## 2. Definition of Done

- [x] `LH-FA-OBS-003` erfüllt: `BayesUpdate.posterior` = `Posterior ∝ Prior ×
      Likelihood`, nicht-überschreibend (neuer `BeliefState`, Prior
      unverändert); Tests mit bekanntem Prior/Likelihood → erwarteter,
      renormierter Posterior (`BayesUpdateTest`, deterministisch `LH-QA-03`).
- [x] `LH-FA-OBS-005` erfüllt: die Resthypothese erhält eine eigene Likelihood
      und Masse; Test referenziert.
- [x] Ergebnis-Belief gültig und normiert (Wiederverwendung `BeliefState.of`
      aus `slice-002`); `posterior_ist_normiert`-Test.
- [x] Update im Domain-Modul `hexagon:domain`, framework-frei
      (`ADR-0001`/`ADR-0003`), deterministisch (`LH-QA-03`).
- [x] `make gates` grün (`make build`/`make test` im Docker).
- [ ] Closure-Notiz mit Steering-Loop-Lerneintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `src/commonMain/kotlin/**` (Belief-Engine/Update) | neu | Bayes-Update (`ARC-02`) |
| Likelihood-Eingabetyp (Port-nah, aber Kern-Vertrag) | neu | Likelihood je Hypothese inkl. Resthypothese (`LH-FA-OBS-005`) |
| `src/commonTest/kotlin/**` | neu | deterministische Update-Tests mit Fake-Likelihoods (`LH-QA-03`) |

## 4. Trigger

`slice-002`-Validierung geliefert (normierter `BeliefState` erzwungen).
`slice-002` ist `in-progress`; das blockiert `slice-003` nicht (Substrat da).

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

**Was funktionierte:** `BayesUpdate.posterior` nicht-überschreibend, renormiert
via `BeliefState.of` (Posterior per Konstruktion gültig). **Review-Nachlauf:**
Ablehnung unbekannter Likelihood-IDs ergänzt (Befund 2). **Steering-Loop:**
Renormierung an `BeliefState.of` delegiert → keine Doppel-Validierung. **Offen:**
LLM-Speisung der Likelihoods = Welle-05.

## 8. Sub-Area-Modus-Begründung

Alle berührten Sub-Areas GF (frisches Repo, Doku führt, kein Bestandscode;
siehe Kurs Modul 5 §Worked Mini-Example). Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
