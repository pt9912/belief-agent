# Review-Report: slice-043 Design-Review — 2026-07-10

**Review-Art:** Design — geprueft *wogegen*: Architektur (Layer-Grenzen,
Port-Vertrag, ADR-Vertraeglichkeit einer neuen Komponente), **bevor** die
Details festgezurrt sind (Modul 10 §Drei Review-Arten).

**Gegenstand:** `docs/plan/planning/open/slice-043-aktionsvorschlag-koog-langchain4j-paritaet.md`
(Loesungs-Schnitt: **zweiter** echter Outbound-Adapter hinter
`AktionsVorschlagsPort` — Paritaetspfad Koog neben dem bestehenden
LangChain4j-Adapter aus slice-042; `ARC-07`/`ARC-08`. Repo-Zustand:
`llm-action-koog` fehlt ueberall (settings/`.a-check`/Dockerfile) → der zu
ergaenzende Pfad ist **Koog**.)

**Skill:** `.harness/skills/reviewer.md` @ v1.0 · <!-- d-check:ignore -->
**Modell:** Claude Opus 4.8 (1M) · **Datum:** 2026-07-10

**Rollentrennung (Modul 8):** Unabhaengiger Frischkontext. Ich habe slice-043
**nicht** geplant (Autor: Codex) und fuehre **nicht** parallel den Plan-Review.
Reviewer kategorisiert; Entscheidung/Umsetzung bei Architect/Planner; keine
Verifikation (Modul 11).

**Eingangs-Kontext** (Skill §Kontext-Eingang; Safety-Pfad → Ist-Code mit
Zeilenankern real gelesen):

