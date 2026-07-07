# Verification-Report: slice-029 - 2026-07-07

**Verifikations-Art:** Slice-Verifikation gegen DoD/Spec.

**Gegenstand:** `slice-029` - Beispiele mit CLI-Composition-Root reconciliieren.

**Regelwerk:** `.harness/baseline/v1.4.0/regelwerk/modul-11-verification.md`.

**Eingangs-Kontext:**

- `docs/plan/planning/in-progress/slice-029-example-reconciliation-cli-root.md`
- `spec/lastenheft.md` (`LH-FA-LLM-002`, `LH-FA-LLM-004`,
  `LH-FA-POL-006`, `LH-OUT-04`, `LH-QA-03`, `LH-QA-04`)
- `spec/architecture.md`
- `example/langchain/`
- `example/koog/`
- `make example-langchain`, `make example-koog`, `make doc-check`

## Verifikation

| DoD / Spec-Anker | Evidenz | Status |
|---|---|---|
| Beide READMEs nennen `adapters:inbound:cli` als produktiven Einstieg | `example/langchain/README.md` und `example/koog/README.md` nennen `adapters:inbound:cli` und `make cli-demo`. | erfuellt |
| Beide Beispiel-Runs zeigen die Executor-Grenze ueber `freigabe.aktion` | `make example-langchain` und `make example-koog` geben `executor_boundary=Zyklusergebnis.Gehandelt.freigabe.aktion` aus. | erfuellt |
| Beispiele importieren keinen CLI-Adapter und duplizieren den Root nicht | Code bleibt bei direkter Demo-Verdrahtung fuer `LlmPort`/Framework-Grenze; keine `adapters:inbound:cli`-Dependency in `example/*/build.gradle.kts`. | erfuellt |
| `LH-FA-LLM-002`/`LH-FA-LLM-004`: Framework-Beispiele bleiben abgegrenzte LLM-Aufgabe | Beispiele demonstrieren Likelihood-Schaetzung hinter `LlmPort`, nicht Aktionsausfuehrung oder Composition-Root. | erfuellt |
| `LH-FA-POL-006`/`LH-OUT-04`: kein Bypass-Versprechen in Beispielen | README und Ausgabe binden Ausfuehrung an `Zyklusergebnis.Gehandelt.freigabe.aktion`; negative Ergebnisse bleiben geschlossen. | erfuellt |
| Sensoren fuer Beispiele | `make example-langchain`, `make example-koog` und `make doc-check` gruen. | erfuellt |
| Closure-Notiz mit Steering-Loop-Eintrag | Slice-Datei enthaelt Closure-Notiz mit Steering-Loop-Eintrag vor der Verschiebung nach `done/`. | erfuellt |

## Sensors

- `git diff --check` - gruen.
- `make doc-check` - gruen (`d-check: 76 Datei(en) geprueft, 0 Befund(e)`).
- `make example-langchain` - gruen; Ausgabe enthaelt
  `production_composition_root=adapters:inbound:cli` und
  `executor_boundary=Zyklusergebnis.Gehandelt.freigabe.aktion`.
- `make example-koog` - gruen; Ausgabe enthaelt
  `production_composition_root=adapters:inbound:cli` und
  `executor_boundary=Zyklusergebnis.Gehandelt.freigabe.aktion`.

## Ergebnis

Keine DoD-Verletzung gefunden. Keine Carveouts.
