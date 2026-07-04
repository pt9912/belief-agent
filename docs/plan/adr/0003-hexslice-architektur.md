# ADR-0003: HexSlice-Architektur — vertikale Use-Case-Slices im hexagonalen Kern (KMP, Gradle-Multi-Modul)

**Status:** Proposed

**Datum:** 2026-07-04

**Autor:** belief-agent

**Bezug:** [`LH-QA-04`](../../../spec/lastenheft.md#lh-qa-04--erweiterbarkeit), [`LH-FA-LLM-001`](../../../spec/lastenheft.md#lh-fa-llm-001--sprachmodell-als-austauschbares-modul)

**Schärft:** [`architecture.md §2`](../../../spec/architecture.md#2-schichten-und-constraints)

**Verhältnis:** Schärft `ADR-0001` (hexagonal — Ports & Adapter, Entscheidungslogik im Kern), baut auf `ADR-0002` (KMP) auf. **Supersedet keine** ADR.

---

## Kontext

`ADR-0001` legt die hexagonale Architektur fest (Ports & Adapter, Kern
adapterfrei), lässt aber die **innere Organisation des Kerns** offen;
`architecture.md §2` beschreibt sie bisher **komponenten-orientiert**
(ARC-02 Engine, ARC-03 Gate, ARC-04 VoI, ARC-05 Eskalation …). Das macht
Ports global und trennt Use Cases nicht sichtbar.

Das Referenzmuster **HexSlice** (github.com/pt9912/hexslice-architecture)
kombiniert Hexagonal mit **Vertical Slice**: Der Application Core wird nach
**Use Cases** organisiert (jeder Use Case ein vertikaler Slice mit
command/query · handler · validator · result), **Ports leben lokal** beim
Use Case (drei Lokalitätsstufen: use-case / business-area / application-wide).
Das passt zu `LH-QA-04` (neue Quellen/Aktionen ergänzbar ohne Kern-Änderung)
und zum Werkzeug **a-check**, dessen Rollen `domain/app/port/adapter` exakt
die HexSlice-Abhängigkeitsregeln prüfen.

## Entscheidung

Wir adoptieren **HexSlice** als innere Struktur des hexagonalen Kerns,
realisiert als **Gradle-Multi-Modul** auf **Kotlin Multiplatform** (`ADR-0002`;
JVM-Ziel zuerst, `LH-RB-04`). Verzeichnis-/Modulstruktur (Paketbasis
`dev.beliefagent`):

```
hexagon/
  domain/                          # Entities · Value Objects · Domain-Events/-Services (commonMain)
  application/<area>/<use-case>/    # command|query · handler · validator · result
    ports/                          # use-case-lokaler Port
  application/<area>/ports/         # business-area-geteilter Port
  application/ports/                # anwendungsweiter Port
adapters/
  inbound/<type>/<area>/            # ruft Use Cases auf (cli, …) — jvmMain
  outbound/<type>/<area>/           # implementiert Ports (LLM, Quellen, Audit) — jvmMain
```

Jede Area/jeder Adapter ist ein eigenes Gradle-Modul; die
Abhängigkeitsrichtung zeigt **nach innen** (`adapter → application → domain`).
Die Reinheit ist **dreifach** gesichert: Gradle-Modul-Grenzen (der
Domain-/Application-Kern hat keine Adapter-Dependency), **a-check**
(Rollen-Regeln) und die **KMP-Source-Set-Sichtbarkeit** (`commonMain` sieht
`jvmMain` nicht → `application → adapter` ist compilerseitig unmöglich).

## Verglichene Alternativen

### Option A — Komponenten-hexagonal, ein Modul (Ausgangs-Stand)

- Pro: wenig Struktur-Overhead.
- Contra: globale Ports, Use Cases nicht sichtbar getrennt; a-check nur grob
  nutzbar; skaliert schlecht mit wachsenden Aktionstypen (`LH-QA-04`).

### Option B — Plain-JVM Multi-Modul (wie d-migrate)

- Pro: einfachste Modul-Semantik; Referenzprojekt vorhanden.
- Contra: verwirft die strukturelle KMP-`commonMain`-Reinheit (`ADR-0002`
  müsste supersedet werden) ohne fachlichen Gewinn; Ports als eigene Module
  statt lokal beim Use Case (nicht HexSlice).

### Option C — HexSlice auf KMP-Multi-Modul (gewählt)

- Pro: vertikale Use-Case-Slices + lokale Ports (`LH-QA-04`); dreifache
  Reinheits-Erzwingung; a-check bekommt pro Layer einen eigenen Modul-Baum
  (entschärft das KMP-Multi-Root-Problem des Arch-Gates); `ADR-0002` bleibt.
- Contra: mehr Module/Boilerplate; a-check-Feinregeln über Source-Set-Grenzen
  bleiben abhängig von der offenen a-check-Rückmeldung (siehe Fitness Function).

## Konsequenzen

- Positiv: Use Cases sind lokal fassbar/testbar; neue Quellen/Aktionen als
  neue Slices/Adapter ohne Kern-Änderung (`LH-QA-04`); LLM/Quellen als
  outbound-Adapter hinter lokalen Ports (`LH-FA-LLM-001`).
- Negativ: mehr Gradle-Module und Konventions-Aufwand am Rand.
- Folgepflicht: `architecture.md §2` von ARC-Komponenten auf
  domain/application-slices/ports/adapter umstellen (ARC-IDs bleiben als
  Verantwortungs-Anker erhalten, neu zugeordnet); a-check-Config auf
  HexSlice-Rollen.

## Fitness Function (falls maschinell prüfbar)

| Tooling | Regel | Make-Target |
|---|---|---|
| Gradle-Modul-Graph | `hexagon:domain`/`hexagon:application` haben keine Abhängigkeit auf `adapters:*` | `make build` |
| `a-check` (Rollen domain/app/port/adapter) | domain importiert nur domain; application nutzt domain+port, keinen Adapter | `make arch-check` |

`arch-check` wird verdrahtet, sobald die a-check-Rückmeldung zum
KMP-Source-Set-Fall vorliegt (gemeldet, offen); bis dahin tragen
Modul-Grenzen + KMP-Sichtbarkeit die Reinheit.

## Re-Evaluierungs-Trigger

Wenn die Slice-/Modul-Zahl unhandlich wird (Konventions-Plugin nötig); wenn
a-check den KMP-Fall löst (arch-check scharfschalten); wenn ein Nicht-JVM-Ziel
gefordert wird (`ADR-0002`-Re-Eval).

## Geschichte

| Datum | Ereignis | Verweis |
|---|---|---|
| 2026-07-04 | Proposed — HexSlice auf KMP-Multi-Modul, schärft ADR-0001 | Diskussion Architektur (HexSlice vs. hexagonal, d-migrate-Referenz) |
