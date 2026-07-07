# Verification-Report: slice-021 — 2026-07-07

**Verifikations-Art:** Slice-Verifikation gegen DoD/Spec.

**Gegenstand:** `slice-021` — Hypothesen-Kandidaten und Übernahme-Regel.

**Regelwerk:** `.harness/baseline/v1.4.0/regelwerk/modul-11-verification.md`.

**Eingangs-Kontext:**

- `docs/plan/planning/in-progress/slice-021-llm-hypothesen-port-fake.md`
- `spec/lastenheft.md` (`LH-FA-BEL-006`, `LH-FA-BEL-007`, `LH-FA-LLM-003`,
  `LH-QA-03`)
- `hexagon/domain/src/commonMain/kotlin/dev/beliefagent/domain/belief/`
- `hexagon/domain/src/commonTest/kotlin/dev/beliefagent/domain/belief/`
- `make gates`

## Verifikation

| DoD / Spec-Anker | Evidenz | Status |
|---|---|---|
| `LH-FA-BEL-006`/`LH-FA-BEL-007`: Kandidaten tragen Hypothesen-ID, Score und Evidenzreferenz | `HypothesenKandidat`, `KandidatenScore`, `EvidenzReferenz`; Tests in `HypothesenKandidatTest`. | erfüllt |
| `LH-FA-LLM-003`: ungültiger oder fehlender Score wird nicht modellimplizit akzeptiert | `KandidatenScore` erzwingt `(0,1]`; `score_muss_explizit_im_gueltigen_intervall_liegen` deckt `0`, negativ, `>1` und `NaN` ab. | erfüllt |
| Fehlende Evidenz macht Kandidaten ungültig | `HypothesenKandidat` verlangt nichtleere `stuetzendeEvidenz`; `kandidat_ohne_evidenzreferenz_ist_ungueltig`. | erfüllt |
| Reine Domänenregel übernimmt neue/verfeinerte Kandidaten in normierten `BeliefState` | `HypothesenraumErweitern.mitKandidaten`; Tests für neue Hypothese, bestehende Hypothese, leere Kandidatenliste und Score-Summe `>1`. | erfüllt |
| Übernommene Evidenz bleibt referenzierbar | `Hypothese.stuetzendeEvidenz`; `posterior_erhaelt_evidenzreferenzen_der_hypothesen` hält Referenzen auch nach Bayes-Update fest. | erfüllt |
| `LH-QA-03` deterministische Tests | `make test` grün; neue Tests sind ohne Zufall, Zeit oder Netz. | erfüllt |
| Repo-Gates | `make gates` grün. | erfüllt |

## Sensors

- `git diff --check` — grün.
- `make test` — grün.
- `make gates` — grün (`doc-check`, `build`, `test`, `coverage-gate`,
  `arch-check`).

## Ergebnis

Keine DoD-Verletzung gefunden. Keine Carveouts.
