# Slice slice-013: aktion-gaten — nicht-umgehbares Gate + Human-Approval-Port

**Status:** open → next → in-progress → done (siehe [Planning-README](../README.md)).

**Welle:** [`welle-03-aktionen-gates`](../welle-03-aktionen-gates.md).

**Bezug:** `LH-FA-POL-004`, `LH-FA-POL-006`, `LH-QA-03`, `LH-QA-04`; `ADR-0001`,
`ADR-0003`; `ARC-03`, `ARC-07`.

**Autor:** pt9912. **Datum:** 2026-07-05.

---

## 1. Ziel

Der application-Use-Case **aktion-gaten** (`hexagon:application`, `ARC-03`) macht
das Konfidenz-Gate (slice-012) zu einem **nicht umgehbaren Schritt** vor jeder
Aktion (`LH-FA-POL-006`): keine Aktion erhält einen Pfad, der das Gate auslässt.
Für **extern-wirksame** Aktionen holt er zusätzlich zur Konfidenz-Schwelle eine
explizite **menschliche Freigabe** über den **Human-Approval-Port**
(`LH-FA-POL-004`) ein, bevor er freigibt; dahinter steht in welle-03 ein
deterministischer **Fake-Approval-Adapter** (`LH-QA-03`).

## 2. Definition of Done

- [x] `LH-FA-POL-006` erfüllt: `AktionGaten.pruefe` ist der einzige Pfad, ruft
      `KonfidenzGate` **immer** und hebt nie eine Nicht-Freigabe an → nicht
      umgehbar; `AktionGatenTest` (Ablehnung bleibt Ablehnung).
- [x] `LH-FA-POL-004` erfüllt: extern-wirksame Freigabe braucht **zusätzlich** die
      menschliche Freigabe über den `HumanApprovalPort`; ohne → Eskalation;
      `FakeApproval`-Adapter (Default verweigert, fail-safe).
- [x] Ports lokal beim Use-Case; Kern importiert keinen Adapter (`arch-check`
      grün über **6 Module**, `ADR-0001`/`ADR-0003`); DI am Adapter-Rand (cli folgt).
- [x] `make gates` grün (5 Gates; 101 Tests).
- [ ] Closure-Notiz (bei Welle-03-Closure).

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/application/belief/aktion-gaten/**` | neu | Use-Case: Gate als Pflichtschritt (`ARC-03`, `LH-FA-POL-006`) |
| `hexagon/application/.../ports/HumanApprovalPort.kt` | neu | Freigabe-Vertrag (`LH-FA-POL-004`, `ARC-07`) |
| `adapters/outbound/approval-fake/**` | neu | deterministischer Fake-Approval-Adapter (`LH-QA-03`) |
| `settings.gradle.kts`, `Dockerfile`, `.a-check.yml` | update | Adapter-Modul einhängen (Muster slice-009/010) |

## 4. Trigger

`slice-011` **und** `slice-012` done (Aktion/Wirkungsklasse + Gate-Regel
vorhanden).

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz; Datei nach `done/`. Erfüllt mit slice-011/012
den **Welle-03-Closure-Trigger** (extern-wirksame Aktion ohne Freigabe / bei
hoher Resthypothese wird abgelehnt/eskaliert).

## 6. Risiken und offene Punkte

- Ohne Composition-Root (cli) läuft die Gate-Orchestrierung E2E-nah im Testcode
  (wie slice-010); der Produktions-`ARC-09`-Zyklus folgt.
- **Review-Nachlauf (welle-03-Gate-Kette, Code-Review):** gefixt — Freigabe-
  Prädikat auf `.irreversibel` (fail-closed für künftige irreversible Klassen);
  verbindlicher, **nur-intern konstruierbarer** `Aktionsfreigabe.Freigegeben`-Typ
  (Provenienz + strukturelle POL-006, nicht via `KonfidenzGate` umgehbar);
  Uhr-Monotonie-Vertrag; Ausgangs-Belief protokolliert (volle Rekonstruierbarkeit,
  `LH-FA-AUD-002`); `Rekonstruktion.zustandNach` (sequenz-genau). **Follow-ups:**
  der künftige **Executor** darf nur `Aktionsfreigabe.Freigegeben` ausführen
  (a-check-Regel bei Bedarf); der **echte Approval-Adapter** (welle-05) muss die
  Freigabe binden/einmal-gültig machen (`HumanApprovalPort`-KDoc).

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss füllen. -->

## 8. Sub-Area-Modus-Begründung

Neue Sub-Areas `hexagon:application/aktion-gaten`, `adapters/outbound/approval-fake`
— GF (frisch angelegt, Doku führt, kein Bestandscode). Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
