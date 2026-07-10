# Review-Report: slice-043 Code-/Safety-Review (Frischkontext) — 2026-07-10

**Review-Art:** Code-Safety — eigener fail-closed-/Nicht-Umgehbarkeits-Durchgang für
einen **adversarialen LLM-Parser an der Aktions-Grenze** (Sicherheitsfunktion),
Reviewer-Skill §„Repo-Zusatz — Code-Safety-Review" (Praxis `slice-035`..`042`).
Ergänzt den allgemeinen slice-043-Code-Review (`2026-07-10-slice-043-code-review.md`,
dort F-4: dieser Durchgang war offen).

**Kontext-Trennung (Modul 8):** unabhängiger Frischkontext-Lauf (separater Reviewer-
Agent, gleicher Skill) — hat den Code nicht geschrieben und kannte die
Implementierungs-Begründungen nicht. Jedes Finding ist gegen den Ist-Code
gegenverifiziert (Zeilen-/Zweig-Belege inline).

**Gegenstand:** `adapters/outbound/llm-action-koog/**` (`StrictAktionsVorschlagParser`,
`AktionsVorschlagPromptFactory`, reflektive Fabrik `instantiateLlmClient`,
`runBlocking`-Transport-Wiring, Contract-Test-Matrix inkl.
`KoogFabrikAblehnungsStubs.kt`).

**Skill:** `.harness/skills/reviewer.md` @ v1.0 · <!-- d-check:ignore (Adopter-spezifischer Skill-Pfad) -->
**Modell:** claude-opus-4-8[1m] (Frischkontext-Subagent) · **Datum:** 2026-07-10

**Eingangs-Kontext:** Slice-Plan §2 DoD / §6 / §9 (F-1/F-3); `LH-FA-POL-006`,
`LH-QA-02/03`, Hard Rule 3.7/3.8; `AktionsVorschlagsPort` (unverändert),
`AktionsVorschlagen` (Semantik), `AktionsVorschlag`; **Soll-Referenz**
`llm-action-langchain4j` (parser-identisch); das laut Plan **nicht** zu spiegelnde
Muster `KoogLlmPort.kt:230-231`; Präzedenz `2026-07-09-slice-042-code-safety-review.md`
(F1 Trailing-Token, F2 null-Test, F3 DoS-Defaults).

