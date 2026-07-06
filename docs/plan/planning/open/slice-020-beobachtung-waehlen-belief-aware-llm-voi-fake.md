# Slice slice-020: beobachtung-waehlen belief-aware + LLM-VoI-Fake

**Status:** open → next → in-progress → done (siehe
[Planning-README](../README.md)).

**Welle:** `welle-05-llm-port` ([Roadmap](../in-progress/roadmap.md)).

**Bezug:** `LH-FA-VOI-002`, `LH-FA-LLM-002`, `LH-FA-LLM-003`, `LH-QA-03`; `ADR-0001`,
`ADR-0003`; `ARC-04`, `ARC-07`.

**Autor:** pt9912. **Datum:** 2026-07-06.

---

## 1. Ziel

`slice-016` liefert fixe Kandidaten; dieser Slice schließt den offenen Punkt
`welle-04` **F4b** (`belief-abhängige` Kandidaten) im Rahmen von
`welle-05-llm-port` ab. `BeobachtungsAuswahlPort` liefert
belief-abhängige `VoiKandidat`-Listen über einen LLM-fähigen
Fallback/Adapter (initial über deterministischen Fake), damit `beobachtung-waehlen`
nicht länger mit einer statischen Kandidatenliste arbeitet.

## 2. Definition of Done

- [ ] `LH-FA-VOI-002` erfüllt: die Beobachtungsauswahl nutzt belief-abhängige
  Kandidatenlisten und liefert die bestgeeignete Beobachtung deterministisch bei
  gegebenen Fake-Daten.
- [ ] `LH-FA-LLM-002` erfüllt: der LLM-Fake erzeugt **modellgrenzenklar** die
  für VoI benötigten Kandidaten (inkl. Kosten/Discrimination), keine Domänenregel
  enthält Provider-spezifische Logik.
- [ ] `LH-FA-LLM-003` erfüllt: generierte Kandidaten werden als explizit
  strukturierte Werte (inkl. `erwarteteDiskriminierung`) übergeben; keine
  stillen Defaults.
- [ ] `LH-QA-03` erfüllt: neuer Pfad hat fassbare Tests (Unit + deterministische
  Fakes) für mindestens: positive/negative Kandidaten und leere Kandidaten.
- [ ] `make gates` grün.
- [ ] Doku-Update in `docs/user/integration.md`, falls der Integratorvertrag für
  VoI-Kandidaten konkretisiert wird.
- [ ] Closure-Notiz mit Steering-Loop-Eintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/beobachtungwaehlen/ports/BeobachtungsAuswahlPort.kt` | update | Kandidaten-Contract belief-kontextfähig machen |
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/beobachtungwaehlen/BeobachtungWaehlen.kt` | update | Auswahl-Use-Case mit belief-nahem Aufrufpfad verbinden |
| `adapters/outbound/voi-fake/src/commonMain/kotlin/dev/beliefagent/adapter/voi/FakeVoiKandidatenQuelle.kt` | neu | Deterministischer, belief-aware LLM-ähnlicher Fake (`LLM-VoI-Fake`) |
| `hexagon/application/src/commonTest/...` | update | neue Use-Case-/Port-Tests inkl. F4b-Fall |
| `hexagon/application/src/commonTest/...` | neu | Regression gegen `F4b` (belief-abhängige Liste → nicht-statische Wiederholung) |
| `example/langchain` / `example/koog` | update | Beispiele auf neuen `beobachtungs-waehlen`-Contract verdrahten |
| `docs/user/integration.md` | update | Vertrag und Konfigurationswirkung dokumentieren |

## 4. Trigger

`welle-05-llm-port` offen + Abschluss von `slice-019` (LLM-Port-Adapter) +
`slice-016` liefert funktionierenden `BeobachtungWaehlen`-Use-Case.

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz + Slice in `done/`.

## 6. Risiken und offene Punkte

- Signaturänderung am `BeobachtungsAuswahlPort` kann den Test-/Example-Fluss
  bremsen; Rückwärtskompatibilität in frühen Infrastrukturschichten sicherstellen.
- Falsche Kandidaten-Normalisierung (Konsistenz von `erwarteteDiskriminierung`
  bei extremen Beliefs) kann die `ARC-04`-Entscheidung entwerten.
- Ein LLM-ähnlicher Kandidaten-Fake darf nicht als echte Modellqualität verkauft
  werden; klar als deterministischer Fallback kennzeichnen.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** TODO.

**Was ist offen geblieben:** TODO.

**Steering-Loop:** TODO.

**Folge-Slices:** falls externe Kalibrierung nötig ist, auf `slice-022` verweisen.

## 8. Sub-Area-Modus-Begründung

Neue Sub-Areas:

- `hexagon/application/beobachtung-waehlen` — GF (Port-/Use-Case-Logik folgt
  Spec/Architektur; Vorlauf vor Produktivbindung).
- `adapters/outbound/voi-fake` — GF (`LLM-VoI-Fake` als testbar deterministische
  Fallback-Sicht, Spezifikation vor Code).
