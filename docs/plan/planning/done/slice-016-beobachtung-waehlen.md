# Slice slice-016: beobachtung-waehlen — VoI-Use-Case + Auswahl-Port + Fake

**Status:** open → next → in-progress → done (siehe [Planning-README](../README.md)).

**Welle:** `welle-04-voi-eskalation` ([Ergebnisse](welle-04-voi-eskalation-results.md)).

**Bezug:** `LH-FA-VOI-002`, `LH-QA-03`; `ADR-0001`, `ADR-0003`, `ADR-0006`;
`ARC-04`, `ARC-07`.

**Autor:** pt9912. **Datum:** 2026-07-06.

---

## 1. Ziel

Der application-Use-Case **beobachtung-waehlen** (`hexagon:application`, `ARC-04`)
wählt die informativste nächste Beobachtung über einen **Beobachtungs-Auswahl-Port**:
der Port liefert die `VoiKandidat`-Kandidaten (welle-05: LLM; welle-04:
deterministischer Fake, `LH-QA-03`), der Use-Case wendet den `VoiSelektor`
(slice-014) an und liefert die Wahl — oder das Signal **„keine günstige
Beobachtung"** (leere Kandidaten → Eskalations-Kandidat im Zyklus, slice-017). Ein
neues Outbound-Adapter-Modul `voi-fake` implementiert den Port.

Dieser Schnitt **isoliert bewusst das Multi-Modul-/Arch-/Build-Risiko** (neues
Adapter-Modul verdrahten) **vor** der Zyklus-Orchestrierung (slice-017) — Präzedenz
slice-008/009.

## 2. Definition of Done

- [x] `LH-FA-VOI-002`: `beobachtung-waehlen` liefert aus den Port-Kandidaten die
      `VoiSelektor`-Wahl bzw. **„keine"** (`null`) bei leeren Kandidaten; Test
      (`BeobachtungWaehlenTest` + Fake-Determinismus `FakeKandidatenquelleTest`, `LH-QA-03`).
- [x] **Beobachtungs-Auswahl-Port** lokal beim Use-Case + deterministischer
      `voi-fake`-Adapter (`FakeKandidatenquelle`); Kern importiert keinen Adapter
      (`arch-check` grün — 0 Befunde, `ADR-0001`/`ADR-0003`).
- [x] Neues Adapter-Modul `adapters:outbound:voi-fake` verdrahtet
      (`settings.gradle.kts`, `Dockerfile` deps/koverLog/koverVerify, `.a-check.yml`-Root
      `MR-005`, per-Modul-Coverage `ADR-0006`); Coverage application 100 % / voi-fake
      100 % ≥ 90 %; `make gates` grün.
- [ ] Closure-Notiz (bei Welle-04-Closure).

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/application/belief/beobachtung-waehlen/**` | neu | VoI-Use-Case: Port → `VoiSelektor` → Wahl (`ARC-04`, `LH-FA-VOI-002`) |
| `hexagon/application/.../ports/BeobachtungsAuswahlPort.kt` | neu | Kandidaten-Quelle für den VoI-Selektor (`ARC-07`) |
| `adapters/outbound/voi-fake/**` | neu | deterministischer Kandidaten-Fake (`LH-QA-03`) |
| `settings.gradle.kts`, `Dockerfile`, `.a-check.yml` | update | Adapter-Modul + per-Modul-Coverage-Gate einhängen (`ADR-0006`, `MR-005`) |

## 4. Trigger

`slice-014` done (VoI-Selektor + `VoiKandidat` vorhanden).

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz; Datei nach `done/`. Liefert die Auswahl-Fähigkeit,
auf der `slice-017` (Entscheidungszyklus) aufbaut.

## 6. Risiken und offene Punkte

- Neues Adapter-Modul = Multi-Modul-`arch-check` (`MR-005`) + per-Modul-Coverage +
  Dockerfile-Stage: das bekannte Muster (slice-009/010), hier **isoliert vor** der
  Orchestrierung.
- `VoiKandidat`-Kosten (slice-014) und das Budget (slice-015) treffen erst im Zyklus
  (`slice-017`) aufeinander; hier nur die Auswahl selbst.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** erstes application-Slice + neues Outbound-Adapter-Modul
`voi-fake` in einem Zug verdrahtet (settings/Dockerfile/`.a-check.yml`); Multi-Modul-
`arch-check` über 7 Module grün, per-Modul-Coverage 100 %. **Steering-Loop:** der
`ARC-09`-Schnitt (Modul 5) — **zuerst das Modul-/Build-Risiko isolieren**, dann die
Orchestrierung — hat sich bewährt (Präzedenz slice-008). **Regel bestätigt:** neues
Adapter-Modul zuerst schneiden; application-Test hängt via In-Test-Port-Stub **nicht**
am Adapter (Abhängigkeitsrichtung nach innen).

## 8. Sub-Area-Modus-Begründung

Neue Sub-Areas `hexagon:application/beobachtung-waehlen`, `adapters/outbound/voi-fake`
— GF (frisch angelegt, Doku führt, kein Bestandscode). Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
