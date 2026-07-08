# Slice slice-036: Lokaler Human-Approval-Adapter

**Status:** in-progress (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung.

**Bezug:** `LH-FA-POL-004`, `LH-FA-POL-005`, `LH-FA-POL-006`, `LH-OUT-04`,
`LH-QA-02`, `LH-QA-03`, `LH-QA-04`; `ADR-0001`, `ADR-0003`; `ARC-07`,
`ARC-08`, `ARC-09`.

**Autor:** Codex. **Datum:** 2026-07-08.

---

## 1. Ziel

Ein echter lokaler Approval-Adapter hinter `HumanApprovalPort` verlangt fuer
extern-wirksame Aktionen eine explizite, kontextgebundene menschliche Freigabe:
Die Freigabe ist an `ApprovalAnfrage(aktion, belief)`, eine Nonce und eine
lokale Identitaetsangabe gebunden und wird genau einmal konsumiert.

## 2. Definition of Done

- [ ] Neuer Outbound-Adapter `adapters/outbound/approval-local` implementiert
  `HumanApprovalPort` ohne Netz und ohne extern-wirksame Nebenwirkung:
  Anfrage-Rendering, Nonce-Erzeugung, Identitaets-/Bestaetigungs-Eingabe und
  Einmaligkeitspruefung sind testbar abstrahiert; Default und Fehlerfaelle
  verweigern fail-closed (`LH-QA-02`, `LH-FA-POL-004`).
- [ ] Safety-Verhalten ist deterministisch getestet (`LH-QA-03`):
  falsche Nonce, fehlende Identitaet, Kontext-Digest-Mismatch, wiederverwendete
  Nonce, EOF/Abbruch und nicht-interaktive Defaults geben keine Freigabe; eine
  exakt passende Eingabe gibt nur die konkrete Anfrage frei und kann nicht fuer
  eine wert-gleiche Aktion unter anderem `BeliefState` wiederverwendet werden.
- [ ] Build-/Arch-/Doku-Integration ist vollstaendig: Modul in
  `settings.gradle.kts`, `.a-check.yml`, `Dockerfile`/Coverage-Gate und
  Integrationsdoku aufgenommen; `adapters/inbound/cli` bleibt standardmaessig
  auf Fake/konfiguriertem Adapter, bis ein separater Binding-Slice den lokalen
  Approval-Adapter bewusst verdrahtet. Review-/Verification-Artefakte,
  `make doc-check`, `make gates` und Closure-Notiz liegen vor.

## 3. Plan (vor Code)

| Datei / Komponente | Aenderungs-Art | Begruendung |
|---|---|---|
| `adapters/outbound/approval-local` | neu | Lokaler Human-Approval-Adapter hinter `HumanApprovalPort` (`ARC-08`) mit Nonce, Identitaet und Kontextbindung. |
| `adapters/outbound/approval-local/src/commonMain/**` | neu | Port-Implementierung plus abstrahierte I/O-, Nonce- und Clock/Identity-Grenzen, damit Tests deterministisch bleiben. |
| `adapters/outbound/approval-local/src/commonTest/**` | neu | Negativmatrix und positiver Einmaligkeitsfall fuer `ApprovalAnfrage`. |
| `settings.gradle.kts` | update | Neues Adaptermodul registrieren. |
| `.a-check.yml` | update | Adapterrolle und erlaubte Kanten fuer `approval-local` aufnehmen; keine Adapter-zu-Adapter-Fachkopplung. |
| `Dockerfile` | update | Neues Modul in Build-, Test- und Coverage-Stages aufnehmen. |
| `docs/user/integration.md` | update | Lokalen Approval-Adapter, fail-closed Defaults und Binding-Grenze dokumentieren. |
| `docs/user/cli-entscheidungsnachweis.md` | update | Safety-Nachweis um Nonce/Kontextbindung ergaenzen, ohne produktive CLI-Umbindung zu behaupten. |
| `docs/reviews/*slice-036*` | neu | Code-/Safety-Review-Artefakt fuer den echten Approval-Adapter. |
| `docs/verifications/*slice-036*` | neu | Verification-Artefakt fuer DoD, Sensoren und Negativmatrix. |

## 4. Trigger

`slice-035` liegt in `done/` und liefert den kontextgebundenen
`ApprovalAnfrage`-Vertrag. Kein Slice liegt in `in-progress/` (WIP-Limit 1).
Vor Start bestaetigt ein kurzer Design-Check, dass der Adapter nur den Port
implementiert und keine CLI-/Executor-Umbindung in diesen Slice zieht.

## 5. Closure-Trigger

DoD vollstaendig + Review/Verification abgeschlossen + `make gates` gruen +
Closure-Notiz geschrieben + Slice nach `done/` verschoben.

## 6. Risiken und offene Punkte

- Interaktive I/O kann Tests flaky machen. Deshalb muessen Eingabe, Ausgabe,
  Nonce und Identitaetsquelle als Ports/Funktionen injizierbar sein; echte
  Terminal-I/O darf nur am Adapterrand liegen.
- Ein lokaler Approval-Adapter ist noch kein produktiver Remote-/UI-Approval-
  Kanal. Kanalwahl, Audit-Persistenz des Approval-Vorgangs und CLI-Binding
  bleiben Folgeslices, falls sie ueber reine Doku hinausgehen.
- Einmaligkeit darf nicht nur pro Prozessvariable wirken, wenn der Adapter in
  spaeteren Composition-Routes persistent genutzt wird. Dieser Slice darf eine
  lokale In-Memory-Einmaligkeit liefern, muss aber die Persistenzgrenze fuer den
  Folgeslice benennen.
- Der Adapter darf keine Ausfuehrungsfreigabe ersetzen: Executoren konsumieren
  weiterhin nur `Zyklusergebnis.Gehandelt.freigabe.aktion`
  (`LH-FA-POL-006`, `LH-OUT-04`).

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss fuellen. -->

## 8. Sub-Area-Modus-Begründung

### Sub-Area: Outbound-Adapter Approval (`adapters/outbound/approval-*`)

- **Modus:** Hybrid
- **Konventionen-Dichte:** mittel. `ADR-0003` und `spec/architecture.md`
  regeln Adapter hinter Ports (`ARC-08`); `slice-035` liefert den
  kontextgebundenen Port-Vertrag. Einen echten, interaktiven Approval-Adapter
  gibt es noch nicht.
- **Phase-Reife:** Phase 3-4. Der Fake-Adapter ist seit `slice-013` stabil,
  der echte Adapter ist neu und muss gegen die bestehende Port-/Gate-Policy
  reconciled werden.
- **Evidenz-/Diskrepanz-Risiko:** mittel bis hoch. Approval ist Teil der
  Safety-Funktion; Fehler bei Nonce, Kontextbindung oder Fail-Closed-Defaults
  koennen extern-wirksame Aktionen faelschlich freigeben.
- **Reconciliation-Aufwand:** ein Slice fuer isolierten Adapter plus
  Build-/Arch-/Doku-Registrierung. CLI-Binding und persistenter Approval-Audit
  bleiben Folge-Slices.

### Sub-Area: CLI-Composition und Integrationsdokumentation

- **Modus:** GF
- **Konventionen-Dichte:** mittel. `adapters/inbound/cli` ist der
  Composition-Root (`ARC-09`), aber dieser Slice darf ihn noch nicht produktiv
  auf den lokalen Adapter umstellen.
- **Phase-Reife:** Phase 4. CLI-Szenarien und Executor-Grenze sind durch
  `slice-024`, `slice-030` und `slice-035` abgesichert.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Doku darf den Adapter als vorhanden
  beschreiben, aber kein produktives CLI-Binding behaupten, solange dieses nicht
  implementiert und getestet ist.
- **Reconciliation-Aufwand:** gering in diesem Slice; Folge-Slice fuer bewusstes
  CLI-/Runtime-Binding.
