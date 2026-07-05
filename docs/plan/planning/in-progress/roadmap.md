# Roadmap — belief-agent

**Status:** Aktiv. **Letzte Änderung:** 2026-07-05.

**Format-Regel:** Die Roadmap ist eine Reihenfolge von **Wellen**, keine
Reihenfolge von Terminen. Termine — falls überhaupt — sind Konsequenz der
Wellen-Schätzung, nicht Treiber.

---

## Aktuelle Welle

**Aktuelle Welle: [`welle-02-evidenz-audit`](../welle-02-evidenz-audit.md)** —
Evidenz-Aufnahme + Audit.

- `slice-005` (Domänentypen Beobachtung/Quelle/Evidenz/Ereignis) **geliefert**
  (`make gates` grün, 36 Tests); liegt in `in-progress/` bis Welle-Closure.
- `slice-006` (Dedup korrelierter Beobachtungen, `LH-FA-OBS-004`) **geliefert**
  (`make gates` grün, 46 Tests, Line-Coverage 96,81 %); reine Domänen-Regel
  `Dedup` in `hexagon:domain`, Kriterium: gleiche `Quelle` + `Evidenz`
  (Zeitstempel-unabhängig) → nicht doppelt zählen. Liegt in `in-progress/` bis
  Welle-Closure.
- `slice-007` (Ereignisprotokoll + Belief-Rekonstruktion,
  `LH-FA-AUD-001`/`002`/`003`) **geliefert** (`make gates` grün, 59 Tests,
  Line-Coverage 97,37 %); `EreignisProtokoll` (append-only, monotone
  Zeitstempel, Vergangenheit nicht mutierbar) + `Rekonstruktion` (Replay →
  Belief) in `hexagon:domain`. **Audit-Port → `slice-008`** (Weg C:
  anwendungsweiter Port gehört in die application-Schicht, nicht in die Domäne).
  Liegt in `in-progress/` bis Welle-Closure.
- `slice-008` (Fundament: `hexagon:application`-Modul + **Audit-Port** `ARC-06`
  + Dockerfile-Multi-Modul + Multi-Modul-`arch-check`) **geliefert** (`make
  gates` grün; arch-check **echt durchsetzend** über domain+application via
  a-check **v0.11.0**, negativ-getestet — `CO-001`-Klasse **ohne Carveout**
  aufgelöst). Liegt in `in-progress/` bis Welle-Closure.
- `slice-009` (Pipeline `belief-aktualisieren` + LLM-/Uhr-Port + Fake-LLM,
  `LH-FA-OBS-002`) **geliefert** (`make gates` grün, 67 Tests; erstes
  `adapters:*`-Modul `llm-fake`, arch-check echt über drei Module). Liegt in
  `in-progress/` bis Welle-Closure.
- `slice-010` (Beobachtungs-Port + Quelle-Adapter + E2E-Persistenz,
  `LH-FA-OBS-001`) **geliefert** (`make gates` grün, 71 Tests; E2E
  `Quelle→Update→Protokoll→Persistenz→Rekonstruktion` grün). Liegt in
  `in-progress/` bis Welle-Closure.
- **⇒ Resume-Punkt: welle-02-Closure** — alle Slices `005`..`010` geliefert,
  Closure-Trigger erfüllt (E2E-Spur grün). Closure: Closure-Notizen je Slice,
  Slices → `done/`, Lerneintrag in `done/welle-02-evidenz-audit-results.md`,
  Roadmap auf welle-03.

## Nächste Wellen

| Welle | Trigger | Wichtigste Slices | Geschätzter Aufwand |
|---|---|---|---|
| welle-02-evidenz-audit | welle-01 done | Beobachtungs-Aufnahme, Bayes-Update-Pipeline, Audit-/Event-Log (`LH-FA-OBS`, `LH-FA-AUD`) | M |
| welle-03-aktionen-gates | welle-02 done | Wirkungsklassen, Konfidenz-Gate, menschliche Freigabe (`LH-FA-ACT`, `LH-FA-POL`) | M |
| welle-04-voi-eskalation | welle-03 done | VoI-Selektor, Eskalations-Manager, Budget (`LH-FA-VOI`, `LH-FA-ESK`) | M |
| welle-05-llm-port | welle-03 done | LLM-Port + erster Adapter, Konfidenz-Externalisierung (`LH-FA-LLM`) | L |

## Meilensteine

Meilenstein-/Release-Punkte sind **extern beobachtbare Zustände** und leiten
sich aus *tatsächlich existierenden* Wellen/Slices ab (Regelwerk Modul 06) —
**keine Vorab-Bindung an noch nicht existierende Wellen**. Ob ein Meilenstein
bzw. Release-Punkt erreicht ist, wird **je Slice** entschieden und erst dann
mit konkreter Wellen-/Slice-Bindung eingetragen, wenn die liefernden Slices
in `done/` sind.

| Meilenstein | Welle(n)/Slice(s) | Trigger (beobachtbar) | Status |
|---|---|---|---|
| M1 — Belief-Kern lauffähig | welle-01 (`slice-001`..`slice-004`) | ungültiger Belief State wird nachweislich zurückgewiesen (`LH-FA-BEL-004`) **und** deterministisches Bayes-Update grün (`make test`) | **erreicht (2026-07-04)** |

