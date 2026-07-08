# Slice slice-039: Remote/UI-Approval-Kanal

**Status:** open (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung.

**Bezug:** `LH-FA-POL-004`, `LH-FA-POL-006`, `LH-OUT-04`, `LH-QA-02`,
`LH-QA-03`, `LH-QA-04`; `ADR-0001`, `ADR-0003`; `ARC-07`, `ARC-08`,
`ARC-09`.

**Autor:** Codex. **Datum:** 2026-07-08.

---

## 1. Ziel

Ein Remote/UI-Approval-Kanal wird als konkreter, hermetisch testbarer Kanal
hinter der Kanalwahl aus `slice-038` bereitgestellt: Er transportiert die
`ApprovalAnfrage` an eine entfernte oder UI-nahe Bediengrenze und akzeptiert nur
eine kontext- und nonce-gebundene Antwort als menschliche Freigabe.

## 2. Definition of Done

- [ ] Ein neuer Kanaladapter (z. B. `approval-remote-ui`) implementiert den in
  `slice-038` definierten Kanalvertrag hinter `HumanApprovalPort`/Dispatcher:
  Anfrage-Serialisierung, Nonce/Kontext-Digest, Antwortvalidierung und
  Transportfehler sind testbar abstrahiert; ohne gueltige Antwort wird
  fail-closed verweigert (`LH-QA-02`, `LH-FA-POL-004`).
- [ ] Remote/UI-Negativmatrix ist deterministisch und netzfrei getestet
  (`LH-QA-03`): Timeout/EOF, Transportfehler, unbekannte Identitaet, falsche
  Nonce, Kontext-Digest-Mismatch, Replay und doppelte Antwort fuehren zu keiner
  Freigabe; eine exakt passende Antwort gibt nur die konkrete Anfrage frei.
- [ ] Build-/Arch-/Doku-Integration ist vollstaendig: neues Modul/Adapter in
  `settings.gradle.kts`, `.a-check.yml`, `Dockerfile`/Coverage-Gate und
  Kanalwahl-Doku registriert; reale Netzwerk-/UI-Implementierung bleibt hinter
  einer abstrahierten Transportgrenze und wird nicht fuer lokale Gates benoetigt.
  Review-/Verification-Artefakte, `make doc-check`, `make gates` und
  Closure-Notiz liegen vor.

## 3. Plan (vor Code)

| Datei / Komponente | Aenderungs-Art | Begruendung |
|---|---|---|
| `adapters/outbound/approval-remote-ui` | neu | Konkreter Remote/UI-Approval-Kanal hinter dem Kanalvertrag (`ARC-08`). |
| `adapters/outbound/approval-remote-ui/src/commonMain/**` | neu | Serialisierung, Transport-Abstraktion, Antwortvalidierung, Fail-Closed-Policy. |
| `adapters/outbound/approval-remote-ui/src/commonTest/**` | neu | Hermetische Transport-Fakes fuer Positivfall und Negativmatrix. |
| `settings.gradle.kts` | update | Neues Adaptermodul registrieren. |
| `.a-check.yml` | update | Adapterrolle und erlaubte Kanten aufnehmen; keine fachliche Adapterkopplung. |
| `Dockerfile` | update | Neues Modul in Build-, Test- und Coverage-Stages aufnehmen. |
| `docs/user/integration.md` | update | Remote/UI-Kanal, lokale Testbarkeit und Produktivgrenzen dokumentieren. |
| `docs/user/cli-entscheidungsnachweis.md` | update | Safety-Nachweis um Remote/UI-Negativpfade ergaenzen. |
| `docs/reviews/*slice-039*` | neu | Code-/Safety-Review-Artefakt fuer Remote/UI-Kanal. |
| `docs/verifications/*slice-039*` | neu | Verification-Artefakt fuer DoD und Negativmatrix. |

## 4. Trigger

`slice-038` liegt in `done/` und liefert die fail-closed Kanalwahl. Kein Slice
liegt in `in-progress/` (WIP-Limit 1). Vor Start wird bestaetigt, ob der
Kanaladapter in einem eigenen Modul (`approval-remote-ui`) entsteht und ob die
Transportgrenze ohne Live-Netzwerk in Tests bedienbar ist.

## 5. Closure-Trigger

DoD vollstaendig + Review/Verification abgeschlossen + `make gates` gruen +
Closure-Notiz geschrieben + Slice nach `done/` verschoben.

## 6. Risiken und offene Punkte

- Remote/UI fuehrt neue Failure-Modes ein. Timeouts, doppelte Antworten,
  Replay und kaputte Serialisierung muessen fail-closed sein.
- Lokale Gates muessen hermetisch bleiben. Live-Netzwerk, echter Browser oder
  externer UI-Dienst duerfen nicht fuer `make gates` erforderlich werden.
- Authentisierung/Autorisierung eines produktiven Remote-Kanals kann groesser
  werden als dieser Slice. Wenn mehr als lokale Identitaets-/Antwortvalidierung
  noetig wird, entsteht ein Folge-Slice oder eine ADR.
- Persistenter Approval-Audit bleibt eigener Slice. Dieser Slice darf nur die
  auditierbaren Ereignisse/IDs benennen, nicht eine dauerhafte Speicherung
  einfuehren.

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss fuellen. -->

## 8. Sub-Area-Modus-Begründung

### Sub-Area: Remote/UI-Approval-Kanal

- **Modus:** Hybrid
- **Konventionen-Dichte:** mittel. `slice-035` liefert den Kontextvertrag,
  `slice-038` die Kanalwahl; ein Remote/UI-Kanal ist neuer Adapterbestand.
- **Phase-Reife:** Phase 2-3. Die Port-/Kanalgrenzen sind geplant, konkrete
  Remote-/UI-Transportregeln entstehen erst in diesem Slice.
- **Evidenz-/Diskrepanz-Risiko:** hoch. Remote/UI-Antworten koennen durch
  Timeout, Replay, Identitaetsfehler oder Kontextdrift unsicher werden.
- **Reconciliation-Aufwand:** ein Slice fuer Kanaladapter + hermetische
  Negativmatrix. Produktive Authentisierung und Audit-Persistenz bleiben
  Folge-Slices.

### Sub-Area: Approval-Kanalwahl / Composition

- **Modus:** Hybrid
- **Konventionen-Dichte:** mittel. `slice-038` definiert die fail-closed
  Kanalwahl; dieser Slice konsumiert sie mit einem neuen konkreten Kanal.
- **Phase-Reife:** Phase 3-4 nach `slice-038`. Der Dispatcher existiert, aber
  weitere Kanaele sind neu.
- **Evidenz-/Diskrepanz-Risiko:** mittel bis hoch. Falsche Registrierung darf
  den Default nicht oeffnen und darf nicht mehrere Kanaele pro Anfrage
  konsumieren.
- **Reconciliation-Aufwand:** gering bis mittel; neue Kante in Arch-/Doku- und
  Build-Konfiguration.
