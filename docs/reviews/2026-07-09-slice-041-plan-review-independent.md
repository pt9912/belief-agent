# Review-Report: slice-041 Plan-Review (Unabhaengiger Frischkontext-Lauf) — 2026-07-09

**Review-Art:** Plan — geprueft *wogegen*: Spec (`spec/lastenheft.md`) und
Accepted-ADRs, **vor** Implementierung, ohne Diff (Modul 10 §Drei Review-Arten).

**Gegenstand:** `docs/plan/planning/open/slice-041-dauerhafte-audit-datenbank.md`
(dritte Fassung, Working Tree; inkl. §9 Rueckkanten-Tabelle).

**Lauf-Charakter:** **Unabhaengiger Frischkontext-Reviewer** (Modul 8). Dieser
Report entsteht in einem **frischen Kontext**, getrennt von der bestehenden
Same-Context-Kette (`…-plan-review.md`, `…-plan-review-rerun.md`,
`…-plan-review-rerun2.md`), die „konvergiert, 0 Blocker" meldet. Die eigenen
Findings unten wurden **vor** dem Lesen der bestehenden Reports gebildet und der
Ist-Code selbst verifiziert (Plan-Prosa nicht geglaubt). Reconciliation mit der
Kette am Ende.

**Skill:** `.harness/skills/reviewer.md` @ v1.0
**Modell:** unabhaengiger Reviewer (Claude Opus 4.8) · **Datum:** 2026-07-09

**Eingangs-Kontext** (die Vertraege, gegen die geprueft wurde — Skill §Kontext-Eingang):

- `docs/plan/planning/open/slice-041-dauerhafte-audit-datenbank.md` (Plan)
- `spec/lastenheft.md`: `LH-FA-AUD-001/002/003/004`, `LH-QA-02/03/04/06`
- `spec/architecture.md`: `ARC-06`, `ARC-08`, `ARC-09`
- `docs/plan/adr/0001…`, `0002…`, `0003…`; **zusaetzlich geprueft:** `0004-coverage-gate.md`, `0006-coverage-gate-scope.md`
- `AGENTS.md §3`/`§5`, `harness/README.md` (Source Precedence)
- **Safety-Ist-Code (Write-Pfad, alle `anhaengen`-Konsumenten):**
  `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/gaten/AktionGaten.kt`,
  `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/aktionsvorschlag/AktionsVorschlagen.kt`,
  `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/konfidenzexternalisieren/KonfidenzExternalisieren.kt`,
  `example/code-agent/src/main/kotlin/dev/beliefagent/example/codeagent/Main.kt`
- **Read-Pfad-Konsumenten:** `adapters/inbound/cli/src/main/kotlin/dev/beliefagent/adapter/cli/Runtime.kt`, `example/code-agent/.../Main.kt`
- `AuditPort.kt`, `MemoryAudit.kt`, `EreignisProtokoll.kt`, `Rekonstruktion.kt`, `settings.gradle.kts`, `.a-check.yml`, `Dockerfile`
- **vorherige Findings am gleichen Modul (Skill-Pflicht):** die fuenf bestehenden `docs/reviews/2026-07-09-slice-041-*`-Reports (erst NACH eigener Findingsbildung gelesen)

---

## Findings

### IPR-1 — Write-Pfad-„fail-closed"-Behauptung deckt nur 1 von 4 `anhaengen`-Konsumenten

