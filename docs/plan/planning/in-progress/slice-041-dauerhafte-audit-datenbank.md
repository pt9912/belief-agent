# Slice slice-041: Persistenter AuditPort-Adapter

**Status:** open (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung.

**Bezug:** `LH-FA-AUD-001`, `LH-FA-AUD-002`, `LH-FA-AUD-003`, `LH-FA-AUD-004`,
`LH-QA-02`, `LH-QA-03`, `LH-QA-04`, `LH-QA-06`; `ADR-0001`, `ADR-0002`, `ADR-0003`,
`ADR-0004`, `ADR-0006`; `ARC-06`, `ARC-08`, `ARC-09`.

**Autor:** Codex. **Datum:** 2026-07-08.

---

## 1. Ziel

Ein echter, nicht-Memory-Outbound-Adapter hinter dem bestehenden `AuditPort`
ersetzt `MemoryAudit` fuer persistente Audit-Spuren: Ereignisse werden
append-only gespeichert, nach Prozess-Neustart rekonstruierbar geladen und bei
Schreib-/Lesefehlern fail-closed sichtbar gemacht. Der Slice liefert damit den
kleinsten dauerhaften Audit-Adapter; allgemeine Audit-Datenbank-Funktionen wie
Retention, Migrationen, Backups und Compliance-Export bleiben Folgearbeit.

## 2. Definition of Done

- [ ] Neues Outbound-Adaptermodul `adapters/outbound/audit-file` (JVM-Ziel,
  `src/main/kotlin`, Datei-IO über `java.nio`) implementiert `AuditPort` hinter
  `ARC-08`; `hexagon:*` importiert keine Storage-/IO-Pakete und keinen Adapter.
  Source-Set und Abhängigkeitsfläche sind im Design-Review geklärt (§9 DR-F2):
  `jvmMain`/`src/main` wie alle IO-tragenden Adapter (`observation-build-report`,
  `observation-git-local`, `llm-langchain4j`), **keine neue Build-Abhängigkeit**
  (kein Serialisierungs-Plugin) — konform zu `ADR-0002` (JVM-Ziel zuerst, Dep am
  Rand), ohne Folge-ADR. Die lokale Speicherform ist inspizierbar (`LH-QA-06`);
  Compliance-Export bleibt Folgearbeit.
- [ ] Der Adapter speichert alle bestehenden `Ereignis`-Typen (inkl. der
  slice-040-Approval-Ereignisse `ApprovalAngefragt`/`Erteilt`/`Verweigert`/
  `Fehler`, die bereits über denselben `AuditPort` laufen) append-only und
  geordnet; vorhandene Ereignisse werden nie überschrieben oder gelöscht.
  Serialisierung bleibt in Kotlin/JDK-Stdlib (deterministisches, versioniertes
  Textformat, kein neues Plugin); die Speichertechnologie ist lokal hermetisch
  testbar. Nur falls sich in der Implementierung doch eine Serialisierungs-/
  Storage-Bibliothek als nötig erweist, greift der `ADR-0002`-Guard: vor Code
  Folge-ADR (§9 DR-F2). Append-only/Unveränderlichkeit ist gegen die
  **Adapter-API** garantiert; ein Out-of-Band-Umschreiben der Datei
  (Tamper-Evidenz) und nebenläufige Writer sind **out of scope** dieses
  Single-Writer-Stores (§9 IDR-3/IDR-4).
- [ ] Restart-/Replay-Verhalten ist deterministisch getestet (`LH-QA-03`):
  Laden nach Neustart, leere Datenbank (legitim leer ≠ Fehler), abgeschnittener
  **Trailing**-Record (Crash-mid-write) → N-1 rekonstruiert + sichtbar markiert,
  **Interior**-Korruption → Wurf (§9 IDR-2), Schreibfehler, Reihenfolge und
  Rekonstruktion über `AuditPort.lade()` sind abgedeckt. Fehlerkanal ist die
  **geworfene Exception**; der Adapter gibt bei Schreib-/Lesefehler nie still ein
  leeres Protokoll zurück (Sichtbarkeit an der Adapter-Grenze — `LH-QA-02`,
  `LH-FA-AUD-002`, `LH-FA-AUD-003`). **Write-Pfad (nur `AktionGaten` ist
  fail-closed — §9 IDR-1/IPR-1):** von den vier `anhaengen`-Konsumenten fängt nur
  `AktionGaten` die Exception und **eskaliert fail-closed** (verifiziert).
  `AktionsVorschlagen` verschluckt einen Audit-Schreibfehler im Validierungs-
  `runCatching{}.getOrNull()` (`AktionsVorschlagen.kt:57/89/91`) → das
  Pflicht-Ereignis `AktionVorgeschlagen` (`LH-FA-AUD-001`) ginge still verloren;
  `KonfidenzExternalisieren` schreibt `KonfidenzPort` vor dem Audit (`:48/49`) →
  Teilzustand; das code-agent-Beispiel propagiert uncaught. **Harte
  Vorbedingung:** der persistente (werfende) Adapter wird in diesem Slice
  **nicht** produktiv in diese Pfade gebunden (MemoryAudit bleibt Default, kein
  CLI-Binding) — die Lücke bleibt latent. Geordnetes fail-closed der
  Nicht-`AktionGaten`-Konsumenten ist benannter **Folgeslice**, fällig **bevor**
  ein werfender Adapter in den Vorschlags-/Konfidenzpfad gebunden wird.
  **Read-Pfad:** die geworfene `lade()`-Exception fangen die
  heutigen Konsumenten **nicht** — das ist bewusst so (§9 DR-R1: `lade()`-
  Konsumenten sind observability-only und kein Gate-Input; geordnete
  Read-Fail-Eskalation ist benannter Folgeslice, nicht slice-040-Parität). Der
  `AuditPort`-Vertrag wird **nicht** auf `Result` umgestellt (das berührte Kern,
  `MemoryAudit` und alle Konsumenten — außerhalb des kleinsten Adapters, §9 DR-F1).
- [ ] Der Adapter prüft **Deserialisierungs-Integrität** vor der Rückgabe:
  unbekannter Typ-Tag, fehlendes Pflichtfeld oder ein defekter Datensatz **im
  Inneren** des Protokolls → sichtbarer (geworfener) Fehler statt gate-fähiger
  Teilhistorie. **Ausnahme Trailing-Truncation (§9 IDR-2):** ein einzelner
  abgeschnittener **letzter** Datensatz (typischer Crash-während-Append) wird
  toleriert — die N-1 validen Records werden über `EreignisProtokoll.von(...)`
  rekonstruiert und der abgeschnittene Rest **sichtbar** gemeldet (`LH-QA-06`),
  nicht still verworfen; damit bleibt „nach Neustart rekonstruierbar" (DoD 1,
  `LH-FA-AUD-002`) wahr, während Interior-Defekte weiter laut fehlschlagen
  (`LH-QA-02`).
  Die **Sequenz-/Ordnungs-Invariante** (kein Rück-Datieren) reimplementiert der
  Adapter **nicht**: er baut das Protokoll über `EreignisProtokoll.von(...)`, das
  die Append-only-Ordnung als Domain-Invariante erzwingt
  (`IllegalArgumentException` bei Rückdatierung); Rekonstruierbarkeit bleibt
  Sache von `Rekonstruktion` (Domain). Keine Doppel-Quelle der append-only-Regel
  im Adapter (§9 DR-F3; `LH-FA-AUD-001`, `LH-FA-AUD-003`, `LH-FA-AUD-004`).
