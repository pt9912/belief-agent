# Slice slice-050: CLI-BeobachtungsAuswahl-Default-Binding

**Status:** open (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung.

**Bezug:** [`LH-FA-OBS-001`](../../../../spec/lastenheft.md#lh-fa-obs-001--heterogene-beobachtungsquellen), [`LH-FA-OBS-004`](../../../../spec/lastenheft.md#lh-fa-obs-004--deduplizierung-korrelierter-beobachtungen), [`LH-FA-OBS-006`](../../../../spec/lastenheft.md#lh-fa-obs-006--zeitstempel-und-quelle-je-beobachtung),
[`LH-FA-VOI-001`](../../../../spec/lastenheft.md#lh-fa-voi-001--information-vor-handlung-bei-unsicherheit), [`LH-FA-VOI-002`](../../../../spec/lastenheft.md#lh-fa-voi-002--diskriminierung-der-zwei-wahrscheinlichsten-hypothesen), [`LH-FA-VOI-003`](../../../../spec/lastenheft.md#lh-fa-voi-003--gewinn-kosten-abwägung), [`LH-FA-VOI-004`](../../../../spec/lastenheft.md#lh-fa-voi-004--lokaleheuristische-voi-bewertung),
[`LH-FA-ESK-001`](../../../../spec/lastenheft.md#lh-fa-esk-001--eskalationsbedingung), [`LH-QA-02`](../../../../spec/lastenheft.md#lh-qa-02--konservatives-standardverhalten-fail-safe), [`LH-QA-03`](../../../../spec/lastenheft.md#lh-qa-03--testbarkeit), [`LH-QA-04`](../../../../spec/lastenheft.md#lh-qa-04--erweiterbarkeit); [`ADR-0001`](../../adr/0001-hexagonal-llm-port.md),
[`ADR-0003`](../../adr/0003-hexslice-architektur.md), [`ADR-0006`](../../adr/0006-coverage-gate-scope.md), `ARC-04`, `ARC-07`, `ARC-08`, `ARC-09`.

**Autor:** Codex. **Datum:** 2026-07-08.

---

## 1. Ziel

Der produktiv gedachte `adapters/inbound/cli`-Composition-Root bindet den echten
`BeobachtungsAuswahlPort`-Adapter aus `slice-045` als bewusst konfigurierbaren
Default-Pfad ein, ohne `VoiSelektor`, Eskalationslogik, Gate oder Executor-Grenze
zu veraendern.

## 2. Definition of Done

- [ ] `slice-045` liegt in `done/` und liefert den echten
  `BeobachtungsAuswahlPort`-Adapter; `slice-049` liegt in `done/` oder der
  CLI-Slice uebernimmt dessen offene Composition-/Fixture-Erkenntnisse
  dokumentiert.
- [ ] `adapters/inbound/cli` bietet eine explizite Konfiguration fuer den
  echten BeobachtungsAuswahl-Adapter: Default-Fixture/-Pfad, Override per CLI-
  Argument oder ENV und eine bewusste Option fuer den bisherigen deterministischen
  Fake-/Szenario-Modus.
- [ ] Der CLI-Default ist fail-closed: fehlende, leere, kaputte oder semantisch
  ungueltige Kandidatenquelle erzeugt eine sichtbare Diagnose und keinen stillen
  Fallback auf `FakeKandidatenquelle` oder "keine guenstige Beobachtung"
  ([`LH-QA-02`](../../../../spec/lastenheft.md#lh-qa-02--konservatives-standardverhalten-fail-safe), [`LH-QA-03`](../../../../spec/lastenheft.md#lh-qa-03--testbarkeit)).
- [ ] E2E-/CLI-Sensoren zeigen drei Pfade: gueltiger echter Kandidatenkatalog
  kann einen Sammelschritt ausloesen, legitime leere Kandidaten fuehren zur
  bestehenden Erschoepfungs-/Eskalationslogik, Format-/IO-Fehler bleiben
  fail-closed sichtbar.
- [ ] Architekturgrenze bleibt sauber: `adapters/inbound/cli` darf den
  Outbound-Adapter als Composition-Root binden; `hexagon:*` importiert keinen
  Adapter und Outbound-Adapter importieren einander nicht. `.a-check.yml` erlaubt
  nur die noetige `inbound_cli -> observation-voi-*`-Kante.
- [ ] Build-/Arch-/Coverage-/Doku-Integration ist vollstaendig:
  `adapters/inbound/cli/build.gradle.kts`, `.a-check.yml`, `Dockerfile`/Gates,
  CLI-Tests und relevante User-Doku sind aktualisiert.
- [ ] Nutzer-/CLI-Doku beschreibt Default, Override, Fake-/Szenario-Modus,
  Fehlerklassen und die Abgrenzung zu Live-Quellen, Adapter-zu-Adapter-Kopplung
  und Gate-/Executor-Policy.
- [ ] `make cli-demo`, ein enger CLI-Sensor fuer den echten Kandidatenpfad,
  `make doc-check` und `make gates` sind gruen; Closure-Notiz benennt
  verbleibende Runtime-/Operations-Folgeslices.

## 3. Plan (vor Code)

| Datei / Komponente | Aenderungs-Art | Begruendung |
|---|---|---|
| `adapters/inbound/cli/src/main/kotlin/**` | update | CLI-Konfiguration und Koin-Binding fuer den echten `BeobachtungsAuswahlPort`-Adapter. |
| `adapters/inbound/cli/src/test/kotlin/**` | update/neu | E2E-/Negativmatrix fuer echten Kandidatenpfad, Fake-Modus, leere Kandidaten und Fehlerdiagnosen. |
| `adapters/inbound/cli/build.gradle.kts` | update | Dependency auf den echten BeobachtungsAuswahl-Adapter aufnehmen. |
| `.a-check.yml` | update | `inbound_cli -> observation-voi-*` erlauben, sonstige Adapterkopplung weiter verbieten. |
| `Dockerfile` | update | Falls CLI-Runtime-Fixtures im Image benoetigt werden, reproduzierbar bereitstellen. |
| `Makefile` | update | Engen CLI-Sensor fuer den echten Kandidatenpfad ergaenzen, falls noch kein passendes Target existiert. |
| `docs/user/cli-entscheidungsnachweis.md` | update | CLI-Entscheidungsspur, VoI-Default und Fehlerklassen dokumentieren. |
| `docs/user/integration.md` | update | Produktives CLI-Binding des BeobachtungsAuswahl-Adapters dokumentieren. |
| `docs/reviews/*slice-050*` | neu | Review-Artefakt mit Fokus Composition-Grenze, fail-closed Default und keine Policy-Verschiebung. |
| `docs/verifications/*slice-050*` | neu | Verification-Artefakt fuer CLI-Sensoren, DoD und Gates. |

## 4. Trigger

`slice-045` liegt in `done/` und liefert den echten Adapter. `slice-049` sollte
in `done/` liegen, wenn dessen Code-Agent-Composition den Fixture-/Fehlervertrag
zuerst praktisch validiert; falls `slice-049` bewusst uebersprungen wird, muss
dieser Slice dessen offene Risiken vor Code explizit in Review/Verification
uebernehmen. Kein Slice liegt in `in-progress/` (WIP-Limit 1). Vor Start werden
Adaptername, lokales Kandidatenformat und CLI-Default-/Override-Policy aus den
vorherigen Slices uebernommen.

## 5. Closure-Trigger

DoD vollstaendig + Review/Verification abgeschlossen + `make gates` gruen +
Closure-Notiz geschrieben + Slice nach `done/` verschoben.

## 6. Risiken und offene Punkte

- Ein stiller Fallback vom echten Adapter auf `FakeKandidatenquelle` waere
  irrefuehrend. Fake-/Szenario-Modus muss explizit gewaehlt und in Ausgaben oder
  Tests erkennbar sein.
- Format-/IO-Fehler duerfen nicht wie legitime Beobachtungs-Erschoepfung wirken.
  Der CLI-Pfad braucht klare Diagnose- und Exit-/Terminal-Semantik.
- Der Slice darf keine Auswahlregel duplizieren. Ranking bleibt im
  `VoiSelektor`; CLI bindet nur Port, Konfiguration und Daten.
- Adapter-zu-Adapter-Kopplung bleibt verboten. Wenn Build-/Git-Daten in
  Kandidaten einfliessen sollen, laufen sie ueber das lokale Kandidatenformat
  oder einen spaeteren Aggregations-/Composition-Slice.
- Gate-, Approval- und Executor-Policy bleiben unveraendert. Eine Veraenderung
  der Handlungsfreigabe waere ein separater Safety-Slice.

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss fuellen. -->

## 8. Sub-Area-Modus-Begründung

### Sub-Area: `adapters/inbound/cli`

- **Modus:** Hybrid
- **Konventionen-Dichte:** hoch. `slice-024` etabliert den CLI-Composition-Root
  nach `ARC-09`; `slice-030` stabilisiert Demo-Szenarien. Der neue Default-Pfad
  bringt echte Kandidatenquelle und Fehlerdiagnostik in denselben Runtime-Root.
- **Phase-Reife:** Phase 4 fuer CLI-Composition, Phase 2-3 fuer produktives
  Binding des echten BeobachtungsAuswahl-Adapters.
- **Evidenz-/Diskrepanz-Risiko:** mittel bis hoch. Ein falscher Default oder
  stiller Fallback kann Sammeln/Eskalieren anders ausloesen als die
  deterministischen Szenarien.
- **Reconciliation-Aufwand:** Teil dieses Slice: CLI-Konfiguration, E2E-
  Negativmatrix, Doku, Arch-Kante und Gates.

### Sub-Area: `adapters/outbound/observation-voi-*`

- **Modus:** GF
- **Konventionen-Dichte:** hoch nach Abschluss von `slice-045`; der Adapter
  fuehrt seinen Port- und Formatvertrag.
- **Phase-Reife:** Phase 3 nach erstem echten Adapter und ggf. Code-Agent-
  Composition aus `slice-049`.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Der CLI-Slice darf keine zusaetzliche
  fachliche Auswahl- oder Reparaturlogik in den Adapter druecken.
- **Reconciliation-Aufwand:** klein: Adapter-API und Fehlerklassen konsumieren;
  Contract-Aenderungen separat planen.

### Sub-Area: `hexagon/application/belief/beobachtungwaehlen`

- **Modus:** GF
- **Konventionen-Dichte:** hoch. `BeobachtungWaehlen` und `VoiSelektor` sind
  stabile Application-/Domain-Regeln; dieser Slice soll sie nur ueber den Port
  verwenden.
- **Phase-Reife:** Phase 4.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Jede Port- oder Selektor-Aenderung
  koennte die Sicherheits-/Eskalationssemantik verschieben; dieser Slice plant
  keine Core-Aenderung.
- **Reconciliation-Aufwand:** keiner im Core. Wenn der Port fuer Runtime-
  Diagnosen erweitert werden muss, wird ein separater Contract-Slice geplant.
