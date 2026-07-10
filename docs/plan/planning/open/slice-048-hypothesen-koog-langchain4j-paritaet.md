# Slice slice-048: Hypothesen Koog/LangChain4j-Paritaet

**Status:** open (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung.

**Bezug:** [`LH-FA-LLM-001`](../../../../spec/lastenheft.md#lh-fa-llm-001--sprachmodell-als-austauschbares-modul), [`LH-FA-LLM-002`](../../../../spec/lastenheft.md#lh-fa-llm-002--abgegrenzte-modell-aufgaben), [`LH-FA-LLM-003`](../../../../spec/lastenheft.md#lh-fa-llm-003--externalisierung-der-modell-konfidenz),
[`LH-FA-LLM-004`](../../../../spec/lastenheft.md#lh-fa-llm-004--anbieter-austauschbarkeit), [`LH-FA-BEL-005`](../../../../spec/lastenheft.md#lh-fa-bel-005--re-hypothesenbildung-bei-hoher-resthypothese), [`LH-FA-BEL-006`](../../../../spec/lastenheft.md#lh-fa-bel-006--dynamischer-hypothesenraum), [`LH-FA-BEL-007`](../../../../spec/lastenheft.md#lh-fa-bel-007--rückverfolgbarkeit-hypothese--evidenz),
[`LH-QA-02`](../../../../spec/lastenheft.md#lh-qa-02--konservatives-standardverhalten-fail-safe), [`LH-QA-03`](../../../../spec/lastenheft.md#lh-qa-03--testbarkeit), [`LH-QA-04`](../../../../spec/lastenheft.md#lh-qa-04--erweiterbarkeit); [`ADR-0001`](../../adr/0001-hexagonal-llm-port.md), [`ADR-0003`](../../adr/0003-hexslice-architektur.md), [`ADR-0006`](../../adr/0006-coverage-gate-scope.md),
`ARC-02`, `ARC-07`, `ARC-08`, `ARC-09`.

**Autor:** Codex. **Datum:** 2026-07-08.

---

## 1. Ziel

Nach dem ersten echten `HypothesenPort`-Provider-Adapter aus `slice-044` wird
der zweite Framework-Pfad ergaenzt, sodass Koog und LangChain4j fuer
Hypothesen-Erzeugung/-Verfeinerung dieselbe strukturierte Modellaufgabe,
dasselbe fail-closed Schema und dieselbe lokale Contract-Matrix tragen.

## 2. Definition of Done

- [ ] Der nach `slice-044` fehlende Framework-Adapter
  (`llm-hypothesen-koog` oder `llm-hypothesen-langchain4j`) implementiert
  `HypothesenPort` hinter `ARC-08`; beide Framework-Pfade bleiben austauschbar
  und `hexagon:*` bleibt frei von Provider-/Framework-Imports ([`LH-FA-LLM-001`](../../../../spec/lastenheft.md#lh-fa-llm-001--sprachmodell-als-austauschbares-modul),
  [`LH-FA-LLM-004`](../../../../spec/lastenheft.md#lh-fa-llm-004--anbieter-austauschbarkeit)).
- [ ] Beide Adapter nutzen denselben fachlichen Response-Contract fuer
  Hypothesen-Kandidaten: Kandidaten-ID, `score` und `stuetzendeEvidenz`.
  Unterschiede in Framework-Runnern duerfen nicht zu unterschiedlichen
  fachlichen Akzeptanz- oder Fehlerregeln fuehren.
- [ ] Gemeinsame oder bewusst duplizierte Contract-Tests belegen Paritaet fuer
  gueltige Antwort, leere Antwort, kaputtes JSON, doppelte Felder, unbekannte
  Felder, leere IDs, nicht-endliche Scores, Scores ausserhalb `(0,1]`, fehlende
  Evidenz und Trennung zu `LlmPort`/`AktionsVorschlagsPort` ([`LH-QA-02`](../../../../spec/lastenheft.md#lh-qa-02--konservatives-standardverhalten-fail-safe),
  [`LH-QA-03`](../../../../spec/lastenheft.md#lh-qa-03--testbarkeit)).
- [ ] Prompt- und Parser-Paritaet ist dokumentiert: beide Framework-Pfade
  beschreiben dieselbe abgegrenzte Modellaufgabe aus [`LH-FA-LLM-002`](../../../../spec/lastenheft.md#lh-fa-llm-002--abgegrenzte-modell-aufgaben) und liefern
  nur `HypothesenKandidat`en, keine Likelihoods, Aktionsvorschlaege, Gate-
  Entscheidungen, Belief-Mutationen oder Konfidenz-Gate-Werte.
- [ ] Build-/Arch-/Coverage-Integration ist fuer beide Adapter vollstaendig:
  `settings.gradle.kts`, `.a-check.yml`, `Dockerfile`, Modul-`build.gradle.kts`
  und Kover-Gates enthalten den neuen Paritaetspfad ([`ADR-0003`](../../adr/0003-hexslice-architektur.md), [`ADR-0006`](../../adr/0006-coverage-gate-scope.md)).
- [ ] Integrationsdoku beschreibt beide Hypothesen-Providerpfade symmetrisch und
  nennt klar: keine CLI-Default-Umbindung, keine Live-Provider-Tests, keine
  Secrets in Doku/Tests und keine produktive Provider-Konfiguration.
- [ ] `make doc-check` und `make gates` sind gruen; Closure-Notiz benennt den
  Paritaets-Contract und verbleibende provider-spezifische Unterschiede.

## 3. Plan (vor Code)

| Datei / Komponente | Aenderungs-Art | Begruendung |
|---|---|---|
| `adapters/outbound/llm-hypothesen-koog` oder `adapters/outbound/llm-hypothesen-langchain4j` | neu | Fehlenden Framework-Pfad hinter `HypothesenPort` ergaenzen. |
| bestehender `llm-hypothesen-*` Adapter aus `slice-044` | update | Paritaets-Contract falls noetig gemeinsam nutzbar machen, ohne Framework-Abhaengigkeiten in den Core zu ziehen. |
| `.../src/test/kotlin/**` beider Adapter | update/neu | Gemeinsame Contract-Matrix fuer Parser, Prompt und fail-closed Mapping. |
| `settings.gradle.kts` | update | Neues Adaptermodul registrieren. |
| `.a-check.yml` | update | Adapterrolle und erlaubte Kanten aufnehmen; Core bleibt providerfrei. |
| `Dockerfile` | update | Dependency-, Test- und Coverage-Gate-Stages um den zweiten Adapter ergaenzen. |
| `docs/user/integration.md` | update | Koog/LangChain4j-Paritaet fuer Hypothesen-Provider dokumentieren. |
| `docs/reviews/*slice-048*` | neu | Review-Artefakt mit Fokus Paritaet, Parser-Drift und Port-Trennung. |
| `docs/verifications/*slice-048*` | neu | Verification-Artefakt fuer Contract-Matrix und Gates. |

## 4. Trigger

`slice-044` liegt in `done/` und hat genau einen echten
Hypothesen-Providerpfad geliefert. Kein Slice liegt in `in-progress/`
(WIP-Limit 1). Vor Start wird festgestellt, welcher Framework-Pfad nach
`slice-044` fehlt; dieser Slice implementiert nur diesen fehlenden Pfad plus
Paritaetsverifikation.

## 5. Closure-Trigger

DoD vollstaendig + Review/Verification abgeschlossen + `make gates` gruen +
Closure-Notiz geschrieben + Slice nach `done/` verschoben.

## 6. Risiken und offene Punkte

- Paritaet darf nicht durch Kopieren unscharfer Parser entstehen. Beide Pfade
  muessen dieselben fachlichen Fehlerregeln sichtbar machen und fail-closed
  bleiben.
- Gemeinsamer Parser-Code darf keine Framework-Abhaengigkeit in `hexagon:*` oder
  einen fremden Adapter ziehen; falls gemeinsamer Code noetig ist, muss er in
  einem klaren Adapter-internen Modul oder durch bewusst duplizierte Tests
  begruendet werden.
- `score` bleibt Kandidaten-/Resthypothesen-Semantik und ist keine
  gate-faehige Erfolgswahrscheinlichkeit. Konfidenz-Gate, Aktionsvorschlaege,
  Approval und Ausfuehrung bleiben ausserhalb dieses Slice.
- Provider-/Modellwahl, API-Keys, Live-Netztests, CLI-Default-Binding und
  produktive Prompt-Kalibrierung bleiben ausserhalb dieses Slice.
- Wenn `slice-044` den Port-Vertrag aendern musste, wird vor Implementation
  dieses Slice geprueft, ob ein Contract-Reconciliation-Slice dazwischen noetig
  ist.

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss fuellen. -->

## 8. Sub-Area-Modus-Begründung

### Sub-Area: `adapters/outbound/llm-hypothesen-*`

- **Modus:** Hybrid
- **Konventionen-Dichte:** hoch fuer Outbound-Adapter nach `ARC-08` und
  Build-/Coverage-Einbindung nach [`ADR-0003`](../../adr/0003-hexslice-architektur.md)/[`ADR-0006`](../../adr/0006-coverage-gate-scope.md); mittel fuer
  Paritaetsregeln zwischen zwei echten Framework-Pfaden, weil `slice-044`
  zunaechst nur einen Pfad liefert.
- **Phase-Reife:** Phase 3 fuer den ersten Provider-Adapter aus `slice-044`,
  Phase 2-3 fuer den zweiten Pfad und die Paritaetsmatrix.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Koog und LangChain4j haben
  unterschiedliche Runner-/Prompt-Abstraktionen; ohne Contract-Matrix koennen
  Parser, Prompt und Fehlerverhalten auseinanderlaufen.
- **Reconciliation-Aufwand:** Teil dieses Slice: zweiter Adapter, gemeinsame
  Contract-Matrix, Build-/Arch-/Coverage-Integration. Graduation-Trigger:
  beide Framework-Pfade sind in `make gates` enthalten und Verification zeigt
  gleiche fachliche Akzeptanz-/Ablehnungsregeln.

### Sub-Area: `hexagon/application/belief/aktualisieren`

- **Modus:** GF
- **Konventionen-Dichte:** hoch fuer lokale Ports und Re-Hypothesen-Flow
  (`slice-025`, `ARC-07`). Dieser Slice soll den bestehenden `HypothesenPort`
  nur implementieren, nicht erweitern.
- **Phase-Reife:** Phase 4 fuer `HypothesenPort` und `BeliefAktualisieren`.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Framework-Paritaet darf den Port nicht
  in einen generischen LLM-Port zurueckdrehen oder Re-Hypothesen mit
  Likelihood-/Aktionslogik vermischen.
- **Reconciliation-Aufwand:** keiner im Core. Falls der Port erweitert werden
  muss, wird ein separater Contract-Slice geplant.

### Sub-Area: `docs/user/integration.md`

- **Modus:** GF
- **Konventionen-Dichte:** mittel bis hoch. Nutzer-Doku beschreibt bereits
  LLM-Framework-Adapter und den CLI-Composition-Root; dieser Slice ergaenzt
  symmetrische Hypothesen-Providerpfade.
- **Phase-Reife:** Phase 3. Die Doku folgt dem neuen Adapterpaar und soll keine
  Live-Secrets oder Default-Umbindung versprechen.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Doku koennte Paritaet als produktive
  Provider-Konfiguration missverstehen; der Slice muss klar zwischen Adapter-
  Existenz und Runtime-Binding trennen.
- **Reconciliation-Aufwand:** klein im Slice: Integrationsabschnitt und
  Verification-Link aktualisieren. Folge-Slice nur, wenn CLI-/Runtime-Binding
  tatsaechlich geplant wird.
