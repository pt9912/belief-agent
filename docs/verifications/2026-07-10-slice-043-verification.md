# Verification-Report: slice-043 (Koog-Aktionsvorschlag-Adapter, Koog/LangChain4j-Parität) — 2026-07-10

**Verification-Art:** DoD-/Spec-Verifikation gegen Slice-Plan, Code-/Doku-Artefakte
und Sensoren (Modul 11). Prüft *Code gegen DoD/Spec/Plan* — nicht „ist es gut?"
(Review, Modul 10). Eigene Rolle, eigener Eingangs-Kontext (Modul 8).

**Gegenstand:** `docs/plan/planning/in-progress/slice-043-aktionsvorschlag-koog-langchain4j-paritaet.md`

**Skill/Regelwerk:** `.harness/baseline/v1.4.0/regelwerk/modul-01-entwicklungszyklus.md`,
`…/modul-08-agentenrollen.md`, `…/modul-11-verification.md` @ v1.4.0.

**Modell:** Claude Opus 4.8 (1M) · **Datum:** 2026-07-10

**Rollentrennung (Modul 8):** Der Verifier hat weder implementiert (Autor Codex)
noch den Code reviewt (`docs/reviews/2026-07-10-slice-043-{plan,design,code,code-safety}-review.md`);
frischer Kontext, andere Eingabe (DoD/Spec/Plan statt Plan/ADR).

**Prüfstand:** **Committeter** Stand — `HEAD = 5987980` (`docs(reviews)`), davor
`5d1fe0e` (MR-013 ids-Scope) und `a862ac1` (`feat(action)` slice-043). Arbeitsbaum
**sauber** (`git status`: nichts zu committen). Die Docker-Gates bauen genau diesen
committeten Stand (inhaltsbasierter COPY-Cache = committeter Quellstand); slice-043
liegt planmäßig noch in `in-progress/` (Closure ausstehend, Handoff unten).

---

## Eingangs-Kontext

- `spec/lastenheft.md`: `LH-FA-LLM-001/002/003/004`, `LH-FA-ACT-001..004`, `LH-FA-POL-006`, `LH-QA-02/03/04`
- `spec/architecture.md`: `ARC-07`, `ARC-08`, `ARC-09`
- `docs/plan/adr/0001…`, `0002…`, `0003…`, `0006-coverage-gate-scope.md`
- `docs/reviews/2026-07-10-slice-043-{plan,design,code,code-safety}-review.md`
- `adapters/outbound/llm-action-koog/**` (Impl. + 3 Test-Dateien + `build.gradle.kts`)
- `adapters/outbound/llm-action-langchain4j/**` (slice-042, **Soll-Referenz** Parität, unverändert)
- `hexagon/application/.../aktionsvorschlag/ports/AktionsVorschlagsPort.kt` (Vertrag, unverändert)
- `hexagon/application/.../aktionsvorschlag/AktionsVorschlagen.kt` (Use-Case-Semantikrand, unverändert)
- `hexagon/application/.../aktionsvorschlag/dto/AktionsVorschlag.kt` (6-Feld-Contract)
- `settings.gradle.kts`, `.a-check.yml`, `Dockerfile`, `docs/user/integration.md`
- Das laut Plan **nicht** zu spiegelnde Muster `KoogLlmPort.kt:230-231` (`readTree(raw.trim())`)

---

## Verification (DoD §2 gegen Code + Sensoren)