**Ausblick (unverbindlich, noch ohne Wellen-Bindung):** vollständiger
Entscheidungszyklus (Gate + VoI + Eskalation) und Sprachmodell-Anbindung sind
erklärte spätere Ziele. Sie werden als Meilenstein mit beobachtbarem Trigger
eingetragen, sobald die tragenden Slices existieren und ein Release-/
Beobachtungs-Punkt tatsächlich erreicht ist — Entscheidung je Slice.

## Abhängigkeitsgraph

```mermaid
flowchart LR
    W1[welle-01<br/>Belief-Kern]
    W2[welle-02<br/>Evidenz + Audit]
    W3[welle-03<br/>Aktionen + Gates]
    W4[welle-04<br/>VoI + Eskalation]
    W5[welle-05<br/>LLM-Port]

    W1 --> W2
    W2 --> W3
    W3 --> W4
    W3 --> W5
```

## Abgeschlossene Wellen

| Welle | Abgeschlossen | Ergebnis |
|---|---|---|
| [`welle-01-belief-kern`](../welle-01-belief-kern.md) | 2026-07-04 | M1 erreicht; 30 Tests, 94,83 % Coverage; [Ergebnisse](../done/welle-01-belief-kern-results.md). Rest: `CO-001` (arch-check). |

## Historische Trigger-Verschiebungen

| Datum | Was wurde geändert? | Warum? |
|---|---|---|
| 2026-06-22 | Initiale Roadmap (Bootstrap) | — |
| 2026-07-04 | Welle-01 gestartet (Status `in-progress`); Slices `slice-001`..`slice-004` in `open/` angelegt | Start-Trigger erfüllt: `ADR-0001` & `ADR-0002` Accepted |
| 2026-07-04 | Meilensteine entkoppelt: M2/M3-Vorab-Bindung an noch nicht existierende Wellen entfernt (Je-Slice-Entscheidung); M1-Trigger auf beobachtbaren Zustand geschärft | Regelwerk Modul 06: keine Phantom-Bindung, beobachtbare Trigger |
| 2026-07-04 | Welle-01 abgeschlossen (Status `done`, Slices → `done/`); M1 erreicht; `CO-001` (arch-check) angelegt | Closure-Trigger erfüllt; `make gates` grün, Review durchgeführt |
| 2026-07-04 | `CO-001` aufgelöst: a-check v0.10.0 (fail-closed-Fix); `arch-check` verdrahtet, `make gates` = 5 Gates | Upstream-Fix des gemeldeten KMP-Falsch-negativ |
| 2026-07-04 | welle-02-evidenz-audit aufgesetzt (`slice-005`..`slice-008` in `open/`); d-check-Module erweitert (`MR-006`) + `version.md` | welle-01 done → nächste Welle |
| 2026-07-05 | `slice-006` geliefert (Dedup korrelierter Beobachtungen, `LH-FA-OBS-004`); Resume-Punkt → `slice-007` | `make gates` grün (46 Tests, 96,81 % Coverage); DoD erfüllt, liegt in `in-progress/` bis Welle-Closure |
| 2026-07-05 | `slice-007` geliefert (Ereignisprotokoll + Rekonstruktion, `LH-FA-AUD-001`/`002`/`003`); Audit-Port nach `slice-008` verschoben (Weg C); Resume-Punkt → `slice-008` | `make gates` grün (59 Tests, 97,37 % Coverage); Audit-Port ist anwendungsweiter Port → application-Schicht (`architecture.md` §2), nicht Domäne; slice-007 bleibt reiner domain-Slice |
| 2026-07-05 | `slice-008` zerlegt (Modul 5, zu groß): `slice-008` (Fundament: Modul + Audit-Port + Multi-Modul-`arch-check`), `slice-009` (Pipeline), `slice-010` (Quelle + E2E) | 7 DoD-Punkte über mehrere Schichten + Multi-Modul-a-check-Risiko → nicht in einer Sitzung lieferbar; Schnitt nach Lieferwert; a-check-Risiko zuerst isoliert retiren |
| 2026-07-05 | `slice-008` (Fundament) geliefert; a-check v0.10.0 → **v0.11.0** (Multi-Modul-KMP-Resolution, `MR-005`); Resume-Punkt → `slice-009` | v0.10.0 konnte Multi-Modul nicht durchsetzen (Guard-Reject bzw. falsch-grün, negativ-getestet); Fix-Prompt an a-check → v0.11.0 löst datei-mengen-bewusst auf, echt durchsetzend; kein Carveout |
| 2026-07-05 | `slice-009` (Pipeline `belief-aktualisieren`) geliefert; erstes `adapters:*`-Modul `llm-fake`; Resume-Punkt → `slice-010` | `make gates` grün (67 Tests); Use-Case + LLM-/Uhr-Port + Fake-LLM; arch-check echt über domain/application/adapters (a-check v0.11.0, Adapter-Root ergänzt) |
| 2026-07-05 | `slice-010` geliefert (Beobachtungs-Port + Quelle-/Audit-Adapter + E2E); **welle-02-Closure-Trigger erfüllt** | `make gates` grün (71 Tests); E2E `Quelle→Update→Protokoll→Persistenz→Rekonstruktion` demonstriert die Welle-Ziele (`LH-FA-OBS-001`/`002`, `LH-FA-AUD-002`) |
