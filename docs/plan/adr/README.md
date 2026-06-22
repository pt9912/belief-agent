# ADR-Index — belief-agent

| ID | Titel | Status | Bezug |
|---|---|---|---|
| [0001](0001-hexagonal-llm-port.md) | Hexagonale Architektur — LLM als austauschbarer Port, Entscheidungslogik im Kern | Proposed | `LH-FA-LLM-001` |

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
