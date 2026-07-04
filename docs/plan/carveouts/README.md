# Carveouts — belief-agent

Aktive Carveouts mit Auflösungs-Trigger. Aufgelöste Carveouts wandern nach
`done/` (reiner `git mv`).

## Aktive Carveouts

| ID | Gate | Kurztitel | Auflösungs-Trigger | Letzte Prüfung |
|---|---|---|---|---|
| [CO-001](CO-001-arch-check-a-check.md) | `arch-check` | a-check-KMP-Falsch-negativ, Upstream ausstehend | a-check-Fix oder dokumentiertes KMP-Rezept | 2026-07-04 |

## Aufgelöste Carveouts

(noch keine)

## Konventionen

- Jeder aktive Carveout braucht: Trigger, Folge-Slice, letzten Prüf-Termin.
- Bei Welle-Closure: Carveout-Audit zwingend — welche gültig, welche aufgelöst?
- Vorlage: [`carveout.template.md`](carveout.template.md).
- Siehe Kurs Modul 7.
