# Slice slice-022: Konfidenz-Externalisierung

**Status:** open → next → in-progress → done (siehe
[Planning-README](../README.md)).

**Welle:** Roadmap-Follow-up zu `welle-05-llm-port`
([Roadmap](../in-progress/roadmap.md)); bei aktiver Umsetzung Welle explizit
wieder öffnen oder als gezielten Follow-up-Slice starten.

**Bezug:** `LH-FA-LLM-003`, `LH-FA-AUD-001`, `LH-FA-AUD-003`,
`LH-QA-04`; `ADR-0001`, `ADR-0003`; `ARC-06`, `ARC-08`, `ARC-09`.

**Autor:** pt9912. **Datum:** 2026-07-06.

---

## 1. Ziel

Modell-Kennzahlen aus LLM-Interaktionen sollen nicht implizit im
Entscheidungsfluss verweilen. Dieser Slice liefert den kleinsten Kern dafür:
Business-Area-Contract, Externalisieren-/Override-Use-Case und append-only
Audit-Ereignisse. Adapter, Golden-Set-Replay und Zyklus-/Gate-Bindung folgen
in separaten Slices.

## 2. Definition of Done

- [x] `LH-FA-LLM-003` erfüllt: rohe Modell-Konfidenz wird in explizite,
  protokollierbare Contract-Typen mit stabiler Referenz überführt.
- [x] Overrides erzeugen ein neues Audit-Ereignis und mutieren keinen bestehenden
  Eintrag (`LH-FA-AUD-001`, `LH-FA-AUD-003`).
- [x] `LH-QA-04` erfüllt: der Konfidenz-Contract liegt business-area-geteilt
  unter `hexagon/application/belief/ports`, sodass `slice-023` ihn konsumieren
  kann, ohne use-case-lokale DTOs zu importieren.
- [x] Deterministische Application-Tests decken Externalisierung, Override und
  Ablehnung ungueltiger Konfidenzwerte ab.
- [x] `make gates` grün.
- [x] Closure-Notiz mit Steering-Loop-Eintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/ports/ExternalisierteKonfidenz.kt` | neu | Gemeinsame Contract-Typen/Value-Types für externalisierte Modell-Konfidenz; nicht unter einem lokalen Use-Case-Pfad, damit `slice-023` keine fremden lokalen DTOs importiert |
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/ports/KonfidenzPort.kt` | neu | Business-Area-geteilter Vertrag zur Externalisierung (`ARC-07`/`ARC-08`) |
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/konfidenzexternalisieren/KonfidenzExternalisieren.kt` | neu | Use-Case für Rohwert-Mapping, stabile Referenz und Override |
| `hexagon/domain/src/commonMain/kotlin/dev/beliefagent/domain/belief/Ereignis.kt` | update | Audit-Ereignisse für externalisierte Konfidenz und Override append-only modellieren |
| `hexagon/application/src/commonTest/.../konfidenzexternalisieren/` | neu | Deterministische Tests für Externalisierung, Override und ungueltige Rohwerte |

## 4. Trigger

Roadmap-Follow-up aus `welle-05-llm-port` offen. `slice-021`, `slice-025`
und `slice-026` liegen in `done/`; der Hypothesen-LLM-Pfad hat damit bereits
explizite Scores und zeigt den Bedarf für einen allgemeinen
Konfidenz-Contract.

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz + Slice in `done/`.

## 6. Risiken und offene Punkte

- Überschreibbarkeit braucht klare Governance, sonst verwischt die
  Verantwortung zwischen LLM-Port und Anwendungslogik.
- Override ohne neues Audit-Ereignis würde `LH-FA-LLM-003` verletzen und die
  Entscheidungsspur nachträglich verändern.
- Der Contract darf noch keine Gate-Entscheidung erzeugen; Zyklus-/Gate-Bindung
  folgt in `slice-028`.
- Adapter und Golden-Set-Replay bleiben aus diesem Slice herausgeschnitten, damit
  Review und Gate-Scope klein bleiben.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** Der Schnitt blieb klein: Contract-Typen, Port und
Use-Case liegen im Application-Core; Domain-Ereignisse tragen nur primitive
Audit-Felder und erzeugen keine Application-Abhaengigkeit.

**Was ist offen geblieben:** Persistenz-/Replay-Adapter, Golden-Set-Fixtures
und die Bindung an Entscheidungszyklus/Gate-Pfad bleiben bewusst in
`slice-027` und `slice-028`.

**Steering-Loop:** Der Planning-Split war wirksam: ohne ihn waeren Adapter,
Replay, Architektur- und Zyklusbindung in denselben Review-Diff gerutscht. Die
Konfidenz-Externalisierung sollte weiterhin nur Contract + append-only Audit
liefern; Gate-Konsum gehoert in einen separaten Slice.

**Folge-Slices:** `slice-027` (Konfidenz-Replay-Fake-Adapter), `slice-028`
(Konfidenz an Zyklus/Gate-Pfad binden), danach E2E-Validierung in `slice-024`.

## 8. Sub-Area-Modus-Begründung

Berührte Sub-Areas:

### Sub-Area: `hexagon/application/belief/ports`

- **Modus:** Hybrid
- **Konventionen-Dichte:** Hoch für lokale/business-area Ports aus
  `ADR-0003`; neu ist ein geteilter Konfidenz-Contract, den `slice-023`
  konsumieren soll.
- **Phase-Reife:** Phase 3. Spec führt (`LH-FA-LLM-003`), Code folgt mit
  neuen Contract-Typen.
- **Evidenz-/Diskrepanz-Risiko:** Mittel. Ein use-case-lokaler Contract würde
  `slice-023` zu falschen Imports zwingen; ein zu breiter Contract könnte Gate-
  oder Adapterverantwortung aufnehmen.
- **Reconciliation-Aufwand:** Teil dieses Slice: Contract bleibt business-area
  geteilt und enthält keine Gate-Entscheidung. Adapter folgen in `slice-027`.

### Sub-Area: `hexagon/application/belief/konfidenzexternalisieren`

- **Modus:** GF
- **Konventionen-Dichte:** Hoch. Use-Case-Struktur und Tests folgen den
  bestehenden Application-Slices; externe Persistenz läuft über Ports.
- **Phase-Reife:** Phase 3. Neue Übersetzungslogik wird aus Spec/Contract
  heraus gebaut.
- **Evidenz-/Diskrepanz-Risiko:** Niedrig bis mittel. Niedrig für den neuen
  Use Case, mittel an der Grenze zu Audit: Overrides müssen append-only bleiben.
- **Reconciliation-Aufwand:** Teil dieses Slice: Tests gegen Externalisierung,
  Override und ungueltige Rohwerte. Zyklusbindung folgt in `slice-028`.

### Sub-Area: `hexagon/domain/belief/Ereignis`

- **Modus:** Hybrid
- **Konventionen-Dichte:** Hoch. Ereignisse sind append-only und
  rekonstruierbar (`LH-FA-AUD-001`/`003`); neu sind Konfidenz-Ereignisse.
- **Phase-Reife:** Phase 4 für bestehende Audit-Ereignisse, Phase 3 für neue
  Konfidenz-Ereignisse.
- **Evidenz-/Diskrepanz-Risiko:** Mittel. Ein Override darf keinen alten
  Eintrag ersetzen und muss als neues Ereignis sichtbar bleiben.
- **Reconciliation-Aufwand:** Teil dieses Slice: Ereignistypen und Tests.
  Persistenzadapter folgen in `slice-027`.