- [ ] Build-/Arch-/Doku-Integration ist vollständig: Modul in
  `settings.gradle.kts`, `.a-check.yml` und `Dockerfile` aufgenommen.
  **Coverage-Gate (`ADR-0004`/`ADR-0006`, §9 IPR-2):** das neue Modul trägt einen
  eigenen `build.gradle.kts` mit per-Modul-`kover { reports { verify { rule {
  minBound(90) } } } }` (Adapter-Floor 90 %) **und** wird im `Dockerfile` an allen
  Enumerationsstellen explizit ergänzt (`COPY …/build.gradle.kts`,
  `:dependencies`, `:test`, `:koverLog`, `:koverVerify`) — kein zentrales
  Auto-Inherit. CLI-/Runtime-Binding bleibt separater bewusster Schritt, falls es
  über Test-/Demo-Konfiguration hinausgeht. Review-/Verification-Artefakte,
  `make doc-check`, `make gates` und Closure-Notiz liegen vor.

## 3. Plan (vor Code)

| Datei / Komponente | Aenderungs-Art | Begruendung |
|---|---|---|
| `adapters/outbound/audit-file` (JVM, `src/main/kotlin`) | neu | Persistenter nicht-Memory-`AuditPort`-Adapter hinter `ARC-08`; `java.nio`-Datei-IO, keine neue Build-Abhängigkeit (§9 DR-F2). |
| `.../src/main/kotlin/**` | neu | Append-only Datei-Speicherung + Stdlib-Serialisierung, Laden via `EreignisProtokoll.von(...)`, geworfene Fehlergrenze (§9 DR-F1/DR-F3). |
| `.../src/test/kotlin/**` | neu | Restart-, Korruptions-, Schreibfehler-, Reihenfolge- und Rekonstruktionstests. |
| `settings.gradle.kts` | update | Neues Adaptermodul `adapters:outbound:audit-file` registrieren. |
| `.a-check.yml` | update | Neue Rolle `outbound_audit_file`, Root `adapters/outbound/audit-file/src/main/kotlin/dev/beliefagent`, Kanten `→ application`/`→ domain`; Kern bleibt adapterfrei. |
| `adapters/outbound/audit-file/build.gradle.kts` | neu | `kotlin("jvm")` + `kover`-Plugin wie `observation-build-report`; per-Modul-`kover { … verify { rule { minBound(90) } } }` (Adapter-Floor, `ADR-0006`). |
| `Dockerfile` | update | Neues Modul an **allen** Enumerationsstellen ergänzen: `COPY …/build.gradle.kts`, `:dependencies`, `:test`, `:koverLog`, `:koverVerify` (`ADR-0006`: kein Auto-Inherit). |
| `docs/user/integration.md` | update | Persistenten Audit-Adapter, lokale Speicherform und Fehlerverhalten dokumentieren. |
| `docs/user/cli-entscheidungsnachweis.md` | update | Persistente Audit-Spur als optionalen Nachweis benennen, ohne CLI-Default umzubinden. |
| `docs/reviews/*slice-041*` | neu | Design-/Code-Review-Artefakt fuer persistenten AuditPort-Adapter. |
| `docs/verifications/*slice-041*` | neu | Verification-Artefakt fuer DoD und Restart-/Replay-Matrix. |

