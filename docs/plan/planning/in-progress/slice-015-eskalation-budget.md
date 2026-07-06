# Slice slice-015: Eskalation — definierter Zustand + Bedingung + Budget

**Status:** open → next → in-progress → done (siehe [Planning-README](../README.md)).

**Welle:** `welle-04-voi-eskalation` (aktiv, siehe [Roadmap](roadmap.md)).

**Bezug:** `LH-FA-ESK-001`, `LH-FA-ESK-002`, `LH-FA-ESK-003`, `LH-FA-ESK-004`,
`LH-QA-03`; `ADR-0001`, `ADR-0003`; `ARC-05`.

**Autor:** pt9912. **Datum:** 2026-07-05.

---

## 1. Ziel

Die Domänen-Bausteine für Eskalation (`hexagon:domain`, `ARC-05`): ein
**Eskalation-Zustand** als **definiertes, erwartetes Ergebnis** — kein Fehler,
keine Exception (`LH-FA-ESK-002`) — mit vollem **Kontext**: aktueller Belief,
gesammelte Evidenz und **Grund** (welches Gate, welche Schwelle, Stand der
Resthypothese) (`LH-FA-ESK-003`). Eine **Eskalationsbedingung**: günstige
Beobachtungen erschöpft **und** Resthypothese hoch **und** Gate geschlossen
(`LH-FA-ESK-001`). Ein **Budget** (Schritte/Kosten), dessen Erschöpfung
**eigenständig** zur Eskalation führt (`LH-FA-ESK-004`).

## 2. Definition of Done

- [x] `LH-FA-ESK-002`: Eskalation ist ein **definierter Zustand** (kein
      Fehlerstatus/Exception); Test belegt normalen Rückgabewert
      (`eskalation_ist_definierter_zustand_kein_fehler`).
- [x] `LH-FA-ESK-003`: Eskalation trägt **Kontext** — Belief, gesammelte Evidenz,
      Grund (Gate/Schwelle/Resthypothese-Stand); Test prüft alle drei
      (`eskalation_traegt_kontext_belief_evidenz_grund` + `grund_*`).
- [x] `LH-FA-ESK-001`: Bedingung erfüllt **gdw.** Beobachtungen erschöpft **und**
      Resthypothese hoch **und** Gate geschlossen; Test mit jeder Teilbedingung
      einzeln negativ (`EskalationsbedingungTest`, θ_esc=θ_rehyp).
- [x] `LH-FA-ESK-004`: Budget (Schritte/Kosten) → Erschöpfung löst **eigenständig**
      Eskalation aus (getrennter Pfad, nicht an Gate-Bedingung gekoppelt); Test.
- [x] Kern-lokal (`hexagon:domain/eskalation`), framework-frei (`ADR-0001`/`ADR-0003`);
      Coverage domain 98,13 % ≥ 90 % (`ADR-0004`/`ADR-0006`); `make gates` grün.
- [ ] Closure-Notiz (bei Welle-04-Closure).

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/domain/.../eskalation/Eskalation.kt` | neu | definierter Zustand + Kontext (`LH-FA-ESK-002/003`, `ARC-05`) |
| `hexagon/domain/.../eskalation/Eskalationsbedingung.kt` | neu | Bedingung erschöpft ∧ Resthypothese hoch ∧ Gate zu (`LH-FA-ESK-001`) |
| `hexagon/domain/.../eskalation/Budget.kt` | neu | Schritte/Kosten-Budget, eigenständiger Trigger (`LH-FA-ESK-004`) |
| `hexagon/domain/.../eskalation/*Test.kt` | neu | Teilbedingungen negativ, Kontext, Budget, kein Fehler |

## 4. Trigger

welle-04 gestartet (welle-03 done) — Resthypothese-Maß + Gate-Zustand
(`GateEntscheidung`) vorhanden.

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz; Datei nach `done/`. Trägt mit slice-014/016 zum
Welle-04-Closure bei.

## 6. Risiken und offene Punkte

- **Abgrenzung** zur bestehenden `GateEntscheidung.Eskalation` (slice-012) und zum
  `Ereignis` „EskalationAngefordert" (slice-005): die welle-04-`Eskalation` ist der
  **volle Zustands-Kontext**, den der Zyklus produziert; die Gate-Eskalation ist
  nur das Regel-Signal. Auf **einen** kohärenten Eskalations-Begriff achten (nicht
  zwei divergierende) — ggf. Gate-Signal in die Eskalation überführen.
- Budget als eigenständiger Trigger (`LH-FA-ESK-004`) darf **nicht** an die
  Gate-Teilbedingung gekoppelt werden (getrennter Pfad).

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss füllen. -->

## 8. Sub-Area-Modus-Begründung

Neue Sub-Area `hexagon:domain/eskalation` — GF (frisch angelegt, Doku führt, kein
Bestandscode). Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
