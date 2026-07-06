# Slice slice-009: Belief-Update-Pipeline (`belief-aktualisieren`) + LLM-/Uhr-Port + Fake-LLM

**Status:** open → next → in-progress → done (siehe [Planning-README](../README.md)).

**Welle:** `welle-02-evidenz-audit` ([Ergebnisse](welle-02-evidenz-audit-results.md)).

**Bezug:** `LH-FA-OBS-002`, `LH-QA-03`, `LH-QA-04`; `ADR-0001`, `ADR-0003`;
`ARC-02`, `ARC-07`, `ARC-08`.

**Autor:** pt9912. **Datum:** 2026-07-05 (Zerlegung des ursprünglichen slice-008).

---

## 1. Ziel

Die **nachvollziehbare Belief-Update-Pipeline** (`LH-FA-OBS-002`) als
application-Slice `belief-aktualisieren` (`hexagon:application`, `ARC-02`): aus
einer `Beobachtung` werden Likelihoods über den **LLM-Port** geholt (welle-02:
deterministischer **Fake-Adapter**, `LH-QA-03`), korrelierte Evidenz wird
dedupliziert (slice-006), `BayesUpdate` (slice-003) fortgeschrieben und das
Ergebnis samt Ereignissen ins `EreignisProtokoll` (slice-007) geschrieben. Zeit
kommt über einen **Uhr-Port** (Fake in Tests) — kein `Clock` im Kern. Erster
`adapters:*` (Fake-LLM).

## 2. Definition of Done

- [x] `LH-FA-OBS-002` (Kern) erfüllt: `BeliefAktualisieren.ausfuehren` dedupt
      (slice-006) → `LlmPort`-Likelihoods → `BayesUpdate` (slice-003) → Posterior
      + `BeobachtungErfasst`/`BeliefAktualisiert`-Ereignisse; **E2E-nah** gegen
      den echten `FakeLlm` getestet (`FakeLlmTest`), deterministisch (`LH-QA-03`).
- [x] **LLM-Port** lokal beim Use-Case (`…/aktualisieren/ports/LlmPort.kt`);
      Fake-LLM-Adapter `adapters/outbound/llm-fake` (`FakeLlm`) liefert
      deterministische Likelihoods (Evidenz nennt Hypothese → hohe Likelihood).
- [x] **Uhr-Port** (`…/ports/UhrPort.kt`) liefert `Zeitstempel` (Fake-Uhr in
      Tests) — kein `Clock`-Aufruf in Domäne/Application.
- [x] Ports lokal beim Use-Case; Kern importiert keinen Adapter (`arch-check`
      grün **über drei Module**, `ADR-0001`/`ADR-0003`). DI: noch kein
      Composition-Root (cli später) → Konstruktor-Injektion in Tests; Koin folgt
      mit dem Composition-Root.
- [x] `make gates` grün (5 Gates; 67 Tests). Coverage-Gate auf `hexagon:domain`;
      application/adapter sind test-abgedeckt, aber noch nicht gate-scoped
      (Follow-up-ADR, wenn mehr application-Logik da ist).
- [x] Closure-Notiz (bei Welle-02-Closure).

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/application/belief/belief-aktualisieren/**` | neu | Use-Case (command · handler · result) + lokale Ports (`ARC-02`, HexSlice) |
| `hexagon/application/.../ports/` (LLM-, Uhr-Port) | neu | Verträge lokal beim Use-Case (`ARC-07`) |
| `adapters/outbound/llm-fake/**` | neu | deterministischer Fake-LLM (Likelihoods, `LH-QA-03`) |
| `settings.gradle.kts`, `Dockerfile`, `.a-check.yml` | update | neues Adapter-Modul einhängen (auf slice-008-Multi-Modul-Basis) |

## 4. Trigger

`slice-008` done (`hexagon:application`-Modul + Multi-Modul-`arch-check`
vorhanden). Nutzt `slice-003` (BayesUpdate), `slice-006` (Dedup), `slice-007`
(Protokoll).

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz; Datei nach `done/`.

## 6. Risiken und offene Punkte

- Adapter-Modul + DI-Verdrahtung (Koin) am Rand — Umfang beobachten.
- Fake-LLM-Likelihoods müssen deterministisch und begründet gewählt sein
  (`LH-QA-03`), damit Tests stabil sind.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** Use-Case `BeliefAktualisieren` (Dedup → LLM-Port →
`BayesUpdate` → Ereignisse) + lokale Ports (LLM/Uhr); erstes Adapter-Modul
`llm-fake`, E2E-nah im Adapter-Test. **Steering-Loop:** DI/Koin auf den
Composition-Root (cli, welle-03) verschoben — bis dahin Konstruktor-Injektion,
kein Framework im Kern. **Offen (benannte Lücke):** Coverage-Gate ist
domain-only; application/adapter-Logik ist test-abgedeckt, aber nicht
gate-scoped → Coverage-Scope-ADR als Follow-up.

## 8. Sub-Area-Modus-Begründung

Neue Sub-Areas `hexagon:application/belief-aktualisieren`, `adapters/outbound/llm-fake`
— GF (frisch angelegt, Doku führt, kein Bestandscode). Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