- `kategorie`: MEDIUM
- `quelle`: `LH-QA-02` (fail-safe/Sichtbarkeit), `LH-FA-AUD-001` (Ereignis unveraenderlich protokolliert); Skill §Klassifikation („stiller … leeres Ergebnis, wo ein sichtbarer Fehler/Eskalation gehoert")
- `pfad`: `docs/plan/planning/open/slice-041-dauerhafte-audit-datenbank.md:50`
- `befund`: DoD 3 begruendet den geworfenen Fehlerkanal des Adapters mit „die
  geworfene `anhaengen`-Exception faengt **der Konsument** `AktionGaten` und
  eskaliert fail-closed (… verifiziert)". Der Write-Pfad hat aber **vier**
  Produktiv-/Beispiel-Konsumenten von `audit.anhaengen`, die die identische
  Adapter-Exception **unterschiedlich** behandeln, nur einer davon eskaliert:
  (1) `AktionGaten.kt:89-103` — `try/catch` → `Aktionsfreigabe.Eskaliert`
  (fail-closed, wie behauptet); (2) `AktionsVorschlagen.kt:89` — der
  `anhaengen`-Aufruf liegt in `runCatching { … }.getOrNull()`
  (`AktionsVorschlagen.kt:57`/`:91`), d. h. ein Schreibfehler des persistenten
  Adapters wird **verschluckt** und der Vorschlag faellt auf `null` → im Zyklus
  `Zyklusergebnis.Abgelehnt` — das Pflicht-Ereignis „Aktion vorgeschlagen"
  (`LH-FA-AUD-001`) geht **still verloren**, ununterscheidbar von „kein
  gate-faehiger Vorschlag"; (3) `KonfidenzExternalisieren.kt:49` wird aus genau
  diesem `runCatching` gerufen und wird **ebenfalls verschluckt** (zusaetzlich
  bleibt `KonfidenzPort` bereits beschrieben, `KonfidenzExternalisieren.kt:48` —
  Teilzustand); (4) `example/code-agent/Main.kt:176`
  (`updated.ereignisse.forEach(audit::anhaengen)`) ist ungeschuetzt, faengt oben
  nur `FixtureInputFailure` → eine generische Schreib-Exception propagiert
  **uncaught** (Prozess-Abbruch), keine Eskalation. Der Plan generalisiert eine
  Ein-Konsumenten-Praezedenz auf „den Write-Pfad" und laesst die drei anderen
  Konsumenten uncharakterisiert — dieselbe Asymmetrie, die die Kette fuer den
  **Read**-Pfad (DR-R1) sorgfaeltig aufloest, bleibt fuer die **Write**-Konsumenten
  jenseits `AktionGaten` unbetrachtet.
- `verifizierbar`: ja — `grep "anhaengen"` ueber Produktivquellen zeigt die vier
  Konsumenten; `make test` mit einem Schreibfehler-Fixture auf dem
  Vorschlags-/Zykluspfad zeigt, ob der Fehler eskaliert oder als stilles
  `Abgelehnt` verschluckt wird.

### IPR-2 — Coverage-Gate-Erweiterung ohne `ADR-0004`/`ADR-0006`-Referenz und ohne Modul-`build.gradle.kts`/`kover`-Block

- `kategorie`: MEDIUM
- `quelle`: `ADR-0006` (Coverage-Gate-Scope, per-Modul `kover`-Block Pflicht), `ADR-0004`; `AGENTS.md §5` (beruehrte IDs referenzieren); Skill §Klassifikation („Fehlende ID-Referenz … obwohl ein oeffentlicher Vertrag betroffen ist")
- `pfad`: `docs/plan/planning/open/slice-041-dauerhafte-audit-datenbank.md:67`
- `befund`: DoD 5 nimmt das neue Modul „in … Dockerfile/Coverage-Gate … auf".
  Der Coverage-Gate-Scope ist ein oeffentlicher Vertrag, den `ADR-0006`
  (erweitert `ADR-0004`) regelt — inklusive der **expliziten** Pflicht, dass ein
  neues Modul „erst gegatet [ist], wenn es einen `kover`-Block **und** einen
  `koverVerify`-Eintrag im Dockerfile bekommt — keine zentrale Automatik"
  (`docs/plan/adr/0006-coverage-gate-scope.md:66`; Dockerfile enumeriert je
  Modul, `Dockerfile:74`). Der Kopf **Bezug** (`:8`) nennt `ADR-0001/0002/0003`,
  aber **weder `ADR-0004` noch `ADR-0006`**; die Datei-Tabelle §3 (`:78`) fuehrt
  `settings.gradle.kts`, `.a-check.yml` und `Dockerfile` als Aenderungen, aber
  **kein** `adapters/outbound/audit-file/build.gradle.kts` und benennt den per-Modul-
  `kover { … minBound }`-Block nicht. Fuer ein Safety/Control-Repo, in dem die
  Nicht-Auto-Vererbung von `ADR-0006` gerade der Schutz gegen still ungegatete
  neue Module ist, ist die fehlende ID-/Artefakt-Verankerung eine Luecke.
- `verifizierbar`: teilweise — die fehlende ADR-Referenz ist Doc-Konsistenz (kein
  Gate); die per-Modul-`kover`-Pflicht bestaetigt spaeter `make coverage-gate`
  (ein ungegatetes neues Modul faellt nicht rot auf, gerade das ist der Punkt).

## Negativbefunde

- geprueft, ohne Befund: **Read-Pfad** — die zwei einzigen Produktiv-`.lade()`-
  Konsumenten (`Runtime.kt:249` `auditEreignisse()`, `Main.kt:137`
  `audit.lade().groesse`) sind observability-only; `Rekonstruktion` wird im
  Produktivcode nicht aufgerufen (nur KDoc), der Zyklus-/Gate-Pfad konsumiert
  **weder** `.lade()` **noch** `Rekonstruktion`. Die DR-R1-Praemisse der Kette
  **haelt** (eigenstaendig per `grep` verifiziert).
- geprueft, ohne Befund: **`EreignisProtokoll.von(...)`** existiert
  (`EreignisProtokoll.kt:49`) und erzwingt die Append-only-Ordnung
  (`IllegalArgumentException` bei Rueckdatierung, `:30-39`); die DoD-4-Wiederverwendung
  statt Adapter-Reimplementierung ist gegen den Code plausibel.
- geprueft, ohne Befund: **`LH-FA-AUD-004` (Zeitstempel-Quelle)** — jedes
  `Ereignis` traegt seinen `zeitstempel`; Reload baut ueber `von(...)` aus den
  **ereignis-getragenen** Zeitstempeln (kein Neu-Stempeln per Wall-Clock),
  Ordnung deterministisch re-validiert.
- geprueft, ohne Befund: **`LH-QA-06` (Inspizierbarkeit)** — DoD 1 verankert die
  lokale, inspizierbare Speicherform (`:33`); Export = Folgearbeit, sauber
  abgegrenzt.
- geprueft, ohne Befund: **Wellen-Zuordnung/WIP** (welle-05, WIP=0),
  **Abgrenzung** (Retention/Migration/Backup/Export/CLI-Binding Folgearbeit),
  **Trigger-slice-040-in-Scope** (§4) — unveraendert korrekt.
- geprueft, ohne Befund: **Hard Rule 3.6** (Gate-Lockern nur per ADR) nicht
  beruehrt; der Plan senkt keine Schwelle (das IPR-2-Finding betrifft eine
  fehlende Referenz/Enumerierung, keine Absenkung).

## Ausgefuehrte Sensoren

- `Read`/`grep` ueber Plan, Lastenheft, `architecture.md`, `ADR-0001/0002/0003/0004/0006`,
  `AGENTS.md`, `harness/README.md`, alle vier `anhaengen`-Konsumenten, beide
  `.lade()`-Konsumenten, `EreignisProtokoll`/`Rekonstruktion`, `settings.gradle.kts`,
  `.a-check.yml`, `Dockerfile`, `observation-build-report`/`audit-memory`
  `build.gradle.kts`.
- `grep "anhaengen"` (Produktivquellen) — **4** Write-Konsumenten belegt; `grep "\.lade()"` — 2 Read-Konsumenten; `grep "Rekonstruktion"` — keine Produktivaufrufe.
- `make doc-check` — **PASS** (`d-check` @ `sha256:3bbdb19b…`: **149 Datei(en) geprueft, 0 Befund(e)**; validiert auch diesen Report und den Design-Review-Independent).

## Summary

| Kategorie | Anzahl |
|---|---:|
| HIGH | 0 |
| MEDIUM | 2 |
| LOW | 0 |
| INFO | 0 |

## Verdikt

**Merge-blockierend (Plan-Ebene):** ja — **2 neue MEDIUM** (IPR-1
Write-Pfad-Charakterisierung, IPR-2 Coverage-Gate-/ADR-Referenz), die die
Same-Context-Kette **nicht** aufgefuehrt hat. Ich stimme der „konvergiert, 0
Blocker"-Bewertung der bestehenden Kette **nicht vollstaendig** zu.

**Reconciliation mit der bestehenden Kette:** Die Kette liegt **richtig** bei
Read-Pfad (DR-R1), `EreignisProtokoll.von`-Reuse (DR-F3), Source-Set/`java.nio`-
Praezedenz (DR-F2, Plan-Ebene) und der Plan-Konvergenz (PR-F1..F4/PR-R1) — alle
eigenstaendig nachgeprueft und bestaetigt. **Abweichung:** Der Rerun-1-Negativbefund
„**Write-Pfad fail-closed ist real**" ist nur fuer `AktionGaten` wahr; er wurde
auf „den Write-Pfad" verallgemeinert, ohne die drei weiteren `anhaengen`-Konsumenten
zu pruefen (IPR-1). Zusaetzlich blieb die `ADR-0006`-Coverage-Verankerung
unadressiert (der Kette-Negativbefund „nimmt das Modul in Coverage-Gates auf"
notiert die Aufnahme, nicht die fehlende ADR-Referenz/`kover`-Blockpflicht, IPR-2).

**Uebergabe:** IPR-1/IPR-2 an die Planung (Rueckkante Review → Plan, §9
fortschreiben). Der Report kategorisiert nur (Skill §„Was dieser Skill NICHT
macht"); er ersetzt keine Verifikation (Modul 11).
