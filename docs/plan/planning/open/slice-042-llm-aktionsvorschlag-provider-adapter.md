# Slice slice-042: LLM-Aktionsvorschlag-Provider-Adapter

**Status:** open (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung.

**Bezug:** `LH-FA-LLM-001`, `LH-FA-LLM-002`, `LH-FA-LLM-003`,
`LH-FA-LLM-004`, `LH-FA-ACT-001`, `LH-FA-ACT-002`, `LH-FA-ACT-003`,
`LH-FA-ACT-004`, `LH-FA-POL-006`, `LH-QA-02`, `LH-QA-03`, `LH-QA-04`;
`ADR-0001`, `ADR-0003`, `ADR-0006`, `ARC-03`, `ARC-07`, `ARC-08`, `ARC-09`.

**Autor:** Codex. **Datum:** 2026-07-08.

---

## 1. Ziel

Ein echter, lokal testbarer LLM-/Provider-Adapter implementiert
`AktionsVorschlagsPort` fuer die abgegrenzte Modellaufgabe "Aktionen
vorschlagen" und liefert ausschliesslich strukturierte Rohvorschlaege, die der
bestehende Use Case `AktionsVorschlagen` weiterhin gegen Belief, Evidenz,
Wirkungsklassen und externalisierte Konfidenz validiert.

## 2. Definition of Done

- [ ] Neues Outbound-Adaptermodul (z. B. `llm-action-langchain4j` oder nach
  Design-Review gleichwertig) implementiert `AktionsVorschlagsPort` hinter
  `ARC-08`; `hexagon:*` importiert keine Provider-/Framework-Pakete.
- [ ] Prompt, Response-DTO und Parser sind strikt schema-gebunden: genau die
  erlaubten Felder `beschreibung`, `hypotheseId`, `wirkungsklasse`,
  `pSuccess`, `konfidenzReferenz`, `stuetzendeEvidenz`; unbekannte Felder,
  doppelte JSON-Felder, unbekannte Hypothesen, ungueltige Wirkungsklassen,
  fehlende Evidenz, nicht-endliche Zahlen und Werte ausserhalb `[0,1]` werden
  fail-closed verworfen oder als Adapterfehler sichtbar.
- [ ] Lokale Tests ohne Provider/API-Key decken erfolgreiche Normalisierung,
  leere Antwort, kaputtes JSON, Schema-Abweichungen, Prompt-Inhalt,
  unbekannte Hypothese, Konfidenzreferenz-Pflicht und keine Gate-/Executor-
  Kopplung ab (`LH-QA-02`, `LH-QA-03`).
- [ ] Build-/Arch-/Coverage-Integration ist vollstaendig:
  `settings.gradle.kts`, `.a-check.yml`, `Dockerfile`, Modul-`build.gradle.kts`
  und Kover-Gate sind ergaenzt (`ADR-0003`, `ADR-0006`).
- [ ] Nutzer-/Integrationsdoku und Verification-Artefakt beschreiben, dass der
  Adapter nur Vorschlaege liefert: keine Freigabe, keine Ausfuehrung, kein
  CLI-Default-Binding, keine Produktiv-Secrets in Tests oder Doku.
- [ ] `make doc-check` und `make gates` sind gruen; Closure-Notiz benennt, ob
  ein zweiter Provider-/Framework-Pfad als Folgeslice noetig ist.

## 3. Plan (vor Code)

| Datei / Komponente | Aenderungs-Art | Begruendung |
|---|---|---|
| `adapters/outbound/llm-action-langchain4j` oder enger benannter Provider-Adapter | neu | Echter Outbound-Adapter hinter `AktionsVorschlagsPort` (`ARC-08`). |
| `.../src/main/kotlin/**` | neu | Prompt-Factory, Runner/Fabrik (`fromChatModel` oder aequivalent), strikter JSON-Parser, Response-Mapping zu `AktionsVorschlag`. |
| `.../src/test/kotlin/**` | neu | Lokale Stub-/Runner-Tests ohne Netz/API-Key fuer Schema, Prompt, Fehlerfaelle und Fail-closed Verhalten. |
| `settings.gradle.kts` | update | Neues Adaptermodul registrieren. |
| `.a-check.yml` | update | Adapterrolle und erlaubte Kanten aufnehmen; Core bleibt providerfrei. |
| `Dockerfile` | update | Dependency-, Test- und Coverage-Gate-Stages um das Modul ergaenzen. |
| `docs/user/integration.md` | update | Einbaupfad fuer echten Aktionsvorschlags-Provider dokumentieren; Secrets/Modelle bleiben im Composition-Root. |
| `docs/reviews/*slice-042*` | neu | Review-Artefakt mit Fokus Parser-/Safety-Grenze. |
| `docs/verifications/*slice-042*` | neu | Verification-Artefakt fuer DoD, lokale Tests und Gates. |

## 4. Trigger

`slice-019`, `slice-023`, `slice-024` und `slice-028` liegen in `done/`:
echte LLM-Framework-Bindings existieren als Vorbild, der
`AktionsVorschlagsPort` samt Fake ist vorhanden, der CLI-Composition-Root
existiert, und externalisierte Konfidenz ist im Entscheidungszyklus gebunden.
Kein Slice liegt in `in-progress/` (WIP-Limit 1). Vor Start wird genau ein
Provider-/Framework-Pfad fuer diesen Slice festgelegt; falls zwei Pfade
gleichzeitig noetig sind, wird der Slice vor Code geteilt.

## 5. Closure-Trigger

DoD vollstaendig + Review/Verification abgeschlossen + `make gates` gruen +
Closure-Notiz geschrieben + Slice nach `done/` verschoben.

## 6. Risiken und offene Punkte

- Der Adapter darf keine `Aktionsfreigabe` erzeugen und keinen Executor-Pfad
  oeffnen. Das Konfidenz-Gate und Human Approval bleiben ausserhalb der
  Modellaufgabe (`LH-FA-POL-006`).
- LLM-Antworten koennen frei formuliert, unvollstaendig oder manipuliert sein.
  Parser und Mapping muessen deshalb strikt und fail-closed bleiben.
- `pSuccess` ist nur ein Rohwert im Vorschlag; Gate-Faehigkeit entsteht erst im
  bestehenden `AktionsVorschlagen`-Use-Case ueber externalisierte Konfidenz.
- Provider-/Modellwahl, API-Keys, Runtime-Secrets, Live-Netztests und CLI-
  Default-Binding bleiben ausserhalb dieses Slice.
- Ein zweiter Framework-Pfad (z. B. Koog neben LangChain4j) bleibt ein
  Folgeslice, wenn Paritaet tatsaechlich gebraucht wird.

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss fuellen. -->

## 8. Sub-Area-Modus-Begründung

### Sub-Area: `adapters/outbound/llm-action-*`

- **Modus:** GF
- **Konventionen-Dichte:** hoch. `ARC-08`, `ADR-0003` und `ADR-0006` fuehren
  Outbound-Adapter, Build-/Arch-Gate-Einbindung und per-Modul-Coverage; echte
  LLM-Adapter in `llm-langchain4j`/`llm-koog` liefern das lokale Muster fuer
  Runner, Prompt-Factory, Parser und Stub-Tests.
- **Phase-Reife:** Phase 3. Der Port und der Fake sind stabil (`slice-023`),
  der echte Aktionsvorschlags-Provider-Adapter entsteht neu.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Das Risiko liegt in Parser-Drift:
  ein Provider-Adapter koennte Felder akzeptieren, die der Fake nicht abbildet,
  oder Vorschlaege erzeugen, die als Entscheidung missverstanden werden.
- **Reconciliation-Aufwand:** Teil dieses Slice: strikte Schema-Tests, Build-/
  Arch-/Coverage-Integration und Verification. Graduation-Trigger:
  `make gates` gruen und Closure ohne offene Parser-/Gate-Drift.

### Sub-Area: `hexagon/application/belief/aktionsvorschlag`

- **Modus:** Hybrid
- **Konventionen-Dichte:** hoch fuer den bestehenden Port und Use Case
  (`slice-023`, `ARC-07`), mittel fuer echte Provider-Antworten, weil bisher nur
  der Fake die Rohwerte liefert.
- **Phase-Reife:** Phase 4 fuer `AktionsVorschlagsPort` und
  `AktionsVorschlagen`; Phase 2-3 fuer Provider-Response-Kompatibilitaet.
- **Evidenz-/Diskrepanz-Risiko:** mittel bis hoch. Provider-Rohdaten koennen
  Hypothesen-, Evidenz- oder Konfidenzreferenzen anders benennen als der Use
  Case erwartet; das muss fail-closed statt still normalisiert werden.
- **Reconciliation-Aufwand:** klein im Slice: keine Port-Aenderung geplant,
  aber Tests muessen zeigen, dass Provider-Antworten weiterhin nur als
  `AktionsVorschlag` in den bestehenden Validierungsrand gehen. Falls der Port
  erweitert werden muss, wird ein separater Contract-Slice geplant.

### Sub-Area: `adapters/inbound/cli` / Runtime-Binding

- **Modus:** GF
- **Konventionen-Dichte:** hoch. `ARC-09` verortet Composition-Root und
  Executor-Grenze im Inbound-Adapter; dieser Slice soll keinen CLI-Default und
  keine Ausfuehrungsgrenze veraendern.
- **Phase-Reife:** Phase 4. CLI-Composition und Executor-Grenze sind vorhanden
  (`slice-024`, `slice-030`); Provider-Binding bleibt ein bewusster Folgeschritt.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Eine nebenbei geaenderte Runtime-
  Verdrahtung koennte Netz/Secrets oder echte Vorschlaege in hermetische Gates
  bringen.
- **Reconciliation-Aufwand:** keiner in diesem Slice; eigener Binding-Slice,
  falls CLI-Flags, Modellwahl, Secrets oder Default-Adapter geaendert werden.
