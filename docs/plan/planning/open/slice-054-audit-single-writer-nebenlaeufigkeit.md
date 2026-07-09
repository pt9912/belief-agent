# Slice slice-054: Audit Single-Writer-/Nebenläufigkeits-Absicherung

**Status:** open (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung (Audit-Persistenz-Folgeslices).

**Bezug:** `LH-FA-AUD-001`, `LH-QA-03`, `LH-QA-04`; `ADR-0003`; `ARC-06`.

**Autor:** Claude. **Datum:** 2026-07-09.

---

## 1. Ziel

Der persistente Datei-Audit-Store **erzwingt** sein Single-Writer-Modell (z. B.
exklusives File-Lock) oder erkennt konkurrierenden Zugriff und schlägt
fail-closed fehl — statt bei nebenläufigen/Multi-Prozess-Writern Records zu
verschränken (Interleaving → korrupte Datensätze). Heute gilt die
Single-Writer-Annahme unter der single-threaded Runtime nur implizit.

## 2. Definition of Done

- [ ] Der Adapter erzwingt Single-Writer (exklusives Lock) **oder** erkennt
  konkurrierenden Zugriff und schlägt fail-closed fehl, statt Records zu
  verschränken (`LH-FA-AUD-001`).
- [ ] Deterministischer Test (`LH-QA-03`) mit simuliertem parallelem Writer zeigt
  entweder Serialisierung oder sauberen fail-closed; `make gates` grün.

## 3. Plan (vor Code)

| Datei / Komponente | Aenderungs-Art | Begruendung |
|---|---|---|
| `adapters/outbound/audit-file/**` | update | Lock-/Single-Writer-Durchsetzung beim `anhaengen`/Öffnen. |
| `.../src/test/**` | neu | Paralleler-Writer-Fixture → Serialisierung oder fail-closed. |
| `AuditPort.kt` (KDoc) | ggf. update | Single-Writer-Annahme im Vertrag dokumentieren, falls verallgemeinert. |

## 4. Trigger

Aus [`slice-041`](../in-progress/slice-041-dauerhafte-audit-datenbank.md) §9 (IDR-4): fällig,
sobald die Runtime nebenläufig wird (Multi-Thread-/Multi-Prozess-Schreiber auf
denselben Store). Unter der heutigen single-threaded, sequentiellen Runtime ist
das latent; `slice-041` dokumentiert die Single-Writer-Annahme, dieser Slice
erzwingt sie. Setzt den `audit-file`-Adapter voraus.

## 5. Closure-Trigger

DoD vollständig + Review/Verification abgeschlossen + `make gates` grün +
Closure-Notiz geschrieben + Slice nach `done/` verschoben.

## 6. Risiken und offene Punkte

- File-Locking-Semantik ist plattformabhängig; die Lösung muss zur JVM-Zielplattform
  (`ADR-0002`) passen und hermetisch testbar bleiben (`LH-QA-03`).
- Multi-Prozess-Absicherung kann über reines In-Prozess-Locking hinausgehen; falls
  ein verteiltes Szenario nötig wird, ist das eigene Folgearbeit.

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss fuellen. -->

## 8. Sub-Area-Modus-Begründung

### Sub-Area: Audit-Store-Nebenläufigkeit (`adapters/outbound/audit-file`)

- **Modus:** Hybrid
- **Konventionen-Dichte:** niedrig. Kein Nebenläufigkeitsmodell im Repo verankert
  (`MemoryAudit` ist nicht thread-safe).
- **Phase-Reife:** Phase 2. Setzt den `slice-041`-Store voraus; nur relevant bei
  nebenläufiger Runtime.
- **Evidenz-/Diskrepanz-Risiko:** heute niedrig (single-threaded), hoch sobald
  parallele Writer möglich werden.
- **Reconciliation-Aufwand:** ein Slice (Lock + Test).