## 4. Trigger

`slice-010` liegt in `done/` und stellt `MemoryAudit` als deterministischen
Stand-in bereit; `slice-022` ergaenzt weitere Audit-Ereignisse fuer
externalisierte Konfidenz. Kein Slice liegt in `in-progress/` (WIP-Limit 1).
Der Schnitt ist entschieden (Architect/Planner, informiert durch den
Design-Review — §9 DR-F2; der Reviewer kategorisiert nur, Modul 8): einfacher
lokaler append-only Datei-Adapter (`jvmMain`/`src/main`, `java.nio`, keine neue
Build-Abhängigkeit) — kein Folge-ADR. Der `ADR-0002`-Guard bleibt nur
Rückfallebene, falls die Implementierung doch eine Serialisierungs-/Storage-Lib
braucht. `slice-040` (done) schreibt seine Approval-Audit-Ereignisse
(`ApprovalAngefragt` u. a.) bereits über denselben `AuditPort`; sie sind daher
**jetzt** im Serialisierungs-Scope dieses Adapters (DoD-Punkt 2), nicht spätere
Arbeit, und `slice-040` ist keine Voraussetzung für den ersten echten
`AuditPort`-Adapter.

## 5. Closure-Trigger

DoD vollstaendig + Review/Verification abgeschlossen + `make gates` gruen +
Closure-Notiz geschrieben + Slice nach `done/` verschoben.

