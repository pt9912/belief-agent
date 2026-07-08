# Review-Report: slice-037 Design - 2026-07-08

**Review-Art:** Design - Design-Review gegen Architektur, Layer-Grenzen,
Port-Schnitt und ADR-Vertraeglichkeit.

**Gegenstand:** `docs/plan/planning/open/slice-037-cli-approval-binding.md`

**Skill:** `.harness/baseline/v1.4.0/regelwerk/modul-10-review-harness.md`
@ `v1.4.0` . **Modell:** Codex GPT-5 . **Datum:** 2026-07-08

**Eingangs-Kontext** (die Vertraege, gegen die geprueft wurde):

- `docs/plan/planning/open/slice-037-cli-approval-binding.md`
- `.harness/baseline/v1.4.0/regelwerk/modul-10-review-harness.md`
- `spec/architecture.md`
- `docs/plan/adr/0001-hexagonal-llm-port.md`
- `docs/plan/adr/0003-hexslice-architektur.md`
- `spec/lastenheft.md` zu `LH-FA-POL-004`, `LH-FA-POL-006`, `LH-OUT-04`,
  `LH-QA-02`, `LH-QA-03`, `LH-QA-04`
- `docs/plan/planning/done/slice-036-approval-local-adapter.md`
- bestehende Adapter-/Build-/Arch-Konfiguration:
  `settings.gradle.kts`, `.a-check.yml`, `adapters/inbound/cli`,
  `adapters/outbound/approval-local`
- `AGENTS.md` Hard Rules aus Session-Kontext

---

## Findings

### F-1 - Build-Graph-Kante fehlt im Design-Schnitt

- `kategorie`: MEDIUM
- `quelle`: ADR-0003
- `pfad`: `docs/plan/planning/open/slice-037-cli-approval-binding.md:49`
- `befund`: Der Design-Schnitt macht die erlaubte `a-check`-Kante
  `inbound_cli -> approval-local` sichtbar, aber keine korrespondierende
  Gradle-Modulkante fuer den CLI-Composition-Root. Damit ist die
  ADR-0003-Fitness ueber Gradle-Modulgrenzen im Design nicht vollstaendig
  abgebildet.
- `verifizierbar`: ja - `make build` prueft den Modulgraphen; `make
  arch-check` prueft nur die Architektur-Kanten.

## Negativbefunde

- geprueft, ohne Befund: `ADR-0001`. Der Core bleibt adapterfrei; das geplante
  Binding liegt im Inbound-Composition-Root.
- geprueft, ohne Befund: `ADR-0003` zur HexSlice-Rollenrichtung. Der
  `cli`-Composition-Root darf ausgewaehlte Outbound-Adapter an Ports binden;
  fachliche Adapter-zu-Adapter-Kopplung wird nicht geplant.
- geprueft, ohne Befund: `ARC-08`. `approval-local` bleibt Outbound-Adapter
  hinter `HumanApprovalPort` und wird nicht als Application-Use-Case oder
  Domain-Service modelliert.
- geprueft, ohne Befund: `ARC-09`. Die geplante Aenderung sitzt im
  CLI-Composition-Root und stoesst weiter den bestehenden Entscheidungszyklus
  an.
- geprueft, ohne Befund: Gate-Nicht-Umgehbarkeit. Das Design verschiebt weder
  `KonfidenzGate` noch `AktionGaten`; lokale Freigabe ersetzt keine harte
  Schwelle.
- geprueft, ohne Befund: Executor-Grenze. Der Plan haelt Ausfuehrung an
  `Zyklusergebnis.Gehandelt.freigabe.aktion` gebunden und nennt negative
  CLI-Pfade mit `executed=false`.
- geprueft, ohne Befund: Default-/Fehlerverhalten. Das Design verlangt einen
  bewussten `approval-local`-Modus; Default und falsche/wiederverwendete lokale
  Freigaben bleiben geschlossen.
- geprueft, ohne Befund: Testdesign. Der Plan fordert Runtime-/CLI-Tests fuer
  passenden lokalen Approval-Pfad, fehlende/falsche Freigabe und Wiederverwendung.
- geprueft, ohne Befund: Folgegrenzen. Approval-Audit-Persistenz,
  Remote-/UI-Kanalwahl und neue Ausfuehrungsadapter sind nicht Teil des
  Design-Schnitts.
- geprueft, ohne Befund: `make doc-check` fuer den Review-Artefaktstand.
- geprueft, ohne Befund: `make gates` fuer den Review-Artefaktstand.

## Summary

| Kategorie | Anzahl |
|---|---:|
| HIGH | 0 |
| MEDIUM | 1 |
| LOW | 0 |
| INFO | 0 |

## Verdikt

**Merge-blockierend:** ja - MEDIUM sollte vor Implementation/Promotion
geklaert werden, damit der Architektur- und Build-Graph fuer das CLI-Binding
vollstaendig beschrieben ist.

**Uebergabe:** Design-Handoff mit einem offenen Finding. Der Report ersetzt
keine Verification und keine spaetere Code-/Safety-Review des
Implementationsdiffs.