**Arbeitsbaum-Hinweis (Auditierbarkeit):** Der Diff wurde **nach** dem allgemeinen
Code-Review nachgezogen — die dort als F-1 (MEDIUM) kategorisierte Testlücke der
reflektiven Fabrik ist im Ist-Baum **geschlossen** (neue Datei
`KoogFabrikAblehnungsStubs.kt`, vier Ablehnungsast-Tests `KoogAktionsVorschlagsPortTest.kt:288-340`,
KDoc referenziert „Code-Review slice-043 F-1"). Dieser Safety-Lauf prüft diesen
aktualisierten Stand.

---

## Findings

Reihenfolge **HIGH zuerst**. **Keine HIGH, keine MEDIUM, keine LOW.** Verifiziert:
der Adapter liefert ausschließlich `List<AktionsVorschlag>`
(`KoogAktionsVorschlagsPort.kt:137-142`), erzeugt keine `Aktionsfreigabe`/
`KonfidenzgebundeneAktion`, ruft kein Gate, öffnet keinen Executor-Pfad
(`LH-FA-POL-006`, Hard Rule 3.7/3.8); kein fabrizierter Vorschlag. Die Koerzions-/
Typ-Grenze (`wireDefekt` `:270-284` läuft **vor** jeder `asText()/asDouble()`-Koerzion
`:254-266`) ist gegen alle geprüften adversarialen Shapes dicht. Der slice-042-
Trailing-Token-Fix (F1) ist präsent und **nicht** regressiert (`:239-241`); der
null-Feld-Test (slice-042 F2) ist übernommen (`:108-113`).

### SR-F1 — Reflektive Klassenladung führt Static-Initializer der Fremdklasse vor dem `isAssignableFrom`-Typ-Check aus (neue Fläche ggü. slice-042)

- `kategorie`: INFO
- `quelle`: Maintainability / latente Gadget-Fläche; `LH-QA-02` (nur falls Trust-Boundary künftig kippt)
- `pfad`: `KoogAktionsVorschlagsPort.kt:120` (`Class.forName(clientClass)`) vs. Typ-Check `:123-125`
- `befund`: `Class.forName(String)` (1-arg, `:120`) lädt **und initialisiert** die
  benannte Klasse; der `require(LLMClient::class.java.isAssignableFrom(...))`-Typ-Check
  läuft erst danach (`:123-125`). Ein `clientClass`-Wert, der **kein** `LLMClient`
  ist, hat seinen Static-Initializer damit bereits ausgeführt, bevor er zurückgewiesen
  wird. `clientClass` stammt aus **Deployment-Config** (env-getriebenes
  Composition-Root-Wiring; Präzedenz `KoogLlmPort.kt:93` liest `KOOG_CLIENT_CLASS` via
  `System.getenv`), **nicht** aus dem adversarialen LLM-/Belief-Pfad; der
  Action-Adapter-`fromLlmClient(clientClass)`-Pfad hat aktuell keinen Produktiv-
  Aufrufer. Type-Confusion ist korrekt geschützt (`newInstance() as LLMClient` erst
  nach `isAssignableFrom`). Beobachtung, keine Aktion erwartet, solange die
  Trust-Boundary Operator-Config bleibt.
- `verifizierbar`: nein — nur unter Fixture-Klasse mit beobachtbarem Static-Init-Seiteneffekt; kein `make`-Gate deckt die Aufruf-Reihenfolge.

### SR-F2 — DoS-Grenzen nur über Jackson-`StreamReadConstraints`-Defaults, nicht gepinnt (slice-042 F3 rekurriert)

- `kategorie`: INFO
- `quelle`: Maintainability / Ressourcen; **rekurriert** slice-042-Code-Safety F3 (2. Auftreten im Safety-Lauf)
- `pfad`: `KoogAktionsVorschlagsPort.kt:217-221` (Mapper-Konfig), `:233` (`raw.trim()`)
- `befund`: `JsonFactory.builder().enable(STRICT_DUPLICATE_DETECTION).build()` belässt
  `StreamReadConstraints` auf Default (Tiefe >1000, String-Value >20 MB).
  Überschreitung wirft `StreamConstraintsException` (⊂ `JsonProcessingException`) →
  im `catch` `:244-251` gefangen und als `AktionsVorschlagAntwortFehler` gewrappt
  (**fail-closed**). Grenzen sind aber nur Default, nicht explizit gepinnt/getestet;
  eine künftige Jackson-Änderung könnte still lockern. Zusätzlich alloziert
  `raw.trim()` `:233` eine Voll-Kopie der (per Whole-Document nicht begrenzten)
  Transport-Antwort vor dem Parsen. Identisch zum Sibling — akzeptierte Grenze in
  diesem Slice.
- `verifizierbar`: nein — nur unter Extrem-Fixture.

### SR-F3 — Transport-/Provider-Ausfall propagiert als roher Fremdtyp, nicht adaptergehüllt (rekurriert, fail-closed)

- `kategorie`: INFO (Steering-Loop-Signal — **3. Auftreten**)
- `quelle`: `LH-QA-02`; **rekurriert** slice-042 F-4 + slice-043-Code-Review F-3
- `pfad`: `KoogAktionsVorschlagsPort.kt:66-67` / `:81-82` (`runBlocking { executor/client.execute(...).textContent() }`); Test `KoogAktionsVorschlagsPortTest.kt:124-129`
- `befund`: `runBlocking` schluckt nichts — eine Transport-Exception propagiert
  **sichtbar** (Test: `IllegalStateException`, ungehüllt), fail-closed und von
  `emptyList()` unterscheidbar. Sie ist aber **nicht** in `AktionsVorschlagAntwortFehler`
  gehüllt; ein Konsument kann „Provider unreachable" nicht per Typ von einem internen
  Fehler trennen. Aus Safety-Sicht **kein Loch** (sichtbarer Wurf). Da derselbe
  Design-Punkt nun 3× auftritt (slice-042 F-4, slice-043 Code-Review F-3, hier),
  ist die Steering-Loop-Schwelle (Skill §Pflege) erreicht: zu entscheiden ist, ob
  ein Folge-Slice (geordnete Konsumenten-Eskalation) oder eine AGENTS.md-/ADR-Notiz
  fällig ist — **Entscheidung: Architect/Planner**, der Reviewer verweist nur.
- `verifizierbar`: ja — `make test` (`provider_ausfall_propagiert_sichtbar`).

## Negativbefunde (geprüft, ohne Befund — je Safety-Frage)

- **Trailing-Token / Prefix-valid-then-garbage — kein Loch, keine Regression:**
  `parseJson` prüft nach dem ersten Wert explizit `parser.nextToken() != null` und
  wirft (`:239-241`); `[<gültig>] {"leak":1}` und `[] GARBAGE` werfen (Tests
  `:93-99`, `:102-105`). Übernommen aus slice-042 F1-Fix; **nicht** die schwächere
  `readTree(raw.trim())`-Vorlage aus `KoogLlmPort.kt:230-231`.
- **Koerzions-Grenze — kein Loch, kein NPE:** `wireDefekt` (`:270-284`) prüft
  `isObject` → exakte `ERLAUBTE_FELDER`-Menge → `isTextual`/`isNumber && isFinite`/
  `isArray`/Element-`isTextual` **vor** dem Mapping (`:254-266`). Falscher Typ,
  JSON-`null` (`NullNode.isTextual=false`, Test `:108-113`), fehlendes/extra Feld
  (Set-Mismatch → verworfen, `get()` nie auf fehlendem Feld → kein NPE),
  `1e400`→Infinity (`!isFinite`, Test `:166-172`), Nicht-Objekt-Element,
  Nicht-String-Evidenz, Numeric-String `"0.8"` (`isNumber=false`) → alle sauber
  verworfen. Doppelte Felder → Whole-Stream-Wurf via `STRICT_DUPLICATE_DETECTION`
  (`:217-221`, Test `:116-121`).
- **Fail-open vs fail-safe — sauber:** blank→`emptyList` (`:224`), Nicht-Array→Wurf
  (`:226-228`), Parse-Fehler→Wurf (`:244-251`); „Provider unreachable" (Transport-Wurf)
  bleibt von „kein Vorschlag" (`emptyList`) unterscheidbar. Leeres `textContent()` →
  blank-Pfad → `emptyList` (Design-Parität zum Sibling; kein gefährlicher stiller
  Erfolg), nie fabriziert.
- **Error-Swallowing — kein Loch:** der `catch` fängt **nur** `JsonProcessingException`
  und re-wirft sichtbar; kein `runCatching` um den Parse-Pfad; per-Element-Verwurf
  (`vorschlagOderVerwerfen` `:253-267`) meldet über `warnung` und verbirgt **keinen**
  Gesamtantwort-Defekt (Non-Array/Trailing/Dup werden vor der Element-Schleife
  geworfen). Die `runCatching`/`getOrElse` in `instantiateLlmClient` (`:120-133`)
  übersetzen jeweils in einen **sichtbaren** `IllegalArgumentException`-Wurf.
- **Gate/Executor-Nicht-Umgehung — kein Loch:** `vorschlaege` (`:137-142`) liefert
  ausschließlich `List<AktionsVorschlag>`; kein Bezug zu `Aktionsfreigabe`/Gate/
  Executor (`LH-FA-POL-006`). Downstream `AktionsVorschlagen.kt` erzeugt ebenfalls
  keine Freigabe und ruft kein Gate — Semantik bleibt dort (`:59` unbekannte
  Hypothese, `:63` `Wirkungsklasse.valueOf`, `:68` Evidenz-Nicht-Leere).
- **Reflektive Klassenladung — fail-closed (bis auf SR-F1-Reihenfolge):** alle vier
  Ablehnungsäste werfen sichtbar (`:120-133`) und sind negativ getestet
  (`KoogFabrikAblehnungsStubs.kt` + Tests `:288-340`; schließt Code-Review-F-1).
  Ein feindlich injizierter `LLMClient` könnte beliebigen Text liefern — der läuft
  durch **denselben** strikten Parser und umgeht dessen Garantie nicht.
- **Ressourcen/DoS — adäquat (→ SR-F2):** Tiefe/Riesen-Strings werfen fail-closed
  über Jackson-Defaults; nicht gepinnt.
- **Semantik-Leak — kein Loch:** unbekannte Hypothese, `QUATSCH`-Wirkungsklasse,
  `pSuccess=5.0`, leere Evidenz werden **durchgereicht** (Test `:191-205`);
  `isFinite()` ist Wire-Integrität, **nicht** `[0,1]`-Semantik. Keine Use-Case-
  Invariante im Adapter dupliziert; `AktionsVorschlagsPort.kt` unverändert.

## Summary

| Kategorie | Anzahl |
|---|---|
| HIGH | 0 |
| MEDIUM | 0 |
| LOW | 0 |
| INFO | 3 (SR-F1 reflektiver Static-Init · SR-F2 DoS-Defaults rekurriert · SR-F3 roher Transport-Typ, 3. Auftreten) |
| Negativbefunde (geprüft, sauber) | 8 (je Safety-Frage) |

## Verdikt

**Merge-blockierend (Safety): nein.** Der Koerzions-/Typ-Kern ist fail-closed und ein
faithful duplicate des slice-042-Parsers **ohne Regression** (Trailing-Guard präsent
`:239-241`, null-Test übernommen `:108-113`, DoS als SR-F2 rekurrent). Kein
fabrizierter Vorschlag, keine Gate-/Executor-Kopplung, kein geschlucktes Signal, keine
Semantik-Duplizierung. Die einzige neue Fläche — reflektive Klassenladung — ist
fail-closed und deployment-config-begrenzt; SR-F1 (Static-Init vor Typ-Check) ist eine
latente INFO-Beobachtung ohne aktuellen Angriffspfad. SR-F2/SR-F3 sind rekurrente
INFO-Design-Punkte; SR-F3 hat mit dem 3. Auftreten die Steering-Loop-Schwelle erreicht
(Verweis an Architect/Planner — Reviewer entscheidet nicht).

**Übergabe:** INFO-Findings gehen als Beobachtung/Verweis an Implementation
(SR-F1/SR-F2 akzeptierte Grenzen) bzw. Architect/Planner (SR-F3 Steering-Loop). Der
Report ersetzt **keine** Verifikation — DoD-/Gates-Konformität prüft der Verifier
separat (Modul 11, `docs/verifications/*`).

## Geschichte

| Version | Datum | Änderung |
|---|---|---|
| 1 | 2026-07-10 | Initialer Frischkontext-Code-Safety-Lauf (Modul 8) über den nach dem Code-Review nachgezogenen Stand (F-1-Testlücke geschlossen). SR-F1 (reflektiver Static-Init, INFO), SR-F2 (DoS-Defaults, rekurriert, INFO), SR-F3 (roher Transport-Typ, 3. Auftreten, INFO/Steering-Loop). Keine HIGH/MEDIUM/LOW. |
