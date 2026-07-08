# ADR-0009: DecisionPolicy — benanntes Konfigurations-Bündel über Gate/Zyklus/VoI, kein neuer Ausführungspfad

**Status:** Proposed

**Datum:** 2026-07-08

**Autor:** belief-agent

**Bezug:** [`LH-FA-POL-006`](../../../spec/lastenheft.md#lh-fa-pol-006--nicht-umgehbares-gate), [`LH-FA-POL-007`](../../../spec/lastenheft.md#lh-fa-pol-007--konfigurierbare-schwellwerte), [`LH-FA-ESK-001`](../../../spec/lastenheft.md#lh-fa-esk-001--eskalationsbedingung), [`LH-FA-VOI-003`](../../../spec/lastenheft.md#lh-fa-voi-003--gewinn-kosten-abwägung), [`LH-QA-02`](../../../spec/lastenheft.md#lh-qa-02--konservatives-standardverhalten-fail-safe), [`LH-QA-03`](../../../spec/lastenheft.md#lh-qa-03--testbarkeit); [ADR-0003](0003-hexslice-architektur.md), [ADR-0007](0007-eskalations-schwelle.md), [ADR-0008](0008-schwellwerte-spec-konform.md), [ADR-0001](0001-hexagonal-llm-port.md).

**Schärft:** [`architecture.md` §Use-Case: Entscheidungszyklus mit Konfidenz-Gate](../../../spec/architecture.md#use-case-entscheidungszyklus-mit-konfidenz-gate-lh-fa-obs-002-lh-fa-pol-001-lh-fa-pol-004) — die Naht, an der eine Policy die Knöpfe bündelt. Wird mit `Accepted` verbindlich; bis dahin Vorschlag.

---

## Kontext

Der Entscheidungszyklus (`ARC-09`, `Entscheidungszyklus.entscheide`) verbindet
**handeln | sammeln | eskalieren**. Sein Verhalten hängt heute an vier bereits
vorhandenen, aber **verstreuten** Stellschrauben:

| Achse | Parameter | Ort |
|---|---|---|
| Sicherheits-Strenge der Freigabe | `GateSchwellen` (Erfolg je Wirkungsklasse + θ_other_block) | `KonfidenzGate`, injiziert in `AktionGaten` |
| Eskalieren-statt-ablehnen | `eskalationsSchwelle` θ_esc (Default 0,30, `ADR-0007`) | `Entscheidungszyklus`-Konstruktor |
| Explorationstiefe / Kosten | `Budget` (maxSchritte, Kosten) | `entscheide(...)`-Parameter je Aufruf |
| „welche Information" | VoI = erwarteter Gewinn **/ Kosten** über die Top-2-Trennung | `VoiSelektor` (heute fixe Regel) |

Um eine kohärente **Haltung** einzustellen — z. B. „konservativ", „explorativ",
„kostenoptimiert", „sicherheitskritisch" — muss der Composition-Root diese vier
Parameter heute **einzeln und konsistent** setzen. Das ist fehleranfällig
(strenge Gate-Schwellen mit versehentlich laxem θ_esc), nicht als „diese Haltung
ist konservativ" testbar und nirgends als Menge zulässiger Presets dokumentiert.

Der naheliegende Reflex — eine `DecisionPolicy` als **Strategie im
Ausführungspfad** (`BeliefState → DecisionPolicy → Decision → Gate`) — kollidiert
mit einer Kern-Zusicherung: das Gate ist **nicht umgehbar** (`LH-FA-POL-006`).
`Aktionsfreigabe.Freigegeben` ist ausschließlich in `AktionGaten` konstruierbar
(`internal`); die Schwellen-Monotonie ist fail-closed erzwungen
(`GateSchwellen.init`, `ADR-0008`); irreversible Aktionen brauchen zusätzlich
menschliche Freigabe (`LH-FA-POL-004`). Eine Policy, die *im Pfad* freigeben
könnte, wäre ein **zweiter Freigabe-Pfad** und damit ein Loch in genau der
Eigenschaft, die das Framework ausmacht.

## Entscheidung

Wir führen **`DecisionPolicy` als reines, invariant-erhaltendes
Konfigurations-Bündel** ein: ein Wertobjekt im Kern (`ADR-0003`,
Abhängigkeit nach innen), das die vier vorhandenen Stellschrauben
(`GateSchwellen`, θ_esc, `Budget`-Vorgabe, VoI-Gewichtung) zu **benannten
Presets** zusammenfasst. Der Composition-Root wählt **ein** Preset statt vier
Parameter zu jonglieren; der Entscheidungszyklus und das Gate bleiben
unverändert und konsumieren wie bisher konkrete Schwellen/Budget.

**Grenze (nicht verhandelbar):** Die Policy **konfiguriert nur Knöpfe, die Gate
und Zyklus ohnehin exponieren** — sie ist **kein** Akteur im Ausführungspfad und
kann die harten Invarianten nicht überschreiben: Nicht-Umgehbarkeit
(`LH-FA-POL-006`), Schwellen-Monotonie (`ADR-0008`), Resthypothese-Sperre
(`LH-FA-POL-005`), menschliche Freigabe für Irreversibles (`LH-FA-POL-004`),
fail-closed (`LH-QA-02`). „explorativ" darf ein Deploy nicht auto-freigeben. Ein
Preset, das die `GateSchwellen`-Invarianten verletzt, **scheitert fail-closed
beim Bau** (bestehende `require`s), nicht zur Laufzeit.

So gezeichnet — Policy sitzt **neben/über**, nicht **vor** dem Zyklus:

```
DecisionPolicy  (Preset: GateSchwellen + θ_esc + Budget + VoI-Gewicht)
      │  konfiguriert (am Composition-Root)
      ▼
BeliefState ─► Entscheidungszyklus ─► { Gate ⇄ VoI } ─► Zyklusergebnis ─► Executor
                                       ▲ harte Invarianten policy-fest
```

## Verglichene Alternativen

### Option A — Status quo: Knöpfe bleiben verstreut (verworfen)

- Pro: nichts zu bauen; maximale Freiheit pro Aufruf.
- Contra: keine benannte, kohärente Haltung; jede Haltung muss der
  Composition-Root manuell konsistent zusammensetzen (leicht widersprüchlich);
  nicht als „ist konservativ" testbar; keine dokumentierte Preset-Menge.

### Option B — DecisionPolicy als Strategie im Ausführungspfad (verworfen)

- Pro: maximale Ausdruckskraft; eine Policy könnte beliebige, auch
  zustandsabhängige Entscheidungslogik tragen.
- Contra: durchbricht `LH-FA-POL-006` — eine freigebende Policy *im Pfad* ist ein
  zweiter Freigabe-Pfad neben `AktionGaten`; dupliziert die bereits im Kern
  liegende Entscheidungslogik (`ADR-0001`/`ADR-0003`); erhöht das Risiko, dass
  „explorativ" versehentlich Sicherheit aufweicht (Fail-open-Richtung, gegen
  `LH-QA-02`).

### Option C — DecisionPolicy als reines Konfigurations-Bündel (gewählt)

- Pro: kleiner, realer Gap statt neuer Pfad; harte Invarianten bleiben im Gate
  und sind policy-fest; jedes Preset ist ein deterministisch testbares Tripel
  (`LH-QA-03`); respektiert `ADR-0003` (Wertobjekt im Kern) und
  `LH-FA-POL-006/007`; genau die gewünschte Austauschbarkeit „Policy tauschen,
  Rest unangetastet".
- Contra: Presets sind nur so gut wie ihre Werte (Kalibrierung offen, wie θ in
  `ADR-0007`); nicht jede denkbare Haltung ist durch vier Knöpfe ausdrückbar
  (kontext-/zustandsabhängige Strategien bleiben bewusst außen vor → Option B als
  späterer Re-Trigger).

## Konsequenzen

- Positiv: eine benannte, testbare Haltung; der Composition-Root wählt **ein**
  Preset statt vier Parameter konsistent zu halten; Sicherheit bleibt im Gate
  (Policy kann nicht auto-freigeben); erweiterbar ohne Eingriff in Zyklus/Gate.
- Negativ: **noch ein Ort** für Schwellen-Werte — verlangt Reconciliation-
  Disziplin (ein Preset baut ein bereits invariant-geprüftes `GateSchwellen` und
  respektiert die θ-Verträge aus `ADR-0007`/`ADR-0008`); Preset-Kalibrierung ist
  Folgearbeit.
- Folgepflicht: Implementierungs-Slice (Wertobjekt + Presets `konservativ` /
  `explorativ` / `kostenoptimiert` / `sicherheitskritisch` + Composition-Root-
  Bindung + Determinismus-Tests je Preset); VoI-Gewichtung setzt eine
  parametrisierbare `VoiSelektor`-Kennzahl voraus (heute fix); Roadmap-Absatz
  „Policies" in der README.

## Fitness Function (falls maschinell prüfbar)

| Tooling | Regel | Make-Target |
|---|---|---|
| a-check | `DecisionPolicy` lebt im Kern (`hexagon/domain` bzw. `application`) und importiert **kein** Adapter/Framework (`ADR-0001`/`ADR-0003`) | `make arch-check` |
| Kotlin `internal` + Test | Es bleibt **genau ein** Konstruktionsort für `Aktionsfreigabe.Freigegeben` (`AktionGaten`); die Policy fügt keinen zweiten Freigabe-Pfad hinzu (`LH-FA-POL-006`) | `make test` |
| Test (`LH-QA-03`) | Gleiches Preset + gleiche Ports + gleicher Prior ⇒ gleiches `Zyklusergebnis`; ein invariantenverletzendes Preset wirft fail-closed beim Bau | `make test` |

## Re-Evaluierungs-Trigger

Wenn kontext-/zustandsabhängige Policies gebraucht werden (dann Option B neu
bewerten); wenn ein fünfter Knopf nötig wird; welle-05 (LLM-Port) liefert
kalibrierte Preset-Werte; oder die θ-Reconciliation aus `ADR-0007` verschiebt die
Default-Bündel.

## Geschichte

| Datum | Ereignis | Verweis |
|---|---|---|
| 2026-07-08 | **Proposed** — aus Architektur-Diskussion „DecisionPolicy" (Austauschbarkeit von Haltungen ohne Eingriff in Gate/Zyklus) | — |
