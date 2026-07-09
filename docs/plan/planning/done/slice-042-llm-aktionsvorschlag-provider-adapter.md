# Slice slice-042: LLM-Aktionsvorschlag-Provider-Adapter

**Status:** open (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung.

**Bezug:** `LH-FA-LLM-001`, `LH-FA-LLM-002`, `LH-FA-LLM-003`,
`LH-FA-LLM-004`, `LH-FA-ACT-001`, `LH-FA-ACT-002`, `LH-FA-ACT-003`,
`LH-FA-ACT-004`, `LH-FA-POL-006`, `LH-QA-02`, `LH-QA-03`, `LH-QA-04`;
`ADR-0001`, `ADR-0002`, `ADR-0003`, `ADR-0006`, `ARC-03`, `ARC-07`, `ARC-08`, `ARC-09`.

**Autor:** Codex. **Datum:** 2026-07-08.

---

## 1. Ziel

Ein echter, lokal testbarer LLM-/Provider-Adapter implementiert
`AktionsVorschlagsPort` fuer die abgegrenzte Modellaufgabe "Aktionen
vorschlagen" und liefert ausschliesslich strukturierte Rohvorschlaege, die der
bestehende Use Case `AktionsVorschlagen` weiterhin gegen Belief, Evidenz,
Wirkungsklassen und externalisierte Konfidenz validiert.

## 2. Definition of Done

- [ ] Neues Outbound-Adaptermodul `adapters/outbound/llm-action-langchain4j`
  (JVM, `src/main/kotlin`) implementiert `AktionsVorschlagsPort` hinter `ARC-08`;
  `hexagon:*` importiert keine Provider-/Framework-Pakete. Framework:
  **LangChain4j** (`dev.langchain4j:1.17.1`, bereits adoptiert → keine neue
  Toolchain-Fläche, kein Folge-ADR; §9 F-3). Koog-Parität ist `slice-043`.
- [ ] **Schicht-Trennung (Adapter = Wire-Integrität, Use Case = Semantik; §9 F-1):**
  Prompt, Response-DTO und Parser sind strikt schema-gebunden — der **Adapter**
  prüft nur Wire-/Deserialisierungs-Integrität: genau die erlaubten JSON-Felder
  `beschreibung`, `hypotheseId`, `wirkungsklasse`, `pSuccess`, `konfidenzReferenz`,
  `stuetzendeEvidenz`; unbekannte/doppelte JSON-Felder, fehlende Pflichtfelder,
  falscher Typ/Shape und nicht-endliche Zahlen werden fail-closed behandelt und
  auf `AktionsVorschlag` (primitive Rohwerte) gemappt. Die **Semantik**
  (unbekannte Hypothese, ungültige Wirkungsklasse, Evidenz-Auflösung/Nicht-Leere,
  Konfidenz-Bereich `[0,1]`) bleibt im Use Case `AktionsVorschlagen`
  (`:59/63/66/68/71`) — der Adapter dupliziert sie **nicht** und **kann** die
  Evidenzprüfung nicht leisten (`vorschlaege(belief)` trägt keinen Evidenz-Kontext;
  §8 hält den Port unverändert). Spiegelt slice-041 DR-F3.
- [ ] **Fehler-Signalisierung je Klasse (§9 F-2):** leere Provider-Antwort →
  `emptyList()` (legitim „kein Vorschlag", Fake-Parity `FakeAktionsVorschlagsPort.kt:38-40`);
  einzelner wire-defekter Vorschlag → dieser wird verworfen, valide bleiben;
  Provider-Ausfall/Transport-Fehler/komplett unparsebare Antwort → **sichtbarer**
  Adapterfehler (geworfen; propagiert außerhalb des per-Vorschlag-`runCatching`,
  `AktionsVorschlagen.kt:47`) — „Provider unreachable" bleibt unterscheidbar von
  „kein Vorschlag" (`LH-QA-02`).
- [ ] Lokale Tests ohne Provider/API-Key decken ab: erfolgreiche Normalisierung,
  Wire-Fehlerklassen (leere Antwort → leer; wire-defekter Einzelvorschlag →
  verworfen; Provider-Ausfall/unparsebar → sichtbarer Wurf), Prompt-Inhalt,
  Pass-Through semantisch offener Rohwerte an den Use-Case-Validierungsrand (der
  Adapter re-validiert Hypothese/Wirkungsklasse/Evidenz/Bereich nicht) und keine
  Gate-/Executor-Kopplung (`LH-QA-02`, `LH-QA-03`).
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
| `adapters/outbound/llm-action-langchain4j` (JVM, `src/main/kotlin`) | neu | Echter LangChain4j-Outbound-Adapter hinter `AktionsVorschlagsPort` (`ARC-08`); Provider-Dep bereits adoptiert, keine neue Toolchain-Fläche (§9 F-3). |
| `.../src/main/kotlin/**` | neu | Prompt-Factory, Runner/Fabrik (`fromChatModel` oder aequivalent), strikter JSON-Parser, Response-Mapping zu `AktionsVorschlag`. |
| `.../src/test/kotlin/**` | neu | Lokale Stub-/Runner-Tests ohne Netz/API-Key fuer Schema, Prompt, Fehlerfaelle und Fail-closed Verhalten. |
| `settings.gradle.kts` | update | Neues Adaptermodul registrieren. |
| `.a-check.yml` | update | Neue Rolle `outbound_llm_action_langchain4j`, Root `adapters/outbound/llm-action-langchain4j/src/main/kotlin/dev/beliefagent`, Kanten `→ application`/`→ domain`; Core bleibt providerfrei. |
| `Dockerfile` | update | Dependency-, Test- und Coverage-Gate-Stages um das Modul ergaenzen. |
| `docs/user/integration.md` | update | Einbaupfad fuer echten Aktionsvorschlags-Provider dokumentieren; Secrets/Modelle bleiben im Composition-Root. |
| `docs/reviews/*slice-042*` | neu | Review-Artefakt mit Fokus Parser-/Safety-Grenze. |
| `docs/verifications/*slice-042*` | neu | Verification-Artefakt fuer DoD, lokale Tests und Gates. |

## 4. Trigger

`slice-019`, `slice-023`, `slice-024` und `slice-028` liegen in `done/`:
echte LLM-Framework-Bindings existieren als Vorbild, der
`AktionsVorschlagsPort` samt Fake ist vorhanden, der CLI-Composition-Root
existiert, und externalisierte Konfidenz ist im Entscheidungszyklus gebunden.
Kein Slice liegt in `in-progress/` (WIP-Limit 1). Der Provider-/Framework-Pfad
ist entschieden (Architect, §9 F-3/PR-F1): **LangChain4j** (bereits adoptiert)
für diesen Slice; Koog-Parität ist `slice-043`. Fiele die Wahl auf ein **nicht**
adoptiertes Framework, griffe der `ADR-0002`-Guard (Folge-ADR vor Code) — hier
nicht der Fall.

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
- Ein zweiter Framework-Pfad (Koog neben LangChain4j) ist `slice-043`
  (Parität), kein Teil dieses Slice.
- `ADR-0002`-Guard (§9 F-3): LangChain4j/Koog sind bereits adoptiert → kein
  Folge-ADR. Nur falls ein **nicht** adoptiertes Framework gewählt würde, wäre vor
  Code ein Folge-ADR nötig (hier nicht der Fall).
- Schicht-Trennung (§9 F-1) muss testbar scharf bleiben: der Adapter darf die
  Use-Case-Semantik (Hypothese/Wirkungsklasse/Evidenz/Bereich) nicht duplizieren,
  sonst Doppel-Quelle/Drift wie in slice-041 DR-F3.

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

## 9. Design-/Review-Klärung (Rückkante Review → Plan, Modul 8)

Übergabe-Artefakt für die Findings aus
[`docs/reviews/2026-07-09-slice-042-design-review.md`](../../../reviews/2026-07-09-slice-042-design-review.md)
(DR: 1× MEDIUM, 1× LOW, 1× INFO — merge-blockierend) und
[`docs/reviews/2026-07-09-slice-042-plan-review.md`](../../../reviews/2026-07-09-slice-042-plan-review.md)
(PR: 1× LOW, 1× INFO). Entscheidung und Umsetzung liegen bei Architect/Planner
(Modul 8); der Reviewer kategorisiert nur. Vor Implementierungsstart abzuschließen.

| Finding | Kat. | Entscheidung | Verankert in |
|---|---|---|---|
| DR-F1 / PR-F2 Validierungs-Schicht | MEDIUM / LOW | Schicht-Trennung wie slice-041 DR-F3: **Adapter** = Wire-/Deserialisierungs-Integrität (erlaubte/unbekannte/doppelte JSON-Felder, Pflichtfelder, Typ/Shape, nicht-endliche Zahlen); **Use Case** `AktionsVorschlagen` behält die Semantik (Hypothese/Wirkungsklasse/Evidenz/Bereich, `:59/63/66/68/71`). Der Adapter dupliziert sie nicht und **kann** Evidenz nicht prüfen (`vorschlaege(belief)` ohne Evidenz-Kontext; §8 hält den Port unverändert). | §2 DoD 2, §3, §8 |
| DR-F2 Fehler-Signalisierung | LOW | Je Fehlerklasse bestimmt: leere Antwort → `emptyList()` (Fake-Parity); wire-defekter Einzelvorschlag → verworfen, valide bleiben; Provider-Ausfall/unparsebar → sichtbarer Wurf (propagiert außerhalb `AktionsVorschlagen.kt:47`). „Provider unreachable" ≠ „kein Vorschlag". Kein Pflicht-Audit-Verlust wie slice-041. | §2 DoD 2/3 |
| DR-F3 ADR-0002-Guard | INFO | LangChain4j/Koog bereits adoptiert → neuer `llm-action-langchain4j`-Adapter fügt keine Toolchain-Fläche hinzu (kein Folge-ADR). Nur bei einem **nicht** adoptierten Framework griffe der `ADR-0002`-Guard (Folge-ADR vor Code). `ADR-0002` in **Bezug** ergänzt. | Kopf **Bezug**, §2 DoD 1, §4, §6 |
| PR-F1 Modul/Framework offen | INFO | Konvergiert: `adapters/outbound/llm-action-langchain4j` (LangChain4j), `.a-check`-Rolle `outbound_llm_action_langchain4j` + Root; nicht mehr disjunktiv. Koog-Parität = `slice-043`. | §2 DoD 1, §3, §4 |
