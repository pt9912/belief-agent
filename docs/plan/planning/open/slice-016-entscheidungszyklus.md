# Slice slice-016: Entscheidungszyklus — sammeln-statt-handeln + eskalieren

**Status:** open → next → in-progress → done (siehe [Planning-README](../README.md)).

**Welle:** [`welle-04-voi-eskalation`](../welle-04-voi-eskalation.md).

**Bezug:** `LH-FA-VOI-001`, `LH-QA-03`, `LH-QA-04`; `ADR-0001`, `ADR-0003`;
`ARC-04`, `ARC-05`, `ARC-09`.

**Autor:** pt9912. **Datum:** 2026-07-05.

---

## 1. Ziel

Der application-Use-Case **entscheidungszyklus** (`hexagon:application`, `ARC-09`)
verbindet die Bausteine zu **sammeln | handeln | eskalieren**: bei hoher
Unsicherheit **und** teurer/irreversibler Zielaktion sammelt er **zuerst
Information** statt zu handeln (`LH-FA-VOI-001`) — wählt via `VoiSelektor`
(slice-014) die nächste Beobachtung, aktualisiert den Belief (welle-02) und gatet
die Aktion (welle-03); sind die günstigen Beobachtungen erschöpft **oder** ist das
Budget aus, eskaliert er als definierten Zustand mit Kontext (slice-015). Ein
**Beobachtungs-Auswahl-Port** + deterministischer Fake (`LH-QA-03`) liefert die
Kandidaten; E2E im Testcode (produktiver cli-Composition-Root folgt).

## 2. Definition of Done

- [ ] `LH-FA-VOI-001`: bei hoher Unsicherheit + teurer/irreversibler Aktion
      **sammelt** der Zyklus (wählt Beobachtung) statt zu handeln; Test.
- [ ] Zyklus verdrahtet `VoiSelektor` (014) + Belief-Update (welle-02) +
      `AktionGaten` (welle-03) + Eskalation (015) zu **sammeln | handeln |
      eskalieren**; E2E-Test (sammeln-dann-handeln; erschöpft/Budget → eskalieren
      **mit Kontext**).
- [ ] Beobachtungs-Auswahl-Port lokal beim Use-Case + Fake-Adapter; Kern importiert
      keinen Adapter (`arch-check` grün, `ADR-0001`/`ADR-0003`).
- [ ] Coverage ≥ 90 % (application + neuer Adapter, `ADR-0006`); `make gates` grün.
- [ ] Closure-Notiz (bei Welle-04-Closure).

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/application/belief/entscheidungszyklus/**` | neu | Zyklus-Orchestrierung sammeln/handeln/eskalieren (`ARC-09`, `LH-FA-VOI-001`) |
| `hexagon/application/.../ports/BeobachtungsAuswahlPort.kt` | neu | Kandidaten-Quelle für den VoI-Selektor (`ARC-04`/`ARC-07`) |
| `adapters/outbound/voi-fake/**` | neu | deterministischer Kandidaten-Fake (`LH-QA-03`) |
| `settings.gradle.kts`, `Dockerfile`, `.a-check.yml` | update | Adapter-Modul + per-Modul-Coverage-Gate (`ADR-0006`) einhängen |

## 4. Trigger

`slice-014` **und** `slice-015` done (VoI-Selektor + Eskalation vorhanden).

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz; Datei nach `done/`. Erfüllt mit slice-014/015 den
**Welle-04-Closure-Trigger**: der Zyklus sammelt bei Unsicherheit statt zu handeln,
und bei erschöpften Beobachtungen/Budget eskaliert er als definierter Zustand mit
Kontext.

## 6. Risiken und offene Punkte

- **Umfang — Teilungs-Kandidat:** der Zyklus verbindet vier Bausteine (VoI +
  Belief-Update + Gate + Eskalation). Wird slice-016 zu groß (Modul 5, Schnitt nach
  Lieferwert), vorab in `beobachtung-waehlen` (VoI-Use-Case) **+** `entscheidungszyklus`
  (Loop/Eskalation) teilen — Präzedenz slice-008. **Vor Umsetzung Größe prüfen.**
- Ohne cli-Composition-Root läuft der Zyklus E2E-nah im Testcode (wie slice-010/013);
  der produktive `ARC-09`-Zyklus (cli) folgt.
- Abbruch-/Terminierung: der Loop muss durch das Budget (slice-015) garantiert
  terminieren (kein Endlos-Sammeln, `LH-FA-ESK-004`).

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss füllen. -->

## 8. Sub-Area-Modus-Begründung

Neue Sub-Areas `hexagon:application/entscheidungszyklus`, `adapters/outbound/voi-fake`
— GF (frisch angelegt, Doku führt, kein Bestandscode). Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
