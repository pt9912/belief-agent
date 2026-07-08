# Slice slice-040: Approval-Audit-Persistenz

**Status:** done (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung.

**Bezug:** `LH-FA-POL-004`, `LH-FA-POL-006`, `LH-FA-AUD-001`,
`LH-FA-AUD-002`, `LH-FA-AUD-003`, `LH-OUT-04`, `LH-QA-02`, `LH-QA-03`,
`LH-QA-04`; `ADR-0001`, `ADR-0003`; `ARC-06`, `ARC-07`, `ARC-08`, `ARC-09`.

**Autor:** Codex. **Datum:** 2026-07-08.

---

## 1. Ziel

Der Approval-Vorgang wird append-only auditierbar: Anfrage, Kanalwahl,
Antwortentscheidung und Ablehnungs-/Fehlergrund werden als nachvollziehbare
Audit-Ereignisse persistiert, ohne die Freigabe selbst zu umgehen oder den
Executor-Pfad zu erweitern.

## 2. Definition of Done

- [ ] Approval-Audit-Ereignisse sind als stabiler Contract modelliert:
  `ApprovalAngefragt`, `ApprovalErteilt`, `ApprovalVerweigert` und
  `ApprovalFehler` oder gleichwertig enthalten Anfrage-/Kontext-Digest,
  Kanal, Nonce/Antwortreferenz, Identitaetsreferenz und Ergebnisgrund, aber
  keine sensiblen Klartext-Geheimnisse (`LH-FA-AUD-001`/`003`).
- [ ] `HumanApprovalPort` liefert keinen nackten Boolean mehr, sondern ein
  adapterfreies Approval-Ergebnis mit Freigabeentscheidung und
  `ApprovalAuditSnapshot` oder gleichwertig: Anfrage-/Kontext-Digest, gewaehlter
  Kanal, Nonce-/Antwortreferenz, Identitaetsreferenz und kanalinterner
  Ergebnisgrund. Adapterdetails, UI-Tokens und Transportobjekte bleiben
  ausserhalb des Core-Vertrags.
- [ ] Der Approval-Pfad schreibt append-only ueber den bestehenden `AuditPort`
  aus `AktionGaten`: Der Kanal/Dispatcher liefert die Audit-Metadaten, aber die
  finale Ereigniserzeugung fuer Anfrage, Kanalantwort, Gate-Ausgang und
  Fehlergrund passiert an der Stelle, die auch den endgueltigen
  `Aktionsfreigabe`-Ausgang kennt. Audit-Ausfall ist fail-closed fuer
  extern-wirksame Aktionen, wenn dadurch die Entscheidungsspur fehlen wuerde
  (`LH-QA-02`, `LH-FA-POL-004`, `LH-OUT-04`).
- [ ] Persistenz-/Replay-Verhalten ist deterministisch getestet (`LH-QA-03`):
  Ereignisreihenfolge, keine Ueberschreibung, Rekonstruktion eines
  Approval-Vorgangs, Audit-Ausfall, verweigerte Freigabe und erfolgreiche
  Freigabe mit `Zyklusergebnis.Gehandelt.freigabe.aktion` bleiben nachvollziehbar.
  Build-/Arch-/Doku-Integration, Review-/Verification-Artefakte,
  `make doc-check`, `make gates` und Closure-Notiz liegen vor.

## 3. Plan (vor Code)

| Datei / Komponente | Aenderungs-Art | Begruendung |
|---|---|---|
| `hexagon/domain/src/commonMain/**/Ereignis.kt` oder approval-audit Contract | update/neu | Approval-Audit-Ereignisse append-only und rekonstruierbar modellieren (`ARC-06`); Ereignisse enthalten nur Referenzen/Digests, keine Adapterobjekte. |
| `hexagon/application/src/commonMain/**/gaten/ports/HumanApprovalPort.kt` | update | Port-Rueckgabe von Boolean auf adapterfreies Approval-Ergebnis mit `ApprovalAuditSnapshot` schaerfen, damit Kanal, Nonce-/Antwortreferenz, Identitaetsreferenz und Ergebnisgrund den Core erreichen. |
| `hexagon/application/src/commonMain/**/gaten/AktionGaten.kt` | update | Einziger Erzeugungsort der finalen Approval-Entscheidungsspur: schreibt Anfrage-/Antwort-/Fehler-/Gate-Ausgangsereignisse ueber `AuditPort` und bleibt Besitzer der finalen `Aktionsfreigabe`. |
| `adapters/inbound/cli/src/main/**` | update | Kanal-Dispatcher reicht den ausgewaehlten Kanal und kanalbezogene Fehler als Port-Ergebnis weiter; er schreibt nicht direkt in `AuditPort`. |
| `adapters/outbound/approval-local/**` und `adapters/outbound/approval-remote-ui/**` | update | Kanaele liefern `ApprovalAuditSnapshot`-Daten aus Nonce, Kontext-Digest, Identitaet und Antwortreferenz; keine direkte Core-/AuditPort-Kopplung. |
| `hexagon/application/src/commonTest/**` | update | Tests fuer Ereignisfolge, Audit-Ausfall fail-closed, vollstaendige Snapshot-Felder und keine Executor-Umgehung. |
| `adapters/outbound/audit-memory/**` | update | Falls der bestehende Memory-Adapter neue Ereignistypen serialisieren/replayen muss. |
| `adapters/inbound/cli/src/test/**` | update | E2E-Sicht: Approval-Audit-Spur sichtbar, ohne neue Ausfuehrungsroute. |
| `.a-check.yml` | update | Nur falls neue Audit-/Approval-Komponente entsteht; Kanten bleiben innenorientiert. |
| `Dockerfile` | update | Nur falls neue Module/Tests in Build-/Coverage-Stages aufgenommen werden muessen. |
| `docs/user/integration.md` | update | Approval-Audit-Persistenz, Fail-Closed bei Audit-Ausfall und Datenschutzgrenze dokumentieren. |
| `docs/user/cli-entscheidungsnachweis.md` | update | Entscheidungsspur um Approval-Audit-Ereignisse ergaenzen. |
| `docs/reviews/*slice-040*` | neu | Code-/Safety-Review-Artefakt fuer Approval-Audit-Persistenz. |
| `docs/verifications/*slice-040*` | neu | Verification-Artefakt fuer DoD, Sensoren und Replay-/Audit-Negativmatrix. |

## 4. Trigger

`slice-039` liegt in `done/` und liefert einen konkreten Remote/UI-Kanal hinter
der Kanalwahl. Kein Slice liegt in `in-progress/` (WIP-Limit 1). Vor Start wird
bestaetigt, dass der bestehende `AuditPort` der Schreibpfad bleibt und dass die
Approval-Metadaten ueber eine Port-Rueckgabe/Snapshot-Grenze aus dem gewaehlten
Kanal zum `AktionGaten` gelangen. Falls stattdessen ein separater
Approval-Audit-Port oder eine andere Ereigniserzeugungsschicht noetig wird,
entsteht vor Code ein Design-Review/Folge-ADR.

## 5. Closure-Trigger

DoD vollstaendig + Review/Verification abgeschlossen + `make gates` gruen +
Closure-Notiz geschrieben + Slice nach `done/` verschoben.

## 6. Risiken und offene Punkte

- Audit darf keine Freigabe ersetzen. Der Executor bleibt an
  `Zyklusergebnis.Gehandelt.freigabe.aktion` gebunden.
- Audit-Ausfall ist Safety-relevant. Wenn die Entscheidungsspur fuer
  extern-wirksame Aktionen nicht geschrieben werden kann, muss der Pfad
  fail-closed oder eskalierend enden.
- Approval-Audit darf keine sensiblen Geheimnisse oder vollstaendige UI-Token
  persistieren. Persistiert werden Referenzen/Digests, nicht Roh-Geheimnisse.
- Allgemeine dauerhafte Audit-Datenbank, Retention-Policy und externe
  Compliance-Exports bleiben Folgeslices, falls sie mehr als lokale
  Persistenz-/Replay-Faehigkeit erfordern.
- Der Kanal-Dispatcher sieht Kanalwahl und Adapterfehler, aber nicht den
  finalen Gate-Ausgang. Er darf deshalb keine abschliessenden
  Approval-Entscheidungsereignisse schreiben, sondern liefert nur ein
  adapterfreies Ergebnis an `AktionGaten`.
- `AktionGaten` sieht den finalen Gate-Ausgang, aber keine Adapterdetails. Die
  Snapshot-Grenze muss deshalb alle auditrelevanten Kanalmetadaten explizit
  transportieren, ohne den Core an `approval-local` oder `approval-remote-ui` zu
  koppeln.

## 7. Closure-Notiz (nach `done/`)

Abgeschlossen am 2026-07-08. Implementiert wurde eine append-only
Approval-Audit-Spur ueber den bestehenden `AuditPort`: `HumanApprovalPort`
liefert ein adapterfreies `ApprovalErgebnis` mit `ApprovalAuditSnapshot`,
`AktionGaten` schreibt `ApprovalAngefragt` plus Ergebnisereignis und bleibt bei
Audit-Ausfall fail-closed. Local-, Remote/UI- und Fake-Approval sowie CLI- und
Example-Bindings sind auf den neuen Vertrag angepasst.

Review-/Verification-Artefakte:
`docs/reviews/2026-07-08-slice-040-code-safety-review.md` und
`docs/verifications/2026-07-08-slice-040-verification.md`. Ausgefuehrte
Sensoren laut Implementierung/Verification: `git diff --check`, `make test`,
`make doc-check`, `make cli-demo-approval-remote-ui` und `make gates` gruen.
Keine Carveouts.

## 8. Sub-Area-Modus-Begründung

### Sub-Area: Audit/Event-Log (`ARC-06`)

- **Modus:** Hybrid
- **Konventionen-Dichte:** hoch fuer append-only Grundregeln
  (`LH-FA-AUD-001`/`003`, `slice-007`, `slice-010`), mittel fuer
  Approval-spezifische Ereignisse, die noch nicht modelliert sind.
- **Phase-Reife:** Phase 4 fuer allgemeines Audit, Phase 2-3 fuer
  Approval-Audit. Bestehende Memory-Persistenz und Rekonstruktion sind stabil,
  der neue Ereignistyp ist fachlich neu.
- **Evidenz-/Diskrepanz-Risiko:** hoch. Eine fehlende oder ueberschreibbare
  Approval-Spur wuerde `LH-FA-POL-004` praktisch schwer auditierbar machen.
- **Reconciliation-Aufwand:** ein Slice fuer Contract, Erzeugung,
  Port-Rueckgabe-Schaerfung, Memory-/Replay-Integration und Doku; produktive
  Datenbank/Retention bleibt Folgearbeit.

### Sub-Area: Approval-Kanalwahl / Remote-UI

- **Modus:** Hybrid
- **Konventionen-Dichte:** mittel. `slice-038`/`039` liefern Kanalwahl und
  Remote/UI-Kanal; Audit-Persistenz konsumiert deren Ereignisse.
- **Phase-Reife:** Phase 3-4 nach `slice-039`. Die Kanaluebergaenge sind
  vorhanden, aber ihre dauerhafte Entscheidungsspur fehlt noch.
- **Evidenz-/Diskrepanz-Risiko:** mittel bis hoch. Audit darf keine zusaetzliche
  Freigabelogik einfuehren und darf Kontext-/Nonce-Daten nicht unsicher
  speichern.
- **Reconciliation-Aufwand:** mittel; Tests muessen Kanalantwort,
  Snapshot-Transport, Audit-Ereignisse und Executor-Grenze zusammen pruefen.