## 6. Risiken und offene Punkte

- Dauerhaftigkeit darf keine stillen Recovery-Pfade erfinden: korrupte oder
  teilgeschriebene Daten muessen diagnostiziert werden und duerfen nicht als
  valides leeres Protokoll erscheinen.
- Read-Pfad-Konsumenten von `lade()` sind heute observability-only
  (`Runtime.auditEreignisse()`, code-agent-Beispiel) und **kein** Gate-Input
  (verifiziert: `auditEreignisse()` hat keinen Produktiv-Entscheider,
  `Rekonstruktion` liegt nicht im Live-Gating-Pfad). Ein geworfener Lesefehler
  faellt dort als lauter Fehlschlag der Inspektion an — fail-safe an der
  Adapter-Grenze, aber **ohne** geordnete Eskalation. Geordnetes
  Read-Fail-Handling in den Konsumenten ist bewusster **Folgeslice**, faellig
  sobald ein Slice `lade()`/`Rekonstruktion` in einen Gate-/Entscheidungspfad
  fuehrt (§9 DR-R1).
- Storage-/Source-Set-Wahl ist im Design-Review geklärt (§9 DR-F2): JVM
  `src/main` + `java.nio`, keine neue Build-Abhängigkeit → kein Folge-ADR. Der
  `ADR-0002`-Guard bleibt nur Rückfallebene, falls in der Implementierung doch
  eine Serialisierungs-/Storage-Bibliothek nötig wird.
- CLI-Produktivbinding an den persistenten Audit-Adapter bleibt separater
  Folgeslice, falls es Konfiguration, Pfadpolitik oder Migration beruehrt.
- Retention-Policy, Datenbankmigrationen, Backups und externe Compliance-Exports
  bleiben Folgeslices; dieser Slice liefert nur dauerhafte lokale
  Append-only-Persistenz hinter `AuditPort`.
- Trailing-Truncation-Recovery (§9 IDR-2) unterscheidet einen abgeschnittenen
  letzten Record (tolerieren + sichtbar markieren) von Interior-Korruption
  (werfen). Die Detektion „nur der letzte Record ist unvollständig" muss robust
  sein — jede Nicht-Trailing-Parse-/Ordnungsverletzung bleibt harter Fehler.
- Tamper-Evidenz (§9 IDR-3): das inspizierbare Textformat (`LH-QA-06`) hat
  **keine** Hash-Chain/Signatur; ein ordnungserhaltendes Out-of-Band-Umschreiben
  lädt unentdeckt. `LH-FA-AUD-001`-Unveränderlichkeit gilt gegen die Adapter-API,
  nicht gegen einen Dateisystem-Akteur. Tamper-Evidenz ist Architect-/
  Folgeentscheidung (Threat-Model; in Spannung zur Klartext-Inspizierbarkeit).
- Single-Writer (§9 IDR-4): der Adapter nimmt einen Prozess/einen Schreiber an
  (heutige single-threaded Runtime) und dokumentiert das. Nebenläufige/Multi-
  Prozess-Writer (Interleaving → korrupte Records) sind out of scope; File-
  Locking/Single-Writer-Enforcement ist Folgeslice, falls die Runtime nebenläufig
  wird.

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss fuellen. -->

## 8. Sub-Area-Modus-Begründung

### Sub-Area: Audit-Persistenzadapter (`adapters/outbound/audit-*`)

- **Modus:** Hybrid
- **Konventionen-Dichte:** hoch fuer `AuditPort` und append-only Verhalten
  (`slice-007`, `slice-010`, `ARC-06`), mittel fuer dauerhafte lokale
  Storage-Technologie, die noch nicht festgelegt ist.
