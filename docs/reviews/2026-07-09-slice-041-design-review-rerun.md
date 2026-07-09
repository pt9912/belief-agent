# Review-Report: slice-041 Design-Review (Rerun) — 2026-07-09

**Review-Art:** Design — geprueft *wogegen*: Architektur (Layer-Grenzen, Ports,
ADR-Vertraeglichkeit), **bevor** die Details festgezurrt sind (Modul 10).

**Gegenstand:** `docs/plan/planning/open/slice-041-dauerhafte-audit-datenbank.md`
— **revidierte Fassung** (Working Tree gegen Commit `545b391`; neuer §9).

**Anlass des Rerun:** Nachpruefung der 3 blockierenden Erst-Lauf-Findings
(DR-F1/F2/F3) gegen die §9-Aufloesungen des Planners (Rueckkante Review → Plan,
Modul 8), skill-gebunden.

**Skill:** `.harness/skills/reviewer.md` @ v1.0
**Modell:** Claude Opus 4.8 (1M) · **Datum:** 2026-07-09

**Eingangs-Kontext** (Skill §Kontext-Eingang, Safety-Pfad → Ist-Code gelesen):

- Revidierter Slice-Plan + `git diff` gegen `545b391`
- `spec/architecture.md` `ARC-06/08/09`; `ADR-0001/0002/0003`
- `.../ports/AuditPort.kt`, `.../audit-memory/MemoryAudit.kt`,
  `.../domain/belief/EreignisProtokoll.kt`, `.../Rekonstruktion.kt`
- **Safety-Ist-Code:** `.../gaten/AktionGaten.kt` (Write-Pfad fail-closed),
  `adapters/inbound/cli/.../Runtime.kt` (Read-Pfad `lade()`-Konsum),
  `example/code-agent/.../Main.kt`
- `settings.gradle.kts`, `.a-check.yml` (Root-/Rollen-Muster der `src/main`-Adapter)
- **vorherige Findings:** `docs/reviews/2026-07-09-slice-041-design-review.md`

> **Kontext-Hinweis (Modul 8):** Rerun im selben Kontext; Delta ist die
> code-gestuetzte Nachpruefung der Aufloesungen, keine unabhaengige Zweitsicht.

---

## Nachpruefung der Erst-Lauf-Findings (DR)

| Finding (Erst-Lauf) | Kat. | Status | Verifikation |
|---|---|---|---|
| DR-F2 Source-Set/Dependency | MEDIUM | **behoben** | §9 waehlt Option (a): `audit-file` unter `src/main` + `java.nio`, keine neue Build-Dep, kein Folge-ADR. Konform `ADR-0002` (Dep am Rand); `.a-check.yml`-Root `.../src/main/kotlin/dev/beliefagent` **deckt sich mit** dem Ist-Muster der `src/main`-Adapter (`observation-build-report` etc.). Verifizierbar bei Impl. via `make build`/`make arch-check`. |
| DR-F3 Validierungs-Schicht | MEDIUM | **behoben** | DoD 4 trennt sauber: Adapter = Deserialisierungs-Integritaet; Ordnung ueber `EreignisProtokoll.von(...)` (Domain erzwingt Append-only, `EreignisProtokoll.kt:30/49`), Rekonstruktion bleibt `Rekonstruktion` (Domain). Keine Doppel-Quelle. |
| DR-F1 Port-Fehlersemantik | MEDIUM | **teilweise** | **Write-Pfad verifiziert** (siehe unten): `AktionGaten` faengt `anhaengen`-Fehler und eskaliert fail-closed. **Read-Pfad offen** → neues DR-R1. |

## Findings (Rerun)

### DR-R1 — Read-Pfad-Fehlersemantik: slice-040-Praezedenz traegt nur den Write-Pfad

