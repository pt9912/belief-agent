# ADR-Index — belief-agent

| ID | Titel | Status | Bezug |
|---|---|---|---|
| [0001](0001-hexagonal-llm-port.md) | Hexagonale Architektur — LLM als austauschbarer Port, Entscheidungslogik im Kern | Accepted | [`LH-FA-LLM-001`](../../../spec/lastenheft.md#lh-fa-llm-001--sprachmodell-als-austauschbares-modul) |
| [0002](0002-implementierungssprache-jvm-java.md) | Implementierungssprache und Plattform — Kotlin Multiplatform (JVM-Ziel zuerst) | Accepted | `LH-RB-04` |
| [0003](0003-hexslice-architektur.md) | HexSlice — vertikale Use-Case-Slices im hexagonalen Kern (KMP, Gradle-Multi-Modul) | Accepted | [`LH-QA-04`](../../../spec/lastenheft.md#lh-qa-04--erweiterbarkeit) |
| [0004](0004-coverage-gate.md) | Coverage-Gate — bootstrap-aware Line-Coverage-Schwelle (Kover) | Accepted | [`LH-QA-03`](../../../spec/lastenheft.md#lh-qa-03--testbarkeit) |
| [0005](0005-konfidenz-gate-schwellwerte.md) | Konfidenz-Gate — Default-Schwellwerte je Wirkungsklasse + Resthypothese-Sperre (superseded → `ADR-0008`) | Accepted | [`LH-FA-POL-003`](../../../spec/lastenheft.md#lh-fa-pol-003--schwellen-je-wirkungsklasse) |
| [0006](0006-coverage-gate-scope.md) | Coverage-Gate-Scope — Gate auf application + Adapter erweitern (per Modul) | Accepted | [`LH-QA-03`](../../../spec/lastenheft.md#lh-qa-03--testbarkeit) |
| [0007](0007-eskalations-schwelle.md) | Eskalations-Schwelle θ_esc = 0,30 (spec-konform), entkoppelt von der Gate-Sperre | Accepted | [`LH-FA-ESK-001`](../../../spec/lastenheft.md#lh-fa-esk-001--eskalationsbedingung) |
| [0008](0008-schwellwerte-spec-konform.md) | Gate-/Resthypothese-Schwellwerte spec-konform verschärft (Supersedes `ADR-0005`) | Accepted | [`LH-FA-POL-003`](../../../spec/lastenheft.md#lh-fa-pol-003--schwellen-je-wirkungsklasse) |
| [0009](0009-decision-policy-konfigurationsbuendel.md) | DecisionPolicy — benanntes Konfigurations-Bündel über Gate/Zyklus/VoI, kein neuer Ausführungspfad | Proposed | [`LH-FA-POL-007`](../../../spec/lastenheft.md#lh-fa-pol-007--konfigurierbare-schwellwerte) |
| [0010](0010-auditport-ordnungssemantik.md) | AuditPort-Ordnungssemantik — Append-only am Load vs. Write-Parität ([slice-041](../planning/done/slice-041-dauerhafte-audit-datenbank.md) MEDIUM-1) | Proposed | [`LH-FA-AUD-001`](../../../spec/lastenheft.md#lh-fa-aud-001--unveränderliches-ereignisprotokoll) |

## Konventionen

- ADRs sind nach `Accepted` **immutable** (siehe Kurs Modul 4).
- Schärfungen entstehen als neue ADR mit `Supersedes ADR-NNNN`.
- Bei `Accepted`: diesen Index aktualisieren (Status, Datum).
- Jede ADR deklariert im `**Schärft:**`-Feld *aufwärts*, welche Spec-Stelle
  sie verbindlich macht (Referenz-Richtung: nur volatil → stabil).
  Prozess-ADRs ohne Spec-Stratum tragen `—`.

## Neue ADR anlegen

Vorlage: [`NNNN-titel.template.md`](NNNN-titel.template.md) nach
`<NNNN>-<kurztitel-kebab>.md` kopieren, ausfüllen, hier eintragen.
