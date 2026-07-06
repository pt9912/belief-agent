# Slice slice-017: Entscheidungszyklus — sammeln-statt-handeln + eskalieren

**Status:** open → next → in-progress → done (siehe [Planning-README](../README.md)).

**Welle:** `welle-04-voi-eskalation` (aktiv, siehe [Roadmap](../in-progress/roadmap.md)).

**Bezug:** `LH-FA-VOI-001`, `LH-QA-02`, `LH-QA-03`, `LH-QA-04`; `ADR-0001`,
`ADR-0003`, `ADR-0006`; `ARC-04`, `ARC-05`, `ARC-09`.

**Autor:** pt9912. **Datum:** 2026-07-06.

---

## 1. Ziel

Der application-Use-Case **entscheidungszyklus** (`hexagon:application`, `ARC-09`)
verbindet die Bausteine zu **sammeln | handeln | eskalieren**: bei hoher
Unsicherheit **und** teurer/irreversibler Zielaktion **sammelt** er zuerst
Information statt zu handeln (`LH-FA-VOI-001`) — nutzt `beobachtung-waehlen`
(slice-016) für die nächste Beobachtung, aktualisiert den Belief (welle-02) und
gatet die Aktion (welle-03); sind die günstigen Beobachtungen erschöpft **oder** ist
das Budget aus, **eskaliert** er als definierten Zustand mit Kontext (slice-015).
E2E im Testcode (produktiver cli-Composition-Root folgt).

## 2. Definition of Done

- [ ] `LH-FA-VOI-001`: bei hoher Unsicherheit + teurer/irreversibler Aktion
      **sammelt** der Zyklus (via `beobachtung-waehlen`, slice-016) statt zu handeln;
      Test.
- [ ] Zyklus verdrahtet `beobachtung-waehlen` (016) + Belief-Update (welle-02) +
      `AktionGaten` (welle-03) + Eskalation (015) zu **sammeln | handeln |
      eskalieren**; erschöpfte Beobachtungen **oder** Budget → **Eskalation mit
      Kontext** (slice-015); **garantierte Terminierung** über das Budget
      (`LH-FA-ESK-004`, `LH-QA-02` fail-safe); E2E-Test gegen Fakes.
- [ ] Coverage ≥ 90 % (application, `ADR-0006`); `make gates` grün.
- [ ] Closure-Notiz (bei Welle-04-Closure).

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/application/belief/entscheidungszyklus/**` | neu | Zyklus-Orchestrierung sammeln/handeln/eskalieren (`ARC-09`, `LH-FA-VOI-001`) |
| `hexagon/application/.../entscheidungszyklus/*Test.kt` | neu | E2E gegen Fakes: sammeln-dann-handeln; erschöpft/Budget → eskalieren mit Kontext; Terminierung |

## 4. Trigger

`slice-014`, `slice-015` **und** `slice-016` done (VoI-Selektor + Eskalation +
`beobachtung-waehlen`-Use-Case/Fake vorhanden).

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz; Datei nach `done/`. Erfüllt mit slice-014/015/016
den **Welle-04-Closure-Trigger**: der Zyklus sammelt bei Unsicherheit statt zu
handeln, und bei erschöpften Beobachtungen/Budget eskaliert er als definierter
Zustand mit Kontext.

## 6. Risiken und offene Punkte

- Reine application-**Orchestrierung** — **kein** neues Modul mehr (Auswahl-Port +
  `voi-fake` kommen aus `slice-016`); Fokus auf Loop-Logik + E2E.
- Ohne cli-Composition-Root läuft der Zyklus E2E-nah im Testcode (wie slice-010/013);
  der produktive `ARC-09`-Zyklus (cli) folgt (spätere Welle).
- Abbruch/Terminierung: der Loop muss durch das Budget (slice-015) garantiert
  terminieren (kein Endlos-Sammeln, `LH-FA-ESK-004`).

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss füllen. -->

## 8. Sub-Area-Modus-Begründung

Neue Sub-Area `hexagon:application/entscheidungszyklus` — GF (frisch angelegt, Doku
führt, kein Bestandscode). Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
