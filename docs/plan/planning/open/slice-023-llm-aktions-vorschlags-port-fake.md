# Slice slice-023: LLM-Aktions-Vorschlags-Port + Fake

**Status:** open → next → in-progress → done (siehe
[Planning-README](../README.md)).

**Welle:** `welle-05-llm-port` ([Roadmap](../in-progress/roadmap.md)).

**Bezug:** `LH-FA-LLM-002`, `LH-FA-LLM-003`, `LH-QA-03`, `LH-QA-04`; `ADR-0001`,
`ADR-0003`, `ADR-0006`; `ARC-07`, `ARC-08`.

**Autor:** pt9912. **Datum:** 2026-07-06.

---

## 1. Ziel

`LLM-Port-Adapter` liefert heute Likelihoods, aber keinen strukturierten Pfad für
`Aktion`-Vorschläge. Dieser Slice führt einen dedizierten Port und ein kleines
Use-Case-Rand für `Aktion`-Vorschläge ein (`WIRKUNGSKLASSE`, `p_success`,
`stuetzendeEvidenz`) und hält diese Aufgabe klar von Update/Beobachtung getrennt.
Die erste Realisierung bleibt deterministisch mit `llm-action-fake` als
nicht-produktivem Adapter.

## 2. Definition of Done

- [ ] `LH-FA-LLM-002` erfüllt: der neue Port ist auf `Aktion`-Vorschläge begrenzt
  und führt keine Entscheidung, kein Update und keine Aktionsausführung aus.
- [ ] `LH-FA-LLM-003` erfüllt: `p_success`/Konfidenz aus dem Modell ist als
  explizite Zahl modelliert und vor der Weitergabe an Gates validiert.
- [ ] `LH-QA-03` erfüllt: deterministische Tests für leere Rückgaben, kaputte
  Felder, Konsistenz zu bekannten Hypothesen + Fake-Determinismus.
- [ ] `LH-QA-04` erfüllt: neuer `ports/`-Vertrag + `aktions-vorschlags`-Use-Case-Rand ist
  strukturell nachvollziehbar verdrahtbar (ohne Kernlogik-Leak).
- [ ] `make gates` grün.
- [ ] Doku-Update in `docs/user/integration.md` für vorgeschlagene Aktionen als
  eigenständige LLM-Aufgabe, ohne Ausführungsverantwortung.
- [ ] Closure-Notiz mit Steering-Loop-Eintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/aktionsvorschlag/ports/AktionsVorschlagsPort.kt` | neu | Port-Vertrag für strukturierte Aktionsvorschläge (`ARC-07`, `LH-FA-LLM-002`). |
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/aktionsvorschlag/AktionsVorschlagen.kt` | neu | Kleiner Application-Use-Case zur Verifikation/Umsetzung der Port-Ausgabe für spätere Welleschritte (`ARC-09`). |
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/aktionsvorschlag/dto/AktionsVorschlag.kt` | neu | Domänennahe Datenstruktur für `Aktion`-Vorschlag + Konfidenz. |
| `adapters/outbound/llm-action-fake/**` | neu | Deterministischer Fake für Aktionen-vorschlagen (`LH-FA-LLM-002`/`LH-QA-03`). |
| `hexagon/application/src/commonTest/...` | neu | Tests zu Validierung, deterministische Vorschläge, leere/inkonsistente Antworten. |
| `adapters/outbound/llm-action-fake/src/commonTest/...` | neu | Fake-Contract und Parser/Normalisierungstest. |
| `docs/user/integration.md` | update | Vertrag der neuen Aktions-Vorschlags-Pipeline dokumentieren. |

## 4. Trigger

`slice-020` und `slice-022` geliefert; `slice-019` vorhanden, damit LLM-Aufgaben
über den vorhandenen Rahmen (`LlmPort`) verdrahtet werden können.

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz + Slice in `done/`.

## 6. Risiken und offene Punkte

- Eine klare Schnittstelle für `p_success` kann ungewollt zum `Gate`/`Entscheidungs`
  driften; der Port muss nur Vorschlag, nicht Freigabe erzeugen.
- Aktionsvorschlag und Domäne haben unterschiedliche Semantik; falsche Namens-/
  Evidenz-Kopplung erschwert spätere Governance.
- Fehlende Verfügbarkeitssignale (z. B. keine Vorschläge) dürfen nicht als
  implizite Freigabe interpretiert werden; fail-closed Verhalten ist Pflicht.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** TODO.

**Was ist offen geblieben:** TODO.

**Steering-Loop:** TODO.

**Folge-Slices:** Produktiver `ARC-09`-Composition-Root (slice-024) für das
  Vorschlags- und Ausführungs-Flussmuster.

## 8. Sub-Area-Modus-Begründung

Neue Sub-Areas:

- `hexagon/application` (`aktionsvorschlag`) — GF (neue Schnittstelle, lokale
  Use-Case-Schnitt; Spec vor Code).
- `adapters/outbound:llm-action-fake` — GF (deterministische Fakes für LLM-
  Aufgabenscoping).
