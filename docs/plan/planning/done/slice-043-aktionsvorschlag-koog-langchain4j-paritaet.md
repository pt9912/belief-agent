# Slice slice-043: Aktionsvorschlag Koog/LangChain4j-Paritaet

**Status:** done (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung.

**Bezug:** [`LH-FA-LLM-001`](../../../../spec/lastenheft.md#lh-fa-llm-001--sprachmodell-als-austauschbares-modul), [`LH-FA-LLM-002`](../../../../spec/lastenheft.md#lh-fa-llm-002--abgegrenzte-modell-aufgaben), [`LH-FA-LLM-003`](../../../../spec/lastenheft.md#lh-fa-llm-003--externalisierung-der-modell-konfidenz),
[`LH-FA-LLM-004`](../../../../spec/lastenheft.md#lh-fa-llm-004--anbieter-austauschbarkeit), [`LH-FA-ACT-001`](../../../../spec/lastenheft.md#lh-fa-act-001--vier-wirkungsklassen), [`LH-FA-ACT-002`](../../../../spec/lastenheft.md#lh-fa-act-002--einstufung-nach-seiteneffekt-reichweite), [`LH-FA-ACT-003`](../../../../spec/lastenheft.md#lh-fa-act-003--erfolgswahrscheinlichkeit-je-aktion),
[`LH-FA-ACT-004`](../../../../spec/lastenheft.md#lh-fa-act-004--rückverfolgbarkeit-aktion--evidenz), [`LH-FA-POL-006`](../../../../spec/lastenheft.md#lh-fa-pol-006--nicht-umgehbares-gate), [`LH-QA-02`](../../../../spec/lastenheft.md#lh-qa-02--konservatives-standardverhalten-fail-safe), [`LH-QA-03`](../../../../spec/lastenheft.md#lh-qa-03--testbarkeit), [`LH-QA-04`](../../../../spec/lastenheft.md#lh-qa-04--erweiterbarkeit);
[`ADR-0001`](../../adr/0001-hexagonal-llm-port.md), [`ADR-0002`](../../adr/0002-implementierungssprache-jvm-java.md), [`ADR-0003`](../../adr/0003-hexslice-architektur.md), [`ADR-0006`](../../adr/0006-coverage-gate-scope.md), `ARC-07`, `ARC-08`, `ARC-09`.

**Autor:** Codex. **Datum:** 2026-07-08.

---

## 1. Ziel

Nach dem ersten echten `AktionsVorschlagsPort`-Provider-Adapter aus
`slice-042` wird der zweite Framework-Pfad ergaenzt, sodass Koog und LangChain4j
fuer Aktionsvorschlaege dieselbe strukturierte Modellaufgabe, dasselbe
fail-closed Schema und dieselben lokalen Contract-Tests tragen.

## 2. Definition of Done

- [ ] Das nach `slice-042` fehlende Adaptermodul
  `adapters/outbound/llm-action-koog` (JVM, `src/main/kotlin`) implementiert
  `AktionsVorschlagsPort` hinter `ARC-08`; Framework **Koog**
  (`ai.koog:koog-agents:1.0.0`, bereits adoptiert in
  `llm-koog/build.gradle.kts:17` → keine neue Toolchain-Flaeche, kein
  Folge-ADR; §9 F-5). `hexagon:*` bleibt frei von Provider-/Framework-Imports,
  beide Framework-Pfade bleiben austauschbar ([`LH-FA-LLM-001`](../../../../spec/lastenheft.md#lh-fa-llm-001--sprachmodell-als-austauschbares-modul), [`LH-FA-LLM-004`](../../../../spec/lastenheft.md#lh-fa-llm-004--anbieter-austauschbarkeit)).
- [ ] Beide Adapter nutzen denselben fachlichen Response-Contract fuer
  Aktionsvorschlaege: `beschreibung`, `hypotheseId`, `wirkungsklasse`,
  `pSuccess`, `konfidenzReferenz`, `stuetzendeEvidenz`. Unterschiede in
  Framework-Runnern duerfen nicht zu unterschiedlichen fachlichen Akzeptanz-
  oder Fehlerregeln fuehren. Paritaet entsteht durch **bewusste Duplikation**
  je Framework-Pfad (Repo-Praezedenz `llm-koog`/`llm-langchain4j` fuer
  `LlmPort`: je eigener Parser/Prompt-Factory, **kein** geteiltes
  Produktivmodul) und wird ueber die gemeinsame Contract-Test-Matrix belegt —
  nicht ueber geteilten Produktivcode, der `ARC-08` (Adapter→Adapter) oder den
  Core ([`ADR-0001`](../../adr/0001-hexagonal-llm-port.md)/[`ADR-0003`](../../adr/0003-hexslice-architektur.md)) verletzen wuerde (§9 F-2).
- [ ] **Wire-/Parser-Paritaet (Adapter-Ebene):** Contract-Tests belegen fuer
  **beide** Runner dieselbe Fehlerklassen-Aequivalenz — gueltige Antwort;
  leere Antwort → `emptyList()`; kaputtes/unparsebares JSON → sichtbarer Wurf;
  **Tokens hinter dem ersten JSON-Wert → sichtbarer Wurf** (slice-042
  SR-F1-Haertung, nicht „kein Vorschlag“); doppelte Felder →
  Gesamtantwort-Defekt/Wurf; unbekannte/fehlende Felder, falscher Typ/Shape und
  nicht-endliche Zahlen → per-Vorschlag fail-closed verworfen. Der Koog-Parser
  darf die schwaechere `readTree(raw.trim())`-Vorlage (`KoogLlmPort.kt:230-231`)
  **nicht** spiegeln; der Trailing-Token-Guard (`nextToken()==null`) ist Pflicht
  ([`LH-QA-02`](../../../../spec/lastenheft.md#lh-qa-02--konservatives-standardverhalten-fail-safe), [`LH-QA-03`](../../../../spec/lastenheft.md#lh-qa-03--testbarkeit); §9 F-1/F-3).
- [ ] **Semantik bleibt im Use Case (keine Adapter-Duplikation):** unbekannte
  Hypothese, ungueltige Wirkungsklasse, fehlende Evidenz, ungueltiges
  `pSuccess` und fehlende/leere Konfidenzreferenz validiert `AktionsVorschlagen`
  (`:59/63/68/71`, Dedup `:48-50`) — **nicht** der Adapter. „Fehlende Evidenz“
  ist am Adapter nicht pruefbar (`vorschlaege(belief)` traegt keinen
  Evidenz-Kontext; `bekannteEvidenz` lebt im `AktionsVorschlagenBefehl`, §8
  haelt den Port unveraendert); beide Adapter reichen semantisch offene Rohwerte
  durch (wie `LangChain4jAktionsVorschlagsPortTest.kt:167-182`; §9 F-1).
- [ ] Build-/Arch-/Coverage-Integration fuer den neuen Koog-Pfad ist
  vollstaendig: `settings.gradle.kts` (`adapters:outbound:llm-action-koog`),
  `.a-check.yml` (Rolle `outbound_llm_action_koog`, Root
  `adapters/outbound/llm-action-koog/src/main/kotlin/dev/beliefagent`, Kanten
  `→ application`/`→ domain`), `Dockerfile` (Dependency-/Test-/Coverage-Stages),
  Modul-`build.gradle.kts` und Kover-Gate. Der symmetrische, core-freie
  Zweit-Adapter belegt Erweiterbarkeit ohne Kernaenderung ([`LH-QA-04`](../../../../spec/lastenheft.md#lh-qa-04--erweiterbarkeit),
  [`ADR-0003`](../../adr/0003-hexslice-architektur.md), [`ADR-0006`](../../adr/0006-coverage-gate-scope.md); §9 F-4/PR-F3).
- [ ] Integrationsdoku beschreibt beide Aktionsvorschlags-Providerpfade
  symmetrisch und nennt klar: keine CLI-Default-Umbindung, keine Live-Provider-
  Tests, keine Secrets in Doku/Tests, keine Gate-/Approval-/Executor-Aenderung.
- [ ] `make doc-check` und `make gates` sind gruen; Closure-Notiz benennt den
  Paritaets-Contract und verbleibende Provider-spezifische Unterschiede.

## 3. Plan (vor Code)

| Datei / Komponente | Aenderungs-Art | Begruendung |
|---|---|---|
| `adapters/outbound/llm-action-koog` (JVM, `src/main/kotlin`) | neu | Fehlenden **Koog**-Pfad hinter `AktionsVorschlagsPort` ergaenzen: eigene Prompt-Factory, Runner/Fabrik, strikter JSON-Parser (mit Trailing-Token-Guard wie slice-042), Response-Mapping zu `AktionsVorschlag`. |
| bestehender `llm-action-langchain4j`-Adapter (`slice-042`) | keine Produktivaenderung | **Soll-Referenz** fuer Paritaet; **kein** geteiltes Produktivmodul (bewusste Duplikation, §9 F-2). Nur die Contract-Test-Matrix wird ggf. gespiegelt. |
| `.../llm-action-koog/src/test/kotlin/**` (ggf. Matrix in `llm-action-langchain4j` gespiegelt) | neu | Wire-/Parser-Contract-Matrix je Runner; **keine** Use-Case-Semantik am Adapter (§9 F-1). |
| `settings.gradle.kts` | update | `adapters:outbound:llm-action-koog` registrieren. |
| `.a-check.yml` | update | Rolle `outbound_llm_action_koog`, Root, Kanten `→ application`/`→ domain`; Core bleibt providerfrei. |
| `Dockerfile` | update | Dependency-, Test- und Coverage-Gate-Stages um `llm-action-koog` ergaenzen. |
| `docs/user/integration.md` | update | Koog/LangChain4j-Paritaet fuer Aktionsvorschlaege dokumentieren. |
| `docs/reviews/*slice-043*` | neu | Review-Artefakt mit Fokus Paritaet und Framework-Drift. |
| `docs/verifications/*slice-043*` | neu | Verification-Artefakt fuer Contract-Matrix und Gates. |

## 4. Trigger

`slice-042` liegt in `done/` und hat genau einen echten
Aktionsvorschlags-Providerpfad (LangChain4j) geliefert. Kein Slice liegt in
`in-progress/` (WIP-Limit 1). Der fehlende Pfad ist durch den Repo-Zustand
eindeutig **Koog**: `llm-action-koog` fehlt in `settings.gradle.kts`,
`.a-check.yml` und `Dockerfile` (Architect-Entscheidung, §9 F-4). Dieser Slice
implementiert nur diesen Koog-Pfad plus Paritaetsverifikation. Da
`ai.koog:koog-agents` bereits adoptiert ist (`llm-koog/build.gradle.kts:17`),
greift der [`ADR-0002`](../../adr/0002-implementierungssprache-jvm-java.md)-Guard nicht; nur ein **nicht** adoptiertes Framework
verlangte einen Folge-ADR vor Code (§9 F-5).

## 5. Closure-Trigger

DoD vollstaendig + Review/Verification abgeschlossen + `make gates` gruen +
Closure-Notiz geschrieben + Slice nach `done/` verschoben.

## 6. Risiken und offene Punkte

- Paritaet darf nicht durch Kopieren unscharfer Parser entstehen. Insbesondere
  darf der Koog-Parser **nicht** die schwaechere `readTree(raw.trim())`-Vorlage
  aus `KoogLlmPort.kt:230-231` spiegeln, sonst kehrt die in slice-042 (SR-F1)
  geschlossene Trailing-Token-Nachsicht zurueck; der Trailing-Token-Guard
  (`nextToken()==null`) ist Paritaets-Pflicht (§9 F-3).
- **Schicht-Besitz entschieden (§9 F-2):** Paritaet entsteht durch bewusste
  Duplikation je Framework-Pfad (Repo-Praezedenz `llm-koog`/`llm-langchain4j`),
  **nicht** durch ein geteiltes Produktivmodul. So entsteht keine
  Adapter→Adapter-Kante (`ARC-08`) und keine Framework-Dep im Core
  ([`ADR-0001`](../../adr/0001-hexagonal-llm-port.md)/[`ADR-0003`](../../adr/0003-hexslice-architektur.md)); Paritaet wird ueber die geteilte
  Contract-Test-Matrix belegt, nicht ueber geteilten Produktivcode.
- **Schicht-Besitz der Semantik (§9 F-1):** Der Adapter prueft nur Wire-/
  Deserialisierungs-Integritaet; unbekannte Hypothese, Wirkungsklasse,
  Evidenz-Aufloesung und Konfidenz-Bereich bleiben im Use Case
  `AktionsVorschlagen`. „Fehlende Evidenz“ ist am Adapter nicht pruefbar (Port
  ohne Evidenz-Kontext).
- Provider-/Modellwahl, API-Keys, Live-Netztests, CLI-Default-Binding,
  Approval und Aktionsausfuehrung bleiben ausserhalb dieses Slice.
- [`ADR-0002`](../../adr/0002-implementierungssprache-jvm-java.md)-Guard (§9 F-5): `ai.koog:koog-agents` ist bereits adoptiert
  (`llm-koog/build.gradle.kts:17`) → kein Folge-ADR. Nur ein **nicht**
  adoptiertes Framework verlangte einen Folge-ADR vor Code.
- `slice-042` hat den Port-Vertrag **nicht** geaendert
  (`AktionsVorschlagsPort.kt:14` unveraendert) → kein
  Contract-Reconciliation-Slice noetig (Design-Review-Negativbefund bestaetigt).

## 7. Closure-Notiz (nach `done/`)

Abgeschlossen am 2026-07-10. Implementiert wurde der zweite echte (nicht-Fake)
Framework-Pfad hinter `AktionsVorschlagsPort`
(`adapters/outbound/llm-action-koog`, `KoogAktionsVorschlagsPort`), symmetrisch zum
LangChain4j-Adapter aus `slice-042`. Beide liefern ausschließlich **rohe**
`AktionsVorschlag`-Werte, prüfen nur Wire-/Deserialisierungs-Integrität (exakt 6
Felder, Typ/Shape, endliche Zahlen) und überlassen die Semantik dem Use Case
`AktionsVorschlagen`. Kein Gate/Executor, keine Freigabe, kein CLI-Default-Binding,
keine Secrets (Stub-Runner in Tests).

**Paritäts-Contract.** Gleicher 6-Feld-Response-Contract (`beschreibung`,
`hypotheseId`, `wirkungsklasse`, `pSuccess`, `konfidenzReferenz`,
`stuetzendeEvidenz`) und gleiche Fehlerklassen-Äquivalenz über **beide** Runner
(gültig → Vorschläge; leer/`[]` → `emptyList()`; unparsebar/Nicht-Array/
**Trailing-Tokens**/doppelte Felder/Provider-Ausfall → sichtbarer Wurf; unbekannte/
fehlende/falsch-getypte/nicht-endliche Felder → per-Vorschlag fail-closed
verworfen). Parität entsteht durch **bewusste Duplikation** je Framework-Pfad
(eigener strikter Parser + Prompt-Factory), **nicht** durch ein geteiltes
Produktivmodul — keine `ARC-08`-Adapter→Adapter-Kante, kein Framework im Core
(`ADR-0001`/`ADR-0003`); belegt über die geteilte Contract-Test-Matrix.

**Verbleibende provider-spezifische Unterschiede.** Koog bindet über
`LLMClient`/`PromptExecutor` plus die reflektive `fromLlmClient(clientClass)`-Fläche,
LangChain4j über `ChatModel` — beide hinter demselben Port, ohne fachliche
Divergenz. Residuen (Folgearbeit): roher Transport-Fehlertyp nicht adaptergehüllt
(SR-F3, 3. Auftreten → Steering-Loop, Entscheidung **als INFO belassen**),
Jackson-`StreamReadConstraints`-DoS-Grenzen nur über Defaults (SR-F2), reflektiver
Static-Init vor Typ-Check (SR-F1, deployment-config-begrenzt).

**Lerneintrag.** (1) Parität muss zur **gehärteten** Soll-Referenz gezogen werden,
nicht zum älteren gleich-Framework-Code: Der Koog-Parser übernahm den
Trailing-Token-Guard (`nextToken()==null`) aus `llm-action-langchain4j` (slice-042
SR-F1) und spiegelte **nicht** die schwächere `readTree(raw.trim())`-Vorlage aus
`KoogLlmPort.kt` — sonst wäre die in slice-042 geschlossene Trailing-Token-Nachsicht
zurückgekehrt. (2) Rollentrennung (Modul 8) trägt erneut: der unabhängige
Frischkontext-Code-Review fand mit F-1 (MEDIUM) eine **neue öffentliche Fläche**
außerhalb der slice-042-Contract-Matrix — die reflektive `fromLlmClient(clientClass)`-
Fabrik hatte 3 von 4 Ablehnungsästen ungetestet; geschlossen mit
`KoogFabrikAblehnungsStubs.kt` + vier Negativtests. „Contract-Parität" heißt **nicht**
„identische Fabrik-Fläche".

**Nachweis.** Review-/Verification-Artefakte: `2026-07-10-slice-043-code-review.md`,
`2026-07-10-slice-043-code-safety-review.md`, `2026-07-10-slice-043-verification.md`
(plus Plan-/Design-Reviews gleichen Datums). Ausgeführte Sensoren: `make gates`
(EXIT 0: doc-check · build · test 27 Tests · coverage-gate 90 %-Floor · arch-check).

## 8. Sub-Area-Modus-Begründung

### Sub-Area: `adapters/outbound/llm-action-*`

- **Modus:** Hybrid
- **Konventionen-Dichte:** hoch fuer Outbound-Adapter nach `ARC-08` und
  Build-/Coverage-Einbindung nach [`ADR-0003`](../../adr/0003-hexslice-architektur.md)/[`ADR-0006`](../../adr/0006-coverage-gate-scope.md); mittel fuer
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

## 9. Design-/Review-Klärung (Rückkante Review → Plan, Modul 8)

Übergabe-Artefakt für die Findings aus dem Design-Review
`docs/reviews/2026-07-10-slice-043-design-review.md` (DR: 3× MEDIUM, 1× LOW,
1× INFO) und dem Plan-Review
`docs/reviews/2026-07-10-slice-043-plan-review.md` (PR: 1× MEDIUM, 2× LOW,
2× INFO) — beide merge-blockierend. Beide Läufe überlappen thematisch; die IDs
sind zusammengeführt (`§9 F-n` folgt der Design-Review-Nummer). Entscheidung und
Umsetzung liegen bei Architect/Planner (Modul 8); der Reviewer kategorisiert nur.
Vor Implementierungsstart abzuschließen.

| Finding | Kat. | Entscheidung | Verankert in |
|---|---|---|---|
| F-1 · DR-F1/PR-F1 — Contract-Matrix mischt Wire + Semantik | MEDIUM | **Schicht-Trennung wie slice-042 §9 F-1:** Der **Adapter** testet nur Wire-/Parser-Parität (gültige/leere/kaputte/trailing/doppelte/unbekannte/fehlende Felder, Typ/Shape, nicht-endliche Zahlen). Die fünf **semantischen** Fälle (unbekannte Hypothese, Wirkungsklasse, fehlende Evidenz, `pSuccess`, Konfidenzreferenz) bleiben im Use Case `AktionsVorschlagen` (`:59/63/68/71`, Dedup `:48-50`) und werden **nicht** am Adapter dupliziert. „Fehlende Evidenz“ ist am Adapter unmöglich (`vorschlaege(belief)` ohne Evidenz-Kontext; Port unverändert). „Fehlende Konfidenzreferenz“ ist schicht-mehrdeutig: fehlendes Feld → Wire-Verwurf; leere/dupl. Referenz → Use-Case-Dedup. | §2 (DoD Wire/Semantik), §3, §6, §8 |
| F-2 · DR-F2/PR-F5 — Besitz des gemeinsamen Parser-/Contract-Codes | MEDIUM | **Bewusste Duplikation je Framework-Pfad** (Repo-Präzedenz `llm-koog`/`llm-langchain4j` für `LlmPort`: je eigener Parser/Prompt, kein geteiltes Modul). **Kein** neues Shared-Produktivmodul → keine Adapter→Adapter-Kante (`ARC-08`), keine Framework-Dep im Core ([`ADR-0001`](../../adr/0001-hexagonal-llm-port.md)/[`ADR-0003`](../../adr/0003-hexslice-architektur.md)). Parität wird über die gemeinsame Contract-Test-Matrix belegt, nicht über geteilten Produktivcode. | §2 (DoD Contract), §3, §6 |
| F-3 · DR-F3 — fail-closed-Parität nur grob; Härtungs-Regression möglich | MEDIUM | Trailing-Token-Fall (`[gültig]{…}` → sichtbarer Wurf, slice-042 SR-F1) und Klassen-Trennung (leere Antwort → `emptyList()` vs. Provider-Ausfall/unparsebar → Wurf) werden als Paritäts-Assertion über **beide** Runner gepinnt. Der Koog-Adapter darf die schwächere `readTree(raw.trim())`-Vorlage (`KoogLlmPort.kt:230-231`) **nicht** spiegeln; Trailing-Token-Guard (`nextToken()==null`) ist Pflicht. | §2 (DoD Wire), §6 |
| F-4 · DR-F4/PR-F2 — disjunktiver Modulname / `.a-check`-Rolle | LOW | Konvergiert (Repo-Zustand eindeutig): Modul `adapters/outbound/llm-action-koog`, Framework **Koog**; `.a-check`-Rolle `outbound_llm_action_koog`, Root `…/src/main/kotlin/dev/beliefagent`, Kanten `→ application`/`→ domain`; `settings.gradle.kts`/`Dockerfile` entsprechend. Nicht mehr disjunktiv. | §2 (DoD 1/Build), §3, §4 |
| PR-F3 — [`LH-QA-04`](../../../../spec/lastenheft.md#lh-qa-04--erweiterbarkeit) nicht in DoD-Zeile gemappt | LOW | [`LH-QA-04`](../../../../spec/lastenheft.md#lh-qa-04--erweiterbarkeit) (Erweiterbarkeit ohne Kernänderung, Soll) an die Build-/Arch-/Coverage-DoD gebunden: der symmetrische, core-freie Zweit-Adapter **ist** der Erweiterungsnachweis. | §2 (DoD Build) |
| F-5 · DR-F5/PR-F4 — [`ADR-0002`](../../adr/0002-implementierungssprache-jvm-java.md)-Guard + Bezug | INFO | [`ADR-0002`](../../adr/0002-implementierungssprache-jvm-java.md) in **Bezug** ergänzt. `ai.koog:koog-agents:1.0.0` bereits adoptiert (`llm-koog/build.gradle.kts:17`) → keine neue Toolchain-Fläche, kein Folge-ADR (konsistent zu slice-042 §9 DR-F3). Nur ein **nicht** adoptiertes Framework griffe den Guard. | Kopf **Bezug**, §2 (DoD 1), §4, §6 |
