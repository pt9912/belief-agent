# Planning — belief-agent

Slice-Lifecycle: `open/` → `next/` → `in-progress/` → `done/`.

Reine `git mv`-Commits beim Wechsel zwischen Verzeichnissen — siehe Hard
Rule „git mv + Inhaltsänderung = zwei Commits" in
[`../../../AGENTS.md`](../../../AGENTS.md).

## Lifecycle-Bedeutungen

| Verzeichnis | Bedeutung |
|---|---|
| `open/` | Geplant, noch nicht priorisiert. Keine Garantie auf Umsetzung. |
| `next/` | Als Nächstes priorisiert. Verantwortlicher zugeordnet. |
| `in-progress/` | Branch / PR existiert. |
| `done/` | DoD erfüllt, gemerged, Closure-Notiz vorhanden. |

## Slices vs. Wellen — zwei Status-Mechanismen

- **Slices** tragen ihren Status über das **Verzeichnis** (open → … → done).
- Eine **Welle** (Bündel von Slices) lebt **ausschließlich in der Roadmap**
  ([`in-progress/roadmap.md`](in-progress/roadmap.md): Meilensteine, Wellen, aktive
  Welle) als **Eintrag** mit Slice-IDs · beobachtbarem Trigger · Closure-Kriterien
  (Regelwerk Modul 6); ihr Status lebt im Prosa-Eintrag, nicht im Verzeichnis. **Es
  gibt keine eigenständigen `<welle-id>.md`-Wellen-Plan-Dateien** — die
  Lifecycle-Verzeichnisse sind **slice-reserviert** (Modul 5). Welle-Closure:
  Lerneintrag in `done/<welle-id>-results.md`. Begründung: `MR-009`.

## Vorlagen

- Slice: [`slice.template.md`](slice.template.md)

(Kein Wellen-Template: Wellen werden als Roadmap-Eintrag geführt, nicht als
eigene Datei — siehe oben und `MR-009`.)

## Aktueller Stand

Der Stand ergibt sich aus den `open/`/`next/`/`in-progress/`/`done/`-
Verzeichnissen, nicht aus einem Snapshot hier. Beim Bootstrap-Ende existiert
noch kein Slice; die erste Welle wird als Eintrag in
[`in-progress/roadmap.md`](in-progress/roadmap.md) geführt.

## Roadmap

Siehe [`in-progress/roadmap.md`](in-progress/roadmap.md).
