# Review-Report: slice-042 (LLM-Aktionsvorschlag-Provider-Adapter) — 2026-07-09

**Review-Art:** Code — geprüft wird der **fertige Diff gegen Plan + Konventionen**
(`AGENTS.md` Hard Rules), Modul 10 §Drei Review-Arten. Plan-/Design-Review
(inkl. blockierendem DR-F1) liegen vor und sind in `slice-042 §9` aufgelöst;
dieser Lauf prüft den **Code** gegen diese aufgelösten Entscheidungen.

**Gegenstand:** Arbeitsbaum-Diff slice-042 — neues (untracked) Modul
`adapters/outbound/llm-action-langchain4j/`
(`LangChain4jAktionsVorschlagsPort.kt` inkl. `StrictAktionsVorschlagParser`,
`build.gradle.kts`, Testdatei) plus tracked Änderungen an `settings.gradle.kts`,
`.a-check.yml`, `Dockerfile`, `docs/user/integration.md`.

**Skill:** `.harness/skills/reviewer.md` @ v1.0 <!-- d-check:ignore (Skill-Pfad) -->
**Modell:** claude-opus-4-8[1m] · **Datum:** 2026-07-09

**Eingangs-Kontext** (die Verträge, gegen die geprüft wurde):

- Slice-Plan `docs/plan/planning/in-progress/slice-042-...md` (DoD §2, Plan §3,
  §9-Auflösungen DR-F1/PR-F2, DR-F2, DR-F3/PR-F1).
- Berührte `LH-*`: `LH-FA-LLM-001..004`, `LH-FA-ACT-001..004`, `LH-FA-POL-006`,
  `LH-QA-02/03/04`.
- ADRs: `ADR-0001`/`ADR-0003` (Hexagonal/HexSlice), `ADR-0002` (Dep am Rand),
  `ADR-0006` (Coverage-Scope), `ARC-03/07/08/09`.
- `AGENTS.md §3` (Hard Rules) + Source Precedence.
- Ist-Code der berührten Verträge: `AktionsVorschlagsPort`, `AktionsVorschlag`,
  `AktionsVorschlagen` (Use-Case-Semantik), `FakeAktionsVorschlagsPort` (Parität),
  `Wirkungsklasse`; Provider-Adapter-Präzedenz `llm-langchain4j`.
- Vorherige Findings am Modul: `2026-07-09-slice-042-{design,plan}-review.md`;
  slice-041-Review-Kette (Schicht-Trennungs-Lehre).

---

## Findings

Reihenfolge **HIGH zuerst** (Skill §HIGH zuerst). **Keine HIGH-Findings:** der
Adapter erzeugt keine `Aktionsfreigabe`, öffnet keinen Executor-Pfad, umgeht kein
Gate (`LH-FA-POL-006`); Fehler sind fail-closed (Wurf oder Verwurf, nie
fabrizierte Vorschläge); die semantische Validierung bleibt im Use Case (DR-F1
umgesetzt — Pass-Through-Test `reicht_semantisch_offene_rohwerte_an_den_use_case_durch`).

### F-1 — Fehlerklasse „doppelte JSON-Felder" endet als Ganz-Antwort-Wurf, nicht als Einzel-Verwurf

