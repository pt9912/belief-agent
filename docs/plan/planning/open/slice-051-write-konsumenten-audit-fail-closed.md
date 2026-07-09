# Slice slice-051: Write-Konsumenten — Audit-Schreibfehler fail-closed

**Status:** open (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung (Audit-Persistenz-Folgeslices).

**Bezug:** `LH-FA-AUD-001`, `LH-QA-02`, `LH-QA-03`, `LH-FA-POL-004`;
`ADR-0001`, `ADR-0003`; `ARC-06`.

**Autor:** Claude. **Datum:** 2026-07-09.

---

## 1. Ziel

Die Nicht-`AktionGaten`-Konsumenten von `AuditPort.anhaengen` machen einen
Audit-Schreibfehler **sichtbar/fail-closed**, statt ihn im Validierungs-
`runCatching` zu verschlucken (`AktionsVorschlagen`) oder uncaught zu
propagieren (code-agent-Beispiel). Der Slice trennt „ungültiger Vorschlag
(drop)" von „Audit-Schreibfehler (eskalieren/sichtbar)" und beseitigt den
Teilzustand in `KonfidenzExternalisieren`.

## 2. Definition of Done

- [ ] `AktionsVorschlagen` unterscheidet Validierungsabweisung (drop, wie bisher)
  von einem Audit-Schreibfehler (`audit.anhaengen`): Letzterer wird nicht
  verschluckt, sondern sichtbar/fail-closed signalisiert; das Pflicht-Ereignis
  `AktionVorgeschlagen` geht nicht still verloren (`LH-FA-AUD-001`, `LH-QA-02`).
- [ ] `KonfidenzExternalisieren` hinterlässt bei Audit-Schreibfehler keinen
  Teilzustand (KonfidenzPort geschrieben, Audit nicht) ohne sichtbare Spur.
- [ ] Deterministische Tests (`LH-QA-03`) mit werfendem Audit-Adapter zeigen
  Eskalation/Sichtbarkeit statt stillem `Abgelehnt`; `make gates` grün.

## 3. Plan (vor Code)

| Datei / Komponente | Aenderungs-Art | Begruendung |
|---|---|---|
| `.../aktionsvorschlag/AktionsVorschlagen.kt` | update | Audit-`anhaengen` aus dem Validierungs-`runCatching` herauslösen bzw. Schreibfehler von Validierungsabweisung unterscheiden. |
| `.../konfidenzexternalisieren/KonfidenzExternalisieren.kt` | update | Schreibreihenfolge/Fehlerbehandlung so, dass ein Audit-Fehler keinen stillen Teilzustand lässt. |
| `example/code-agent/.../Main.kt` | update | Generischen Audit-Schreibfehler geordnet behandeln statt uncaught. |
| `hexagon/application/src/commonTest/**` | neu/update | Werfender Audit-Adapter → Eskalation/Sichtbarkeit statt stillem Drop. |

## 4. Trigger

Harte Vorbedingung aus
[`slice-041`](../in-progress/slice-041-dauerhafte-audit-datenbank.md) §9 (IDR-1/IPR-1): fällig
**bevor** ein werfender (persistenter) `AuditPort`-Adapter produktiv in den
Vorschlags-/Konfidenzpfad gebunden wird. Bis dahin bleibt `MemoryAudit` (wirft
nie) der Default — die Lücke ist latent, aber benannt, nicht still.

## 5. Closure-Trigger

DoD vollständig + Review/Verification abgeschlossen + `make gates` grün +
Closure-Notiz geschrieben + Slice nach `done/` verschoben.

## 6. Risiken und offene Punkte

- Die Änderung berührt den Application-Kern (nicht nur einen Adapter); die
  Trennung „Validierungsabweisung vs. Audit-Fehler" muss testbar scharf bleiben.
- Fail-closed auf dem Vorschlagspfad darf keine legitime Validierungsabweisung
  (unbekannte Hypothese, fehlende Evidenz) in eine Eskalation umdeuten.

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss fuellen. -->

## 8. Sub-Area-Modus-Begründung

### Sub-Area: Audit-Write-Pfad (`application/**`)

- **Modus:** Hybrid
- **Konventionen-Dichte:** hoch für append-only/`AuditPort` (`ARC-06`), mittel
  für die Fehlerbehandlungs-Konvention der Write-Konsumenten (heute uneinheitlich).
- **Phase-Reife:** Phase 3-4. Port stabil; die per-Konsument-Fehlerbehandlung ist
  fachlich neu zu vereinheitlichen.
- **Evidenz-/Diskrepanz-Risiko:** hoch. Ein still verschluckter Audit-Schreibfehler
  verletzt die Entscheidungsspur (`LH-FA-AUD-001`).
- **Reconciliation-Aufwand:** ein Slice für die drei Konsumenten + Tests.
