# Verification-Report: slice-032 - 2026-07-07

**Verifikations-Art:** Slice-Verifikation gegen DoD/Spec.

**Gegenstand:** `slice-032` - Code-Agent bindet Build/Repo-Beobachtungen.

**Regelwerk:** `.harness/baseline/v1.4.0/regelwerk/modul-11-verification.md`.

**Eingangs-Kontext:**

- `docs/plan/planning/done/slice-032-code-agent-beobachtungsbinding.md`
- `spec/lastenheft.md` (`LH-FA-OBS-001`, `LH-FA-OBS-002`,
  `LH-FA-OBS-006`, `LH-QA-02`, `LH-QA-04`, `LH-FA-POL-006`)
- `spec/architecture.md` (`ARC-08`)
- `example/code-agent/`
- `Makefile`, `Dockerfile`, `.a-check.yml`

## Verifikation

| DoD / Spec-Anker | Evidenz | Status |
|---|---|---|
| Build-/Git-Beobachtungen ersetzen statische Seed-Observations | `Main.kt` erzeugt Beobachtungen ueber `BuildReportBeobachter` und `GitStatusBeobachter` mit `GitStatusQuellenFactory(source=fixture)`. | erfuellt |
| Demo-Ausgabe zeigt Quelle, Zeitstempel, `scenario`, `terminal`, `executed` | `make example-code-agent` und `docker run` geben `observation source=BUILD`, `timestamp=10`, `observation source=REPO`, `timestamp=11`, `scenario=code-agent`, `terminal=eskaliert`, `executed=false` aus. | erfuellt |
| Make-Kontrakt mit Default-Fixtures | `Makefile` setzt `CODE_AGENT_BUILD_FIXTURE` und `CODE_AGENT_REPO_FIXTURE` repo-relativ und reicht beide als Docker-Build-Args weiter. | erfuellt |
| Runtime-Image direkt startbar | `Dockerfile` setzt nicht-leere ENV-Defaults und `ENTRYPOINT`; `docker run --rm belief-agent:example-code-agent` startet erfolgreich. | erfuellt |
| Architekturregeln fuer Example-Bindung | `.a-check.yml` enthaelt Example-Kanten zu `outbound_observation_build_report` und `outbound_observation_git_local` sowie den `example/code-agent`-Root. | erfuellt |
| Produktive Composition-Routen nicht erweitert | Der Diff erweitert `adapters/inbound/cli` nicht; neue Beobachtungsadapter bleiben nur im Example gebunden. | erfuellt |
| README dokumentiert Fixture- und Offline-Kontrakt | `example/code-agent/README.md` nennt Default-Pfade, Overrides, Offline-Annahme und Abgrenzung zu `slice-033`. | erfuellt |
| Gates und Slice-Sensoren | `make example-code-agent`, `docker run --rm belief-agent:example-code-agent`, `make doc-check` und `make gates` laufen gruen. | erfuellt |
| Review- und Verification-Harness-Berichte | Review-Report ohne Findings; dieser Verification-Report ohne DoD-Verletzung. | erfuellt |

## Sensors

- `make example-code-agent` - zunaechst rot wegen repo-relativem Fixture-Pfad im
  Gradle-Modul-Working-Directory; nach Pfadauflosung gegen Repo-Root gruen und
  Image `belief-agent:example-code-agent` erzeugt.
- `docker run --rm belief-agent:example-code-agent` - gruen; Ausgabe enthaelt
  `observation source=BUILD`, `observation source=REPO`, `terminal=eskaliert`,
  `executed=false`.
- `make doc-check` - gruen.
- `make gates` - gruen; `arch-check` meldet `gesamt: 0 Befund(e)`.

## Ergebnis

Keine DoD-Verletzung gefunden. Keine Carveouts.
