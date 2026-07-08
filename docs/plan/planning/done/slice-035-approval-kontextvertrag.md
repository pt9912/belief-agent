# Slice slice-035: Human-Approval-Kontextvertrag

**Status:** done (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung.

**Bezug:** `LH-FA-POL-004`, `LH-FA-POL-005`, `LH-FA-POL-006`, `LH-OUT-04`,
`LH-QA-02`, `LH-QA-03`, `LH-QA-04`; `ADR-0001`, `ADR-0003`; `ARC-03`,
`ARC-07`, `ARC-09`.

**Autor:** Codex. **Datum:** 2026-07-08.

---

## 1. Ziel

Der `HumanApprovalPort` wird so geschaerft, dass eine menschliche Freigabe nicht
mehr nur an eine `Aktion`, sondern an den konkreten Entscheidungskontext aus
`Aktion` und aktuellem `BeliefState` gebunden wird. Damit entsteht der
notwendige Vertrag fuer einen spaeteren echten Approval-Adapter mit
Nonce/Identitaet/Einmaligkeit, ohne in diesem Slice bereits einen interaktiven
Adapter oder externe I/O einzufuehren.

## 2. Definition of Done

- [x] Port-Contract-Slice bleibt eng: `HumanApprovalPort` konsumiert eine
  strukturierte Approval-Anfrage mit `Aktion` und aktuellem `BeliefState`;
  `AktionGaten` erzeugt diese Anfrage erst nach bestandener
  `KonfidenzGate`-Freigabe und nie fuer Gate-Ablehnung, Gate-Eskalation oder
  Resthypothese-Sperre. Bestehende Fake-/Static-Approvals werden nur soweit
  reconciled, wie es fuer Kompilierbarkeit und bestehende Tests noetig ist;
  keine neue Approval-I/O, kein Nonce-Speicher, keine Executor-Aenderung.
- [x] Safety-Verhalten bleibt fail-closed und ist getestet: fehlende oder
  verweigerte Freigabe eskaliert weiter, Gate-Nichtfreigaben loesen keinen
  Approval-Call aus, und kein Pfad ermoeglicht Ausfuehrung ohne
  `Zyklusergebnis.Gehandelt.freigabe.aktion`.
- [x] Oeffentlicher Contract ist vollstaendig reconciled:
  `spec/architecture.md`, `docs/user/integration.md` und
  `docs/user/cli-entscheidungsnachweis.md` benennen die Kontextbindung als
  Voraussetzung fuer den folgenden echten Adapter; ein Design-Review als
  Architect-Handoff bestaetigt `ADR-0001`/`ADR-0003` oder skizziert eine
  Folge-ADR; Review-/Verification-Artefakte, `make doc-check`, `make gates` und
  Closure-Notiz liegen vor.

## 3. Plan (vor Code)

| Datei / Komponente | Aenderungs-Art | Begruendung |
|---|---|---|
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/gaten/ports/HumanApprovalPort.kt` | update | Port-Vertrag von boolescher `Aktion`-Freigabe auf kontextgebundene Anfrage schaerfen (`ARC-07`, `LH-FA-POL-004`). |
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/gaten/AktionGaten.kt` | update | Approval-Anfrage mit aktuellem Belief erst nach Gate-Freigabe bauen; Gate-/Resthypothese-Sperren duerfen keinen Approval-Call ausloesen. |
| `hexagon/application/src/commonTest/kotlin/dev/beliefagent/application/belief/gaten/**` | update | Negativ- und Kontexttests: kein Approval-Call bei Gate-Nichtfreigabe, Kontext enthaelt aktuelle Resthypothese, verweigerte Freigabe eskaliert. |
| `adapters/outbound/approval-fake/**` | update | Fake-Adapter auf neuen Vertrag heben; Default bleibt verweigert und deterministisch. |
| `adapters/inbound/cli`, `example/langchain`, `example/koog`, `example/code-agent` | compile-only update | Bestehende Bindings/In-Test-Approvals an die neue Port-Signatur anpassen, ohne Runtime-Policy, Szenarien oder Executor-Verhalten zu erweitern. |
| `spec/architecture.md` | update | `ARC-07`/`ARC-09` ergaenzen: Human-Approval erhaelt Entscheidungskontext, Ausfuehrung bleibt an `Aktionsfreigabe.Freigegeben` gebunden. |
| `docs/user/integration.md` | update | Integrationshinweis fuer echte Approval-Adapter: Kontextbindung ist Vertrag; Nonce/Identitaet/Einmaligkeit folgen im Adapter-Slice. |
| `docs/user/cli-entscheidungsnachweis.md` | update | Safety-Nachweis und Approval-Matrix mit dem neuen Kontextvertrag reconciliieren. |
| `docs/reviews/*slice-035-design-review*` | neu | Architect-Handoff-Artefakt: bestaetigt ADR-0001/ADR-0003-Konformitaet oder fordert Folge-ADR. |
| `docs/reviews/*slice-035*` | neu | Safety-Review-Artefakt fuer Approval-Kontextvertrag. |
| `docs/verifications/*slice-035*` | neu | Verification-Artefakt fuer DoD und negative Safety-Pfade. |

## 4. Trigger

`slice-024`, `slice-030` und `slice-033` liegen in `done/`: der
CLI-Composition-Root, die sichtbaren Safety-Szenarien und der Code-Agent-Run sind
stabil. Vor Start wird geprueft, dass kein Slice in `in-progress/` liegt
(WIP-Limit 1) und dass `welle-05-llm-port Stabilisierung` fuer diesen Slice
aktiviert oder der Slice als gezielter Follow-up gestartet wird. Der Uebergang
Planner -> Implementation braucht nach Modul 8 ein Architect-Uebergabeartefakt:
`docs/reviews/<YYYY-MM-DD>-slice-035-design-review.md` bestaetigt den
ADR-Bezug oder skizziert die noetige Folge-ADR. Ohne dieses Artefakt geht der
Slice nicht nach `in-progress/`.

## 5. Closure-Trigger

DoD vollstaendig + Review/Verification abgeschlossen + `make gates` gruen +
Closure-Notiz geschrieben + Slice nach `done/` verschoben.

## 6. Risiken und offene Punkte

- Der bestehende Port importiert nur Domaintypen. Eine zu breite Anfrage darf
  keine Application- oder Adaptertypen in den Port ziehen; sonst wuerde
  `ARC-07`/`ADR-0003` verletzt.
- Ein echter Approval-Adapter mit Nonce, Identitaet, Speicher und Interaktion ist
  bewusst nicht Teil dieses Slice. Folge-Slice: `slice-036` oder naechste freie
  ID fuer `approval-local`/`approval-cli` mit Einmaligkeit.
- CLI-/Example-Dateien duerfen in diesem Slice nur mechanisch an die
  Port-Signatur angepasst werden. Sobald neue Szenarien, Runtime-Konfiguration
  oder echte Approval-I/O noetig werden, wird das in einen Folge-Slice
  geschnitten.
- Kontextbindung darf nicht als Ausfuehrungsfreigabe missverstanden werden:
  Executoren duerfen weiterhin nur `Zyklusergebnis.Gehandelt.freigabe.aktion`
  konsumieren (`LH-FA-POL-006`, `LH-OUT-04`).
- Falls der aktuelle `BeliefState` fuer den spaeteren Adapter zu grob ist
  (z. B. fehlender Evidenz-/Audit-Bezug), wird dieser Slice nicht ausgeweitet;
  die Luecke wird als Folge-Slice oder ADR/Spec-Schaerfung dokumentiert.
- Rollen nach Modul 8 duerfen nicht verschmelzen: Wer implementiert, erzeugt
  Review- und Verification-Eingaben, schliesst sie aber nicht im selben Kontext
  selbst ab. Findings oder Architektur-Widerspruch brauchen ein benanntes
  Uebergabeartefakt.

## 7. Closure-Notiz (nach `done/`)

Abgeschlossen am 2026-07-08. Der `HumanApprovalPort` konsumiert jetzt eine
`ApprovalAnfrage` aus konkreter `Aktion` und aktuellem `BeliefState`;
`AktionGaten` erzeugt diese Anfrage nur nach `KonfidenzGate.Freigabe` und nie
bei Gate-Ablehnung, Gate-Eskalation oder Resthypothese-Sperre.

Review/Verification: Architect-Handoff
`docs/reviews/2026-07-08-slice-035-design-review.md`, Code-/Safety-Review
`docs/reviews/2026-07-08-slice-035-code-safety-review.md` und Verification
`docs/verifications/2026-07-08-slice-035-verification.md` liegen vor. Der
LOW-Befund aus dem Code-/Safety-Review zum Slice-Kopfstatus wurde korrigiert.
`make gates` lief gruen; nach der Closure-Verschiebung wurde `make doc-check`
erneut gruen ausgefuehrt.

Folgegrenze: Ein echter Approval-Adapter mit Nonce, Identitaet, Einmaligkeit
und interaktiver I/O bleibt Folgeslice; dieser Slice liefert nur den
kontextgebundenen Port-Vertrag.

## 8. Sub-Area-Modus-Begründung

### Sub-Area: Approval-/Gate-Use-Case (`hexagon/application/.../gaten`)

- **Modus:** GF
- **Konventionen-Dichte:** hoch. `spec/lastenheft.md` fordert die nicht
  abschaltbare menschliche Freigabe fuer extern-wirksame Aktionen
  (`LH-FA-POL-004`) und das nicht umgehbare Gate (`LH-FA-POL-006`);
  `spec/architecture.md` verortet den `HumanApprovalPort` use-case-lokal als
  `ARC-07`.
- **Phase-Reife:** Phase 4. Gate, Resthypothese-Sperre und Fake-Approval sind
  seit `slice-013`/`slice-024` implementiert und getestet; dieser Slice schaerft
  den Port-Vertrag fuer die naechste Adapter-Stufe.
- **Evidenz-/Diskrepanz-Risiko:** niedrig bis mittel. Der fachliche Vertrag ist
  bereits in Port-Kommentaren und User-Doku angelegt, aber die aktuelle Signatur
  traegt den Kontext noch nicht. Tests muessen die Reconciliation sichtbar machen.
- **Reconciliation-Aufwand:** ein Slice fuer Contract + lokale Bindings. Der echte
  Adapter bleibt Folge-Slice.

### Sub-Area: CLI-Composition und Beispiele

- **Modus:** GF
- **Konventionen-Dichte:** mittel. `adapters/inbound/cli` ist seit `slice-024`
  der Composition-Root; Beispiele duerfen Ports binden, aber die
  Executor-Grenze nicht verschieben.
- **Phase-Reife:** Phase 4. CLI-Demo, Beispielmodule und Code-Agent-Run sind
  durch `slice-029`..`033` stabilisiert; Anpassungen sind Signatur-Reconciliation,
  keine neue Fachlogik.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Falsch aktualisierte Static-/Fake-
  Approvals koennten positive Demo-Pfade unbeabsichtigt oeffnen oder negative
  Pfade verdecken.
- **Reconciliation-Aufwand:** gering bis mittel; bestehende Tests und CLI-Sensoren
  muessen nachgezogen werden, echte Approval-I/O bleibt Folge-Slice.

### Sub-Area: Integrationsdokumentation

- **Modus:** GF
- **Konventionen-Dichte:** mittel. `docs/user/integration.md` ist die aktuelle
  Integrationsquelle fuer Ports und "noch nicht stabil"-Grenzen; Source
  Precedence bleibt unter Spec/Architektur.
- **Phase-Reife:** Phase 3. Die Doku nennt bereits den echten Approval-Adapter als
  offene Stabilisierung; dieser Slice praezisiert den davor notwendigen
  Port-Vertrag.
- **Evidenz-/Diskrepanz-Risiko:** niedrig. Doku darf nur den neuen Contract und
  die Folgegrenze benennen, nicht einen echten Adapter behaupten.
- **Reconciliation-Aufwand:** erledigt in diesem Slice; Folge-Slice fuer echten
  Adapter wird in Closure/Roadmap benannt.
