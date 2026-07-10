# Slice slice-045: Realer BeobachtungsAuswahlPort-Adapter

**Status:** open (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung.

**Bezug:** [`LH-FA-OBS-001`](../../../../spec/lastenheft.md#lh-fa-obs-001--heterogene-beobachtungsquellen), [`LH-FA-OBS-004`](../../../../spec/lastenheft.md#lh-fa-obs-004--deduplizierung-korrelierter-beobachtungen), [`LH-FA-OBS-006`](../../../../spec/lastenheft.md#lh-fa-obs-006--zeitstempel-und-quelle-je-beobachtung),
[`LH-FA-VOI-002`](../../../../spec/lastenheft.md#lh-fa-voi-002--diskriminierung-der-zwei-wahrscheinlichsten-hypothesen), [`LH-FA-VOI-003`](../../../../spec/lastenheft.md#lh-fa-voi-003--gewinn-kosten-abwägung), [`LH-FA-VOI-004`](../../../../spec/lastenheft.md#lh-fa-voi-004--lokaleheuristische-voi-bewertung), [`LH-FA-ESK-001`](../../../../spec/lastenheft.md#lh-fa-esk-001--eskalationsbedingung),
[`LH-QA-02`](../../../../spec/lastenheft.md#lh-qa-02--konservatives-standardverhalten-fail-safe), [`LH-QA-03`](../../../../spec/lastenheft.md#lh-qa-03--testbarkeit), [`LH-QA-04`](../../../../spec/lastenheft.md#lh-qa-04--erweiterbarkeit); [`ADR-0001`](../../adr/0001-hexagonal-llm-port.md), [`ADR-0003`](../../adr/0003-hexslice-architektur.md), [`ADR-0006`](../../adr/0006-coverage-gate-scope.md),
`ARC-04`, `ARC-07`, `ARC-08`, `ARC-09`.

**Autor:** Codex. **Datum:** 2026-07-08.

---

## 1. Ziel

Ein echter, lokal testbarer Outbound-Adapter implementiert
`BeobachtungsAuswahlPort` und liefert belief-abhaengige `VoiKandidat`en aus
strukturierten lokalen Beobachtungs-/Kandidaten-Eingaben, ohne die
VoI-Selektionslogik, Eskalationslogik oder Runtime-Composition zu veraendern.

## 2. Definition of Done

- [ ] Neues Outbound-Adaptermodul (z. B. `observation-voi-catalog` oder enger
  benannt) implementiert `BeobachtungsAuswahlPort` hinter `ARC-08`; `hexagon:*`
  importiert keine Adapter-/IO-/Framework-Pakete.
- [ ] Der Adapter liest eine lokal hermetisch testbare Kandidatenquelle
  (z. B. JSON/Fixture oder explizite lokale Datenstruktur) und mappt sie strikt
  in `VoiKandidat`: `Beobachtung` mit `Quelle`, `Zeitstempel`, `Evidenz`,
  `erwarteteDiskriminierung >= 0` und `kosten > 0`.
- [ ] Fail-closed Verhalten ist getestet: kaputtes Format, unbekannte Quelle,
  fehlende Evidenz, nicht-monotone oder ungueltige Zeitstempel, negative/NaN-
  Diskriminierung, nicht-positive/NaN-Kosten und leere Kandidaten werden
  sichtbar abgelehnt oder liefern bewusst keine guenstige Beobachtung
  ([`LH-QA-02`](../../../../spec/lastenheft.md#lh-qa-02--konservatives-standardverhalten-fail-safe), [`LH-QA-03`](../../../../spec/lastenheft.md#lh-qa-03--testbarkeit)).
- [ ] Der Adapter bleibt belief-aware, aber enthaelt keine Auswahlentscheidung:
  er darf Kandidaten nach Top-2-Hypothesen oder aehnlichem lokalem Kontext
  anbieten; `VoiSelektor` bleibt die einzige Auswahlregel fuer Gewinn/Kosten.
- [ ] Keine Adapter-zu-Adapter-Kopplung: bestehende `observation-build-report`
  oder `observation-git-local` werden nicht importiert. Wenn deren Daten genutzt
  werden sollen, geschieht das ueber ein dokumentiertes lokales Eingabeformat
  oder einen spaeteren Composition-Slice.
- [ ] Build-/Arch-/Coverage-Integration ist vollstaendig:
  `settings.gradle.kts`, `.a-check.yml`, `Dockerfile`, Modul-`build.gradle.kts`
  und Kover-Gate sind ergaenzt ([`ADR-0003`](../../adr/0003-hexslice-architektur.md), [`ADR-0006`](../../adr/0006-coverage-gate-scope.md)).
- [ ] Nutzer-/Integrationsdoku und Verification-Artefakt beschreiben den
  Adapter, aber kein CLI-Default-Binding, keine Live-Provider- oder
  Shell-Ausfuehrung und keine Aenderung an Eskalation/Gate/Executor.
- [ ] `make doc-check` und `make gates` sind gruen; Closure-Notiz benennt, ob
  ein Binding-Slice fuer `example/code-agent` oder CLI noetig ist.

## 3. Plan (vor Code)

| Datei / Komponente | Aenderungs-Art | Begruendung |
|---|---|---|
| `adapters/outbound/observation-voi-catalog` oder enger benannter Adapter | neu | Echter Outbound-Adapter hinter `BeobachtungsAuswahlPort` (`ARC-08`). |
| `.../src/main/kotlin/**` | neu | Parser/Loader, Top-2-/Belief-Kontext-Mapping und Mapping zu `VoiKandidat`. |
| `.../src/test/kotlin/**` | neu | Lokale Fixture-/Parser-Tests, Fehlerfallmatrix, belief-aware Kandidaten und leere Kandidaten. |
| `settings.gradle.kts` | update | Neues Adaptermodul registrieren. |
| `.a-check.yml` | update | Adapterrolle und erlaubte Kanten aufnehmen; Core bleibt adapterfrei. |
| `Dockerfile` | update | Dependency-, Test- und Coverage-Gate-Stages um das Modul ergaenzen. |
| `docs/user/integration.md` | update | Lokales Kandidatenformat, Fehlerverhalten und bewusste Nicht-Bindung dokumentieren. |
| `docs/reviews/*slice-045*` | neu | Review-Artefakt mit Fokus Port-Trennung und Adapter-zu-Adapter-Kopplung. |
| `docs/verifications/*slice-045*` | neu | Verification-Artefakt fuer DoD, lokale Tests und Gates. |

## 4. Trigger

`slice-016`, `slice-020`, `slice-031` und `slice-034` liegen in `done/`:
`BeobachtungsAuswahlPort` ist belief-aware, `voi-fake` zeigt den Contract,
realistische lokale Beobachtungsquellen und Git-Source-Strategien existieren
als Umfeld. Kein Slice liegt in `in-progress/` (WIP-Limit 1). Vor Start wird
entschieden, ob der Adapter ein allgemeines lokales Kandidatenformat oder einen
enger benannten Code-Agent-Kandidatenkatalog liefert; CLI-/Example-Binding
bleibt getrennt.

## 5. Closure-Trigger

DoD vollstaendig + Review/Verification abgeschlossen + `make gates` gruen +
Closure-Notiz geschrieben + Slice nach `done/` verschoben.

## 6. Risiken und offene Punkte

- Der Adapter darf nicht selbst die beste Beobachtung waehlen. Ranking bleibt im
  `VoiSelektor`; der Adapter liefert nur explizite Kandidatenwerte.
- Adapter-zu-Adapter-Importe wuerden `ARC-08` verletzen. Gemeinsame Nutzung von
  Build-/Git-Daten braucht ein lokales Eingabeformat oder einen separaten
  Composition-Slice.
- Leere Kandidaten sind fachlich "keine guenstige Beobachtung" und treiben ggf.
  Eskalation; Format-/IO-Fehler duerfen nicht still als legitime Erschoepfung
  maskiert werden.
- Kosten und erwartete Diskriminierung sind heuristische Werte. Der Slice liefert
  keinen global optimalen Policy-Plan und keine produktive LLM-Kalibrierung.
- CLI-Default-Binding, `example/code-agent`-Binding und Live-Quellen bleiben
  Folgeslices, falls sie benoetigt werden.

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss fuellen. -->

## 8. Sub-Area-Modus-Begründung

### Sub-Area: `adapters/outbound/observation-voi-*`

- **Modus:** GF
- **Konventionen-Dichte:** hoch. `ARC-08`, [`ADR-0003`](../../adr/0003-hexslice-architektur.md) und [`ADR-0006`](../../adr/0006-coverage-gate-scope.md) fuehren
  Outbound-Adapter, Build-/Arch-Gate-Einbindung und per-Modul-Coverage; `voi-fake`
  und die realistischen Beobachtungsadapter aus `slice-031` liefern lokale
  Strukturmuster.
- **Phase-Reife:** Phase 3. Port und Fake sind stabil (`slice-016`, `slice-020`);
  ein echter Kandidatenadapter entsteht neu.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Falsche Kandidatenformate koennen
  Erschoepfung, Kosten oder Diskriminierung anders deuten als der Domain-Vertrag.
- **Reconciliation-Aufwand:** Teil dieses Slice: strikte Parser-/Mapping-Tests,
  Build-/Arch-/Coverage-Integration und Verification. Graduation-Trigger:
  `make gates` gruen und Closure ohne offene Port-/Selektor-Drift.

### Sub-Area: `hexagon/application/belief/beobachtungwaehlen`

- **Modus:** Hybrid
- **Konventionen-Dichte:** hoch fuer den bestehenden Port und Use Case
  (`slice-016`, `slice-020`, `ARC-07`), mittel fuer reale Kandidateneingaben,
  weil bisher nur `voi-fake` die Werte liefert.
- **Phase-Reife:** Phase 4 fuer `BeobachtungsAuswahlPort` und
  `BeobachtungWaehlen`; Phase 2-3 fuer reale Kandidatenquellen.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Ein echter Adapter koennte
  Kandidatenfilterung, Erschoepfung oder Auswahlsemantik in den Adapter ziehen.
- **Reconciliation-Aufwand:** klein im Slice: keine Port-Aenderung geplant,
  aber Tests muessen zeigen, dass reale Kandidaten weiterhin nur als
  `VoiKandidat` in den bestehenden Selektorpfad gehen. Falls der Port erweitert
  werden muss, wird ein separater Contract-Slice geplant.

### Sub-Area: `adapters/inbound/cli` / Example-Binding

- **Modus:** GF
- **Konventionen-Dichte:** hoch. `ARC-09` verortet Runtime-Binding im
  Composition-Root; dieser Slice soll keinen CLI-Default und kein
  `example/code-agent`-Binding veraendern.
- **Phase-Reife:** Phase 4. CLI-Composition und Code-Agent-Beobachtungsbinding
  existieren; die neue VoI-Kandidatenquelle bleibt zunaechst isoliert.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Eine nebenbei geaenderte Runtime-
  Verdrahtung koennte echte Kandidaten in hermetische Demos bringen oder
  Eskalationspfade veraendern.
- **Reconciliation-Aufwand:** keiner in diesem Slice; eigener Binding-Slice,
  falls CLI-Flags, Example-Konfiguration oder Default-Adapter geaendert werden.
