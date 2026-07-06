# Slice slice-020: beobachtung-waehlen belief-aware + LLM-VoI-Fake

**Status:** open → next → in-progress → done (siehe
[Planning-README](../README.md)).

**Welle:** `welle-05-llm-port` ([Roadmap](../in-progress/roadmap.md)).

**Bezug:** `LH-FA-VOI-002`, `LH-QA-03`; Guardrails: `LH-FA-LLM-002`,
`LH-FA-LLM-003`; `ADR-0001`, `ADR-0003`; `ARC-04`, `ARC-07`.

**Autor:** pt9912. **Datum:** 2026-07-06.

---

## 1. Ziel

`slice-016` liefert fixe Kandidaten; dieser Slice schließt den offenen Punkt
`welle-04` **F4b** (`belief-abhängige` Kandidaten) als Follow-up zu
`welle-05-llm-port` ab. `BeobachtungsAuswahlPort` liefert
belief-abhängige `VoiKandidat`-Listen über einen deterministischen
Beobachtungs-/VoI-Adapter, damit `beobachtung-waehlen` nicht länger mit einer
statischen Kandidatenliste arbeitet. Eine spätere LLM-Bindung darf die
Modellgrenzen aus `LH-FA-LLM-002` nicht erweitern: das Modell erzeugt nicht
selbst VoI-Entscheidungslogik, sondern liefert höchstens klar strukturierte
Eingangswerte hinter einem Port.

## 2. Definition of Done

- [ ] `LH-FA-VOI-002` erfüllt: die Beobachtungsauswahl nutzt belief-abhängige
  Kandidatenlisten und liefert die bestgeeignete Beobachtung deterministisch bei
  gegebenen Fake-Daten.
- [ ] `LH-FA-LLM-002` nicht ausgeweitet: der Slice führt keine neue
  Modell-Aufgabe ein; VoI-Kandidaten entstehen im Beobachtungs-/VoI-Adapter
  deterministisch und providerfrei, Domänen- und Application-Regeln enthalten
  keine Provider-spezifische Logik.
- [ ] `LH-FA-LLM-003` bleibt gewahrt, falls LLM-gelieferte Eingangswerte
  beteiligt sind: alle modellbeeinflussten Zahlen werden explizit strukturiert,
  protokollierbar und überschreibbar übergeben; der Slice führt keine stillen
  Defaults für `erwarteteDiskriminierung` ein.
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
| `adapters/outbound/voi-fake/src/commonMain/kotlin/dev/beliefagent/adapter/voi/FakeVoiKandidatenQuelle.kt` | update | Deterministischer, belief-aware Beobachtungs-/VoI-Fallback |
| `hexagon/application/src/commonTest/...` | update | neue Use-Case-/Port-Tests inkl. F4b-Fall |
| `hexagon/application/src/commonTest/...` | neu | Regression gegen `F4b` (belief-abhängige Liste → nicht-statische Wiederholung) |
| `example/langchain` / `example/koog` | update | Beispiele auf neuen `beobachtungs-waehlen`-Contract verdrahten |
| `docs/user/integration.md` | update | Vertrag und Konfigurationswirkung dokumentieren |

## 4. Trigger

Roadmap-Follow-up aus `welle-05-llm-port` offen + `slice-019` abgeschlossen
(LLM-Port-Adapter) + `slice-016` liefert funktionierenden
`BeobachtungWaehlen`-Use-Case. Falls keine Welle aktiv ist, wird dieser Slice
als gezielter Follow-up-Slice gestartet oder `welle-05-llm-port` explizit
wieder geöffnet.

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz + Slice in `done/`.

## 6. Risiken und offene Punkte

- Signaturänderung am `BeobachtungsAuswahlPort` kann den Test-/Example-Fluss
  bremsen; Rückwärtskompatibilität in frühen Infrastrukturschichten sicherstellen.
- Falsche Kandidaten-Normalisierung (Konsistenz von `erwarteteDiskriminierung`
  bei extremen Beliefs) kann die `ARC-04`-Entscheidung entwerten.
- Ein deterministischer Kandidaten-Fake darf nicht als echte Modellqualität
  verkauft werden; klar als Beobachtungs-/VoI-Fallback kennzeichnen und nicht
  als Erweiterung der `LH-FA-LLM-002`-Modellaufgaben behandeln.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** TODO.

**Was ist offen geblieben:** TODO.

**Steering-Loop:** TODO.

**Folge-Slices:** falls externe Kalibrierung nötig ist, auf `slice-022` verweisen.

## 8. Sub-Area-Modus-Begründung

Berührte Sub-Areas:

- `hexagon/application/beobachtung-waehlen` — Hybrid/BF (`slice-016` hat
  Port-/Use-Case-Logik bereits geliefert; dieser Slice ändert den bestehenden
  Contract belief-kontextfähig und braucht Regression gegen den alten
  statischen Pfad).
- `adapters/outbound/voi-fake` — Hybrid/BF (`slice-016` hat das Adapter-Modul
  bereits geliefert; dieser Slice erweitert es zu einem deterministischen
  belief-aware Beobachtungs-/VoI-Fallback, ohne Modellqualitäts-Claims).
