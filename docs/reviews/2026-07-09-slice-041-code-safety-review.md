# Review-Report: slice-041 Code-/Safety-Review (Frischkontext) — 2026-07-09

**Review-Art:** Code-Safety — eigener fail-closed-/Nicht-Umgehbarkeits-Durchgang
für einen **Audit-/Ereignisprotokoll**-berührenden Diff (Sicherheitsfunktion),
Reviewer-Skill §„Repo-Zusatz — Code-Safety-Review" (etablierte Praxis
`slice-035`..`040`). Ergänzt den allgemeinen slice-041-Code-Review vom selben Tag
(dort Finding F-2: dieser Durchgang war offen). Verlinkung nachgezogen, sobald
beide Review-Artefakte mit dem Slice-Diff committet sind (F-4).

**Kontext-Trennung (Modul 8):** unabhängiger Frischkontext-Lauf — der Reviewer hat
den Code **nicht** geschrieben und kannte die Implementierungs-Begründungen nicht.
Genau diese Trennung findet den unten stehenden HIGH-Befund, den der
Autor-Kontext als „bekannte Grenze" unterschätzt hatte.

**Gegenstand:** neues Modul `adapters/outbound/audit-file/`
(`DateiAudit.kt`, `EreignisSerialisierung.kt`, `AuditFehler.kt`, 2 Testdateien,
`build.gradle.kts`).

**Skill:** `.harness/skills/reviewer.md` @ v1.0 <!-- d-check:ignore (Skill-Pfad) -->
**Modell:** claude-opus-4-8[1m] (Frischkontext-Subagent) · **Datum:** 2026-07-09

**Eingangs-Kontext:**

- Slice-Plan `docs/plan/planning/in-progress/slice-041-dauerhafte-audit-datenbank.md`
  (§2 DoD, §9 DR-F1/F3, DR-R1, IDR-1/2/3/4).
- `LH-FA-AUD-001/002/003`, `LH-QA-02/03`.
- Safety-Verträge: `AuditPort`, `EreignisProtokoll`, `Ereignis` (sealed),
  `Rekonstruktion`.