- `kategorie`: MEDIUM
- `quelle`: `LH-QA-02` (fail-safe), `LH-FA-AUD-002`, `ARC-06`; Skill §Klassifikation
  („Fehlende Fehlersemantik … Sichtbarkeit haengt an … Wurf" / „fail-open statt
  fail-safe")
- `pfad`: `docs/plan/planning/open/slice-041-dauerhafte-audit-datenbank.md:47`
- `befund`: DoD 3 begruendet den Fehlerkanal (geworfene Exception statt `Result`)
  mit „wie slice-040: `AktionGaten` faengt `anhaengen`-Fehler und eskaliert
  fail-closed". Diese Praezedenz ist **verifiziert, aber write-pfad-spezifisch**:
  `AktionGaten` kapselt `audit.anhaengen(...)` in `try/catch` (`AktionGaten.kt:90-101`)
  und eskaliert (`:77-78`). Fuer den **Read-Pfad** existiert **kein** analoger
  Handler: beide Produktiv-Konsumenten von `lade()` — `Runtime.auditEreignisse()`
  (`Runtime.kt:249`) und `example/code-agent/Main.kt:137` — rufen `lade()`
  **ungeschuetzt** auf. Ein vom persistenten Adapter geworfener Lesefehler
  propagiert damit **uncaught**, nicht als geordnete fail-safe-Eskalation
  (`LH-QA-02`). Der Plan generalisiert eine Write-Praezedenz auf den Read-Pfad,
  ohne dass dessen Konsumenten das leisten.
- `verifizierbar`: ja — `grep "\.lade()"` ueber Produktivquellen zeigt die zwei
  ungeschuetzten Konsumenten; `make test` mit einem korrupten-Store-Fixture zeigt,
  ob der Lesefehler geordnet behandelt oder uncaught propagiert wird.

## Negativbefunde

- geprueft, ohne Befund: **Write-Pfad fail-closed** ist real und gegen `LH-QA-02`/
  `LH-FA-POL-004` sauber — Adapter-Wurf → `AktionGaten`-Eskalation
  (`AktionGaten.kt:77-101`); der `AuditPort`-Vertrag bleibt korrekt unveraendert
  (kein `Result`-Umbau; Kern/`MemoryAudit`/Konsumenten unberuehrt).
- geprueft, ohne Befund: **Outbound-Adapter hinter bestehendem Port** + `src/main`/
  `java.nio` ist der richtige, ADR-0001/0002/0003-konforme Schnitt; kein
  Kern-Import von Storage/IO/DI.
- geprueft, ohne Befund: **Append-only als Domain-Invariante** genutzt statt
  reimplementiert (`EreignisProtokoll.von`); DR-F3 sauber aufgeloest.
- geprueft, ohne Befund: **Serialisierung in Stdlib** (kein Plugin) mit
  `ADR-0002`-Guard als Rueckfallebene — ehrlich gehedged, keine verfruehte
  Dependency.
- geprueft, ohne Befund: **Retention/Migration/Backup/Export** und
  **CLI-/Runtime-Binding** korrekt aus dem Schnitt ausgeklammert.
- geprueft, ohne Befund: **`MemoryAudit` bleibt** deterministischer Test-/
  Demo-Stand-in; kein voreiliger Default-Wechsel.

## Ausgefuehrte Sensoren

- `Read`/`grep`/`sed` ueber `AuditPort`, `AktionGaten`, `Runtime`, `code-agent/Main`,
  `EreignisProtokoll`, `Rekonstruktion`, `.a-check.yml`, revidierter Plan — gelesen.
- `grep "\.lade()"` (Produktivquellen) — 2 ungeschuetzte Konsumenten belegt
  (`Runtime.kt:249`, `Main.kt:137`).
- `make doc-check` — PASS (`d-check`: 0 Befunde; validiert auch beide Rerun-Reports).

## Summary

| Kategorie | Anzahl |
|---|---:|
| HIGH | 0 |
| MEDIUM | 1 |
| LOW | 0 |
| INFO | 0 |

## Verdikt

**Merge-blockierend:** ja — **1 verbleibendes MEDIUM** (DR-R1), reduziert von 3.
DR-F2/DR-F3 sind aufgeloest und verifiziert; DR-F1 ist auf dem Write-Pfad
verifiziert, auf dem Read-Pfad offen.

**Charakter des Rests:** Klaerungs-Ebene, nicht Neuentwurf. DR-R1 hat zwei
beobachtbare, saubere Aufloesungen (Wahl liegt bei Architect/Planner — Reviewer
kategorisiert nur): (i) Read-Konsumenten-Handling **explizit ausklammern** wie das
CLI-Binding (dann traegt der Adapter nur den Wurf, der geordnete Read-Fail ist
Folgeslice), **oder** (ii) den uncaught-propagierenden Read-Fehler als
akzeptiertes Verhalten dieses Slice **benennen** (statt slice-040-Paritaet zu
behaupten, die fuer Reads nicht gilt).

**Uebergabe:** DR-R1 an Planung/Design-Klaerung (Rueckkante Design → Plan, §9
fortschreiben). Keine Verifikation gegen DoD/Spec — Verifier (Modul 11).