| DoD | Evidenz (verifiziert) | Status |
|---|---|---|
| **DoD 1** — Modul `llm-action-koog` (JVM `src/main/kotlin`) implementiert `AktionsVorschlagsPort` hinter `ARC-08`; Framework **Koog** (bereits adoptiert → kein Folge-ADR); `hexagon:*` provider-/framework-frei, beide Pfade austauschbar. | `class KoogAktionsVorschlagsPort(...) : AktionsVorschlagsPort` (`:50-54`), `override fun vorschlaege(belief): List<AktionsVorschlag>` (`:137-142`). `build.gradle.kts:20` `ai.koog:koog-agents:1.0.0` (via `llm-koog` adoptiert) + `jackson-databind:2.21.3` = Sibling-Version → `ADR-0002`-Guard greift nicht (§9 F-5). **arch-check live: 0 Befunde** → keine Kante Kern→Adapter, kein Framework-Import in `hexagon/*`; LangChain4j-Pfad unverändert daneben. | **erfüllt** |
| **DoD 2** — Gleicher 6-Feld-Response-Contract; Parität durch **bewusste Duplikation** je Pfad (eigener Parser/Prompt, **kein** geteiltes Produktivmodul); belegt über gemeinsame Contract-Test-Matrix, nicht geteilten Produktivcode (kein `ARC-08` Adapter→Adapter, kein Core-Leak). | DTO `AktionsVorschlag` = exakt `beschreibung/hypotheseId/wirkungsklasse/pSuccess/konfidenzReferenz/stuetzendeEvidenz`. Koog-`StrictAktionsVorschlagParser`/`AktionsVorschlagPromptFactory` sind eigenständige Klassen im Koog-Modul (Diff-Vergleich: parser-/prompt-identisch zum LangChain4j-Sibling, **kein** gemeinsames Modul importiert). `.a-check.yml`: **keine** Kante `outbound_llm_action_koog → outbound_llm_action_langchain4j`; arch-check 0 Befunde. | **erfüllt** |
| **DoD 3** — Wire-/Parser-Parität über **beide** Runner: gültig; leer→`emptyList`; kaputt→Wurf; **Trailing-Token→Wurf**; doppelte Felder→Wurf; unbekannt/fehlend/Typ/Shape/nicht-endlich→per-Vorschlag fail-closed verworfen. Koog-Parser **nicht** die schwächere `readTree(raw.trim())`-Vorlage; Trailing-Token-Guard `nextToken()==null` Pflicht. | Koog `parseJson` (`:232-243`): `objectMapper.createParser(raw.trim())` → `readTree` → **`if (parser.nextToken() != null) throw`** (`:239-241`); `STRICT_DUPLICATE_DETECTION` (`:217-221`). `wireDefekt` (`:270-284`) läuft **vor** jeder Koerzion. Test-Matrix spiegelt LangChain4j-Sibling 1:1 (`trailing_tokens_nach_json_wert_werfen`, `trailing_muell_nach_leerem_array_wirft`, `doppelte_json_felder_werfen`, `falscher_typ`/`nicht_endliche_zahl`/`fehlendes_pflichtfeld`/`nicht_objekt`/`evidenz_mit_nicht_string`/`json_null` → verworfen). **`make test` live grün** (koog-Target frisch ausgeführt). | **erfüllt** |
| **DoD 4** — Semantik bleibt im Use Case (keine Adapter-Duplikation): unbekannte Hypothese, Wirkungsklasse, fehlende Evidenz, `pSuccess`, Konfidenzreferenz validiert `AktionsVorschlagen`; Adapter reicht semantisch offene Rohwerte durch. | `AktionsVorschlagen.gateFaehigOderNull`: unbekannte Hypothese `:59`, `beschreibung.isNotBlank` `:62`, `Wirkungsklasse.valueOf` `:63`, Evidenz-Auflösung/`isNotEmpty` `:65-68`, Konfidenz via `KonfidenzExternalisieren` `:70-78`, Referenz-Dedup `:48-50` — **unverändert, nicht im Diff**. Port `vorschlaege(belief)` trägt **keinen** Evidenz-Kontext (`bekannteEvidenz` lebt im `AktionsVorschlagenBefehl:22`) → Adapter *kann* Evidenz nicht prüfen. Koog-Test `reicht_semantisch_offene_rohwerte_an_den_use_case_durch` (`pSuccess=5.0`, unbek. Hypothese, „QUATSCH", leere Evidenz **durchgereicht**) belegt Nicht-Duplizierung. | **erfüllt** |
| **DoD 5** — Build-/Arch-/Coverage-Integration vollständig (`settings`, `.a-check.yml`, `Dockerfile`, Modul-`build.gradle.kts`, Kover); symmetrischer core-freier Zweit-Adapter belegt Erweiterbarkeit (`LH-QA-04`). | `settings.gradle.kts:31` `include(...llm-action-koog)`. `.a-check.yml`: Rolle `outbound_llm_action_koog` (:38), Kanten →application/→domain (:74-75), Root `…/src/main/kotlin/dev/beliefagent` (:126). `Dockerfile` an **allen 5** Stellen (`COPY` build.gradle.kts, `:dependencies`, `:test`, `:koverLog`, `:koverVerify`). `build.gradle.kts:27-37` `kover{ verify{ rule{ minBound(90) } } }` = identisch zum Sibling (**nicht** gesenkt). **`coverage-gate` frisch (`--no-cache-filter`, `--max-workers=1`) grün**: `:llm-action-koog:test → koverGenerateArtifact → koverVerify` frisch ausgeführt, keine Schwellen-Verletzung → Modul-Floor **≥90 %** gehalten. | **erfüllt** |
| **DoD 6** — Doku beschreibt beide Pfade symmetrisch, nennt: kein CLI-Default-Rebinding, keine Live-Provider-Tests, keine Secrets in Doku/Tests, keine Gate-/Approval-/Executor-Änderung. | `integration.md` (Diff): Tabelle listet `KoogAktionsVorschlagsPort.fromLlmClient/fromPromptExecutor` symmetrisch zum LangChain4j-Eintrag; Abschnitt „Aktionsvorschlags-Adapter (Koog/LangChain4j-Parität)" nennt explizit „**keine** Live-Provider-Tests und **keine** Secrets in Doku oder Tests; Gate, Approval und Ausführung bleiben unverändert" und „Kein Adapter … ist CLI-Default". **Kein Produktiv-Binding:** `.a-check.yml` bindet `inbound_cli` nur an `outbound_llm_action_fake`, **nicht** an koog/langchain4j. Tests nutzen Stub-Runner (kein Netz/Key). `doc-check` grün. | **erfüllt** |
| **DoD 7** — `make doc-check` + `make gates` grün; Closure-Notiz benennt Paritäts-Contract + Rest-Unterschiede. | **`make gates` EXIT 0** (doc-check · build · test · coverage-gate · arch-check). `doc-check` (d-check): 168 Dateien, 0 Befunde. Closure-Notiz (§7) noch Platzhalter → **siehe Handoff** (planmäßige Sequenz: erst Verifikation, dann Closure-Notiz, dann `done/`). | **erfüllt bis auf Closure-Notiz (Handoff)** |

## Rückbindung der Review-Findings (Modul 8/10 → verifiziert im committeten Code)

- **F-1 (Code-Review, MEDIUM — merge-blockierend)** Fabrik-Testlücke (3 von 4 Ablehnungsästen ungetestet): **im committeten Stand geschlossen** — `KoogFabrikAblehnungsStubs.kt` (3 Stubs) + 4 Ablehnungsast-Tests (`factory_weist_unbekannte_klasse`/`…_nicht_llm_client_klasse`/`…_klasse_ohne_no_arg_ctor`/`…_fehlschlagende_instanziierung`, `KoogAktionsVorschlagsPortTest.kt:287-340`). Alle Äste werfen sichtbar `IllegalArgumentException`. **verifiziert (im Test-Lauf grün).**
- **F-2 (Code-Review, LOW)** MR-013 „8 entfernt": **im committeten Stand korrigiert** — `harness/conventions.md` MR-013 sagt „**5 entfernt: 4× slice-041, 1× slice-042**"; `grep` bestätigt **0** verbleibende `slice-NNN`-Referenzen in `docs/user`; d-check grün. **verifiziert (Zahl = Realität).**
- **F-3 / SR-F3 (INFO, 3. Auftreten)** roher Transport-Typ (nicht adaptergehüllt): plan-konformes Residuum, fail-closed sichtbar (Test `provider_ausfall_propagiert_sichtbar`). **Steering-Loop-Schwelle erreicht → an Architect/Planner (Handoff).**
- **F-4 (INFO)** eigener Code-Safety-Durchgang: **erbracht** (`2026-07-10-slice-043-code-safety-review.md`, 0 HIGH/MEDIUM/LOW). **verifiziert.**
- **SR-F1 (INFO)** reflektiver Static-Init vor `isAssignableFrom`: latente Beobachtung, deployment-config-begrenzt, kein aktueller Angriffspfad; Type-Confusion korrekt geschützt (`newInstance() as LLMClient` erst nach `isAssignableFrom`). **als akzeptierte Grenze notiert.**
- **SR-F2 (INFO)** DoS-Grenzen über Jackson-Defaults (nicht gepinnt): fail-closed, identisch zum Sibling, akzeptiertes Residuum. **als Folgearbeit notiert.**

## Sensors (computational — selbst ausgeführt gegen den committeten Stand)

- `make gates` — **grün, EXIT 0** (doc-check · build · test · coverage-gate · arch-check).
- `doc-check` (d-check, **live**, `--network none`) — **168 Datei(en), 0 Befund(e)** → alle `LH-*`/`ADR-*`/`ARC-*`-Referenzen in Slice + Doku lösen auf.
- `arch-check` (a-check v0.11.0, **live**) — **`gesamt: 0 Befund(e)`**: Kern provider-/framework-frei, `llm-action-koog → application/domain` konform, keine Adapter→Adapter-Kante.
- `test` — **frisch (`--no-cache-filter test`, EXIT 0)**: `:adapters:outbound:llm-action-koog:test` real ausgeführt (`BUILD SUCCESSFUL in 51s`), Stub-Runner (kein Netz/API-Key).
- `coverage-gate` — **frisch (`--no-cache-filter coverage-gate`, `--max-workers=1`, EXIT 0)**: `:llm-action-koog:test → koverGenerateArtifact → koverVerify` frisch, `minBound(90)` gehalten (`BUILD SUCCESSFUL in 57s`).
- **Sensor-Hinweis (Transparenz, Modul 11):** Das reine **Reporting**-Target `make coverage` (`koverLog`, läuft **ohne** `--max-workers=1` voll-parallel über alle ~24 Testmodule) brach in einem frischen Lauf während des parallelen `compileTestKotlin` nach ~5 min ab (Ressourcen-Erschöpfung) — **kein Code-Defekt**: das kanonische Gate `coverage-gate` (`koverVerify`, `--max-workers=1`, Teil von `make gates`) läuft frisch grün, und `make test` inkl. koog-Target ist grün. Die exakte Zeilen-Prozentzahl je Modul (nur `koverLog`-Output) konnte daher nicht abgelesen werden; die Gate-Assertion `≥ minBound(90)` (ADR-0006) ist dennoch frisch bestätigt.

## Verdikt

**Keine DoD-Verletzung am Code.** `slice-043` ist gegen Plan, Spec (`LH-FA-LLM-*`,
`LH-FA-ACT-*`, `LH-FA-POL-006`, `LH-QA-02/03/04`) und Architektur (`ARC-07/08/09`,
`ADR-0001/0002/0003/0006`) verifiziert; alle sieben DoD-Punkte sind durch Code +
lokale Contract-Test-Matrix (paritätsäquivalent zum LangChain4j-Sibling, plus 6
Koog-Fabrik-Tests) + live/frische Sensoren belegt. Die Wire-/Parser-Parität ist
scharf (Trailing-Token-Guard `nextToken()==null` präsent, **nicht** die schwache
`KoogLlmPort`-Vorlage), die Schicht-Trennung (Adapter=Wire, Use Case=Semantik) ist
testbar durchgezogen, der Adapter erzeugt keine Freigabe, öffnet keinen
Executor-Pfad und ist nicht produktiv gebunden (`LH-FA-POL-006`). Beide
merge-blockierenden Review-Findings (F-1 MEDIUM, F-2 LOW) sind im **committeten**
Stand aufgelöst.

## Handoff an Planner/Implementation (Modul 8)

- **Offener Closure-Schritt (§5/§7):** Closure-Notiz (§7) noch Platzhalter — **keine
  DoD-Verletzung**, sondern planmäßige Sequenz (erst Verifikation, dann
  Closure-Notiz, dann `done/`). Sie soll den **Paritäts-Contract** benennen (gleicher
  6-Feld-Response-Contract, gleiche Fehlerklassen-Äquivalenz über beide Runner,
  bewusste Duplikation statt geteiltem Modul) und die verbleibenden
  provider-spezifischen Unterschiede (Koog `LLMClient`/`PromptExecutor` + reflektive
  `fromLlmClient(clientClass)`-Fläche vs. LangChain4j `ChatModel`).
- **Verschiebung `in-progress/ → done/`:** Nach Closure-Notiz. Da der verifizierte
  Stand bereits committet und der Arbeitsbaum sauber ist, hält ein erneuter
  `make gates`-Lauf nach der `git mv`-Closure (Working Tree == committeter Stand).
- **Steering-Loop-Signal (SR-F3, 3. Auftreten):** „roher Transport-Typ nicht
  adaptergehüllt" ist nun 3× aufgetreten (slice-042 F-4, slice-043 Code-Review F-3,
  Safety SR-F3). **Entscheidung Architect/Planner:** Folge-Slice (geordnete
  Konsumenten-Eskalation, „unreachable" typunterscheidbar) **oder** AGENTS.md-/ADR-Notiz.

## Verbleibende Risiken / Out-of-Scope

- Transport-Fehler-Typ-Unterscheidbarkeit + explizites Pinnen der Jackson-
  `StreamReadConstraints` (DoS-Grenzen): bewusstes Residuum (§6, F-3/SR-F2/SR-F3),
  fällig sobald ein Slice die Vorschläge-Konsumenten geordnet eskalieren lässt.
- Reflektiver `fromLlmClient(clientClass)`-Static-Init vor Typ-Check (SR-F1): latent,
  deployment-config-begrenzt; relevant erst, falls die Trust-Boundary kippt.
- Provider-/Modellwahl, API-Keys, Live-Netztests, CLI-Default-Binding, Approval und
  Aktionsausführung: außerhalb dieses Slice (Composition-Root/Folgeslice).
