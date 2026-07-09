# Review-Report: slice-041 (persistenter AuditPort-Adapter) — 2026-07-09

**Review-Art:** Code — geprüft wird der **fertige Diff gegen Plan + Konventionen**
(`AGENTS.md` Hard Rules), Modul 10 §Drei Review-Arten. Plan-/Design-Reviews
(inkl. unabhängigem Frischkontext-Lauf) liegen bereits vor und sind in
`slice-041 §9` aufgelöst; dieser Lauf prüft den **Code** gegen diese aufgelösten
Entscheidungen.

**Gegenstand:** Arbeitsbaum-Diff slice-041 — neues (untracked) Modul
`adapters/outbound/audit-file/` (`DateiAudit.kt`, `EreignisSerialisierung.kt`,
`AuditFehler.kt`, `build.gradle.kts`, 2 Testdateien) plus tracked Änderungen an
`settings.gradle.kts`, `.a-check.yml`, `Dockerfile`, `docs/user/integration.md`,
`docs/user/cli-entscheidungsnachweis.md`.

**Skill:** `.harness/skills/reviewer.md` @ v1.0 <!-- d-check:ignore (Skill-Pfad) -->
**Modell:** claude-opus-4-8[1m] · **Datum:** 2026-07-09

**Eingangs-Kontext** (die Verträge, gegen die geprüft wurde):

- Slice-Plan `docs/plan/planning/in-progress/slice-041-dauerhafte-audit-datenbank.md`
  (DoD §2, Plan §3, §9-Auflösungen DR-F1/F2/F3, DR-R1, IDR-1..4, IPR-1/2).
- Berührte `LH-*`: `LH-FA-AUD-001/002/003/004`, `LH-QA-02/03/06`.
- ADRs: `ADR-0001`/`ADR-0003` (Hexagonal/HexSlice), `ADR-0002` (JVM-Ziel, Dep am
  Rand), `ADR-0004`/`ADR-0006` (Coverage-Gate + Scope), `ARC-06`/`ARC-08`.
- `AGENTS.md §3` (Hard Rules) + Source Precedence (`harness/README.md`).
- Ist-Code der berührten Safety-Verträge: `AuditPort`, `EreignisProtokoll`,
  `Ereignis` (sealed), `BeliefState`, `Rekonstruktion`, `Zeitstempel`.
- Vorherige Findings am Modul: `2026-07-09-slice-041-{design,plan}-review*.md`
  (Erst-Lauf, Rerun, Rerun2, Independent).

---

## Findings

Reihenfolge **HIGH zuerst** (Skill §HIGH zuerst). Es gibt **keine HIGH- und keine
MEDIUM-Findings**: der Code implementiert die in `§9` aufgelösten Entscheidungen
getreu (throws statt stiller Leer-Rückgabe, `EreignisProtokoll.von(...)` statt
reimplementierter Append-only-Regel, Trailing-Toleranz vs. Interior-Wurf,
per-Modul-`kover`-Floor 90, Dockerfile-Enumeration an allen Stellen).

### F-1 — Enum-`quelle` wird unescaped serialisiert (Invarianten-Doku überbreit)

