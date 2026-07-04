# ADR-0001: Hexagonale Architektur — LLM als austauschbarer Port, Entscheidungslogik im Kern

**Status:** Proposed

**Datum:** 2026-06-22

**Autor:** belief-agent (Bootstrap)

**Bezug:** [`LH-FA-LLM-001`](../../../spec/lastenheft.md#lh-fa-llm-001--sprachmodell-als-austauschbares-modul), [`LH-QA-04`](../../../spec/lastenheft.md#lh-qa-04--erweiterbarkeit), [`LH-FA-POL-006`](../../../spec/lastenheft.md#lh-fa-pol-006--nicht-umgehbares-gate)

**Schärft:** [`architecture.md §2`](../../../spec/architecture.md#2-schichten-und-constraints), [`spezifikation.md §3`](../../../spec/spezifikation.md#3-defaults-und-konstanten)

---

## Kontext

Das Lastenheft verlangt, dass das Sprachmodell ein **austauschbares Modul**
ist und **nicht der Agent selbst** — Entscheidungs- und Kontrolllogik
müssen außerhalb des Modells liegen (`LH-FA-LLM-001`). Zugleich muss das
Konfidenz-Gate **außerhalb der Aktion** liegen und darf nicht umgangen
werden (`LH-FA-POL-006`), und neue Beobachtungsquellen/Aktionstypen müssen
ergänzbar sein, **ohne den Kern zu ändern** (`LH-QA-04`).

Wenn die Belief-, Gate- und VoI-Logik direkt gegen einen konkreten
LLM-SDK-Aufruf oder eine konkrete Beobachtungsquelle programmiert wäre,
wären Anbieter-Wechsel, deterministische Tests (`LH-QA-03`) und Replay
nicht ohne Kern-Änderung möglich — und die Versuchung entstünde, Konfidenz
implizit „im Modell" zu lassen, statt sie in prüfbare, gate-fähige Zahlen
zu überführen (`LH-FA-LLM-003`).

## Entscheidung

Wir wählen eine **hexagonale Architektur (Ports & Adapter)**: Der
Belief-Kern (Domäne, Belief-Engine, Konfidenz-Gate, VoI-Selektor,
Eskalation, Audit) definiert **Ports** (Interfaces); konkrete
Sprachmodelle und Beobachtungsquellen sind **Adapter** hinter diesen
Ports. Der Kern importiert **nie** einen Adapter; die Verdrahtung
geschieht im Orchestrator/Runtime.

## Verglichene Alternativen

### Option A — Direkte LLM-/Quellen-Aufrufe im Kern

- Pro: minimaler Boilerplate; schnellster erster Durchstich.
- Contra: bricht `LH-FA-LLM-001` (Logik im Modell verflochten), kein
  Anbieter-Wechsel ohne Kern-Änderung (`LH-QA-04`), kein deterministischer
  Test/Replay (`LH-QA-03`).

### Option B — LLM-Framework als Rahmen (Agent = Framework-Schleife)

- Pro: viele Bausteine fertig.
- Contra: macht das Framework zum Agenten und das Modell zum Kern — das
  Gegenteil von `LH-FA-LLM-001`; Gate-Nicht-Umgehbarkeit (`LH-FA-POL-006`) liegt
  außerhalb unserer Kontrolle.

### Option C — Hexagonale Architektur mit Ports & Adaptern (gewählt)

- Pro: erfüllt `LH-FA-LLM-001`, `LH-FA-LLM-004` (Anbieter-Austauschbarkeit),
  `LH-QA-04` (Quellen/Aktionen erweiterbar ohne Kern-Änderung); Gate als
  eigener Kern-Schritt vor der Aktion (`LH-FA-POL-006`); deterministisch
  testbar über Fake-Adapter (`LH-QA-03`).
- Contra: etwas Port-/Adapter-Boilerplate; Disziplin nötig, damit der Kern
  adapterfrei bleibt — deshalb die Fitness Function unten.

## Konsequenzen

- Positiv: Anbieter- und Quellen-Wechsel sind Adapter-lokal; Replay und
  deterministische Tests werden über Fake-Adapter möglich.
- Negativ: zusätzliche Interface-Schicht; jeder neue Quellentyp braucht
  einen Port-Vertrag.
- Folgepflicht: Fitness Function (unten), Pflege von
  `spec/architecture.md` §2 als Layering-Quelle.

## Fitness Function (falls maschinell prüfbar)

Greift mit dem ersten Code-Slice (bis dahin „Nicht behauptet" im
Sensors-Roster). Werkzeug ist das Harness-Arch-Gate `a-check`
(sprach-/ziel-agnostisch, digest-gepinnt analog `d-check`); siehe die
Fitness Function in ADR-0002.

| Tooling | Regel | Make-Target |
|---|---|---|
| `a-check` (Harness-Arch-Gate, sprach-/ziel-agnostisch) | Kern-Pakete (Engine, Gate, VoI, Eskalation, Audit) dürfen kein Adapter-Paket importieren | `make arch-check` |

## Re-Evaluierungs-Trigger

Wenn die Implementierungssprache final entschieden ist (`LH-RB-04`,
eigener ADR) oder wenn ein zweiter LLM-Anbieter angebunden wird und der
Port-Vertrag sich als unzureichend erweist.

## Geschichte

| Datum | Ereignis | Verweis |
|---|---|---|
| 2026-06-22 | Proposed | Bootstrap (Modul 2, Schritt 8) |
| 2026-07-04 | Fitness-Function-Werkzeug auf das Harness-Arch-Gate `a-check` festgelegt (statt sprachabhängiger ArchUnit/Linter-Liste); Bindung an ADR-0002 (Kotlin Multiplatform) | Diskussion Sprachwahl |
