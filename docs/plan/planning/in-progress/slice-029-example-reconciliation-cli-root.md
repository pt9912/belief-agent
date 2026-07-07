# Slice slice-029: Beispiele mit CLI-Composition-Root reconciliieren

**Status:** open → next → in-progress → done (siehe
[Planning-README](../README.md)).

**Welle:** gezielter Follow-up zu `welle-05-llm-port` nach `slice-024`.

**Bezug:** `LH-FA-LLM-002`, `LH-FA-LLM-004`, `LH-FA-POL-006`, `LH-OUT-04`,
`LH-QA-03`, `LH-QA-04`; `ADR-0001`, `ADR-0002`, `ADR-0003`; `ARC-08`,
`ARC-09`.

**Autor:** pt9912. **Datum:** 2026-07-07.

---

## 1. Ziel

Die runnable Beispiele `example:langchain` und `example:koog` bleiben
Framework-Adapter-Demos, werden aber an den seit `slice-024` vorhandenen
produktiven `adapters:inbound:cli`-Composition-Root und die Executor-Grenze
reconciled.

## 2. Definition of Done

- [x] Beide Beispiel-READMEs erklären klar: Produktiver Einstieg ist
  `adapters:inbound:cli`; das jeweilige Beispiel demonstriert nur die
  `LlmPort`-/Framework-Grenze.
- [x] Beide Beispiel-Runs zeigen in ihrer Ausgabe die Executor-Grenze über
  `Zyklusergebnis.Gehandelt.freigabe.aktion`, nicht nur ein generisches
  `executor_allowed=true`.
- [x] Beispiele importieren keinen CLI-Adapter und duplizieren den
  Composition-Root nicht; Framework-Beispiel und Produktiv-Root bleiben
  getrennte Rollen.
- [x] `make example-langchain`, `make example-koog`, `make doc-check` und
  `make gates` grün.
- [x] Review- und Verification-Harness-Berichte ohne offene Findings bzw.
  DoD-Verletzungen.
- [x] Closure-Notiz mit Steering-Loop-Eintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `example/langchain/README.md` | update | Beispielrolle gegen `adapters:inbound:cli` klarstellen. |
| `example/koog/README.md` | update | Beispielrolle gegen `adapters:inbound:cli` klarstellen. |
| `example/langchain/src/main/.../Main.kt` | update | Laufzeitausgabe auf `freigabe.aktion`-Executor-Grenze schärfen. |
| `example/koog/src/main/.../Main.kt` | update | Laufzeitausgabe auf `freigabe.aktion`-Executor-Grenze schärfen. |
| `docs/reviews/*slice-029*` | neu | Review-Harness-Artefakt. |
| `docs/verifications/*slice-029*` | neu | Verification-Harness-Artefakt. |
| `docs/plan/planning/in-progress/roadmap.md` | update | Follow-up aktivieren und abschließen. |

## 4. Trigger

`slice-024` ist in `done/` und liefert den produktiv gedachten
CLI-Composition-Root.

## 5. Closure-Trigger

DoD vollständig + Review/Verification + `make gates` grün + Slice in `done/`.

## 6. Risiken und offene Punkte

- Beispiele dürfen nicht zum zweiten Composition-Root werden; sie sollen nur
  die Framework-Port-Grenze demonstrieren.
- Keine neue Abhängigkeit von `example:*` auf `adapters:inbound:cli`, solange
  die Beispiele keine CLI-Runtime testen.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** Der kleine Doku-/Output-Schnitt reichte: Die Beispiele
bleiben unveraendert Framework-Adapter-Demos und muessen den CLI-Root nicht
importieren. Die Beispiel-Ausgabe macht nun sichtbar, dass produktive
Verdrahtung in `adapters:inbound:cli` lebt und Ausfuehrung an
`Zyklusergebnis.Gehandelt.freigabe.aktion` gebunden ist.

**Was ist offen geblieben:** Echte Provider-/Approval-/Ausfuehrungs- und
Persistenzadapter bleiben separate Stabilisierung; die Beispiele testen
weiterhin Mock-/Demo-Runner, keine reale Provider-Komposition.

**Steering-Loop:** Beispiel-READMEs sollten nach Composition-Root-Slices
explizit zwischen Demonstrations-Wiring und produktivem Runtime-Root
unterscheiden, sonst driftet Integrationsdoku in Richtung zweiter Root.

**Folge-Slices:** Echte Approval-, Ausfuehrungs- und Persistenzadapter fuer das
CLI-Bundle bleiben separat.

## 8. Sub-Area-Modus-Begründung

Alle berührten Sub-Areas sind GF: `example/langchain` und `example/koog` sind
runnable Beispiel-Adapter-Demos mit bestehenden Konventionen; es entsteht keine
neue Sub-Area.