- **Phase-Reife:** Phase 4 fuer In-Memory-Audit, Phase 2-3 fuer persistente
  lokale Speicherung. Der Port ist stabil; der nicht-Memory-Adapter ist neu.
- **Evidenz-/Diskrepanz-Risiko:** hoch. Still verlorene oder falsch
  rekonstruierte Audit-Ereignisse verletzen die Entscheidungsspur.
- **Reconciliation-Aufwand:** ein Slice fuer Adapter, Tests, Build-/Arch- und
  Doku-Integration. Retention/Backups/Exports bleiben Folgearbeit.

### Sub-Area: Runtime-/CLI-Integration

- **Modus:** GF
- **Konventionen-Dichte:** mittel. `ARC-09` verortet Binding im
  Composition-Root; dieser Slice soll den Adapter bereitstellen, aber den
  produktiven CLI-Default nicht nebenbei umstellen.
- **Phase-Reife:** Phase 4. CLI-Composition ist stabil; dauerhafte Auditwahl ist
  ein bewusster Folgeschritt.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Eine unbemerkte Default-Umbindung kann
  lokale Demos oder hermetische Tests brechen.
- **Reconciliation-Aufwand:** gering in diesem Slice; eigenes Binding/Migration-
  Slice, falls Runtime-Default oder Pfadpolitik geaendert werden.

## 9. Design-/Review-Klärung (Rückkante Review → Plan, Modul 8)

Übergabe-Artefakt für die Findings aus
[`docs/reviews/2026-07-09-slice-041-design-review.md`](../../../reviews/2026-07-09-slice-041-design-review.md)
(DR, 3× MEDIUM, merge-blockierend) und
[`docs/reviews/2026-07-09-slice-041-plan-review.md`](../../../reviews/2026-07-09-slice-041-plan-review.md)
(PR, 2× LOW / 2× INFO). Entscheidung und Umsetzung liegen bei Architect/Planner
(Modul 8); der Reviewer kategorisiert nur. Diese Klärung ist vor
Implementierungsstart abzuschließen.

Skill-gebundene Rerun-Nachprüfung (`.harness/skills/reviewer.md` v1.0):
[`…-design-review-rerun.md`](../../../reviews/2026-07-09-slice-041-design-review-rerun.md)
bestätigt DR-F1(Write)/F2/F3 behoben und meldet **DR-R1** (Read-Pfad, MEDIUM);
[`…-plan-review-rerun.md`](../../../reviews/2026-07-09-slice-041-plan-review-rerun.md)
bestätigt PR-F1..F4 behoben und meldet **PR-R1** (Attribution, INFO). Beide unten
aufgelöst (Rerun-Tabelle). Ein zweiter Rerun
([`…-design-review-rerun2.md`](../../../reviews/2026-07-09-slice-041-design-review-rerun2.md),
[`…-plan-review-rerun2.md`](../../../reviews/2026-07-09-slice-041-plan-review-rerun2.md))
meldete danach Konvergenz (0 Blocker) — dieses „implementierungsreif" widerlegt
der unabhängige Lauf unten (same-context ≠ unabhängige Zweitsicht).

Unabhängiger Frischkontext-Lauf (echte Kontext-Trennung, Modul 8):
[`…-design-review-independent.md`](../../../reviews/2026-07-09-slice-041-design-review-independent.md)
und [`…-plan-review-independent.md`](../../../reviews/2026-07-09-slice-041-plan-review-independent.md)
bestätigen DR-F2/F3/R1 eigenständig, widersprechen aber dem „0 Blocker"-Verdikt
der Same-Context-Kette: **IDR-1/IPR-1** (Write-Konsumenten-Asymmetrie, MEDIUM),
**IDR-2** (Restart-vs-Teil-Write, MEDIUM), **IPR-2** (Coverage-Gate-/ADR-Referenz,
MEDIUM) sowie **IDR-3/IDR-4** (Tamper-Evidenz, Single-Writer, INFO). Alle unten
aufgelöst (Independent-Tabelle).

