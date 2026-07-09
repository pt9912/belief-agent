# Review-Report: slice-041 Design-Review (Unabhaengiger Frischkontext-Lauf) — 2026-07-09

**Review-Art:** Design — geprueft *wogegen*: Architektur (Layer-Grenzen, Ports,
ADR-Vertraeglichkeit des Loesungs-Schnitts), **bevor** die Details festgezurrt
sind (Modul 10 §Drei Review-Arten).

**Gegenstand:** `docs/plan/planning/open/slice-041-dauerhafte-audit-datenbank.md`
(Loesungs-Schnitt: persistenter Outbound-Adapter hinter `AuditPort`, `ARC-06`/`ARC-08`).

**Lauf-Charakter:** **Unabhaengiger Frischkontext-Reviewer** (Modul 8), getrennt
von der Same-Context-Design-Kette (`…-design-review.md`, `…-rerun.md`,
`…-rerun2.md`), die „konvergiert, 0 Blocker / implementierungsreif" meldet. Die
Rerun-2-Selbstnotiz benennt explizit, dass „eine unabhaengige Frischkontext-Sicht
die staerkere Form bleibt" — genau die wird hier geliefert. Eigene Findings **vor**
Lesen der Kette gebildet, gegen den Ist-Code verifiziert.

**Skill:** `.harness/skills/reviewer.md` @ v1.0
**Modell:** unabhaengiger Reviewer (Claude Opus 4.8) · **Datum:** 2026-07-09

**Eingangs-Kontext** (Vertraege/Ist-Strukturen — Skill §Kontext-Eingang, Safety-Pfad):

- Slice-Plan (dritte Fassung, inkl. §9)
- `spec/architecture.md`: `ARC-06/08/09`, §2 Rollen/erlaubte Importe, §5 Fehlermodelle
- `docs/plan/adr/0001-hexagonal-llm-port.md`, `0002-implementierungssprache-jvm-java.md`, `0003-hexslice-architektur.md`, `0004-coverage-gate.md`, `0006-coverage-gate-scope.md`
- `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/ports/AuditPort.kt` (Vertrag)
- `hexagon/domain/src/commonMain/kotlin/dev/beliefagent/domain/belief/EreignisProtokoll.kt`, `.../Rekonstruktion.kt` (Domain-Invarianten)
- **Write-Konsumenten:** `.../gaten/AktionGaten.kt`, `.../aktionsvorschlag/AktionsVorschlagen.kt`, `.../konfidenzexternalisieren/KonfidenzExternalisieren.kt`, `example/code-agent/.../Main.kt`
- **Read-Konsumenten:** `adapters/inbound/cli/.../Runtime.kt`, `example/code-agent/.../Main.kt`
- `adapters/outbound/audit-memory/.../MemoryAudit.kt`, `settings.gradle.kts`, `.a-check.yml`, `Dockerfile`
- **vorherige Findings:** die drei Same-Context-Design-Reports (erst NACH eigener Findingsbildung gelesen)

---

## Findings

### IDR-1 — Adapter-Wurf erreicht kein **einheitlich sichtbares** fail-closed ueber die Write-Konsumenten

