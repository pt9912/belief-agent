# Slice slice-041: Persistenter AuditPort-Adapter

**Status:** open (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung.

**Bezug:** `LH-FA-AUD-001`, `LH-FA-AUD-002`, `LH-FA-AUD-003`,
`LH-QA-02`, `LH-QA-03`, `LH-QA-04`; `ADR-0001`, `ADR-0002`, `ADR-0003`,
`ARC-06`, `ARC-08`, `ARC-09`.

**Autor:** Codex. **Datum:** 2026-07-08.

---

## 1. Ziel

Ein echter, nicht-Memory-Outbound-Adapter hinter dem bestehenden `AuditPort`
ersetzt `MemoryAudit` fuer persistente Audit-Spuren: Ereignisse werden
append-only gespeichert, nach Prozess-Neustart rekonstruierbar geladen und bei
Schreib-/Lesefehlern fail-closed sichtbar gemacht. Der Slice liefert damit den
kleinsten dauerhaften Audit-Adapter; allgemeine Audit-Datenbank-Funktionen wie
Retention, Migrationen, Backups und Compliance-Export bleiben Folgearbeit.

## 2. Definition of Done

- [ ] Neues Outbound-Adaptermodul (z. B. `audit-file` oder nach Design-Review
  gleichwertig) implementiert `AuditPort` hinter `ARC-08`; `hexagon:*`
  importiert keine Storage-/IO-Pakete und keinen Adapter.
- [ ] Der Adapter speichert alle bestehenden `Ereignis`-Typen append-only und
  geordnet; vorhandene Ereignisse werden nie ueberschrieben oder geloescht. Die
  Speichertechnologie ist lokal hermetisch testbar; falls eine neue
  DB-/Storage-Technologie eingefuehrt wird, ist vor Code ein Design-Review oder
  Folge-ADR erstellt (`ADR-0002`/`0003`).
- [ ] Restart-/Replay-Verhalten ist deterministisch getestet (`LH-QA-03`):
  Laden nach Neustart, leere Datenbank, korruptes/teilgeschriebenes Ereignis,
  Schreibfehler, Reihenfolge und Rekonstruktion ueber `AuditPort.lade()` sind
  abgedeckt; Fehler werden nicht still als leeres Protokoll behandelt
  (`LH-QA-02`, `LH-FA-AUD-002`).
- [ ] Der Adapter validiert die persistierte Ereignisfolge vor der Rueckgabe:
  unbekannte Typen, fehlende Pflichtfelder, Rueckdatierung oder nicht
  rekonstruierbare Sequenzen erzeugen einen sichtbaren Fehler statt einer
  gate-faehigen Teilhistorie (`LH-FA-AUD-001`, `LH-FA-AUD-003`).
- [ ] Build-/Arch-/Doku-Integration ist vollstaendig: Modul in
  `settings.gradle.kts`, `.a-check.yml`, `Dockerfile`/Coverage-Gate und
  Integrationsdoku aufgenommen; CLI-/Runtime-Binding bleibt separater bewusster
  Schritt, falls es ueber Test-/Demo-Konfiguration hinausgeht. Review-/
  Verification-Artefakte, `make doc-check`, `make gates` und Closure-Notiz
  liegen vor.

## 3. Plan (vor Code)

| Datei / Komponente | Aenderungs-Art | Begruendung |
|---|---|---|
| `adapters/outbound/audit-file` oder enger benannter Adapter | neu | Persistenter nicht-Memory-`AuditPort`-Adapter hinter `ARC-08`. |
| `.../src/main/kotlin/**` | neu | Append-only Speicherung, Serialisierung, Laden/Rekonstruktion und Fehlergrenze. |
| `.../src/test/kotlin/**` | neu | Restart-, Korruptions-, Schreibfehler-, Reihenfolge- und Rekonstruktionstests. |
| `settings.gradle.kts` | update | Neues Adaptermodul registrieren. |
| `.a-check.yml` | update | Adapterrolle und erlaubte Kanten aufnehmen; Kern bleibt adapterfrei. |
| `Dockerfile` | update | Neues Modul in Build-, Test- und Coverage-Stages aufnehmen. |
| `docs/user/integration.md` | update | Persistenten Audit-Adapter, lokale Speicherform und Fehlerverhalten dokumentieren. |
| `docs/user/cli-entscheidungsnachweis.md` | update | Persistente Audit-Spur als optionalen Nachweis benennen, ohne CLI-Default umzubinden. |
| `docs/reviews/*slice-041*` | neu | Design-/Code-Review-Artefakt fuer persistenten AuditPort-Adapter. |
| `docs/verifications/*slice-041*` | neu | Verification-Artefakt fuer DoD und Restart-/Replay-Matrix. |

## 4. Trigger

`slice-010` liegt in `done/` und stellt `MemoryAudit` als deterministischen
Stand-in bereit; `slice-022` ergaenzt weitere Audit-Ereignisse fuer
externalisierte Konfidenz. Kein Slice liegt in `in-progress/` (WIP-Limit 1).
Vor Start wird entschieden, ob ein einfacher lokaler append-only Datei-Adapter
ausreicht; falls eine neue DB-/Storage-Technologie eingefuehrt wird, ist vor
Code ein Design-Review oder eine ADR-Pruefung noetig. `slice-040` kann spaeter
weitere Approval-Audit-Ereignisse liefern, darf aber nicht Voraussetzung fuer
den ersten echten `AuditPort`-Adapter sein.

## 5. Closure-Trigger

DoD vollstaendig + Review/Verification abgeschlossen + `make gates` gruen +
Closure-Notiz geschrieben + Slice nach `done/` verschoben.

## 6. Risiken und offene Punkte

- Dauerhaftigkeit darf keine stillen Recovery-Pfade erfinden: korrupte oder
  teilgeschriebene Daten muessen diagnostiziert werden und duerfen nicht als
  valides leeres Protokoll erscheinen.
- Neue Storage-Technologie kann Architektur-/Build-Risiko erzeugen. Wenn die
  Wahl nicht trivial ist, braucht sie Design-Review oder ADR vor Implementation.
- CLI-Produktivbinding an den persistenten Audit-Adapter bleibt separater
  Folgeslice, falls es Konfiguration, Pfadpolitik oder Migration beruehrt.
- Retention-Policy, Datenbankmigrationen, Backups und externe Compliance-Exports
  bleiben Folgeslices; dieser Slice liefert nur dauerhafte lokale
  Append-only-Persistenz hinter `AuditPort`.

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss fuellen. -->

## 8. Sub-Area-Modus-Begründung

### Sub-Area: Audit-Persistenzadapter (`adapters/outbound/audit-*`)

- **Modus:** Hybrid
- **Konventionen-Dichte:** hoch fuer `AuditPort` und append-only Verhalten
  (`slice-007`, `slice-010`, `ARC-06`), mittel fuer dauerhafte lokale
  Storage-Technologie, die noch nicht festgelegt ist.
- **Phase-Reife:** Phase 4 fuer In-Memory-Audit, Phase 2-3 fuer persistente
  lokale Speicherung. Der Port ist stabil; der nicht-Memory-Adapter ist neu.
- **Evidenz-/Diskrepanz-Risiko:** hoch. Still verlorene oder falsch
  rekonstruierte Audit-Ereignisse verletzen die Entscheidungsspur.
- **Reconciliation-Aufwand:** ein Slice fuer Adapter, Tests, Build-/Arch- und
  Doku-Integration. Retention/Backups/Exports bleiben Folgearbeit.

### Sub-Area: Runtime-/CLI-Integration

- **Modus:** GF
- **Konventionen-Dichte:** mittel. `ARC-09` verortet Binding im
  Composition-Root; dieser Slice soll den Adapter bereitstellen, aber den
  produktiven CLI-Default nicht nebenbei umstellen.
- **Phase-Reife:** Phase 4. CLI-Composition ist stabil; dauerhafte Auditwahl ist
  ein bewusster Folgeschritt.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Eine unbemerkte Default-Umbindung kann
  lokale Demos oder hermetische Tests brechen.
- **Reconciliation-Aufwand:** gering in diesem Slice; eigenes Binding/Migration-
  Slice, falls Runtime-Default oder Pfadpolitik geaendert werden.
