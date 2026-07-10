# Slice slice-044: LLM-Hypothesen-Provider-Adapter

**Status:** open (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung.

**Bezug:** [`LH-FA-LLM-001`](../../../../spec/lastenheft.md#lh-fa-llm-001--sprachmodell-als-austauschbares-modul), [`LH-FA-LLM-002`](../../../../spec/lastenheft.md#lh-fa-llm-002--abgegrenzte-modell-aufgaben), [`LH-FA-LLM-003`](../../../../spec/lastenheft.md#lh-fa-llm-003--externalisierung-der-modell-konfidenz),
[`LH-FA-LLM-004`](../../../../spec/lastenheft.md#lh-fa-llm-004--anbieter-austauschbarkeit), [`LH-FA-BEL-005`](../../../../spec/lastenheft.md#lh-fa-bel-005--re-hypothesenbildung-bei-hoher-resthypothese), [`LH-FA-BEL-006`](../../../../spec/lastenheft.md#lh-fa-bel-006--dynamischer-hypothesenraum), [`LH-FA-BEL-007`](../../../../spec/lastenheft.md#lh-fa-bel-007--rückverfolgbarkeit-hypothese--evidenz),
[`LH-QA-02`](../../../../spec/lastenheft.md#lh-qa-02--konservatives-standardverhalten-fail-safe), [`LH-QA-03`](../../../../spec/lastenheft.md#lh-qa-03--testbarkeit), [`LH-QA-04`](../../../../spec/lastenheft.md#lh-qa-04--erweiterbarkeit); [`ADR-0001`](../../adr/0001-hexagonal-llm-port.md), [`ADR-0003`](../../adr/0003-hexslice-architektur.md), [`ADR-0006`](../../adr/0006-coverage-gate-scope.md),
`ARC-02`, `ARC-07`, `ARC-08`, `ARC-09`.

**Autor:** Codex. **Datum:** 2026-07-08.

---

## 1. Ziel

Ein echter, lokal testbarer LLM-/Provider-Adapter implementiert den
application-lokalen `HypothesenPort` fuer die abgegrenzte Modellaufgabe
"Hypothesen erzeugen/verfeinern" und liefert ausschliesslich strukturierte
`HypothesenKandidat`en mit explizitem Score und Evidenzreferenzen.

## 2. Definition of Done

- [ ] Neues Outbound-Adaptermodul (z. B. `llm-hypothesen-langchain4j` oder nach
  Design-Review gleichwertig) implementiert `HypothesenPort` hinter `ARC-08`;
  `hexagon:*` importiert keine Provider-/Framework-Pakete ([`LH-FA-LLM-001`](../../../../spec/lastenheft.md#lh-fa-llm-001--sprachmodell-als-austauschbares-modul),
  [`LH-FA-LLM-004`](../../../../spec/lastenheft.md#lh-fa-llm-004--anbieter-austauschbarkeit)).
- [ ] Prompt, Response-DTO und Parser sind strikt schema-gebunden: genau die
  erlaubten Felder fuer Kandidaten-ID, `score` und `stuetzendeEvidenz`; kaputtes
  JSON, doppelte Felder, unbekannte Felder, leere IDs, nicht-endliche Scores,
  Scores ausserhalb `(0,1]` und fehlende Evidenz werden fail-closed verworfen
  oder als Adapterfehler sichtbar.
- [ ] Der Adapter bleibt auf [`LH-FA-LLM-002`](../../../../spec/lastenheft.md#lh-fa-llm-002--abgegrenzte-modell-aufgaben) beschraenkt: keine Likelihoods,
  keine Aktionsvorschlaege, kein Gate, kein Approval, keine Ausfuehrung und
  keine Mutation des `BeliefState`; die Uebernahme bleibt im bestehenden
  `BeliefAktualisieren`-/Domain-Pfad.
- [ ] Lokale Tests ohne Provider/API-Key decken erfolgreiche Normalisierung,
  leere Antwort, kaputtes JSON, Schema-Abweichungen, Prompt-Inhalt, ungueltige
  Scores, fehlende Evidenz und Trennung zu `LlmPort`/`AktionsVorschlagsPort` ab
  ([`LH-QA-02`](../../../../spec/lastenheft.md#lh-qa-02--konservatives-standardverhalten-fail-safe), [`LH-QA-03`](../../../../spec/lastenheft.md#lh-qa-03--testbarkeit)).
- [ ] Build-/Arch-/Coverage-Integration ist vollstaendig:
  `settings.gradle.kts`, `.a-check.yml`, `Dockerfile`, Modul-`build.gradle.kts`
  und Kover-Gate sind ergaenzt ([`ADR-0003`](../../adr/0003-hexslice-architektur.md), [`ADR-0006`](../../adr/0006-coverage-gate-scope.md)).
- [ ] Nutzer-/Integrationsdoku und Verification-Artefakt beschreiben den echten
  Hypothesen-Providerpfad, aber kein CLI-Default-Binding, keine Live-Provider-
  Tests und keine Secrets in Tests oder Doku.
- [ ] `make doc-check` und `make gates` sind gruen; Closure-Notiz benennt, ob
  ein Koog/LangChain4j-Paritaets-Folgeslice noetig ist.

## 3. Plan (vor Code)

| Datei / Komponente | Aenderungs-Art | Begruendung |
|---|---|---|
| `adapters/outbound/llm-hypothesen-langchain4j` oder enger benannter Provider-Adapter | neu | Echter Outbound-Adapter hinter `HypothesenPort` (`ARC-08`). |
| `.../src/main/kotlin/**` | neu | Prompt-Factory, Runner/Fabrik (`fromChatModel` oder aequivalent), strikter JSON-Parser, Response-Mapping zu `HypothesenKandidat`. |
| `.../src/test/kotlin/**` | neu | Lokale Stub-/Runner-Tests ohne Netz/API-Key fuer Schema, Prompt, Fehlerfaelle und fail-closed Verhalten. |
| `settings.gradle.kts` | update | Neues Adaptermodul registrieren. |
| `.a-check.yml` | update | Adapterrolle und erlaubte Kanten aufnehmen; Core bleibt providerfrei. |
| `Dockerfile` | update | Dependency-, Test- und Coverage-Gate-Stages um das Modul ergaenzen. |
| `docs/user/integration.md` | update | Einbaupfad fuer echten Hypothesen-Provider dokumentieren; Secrets/Modelle bleiben im Composition-Root. |
| `docs/reviews/*slice-044*` | neu | Review-Artefakt mit Fokus Parser-/Port-Trennung. |
| `docs/verifications/*slice-044*` | neu | Verification-Artefakt fuer DoD, lokale Tests und Gates. |

## 4. Trigger

`slice-019`, `slice-025` und `slice-026` liegen in `done/`: echte
LLM-Framework-Bindings existieren als Vorbild, der `HypothesenPort` ist im
Application-Flow angebunden, und der Fake-Adapter liefert den bisherigen
Contract. Kein Slice liegt in `in-progress/` (WIP-Limit 1). Vor Start wird
genau ein Provider-/Framework-Pfad fuer diesen Slice festgelegt; falls zwei
Pfade gleichzeitig noetig sind, wird vor Code ein Paritaets-Folgeslice geplant.

## 5. Closure-Trigger

DoD vollstaendig + Review/Verification abgeschlossen + `make gates` gruen +
Closure-Notiz geschrieben + Slice nach `done/` verschoben.

## 6. Risiken und offene Punkte

- Der Adapter darf den bestehenden `LlmPort` fuer Likelihoods nicht erweitern
  oder mit Hypothesen-Erzeugung vermischen. `HypothesenPort` bleibt getrennt.
- Modellantworten koennen freie Texte, unsichere IDs oder scheinbare
  Wahrscheinlichkeiten enthalten. Nur streng validierte Kandidaten duerfen als
  Domain-`HypothesenKandidat` in den Use Case gelangen.
- `score` beansprucht Resthypothesen-Masse und ist keine gate-faehige
  Erfolgswahrscheinlichkeit; Gate-/Aktionslogik bleibt ausserhalb dieses Slice.
- Provider-/Modellwahl, API-Keys, Live-Netztests, CLI-Default-Binding und
  produktive Prompt-Kalibrierung bleiben ausserhalb dieses Slice.
- Koog/LangChain4j-Paritaet fuer Hypothesen-Provider bleibt ein Folgeslice, wenn
  nach dem ersten echten Pfad Paritaet benoetigt wird.

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss fuellen. -->

## 8. Sub-Area-Modus-Begründung

### Sub-Area: `adapters/outbound/llm-hypothesen-*`

- **Modus:** GF
- **Konventionen-Dichte:** hoch. `ARC-08`, [`ADR-0003`](../../adr/0003-hexslice-architektur.md) und [`ADR-0006`](../../adr/0006-coverage-gate-scope.md) fuehren
  Outbound-Adapter, Build-/Arch-Gate-Einbindung und per-Modul-Coverage; echte
  `LlmPort`-Adapter in `llm-langchain4j`/`llm-koog` liefern das lokale Muster
  fuer Runner, Prompt-Factory, Parser und Stub-Tests.
- **Phase-Reife:** Phase 3. Port, Domain-Kandidat und Fake sind stabil
  (`slice-021`, `slice-025`, `slice-026`); der echte Provider-Adapter entsteht
  neu.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Das Risiko liegt in Parser- und
  Semantik-Drift: Provider-Antworten koennten Scores, Evidenz oder IDs anders
  interpretieren als der Domain-Vertrag.
- **Reconciliation-Aufwand:** Teil dieses Slice: strikte Schema-Tests, Build-/
  Arch-/Coverage-Integration und Verification. Graduation-Trigger:
  `make gates` gruen und Closure ohne offene Port-/Parser-Drift.

### Sub-Area: `hexagon/application/belief/aktualisieren`

- **Modus:** Hybrid
- **Konventionen-Dichte:** hoch fuer lokale Ports und Re-Hypothesen-Flow
  (`slice-025`, `ARC-07`), mittel fuer echte Provider-Antworten, weil bisher nur
  der Fake die Kandidaten liefert.
- **Phase-Reife:** Phase 4 fuer `HypothesenPort` und
  `BeliefAktualisieren`; Phase 2-3 fuer Provider-Response-Kompatibilitaet.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Ein Provider-Adapter koennte
  Kandidaten als Belief-Mutation, Likelihood-Schaetzung oder Aktionssignal
  missverstehen; der Use Case darf weiter nur strukturierte Kandidaten
  konsumieren.
- **Reconciliation-Aufwand:** klein im Slice: keine Port-Aenderung geplant,
  aber Tests muessen zeigen, dass Provider-Antworten weiterhin nur als
  `HypothesenKandidat` in den bestehenden Validierungs- und Uebernahmepfad
  gehen. Falls der Port erweitert werden muss, wird ein separater
  Contract-Slice geplant.

### Sub-Area: `adapters/inbound/cli` / Runtime-Binding

- **Modus:** GF
- **Konventionen-Dichte:** hoch. `ARC-09` verortet Composition-Root und
  Runtime-Binding im Inbound-Adapter; dieser Slice soll keinen CLI-Default und
  keine produktive Provider-Konfiguration veraendern.
- **Phase-Reife:** Phase 4. CLI-Composition ist vorhanden (`slice-024`,
  `slice-030`); echtes Hypothesen-Provider-Binding bleibt ein bewusster
  Folgeschritt.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Eine nebenbei geaenderte Runtime-
  Verdrahtung koennte Netz/Secrets oder echte Modellantworten in hermetische
  Gates bringen.
- **Reconciliation-Aufwand:** keiner in diesem Slice; eigener Binding-Slice,
  falls CLI-Flags, Modellwahl, Secrets oder Default-Adapter geaendert werden.
