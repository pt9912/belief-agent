# Review-Report: slice-041 Design-Review — 2026-07-09

**Review-Art:** Design — geprueft *wogegen*: Architektur (Layer-Grenzen,
Schnittstellen, ADR-Vertraeglichkeit des Loesungs-Schnitts), **bevor** die
Details festgezurrt sind (Modul 10 §Drei Review-Arten).

**Gegenstand:** `docs/plan/planning/open/slice-041-dauerhafte-audit-datenbank.md`
(Loesungs-Schnitt: persistenter Outbound-Adapter hinter `AuditPort`, `ARC-06`/`ARC-08`)

**Skill:** `.harness/baseline/v1.4.0/regelwerk/modul-10-review-harness.md` @ v1.4.0
**Modell:** Claude Opus 4.8 (1M) · **Datum:** 2026-07-09

**Eingangs-Kontext** (die Vertraege/Ist-Strukturen, gegen die geprueft wurde):

- `docs/plan/planning/open/slice-041-dauerhafte-audit-datenbank.md` (Plan)
- `spec/architecture.md`: `ARC-06`, `ARC-08`, `ARC-09`; §2 Rollen/erlaubte Importe
- `docs/plan/adr/0001-hexagonal-llm-port.md` (Kern adapterfrei),
  `0002-implementierungssprache-jvm-java.md` (KMP-Source-Sets, DI/Dep am Rand),
  `0003-hexslice-architektur.md` (HexSlice-Rollen, a-check)
- `hexagon/application/src/commonMain/.../ports/AuditPort.kt` (Port-Vertrag)
- `adapters/outbound/audit-memory/.../MemoryAudit.kt` (bestehender Adapter)
- `hexagon/domain/src/commonMain/.../EreignisProtokoll.kt`,
  `.../Rekonstruktion.kt` (Domain-Invarianten)
- `settings.gradle.kts`, `.a-check.yml` (Modul-/Rollen-/Root-Realitaet)
- `docs/plan/planning/done/slice-040-approval-audit-persistenz.md`
  (fail-closed-Audit-Praezedenz), `docs/reviews/2026-07-08-slice-040-design-review.md`

---

## Findings

### F-1 — Fail-closed-Fehlersemantik nicht auf den `AuditPort`-Vertrag abgebildet

- `kategorie`: MEDIUM
- `quelle`: `ARC-06`, `ADR-0001`/`ADR-0003`, `LH-QA-02`, `LH-FA-AUD-002`/`003`
- `pfad`: `docs/plan/planning/open/slice-041-dauerhafte-audit-datenbank.md:34`
  (vgl. `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/ports/AuditPort.kt:24`)
