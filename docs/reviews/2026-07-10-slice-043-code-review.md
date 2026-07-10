# Review-Report: slice-043 (Koog-Aktionsvorschlag-Adapter, Koog/LangChain4j-Parität) — 2026-07-10

**Review-Art:** Code — geprüft wird der **fertige Arbeitsbaum-Diff gegen Plan +
Konventionen** (`AGENTS.md` Hard Rules), Modul 10 §Drei Review-Arten. Plan- und
Design-Review (`docs/reviews/2026-07-10-slice-043-{plan,design}-review.md`) liegen
vor und sind in `slice-043 §9` (F-1..F-5) aufgelöst; dieser Lauf prüft den **Code**
gegen diese aufgelösten Entscheidungen, nicht die Spec-Konformität (das ist Verifier,
Modul 11).

**Gegenstand:** Arbeitsbaum-Diff slice-043 — neues (untracked) Modul
`adapters/outbound/llm-action-koog/` (`KoogAktionsVorschlagsPort.kt` inkl.
`StrictAktionsVorschlagParser`, `AktionsVorschlagPromptFactory`, Koog-Fabriken;
`build.gradle.kts`; `KoogAktionsVorschlagsPortTest.kt`;
`CapturingKoogAktionsLlmClientClassName.kt`) plus tracked Änderungen an
`settings.gradle.kts`, `.a-check.yml`, `.d-check.yml`, `Dockerfile`,
`docs/user/integration.md`, `docs/user/cli-entscheidungsnachweis.md`,
`harness/conventions.md` (MR-013).

**Skill:** `.harness/skills/reviewer.md` @ v1.0 · <!-- d-check:ignore (Adopter-spezifischer Skill-Pfad) -->
**Modell:** claude-opus-4-8[1m] · **Datum:** 2026-07-10
**Methode:** Rollentrennung Modul 8 — der Diff wurde zusätzlich in einem
**unabhängigen Frischkontext** (separater Reviewer-Agent, gleicher Skill) adversarial
geprüft; jedes Finding ist gegen den Ist-Code gegenverifiziert (Zähl-/Zweig-Belege
unten). Reproduzierbar aus dem Eingangs-Kontext.

**Eingangs-Kontext** (die Verträge, gegen die geprüft wurde):

- Slice-Plan `docs/plan/planning/open/slice-043-...md` (DoD §2, Plan §3, Risiken §6,
  §9-Auflösungen F-1..F-5).
- Berührte `LH-*`: `LH-FA-LLM-001..004`, `LH-FA-ACT-001..004`, `LH-FA-POL-006`,
  `LH-QA-02/03/04`.
- ADRs: `ADR-0001`/`ADR-0003` (Hexagonal/HexSlice), `ADR-0002` (Dep am Rand,
  Framework-Adoption), `ADR-0006` (Coverage-Scope); `ARC-07/08/09`.
- `AGENTS.md §3` (Hard Rules) + Source Precedence (`harness/README.md`).
- Ist-Code der berührten Verträge: `AktionsVorschlagsPort` (unverändert),
  `AktionsVorschlag`, `AktionsVorschlagen` (Use-Case-Semantik); **Soll-Referenz**
  für Parität `adapters/outbound/llm-action-langchain4j/` (slice-042); das laut Plan
  **nicht** zu spiegelnde Muster `KoogLlmPort.kt:230-231`.
- Vorherige Findings am Modul: `2026-07-09-slice-042-{code,code-safety}-review.md`,
  `2026-07-10-slice-043-{plan,design}-review.md`.

---

## Findings

Reihenfolge **HIGH zuerst** (Skill §HIGH zuerst). Jedes Finding folgt dem
**§Output-Schema** des Reviewer-Skills (verbindliche Single Source of Truth).

**Keine HIGH-Findings.** Verifiziert: der Adapter liefert ausschließlich
`List<AktionsVorschlag>` (`KoogAktionsVorschlagsPort.kt:137-142`), erzeugt keine
`Aktionsfreigabe`, öffnet keinen Executor-Pfad und ruft kein Gate — das
Konfidenz-Gate bleibt nicht umgehbar (`LH-FA-POL-006`, Hard Rule 3.7/3.8). Fehler
sind fail-closed: unparsebar/Nicht-Array/**Trailing-Tokens**/doppelte Felder/
Provider-Ausfall → sichtbarer Wurf, wire-defekter Einzelvorschlag → verworfen +
`warnung`, nie fabrizierte Vorschläge (`LH-QA-02`). Der Trailing-Token-Guard
(`nextToken()==null`, `:239-241`) und `STRICT_DUPLICATE_DETECTION` (`:217-221`) sind
faithful aus slice-042 übernommen und spiegeln **nicht** die schwächere
`readTree(raw.trim())`-Vorlage aus `KoogLlmPort.kt:230-231` (§9 F-3 erfüllt). Die
Semantik bleibt im Use Case `AktionsVorschlagen` (Pass-Through-Test
`reicht_semantisch_offene_rohwerte_an_den_use_case_durch`, §9 F-1). Keine
Architektur-/ADR-/KMP-Verletzung, keine gesenkte Schwelle, keine Suppression.

