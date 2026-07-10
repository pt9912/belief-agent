# Slice slice-049: Code-Agent BeobachtungsAuswahl-Composition

**Status:** open (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung.

**Bezug:** [`LH-FA-OBS-001`](../../../../spec/lastenheft.md#lh-fa-obs-001--heterogene-beobachtungsquellen), [`LH-FA-OBS-004`](../../../../spec/lastenheft.md#lh-fa-obs-004--deduplizierung-korrelierter-beobachtungen), [`LH-FA-OBS-006`](../../../../spec/lastenheft.md#lh-fa-obs-006--zeitstempel-und-quelle-je-beobachtung),
[`LH-FA-VOI-001`](../../../../spec/lastenheft.md#lh-fa-voi-001--information-vor-handlung-bei-unsicherheit), [`LH-FA-VOI-002`](../../../../spec/lastenheft.md#lh-fa-voi-002--diskriminierung-der-zwei-wahrscheinlichsten-hypothesen), [`LH-FA-VOI-003`](../../../../spec/lastenheft.md#lh-fa-voi-003--gewinn-kosten-abwägung), [`LH-FA-VOI-004`](../../../../spec/lastenheft.md#lh-fa-voi-004--lokaleheuristische-voi-bewertung),
[`LH-FA-ESK-001`](../../../../spec/lastenheft.md#lh-fa-esk-001--eskalationsbedingung), [`LH-QA-02`](../../../../spec/lastenheft.md#lh-qa-02--konservatives-standardverhalten-fail-safe), [`LH-QA-03`](../../../../spec/lastenheft.md#lh-qa-03--testbarkeit), [`LH-QA-04`](../../../../spec/lastenheft.md#lh-qa-04--erweiterbarkeit); [`ADR-0001`](../../adr/0001-hexagonal-llm-port.md),
[`ADR-0003`](../../adr/0003-hexslice-architektur.md), [`ADR-0006`](../../adr/0006-coverage-gate-scope.md), `ARC-04`, `ARC-07`, `ARC-08`, `ARC-09`.

**Autor:** Codex. **Datum:** 2026-07-08.

---

## 1. Ziel

`example/code-agent` bindet den echten `BeobachtungsAuswahlPort`-Adapter aus
`slice-045` als bewusste Composition-Route ein und ersetzt die lokale statische
Kandidatenquelle durch einen reproduzierbaren Kandidatenkatalog, ohne
`VoiSelektor`, Eskalationslogik, Gate oder Executor-Grenze zu veraendern.

## 2. Definition of Done

- [ ] `slice-045` liegt in `done/` und liefert ein Adaptermodul
  (`observation-voi-*`), das `BeobachtungsAuswahlPort` ohne Adapter-zu-Adapter-
  Kopplung implementiert.
- [ ] `example/code-agent` nutzt den echten BeobachtungsAuswahl-Adapter ueber
  eine explizite Composition-Konfiguration; die bisherige statische
  `BeobachtungsAuswahlPort`-Implementierung wird entfernt oder nur noch als
  Testfixture verwendet.
- [ ] Der Kandidatenkatalog ist lokal, hermetisch und reproduzierbar:
  Default-Fixture im Runtime-Image, Env-/Make-Override fuer Host-Fixtures,
  dokumentierte Fehlerklassen fuer fehlende, leere, kaputte und semantisch
  ungueltige Kandidaten ([`LH-QA-02`](../../../../spec/lastenheft.md#lh-qa-02--konservatives-standardverhalten-fail-safe), [`LH-QA-03`](../../../../spec/lastenheft.md#lh-qa-03--testbarkeit)).
- [ ] Binding bleibt eine Composition-Verantwortung: `example/code-agent` darf
  den Adapter importieren, aber `hexagon:*` und andere Outbound-Adapter bleiben
  frei von Adapter-/IO-Abhaengigkeiten; `.a-check.yml` erlaubt nur die gezielte
  Example-zu-Adapter-Kante.
- [ ] E2E-/Runtime-Sensoren zeigen, dass der Entscheidungszyklus weiter ueber
  `BeobachtungWaehlen` und `VoiSelektor` laeuft: gueltige Kandidaten koennen zum
  Sammelschritt fuehren, leere legitime Kandidaten fuehren zu "keine guenstige
  Beobachtung", Format-/IO-Fehler werden fail-closed sichtbar.
- [ ] Build-/Arch-/Coverage-/Image-Integration ist vollstaendig:
  `example/code-agent/build.gradle.kts`, `.a-check.yml`, `Dockerfile`,
  `Makefile`, Tests und relevante Docs sind aktualisiert.
- [ ] Nutzer-/Integrationsdoku beschreibt die Code-Agent-Composition, Default-
  Fixture-Pfade, Override-ENV und die Abgrenzung: kein produktives
  `adapters/inbound/cli`-Default-Binding, keine Live-Quellen und keine
  Adapter-zu-Adapter-Kopplung.
- [ ] `make example-code-agent-run`, `make doc-check` und `make gates` sind
  gruen; Closure-Notiz benennt, ob ein separater CLI-Binding-Slice noetig ist.

## 3. Plan (vor Code)

| Datei / Komponente | Aenderungs-Art | Begruendung |
|---|---|---|
| `example/code-agent/src/main/kotlin/**` | update | Statische Kandidatenquelle durch Composition-Binding auf den echten Adapter aus `slice-045` ersetzen. |
| `example/code-agent/fixtures/*voi*.fixture` oder Adapter-konformes Format | neu | Reproduzierbarer lokaler Kandidatenkatalog fuer Runtime-Image und Tests. |
| `example/code-agent/src/test/kotlin/**` | update/neu | E2E-/Negativmatrix fuer gueltige Kandidaten, leere Kandidaten, kaputte Fixture und fail-closed Runtime-Ausgabe. |
| `example/code-agent/build.gradle.kts` | update | Dependency auf den echten BeobachtungsAuswahl-Adapter aufnehmen. |
| `.a-check.yml` | update | Gezielt `example/code-agent -> observation-voi-*` erlauben, ohne `adapters/inbound/cli` umzubinden. |
| `Dockerfile` | update | Kandidaten-Default-Fixture ins Runtime-Image kopieren und ENV/ARG bereitstellen. |
| `Makefile` | update | `CODE_AGENT_VOI_FIXTURE` oder gleichwertigen Override fuer Build-/Run-Pfade setzen. |
| `example/code-agent/README.md` | update | Composition, Fixture-Kontrakt und Fehlerklassen dokumentieren. |
| `docs/user/integration.md` | update | Code-Agent-Binding als Example-Composition dokumentieren, CLI-Default abgrenzen. |
| `docs/reviews/*slice-049*` | neu | Review-Artefakt mit Fokus Composition-Grenze, Arch-Kanten und fail-closed Binding. |
| `docs/verifications/*slice-049*` | neu | Verification-Artefakt fuer DoD, Runtime-Sensoren und Gates. |

## 4. Trigger

`slice-045` liegt in `done/` und liefert den echten, lokal testbaren
`BeobachtungsAuswahlPort`-Adapter. `slice-032`/`slice-033` liegen in `done/` und
stabilisieren `example/code-agent` als direkt startbares Runtime-Image mit
Fixture-Vertrag und Fehlerklassen. Kein Slice liegt in `in-progress/`
(WIP-Limit 1). Vor Start wird der Adaptername und sein lokales Kandidatenformat
aus `slice-045` uebernommen; der Composition-Slice erfindet kein zweites Format.

## 5. Closure-Trigger

DoD vollstaendig + Review/Verification abgeschlossen + `make gates` gruen +
Closure-Notiz geschrieben + Slice nach `done/` verschoben.

## 6. Risiken und offene Punkte

- Die Composition darf keine Adapter-zu-Adapter-Kopplung herstellen. Falls Build-
  oder Git-Daten fuer Kandidaten genutzt werden, muessen sie ueber das lokale
  Kandidatenformat aus `slice-045` laufen.
- Ein kaputter Kandidatenkatalog darf nicht wie legitime Beobachtungs-
  Erschoepfung aussehen; Fehler muessen sichtbar fail-closed bleiben.
- Der Slice darf die Auswahlregel nicht in `example/code-agent` duplizieren.
  Ranking bleibt im `VoiSelektor`; Composition bindet nur Port und Daten.
- Produktives `adapters/inbound/cli`-Default-Binding bleibt separater Slice.
  Dieser Slice betrifft `example/code-agent` als Example-/Runtime-Komposition.
- Wenn `slice-045` den `BeobachtungsAuswahlPort`-Contract aendert, wird vor Code
  geprueft, ob ein Contract-Reconciliation-Slice dazwischen noetig ist.

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss fuellen. -->

## 8. Sub-Area-Modus-Begründung

### Sub-Area: `example/code-agent` Composition

- **Modus:** Hybrid
- **Konventionen-Dichte:** mittel bis hoch. `slice-032`/`slice-033` etablieren
  Runtime-Image, Fixture-Defaults und Fehlerklassen; der neue
  BeobachtungsAuswahl-Adapter aus `slice-045` bringt ein weiteres lokales
  Eingabeformat in dieselbe Example-Composition.
- **Phase-Reife:** Phase 4 fuer Code-Agent-Runtime und Build-/Repo-Fixtures,
  Phase 2-3 fuer die neue VoI-Kandidaten-Composition.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Falsches Binding kann
  Beobachtungserschoepfung, Sammelschritte oder Eskalation anders ausloesen als
  der bestehende `VoiSelektor`-Pfad.
- **Reconciliation-Aufwand:** Teil dieses Slice: Runtime-Binding, Fixture-
  Vertrag, Negativmatrix, Docker-/Make-/Doku-Update und Gate-Lauf.

### Sub-Area: `adapters/outbound/observation-voi-*`

- **Modus:** GF
- **Konventionen-Dichte:** hoch nach Abschluss von `slice-045`. Der Adapter
  fuehrt den Port-Vertrag und sein lokales Format; dieser Slice konsumiert ihn
  nur in der Composition.
- **Phase-Reife:** Phase 3 nach erstem echten Adapter.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Composition darf keine zusaetzliche
  Auswahlsemantik oder Adapterkopplung in den Adapter zurueckdruecken.
- **Reconciliation-Aufwand:** klein: nur Adapter-API und Dokumentation aus
  `slice-045` verwenden; Contract-Aenderungen werden separat geplant.

### Sub-Area: `adapters/inbound/cli`

- **Modus:** GF
- **Konventionen-Dichte:** hoch. `ARC-09` erlaubt Composition-Binding im CLI-Root,
  aber dieser Slice soll den produktiven CLI-Default nicht veraendern.
- **Phase-Reife:** Phase 4. CLI-Runtime bleibt stabil und nutzt weiter ihre
  explizite Konfiguration.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Eine nebenbei geaenderte CLI-
  Verdrahtung koennte hermetische Szenarien oder Demo-Ausgaben veraendern.
- **Reconciliation-Aufwand:** keiner in diesem Slice; eigener CLI-Binding-Slice,
  falls Flags, Defaults oder Runtime-Zeitpunkt fuer den echten Adapter geplant
  werden.
