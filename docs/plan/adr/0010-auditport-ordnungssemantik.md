# ADR-0010: AuditPort-Ordnungssemantik — wo wird Append-only erzwungen (Load vs. Write-Parität)?

**Status:** Proposed

**Datum:** 2026-07-09

**Autor:** belief-agent (Implementation-**Vorschlag**; der Architect entscheidet über `Accepted`)

**Bezug:** [`LH-FA-AUD-001`](../../../spec/lastenheft.md#lh-fa-aud-001--unveränderliches-ereignisprotokoll), [`LH-QA-02`](../../../spec/lastenheft.md#lh-qa-02--konservatives-standardverhalten-fail-safe), [`LH-QA-03`](../../../spec/lastenheft.md#lh-qa-03--testbarkeit); [ADR-0001](0001-hexagonal-llm-port.md), [ADR-0003](0003-hexslice-architektur.md). Anlass: unabhängiger Code-Safety-Review des ersten persistenten `AuditPort`-Adapters, Finding **MEDIUM-1** (Write-Pfad-Ordnungs-Divergenz), in Spannung zur Design-Entscheidung **DR-F3** (Ordnung am Load erzwingen).

**Schärft:** [`architecture.md §2`](../../../spec/architecture.md#2-schichten-und-constraints) — die Schicht-/Port-Constraints, an denen der `AuditPort`-Vertrag über mehrere Adapter-Implementierungen hängt. Wird mit `Accepted` verbindlich; bis dahin Vorschlag.

---

## Kontext

Die Append-only-Ordnung des Ereignisprotokolls (kein Rück-Datieren, [`LH-FA-AUD-001`](../../../spec/lastenheft.md#lh-fa-aud-001--unveränderliches-ereignisprotokoll))
ist als **Domänen-Invariante** in `EreignisProtokoll.append` verankert: der Aufruf
wirft `IllegalArgumentException`, wenn ein Ereignis zeitlich vor dem letzten liegt.
Zwei `AuditPort`-Implementierungen konsumieren diese Invariante jedoch **an
unterschiedlichen Stellen**:

| Adapter | Ordnungsprüfung | Verhalten bei Rückdatierung |
|---|---|---|
| `MemoryAudit` | am **Write** (`anhaengen` geht über `EreignisProtokoll.append`) | wirft **sofort** beim `anhaengen`; Store bleibt intakt |
| `DateiAudit` (persistenter Datei-Adapter) | am **Load** (`lade` baut über `EreignisProtokoll.von(...)`) | `anhaengen` akzeptiert still und persistiert; erst das nächste `lade()` wirft — dann **dauerhaft** (Poison-Store) |

Diese Divergenz ist **kein Zufall**, sondern Folge der bewusst getroffenen
Design-Entscheidung **DR-F3**: der Datei-Adapter soll die Ordnungsregel **nicht
reimplementieren** (keine „Doppel-Quelle" der Append-only-Regel im Adapter),
sondern über `EreignisProtokoll.von(...)` am Load erzwingen. Der unabhängige
Code-Safety-Review (Finding **MEDIUM-1**) hat die Konsequenz benannt: gleicher
`AuditPort`-Aufruf, unterschiedliches Fehler-Timing (Write vs. Load), und ein
rückdatierter Write hinterlässt einen **unwiderruflich unladbaren** Datei-Store, wo
`MemoryAudit` den Aufruf sauber abweist.

**Annahme, die die Entscheidung trägt:** Zeitstempel kommen aus dem `UhrPort`
monoton; ein rückdatiertes `anhaengen` ist ein **Aufrufer-Vertragsbruch**, kein
Normalfall. Kippt diese Annahme (nicht-monotone Quelle, mehrere Writer), kippt die
Bewertung. Der persistente Adapter ist heute **nicht** produktiv gebunden
(`MemoryAudit` bleibt Default) — es gibt aktuell keinen Live-Weg, einen rückdatierten
Write auszulösen.

## Entscheidung

> **Vorschlag (Proposed).** Der Implementer schlägt vor; der Architect entscheidet
> über `Accepted` (Modul 8 — Implementer schlägt Folge-ADR vor, entscheidet nicht).

Empfohlen wird **Option A**: **DR-F3 bleibt gültig — die Ordnung wird am Load
erzwungen, nicht am Write reimplementiert.** Die Cross-Impl-Divergenz und der
Poison-Store-auf-Rückdatierung werden als **dokumentiertes Residuum** akzeptiert,
weil (1) der Load-Pfad fail-closed und **sichtbar** wirft ([`LH-QA-02`](../../../spec/lastenheft.md#lh-qa-02--konservatives-standardverhalten-fail-safe) erfüllt — kein
stiller Datenschaden), (2) Rückdatierung einen Aufrufer-Vertragsbruch voraussetzt,
den monotone Uhr-Zeitstempel ausschließen, und (3) der Adapter unverdrahtet ist.
**Re-Evaluierung** wird an einen harten Trigger gebunden (siehe unten): **bevor** ein
persistenter Adapter in einen Pfad gebunden wird, in dem Rückdatierung plausibel ist,
ist auf **Option B** umzustellen.

## Verglichene Alternativen

### Option A — Load-Erzwingung beibehalten, Residuum dokumentieren (empfohlen)

- Pro: honoriert die mehrfach reviewte DR-F3-Entscheidung (keine Doppel-Quelle);
  Load ist fail-closed/sichtbar; kein Write-Zeit-Tail-Read (keine `anhaengen`-O(n));
  minimal.
- Contra: Verhaltens-Divergenz zu `MemoryAudit` (Fehler-Timing); ein rückdatierter
  Write poisont den Datei-Store irreversibel statt ihn sauber abzuweisen.

### Option B — Write-Parität: jeder AuditPort weist Rückdatierung am `anhaengen` ab

- Pro: einheitliches fail-fast über alle Implementierungen; kein Poison-Store; ein
  rückdatierter Write scheitert dort, wo er entsteht.
- Contra: der Datei-Adapter muss den letzten persistierten Zeitstempel beim Schreiben
  lesen und vergleichen — das ist genau die **Doppel-Quelle**, die DR-F3 vermeiden
  wollte (oder ein Voll-Reload je Write, O(n²)); erweitert die `AuditPort`-
  Vertragssemantik (Write wirft jetzt auch auf Ordnung), was **DR-F1** bewusst
  schlank gehalten hat.

### Option C — `AuditPort` auf `Result`/Fehlertyp umstellen (verworfen)

- Pro: explizite Fehlersemantik am Vertrag statt geworfener Exception.
- Contra: bereits als Design-Entscheidung **DR-F1** verworfen — berührt Kern,
  `MemoryAudit` und **alle** Konsumenten, weit außerhalb dieser Ordnungsfrage; ändert
  nichts am Timing-Problem selbst.

## Konsequenzen

- Positiv (bei A): keine Änderung an Kern/Adaptern nötig; DR-F3 bleibt konsistent;
  der Poison-Store ist ein **lauter**, kein stiller Fehler ([`LH-QA-02`](../../../spec/lastenheft.md#lh-qa-02--konservatives-standardverhalten-fail-safe)).
- Negativ (bei A): die Divergenz zu `MemoryAudit` bleibt bestehen und muss vor jeder
  produktiven Bindung eines persistenten Adapters erneut bewertet werden.
- Folgepflicht: Wird **Option B** gewählt, ist eine Folgearbeit nötig (Write-Zeit-
  Ordnungsprüfung im persistenten Adapter + Divergenz-Test gegen `MemoryAudit`); die
  DR-F3-Formulierung im zugehörigen Design-Review ist dann zu schärfen. Bei **Option
  A** genügt ein Vermerk an der späteren produktiven Bindung, dass sie den
  Re-Trigger prüft.

## Fitness Function (falls maschinell prüfbar)

| Tooling | Regel | Make-Target |
|---|---|---|
| Test ([`LH-QA-03`](../../../spec/lastenheft.md#lh-qa-03--testbarkeit)) | Bei **Option A**: ein rückdatierter Store wirft am `lade()` sichtbar (`ordnungsverletzung_im_store_wirft`). Bei **Option B**: `anhaengen` einer Rückdatierung wirft **sofort**, paritätisch zu `MemoryAudit` | `make test` |
| a-check | Der Adapter importiert weiterhin nur `domain`/`application`; die Ordnungsregel bleibt in `EreignisProtokoll` (kein Framework/Kern-Leak) | `make arch-check` |

## Re-Evaluierungs-Trigger

**Hart:** bevor ein persistenter `AuditPort`-Adapter in einen `anhaengen`-Pfad
gebunden wird, in dem Rückdatierung plausibel ist (nicht-monotone Zeitquelle,
Multi-Writer, externer Ereignis-Import) — dann ist Option B verbindlich. Ferner: wenn
eine spätere Erweiterung die Single-Writer-Annahme (ein Prozess/ein Schreiber)
aufhebt.

## Geschichte

| Datum | Ereignis | Verweis |
|---|---|---|
| 2026-07-09 | **Proposed** — aus unabhängigem Code-Safety-Review (Finding MEDIUM-1, Write-Pfad-Ordnungs-Divergenz), Rückkante Review→Plan; Spannung zur Design-Entscheidung DR-F3 | Code-Safety-Review MEDIUM-1 |