- `kategorie`: LOW
- `quelle`: Maintainability
- `pfad`: `adapters/outbound/audit-file/src/main/kotlin/dev/beliefagent/adapter/audit/file/EreignisSerialisierung.kt:65` (Kodierung) vs. `:204` (Dekodierung via `reqS`/`unesc`) vs. KDoc `:37`
- `befund`: Die Klassen-KDoc sagt „Werte sind escaped (`\`, Tab, Newline, CR)",
  doch `BeobachtungErfasst.quelle` wird als `quelle.name` **roh** (ohne `esc`)
  geschrieben, während die Gegenseite `reqS` (`unesc`) anwendet. Für den
  `Quelle`-Enum ohne Sonderzeichen ist der Round-Trip korrekt; die dokumentierte
  „alle Werte escaped"-Invariante gilt an dieser einen Stelle aber nicht.
- `verifizierbar`: nein — kein bestehender Gate-Lauf prüft die Escaping-Symmetrie
  einzelner Felder (Round-Trip-Test deckt nur den heutigen Enum-Wertebereich).

### F-2 — Companion-Pflicht: Code-Safety-Review noch offen

- `kategorie`: INFO (Rollen-/Durchgangs-Verweis — eigener Safety-Durchgang)
- `quelle`: Reviewer-Skill §„Repo-Zusatz — Code-Safety-Review"; Hard Rule 3.7/3.8; `LH-QA-02`
- `pfad`: `adapters/outbound/audit-file/**` (Audit/Ereignisprotokoll = Sicherheitsfunktion)
- `befund`: Der Diff berührt die Sicherheitsfunktion Audit/Ereignisprotokoll;
  der Skill verlangt dafür einen **eigenen** Durchgang mit Fokus
  fail-closed/Nicht-Umgehbarkeit als getrenntes Artefakt
  `docs/reviews/2026-07-09-slice-041-code-safety-review.md` (etablierte Praxis
  `slice-035`..`040`). Dieser Code-Review ersetzt ihn nicht.
- `verifizierbar`: nein — Prozess-/Harness-Artefakt, kein `make`-Gate.

### F-3 — Trailing-Truncation ist nur out-of-band sichtbar (kein Rückgabe-Signal)

- `kategorie`: INFO (Read-Pfad — Folgeslice-Verweis)
- `quelle`: `LH-QA-06`, `LH-QA-02`; Plan §9 IDR-2 / DR-R1
- `pfad`: `adapters/outbound/audit-file/src/main/kotlin/dev/beliefagent/adapter/audit/file/DateiAudit.kt:46` (Default `System.err`), `:105-110`
- `befund`: Bei toleriertem abgeschnittenem Trailing-Record liefert `lade()` die
  N-1 Ereignisse und meldet den Verlust nur über den `warnung`-Seitenkanal
  (Default `System.err`); der Rückgabetyp trägt **kein** Vollständigkeits-Signal.
  Konform zur Plan-Entscheidung (§9 IDR-2, Sichtbarkeit über Warn-Kanal, für
  Tests injizierbar) — beobachtet für den benannten Read-Fail-Eskalations-
  Folgeslice (slice-052), sobald ein Konsument auf Protokoll-Vollständigkeit
  gatet.
- `verifizierbar`: ja — `make test` (`trailing_truncation_*`-Tests belegen
  Warn-Kanal-Sichtbarkeit und N-1-Rekonstruktion).

### F-4 — Diff untracked/uncommittet: Traceability-Gates erst nach Commit prüfbar

- `kategorie`: INFO (Verweis: Doku-Regel / Verifier)
- `quelle`: `AGENTS.md §5` (IDs in PR/Commit), `MR-006` (range-basierte Gates)
- `pfad`: gesamter slice-041-Diff (`adapters/outbound/audit-file/` untracked)
- `befund`: Das Adaptermodul ist noch untracked; `make doc-commits`/
  `make doc-immutable` und die „IDs-im-Commit"-Regel (`AGENTS.md §5`) sind erst
  nach dem Commit auswertbar. Der Commit muss die berührten IDs referenzieren
  (`LH-FA-AUD-001..004`, `LH-QA-02/03/06`, `ADR-0001/0002/0003/0004/0006`,
  `ARC-06/08/09`).
- `verifizierbar`: ja — `make doc-commits` (range-basiert, nach Commit).

## Negativbefunde

- geprüft, ohne Befund: `hexagon/domain/**` (unverändert; `EreignisProtokoll`,
  `Ereignis` sealed-exhaustiv über 12 Typen, `BeliefState.of`-Normierung,
  `Rekonstruktion`, `Zeitstempel: Comparable` — vom Adapter korrekt genutzt).
- geprüft, ohne Befund: `hexagon/application/ports/AuditPort.kt` (unverändert;
  `anhaengen`/`lade`-Vertrag wie in §9 DR-F1 belassen, **kein** `Result`-Umbau).
- geprüft, ohne Befund: `adapters/outbound/audit-file/src/main/**` —
  Fehlerpfade werfen ausnahmslos `AuditPersistenzFehler` (nie stille Leer-
  Rückgabe, `LH-QA-02`); Append-only nicht reimplementiert (`EreignisProtokoll.von`,
  §9 DR-F3); Interior-Defekt/Header/Ordnung → Wurf, Trailing → Toleranz (§9 IDR-2);
  `when` ohne `else` erzwingt Serialisierungs-Abdeckung aller `Ereignis`-Typen;
  `=`/Tab/Newline/`null`-Marker-Kollisionen durch Escaping + Präsenz-Marker
  abgedeckt.
- geprüft, ohne Befund: `adapters/outbound/audit-file/src/test/**` — Negativäste
  breit abgedeckt (Header fehlt, Interior-Korruption, Ordnungsverletzung,
  Schreib-/Lesefehler, unbekannter Tag, Pflichtfeld, Escape-Fehler,
  nicht-normierter Belief, Präsenz-Marker) — `LH-QA-03`.
- geprüft, ohne Befund: `adapters/outbound/audit-file/build.gradle.kts` —
  keine neue Build-Abhängigkeit (nur `kotlin("jvm")` + `kover`), per-Modul-
  `kover { … minBound(90) }` (Adapter-Floor, `ADR-0006`), konform `ADR-0002`.
- geprüft, ohne Befund: `.a-check.yml` — Rolle `outbound_audit_file`, Kanten
  → `application`/→ `domain`, Resolution-Root gesetzt; Kern bleibt adapterfrei
  (`ADR-0001/0003`).
- geprüft, ohne Befund: `Dockerfile` — Modul an **allen** Enumerationsstellen
  (`COPY`, `:dependencies`, `:test`, `:koverLog`, `:koverVerify`) ergänzt
  (`ADR-0006`, kein Auto-Inherit).
- geprüft, ohne Befund: `settings.gradle.kts` — Modul registriert.
- geprüft, ohne Befund: `docs/user/{integration,cli-entscheidungsnachweis}.md` —
  spiegeln Code + Plan korrekt; `MemoryAudit` **explizit** CLI-Default, keine
  versteckte Umbindung eines werfenden Adapters (konsistent DoD §2 / §6).
- keine Suppression (`@Suppress`/`noqa`/`nolint`) im neuen Modul (Hard Rule 3.2).

## Summary

| Kategorie | Anzahl |
|---|---|
| HIGH | 0 |
| MEDIUM | 0 |
| LOW | 1 |
| INFO | 3 |

## Verdikt

**Merge-blockierend (Code-Review):** nein — keine HIGH/MEDIUM. F-1 (LOW) ist
nice-to-fix; F-2/F-3/F-4 (INFO) sind Verweise auf zuständige Rollen/Durchgänge
bzw. Folgeslices, keine Code-Korrektur an diesem Diff.

**Vorbehalt (Durchgang, nicht Kategorie):** „review-vollständig" ist dieser Slice
erst mit dem **Code-Safety-Review** (F-2) — für einen Audit-berührenden Diff im
Safety/Control-Repo ist der eigene fail-closed-Durchgang Skill-Pflicht, nicht
optional.

**Übergabe:** Findings gehen an die Implementation (Rückkante Review → Plan nur
bei Plan-Defekt — hier keiner). Der Report ersetzt **keine** Verifikation:
DoD-/Restart-Replay-Matrix und `make gates` prüft der **Verifier** separat
(Modul 11, `docs/verifications/*`, anderer Eingabe-Kontext) — bewusst **nicht**
in diesem Reviewer-Lauf ausgeführt (Rollentrennung Modul 8).