**Erst-Lauf-Findings:**

| Finding | Kat. | Entscheidung | Verankert in |
|---|---|---|---|
| DR-F1 Port-Fehlersemantik | MEDIUM | `AuditPort`-Vertrag **unverändert** (kein `Result`-Umbau): Fehlerkanal ist die geworfene Exception, der Konsument eskaliert fail-closed (slice-040-Präzedenz `AktionGaten`). Der Adapter gibt bei Fehler nie still ein leeres Protokoll zurück. Kern, `MemoryAudit` und alle Konsumenten bleiben unberührt. | §2 DoD 3, §1 |
| DR-F2 Source-Set/Dependency | MEDIUM | **Option (a):** `adapters/outbound/audit-file` unter `jvmMain`/`src/main`, Datei-IO via `java.nio`, **keine neue Build-Abhängigkeit** (Stdlib-Serialisierung, kein Plugin). Konform `ADR-0002` (JVM-Ziel zuerst, Dep am Rand); Präzedenz `observation-build-report`/`observation-git-local`/`llm-langchain4j`. **Kein Folge-ADR.** `.a-check.yml`-Root `.../src/main/kotlin/...`. `ADR-0002`-Guard bleibt Rückfallebene, falls die Impl. doch eine Lib braucht. | §2 DoD 1/2, §3, §4, §6 |
| DR-F3 Validierungs-Schicht | MEDIUM | Trennung: **Adapter** = Deserialisierungs-Integrität (Typ-Tag, Pflichtfeld, defekter Datensatz → geworfener Fehler). **Domain** = Ordnung/Rekonstruktion: der Adapter baut über `EreignisProtokoll.von(...)` (erzwingt Append-only), reimplementiert die Regel nicht; Rekonstruierbarkeit bleibt `Rekonstruktion`. Keine Doppel-Quelle der append-only-Invariante. | §2 DoD 4, §3 |
| PR-F1 LH-Abdeckung | LOW | `LH-FA-AUD-004` (Zeitstempel/Quelle je Ereignis — trägt Ordnungs-/Rückdatierungsprüfung) und `LH-QA-06` (Protokoll inspizierbar — lokale Speicherform; Export = Folgearbeit) in **Bezug** + DoD ergänzt. | Kopf **Bezug**, §2 DoD 1/4 |
| PR-F2 Trigger-Formulierung | LOW | Trigger korrigiert: slice-040-Approval-Ereignisse laufen **bereits** über denselben `AuditPort` und sind **jetzt** in-Scope (DoD 2), nicht „später". | §4 |
| PR-F3 Modul/Tech offen | INFO | Aufgelöst durch DR-F2: der Plan konvergiert jetzt auf konkretes Modul, Source-Set und Abhängigkeitsfläche; nicht mehr disjunktiv. | §2 DoD 1, §3, §4 |
| PR-F4 kein Reviewer-Skill | INFO | Harness-Pflege, nicht slice-041-Scope. Inzwischen vorhanden: `.harness/skills/reviewer.md`. | — (Harness) |

**Rerun-Findings:**

