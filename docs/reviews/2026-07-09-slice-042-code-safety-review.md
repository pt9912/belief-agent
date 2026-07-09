# Review-Report: slice-042 Code-/Safety-Review (Frischkontext) — 2026-07-09

**Review-Art:** Code-Safety — eigener fail-closed-/Nicht-Umgehbarkeits-Durchgang für
einen **adversarialen LLM-Parser an der Aktions-Grenze** (Sicherheitsfunktion),
Reviewer-Skill §„Repo-Zusatz — Code-Safety-Review" (Praxis `slice-035`..`040`).
Ergänzt den allgemeinen slice-042-Code-Review (dort F-3: dieser Durchgang war offen).

**Kontext-Trennung (Modul 8):** unabhängiger Frischkontext-Lauf — hat den Code nicht
geschrieben und kannte die Implementierungs-Begründungen nicht. Diese Trennung findet
den unten stehenden Trailing-Token-Riss, den der Same-Context-Code-Review übersah.

**Gegenstand:** `adapters/outbound/llm-action-langchain4j/` (`StrictAktionsVorschlagParser`).

**Skill:** `.harness/skills/reviewer.md` @ v1.0 <!-- d-check:ignore (Skill-Pfad) -->
**Modell:** claude-opus-4-8[1m] (Frischkontext-Subagent) · **Datum:** 2026-07-09

**Eingangs-Kontext:** Slice-Plan §2 DoD / §9; `LH-FA-POL-006`, `LH-QA-02/03`;
`AktionsVorschlagsPort`, `AktionsVorschlagen` (Semantik), `AktionsVorschlag`,
`FakeAktionsVorschlagsPort`; Reviewer-Skill-Klassifikation.

---

## Findings

Reihenfolge **HIGH zuerst**. Keine HIGH: der Adapter erzeugt keine `Aktionsfreigabe`,
öffnet keinen Executor-/Gate-Pfad, fabriziert keinen Vorschlag; die Koerzions-/Typ-
Grenze ist gegen alle geprüften adversarialen Szenarien dicht.

### F1 — Trailing-Token-Nachsicht: strukturell defekte Antwort mit gültigem Präfix wird still akzeptiert

