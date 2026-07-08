# Slice slice-038: Approval-Kanalwahl

**Status:** done (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung.

**Bezug:** `LH-FA-POL-004`, `LH-FA-POL-006`, `LH-OUT-04`, `LH-QA-02`,
`LH-QA-03`, `LH-QA-04`; `ADR-0001`, `ADR-0003`; `ARC-07`, `ARC-08`,
`ARC-09`.

**Autor:** Codex. **Datum:** 2026-07-08.

---

## 1. Ziel

Die Form und der Kanal der menschlichen Freigabe werden als explizites,
fail-closed `ARC-09`-Composition-Wiring im CLI-Root modelliert: lokale
CLI-Freigabe bleibt ein konkreter Kanal, weitere Kanaele werden konfigurierbar
adressierbar, ohne den Core-Port `HumanApprovalPort` zu erweitern und ohne dass
eine extern-wirksame Aktion jemals ohne dessen Freigabe ausgefuehrt werden kann.

## 2. Definition of Done

- [x] Kanalwahl ist ausschliesslich als CLI-/`ARC-09`-Composition-Vertrag
  abgebildet: erlaubte Kanaele, Default-Verhalten, unbekannter Kanal, nicht
  konfigurierter Kanal und Kanalfehler sind fail-closed; der Entfall
  menschlicher Freigabe ist nicht konfigurierbar (`LH-FA-POL-004`, `LH-QA-02`).
- [x] `HumanApprovalPort` und `hexagon/application/.../gaten/ports` bleiben
  unveraendert. Konkrete Kanaladressierung, Kanalnamen und Dispatcher-Policy
  leben im CLI-Composition-Root und werden nicht Teil des Core-/Port-Vertrags.
- [x] Ein Kanal-Dispatcher oder gleichwertiger `adapters/inbound/cli`-
  Composition-Baustein waehlt zwischen vorhandenen Approval-Kanaelen, ruft genau
  einen Kanal pro `ApprovalAnfrage` auf und propagiert keine Freigabe, wenn
  Auswahl, Kanalantwort oder Kontextbindung ungueltig sind (`LH-FA-POL-006`,
  `LH-QA-03`).
- [x] CLI-/Doku-Integration beschreibt die Kanalwahl als Konfiguration des
  Approval-Pfads, nicht als neuen Approval-Kanal: `local` ist der einzige in
  diesem Slice nutzbare konkrete Kanal; Remote-/UI-Kanaladapter und persistenter
  Approval-Audit bleiben Folgeslices. Review-/Verification-Artefakte,
  `make doc-check`, `make gates` und Closure-Notiz liegen vor.

## 3. Plan (vor Code)

| Datei / Komponente | Aenderungs-Art | Begruendung |
|---|---|---|
| `adapters/inbound/cli/src/main/**` | update | CLI-naher Kanalwahl-Vertrag, Config-Typ und Dispatcher fuer `local` und spaetere Kanaele; konkrete Kanaladressierung bleibt `ARC-09`-Composition-Wiring. |
| `adapters/inbound/cli/src/test/**` | update | Tests fuer unbekannten Kanal, fehlende Kanalbindung, Kanalfehler und erfolgreichen `local`-Dispatch. |
| `.a-check.yml` | update | Falls neue Dispatcher-/Config-Komponente entsteht: Architekturkante fuer Composition-Root erlauben, fachliche Adapterkopplung weiter verbieten. |
| `docs/user/integration.md` | update | Kanalwahl, Defaults und Grenzen dokumentieren. |
| `docs/user/cli-entscheidungsnachweis.md` | update | Safety-Nachweis um Kanalwahl-Fail-Closed-Pfade ergaenzen. |
| `docs/reviews/*slice-038*` | neu | Plan-/Code-/Safety-Review-Artefakt fuer Kanalwahl. |
| `docs/verifications/*slice-038*` | neu | Verification-Artefakt fuer DoD und Kanalwahl-Negativmatrix. |

## 4. Trigger

`slice-037` liegt in `done/` und beweist das bewusste CLI-Binding des lokalen
Approval-Adapters inklusive Executor-Grenze. Kein Slice liegt in `in-progress/`
(WIP-Limit 1). Vor Start wird bestaetigt, dass dieser Slice keine Core-/Port-
Aenderung, keine neuen Remote-/UI-Kanaladapter und keine Approval-Audit-
Persistenz implementiert.

## 5. Closure-Trigger

DoD vollstaendig + Review/Verification abgeschlossen + `make gates` gruen +
Closure-Notiz geschrieben + Slice nach `done/` verschoben.

## 6. Risiken und offene Punkte

- Kanalwahl darf nicht als Option "keine Freigabe" modelliert werden. Unbekannte
  oder deaktivierte Kanaele muessen fail-closed enden.
- Ein Dispatcher kann versehentlich mehrere Kanaele abfragen und dadurch
  Kontext-/Nonce-Einmaligkeit verwischen. Pro `ApprovalAnfrage` wird genau ein
  Kanal ausgewaehlt.
- Remote-/UI-Kanaladapter sind eigene Slices, weil sie externe Systeme,
  Authentisierung und neue Failure-Modes einfuehren.
- Persistenter Approval-Audit bleibt eigener Slice; dieser Slice darf nur
  dokumentieren, welche Ereignisse spaeter auditierbar sein muessen.
- Kanalnamen und konkrete Adressierung duerfen nicht in
  `hexagon/application/.../gaten/ports` wandern. Der Core sieht weiter nur
  `HumanApprovalPort`; Kanalwahl bleibt Composition-Konfiguration.

## 7. Closure-Notiz (nach `done/`)

Abgeschlossen am 2026-07-08. Implementiert wurde eine fail-closed
Approval-Kanalwahl im CLI-Composition-Root: `local` ist der einzige konkrete
Kanal dieses Slice, unbekannte Kanaele, fehlende Kanalbindungen und
Kanalfehler propagieren keine Freigabe. `HumanApprovalPort` und
`hexagon/application/.../gaten/ports` blieben unveraendert; konkrete
Kanalnamen und Dispatcher-Policy liegen in `adapters/inbound/cli`.

Nachtraeglich wurde die User-Doku auf Make/Docker-only korrigiert:
`approval=local` wird ueber `make cli-demo-approval-local` demonstriert, nicht
ueber direkte Host-Gradle-Aufrufe. Remote-/UI-Kanaladapter und persistenter
Approval-Audit bleiben Folgeslices.

Review/Verification: Plan-/Design-Review lagen vor; Code-/Safety-Review
meldete keine Findings; Verification meldete keine DoD-Verletzung und keine
Carveouts.

Ausgefuehrte Sensors: `make test`, `git diff --check`, `make doc-check`,
`make gates`, `make cli-demo-scenarios`, `make cli-demo-approval-local`.
Finale Closure-Sensors: `make doc-check`, `make gates`.

## 8. Sub-Area-Modus-Begründung

### Sub-Area: CLI-Approval-Kanalwahl / Composition

- **Modus:** Hybrid
- **Konventionen-Dichte:** mittel. `LH-FA-POL-004` erlaubt Form und Kanal als
  konfigurierbar, verbietet aber den Entfall der Freigabe; `ARC-09` verortet
  konkrete Kanaladressierung und Binding im Composition-Root. Ein expliziter
  CLI-Kanalwahl-Vertrag existiert noch nicht.
- **Phase-Reife:** Phase 3. `slice-036`/`037` liefern lokalen Adapter und
  Binding, aber Kanalwahl als separater Vertrag ist neu.
- **Evidenz-/Diskrepanz-Risiko:** mittel bis hoch. Eine falsch modellierte
  Kanalwahl kann Safety-Policy lockern oder Fehlerfaelle als Freigabe behandeln.
- **Reconciliation-Aufwand:** ein Slice fuer CLI-Config-Vertrag, Dispatch und
  Doku. Konkrete Remote-/UI-Kanaele und Audit-Persistenz bleiben Folge-Slices.

### Sub-Area: CLI-Composition-Root

- **Modus:** GF
- **Konventionen-Dichte:** hoch. `ARC-09`, `slice-024` und `slice-037` legen
  den CLI-Root als bewussten Binding-Ort fest.
- **Phase-Reife:** Phase 4. Dieser Slice erweitert die bestehende
  Konfiguration, ohne den Executor-Pfad zu aendern.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Config-Defaults und Fehlkonfiguration
  muessen sichtbar fail-closed bleiben.
- **Reconciliation-Aufwand:** gering bis mittel; Tests und User-Doku muessen die
  neuen Kanalwahl-Faelle explizit abdecken.