### F-1 — Reflektiver Fabrik-Pfad `fromLlmClient(clientClass)`: 3 von 4 Ablehnungsästen ohne Negativtest

- `kategorie`: MEDIUM
- `quelle`: `LH-QA-03` (Testbarkeit / fehlender Negativtest bei neuem öffentlichem Vertrag); Skill §MEDIUM
- `pfad`: `adapters/outbound/llm-action-koog/src/main/kotlin/dev/beliefagent/adapter/action/koog/KoogAktionsVorschlagsPort.kt:119-134` (`instantiateLlmClient`); Test `KoogAktionsVorschlagsPortTest.kt:287-297`
- `befund`: Der neue reflektive Fabrik-Pfad `fromLlmClient(clientClass: String, …)`
  hat vier Ablehnungsäste: Klasse nicht ladbar (`:120-121`), Klasse ist **kein**
  `LLMClient` (`require(isAssignableFrom)`, `:123-125`), **kein** No-arg-Ctor
  (`:127-130`), Instanziierung schlägt fehl (`:132-133`) — je mit eigener
  Fehlermeldung. Getestet ist nur der erste Ast
  (`factory_weist_unbekannte_klasse_zurueck`, „GibtEsNicht"); die drei übrigen Wurf-
  Äste haben keinen Test. Diese Fabrik existiert in der LangChain4j-Soll-Referenz
  nicht (nur `fromChatModel`), ist also neue öffentliche Fläche außerhalb des durch
  die slice-042-Contract-Matrix gedeckten Umfangs.
- `verifizierbar`: teilweise — `make coverage-gate` greift nur, falls die drei
  ungedeckten Wurf-Zeilen die Modulabdeckung unter `minBound(90)` drücken (bei ~8
  ungedeckten von 298 Zeilen unwahrscheinlich); als Test-Matrix-Lücke sonst nicht
  gate-sichtbar (`make test` bleibt grün, weil die Äste schlicht nicht angesprungen
  werden).

### F-2 — MR-013 behauptet „8 entfernt", der `docs/user`-Diff entfernt 5 Slice-Referenzen

- `kategorie`: LOW
- `quelle`: Maintainability / `MR-013` (Traceability-Genauigkeit)
- `pfad`: `harness/conventions.md` (MR-013-Block, „(8 entfernt)")
- `befund`: MR-013 dokumentiert „**8 entfernt**" für Slice-Referenzen aus der
  Nutzer-Doku. Der Arbeitsbaum-Diff (`git diff HEAD -- docs/user`) entfernt jedoch
  **5** nummerierte Slice-Referenzen (4× `slice-041`, 1× `slice-042`; verteilt auf
  `integration.md` ×4 und `cli-entscheidungsnachweis.md` ×1); HEAD trägt genau diese
  5, der Arbeitsbaum 0. `docs/user` wurde von keinem committeten slice-043-Commit
  berührt (letzte Berührung: slice-042-Commit), der Diff ist also vollständig. Die
  parallele Zahl „(4 Links)" ist hingegen korrekt (`LH-FA-LLM-003`, `LH-QA-06`,
  `LH-QA-02`, `LH-FA-POL-004`).
- `verifizierbar`: nein — `make doc-check`/d-check bestätigt nur, dass **0**
  Slice-Referenzen in `docs/user` verbleiben (grün, MR-013-Tripwire greift nicht);
  der Zählwert „8" selbst prüft kein Gate, und `harness/conventions.md` liegt
  außerhalb der `.d-check.yml`-`roots`.

### F-3 — Provider-/Transportausfall propagiert als roher Fremdtyp (nicht adaptergehüllt)

- `kategorie`: INFO (Konsumenten-Eskalation, Folgeslice-Verweis)
- `quelle`: `slice-043 §2 DoD 3` / `LH-QA-02`; **rekurriert** slice-042 F-4 (2. Auftreten)
- `pfad`: `KoogAktionsVorschlagsPort.kt:66/81` (`executor.execute`/`client.execute` in `runBlocking`); Test `KoogAktionsVorschlagsPortTest.kt:124-129`
- `befund`: Ein Runner-/Transportausfall propagiert als **rohe** Fremd-Exception
  (im Test `IllegalStateException`), nicht in `AktionsVorschlagAntwortFehler` gehüllt
  — identisch zum LangChain4j-Sibling und plan-konform (fail-closed sichtbar). Ein
  Konsument kann „Provider unreachable" nicht per Typ von einem internen Fehler
  trennen. Relevant erst, sobald ein Slice die Vorschläge-Konsumenten geordnet
  eskalieren lässt. Hinweis: gleicher Design-Punkt nun 2× (slice-042/043) — beim 3.
  Auftreten Steering-Loop-Kandidat (Skill §Pflege).
- `verifizierbar`: ja — `make test` (`provider_ausfall_propagiert_sichtbar`).

### F-4 — Companion Code-Safety-Review für den Koog-Parser noch offen

- `kategorie`: INFO (eigener Safety-Durchgang; Rollen-/Prozess-Verweis)
- `quelle`: Reviewer-Skill §„Repo-Zusatz — Code-Safety-Review"; `LH-FA-POL-006`, `LH-QA-02`; **rekurriert** slice-042 F-3
- `pfad`: `adapters/outbound/llm-action-koog/**` (strikter Parser gegen adversariale Modellausgabe)
- `befund`: Der Diff berührt dieselbe Sicherheitsgrenze wie slice-042 (adversarialer
  LLM-Parser an der Aktions-Grenze im Safety/Control-Repo). Der Skill verlangt dafür
  einen **eigenen** fail-closed-Durchgang als getrenntes Artefakt
  `docs/reviews/2026-07-10-slice-043-code-safety-review.md` (etablierte Praxis
  `slice-035`..`042`); dieser allgemeine Code-Review ersetzt ihn nicht. Der
  Trailing-Token-Riss aus dem slice-042-Safety-Lauf ist im Parser bereits übernommen;
  die Fabrik-Ablehnungsäste (F-1) wären ein Safety-Kandidat.
- `verifizierbar`: nein — Prozess-/Harness-Artefakt.

### F-5 — Diff untracked/uncommittet: Traceability-Gates + IDs-im-Commit erst nach Commit prüfbar

- `kategorie`: INFO (Doku-Regel / Verifier-Verweis)
- `quelle`: `AGENTS.md §5` (IDs in PR/Commit), `MR-006`; **rekurriert** slice-042 F-5
- `pfad`: gesamtes neues Modul `adapters/outbound/llm-action-koog/` (git status `??`)
- `befund`: Das Adaptermodul ist noch untracked; `make doc-commits`/
  `make doc-immutable` und „IDs-im-Commit" (`AGENTS.md §5`) sind erst nach dem Commit
  auswertbar. Der Commit muss die berührten IDs referenzieren (`LH-FA-LLM-001..004`,
  `LH-FA-ACT-001..004`, `LH-FA-POL-006`, `LH-QA-02/03/04`, `ADR-0001/0002/0003/0006`,
  `ARC-07/08/09`).
- `verifizierbar`: ja — `make doc-commits` (nach Commit).

## Negativbefunde

- geprüft, ohne Befund: `hexagon/application/.../AktionsVorschlagen.kt` +
  `.../ports/AktionsVorschlagsPort.kt` — **unverändert** (nicht im Diff); Semantik
  (unbekannte Hypothese `:59`, `Wirkungsklasse.valueOf` `:63`, Evidenz-Nicht-Leere
  `:68`, `pSuccess`/Konfidenz `:70-78`, Referenz-Dedup `:48-50`) bleibt im Use Case,
  **nicht** im Koog-Adapter dupliziert; Port-Signatur `vorschlaege(belief)` ohne
  Evidenz-Kontext belassen (§9 F-1).
- geprüft, ohne Befund: `adapters/outbound/llm-action-koog/src/main/**` — nur
  Wire-Integrität (exakte `ERLAUBTE_FELDER`, Typ/Shape, `isFinite`); semantisch offene
  Rohwerte (unbekannte Hypothese, `QUATSCH`-Klasse, `pSuccess=5.0`, leere Evidenz)
  werden **durchgereicht**; Trailing-Token-Guard (`:239-241`) +
  `STRICT_DUPLICATE_DETECTION` (`:217-221`), **nicht** die schwache
  `KoogLlmPort`-Vorlage (§9 F-3); keine Gate-/Executor-/Freigabe-Kopplung
  (`LH-FA-POL-006`); Prompt untersagt Freigabe/Ausführung explizit (`:167`);
  KDoc-Links (`[StrictAktionsVorschlagParser]`/`[vorschlaege]`/`[warnung]` am Parser)
  lösen auf — der slice-042-Dangling-Link (F-2 dort) rekurriert **nicht**.
- geprüft, ohne Befund: `adapters/outbound/llm-action-koog/src/test/**` —
  Wire-Fehlerklassen breit abgedeckt und paritätsäquivalent zum LangChain4j-Sibling
  (leer/`[]` → `emptyList`; unparsebar/Nicht-Array/Trailing-Tokens/Trailing-Müll/
  doppelte Felder/Provider-Ausfall → Wurf; `null`-Feld/Unbekannt-Feld/fehlendes Feld/
  falscher Typ/nicht-endlich/Nicht-Objekt/Nicht-String-Evidenz → verworfen;
  Pass-Through, Prompt-Inhalt, Sauber-Pass ohne Warnung; Fabrik-Verdrahtung
  `fromPromptExecutor`/`fromLlmClient`×3 Happy-Path) — `LH-QA-03`. **Lücke nur an den
  3 Fabrik-Ablehnungsästen → F-1.**
- geprüft, ohne Befund: `adapters/outbound/llm-action-koog/build.gradle.kts` — keine
  **neue** Toolchain-Fläche (`ai.koog:koog-agents:1.0.0` bereits in `llm-koog`
  adoptiert, `jackson-databind:2.21.3` = Sibling-Version → `ADR-0002`-Guard greift
  nicht, kein Folge-ADR, §9 F-5); `kover { … minBound(90) }` identisch zum Sibling —
  **nicht** gesenkt (Hard Rule 3.6, `ADR-0006`).
- geprüft, ohne Befund: `.a-check.yml` — Rolle `outbound_llm_action_koog` (Layer-Glob),
  Kanten `→ application`/`→ domain`, Resolution-Root
  `adapters/outbound/llm-action-koog/src/main/kotlin/dev/beliefagent`; **keine**
  Adapter→Adapter-Kante (`ARC-08`), Kern bleibt providerfrei (`ADR-0001/0003`).
- geprüft, ohne Befund: `Dockerfile` — Modul an **allen** Enumerationsstellen
  (`COPY` build.gradle.kts, `:dependencies`, `:test`, `:koverLog`, `:koverVerify`)
  ergänzt; symmetrisch zum LangChain4j-Sibling.
- geprüft, ohne Befund: `settings.gradle.kts` — `include("adapters:outbound:llm-action-koog")`
  registriert (mit `ARC-08`/slice-043-Kommentar).
- geprüft, ohne Befund: `.d-check.yml` — `ids.scope.roots` um `docs/user` erweitert;
  0 verbleibende `slice-NNN`-Referenzen in `docs/user` (d-check grün) — nur der
  MR-013-Zählwert weicht ab (F-2).
- geprüft, ohne Befund: `docs/user/integration.md` + `cli-entscheidungsnachweis.md` —
  beide Aktionsvorschlags-Pfade symmetrisch dokumentiert, „nur Vorschlag, keine
  Freigabe/Ausführung", kein CLI-Default, keine Live-Tests/Secrets; 4 neue
  `LH-*`-Links korrekt.
- geprüft, ohne Befund: `harness/conventions.md` — MR-013 als versionierter Zusatz
  (kein Überschreiben bestehender MR-Einträge, Hard Rule 3.5-konform); inhaltlich
  schlüssig **außer** dem Zählwert „8 entfernt" (F-2).
- keine Suppression (`@Suppress`/`noqa`/`nolint`/`d-check:ignore`) im neuen Modul
  (Hard Rule 3.2).

## Summary

| Kategorie | Anzahl |
|---|---|
| HIGH | 0 |
| MEDIUM | 1 |
| LOW | 1 |
| INFO | 3 |

## Verdikt

**Merge-blockierend (Code-Review):** ja — **1 MEDIUM** (F-1): die drei ungetesteten
Ablehnungsäste (`isAssignableFrom`, No-arg-Ctor, `newInstance`) der neuen reflektiven
Fabrik sind vor Merge zu schließen oder als bewusst ungetestet zu begründen
(Reklassifizierung/Behebung entscheidet Implementation/Architect — Reviewer
kategorisiert nur). Kein Safety-Loch: alle Fehlerpfade sind fail-closed. F-2 (LOW)
ist eine Zahl-Korrektur in MR-013 (nice-to-fix). F-3/F-4/F-5 (INFO) sind Verweise auf
Folgeslice/Safety-Durchgang/Commit.

**Vorbehalt (Durchgang):** „review-vollständig" ist der Slice erst mit dem
**Code-Safety-Review** (F-4) — für einen adversarialen LLM-Parser an der
Aktions-Grenze im Safety/Control-Repo ist der eigene fail-closed-Durchgang
Skill-Pflicht (etablierte Praxis `slice-035`..`042`); dieser Report ersetzt ihn nicht.

**Übergabe:** Findings gehen an die Implementation (Rückkante Review → Plan bei
Plan-/Doku-Defekt — F-2 kann als MR-013-Korrektur zurücklaufen). Der Report ersetzt
**keine** Verifikation: DoD-/Fehlerklassen-Matrix und `make gates` prüft der
**Verifier** separat (Modul 11, `docs/verifications/*`) — bewusst **nicht** in diesem
Reviewer-Lauf ausgeführt (Rollentrennung Modul 8).