- Slice-Plan slice-043 (`open/`).
- `spec/architecture.md` §2 — `ARC-07` Port-Lokalitaet (`:104` „so lokal wie
  moeglich, so geteilt wie noetig"), `ARC-08` Outbound-Adapter (`:106`
  „importiert **nicht** fremden Adapter"), `ARC-03`/`ARC-09` Gate-/Executor-Grenze
  (`:111-135`).
- ADRs: `0001` (hexagonal, Kern adapterfrei), `0003` (HexSlice/Layer,
  `adapter→application→domain`), `0006` (per-Modul-Coverage), `0002`
  (Dependency am Rand). `docs/plan/adr/README.md` (Supersedes-Ketten:
  `0008`⊃`0005`; keine Kette beruehrt diesen Slice).
- `AGENTS.md §3` (Hard Rules 3.5 ADR-Immutabilitaet, 3.6 Gate-Lockern,
  3.7 Gate nicht umgehbar, 3.2 Suppression).
- **Ist-Code (Pflicht):**
  - `.../aktionsvorschlag/ports/AktionsVorschlagsPort.kt:14` — Port-Signatur
    `vorschlaege(belief: BeliefState): List<AktionsVorschlag>` (**kein**
    Evidenz-Kontext).
  - `.../aktionsvorschlag/dto/AktionsVorschlag.kt:11-18` — sechs primitive
    Rohwert-Felder.
  - `.../aktionsvorschlag/AktionsVorschlagen.kt` — Use-Case-Semantik:
    Port-Aufruf **ausserhalb** per-Vorschlag-`runCatching` (`:47`),
    Dedup-Filter konfidenzReferenz (`:48-50`), per-Vorschlag-`runCatching`
    (`:57`), unbekannte Hypothese (`:59`), Beschreibung nicht leer (`:62`),
    `Wirkungsklasse.valueOf` (`:63`), Evidenz-Aufloesung `getValue` +
    Nicht-Leere (`:65-68`), Konfidenz-Bereich via `externalisieren` (`:70-78`);
    `bekannteEvidenz` lebt im `AktionsVorschlagenBefehl` (`:22`), nicht am Port.
  - `adapters/outbound/llm-action-langchain4j/.../LangChain4jAktionsVorschlagsPort.kt`
    (slice-042-Referenz): Prompt-Factory (`:72-92`), `StrictAktionsVorschlagParser`
    (`:120-209`) mit gehaertetem Trailing-Token-Guard (`:150-152`) +
    `STRICT_DUPLICATE_DETECTION` (`:128-132`), Wire-only `wireDefekt` (`:181-195`);
    Test `reicht_semantisch_offene_rohwerte_an_den_use_case_durch`
    (Test `:167-182` — Adapter reicht unbekannte Hypothese/`QUATSCH`-Wirkungsklasse/
    `pSuccess=5.0`/leere Evidenz **durch**, lehnt **nicht** ab).
  - `adapters/outbound/llm-action-fake/.../FakeAktionsVorschlagsPort.kt:34-40`
    (Fake-Parity: kaputte Config → `emptyList()`).
  - **Praezedenz-Framework-Paar** `adapters/outbound/llm-koog/.../KoogLlmPort.kt`
    + `llm-langchain4j/.../LangChain4jLlmPort.kt` (fuer `LlmPort`): **bewusste
    Duplikation** — je eigener Parser/Prompt-Factory/Request-Response, **kein**
    geteiltes Modul. `StrictKoogLikelihoodParser.parseJson` (`KoogLlmPort.kt:230-231`)
    nutzt schlichtes `readTree(raw.trim())` **ohne** Trailing-Token-Guard.
  - Build-/Arch-Realitaet: `settings.gradle.kts:29-30`, `.a-check.yml:36-37/69-72/121-122`
    (nur `outbound_llm_action_langchain4j`), `Dockerfile:32/52/72/88` (nur
    langchain4j).
- **Vorherige Findings:** `2026-07-09-slice-042-design-review.md` (DR-F1 MEDIUM:
  Adapter=Wire-Integritaet vs. Use-Case=Semantik; Evidenz am Port unmoeglich),
  `2026-07-09-slice-042-plan-review.md` (F-2 LOW gleiches Thema, F-1 INFO
  disjunktives Modul), `done/slice-042-...md` §9-Aufloesung + Closure-Notiz
  (SR-F1 Trailing-Token-Haertung). Wurzel-Heuristik slice-041 DR-F3
  (Validierungs-Schicht).

---

## Findings

Jedes Finding folgt dem **§Output-Schema des Reviewer-Skills**. HIGH zuerst;
kein Loesungsvorschlag im `befund`.

### F-1 — Paritaets-Contract-Matrix (DoD 3) mischt Adapter-Wire- und Use-Case-Semantik in einen „Contract-Test"-Topf; fuenf der zehn Faelle besitzt der Adapter nicht (Evidenz kann er nicht pruefen)

- `kategorie`: MEDIUM
- `quelle`: `ARC-07` (Port-Vertrag/Lokalitaet), `ADR-0003` (Layer-Platzierung);
  Skill §Klassifikation „Unklarer Schicht-Besitz" / „Domain-Invariante dupliziert"
- `pfad`: `docs/plan/planning/open/slice-043-...md:34` (DoD 3) und `:54`
  (§3 „Contract-Matrix … beider Adapter"); vgl. `AktionsVorschlagen.kt:48-50/59/63/68/71`,
  `AktionsVorschlagsPort.kt:14`, `LangChain4jAktionsVorschlagsPortTest.kt:167-182`
- `befund`: DoD 3 (`:34-38`) verlangt „gemeinsame oder duplizierte
  Contract-Tests" fuer **zehn** Faelle. Fuenf sind **Wire-/Parser-Ebene** und
  liegen real im Adapter (gueltige Antwort, leere Antwort, kaputtes JSON,
  doppelte Felder, unbekannte Felder — `StrictAktionsVorschlagParser`
  `:134-195`). Die anderen fuenf — unbekannte Hypothese, ungueltige
  Wirkungsklasse, fehlende Evidenz, ungueltiges `pSuccess`, fehlende
  Konfidenzreferenz — validiert der **Use Case** `AktionsVorschlagen`
  (`:59`/`:63`/`:68`/`:71` bzw. Dedup `:48-50`), den slice-042 (DR-F1, §9,
  Closure) bewusst dort verortet; der bestehende Adapter reicht genau diese
  Rohwerte **durch** (Test `LangChain4jAktionsVorschlagsPortTest.kt:167-182`).
  Fuer „fehlende Evidenz" ist eine Adapter-Pruefung **unmoeglich**: der Port
  `vorschlaege(belief)` (`:14`) traegt keinen Evidenz-Kontext (`bekannteEvidenz`
  im Befehl, `AktionsVorschlagen.kt:22`). §3 lokalisiert die Matrix zugleich bei
  „beider **Adapter**" (`:54` „Contract-Matrix fuer Parser, Prompt und
  fail-closed Mapping"). Damit bleibt offen, ob die fuenf semantischen Faelle als
  **Use-Case-Tests** (existierend, korrekt) oder als **Adapter-/Contract-Tests**
  gefordert sind — Letzteres reimplementierte eine Domaeneninvariante im Adapter
  (Doppel-Quelle/Drift) oder testete ein Adapter-Verhalten, das der Adapter nach
  slice-042-Vertrag nicht hat. („fehlende Konfidenzreferenz" ist zusaetzlich
  schicht-mehrdeutig: fehlendes Feld → Wire-Verwurf im Adapter; leere/duplizierte
  Referenz → Use-Case-Dedup `:48-50`/`externalisieren` `:71`.)
- `verifizierbar`: ja — `make arch-check`/`make test`: die Port-Signatur belegt
  den fehlenden Evidenz-Kontext; ein Adapter-Test „unbekannte Hypothese →
  abgelehnt" widerspraeche `LangChain4jAktionsVorschlagsPortTest.kt:167-182`.

### F-2 — Schicht-Besitz des „gemeinsamen Parser-/Contract-Codes" (§6/DoD 2/§3) unbestimmt; Praezedenz dupliziert bewusst

- `kategorie`: MEDIUM
- `quelle`: `ARC-07` („so geteilt wie noetig"), `ARC-08` (`:106` Adapter
  importiert **nicht** fremden Adapter), `ADR-0001`/`ADR-0003`; Skill
  §Klassifikation „Unklarer Schicht-Besitz"
- `pfad`: `docs/plan/planning/open/slice-043-...md:80-83` (§6), `:53` (§3
  „bestehenden llm-action-* Adapter … Paritaets-Contract falls noetig gemeinsam
  nutzbar machen"), `:29` (DoD 2)
- `befund`: Der Schnitt laesst offen, **wo** gemeinsamer Parser-/Contract-Code
  lebt: §6 (`:80-83`) nennt „ein klares Adapter-internes Modul **oder** bewusst
  duplizierte Tests" als Alternativen, §3 (`:53`) „falls noetig gemeinsam
  nutzbar machen". Auf Design-Ebene ist der Besitz damit **unentschieden**. Zwei
  Kanten-Risiken sind an dieser Offenheit aufgehaengt: (a) gemeinsamer Code, der
  eine Framework-Dep in `hexagon:*` zoege, waere ein `ADR-0001`/`0003`-Verstoss
  (HIGH) — im Plan aber **nicht eingetreten**, DoD 1 (`:28`) haelt den Core
  provider-/framework-frei; (b) ein Shared-Modul, das Adapter A an Adapter B
  bindet, verletzte `ARC-08` (`:106`, Adapter importiert nicht fremden Adapter;
  `.a-check.yml` hat **keine** Adapter→Adapter-Kante). Die etablierte
  Repo-Praezedenz fuer ein Framework-Paar (`llm-koog` vs. `llm-langchain4j` fuer
  `LlmPort`) **dupliziert bewusst** (je eigener Parser/Prompt/Request-Response,
  kein geteiltes Modul) — der Schnitt benennt nicht, ob slice-043 dieser
  Praezedenz folgt oder shared Code einfuehrt; fiele die Wahl auf ein neues
  Shared-Modul, benennt DoD 4 (`:39-41`) dessen `.a-check`-Rolle/Root/Kanten und
  Coverage nicht.
- `verifizierbar`: ja — `make arch-check` (Kern-Reinheit + fehlende
  Adapter→Adapter-Kante) bestaetigt einen realisierten Verstoss erst am Diff; auf
  Plan-Ebene ist der Besitz zu fixieren.

### F-3 — „Paritaet" bleibt auf Feld-/Grob-Fehlerklassen; die slice-042-Haertung (Trailing-Token → sichtbarer Wurf) ist nicht als Paritaetsfall gepinnt, und die naechste Kopiervorlage (Koog-`LlmPort`-Parser) ist schwaecher

- `kategorie`: MEDIUM
- `quelle`: `LH-QA-02` (fail-safe/fail-closed), Maintainability; Skill
  §Klassifikation „Fehlende Negativtests bei neuem oeffentlichem Vertrag/Safety-Pfad"
- `pfad`: `docs/plan/planning/open/slice-043-...md:29-33` (DoD 2), `:34-38`
  (DoD 3), `:77-79` (§6-Risiko); vgl. `LangChain4jAktionsVorschlagsPort.kt:150-152`
  vs. `KoogLlmPort.kt:230-231`
- `befund`: DoD 2 (`:29-33`) definiert Paritaet ueber die **Felder** und „keine
  unterschiedlichen fachlichen Akzeptanz-/Fehlerregeln"; DoD 3 (`:34-38`)
  enumeriert als Fehlerfaelle „kaputtes JSON" und „doppelte Felder" (grobe
  Klassen). **Nicht** enumeriert ist der Fall, den slice-042 als
  Frischkontext-Safety-Fund haertete: Tokens hinter dem ersten JSON-Wert →
  **sichtbarer Wurf** statt „kein Vorschlag" (SR-F1;
  `LangChain4jAktionsVorschlagsPort.kt:150-152` `parser.nextToken() != null`),
  ebenso die Klassen-Trennung „leere Antwort → `emptyList` vs. Provider-Ausfall →
  Wurf". §6 (`:77-79`) **benennt** das Kopier-Risiko, DoD 3 **operationalisiert**
  es aber nicht als Paritaets-Assertion. Die naechste Kopiervorlage im Repo — der
  bestehende Koog-`LlmPort`-Parser `StrictKoogLikelihoodParser.parseJson`
  (`KoogLlmPort.kt:230-231`) — nutzt schlichtes `readTree(raw.trim())` **ohne**
  den Trailing-Token-Guard; ein Spiegeln dieser Vorlage reintroduzierte die exakt
  in slice-042 geschlossene Trailing-Token-Nachsicht, ohne dass ein enumerierter
  Paritaetstest sie faengt. „Fehlerklassen-Aequivalenz ueber beide Runner" bleibt
  damit auf Design-Ebene offen.
- `verifizierbar`: ja — `make test`: ein Paritaetstest „`[gueltig] {trailing}` →
  Wurf" ueber **beide** Runner zeigt, ob die Aequivalenz gepinnt ist;
  Parser-Code-Vergleich zeigt die Guard-Luecke.

### F-4 — Disjunktiver Modulname / `.a-check`-Rolle/Root nicht konvergiert

- `kategorie`: LOW
- `quelle`: Modul 1 (Plan konvergiert auf konkreten Diff), `ADR-0003`
  (`.a-check`-Rolle/Root je Modul); Skill §Klassifikation (LOW: Konkretheit)
- `pfad`: `docs/plan/planning/open/slice-043-...md:25` (DoD 1),
  `:52` (§3), `:66-68` (§4 „vor Start feststellen")
- `befund`: DoD 1 (`:25`) und §3 (`:52`) halten „`llm-action-koog` **oder**
  `llm-action-langchain4j`" disjunktiv; §4 (`:66-68`) verschiebt die Bestimmung
  auf „vor Start". Der Repo-Zustand ist eindeutig: `llm-action-koog` fehlt in
  `settings.gradle.kts`, `.a-check.yml` und `Dockerfile` (langchain4j ist da) →
  der fehlende Pfad ist **Koog**. Der Design-Schnitt benennt das konkrete Modul,
  die `.a-check`-Rolle `outbound_llm_action_koog`, Root und Kanten noch nicht;
  DoD 4 (`:39-41`) listet die Integrationsdateien, aber nicht die konkrete Rolle.
  Identische Konstellation wie slice-042 PR-F1 (dort INFO), die in §9 vor Code auf
  ein konkretes Modul + `.a-check`-Rolle/Root konvergierte.
- `verifizierbar`: ja — `make arch-check`/`make build` zeigt am Diff, ob Rolle/Root
  konsistent registriert sind.

### F-5 — `ADR-0002`-Guard und ADR-0002 im **Bezug** nicht benannt (Koog bereits adoptiert)

- `kategorie`: INFO
- `quelle`: `ADR-0002` (Dependency am Rand); Skill §Klassifikation (INFO/
  Doku-Konsistenz)
- `pfad`: `docs/plan/planning/open/slice-043-...md:10` (Bezug), `:84-85` (§6)
- `befund`: Der Koog-Pfad nutzt `ai.koog:koog-agents:1.0.0`, bereits als
  Adapter-Dep adoptiert (`llm-koog/build.gradle.kts:17`) → **keine** neue
  Toolchain-Flaeche, kein Folge-ADR (konsistent zu slice-042 §9 DR-F3). Der Bezug
  (`:10`) listet `ADR-0001/0003/0006`, aber **nicht** `ADR-0002`, obwohl eine
  Framework-Dep am Rand die Kern-Frage ist; slice-042 ergaenzte `ADR-0002` nach
  Review in den Bezug (§9 DR-F3). §6 (`:84-85`) nennt Provider-/Modellwahl als
  out-of-scope, restated aber die `ADR-0002`-Rueckfallebene (Folge-ADR nur bei
  **nicht** adoptiertem Framework) nicht.
- `verifizierbar`: ja — `build.gradle.kts`-Diff zeigt, dass keine **neue**
  Framework-Dep entsteht.

## Negativbefunde

- geprueft, ohne Befund: **`hexagon/domain`** — nicht beruehrt; DoD 1 (`:28`)
  haelt den Core provider-/framework-frei (kein `ai.koog`-/`dev.langchain4j`-Import).
- geprueft, ohne Befund: **`hexagon/application` + `AktionsVorschlagsPort`** —
  **Port unveraendert** ist der richtige Schnitt (Adapter liefert Rohvorschlaege,
  Use-Case-Validierungsrand bleibt intakt); §6 (`:86-88`) benennt sogar den
  Contract-Reconciliation-Guard, falls slice-042 den Port geaendert haette (tat
  er nicht — `AktionsVorschlagsPort.kt:14` unveraendert). (Setzt F-1 voraus:
  Adapter darf Use-Case-Semantik nicht duplizieren.)
- geprueft, ohne Befund: **`adapters/outbound/llm-action-fake`** — Fake-Parity
  (`FakeAktionsVorschlagsPort.kt:34-40`) bleibt gueltiger Referenzpfad, keine
  Aenderung noetig.
- geprueft, ohne Befund: **`adapters/outbound/llm-action-langchain4j`** —
  Paritaets-Referenz; der gehaertete Parser (`:150-152`) ist der Soll-Zustand,
  gegen den Koog-Paritaet zu messen ist (siehe F-3).
- geprueft, ohne Befund: **Praezedenz `llm-koog`/`llm-langchain4j`** — belegt die
  etablierte **bewusste Duplikation** von Framework-Paaren (relevant fuer F-2);
  kein bestehendes Shared-Modul, das der Slice fortschreiben muesste.
- geprueft, ohne Befund: **Executor-/Gate-Grenze** (`LH-FA-POL-006`,
  `ARC-03`/`ARC-09`, Hard Rule 3.7) — sauber ausserhalb des Adapters gehalten;
  DoD 5 (`:42-44`) schliesst CLI-Default-Umbindung, Gate-/Approval-/Executor-
  Aenderung explizit aus.
- geprueft, ohne Befund: **Build-/Arch-Integration** — DoD 4 (`:39-41`) benennt
  `settings.gradle.kts`, `.a-check.yml`, `Dockerfile`, Modul-`build.gradle.kts`
  und Kover-Gates vollstaendig; die slice-041/042-Lehre (Coverage/`ADR-0006`,
  `.a-check`-Rolle/Root) ist eingearbeitet (Konkretheit der Koog-Rolle → F-4).
- geprueft, ohne Befund: **`spec/`/`docs/plan/adr/`** — `ARC-07`/`ARC-08`
  Port-Lokalitaet und Adapter-Rolle konsistent referenziert; keine
  Supersedes-Kette (`ADR-README`) beruehrt; keine ADR-Immutabilitaet/Gate-Lockern
  betroffen (Hard Rule 3.5/3.6).

## Ausgefuehrte Sensoren

- `Read` ueber: Slice-043-Plan; `AktionsVorschlagsPort.kt`, `AktionsVorschlag.kt`,
  `AktionsVorschlagen.kt` (Zeilenanker); `LangChain4jAktionsVorschlagsPort.kt` +
  `-Test.kt`; `FakeAktionsVorschlagsPort.kt`; `KoogLlmPort.kt`,
  `LangChain4jLlmPort.kt` (Praezedenz-Paar); `.a-check.yml`, `settings.gradle.kts`,
  `Dockerfile` (Gate-Stages); `spec/architecture.md §2`; `ADR-0001/0003`,
  `ADR-README`; `AGENTS.md §3`; slice-042 Design-/Plan-Review + Closure/§9.
- `grep`: `llm-action-koog`-Existenz (absent → fehlender Pfad = Koog),
  `ARC-0[3789]`-Anker, `LH-QA-02`, Dockerfile-Gate-Stages.
- `make doc-check`: **ausgefuehrt, PASS** (`d-check` via Docker: 165 Dateien
  geprueft, 0 Befunde — validiert diesen Report gegen die Doc-Regeln). Uebrige
  `make`-Gates (test/coverage/arch/build) sind Design-Review-fremd (kein Diff)
  und bleiben dem Verifikations-/Closure-Lauf ueberlassen.

## Summary

| Kategorie | Anzahl |
|---|---:|
| HIGH | 0 |
| MEDIUM | 3 |
| LOW | 1 |
| INFO | 1 |

## Verdikt

**Merge-blockierend:** ja — **3 MEDIUM** (F-1 Schicht-Besitz der
Paritaets-Contract-Matrix; F-2 unbestimmter Besitz des gemeinsamen
Parser-/Contract-Codes; F-3 fail-closed-Paritaet nur grob, Haertungs-Regression
moeglich). Vor Implementierungsstart zu klaeren: (1) welche Schicht die fuenf
semantischen Contract-Faelle besitzt — Use-Case-Tests vs. (unmoegliche/
duplizierende) Adapter-Tests, insbesondere „fehlende Evidenz" ohne Evidenz-Kontext
am Port; (2) ob Paritaet ueber ein Adapter-internes Shared-Modul oder bewusste
Duplikation (Praezedenz) entsteht, mit eindeutigem Schicht-Besitz und ohne
Framework-/Fremdadapter-Kopplung; (3) ob die fail-closed-Aequivalenz (inkl.
slice-042 Trailing-Token-Haertung) als Paritaetstest ueber beide Runner gepinnt
wird. F-4 (LOW, disjunktives Modul/`.a-check`-Rolle) und F-5 (INFO, `ADR-0002`-
Bezug/Guard) sind vor Closure zu klaeren, nicht blockierend.

**Der potenzielle HIGH** (Framework-Dep im Core ueber gemeinsamen Code) ist auf
Design-Ebene **nicht eingetreten** — DoD 1 haelt den Core provider-frei; die
Offenheit ist als **unklarer Schicht-Besitz (MEDIUM, F-2)** kategorisiert, nicht
als bereits eingetretener Verstoss.

**Uebergabe:** Findings an Planung/Design-Klaerung (Rueckkante Design → Plan; ein
§9-Block wie in slice-041/042 traegt die Aufloesung sauber). Reviewer
kategorisiert — Wahl/Umsetzung bei Architect/Implementation. Keine Verifikation
(Modul 11; eigenes Artefakt `docs/verifications/*`).