| Finding | Kat. | Entscheidung | Verankert in |
|---|---|---|---|
| DR-R1 Read-Pfad-Fehlersemantik | MEDIUM | slice-040-Parität gilt nur für den **Write**-Pfad — DoD 3 entsprechend korrigiert (keine Generalisierung auf Reads). Der Adapter wirft bei korruptem/teilgeschriebenem Store auf `lade()` einen **sichtbaren** Fehler (kein stilles Leerprotokoll → `LH-QA-02`-Sichtbarkeit an der Adapter-Grenze). Die heutigen `lade()`-Konsumenten (`Runtime.auditEreignisse()`, code-agent-Beispiel) sind **observability-only** und **kein** Gate-Input (verifiziert: `auditEreignisse()` ohne Produktiv-Entscheider, `Rekonstruktion` nicht im Live-Gating-Pfad); ein geworfener Lesefehler ist dort ein lauter Fehlschlag der Inspektion. **Geordnete Read-Fail-Eskalation in den Konsumenten = bewusster Folgeslice** (fällig, sobald `lade()`/`Rekonstruktion` einen Gate-Pfad speist) — kombiniert die zwei Reviewer-Optionen (Wortlaut ehrlich + Konsumenten-Handling explizit ausgeklammert). | §2 DoD 3, §6 |
| PR-R1 Rollen-Attribution §4 | INFO | §4 umformuliert: der **Architect/Planner** entscheidet (informiert durch den Design-Review), nicht „der Design-Review hat entschieden" — konsistent zur Rollentrennung (Modul 8) und zur §9-Attribution. | §4 |

**Independent-Findings (Frischkontext-Lauf, Modul 8):**

| Finding | Kat. | Entscheidung | Verankert in |
|---|---|---|---|
| IDR-1 / IPR-1 Write-Konsumenten-Asymmetrie | MEDIUM | „Write-Pfad fail-closed" galt nur für `AktionGaten` — DoD 3 korrigiert und alle vier `anhaengen`-Konsumenten ehrlich charakterisiert: `AktionsVorschlagen` verschluckt (Validierungs-`runCatching`), `KonfidenzExternalisieren` erzeugt Teilzustand (KonfidenzPort vor Audit), code-agent propagiert uncaught. **Harte Vorbedingung:** der werfende Adapter wird hier **nicht** produktiv in diese Pfade gebunden (MemoryAudit-Default, kein CLI-Binding) → Lücke latent. Geordnetes fail-closed dieser Konsumenten = benannter **Folgeslice**, fällig **vor** Bindung eines werfenden Adapters in den Vorschlags-/Konfidenzpfad. | §2 DoD 3, §6 |
| IDR-2 Restart vs. Teil-Write | MEDIUM | Options-Raum benannt + entschieden: abgeschnittener **Trailing**-Record (Crash-mid-write) → N-1 über `EreignisProtokoll.von(...)` rekonstruiert + sichtbar markiert (`LH-QA-06`); **Interior**-Defekt → Wurf. Hält DoD 1/`LH-FA-AUD-002` (rekonstruierbar) und bleibt laut bei Korruption/Manipulation (`LH-QA-02`). Test-Fixture ergänzt. | §2 DoD 3/4, §6 |
| IDR-3 Tamper-Evidenz | INFO | Append-only gilt gegen die **Adapter-API**, nicht gegen Dateisystem-Zugriff; das inspizierbare Textformat hat keine Hash-Chain/Signatur → ordnungserhaltendes Out-of-Band-Umschreiben lädt unentdeckt. Bewusst out of scope (Threat-Model = lokaler Single-Writer-Store); Tamper-Evidenz = Architect-/Folgeentscheidung, in Spannung zur `LH-QA-06`-Klartext-Inspizierbarkeit. | §2 DoD 2, §6 |
| IDR-4 Single-Writer/Nebenläufigkeit | INFO | Single-Writer-Annahme (ein Prozess/ein Schreiber, heutige single-threaded Runtime) explizit dokumentiert. Parallele/Multi-Prozess-Writer out of scope; File-Locking = Folgeslice bei nebenläufiger Runtime. | §2 DoD 2, §6 |
| IPR-2 Coverage-Gate/ADR-Referenz | MEDIUM | `ADR-0004`/`ADR-0006` in **Bezug** ergänzt. §3/DoD 5: neues `build.gradle.kts` mit per-Modul-`kover{minBound(90)}` (Adapter-Floor) **und** `Dockerfile`-Enumerierung an allen Stellen (`COPY`, `:dependencies`, `:test`, `:koverLog`, `:koverVerify`) — kein Auto-Inherit (`ADR-0006`). | Kopf **Bezug**, §2 DoD 5, §3 |
