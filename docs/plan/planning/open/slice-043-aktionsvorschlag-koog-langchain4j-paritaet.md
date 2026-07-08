# Slice slice-043: Aktionsvorschlag Koog/LangChain4j-Paritaet

**Status:** open (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung.

**Bezug:** `LH-FA-LLM-001`, `LH-FA-LLM-002`, `LH-FA-LLM-003`,
`LH-FA-LLM-004`, `LH-FA-ACT-001`, `LH-FA-ACT-002`, `LH-FA-ACT-003`,
`LH-FA-ACT-004`, `LH-FA-POL-006`, `LH-QA-02`, `LH-QA-03`, `LH-QA-04`;
`ADR-0001`, `ADR-0003`, `ADR-0006`, `ARC-07`, `ARC-08`, `ARC-09`.

**Autor:** Codex. **Datum:** 2026-07-08.

---

## 1. Ziel

Nach dem ersten echten `AktionsVorschlagsPort`-Provider-Adapter aus
`slice-042` wird der zweite Framework-Pfad ergaenzt, sodass Koog und LangChain4j
fuer Aktionsvorschlaege dieselbe strukturierte Modellaufgabe, dasselbe
fail-closed Schema und dieselben lokalen Contract-Tests tragen.

## 2. Definition of Done

- [ ] Der nach `slice-042` fehlende Framework-Adapter (`llm-action-koog` oder
  `llm-action-langchain4j`) implementiert `AktionsVorschlagsPort` hinter
  `ARC-08`; beide Framework-Pfade bleiben austauschbar und core-frei von
  Provider-/Framework-Imports (`LH-FA-LLM-001`, `LH-FA-LLM-004`).
- [ ] Beide Adapter nutzen denselben fachlichen Response-Contract fuer
  Aktionsvorschlaege: `beschreibung`, `hypotheseId`, `wirkungsklasse`,
  `pSuccess`, `konfidenzReferenz`, `stuetzendeEvidenz`. Unterschiede in
  Framework-Runnern duerfen nicht zu unterschiedlichen fachlichen Akzeptanz-
  oder Fehlerregeln fuehren.
- [ ] Gemeinsame oder duplizierte Contract-Tests belegen Paritaet fuer
  gueltige Antwort, leere Antwort, kaputtes JSON, doppelte Felder, unbekannte
  Felder, unbekannte Hypothese, ungueltige Wirkungsklasse, fehlende Evidenz,
  ungueltiges `pSuccess` und fehlende Konfidenzreferenz (`LH-QA-02`,
  `LH-QA-03`).
- [ ] Build-/Arch-/Coverage-Integration ist fuer beide Adapter vollstaendig:
  `settings.gradle.kts`, `.a-check.yml`, `Dockerfile`, Modul-`build.gradle.kts`
  und Kover-Gates enthalten den neuen Paritaetspfad (`ADR-0003`, `ADR-0006`).
- [ ] Integrationsdoku beschreibt beide Aktionsvorschlags-Providerpfade
  symmetrisch und nennt klar: keine CLI-Default-Umbindung, keine Live-Provider-
  Tests, keine Secrets in Doku/Tests, keine Gate-/Approval-/Executor-Aenderung.
- [ ] `make doc-check` und `make gates` sind gruen; Closure-Notiz benennt den
  Paritaets-Contract und verbleibende Provider-spezifische Unterschiede.

## 3. Plan (vor Code)

| Datei / Komponente | Aenderungs-Art | Begruendung |
|---|---|---|
| `adapters/outbound/llm-action-koog` oder `adapters/outbound/llm-action-langchain4j` | neu | Fehlenden Framework-Pfad hinter `AktionsVorschlagsPort` ergaenzen. |
| bestehender `llm-action-*` Adapter aus `slice-042` | update | Paritaets-Contract falls noetig gemeinsam nutzbar machen, ohne Framework-Abhaengigkeiten in den Core zu ziehen. |
| `.../src/test/kotlin/**` beider Adapter | update/neu | Gemeinsame Contract-Matrix fuer Parser, Prompt und fail-closed Mapping. |
| `settings.gradle.kts` | update | Neues Adaptermodul registrieren. |
| `.a-check.yml` | update | Adapterrolle und erlaubte Kanten aufnehmen. |
| `Dockerfile` | update | Dependency-, Test- und Coverage-Gate-Stages um den zweiten Adapter ergaenzen. |
| `docs/user/integration.md` | update | Koog/LangChain4j-Paritaet fuer Aktionsvorschlaege dokumentieren. |
| `docs/reviews/*slice-043*` | neu | Review-Artefakt mit Fokus Paritaet und Framework-Drift. |
| `docs/verifications/*slice-043*` | neu | Verification-Artefakt fuer Contract-Matrix und Gates. |

## 4. Trigger

`slice-042` liegt in `done/` und hat genau einen echten
Aktionsvorschlags-Providerpfad geliefert. Kein Slice liegt in `in-progress/`
(WIP-Limit 1). Vor Start wird festgestellt, welcher Framework-Pfad nach
`slice-042` fehlt; dieser Slice implementiert nur diesen fehlenden Pfad plus
Paritaetsverifikation.

## 5. Closure-Trigger

DoD vollstaendig + Review/Verification abgeschlossen + `make gates` gruen +
Closure-Notiz geschrieben + Slice nach `done/` verschoben.

## 6. Risiken und offene Punkte

- Paritaet darf nicht durch Kopieren unscharfer Parser entstehen. Beide Pfade
  muessen dieselben fachlichen Fehlerregeln sichtbar machen und fail-closed
  bleiben.
- Gemeinsamer Parser-Code darf keine Framework-Abhaengigkeit in `hexagon:*`
  oder einen fremden Adapter ziehen; falls gemeinsamer Code noetig ist, muss er
  in einem klaren Adapter-internen Modul oder durch bewusst duplizierte Tests
  begruendet werden.
- Provider-/Modellwahl, API-Keys, Live-Netztests, CLI-Default-Binding,
  Approval und Aktionsausfuehrung bleiben ausserhalb dieses Slice.
- Wenn `slice-042` den Port-Vertrag aendern musste, wird vor Implementation
  dieses Slice geprueft, ob ein Contract-Reconciliation-Slice dazwischen
  noetig ist.

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss fuellen. -->

## 8. Sub-Area-Modus-Begründung

### Sub-Area: `adapters/outbound/llm-action-*`

- **Modus:** Hybrid
- **Konventionen-Dichte:** hoch fuer Outbound-Adapter nach `ARC-08` und
  Build-/Coverage-Einbindung nach `ADR-0003`/`ADR-0006`; mittel fuer
  Paritaetsregeln zwischen zwei echten Framework-Pfaden, weil `slice-042`
  zunaechst nur einen Pfad liefert.
- **Phase-Reife:** Phase 3 fuer den ersten Provider-Adapter aus `slice-042`,
  Phase 2-3 fuer den zweiten Pfad und die Paritaetsmatrix.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Koog und LangChain4j haben
  unterschiedliche Runner-/Prompt-Abstraktionen; ohne Contract-Matrix koennen
  Parser und Fehlerverhalten auseinanderlaufen.
- **Reconciliation-Aufwand:** Teil dieses Slice: zweiter Adapter, gemeinsame
  Contract-Matrix, Build-/Arch-/Coverage-Integration. Graduation-Trigger:
  beide Framework-Pfade sind in `make gates` enthalten und Verification zeigt
  gleiche fachliche Akzeptanz-/Ablehnungsregeln.

### Sub-Area: `docs/user/integration.md`

- **Modus:** GF
- **Konventionen-Dichte:** mittel bis hoch. Nutzer-Doku beschreibt bereits
  LLM-Framework-Adapter und den CLI-Composition-Root; dieser Slice ergaenzt
  symmetrische Aktionsvorschlags-Providerpfade.
- **Phase-Reife:** Phase 3. Die Doku folgt dem neuen Adapterpaar und soll keine
  Live-Secrets oder Default-Umbindung versprechen.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Doku koennte Paritaet als produktive
  Provider-Konfiguration missverstehen; der Slice muss klar zwischen Adapter-
  Existenz und Runtime-Binding trennen.
- **Reconciliation-Aufwand:** klein im Slice: Integrationsabschnitt und
  Verification-Link aktualisieren. Folge-Slice nur, wenn CLI-/Runtime-Binding
  tatsaechlich geplant wird.
