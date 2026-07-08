# Slice slice-046: Persistenter KonfidenzPort-Adapter

**Status:** open (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung.

**Bezug:** `LH-FA-LLM-003`, `LH-FA-AUD-001`, `LH-FA-AUD-003`,
`LH-FA-POL-006`, `LH-QA-02`, `LH-QA-03`, `LH-QA-04`; `ADR-0001`,
`ADR-0003`, `ADR-0006`, `ARC-06`, `ARC-07`, `ARC-08`, `ARC-09`.

**Autor:** Codex. **Datum:** 2026-07-08.

---

## 1. Ziel

Ein echter, nicht-Memory-Outbound-Adapter implementiert `KonfidenzPort` fuer
persistente, append-only externalisierte Modell-Konfidenzen und Overrides, damit
Konfidenz-Historien restart-fest geladen werden koennen, ohne Gate-,
Audit- oder Runtime-Binding-Scope zu vermischen.

## 2. Definition of Done

- [ ] Neues Outbound-Adaptermodul (z. B. `konfidenz-file` oder nach
  Design-Review gleichwertig) implementiert `KonfidenzPort` hinter `ARC-08`;
  `hexagon:*` importiert keine Storage-/IO-Pakete und keinen Adapter.
- [ ] Der Adapter speichert `ExternalisierteKonfidenz` append-only je
  `KonfidenzReferenz`; bestehende Versionen werden nie mutiert oder geloescht,
  und neue Eintraege muessen die Versionsfolge `1..n` pro Referenz fortsetzen.
- [ ] Restart-/Replay-Verhalten ist lokal und deterministisch getestet:
  leerer Store, Laden nach Neustart, mehrere Referenzen, Overrides, Versionen
  ausser Reihenfolge, doppelte Versionen und sortierte Historie in
  Einfuege-/Persistenzreihenfolge (`LH-QA-03`).
- [ ] Fehlerfaelle sind fail-closed sichtbar getestet: kaputtes Format,
  teilgeschriebener Eintrag, nicht-endliche oder ausserhalb `[0,1]` liegende
  Werte, leere Referenz/Quelle, leere Override-Begruendung, IO-/Schreibfehler
  und unlesbarer Store duerfen keine gate-faehige Teilhistorie erzeugen
  (`LH-QA-02`, `LH-FA-LLM-003`).
- [ ] Build-/Arch-/Coverage-Integration ist vollstaendig:
  `settings.gradle.kts`, `.a-check.yml`, `Dockerfile`, Modul-`build.gradle.kts`
  und Kover-Gate sind ergaenzt (`ADR-0003`, `ADR-0006`).
- [ ] Nutzer-/Integrationsdoku und Verification-Artefakt beschreiben den
  persistenten Adapter, seine lokale Speicherform und Fehlerpolitik; kein
  CLI-Default-Binding, keine produktive Pfad-/Secret-Policy und keine allgemeine
  Audit-Datenbank werden in diesem Slice eingefuehrt.
- [ ] `make doc-check` und `make gates` sind gruen; Closure-Notiz benennt, ob
  CLI-/Runtime-Binding oder Migration/Retention als Folgeslice noetig ist.

## 3. Plan (vor Code)

| Datei / Komponente | Aenderungs-Art | Begruendung |
|---|---|---|
| `adapters/outbound/konfidenz-file` oder enger benannter Adapter | neu | Persistenter Outbound-Adapter hinter `KonfidenzPort` (`ARC-08`). |
| `.../src/main/kotlin/**` | neu | Append-only Store, Parser/Serializer, Versionsvalidierung, Fehlergrenze. |
| `.../src/test/kotlin/**` | neu | Restart-/Replay-, Korruptions-, Versionierungs- und IO-Fehler-Tests. |
| `settings.gradle.kts` | update | Neues Adaptermodul registrieren. |
| `.a-check.yml` | update | Adapterrolle und erlaubte Kanten aufnehmen; Core bleibt storagefrei. |
| `Dockerfile` | update | Dependency-, Test- und Coverage-Gate-Stages um das Modul ergaenzen. |
| `docs/user/integration.md` | update | Persistenten Konfidenz-Adapter und lokale Speicherform dokumentieren. |
| `docs/reviews/*slice-046*` | neu | Review-Artefakt mit Fokus Append-only/Fail-closed/Storage-Grenze. |
| `docs/verifications/*slice-046*` | neu | Verification-Artefakt fuer DoD, Restart-/Replay-Matrix und Gates. |

## 4. Trigger

`slice-022`, `slice-027` und `slice-028` liegen in `done/`: der
Konfidenz-Contract, der Memory-/Replay-Adapter und der Zyklus-/Gate-Konsum sind
stabil. Kein Slice liegt in `in-progress/` (WIP-Limit 1). Vor Start wird
entschieden, ob ein einfacher lokaler Datei-Adapter ausreicht; falls eine neue
DB-/Storage-Technologie eingefuehrt wird, ist vor Code ein Design-Review oder
eine ADR-Pruefung noetig.

## 5. Closure-Trigger

DoD vollstaendig + Review/Verification abgeschlossen + `make gates` gruen +
Closure-Notiz geschrieben + Slice nach `done/` verschoben.

## 6. Risiken und offene Punkte

- Persistenzfehler duerfen nicht wie eine leere valide Historie aussehen. Bei
  unlesbarem oder korruptem Store muss der Fehler sichtbar und fail-closed sein.
- Der Adapter darf keine Gate-Entscheidung treffen und keine Version als
  "neueste" reparieren, wenn die append-only Folge kaputt ist.
- Dieser Slice ist kein allgemeiner Audit-Datenbank-Slice und ersetzt nicht
  `AuditPort`; Audit-Persistenz, Retention, Migrationen und Compliance-Exports
  bleiben getrennte Slices.
- CLI-/Runtime-Default-Binding, Pfadpolitik und Migration bestehender
  Memory-Fixtures bleiben Folgeslices, falls benoetigt.

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss fuellen. -->

## 8. Sub-Area-Modus-Begründung

### Sub-Area: `adapters/outbound/konfidenz-*`

- **Modus:** Hybrid
- **Konventionen-Dichte:** hoch fuer `KonfidenzPort`, append-only Versionierung
  und Adapterrolle (`slice-022`, `slice-027`, `ARC-08`, `ADR-0003`), mittel fuer
  die neue nicht-Memory-Speicherform.
- **Phase-Reife:** Phase 4 fuer Contract und Memory-/Replay-Adapter, Phase 2-3
  fuer persistente lokale Speicherung.
- **Evidenz-/Diskrepanz-Risiko:** hoch. Falsch rekonstruierte oder still
  verworfene Konfidenzhistorien koennen Gate-Eingaben unbemerkt veraendern.
- **Reconciliation-Aufwand:** Teil dieses Slice: Append-only-, Restart-,
  Korruptions- und IO-Fehler-Matrix plus Build-/Arch-/Coverage-Integration.
  Graduation-Trigger: `make gates` gruen und Verification ohne offene
  Persistenz-/Versionsdrift.

### Sub-Area: `hexagon/application/belief/ports`

- **Modus:** GF
- **Konventionen-Dichte:** hoch. `KonfidenzPort` ist business-area-geteilter
  Contract und bleibt unveraendert; Adapter implementieren nur den bestehenden
  Port.
- **Phase-Reife:** Phase 4. Contract-Typen und Zyklus-Konsum sind stabil.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Eine Port-Aenderung wuerde mehrere
  Use Cases und den Gate-Pfad beruehren; dieser Slice plant keine Contract-
  Aenderung.
- **Reconciliation-Aufwand:** keiner im Core. Falls der Port fuer Fehlerklassen
  erweitert werden muss, wird ein separater Contract-Slice geplant.

### Sub-Area: `adapters/inbound/cli` / Runtime-Binding

- **Modus:** GF
- **Konventionen-Dichte:** hoch. `ARC-09` verortet Runtime-Binding im
  Composition-Root; dieser Slice soll keinen CLI-Default und keine Pfadpolitik
  veraendern.
- **Phase-Reife:** Phase 4. CLI-Composition nutzt derzeit `MemoryKonfidenzPort`;
  persistentes Binding ist ein bewusster Folgeschritt.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Eine nebenbei geaenderte Runtime-
  Verdrahtung koennte lokale Demos, Fixture-Replay oder hermetische Gates
  veraendern.
- **Reconciliation-Aufwand:** keiner in diesem Slice; eigener Binding-/Migration-
  Slice, falls CLI-Flags, Speicherpfade oder Defaults geaendert werden.