- `.harness/skills/reviewer.md` (Klassifikation, §„Was dieser Skill NICHT macht").

---

## Findings

Reihenfolge **HIGH zuerst**.

### HIGH-1 — `anhaengen()` nach toleriertem Trailing-Truncation-Record erzeugt einen dauerhaft korrupten Store (Byte-Verklebung über die Record-Grenze)

- `kategorie`: HIGH
- `quelle`: `LH-FA-AUD-001` (Append-only/kein Löschen), `LH-FA-AUD-002`
  (rekonstruierbar), `LH-QA-02` (fail-open im Write); widerspricht Plan §9 **IDR-2**
- `pfad`: `adapters/outbound/audit-file/src/main/kotlin/dev/beliefagent/adapter/audit/file/DateiAudit.kt:53` und `:62-67` (Append ohne Record-Grenzen-Guard) i. V. m. `:96-101` (Trailing-Toleranz)
- `befund`: Nach einem tolerierten Trailing-Truncation-Zustand endet die Datei
  ohne `\n` (genau der von IDR-2 adressierte „Crash-während-`anhaengen`"-Fall).
  `anhaengen()` prüft nur `!exists || size==0` für den Header und schreibt dann
  `zeile + '\n'` per `APPEND` direkt ans EOF — also an den newline-losen
  Fragment-Rest. Das Fragment verklebt mit dem neuen Record zu **einer** Zeile;
  das nächste `lade()` sieht keine Trailing-Zeile mehr (`hatAbschluss=true`),
  keine Warnung, und dekodiert die verklebte Zeile als vollständigen Record.
  **Regelfall:** unbekannter/`=`-loser Merge-Tag → `dekodiere` wirft → `lade()`
  wirft **dauerhaft**; die zuvor rekonstruierbaren N-1 Records sind über den
  Adapter nicht mehr ladbar (`LH-FA-AUD-002` gebrochen, Ereignisse faktisch
  gelöscht — `LH-FA-AUD-001`). **Silent-Zweig:** bei Truncation mitten im
  Feld-Wert bleibt jeder Token `=`-haltig; über `LinkedHashMap`-Last-Wins
  dekodiert die Zeile **ohne Wurf und ohne Warnung** zu einem fabrizierten, nie
  angehängten Ereignis (z. B. Approval-behaftet) — stiller Integritätsbruch
  (`LH-QA-02`).
- `verifizierbar`: ja — `make test` (Fixture „Header + N Records + newline-loser
  Rest → `anhaengen` → `lade`"). Ein solcher Test fehlt (siehe MEDIUM-2).
- `Binding-Kontext (ehrlich)`: Der Adapter ist in diesem Slice **nicht** produktiv
  gebunden (alle Composition-Roots nutzen `MemoryAudit`, `Runtime.kt`), Plan §9
  IDR-1 → **Live-Betriebsrisiko heute 0**, Lücke latent. Die Kategorie HIGH bemisst
  die Wirkung auf die Audit-Invariante des Artefakts gegen seine eigene DoD, nicht
  die aktuelle Betriebsexposition.

### MEDIUM-1 — Write-Pfad erzwingt die Append-only-Ordnung nicht; Divergenz zu `MemoryAudit`, stiller Poison-Store bei Rückdatierung

- `kategorie`: MEDIUM
- `quelle`: `LH-FA-AUD-001`, `AuditPort`-Vertrag
- `pfad`: `DateiAudit.kt:49-71` (`anhaengen` liest den bestehenden letzten Zeitstempel nie)
- `befund`: `DateiAudit.anhaengen` hängt blind an; `MemoryAudit.anhaengen` geht
  über `EreignisProtokoll.append` und **wirft am Write** bei Rückdatierung. Ein
  rückdatiertes `anhaengen` wird von `DateiAudit` still akzeptiert und persistiert;
  der Store wirft erst beim nächsten `lade()` — dann dauerhaft. Zwei
  `AuditPort`-Impls verhalten sich am selben Aufruf unterschiedlich.
- `verifizierbar`: ja — `make test`.
- **Spannung zu §9 DR-F3:** DR-F3 verankert die Ordnungsprüfung **bewusst** am
  Load-Pfad und verbietet eine Doppel-Quelle der Regel im Adapter. Eine
  Write-Zeit-Prüfung würde genau diese Doppel-Quelle einführen → **Architect-/
  Planner-Klärung** (Rückkante Review→Plan), keine stille Adapter-Änderung.

### MEDIUM-2 — Fehlender Negativtest auf dem Resume-nach-Crash-Pfad (Append nach Trailing-Truncation)

- `kategorie`: MEDIUM
- `quelle`: `LH-QA-03`; Reviewer-Skill „Fehlende Negativtests bei Safety-Pfad"
- `pfad`: `DateiAuditTest.kt` (gesamte Suite)
- `befund`: Getestet werden Trailing-Truncation-**Lese**-Toleranz und
  Interior-Korruption, aber **nie** die Sequenz „truncierter Store → `anhaengen`
  → `lade`" — genau der Crash-mid-write-und-danach-weiterschreiben-Pfad. Die Lücke
  lässt HIGH-1 durch die Gates.
- `verifizierbar`: ja — `make test`.

### INFO-1 — `lade()` lädt die gesamte Datei unbegrenzt in den Speicher

- `kategorie`: INFO
- `quelle`: Maintainability / Ressourcen
- `pfad`: `DateiAudit.kt:76` (`Files.readAllBytes`), `:82` (`String(bytes)`)
- `befund`: Keine Größengrenze; ein sehr großer Store führt zu OOM beim Laden.
  **Nicht** angreifer-amplifiziert (proportional zu real geschriebenen Bytes). Für
  einen lokalen Single-Writer-Store vertretbar; kein Aktionsbedarf in diesem Slice.
- `verifizierbar`: nein.

## Negativbefunde

- `lade()`-Lesepfad (fail-closed): kein Fail-open-Pfad, der bei Korruption still
  leer zurückgibt. `LEER` **nur** bei nicht-existenter Datei und leeren Bytes
  (beide legitim leer); fehlender/ungültiger Header, Interior-Defekt, IO-Fehler,
  Ordnungsverletzung werfen sichtbar. Geprüft, außer der in HIGH-1/MEDIUM-1
  genannten Write→Read-Interaktion.
- Append-only-Ordnung nicht dupliziert: Protokoll ausschließlich über
  `EreignisProtokoll.von(...)` gebaut, dessen `IllegalArgumentException` in
  `AuditFormatFehler` umgesetzt; Regel nicht reimplementiert (DR-F3 auf **Load**
  erfüllt; Einschränkung Write-Pfad = MEDIUM-1).
- Serialisierungs-Grenzen (kein Boundary-Forging): `esc` deckt `\`, Tab, Newline,
  CR; `=` über `indexOf('=')` (erstes Vorkommen) getrennt; Nullable-Marker
  (`null`/`s:`) kollisionsfrei. Geprüft, sauber.
- Header-/Leer-Handling: nur exakter Header `beliefaudit/v1`; leere Datei,
  nur-Header, Header-ohne-Newline legitim leer; truncierter/fehlender Header wirft.
  Geprüft, sauber.
- DoS/Allokation via `hn`/`evn`: `< 0` wirft; großes `hn` löst keine
  Vorab-Allokation aus (`IntRange`), schlägt am ersten fehlenden indizierten Feld
  schnell fehl. Kein HIGH.
- Trailing-Truncation-Grenze (Read): nur das einzelne newline-lose Endelement wird
  toleriert/verworfen+gewarnt; alle terminierten Records werden validiert,
  Interior-Defekt wirft. Reiner Lesepfad wasserdicht; die Schwäche liegt im
  nachfolgenden Write (HIGH-1).
- Tamper-Evidenz / Multi-Writer: laut §9 IDR-3/IDR-4 explizit out of scope; Code
  adressiert sie nicht (konform zur dokumentierten Grenze).
- Produktiv-Binding: `DateiAudit` außerhalb des Moduls nirgends verdrahtet; alle
  Composition-Roots binden `MemoryAudit`. §9 IDR-1 bestätigt.
- Architektur/ADR: `build.gradle.kts` importiert nur `:hexagon:domain`/
  `:hexagon:application`; keine Kern→Adapter-Kante, keine neue Build-Abhängigkeit
  (ADR-0002/0003 konform). Arch-Durchsetzung `make arch-check` nicht in diesem
  Lauf ausgeführt.

## Summary

| Kategorie | Anzahl |
|---|---|
| HIGH | 1 |
| MEDIUM | 2 |
| LOW | 0 |
| INFO | 1 |

## Verdikt

**Merge-blockierend: JA** — wegen HIGH-1. Der Auslöser ist kein Missbrauch, sondern
der von §9 IDR-2 ausdrücklich adressierte Normalfall „Crash während `anhaengen`,
danach Neustart und Weiterschreiben": der erste Post-Crash-Append verklebt über die
Record-Grenze und macht den Store entweder dauerhaft unladbar (`LH-FA-AUD-001/002`)
oder fabriziert still einen nie angehängten Datensatz (`LH-QA-02`). Beide
Reviewer-HIGH-Anker (fail-open **und** Audit-Invariante) sind getroffen.

Live-Betriebsrisiko heute 0 (Adapter unverdrahtet, `MemoryAudit`-Default). MEDIUM-1
(Write-Ordnungs-Divergenz, in Spannung zu DR-F3) und MEDIUM-2 (Resume-Test) sind
vor einer produktiven Bindung zu klären.

**Übergabe:** Findings an die Implementation. HIGH-1/MEDIUM-2 = Code-Fix + Test;
MEDIUM-1 = Rückkante Review→Plan (Architect/Planner, DR-F3-Spannung); INFO-1 =
akzeptierte Grenze. Der Report ersetzt keine DoD-Verifikation (Verifier, Modul 11).

## Geschichte

| Version | Datum | Änderung |
|---|---|---|
| 1 | 2026-07-09 | Initialer Frischkontext-Code-Safety-Lauf (Modul 8). HIGH-1 (Write-nach-Truncation-Korruption), MEDIUM-1/2, INFO-1. |