- `kategorie`: MEDIUM (fehlende Fehlersemantik auf dem Safety-Pfad + fehlender Negativtest; berührt `LH-QA-02` „stiller Erfolg, wo Verwurf gehört")
- `quelle`: `LH-QA-02`, `§9 DR-F2` / DoD §2 („unparsebar/falsch-geshapt → sichtbarer Wurf"; „unreachable" ≠ „kein Vorschlag")
- `pfad`: `LangChain4jAktionsVorschlagsPort.kt` — `parseJson` (`readTree`), Mapper-Konfig (nur `STRICT_DUPLICATE_DETECTION`, **kein** Trailing-Token-Check)
- `befund`: `readTree` liest genau **einen** JSON-Wert und ignoriert alles danach still
  (empirisch mit jackson 2.21 belegt): `[]  GARBAGE $$$` → leeres ArrayNode, **kein
  Wurf** → `emptyList()`; `[<gültig>] {"secret":"leak"}` → ArrayNode Größe 1, das
  anhängende Objekt still verworfen, Vorschlag durchgereicht. Damit ist eine
  strukturell kaputte Antwort mit gültigem Präfix **ununterscheidbar** von „kein
  Vorschlag" bzw. einer sauberen Antwort — die Fail-closed-Zusage greift für diesen
  Fall nicht. **Kein** fabrizierter Vorschlag, **kein** Gate-Pfad → MEDIUM, nicht HIGH.
- `verifizierbar`: ja — `make test` (Negativtest `"[]tail"` / `"[<gültig>]{...}"` erwartet Wurf).
- **Disposition (Implementation, nach diesem Lauf):** behoben — `parseJson` prüft nach
  dem ersten Wert explizit `parser.nextToken() == null` und wirft
  `AktionsVorschlagAntwortFehler` bei Trailing-Tokens. Tests
  `trailing_tokens_nach_json_wert_werfen` + `trailing_muell_nach_leerem_array_wirft`.

### F2 — Kein expliziter Negativtest für JSON-`null`-Feldwerte

- `kategorie`: LOW
- `quelle`: `LH-QA-03`; Maintainability
- `pfad`: `LangChain4jAktionsVorschlagsPortTest.kt`
- `befund`: `NullNode.isTextual/isNumber/isArray` sind alle `false` → ein JSON-`null`-
  Feldwert wird von `wireDefekt` korrekt verworfen, bevor `asText()`/`asDouble()` je
  koerzieren. Verhalten korrekt, aber **ungetestet** — reine Coverage-Lücke.
- `verifizierbar`: ja — `make test`.
- **Disposition:** behoben — Test `json_null_feldwert_wird_verworfen` ergänzt.

### F3 — DoS-Grenzen nur über Jackson-Defaults, nicht explizit gepinnt

- `kategorie`: INFO
- `quelle`: Maintainability / Ressourcen
- `pfad`: `LangChain4jAktionsVorschlagsPort.kt` (Mapper-Konfig)
- `befund`: Tiefe Verschachtelung (>1000) und Riesen-Strings (>20 MB) werfen über
  Jacksons `StreamReadConstraints`-Defaults (`StreamConstraintsException` ist eine
  `JsonProcessingException` → gefangen + als sichtbarer Fehler gewrappt, fail-closed).
  Grenzen sind aber **nur Default**, nicht explizit gepinnt/getestet — eine künftige
  Jackson-Änderung könnte still lockern. Akzeptierte Grenze in diesem Slice.
- `verifizierbar`: nein (nur unter Extrem-Fixture).

## Negativbefunde

- **Jackson-Koerzion — kein Loch:** `wireDefekt` prüft `isTextual`/`isNumber`/`isArray`
  **vor** dem `asText()`/`asDouble()`-Mapping; `pSuccess` als Bool/String, `beschreibung`
  als Zahl/Array/null, Evidenz-Nicht-Strings → alle verworfen. Keine still-koerzierte
  Feld-Verschmelzung.
- **JSON-`null` — kein Loch:** jedes `null`-Feld wird vor jeder Koerzion verworfen (→ F2 nur Test-Lücke).
- **Fehlendes Feld / NPE — kein Loch:** `felder != ERLAUBTE_FELDER` läuft zuerst und
  erzwingt exakt die 6 Schlüssel → jedes `element.get(x)` ist präsent, kein NPE.
- **Fail-open / leer-vs-unparsebar — sauber (bis auf F1):** blank→`emptyList`,
  Nicht-Array→Wurf, Parse-Fehler→Wurf, all-defektes Array→`emptyList` + Warnungen (downstream inert).
- **Semantik-Leak / DR-F1 — kein Loch:** unbekannte Hypothese, `QUATSCH`-Wirkungsklasse,
  `pSuccess=5.0`, leere Evidenz werden durchgereicht; `isFinite()` ist Wire-Integrität,
  nicht `[0,1]`-Semantik. Keine Duplizierung der Use-Case-Invarianten.
- **Gate/Executor — kein Loch:** Adapter erzeugt ausschließlich `List<AktionsVorschlag>`;
  kein Bezug zu `Aktionsfreigabe`/`KonfidenzgebundeneAktion`/Gate (`LH-FA-POL-006`).
- **Doppelte Felder — korrekt:** `STRICT_DUPLICATE_DETECTION` → Whole-Stream-Wurf wie in
  der geschärften DoD 2 (Code-Review F-1) gefordert; getestet.
- **Ressourcen — adäquat (→ F3 INFO):** Verschachtelung/String-Grenzen werfen fail-closed über Defaults.

## Summary

| Kategorie | Anzahl |
|---|---|
| HIGH | 0 |
| MEDIUM | 1 (F1 — nach diesem Lauf behoben) |
| LOW | 1 (F2 — behoben) |
| INFO | 1 (F3 — akzeptiert) |
| Negativbefunde (geprüft, sauber) | 7 |

## Verdikt

**Merge-blockierend: nein** (F1 grenzwertig-MEDIUM, aber ohne fabrizierten Vorschlag/
Gate-Pfad). Der Koerzions-/Typ-Kern ist fail-closed und gegen alle geprüften
adversarialen Szenarien dicht. F1 (Trailing-Token) war ein echter, verifizierter
Fail-closed-Riss und ist behoben (expliziter Trailing-Check + Negativtests); F2
(null-Test) behoben; F3 (DoS-Defaults) akzeptierte Grenze. Keine Semantik im Adapter,
keine Gate-Kopplung.

## Geschichte

| Version | Datum | Änderung |
|---|---|---|
| 1 | 2026-07-09 | Initialer Frischkontext-Code-Safety-Lauf (Modul 8). F1 (Trailing-Token, behoben), F2 (null-Test, behoben), F3 (DoS-Defaults, INFO). |