- `kategorie`: MEDIUM
- `quelle`: `ARC-06` (Audit-Port-Vertrag), `LH-QA-02` (Sichtbarkeit/fail-safe), `LH-FA-AUD-001` (Ereignis protokolliert); Skill §Klassifikation („stiller … leeres Ergebnis, wo ein sichtbarer Fehler/Eskalation gehoert")
- `pfad`: `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/aktionsvorschlag/AktionsVorschlagen.kt:89`
- `befund`: Die Design-Entscheidung (§9 DR-F1) haelt den `AuditPort`-Vertrag
  bewusst throw-basiert (kein `Result`-Umbau), **begruendet** damit, dass der
  Konsument die geworfene `anhaengen`-Exception fail-closed abfaengt. Diese
  Begruendung traegt architektonisch nur fuer **einen** von vier
  Write-Konsumenten. `AktionsVorschlagen` kapselt den `anhaengen`-Aufruf
  (`:89`) in `runCatching { … }.getOrNull()` (`:57`/`:91`): ein
  Adapter-Schreibfehler wird **verschluckt**, der Vorschlag verschwindet nach
  `null`, der Zyklus liefert `Abgelehnt` — das Pflicht-Ereignis „Aktion
  vorgeschlagen" (`LH-FA-AUD-001`) ist **verloren und unsichtbar**, obwohl der
  Adapter „Sichtbarkeit an der Adapter-Grenze" korrekt geworfen hat. Der
  Sichtbarkeits-Effekt des Wurfs endet am `runCatching` des Konsumenten. Zudem
  wird `konfidenzen.anhaengen(eintrag)` (`KonfidenzExternalisieren.kt:48`)
  **vor** dem verschluckten `audit.anhaengen` (`:49`) ausgefuehrt → moeglicher
  **Teilzustand** (KonfidenzPort geschrieben, Audit nicht) beim
  same-`runCatching`-Rollback. Damit ist die throw-basierte Vertrags-Wahl
  design-seitig unter-begruendet: der Wurf leistet die intendierte
  fail-closed-Sichtbarkeit auf dem primaeren Live-Vorschlagspfad **nicht**.
- `verifizierbar`: ja — `make test` mit Schreibfehler-Fixture auf dem
  Vorschlags-/Zykluspfad zeigt stilles `Abgelehnt` statt Eskalation; `make arch-check`
  bleibt orthogonal (Kern-Reinheit unberuehrt).

### IDR-2 — Restart-Rekonstruierbarkeit vs. „Wurf bei jedem Teil-Datensatz": Interaktion unbehandelt

- `kategorie`: MEDIUM
- `quelle`: `LH-FA-AUD-002` (Rekonstruierbarkeit), `LH-QA-06` (Inspizierbarkeit), `LH-QA-02`; DoD 1/3/4
- `pfad`: `docs/plan/planning/open/slice-041-dauerhafte-audit-datenbank.md:58`
- `befund`: DoD 4 legt fest, ein „defekter/teilgeschriebener Datensatz" fuehre bei
  `lade()` zu einem geworfenen Fehler „statt gate-faehiger Teilhistorie". DoD 1
  fordert zugleich „nach Prozess-Neustart rekonstruierbar geladen". Der Plan
  behandelt „Neustart-Laden" und „Teil-Write" als getrennte DoD-Punkte, **nicht
  ihre Interaktion**: der haeufigste Realfall — Prozess-Absturz **waehrend** eines
  Append — hinterlaesst genau einen abgeschnittenen **Trailing**-Datensatz; die
  Wurf-auf-jeden-Defekt-Regel macht dann `lade()` fuer das **gesamte** Protokoll
  hart fehlschlagen, obwohl N-1 valide Datensaetze vorliegen. Der Plan
  unterscheidet einen abgeschnittenen Trailing-Record (recovery-naher Absturzfall)
  nicht von Mitten-im-File-Korruption (Manipulation); beide → totaler
  Read-Ausfall. Das ist fail-safe (laut), steht aber in Spannung zum Slice-Zweck
  „nach Neustart rekonstruierbar" und zur `lade()`-getragenen `LH-QA-06`-
  Inspizierbarkeit. Der Options-Raum (Trailing-Partial tolerieren+markieren vs.
  bewusst total werfen) ist im Plan **nicht** benannt.
- `verifizierbar`: ja — `make test` mit Crash-mid-write-/Trailing-Torn-Record-Fixture
  zeigt, ob `lade()` das ganze Protokoll wirft oder N-1 rekonstruiert.

### IDR-3 — Append-only nur **logisch** erzwungen; keine Tamper-Evidenz gegen Out-of-Band-Aenderung

- `kategorie`: INFO
- `quelle`: `LH-FA-AUD-001` (unveraenderlich, nie ueberschrieben/geloescht); Threat-Model → Rollen-Verweis Architect
- `pfad`: `docs/plan/planning/open/slice-041-dauerhafte-audit-datenbank.md:36`
- `befund`: DoD 2 sichert „vorhandene Ereignisse werden nie ueberschrieben oder
  geloescht". Das ist gegen die **Adapter-eigenen** Operationen garantiert
  (append-only-API) und die Ordnung wird beim Laden ueber `EreignisProtokoll.von`
  re-validiert (`EreignisProtokoll.kt:30-39/49`). Die Deserialisierungs-Integritaet
  (DoD 4) faengt jedoch nur **unparse-/ordnungswidrige** Daten; ein
  **ordnungserhaltendes** externes Umschreiben (mittleren Datensatz loeschen, Rest
  gueltig geordnet neu schreiben) laedt **sauber und unentdeckt** — es gibt keine
  Tamper-Evidenz (kein Hash-Chain/Signatur) im gewaehlten „inspizierbaren
  Textformat". Die `LH-FA-AUD-001`-Unveraenderlichkeit gilt damit gegen die
  System-API, nicht gegen einen Akteur mit Dateizugriff. Ob das im Threat-Model
  dieses Safety/Control-Adapters liegt (oder mit „Compliance-Export"/Pfadpolitik
  Folgearbeit ist), ist eine **Architect**-Entscheidung — hier nur als offener
  Punkt benannt, nicht als Loesung vorgeschrieben.
- `verifizierbar`: nein — Threat-Model-/Scope-Frage; kein `make`-Gate bestaetigt Tamper-Evidenz.

### IDR-4 — Kein Nebenlaeufigkeits-/Single-Writer-Modell fuer einen dauerhaften Datei-Store spezifiziert

- `kategorie`: INFO
- `quelle`: Maintainability, `LH-QA-04`; `AuditPort.kt:19-26`
- `pfad`: `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/ports/AuditPort.kt:21`
- `befund`: Weder der `AuditPort`-Vertrag noch der Plan benennen ein
  Nebenlaeufigkeits-/Thread-Safety-/Single-Writer-Modell. `MemoryAudit`
  (`MemoryAudit.kt:14-20`) ist nicht thread-safe (schlichtes `var protokoll`);
  ein Datei-Adapter mit parallelen `anhaengen`-Aufrufen (Multi-Thread oder
  Multi-Prozess auf derselben Datei) koennte interleaven → korrupte/verschraenkte
  Datensaetze. Unter der heutigen **single-threaded sequentiellen** Laufzeit
  besteht kein akuter Bug; fuer eine als „dauerhafte Audit-Datenbank" betitelte
  Persistenz ist die Annahme (ein Prozess, ein Schreiber) aber **unausgesprochen**
  und unbewertet. Nur beobachtend benannt.
- `verifizierbar`: nein — Design-/Konventions-Beobachtung; kein Gate deckt Parallel-Writer heute ab.

## Negativbefunde

- geprueft, ohne Befund: **Schnitt** — Outbound-Adapter hinter bestehendem,
  adapterfreiem `AuditPort` (`ARC-06`/`ARC-08`, `ADR-0001/0003`) ist korrekt; der
  Happy-Path erfordert keine Kern-Aenderung. Kern-Reinheit (`hexagon:*` ohne
  Storage/IO/Adapter, kein `org.koin`/`io.*`) unberuehrt.
- geprueft, ohne Befund: **DR-F2 (Source-Set/Dep)** haelt — `observation-build-report`
  (`build.gradle.kts` `kotlin("jvm")`, `src/main`), `observation-git-local`,
  `llm-langchain4j` liegen real unter `src/main` (`.a-check.yml` roots
  `:102/:105/:106`); `java.nio` ohne neue Build-Dep ist gegen diese Praezedenz
  plausibel. Abweichung vom Schwester-Adapter `audit-memory` (`commonMain`) ist
  IO-bedingt korrekt und im Plan anerkannt.
- geprueft, ohne Befund: **DR-F3 (Validierungs-Schicht)** haelt —
  `EreignisProtokoll.von(...)` existiert und erzwingt die Append-only-Ordnung;
  der Adapter erbt „kein Rueck-Datieren" ohne Reimplementierung. Keine
  Doppel-Quelle der Ordnungs-Invariante (setzt IDR-2 nicht ausser Kraft — IDR-2
  betrifft die *Reaktion* auf einen Teil-Record, nicht die Ordnungslogik).
- geprueft, ohne Befund: **DR-R1 (Read-Pfad)** — die drei faktischen Praemissen der
  Kette (`auditEreignisse()` ohne Produktiv-Entscheider; `Rekonstruktion` nicht
  im Live-Gating-Pfad; `.lade()`-Konsumenten observability-only) sind **eigenstaendig
  per `grep` verifiziert — wahr**. Read-Fail landet nur in observability-only.
- geprueft, ohne Befund: **`MemoryAudit` bleibt** deterministischer Test-/
  Demo-Stand-in; CLI-Default bewusst nicht umgebunden (`Runtime.kt:296`).
- geprueft, ohne Befund: **`ARC-09`-Composition** korrekt als separater
  Folgeschritt; der Adapter-Slice bleibt ohne produktives CLI-Binding pruefbar.

## Ausgefuehrte Sensoren

- `Read`/`grep` ueber `AuditPort`, alle vier `anhaengen`-Konsumenten, beide
  `.lade()`-Konsumenten, `EreignisProtokoll`, `Rekonstruktion`, `MemoryAudit`,
  `architecture.md §2/§5`, `ADR-0001/0002/0003/0004/0006`, `settings.gradle.kts`,
  `.a-check.yml`, `Dockerfile`, `observation-build-report`/`audit-memory`
  `build.gradle.kts`.
- `grep "anhaengen"` → 4 Write-Konsumenten; `grep "\.lade()"` → 2 Read-Konsumenten;
  `grep "Rekonstruktion"` → keine Produktivaufrufe.
- `make doc-check` — **PASS** (`d-check` @ `sha256:3bbdb19b…`: **149 Datei(en) geprueft, 0 Befund(e)**; validiert auch diesen Report und den Plan-Review-Independent).

## Summary

| Kategorie | Anzahl |
|---|---:|
| HIGH | 0 |
| MEDIUM | 2 |
| LOW | 0 |
| INFO | 2 |

## Verdikt

**Merge-blockierend:** ja — **2 neue MEDIUM** (IDR-1 Write-Konsumenten-Sichtbarkeit,
IDR-2 Restart-vs-Teil-Write-Interaktion), die die Same-Context-Kette **nicht**
aufgefuehrt hat, plus 2 INFO (IDR-3 Tamper-Evidenz, IDR-4 Nebenlaeufigkeit) fuer
den Architect. Ich stimme dem „konvergiert, 0 Blocker"-Verdikt der bestehenden
Design-Kette **nicht** zu.

**Reconciliation:** DR-F2/DR-F3/DR-R1 halten (eigenstaendig nachgeprueft — die
Kette liegt dort richtig). Der Kette-Negativbefund „**Write-Pfad fail-closed ist
real**" (Rerun 1) ist die eigentliche Luecke: er ist nur fuer `AktionGaten` wahr
und wurde nicht gegen die uebrigen `anhaengen`-Konsumenten geprueft (IDR-1). Die
Restart/Teil-Write-Interaktion (IDR-2) und die Grenzen der
Append-only-Garantie (IDR-3) sind in keinem der drei Vorlaeufer-Reports benannt.

**Uebergabe:** IDR-1/IDR-2 an Planung/Design-Klaerung (Rueckkante Design → Plan,
§9 fortschreiben); IDR-3/IDR-4 an Architect (Threat-Model/Konvention). Der
Reviewer kategorisiert nur (Skill); die Wahl zwischen den beobachteten Optionen
liegt bei Architect/Implementation. Keine Verifikation gegen DoD/Spec — Verifier
(Modul 11).
