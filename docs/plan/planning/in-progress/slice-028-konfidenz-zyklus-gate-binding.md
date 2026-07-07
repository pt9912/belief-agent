# Slice slice-028: Konfidenz an Zyklus/Gate-Pfad binden

**Status:** open → next → in-progress → done (siehe
[Planning-README](../README.md)).

**Welle:** Roadmap-Follow-up zu `welle-05-llm-port`
([Roadmap](../in-progress/roadmap.md)); gezielter Follow-up nach
`slice-022` und `slice-027`; `slice-023` bleibt als separater
Aktionsvorschlags-Port-Slice offen.

**Bezug:** `LH-FA-LLM-003`, `LH-FA-AUD-001`, `LH-FA-AUD-003`,
`LH-FA-POL-006`, `LH-QA-03`, `LH-QA-04`; `ADR-0001`, `ADR-0003`;
`ARC-06`, `ARC-07`, `ARC-08`, `ARC-09`.

**Autor:** pt9912. **Datum:** 2026-07-07.

---

## 1. Ziel

Dieser Slice bindet externalisierte Modell-Konfidenz aus `slice-022` an den
Entscheidungszyklus-/Gate-Pfad, ohne `AktionGaten` direkt zu erweitern oder
Gate-/Freigabe-Logik in den LLM-Konfidenz-Contract zu verschieben.

## 2. Definition of Done

- [x] `LH-FA-LLM-003` ist im Entscheidungsfluss sichtbar: gate-fähige
  Modell-Konfidenz wird nur über den externalisierten Contract konsumiert.
- [x] Die Bindung läuft über `Entscheidungszyklus`/Application-Wiring und nicht
  über direkte Adapter- oder LLM-Abhängigkeiten in `AktionGaten`.
- [x] Overrides bleiben append-only auditierbar; der Zyklus konsumiert die
  neueste gültige externalisierte Konfidenz, ohne alte Einträge zu mutieren.
- [x] `spec/architecture.md` dokumentiert Port-/Mapping-Grenze sprach- und
  meilensteinfrei; `docs/user/integration.md` dokumentiert den
  Integrationskontext für Konfidenz-Override.
- [x] Deterministische Tests belegen normalen Pfad, Override-Pfad und fail-safe
  bei fehlender/ungueltiger externalisierter Konfidenz.
- [x] `make gates` grün.
- [x] Closure-Notiz mit Steering-Loop-Eintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/entscheidungszyklus/` | update | Zyklus-/Wiring-Punkt konsumiert externalisierte Konfidenz, Gate bleibt getrennt |
| `hexagon/application/src/commonTest/kotlin/dev/beliefagent/application/belief/entscheidungszyklus/` | update/neu | Normal-, Override- und fail-safe-Pfade deterministisch prüfen |
| `spec/architecture.md` | update | `ARC-06`/`ARC-07`/`ARC-09`-Mapping für Konfidenz-Port und Zyklusbindung dokumentieren |
| `docs/user/integration.md` | neu/update | Integrationskontext für Konfidenz-Override dokumentieren |
| `docs/plan/planning/in-progress/roadmap.md` | update | Follow-up-Closure mit Golden-Set-/Zyklusbindung nachziehen |

## 4. Trigger

`slice-022` und `slice-027` liegen in `done/`. Die Bindung erfolgt nicht an
Aktionsvorschläge, sondern an eine explizite `KonfidenzgebundeneAktion` am
Application-Rand; `slice-023` ist deshalb keine Vorbedingung fuer diesen
Slice.

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz + Slice in `done/`.

## 6. Risiken und offene Punkte

- Direkte Änderung an `AktionGaten` könnte das nicht-umgehbare Gate verwässern;
  die Bindung gehört an den Application-Zyklus/Wiring-Rand.
- Eine fehlende externalisierte Konfidenz darf nicht als implizite Freigabe
  interpretiert werden.
- Architektur-Doku darf keine Slice-/Wellen-Historie enthalten.
- Der genaue Konsument hängt von `slice-023` ab, falls `p_success` für
  Aktionsvorschläge der erste Gate-Pfad wird.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** Der Wrapper am `ARC-09`-Rand hielt den Scope klein:
Externalisierte Konfidenz wird vor dem bestehenden Gate auf
`Erfolgswahrscheinlichkeit` gemappt, ohne `AktionGaten` oder
`KonfidenzGate` zu erweitern. Der vorhandene `KonfidenzPort` aus
`slice-022`/`slice-027` reichte fuer normale Historien, Overrides und
fail-safe Negativpfade aus.

**Was ist offen geblieben:** Es gibt weiterhin keinen
LLM-Aktionsvorschlags-Port in diesem Slice; `slice-023` bleibt dafuer der
separate Schnitt. Die produktive Verdrahtung im CLI-/Composition-Root bleibt
bei `slice-024`.

**Steering-Loop:** Bei neuen modellabgeleiteten Gate-Eingaben zuerst den
Application-Wiring-Pfad inklusive ungueltiger Historie testen, bevor Gate-API
oder Adapter erweitert werden. Das verhindert, dass Modell-Konfidenz als
implizite Gate-Freigabe missverstanden wird.

**Folge-Slices:** Produktiver `ARC-09`-Composition-Root (`slice-024`).

## 8. Sub-Area-Modus-Begründung

### Sub-Area: `hexagon/application/belief/entscheidungszyklus`

- **Modus:** Hybrid
- **Konventionen-Dichte:** Hoch. `ARC-09`, `LH-FA-POL-006` und bestehende Tests
  sichern den Zyklus als Gate-Orchestrierung; neu ist nur der Konsum
  externalisierter Konfidenz.
- **Phase-Reife:** Phase 4 fuer bestehenden Zyklus, Phase 3 fuer den neuen
  Konfidenz-Konsumenten.
- **Evidenz-/Diskrepanz-Risiko:** Mittel. Ein falscher Schnitt koennte
  Modell-Konfidenz als Gate-Freigabe missverstehen oder `AktionGaten`
  umgehen.
- **Reconciliation-Aufwand:** Teil dieses Slice: Tests gegen normalen Pfad,
  Override und fehlende Konfidenz; bei notwendiger Gate-API-Aenderung wird
  vor Code neu geschnitten.

### Sub-Area: `spec/architecture.md` und `docs/user/integration.md`

- **Modus:** GF
- **Konventionen-Dichte:** Hoch. Architektur ist sprach-/meilensteinfrei;
  Roadmap/Closure bleibt in `docs/plan/planning/`.
- **Phase-Reife:** Phase 4. Bestehende Architektur fuehrt Ports/Adapter und
  Zyklus bereits; Mapping wird nur ergaenzt.
- **Evidenz-/Diskrepanz-Risiko:** Niedrig bis mittel. Risiko liegt in
  Vermischung von zeitlicher Planung mit Architektur oder unklarem
  Integrationsvertrag.
- **Reconciliation-Aufwand:** Teil dieses Slice; `doc-check` und Review
  sichern Links und Schichtgrenzen.
