# Carveouts — belief-agent

Aktive Carveouts mit Auflösungs-Trigger. Aufgelöste Carveouts wandern nach
`done/` (reiner `git mv`).

## Aktive Carveouts

(keine)

## Aufgelöste Carveouts

| ID | Gate | Kurztitel | Aufgelöst | Wie |
|---|---|---|---|---|
| [CO-001](done/CO-001-arch-check-a-check.md) | `arch-check` | a-check-KMP-Falsch-negativ | 2026-07-04 | a-check v0.10.0 fail-closed-Guard + KMP-Rezept; `arch-check` verdrahtet + grün |

## Konventionen

- Jeder aktive Carveout braucht: Trigger, Folge-Slice, letzten Prüf-Termin.
- Bei Welle-Closure: Carveout-Audit zwingend — welche gültig, welche aufgelöst?
- Vorlage: [`carveout.template.md`](carveout.template.md).
- Siehe Kurs Modul 7.
