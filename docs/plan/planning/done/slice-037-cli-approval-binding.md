# Slice slice-037: CLI-Binding fuer lokalen Approval-Adapter

**Status:** done (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung.

**Bezug:** `LH-FA-POL-004`, `LH-FA-POL-006`, `LH-OUT-04`, `LH-QA-02`,
`LH-QA-03`, `LH-QA-04`; `ADR-0001`, `ADR-0003`; `ARC-08`, `ARC-09`.

**Autor:** Codex. **Datum:** 2026-07-08.

---

## 1. Ziel

Der CLI-Composition-Root kann den lokalen Approval-Adapter aus `slice-036`
bewusst statt des Fake-Adapters binden und zeigt im CLI-E2E, dass extern-
wirksame Aktionen nur nach kontextgebundener menschlicher Freigabe bis zur
Executor-Grenze gelangen.

## 2. Definition of Done

- [x] `adapters/inbound/cli` bietet eine explizite Konfiguration fuer
  `approval=fake|local` oder gleichwertig; Default bleibt fail-closed und
  netzfrei. Das Binding waehlt `approval-local` nur bewusst und verdrahtet keine
  neue Ausfuehrungsroute am Gate vorbei (`LH-FA-POL-006`, `LH-OUT-04`).
- [x] CLI-/Runtime-Tests belegen die Safety-Grenze: ohne passende lokale
  Freigabe bleibt `terminal=eskaliert`/`executed=false`; mit passender
  Nonce/Identitaet/Kontextbestaetigung wird genau der bestehende
  `Zyklusergebnis.Gehandelt.freigabe.aktion`-Pfad genutzt; falsche oder
  wiederverwendete Freigabe bleibt geschlossen (`LH-QA-03`).
- [x] Build-/Arch-/Doku-Integration ist vollstaendig:
  `adapters/inbound/cli` darf den Adapter als Composition-Root binden,
  `adapters/inbound/cli/build.gradle.kts` enthaelt die Modulabhaengigkeit auf
  `adapters:outbound:approval-local`, `.a-check.yml` bleibt gegen fachliche
  Adapter-zu-Adapter-Kopplung scharf, `make cli-demo`/ein enger CLI-Sensor
  dokumentiert den lokalen Approval-Pfad, User-Doku, Review-/Verification-
  Artefakte, `make doc-check`, `make gates` und Closure-Notiz liegen vor.

## 3. Plan (vor Code)

| Datei / Komponente | Aenderungs-Art | Begruendung |
|---|---|---|
| `adapters/inbound/cli/src/main/**` | update | CLI-Konfiguration und Koin-Binding fuer `approval-local`, ohne Executor-Policy zu verschieben. |
| `adapters/inbound/cli/src/test/**` | update | E2E-/Contract-Tests fuer lokalen Approval-Pfad, Negativpfade und unveraenderte Executor-Grenze. |
| `adapters/inbound/cli/build.gradle.kts` | update | Gradle-Modulkante vom CLI-Composition-Root zu `adapters:outbound:approval-local` explizit aufnehmen; `make build` prueft den Modulgraphen. |
| `.a-check.yml` | update | Composition-Root-Kante `inbound_cli -> approval-local` erlauben, sonstige Adapterkopplung weiter verbieten. |
| `Makefile` / `Dockerfile` | update | Nur falls ein enger CLI-Sensor oder Runtime-Parameter fuer `approval-local` noetig ist. |
| `docs/user/integration.md` | update | Bewusstes CLI-Binding, Defaults und Bediengrenze fuer lokalen Approval-Adapter dokumentieren. |
| `docs/user/cli-entscheidungsnachweis.md` | update | Nachweis um lokalen Approval-Pfad und negative Executor-Grenze ergaenzen. |
| `docs/reviews/*slice-037*` | neu | Code-/Safety-Review-Artefakt fuer Composition-Binding. |
| `docs/verifications/*slice-037*` | neu | Verification-Artefakt fuer DoD, Sensoren und CLI-Negativpfade. |

## 4. Trigger

`slice-036` liegt in `done/` und liefert `adapters/outbound/approval-local`
inklusive Negativmatrix. Kein Slice liegt in `in-progress/` (WIP-Limit 1). Vor
Start wird bestaetigt, dass dieser Slice nur Binding/CLI-Sensorik umfasst und
keine neue Approval-Persistenz, Remote/UI-Kanalwahl oder Ausfuehrungsadapter
einzieht.

## 5. Closure-Trigger

DoD vollstaendig + Review/Verification abgeschlossen + `make gates` gruen +
Closure-Notiz geschrieben + Slice nach `done/` verschoben.

## 6. Risiken und offene Punkte

- Binding kann versehentlich den Fake-Default lockern. Der Default muss
  fail-closed bleiben und der lokale Adapter muss bewusst gewaehlt werden.
- CLI-Szenarien duerfen keinen zweiten Executor-Pfad erzeugen. Ausgefuehrt wird
  weiterhin nur ueber `Zyklusergebnis.Gehandelt.freigabe.aktion`.
- Interaktive lokale Approval-Eingabe kann Demo-Sensoren blockieren; der Slice
  braucht deterministische Test-/Demo-Eingaben oder einen expliziten
  nicht-interaktiven Testmodus am Adapterrand.
- Audit-Persistenz des Approval-Vorgangs und Remote-/UI-Kanalwahl bleiben
  Folgeslices. Falls sie fuer ein sinnvolles Binding zwingend werden, wird
  `slice-037` vor Code zurueck nach `next/` geschnitten.

## 7. Closure-Notiz (nach `done/`)

Abgeschlossen am 2026-07-08. `adapters/inbound/cli` bindet den lokalen
Approval-Adapter jetzt bewusst ueber `approval=local`; ohne diesen Schalter
bleiben die Szenario-Defaults auf den deterministischen Fake-Approvals. Das
Binding ersetzt nur den `HumanApprovalPort` hinter `AktionGaten` und erzeugt
keinen neuen Executor-Pfad.

Review/Verification: Plan-Review
`docs/reviews/2026-07-08-slice-037-plan-review.md`, Design-Review
`docs/reviews/2026-07-08-slice-037-design-review.md`, Code-/Safety-Review
`docs/reviews/2026-07-08-slice-037-code-safety-review.md` und Verification
`docs/verifications/2026-07-08-slice-037-verification.md` liegen vor; keine
offenen Findings und keine DoD-Verletzung. `make cli-demo`, `make
cli-demo-scenarios`, `make test`, `make doc-check` und `make gates` liefen
gruen.

Folgegrenze: Approval-Kanalwahl, Remote-/UI-Approval und persistenter
Approval-Audit bleiben Folgeslices. Ausfuehrung bleibt weiterhin ausschliesslich
an `Zyklusergebnis.Gehandelt.freigabe.aktion` gebunden.

## 8. Sub-Area-Modus-Begründung

### Sub-Area: CLI-Composition-Root

- **Modus:** GF
- **Konventionen-Dichte:** hoch. `spec/architecture.md` beschreibt
  `adapters/inbound/cli` als Composition-Root (`ARC-09`); `slice-024`,
  `slice-030` und `slice-035` sichern Executor-Grenze und Approval-Kontext.
- **Phase-Reife:** Phase 4. Der CLI-Root ist etabliert und getestet; dieser
  Slice aendert nur bewusstes Adapter-Binding und sichtbare Sensorik.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Fehlerhafte Bindings koennen
  Safety-Demos faelschlich oeffnen oder Fake-/Local-Verhalten vermischen.
- **Reconciliation-Aufwand:** ein Slice fuer Binding + CLI-Sensorik. Persistenz
  und Remote/UI bleiben Folge-Slices.

### Sub-Area: Outbound-Adapter Approval (`approval-local`)

- **Modus:** Hybrid
- **Konventionen-Dichte:** mittel. `slice-036` soll den Adaptervertrag liefern;
  dieser Slice konsumiert ihn am Inbound-Rand.
- **Phase-Reife:** Phase 3-4. Der Adapter ist neu, der Portvertrag stabilisiert;
  Runtime-Binding ist der naechste Integrationsschritt.
- **Evidenz-/Diskrepanz-Risiko:** mittel bis hoch. Der Adapter ist Teil der
  Safety-Funktion; Binding-Fehler koennen Freigaben falsch wiederverwenden oder
  verweigerte Freigaben unsichtbar machen.
- **Reconciliation-Aufwand:** gering bis mittel; abhaengig von `slice-036`.