- `kategorie`: MEDIUM
- `quelle`: `slice-042 §2 DoD 2` + `§9 DR-F2`, `LH-QA-02`; Maintainability
- `pfad`: `adapters/outbound/llm-action-langchain4j/src/main/kotlin/dev/beliefagent/adapter/action/langchain4j/LangChain4jAktionsVorschlagsPort.kt:126` (`STRICT_DUPLICATE_DETECTION`), `:139-148` (`parseJson` → Wurf), vgl. Test `:71-76` (`doppelte_json_felder_werfen`)
- `befund`: DoD 2 führt „unbekannte/**doppelte** JSON-Felder" in der Liste der
  Wire-Defekte, die „auf `AktionsVorschlag` gemappt" werden (per-Vorschlag-Ebene),
  und §9 DR-F2 setzt „wire-defekter **Einzel**vorschlag → verworfen, valide
  bleiben". Die Implementierung erkennt Duplikate über `STRICT_DUPLICATE_DETECTION`
  beim `readTree` und wirft dadurch die **gesamte** Antwort (`AktionsVorschlagAntwortFehler`),
  auch wenn nur ein einzelnes Array-Element ein Duplikat trägt — co-lokalisierte
  valide Vorschläge gehen mit verloren, und „ein defekter Vorschlag" wird
  ununterscheidbar von „komplett unparsebare Antwort". Fail-closed-Richtung
  (wirft, akzeptiert nie still), aber die Granularität weicht von der agreed
  Fehlerklassen-Auflösung ab. Ein defensibler Gegenlesart: ein Duplikat macht das
  JSON-Dokument tokenizer-seitig unparsebar → „unparsebare Antwort → Wurf".
- `verifizierbar`: ja — `make test` (der bestehende Test dokumentiert den
  Ganz-Antwort-Wurf; zu klären ist, ob diese Klasse so gewollt ist → DoD-2/§9-Wortlaut
  oder Parser-Granularität angleichen).

### F-2 — KDoc-Referenz `[warnung]` an der Port-Klasse zeigt ins Leere

- `kategorie`: LOW
- `quelle`: Maintainability
- `pfad`: `.../LangChain4jAktionsVorschlagsPort.kt:29` (KDoc der Port-Klasse)
- `befund`: Der Klassen-KDoc von `LangChain4jAktionsVorschlagsPort` verweist auf
  `[warnung]`, doch `warnung` ist Member von `StrictAktionsVorschlagParser`, nicht
  der Port-Klasse — der Dokka-Link ist unauflösbar. Rein dokumentarisch, kein
  Verhaltenseffekt.
- `verifizierbar`: nein — kein `make`-Gate prüft KDoc-Linkauflösung.

### F-3 — Code-Safety-Review (Companion) noch offen

- `kategorie`: INFO (eigener Safety-Durchgang)
- `quelle`: Reviewer-Skill §„Repo-Zusatz — Code-Safety-Review"; `LH-FA-POL-006`, `LH-QA-02`
- `pfad`: `adapters/outbound/llm-action-langchain4j/**` (adversariale LLM-Ausgabe → Parser-Grenze)
- `befund`: Der Diff berührt eine Sicherheitsgrenze (strikter Parser gegen
  adversariale/manipulierte Modellausgabe; Nicht-Kopplung an Gate/Executor —
  vom Design-Review als Safety-Fokus markiert). Der Skill verlangt dafür einen
  **eigenen** fail-closed-Durchgang als getrenntes Artefakt
  `docs/reviews/2026-07-09-slice-042-code-safety-review.md` (etablierte Praxis
  `slice-035`..`040`); dieser Code-Review ersetzt ihn nicht.
- `verifizierbar`: nein — Prozess-/Harness-Artefakt.

### F-4 — Provider-/Transport-Ausfall propagiert als roher Provider-Fehlertyp

- `kategorie`: INFO (Konsumenten-Eskalation — Folgeslice-Verweis)
- `quelle`: `slice-042 §2 DoD 3` / `§9 DR-F2`, `LH-QA-02`
- `pfad`: `.../LangChain4jAktionsVorschlagsPort.kt:56` (`chat.chat(...)`), Test `:79-84`
- `befund`: Ein Provider-/Transport-Ausfall propagiert als die **rohe**
  Provider-Exception (im Test `IllegalStateException`), **nicht** in einen
  adapter-eigenen Fehlertyp gehüllt — anders als `AktionsVorschlagAntwortFehler`
  für Wire-/Parse-Fehler und anders als slice-041s `AuditPersistenzFehler`-Basis.
  Konform zum Plan (der explizit „die Transport-Exception" propagieren lässt) und
  fail-closed sichtbar; ein Konsument kann „Provider unreachable" aber nicht per
  Typ von einem internen Fehler unterscheiden. Relevant, sobald ein Slice die
  Vorschläge-Konsumenten geordnet eskalieren lässt.
- `verifizierbar`: ja — `make test` (`provider_ausfall_propagiert_sichtbar`).

### F-5 — Diff untracked/uncommittet: Traceability-Gates erst nach Commit prüfbar

- `kategorie`: INFO (Doku-Regel / Verifier-Verweis)
- `quelle`: `AGENTS.md §5` (IDs in PR/Commit), `MR-006`
- `pfad`: gesamter slice-042-Diff (`adapters/outbound/llm-action-langchain4j/` untracked)
- `befund`: Das Adaptermodul ist noch untracked; `make doc-commits`/
  `make doc-immutable` und „IDs-im-Commit" (`AGENTS.md §5`) sind erst nach dem
  Commit auswertbar. Der Commit muss die berührten IDs referenzieren
  (`LH-FA-LLM-001..004`, `LH-FA-ACT-001..004`, `LH-FA-POL-006`, `LH-QA-02/03/04`,
  `ADR-0001/0002/0003/0006`, `ARC-03/07/08/09`).
- `verifizierbar`: ja — `make doc-commits` (nach Commit).

## Negativbefunde

- geprüft, ohne Befund: `hexagon/application/.../ports/AktionsVorschlagsPort.kt`,
  `.../dto/AktionsVorschlag.kt`, `.../AktionsVorschlagen.kt` — **unverändert**;
  Port-Signatur `vorschlaege(belief)` ohne Evidenz-Kontext belassen (§8, DR-F1),
  Use-Case-Semantik (Hypothese/Wirkungsklasse/Evidenz/Bereich `:59/63/66/68/71`)
  nicht in den Adapter dupliziert.
- geprüft, ohne Befund: `adapters/outbound/llm-action-langchain4j/src/main/**` —
  Adapter prüft nur Wire-Integrität (exakte Feldmenge `ERLAUBTE_FELDER`, Typ/Shape,
  endliche `pSuccess`); semantisch offene Rohwerte (unbekannte Hypothese, `QUATSCH`-
  Wirkungsklasse, `pSuccess=5.0`, leere Evidenz) werden **durchgereicht**, nicht
  re-validiert (DR-F1); leere/`[]`-Antwort → `emptyList()` (Fake-Parität); Einzel-
  Wire-Defekt → verworfen + `warnung`; keine Gate-/Executor-/Freigabe-Kopplung
  (`LH-FA-POL-006`); Prompt untersagt Freigabe/Ausführung explizit.
- geprüft, ohne Befund: `adapters/outbound/llm-action-langchain4j/src/test/**` —
  Fehlerklassen breit abgedeckt (leer, `[]`, unparsebar → Wurf, Nicht-Array → Wurf,
  Provider-Ausfall → Wurf, unbekanntes/fehlendes Feld/falscher Typ/Nicht-endlich/
  Nicht-Objekt/Nicht-String-Evidenz → verworfen, Pass-Through, Prompt-Inhalt,
  Sauber-Pass ohne Warnung) — `LH-QA-03`.
- geprüft, ohne Befund: `build.gradle.kts` — keine **neue** Toolchain-Fläche
  (`dev.langchain4j:1.17.1` + `jackson-databind:2.21.3` beide bereits in
  `llm-langchain4j` adoptiert, identische Versionen → `ADR-0002`-Guard greift
  nicht, kein Folge-ADR, §9 F-3); per-Modul-`kover { … minBound(90) }` (`ADR-0006`).
- geprüft, ohne Befund: `.a-check.yml` — Rolle `outbound_llm_action_langchain4j`,
  Kanten → `application`/→ `domain`, Resolution-Root; Kern bleibt providerfrei
  (`ADR-0001/0003`).
- geprüft, ohne Befund: `Dockerfile` — Modul an **allen** Enumerationsstellen
  (`COPY` :32, `:dependencies` :37, `:test` :52, `:koverLog` :72, `:koverVerify` :88).
- geprüft, ohne Befund: `settings.gradle.kts` — Modul registriert.
- geprüft, ohne Befund: `docs/user/integration.md` — Adapter dokumentiert (DoD-5),
  „nur Vorschlag, keine Freigabe/Ausführung", kein CLI-Default-Binding.
- keine Suppression (`@Suppress`/`noqa`/`nolint`) im neuen Modul (Hard Rule 3.2).

## Summary

| Kategorie | Anzahl |
|---|---|
| HIGH | 0 |
| MEDIUM | 1 |
| LOW | 1 |
| INFO | 3 |

## Verdikt

**Merge-blockierend (Code-Review):** ja — **1 MEDIUM** (F-1). Vor Merge zu klären,
ob „doppelte JSON-Felder" als Einzel-Vorschlag-Verwurf (DoD-2-/§9-Wortlaut) oder
als Ganz-Antwort-Wurf (Implementierung) behandelt werden — Wortlaut oder Parser
angleichen. Beide Zweige sind fail-closed, daher kein Safety-Loch; die
Reklassifizierung/Behebung entscheidet Architect/Implementation (Reviewer
kategorisiert nur). F-2 (LOW) nice-to-fix; F-3/F-4/F-5 (INFO) sind Verweise auf
Durchgänge/Folgeslices/Commit.

**Vorbehalt (Durchgang):** „review-vollständig" ist der Slice erst mit dem
**Code-Safety-Review** (F-3) — für einen adversarialen LLM-Parser an der
Aktions-Grenze im Safety/Control-Repo ist der eigene fail-closed-Durchgang
Skill-Pflicht.

**Übergabe:** Findings gehen an die Implementation (Rückkante Review → Plan bei
Plan-Defekt — F-1 kann als DoD-2-Wortlaut-Korrektur zurücklaufen). Der Report
ersetzt **keine** Verifikation: DoD-/Fehlerklassen-Matrix und `make gates` prüft
der **Verifier** separat (Modul 11, `docs/verifications/*`) — bewusst **nicht** in
diesem Reviewer-Lauf ausgeführt (Rollentrennung Modul 8).