- `befund`: Die DoD fordert, Schreib-/Lesefehler seien „fail-closed sichtbar"
  und ein korruptes/teilgeschriebenes Protokoll erzeuge „einen sichtbaren Fehler
  statt einer gate-faehigen Teilhistorie" — nicht still als leeres Protokoll
  (`:34`, `:39`). Der bestehende Vertrag hat aber keinen Fehlerkanal:
  `anhaengen(ereignis): Unit` und `lade(): EreignisProtokoll` koennen einen
  Fehler nur als **geworfene Exception** ausdruecken. Der Plan legt nicht fest,
  ob die Sichtbarkeit adapterlokal ueber Wurf bleibt (konsistent zur
  slice-040-Praezedenz „Audit-Ausfall fail-closed" in `AktionGaten`) oder ob der
  Port-Vertrag geschaerft wird (Result/dokumentierte Ausnahme) — Letzteres
  beruehrt den hexagon-Kern, den zweiten Implementierer `MemoryAudit` und die
  `lade()`/`anhaengen()`-Konsumenten (`Runtime`, `AktionGaten`) und waere breiter
  als „kleinster dauerhafter Audit-Adapter".
- `verifizierbar`: ja — `make arch-check` (kein Kern→Adapter) plus Code-Review
  der `AuditPort`-Signatur und der slice-040-Tests „Audit-Ausfall fail-closed"
  zeigen, ob die Lese-/Schreib-Fehlersichtbarkeit vertragsseitig getragen ist.

### F-2 — Source-Set-Platzierung und IO-/Serialisierungs-Abhaengigkeit unentschieden (`ADR-0002`)

- `kategorie`: MEDIUM
- `quelle`: `ADR-0002` (KMP-Source-Sets, Dependency am Rand), `ADR-0003`, `ARC-08`
- `pfad`: `docs/plan/planning/open/slice-041-dauerhafte-audit-datenbank.md:58`
  (`.a-check.yml`-Zeile) i. V. m. `:32` (Storage-Technologie)
- `befund`: Alle reinen Adapter (`audit-memory`, `konfidenz-memory`,
  `approval-*`) liegen in `commonMain`; `MemoryAudit` macht keine IO. Ein
  persistenter Adapter mit echter Datei-/DB-IO passt nicht in `commonMain` mit
  reiner Kotlin-Stdlib. Der beobachtbare Options-Raum gabelt die Architektur:
  (a) `jvmMain`/`src/main` + `java.nio` (Praezedenz `observation-build-report`,
  `observation-git-local`, `llm-langchain4j` liegen unter `src/main`) — keine
  neue Dependency, JVM-only, `.a-check.yml`-Root `.../src/main/kotlin/...`;
  (b) `commonMain` + multiplattform-IO/Serialisierung (kotlinx-io/okio,
  kotlinx.serialization-Plugin) — **neue Build-Abhaengigkeit/Plugin** →
  `ADR-0002`-Toolchain-Flaeche → Folge-ADR, `.a-check.yml`-Root
  `.../src/commonMain/kotlin/...`. Der Plan nennt „`.a-check.yml` aktualisieren"
  (`:58`), legt aber Source-Set und Dependency-Flaeche — die genau den Root-Pfad
  und die ADR-Betroffenheit bestimmen — nicht fest.
- `verifizierbar`: ja — `make build` (KMP-Source-Set-Sichtbarkeit) und
  `make arch-check` (Root-Pfad je Rolle) bestaetigen die getroffene Wahl, sobald
  Modul/Root existieren.

### F-3 — Validierungs-Schicht: Adapter uebernimmt Domain-Invarianten des Ereignisprotokolls

- `kategorie`: MEDIUM
- `quelle`: `ARC-01` (Domain), `ADR-0003` (Layer-Platzierung), Maintainability
- `pfad`: `docs/plan/planning/open/slice-041-dauerhafte-audit-datenbank.md:39`
  (vgl. `hexagon/domain/src/commonMain/kotlin/dev/beliefagent/domain/belief/EreignisProtokoll.kt:30`,
  `.../Rekonstruktion.kt:16`)
- `befund`: DoD-Punkt 4 weist dem **Adapter** zu, die Ereignisfolge vor der
  Rueckgabe zu validieren — u. a. „Rueckdatierung" und „nicht rekonstruierbare
  Sequenzen" (`:39`). Beides sind bereits bestehende **Domain-Invarianten**:
  `EreignisProtokoll.append`/`von` weisen ein rueck-datiertes Ereignis mit
  `IllegalArgumentException` zurueck, und `lade(): EreignisProtokoll` kann ein
  gueltiges Protokoll nur ueber diese Domain-Konstruktoren erzeugen;
  Rekonstruierbarkeit ist Aufgabe von `Rekonstruktion`. Der Plan trennt nicht
  zwischen Deserialisierungs-Integritaet (Adapter: unbekannter Typ-Tag, fehlendes
  Pflichtfeld) und Sequenz-/Rekonstruktions-Gueltigkeit (Domain) — das Risiko ist
  eine Doppel-Quelle der append-only-Invariante im Adapter (Drift in einer
  Safety-relevanten Spur). Dies spiegelt das slice-040-Muster (Design-Review F-1:
  unklar, welche Schicht die vollstaendige Spur besitzt).
- `verifizierbar`: ja — Code-Review, ob der Adapter das rekonstruierte Protokoll
  ueber `EreignisProtokoll.von(...)` baut (Domain erzwingt Ordnung) statt die
  Ordnungs-/Rekonstruktionspruefung selbst zu implementieren; `make arch-check`
  bestaetigt die Adapter→domain-Kante.

## Negativbefunde

- geprueft, ohne Befund: **Outbound-Adapter hinter bestehendem Port** ist der
  richtige Schnitt (`ARC-08`/`ADR-0003`); der Happy-Path erfordert keine
  Kern-Aenderung — `AuditPort` ist bereits anwendungsweit und adapterfrei gefuehrt.
- geprueft, ohne Befund: **Append-only-Ordnung** ist als Domain-Invariante
  vorhanden (`EreignisProtokoll`); ein Adapter, der ueber `von(...)` baut, erbt
  die „kein Rueck-Datieren"-Garantie ohne Reimplementierung (setzt F-3 voraus).
- geprueft, ohne Befund: **`MemoryAudit` bleibt** als deterministischer
  Test-/Demo-Stand-in nutzbar (`LH-QA-03`); der Plan bindet den CLI-Default
  bewusst nicht um (`:46`, `:113`) — kein voreiliger Runtime-Wechsel.
- geprueft, ohne Befund: **Retention/Migration/Backup/Compliance-Export** sind
  aus dem Design-Schnitt ausgeklammert (`:22`, `:90`) — keine verfruehte
  Modul-/Format-Bindung ueber die in F-2 markierte Entscheidung hinaus.
- geprueft, ohne Befund: **DI/Framework am Rand** (`ADR-0002`, `.a-check.yml`
  `tech`-Block) nicht beruehrt; der Adapter fuehrt kein `org.koin`/`io.*` in den Kern.
- geprueft, ohne Befund: **`ARC-09`-Composition** korrekt als separater
  Folgeschritt behandelt; der Adapter-Slice bleibt ohne produktives CLI-Binding
  pruefbar.

## Ausgefuehrte Sensoren

- Kontext-Lesung (`Read`/`grep`/`find`) ueber `AuditPort`, `MemoryAudit`,
  `EreignisProtokoll`, `Rekonstruktion`, `settings.gradle.kts`, `.a-check.yml`,
  `architecture.md`, `ADR-0001/0002/0003`, slice-040 (+ dessen Design-Review) —
  gelesen; Source-Set-Realitaet je Adapter aus dem Verzeichnisbaum verifiziert.
- `make`-Gates: **nicht** ausgefuehrt (Design-Review ohne Diff). Die drei
  MEDIUM-Befunde sind erst nach der Design-Entscheidung mit `make build`/
  `make arch-check` gate-bestaetigbar (Feld `verifizierbar` je Finding).

## Summary

| Kategorie | Anzahl |
|---|---:|
| HIGH | 0 |
| MEDIUM | 3 |
| LOW | 0 |
| INFO | 0 |

## Verdikt

**Merge-blockierend:** ja — die drei MEDIUM (Port-Fehlersemantik, Source-Set/
Dependency, Validierungs-Schicht) sollten **vor Implementierungsstart** geklaert
werden; frueher = billiger (Modul 10). F-2 kann einen **Folge-ADR** ausloesen
(neue Storage-/IO-/Serialisierungs-Abhaengigkeit unter `ADR-0002`) — genau das
Gate, an das der Plan die Technologie-Wahl bindet (`:32`, `:71`).

**Uebergabe:** Findings gehen an die Planung/Design-Klaerung (Rueckkante
Design → Plan). Als Auffuehrungs-Artefakt bei ADR-Bedarf dient die Folge-ADR-
Huelle (`docs/plan/adr/NNNN-titel.template.md`, Modul 8 §Folge-ADR). Der Report
kategorisiert nur (Modul 10) — die Wahl zwischen den beobachteten Optionen und
ihre Umsetzung liegen bei Architect/Implementation, nicht beim Reviewer. Keine
Verifikation gegen DoD/Spec — das ist Verifier-Aufgabe (Modul 11).
