# Slice slice-052: Read-Konsumenten — geordnete Audit-Lesefehler-Behandlung

**Status:** open (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung (Audit-Persistenz-Folgeslices).

**Bezug:** `LH-QA-02`, `LH-QA-03`, `LH-QA-06`, `LH-FA-AUD-002`; `ARC-06`.

**Autor:** Claude. **Datum:** 2026-07-09.

---

## 1. Ziel

Die heute observability-only `AuditPort.lade()`-Konsumenten
(`Runtime.auditEreignisse()`, code-agent-Beispiel) behandeln einen geworfenen
Audit-Lesefehler **geordnet** (diagnostizierte Meldung/Eskalation), statt den
Prozess uncaught abzubrechen. Damit gilt die fail-safe-Sichtbarkeit (`LH-QA-02`)
auch auf dem Read-Pfad und nicht nur an der Adapter-Grenze.

## 2. Definition of Done

- [ ] `Runtime.auditEreignisse()` und das code-agent-Beispiel fangen einen
  `lade()`-Lesefehler und melden ihn geordnet (Diagnose/Eskalation), statt
  uncaught abzubrechen (`LH-QA-02`); die Inspizierbarkeit (`LH-QA-06`) bleibt
  gewahrt.
- [ ] Deterministischer Test (`LH-QA-03`) mit korruptem-Store-Fixture zeigt die
  geordnete Behandlung; `make gates` grün.

## 3. Plan (vor Code)

| Datei / Komponente | Aenderungs-Art | Begruendung |
|---|---|---|
| `adapters/inbound/cli/.../Runtime.kt` | update | `auditEreignisse()`/`lade()` in geordnete Fehlerbehandlung fassen. |
| `example/code-agent/.../Main.kt` | update | `audit.lade()` (Anzeige) gegen Lesefehler absichern. |
| `adapters/inbound/cli/src/test/**` | neu/update | Korrupt-Store-Fixture → geordnete Meldung statt Abbruch. |

## 4. Trigger

Aus [`slice-041`](slice-041-dauerhafte-audit-datenbank.md) §9 (DR-R1): fällig,
sobald ein persistenter (werfender) Adapter in einen `lade()`-Pfad gebunden wird
**oder** ein Slice `lade()`/`Rekonstruktion` in einen Gate-/Entscheidungspfad
führt. Unter heutigem observability-only-Konsum ist der uncaught-Wurf ein lauter
Inspektions-Fehlschlag (fail-safe-vertretbar), aber nicht geordnet.

## 5. Closure-Trigger

DoD vollständig + Review/Verification abgeschlossen + `make gates` grün +
Closure-Notiz geschrieben + Slice nach `done/` verschoben.

## 6. Risiken und offene Punkte

- Geordnete Behandlung darf einen echten Lesefehler nicht zu einem leeren
  Protokoll verharmlosen (kein fail-open); Sichtbarkeit bleibt Pflicht.
- Wird `lade()`/`Rekonstruktion` künftig ein Gate-Input, ist die Anforderung
  strenger (Eskalation statt bloßer Diagnose) — dann mit `slice-041`-Vorbedingung
  abgleichen.

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss fuellen. -->

## 8. Sub-Area-Modus-Begründung

### Sub-Area: Audit-Read-Pfad (Observability)

- **Modus:** Hybrid
- **Konventionen-Dichte:** mittel. `lade()`-Konsum ist heute ungeschützt; keine
  Read-Fehler-Konvention verankert.
- **Phase-Reife:** Phase 3. Konsumenten stabil, Fehlerbehandlung neu.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Uncaught-Abbruch ist laut, aber nicht
  geordnet; kein stiller Datenverlust.
- **Reconciliation-Aufwand:** ein kleiner Slice (zwei Konsumenten + Test).
