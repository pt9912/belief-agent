# Slice slice-021: LLM-Hypothesen-Port + Fake

**Status:** open → next → in-progress → done (siehe
[Planning-README](../README.md)).

**Welle:** `welle-05-llm-port` ([Roadmap](../in-progress/roadmap.md)).

**Bezug:** `LH-FA-LLM-002`, `LH-FA-LLM-003`, `LH-FA-BEL-005`,
`LH-FA-BEL-006`, `LH-FA-BEL-007`, `LH-QA-03`, `LH-QA-04`; `ADR-0001`,
`ADR-0003`; `ARC-07`, `ARC-08`.

**Autor:** pt9912. **Datum:** 2026-07-06.

---

## 1. Ziel

Bei hoher Resthypothese wird heute nur zwischen vorhandenen Hypothesen operiert;
die eigentliche Hypothesen-Erzeugung/Verfeinerung über LLM bleibt offen. Dieser
Slice definiert einen dedizierten LLM-Hypothesen-Port samt Fake-Adapter,
tracebaren Hypothesen-Kandidaten und deren kontrollierte Übernahme in den
Hypothesenraum, damit `LH-FA-BEL-005` technisch umsetzbar wird, ohne die
Domänenlogik zu verunreinigen.

## 2. Definition of Done

- [ ] `LH-FA-BEL-005` ist im Anwendungsfluss an den neuen Hypothesen-Port angebunden
  (Auslöser bleibt domänenseitig über den bestehenden Schwellwert).
- [ ] `LH-FA-BEL-006` erfüllt: der Port liefert neue bzw. verfeinerte
  Hypothesen-Kandidaten, und der Anwendungsfluss übernimmt gültige Kandidaten
  in einen neuen, normierten `BeliefState`, ohne eine statische Kandidatenliste
  im Kern zu fixieren.
- [ ] `LH-FA-BEL-007` erfüllt: jeder Kandidat trägt eine referenzierbare
  stützende Evidenz/Beobachtung, und übernommene Hypothesen bleiben auf diese
  Evidenz referenzierbar; Tests decken fehlende Traceability als ungültigen
  Kandidatenfall ab.
- [ ] `LH-FA-LLM-002` erfüllt: der Port ist auf die Aufgabe „Hypothesen
  erzeugen/verfeinern“ beschränkt und entkoppelt von Gate-/VoI-/Aktionslogik.
- [ ] `LH-FA-LLM-003` als Guardrail erfüllt: Kandidaten-Konfidenz/Score wird
  explizit als strukturierter Wert protokollierbar gemacht oder bewusst nicht
  akzeptiert; es gibt keine modellimplizite Default-Konfidenz.
- [ ] `LH-QA-03` erfüllt: deterministische Testfälle für Fake-Output, leere und
  inkonsistente Kandidatenfälle (inkl. fehlender Evidenzreferenz).
- [ ] `LH-QA-04` erfüllt: neue Schnittstelle und Tests sind strukturell/tracebar
  in der Architektur hinterlegt.
- [ ] `make gates` grün.
- [ ] Doku-Update, falls `docs/user/integration.md` den Hypothesen-Flow für
  Integratoren ergänzt.
- [ ] Closure-Notiz mit Steering-Loop-Eintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/aktualisieren/ports/HypothesenPort.kt` | neu | Vertrag für hypothesenbezogene LLM-Aufgaben |
| `hexagon/domain/src/commonMain/kotlin/dev/beliefagent/domain/belief/HypothesenKandidat.kt` | neu | Kandidatenvertrag mit Evidenzreferenz für dynamische, tracebare Hypothesen |
| `hexagon/domain/src/commonMain/kotlin/dev/beliefagent/domain/belief/HypothesenraumErweitern.kt` | neu | Gültige Kandidaten kontrolliert in einen normierten Belief State übernehmen |
| `hexagon/domain/src/commonMain/kotlin/dev/beliefagent/domain/belief/Hypothese.kt` / Beleg-Typ | update / neu | Referenz auf stützende Evidenz für übernommene Hypothesen strukturell abbilden |
| `hexagon/domain/src/commonMain/kotlin/dev/beliefagent/domain/belief/Ereignis.kt` | update | Übernahme neuer/verfeinerter Hypothesen protokollierbar machen |
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/aktualisieren/BeliefAktualisieren.kt` | update | Re-Hypothesen-Auslöser führt optional an Port |
| `adapters/outbound/llm-hypothesen-fake/src/...` | neu | Deterministischer Fake für reproduzierbare Revision |
| `adapters/outbound/llm-hypothesen-fake` (Build/Settings) | neu | Adapter-Modul als eigene, port-spezifische Quelle |
| `.a-check.yml` | update | neuen Adapter-Root in die Architekturprüfung aufnehmen (`ARC-08`) |
| `hexagon/domain/src/commonTest/...` | neu | Kandidaten-Invarianten: eindeutige ID, gültige Wahrscheinlichkeit, Evidenzreferenz |
| `hexagon/domain/src/commonTest/...` | neu | Übernahme-Regel: normierter Belief State, Evidenzbeleg bleibt referenzierbar |
| `hexagon/application/src/commonTest/...` | update | Test der Orchestrierung bei `LH-FA-BEL-005` |
| `adapters/outbound/llm-hypothesen-fake/src/commonTest/...` | neu | Fail-safe-Fälle (unbekannte Hypothesen/NaN/negativ) |

## 4. Trigger

Abschluss von `slice-019` **und** `slice-020`.

Kein Alternativtrigger über zusätzliche Spezifikations-Kriterien: die
maßgeblichen Anker (`LH-FA-BEL-005`/`006`/`007`, `LH-FA-LLM-002`/`003`)
existieren bereits. Wenn sich vor Start zeigt, dass ein weiterer
Spezifikationsvertrag nötig ist, wird dieser Slice nicht gestartet, sondern
vorher in `open/` re-geschnitten.

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz + Slice in `done/`.

## 6. Risiken und offene Punkte

- Gefahr: Hypothesen-Erweiterung drängt in Domänenlogik — klare Grenzlinie auf
  `ARC-08`/`ADR-0003` nötig.
- Gefahr: Unbounded Hypothesen-Erweiterung kann Prior-Verteilung bremsen; Caps für
  Neuankunft/Änderung definieren.
- Gefahr: Kandidaten-Konfidenz wird als implizite Modellwahrheit missverstanden;
  nur explizite, strukturierte Scores übernehmen oder Kandidat verwerfen
  (`LH-FA-LLM-003`).
- Gefahr: Fake kann echte Pipeline-Anforderungen maskieren; Fake-Daten müssen
  als solche gekennzeichnet bleiben.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** TODO.

**Was ist offen geblieben:** TODO.

**Steering-Loop:** TODO.

**Folge-Slices:** bei Kalibrierungsbedarf an `slice-022` anschließen.

## 8. Sub-Area-Modus-Begründung

### Sub-Area: `hexagon/domain/belief`

- **Modus:** Hybrid
- **Konventionen-Dichte:** Mittel. `BeliefState`, `ReHypothesenAusloeser` und
  Ereignisse sind durch Lastenheft/Spec/ADRs bereits stark geführt
  (`LH-FA-BEL-002`..`005`, `ADR-0008`); für Kandidaten zur
  Hypothesen-Erweiterung existiert noch kein eigener Domänenvertrag.
- **Phase-Reife:** Phase 3. Die Spec fordert dynamischen Hypothesenraum und
  Evidenz-Traceability (`LH-FA-BEL-006`/`007`), der bestehende Code trägt aber
  noch keine Kandidatenform und keine Übernahme-Regel für neue/verfeinerte
  Hypothesen.
- **Evidenz-/Diskrepanz-Risiko:** Mittel. Inventur kann zeigen, dass bestehende
  Ereignisse (`HypotheseHinzugefuegt`) und `Hypothese`-Werte nicht genug
  Kontext tragen, um Kandidaten-Traceability und spätere Rekonstruktion sauber
  zu verbinden.
- **Reconciliation-Aufwand:** Teil dieses Slice: minimaler Kandidatenvertrag mit
  Evidenzreferenz, explizitem Kandidaten-Score, Invarianten und
  Übernahme-Regel. Graduation-Trigger: nach grünem Gate und Closure-Notiz wird
  die Kandidatenform als GF-Basis für spätere echte LLM-Adapter genutzt; falls
  Audit-/Rekonstruktionsdaten darüber hinaus erweitert werden müssen, entsteht
  ein Folge-Slice.

### Sub-Area: `hexagon/application/belief-aktualisieren`

- **Modus:** Hybrid
- **Konventionen-Dichte:** Hoch für Layering und lokale Ports
  (`ARC-07`, `ADR-0003`), mittel für den konkreten Re-Hypothesen-Pfad: der
  bestehende Use Case nutzt bereits `LlmPort` für Likelihoods, aber noch keinen
  getrennten Port für Hypothesen-Erzeugung/Verfeinerung.
- **Phase-Reife:** Phase 4 für die bestehende Update-Pipeline, Phase 3 für die
  neue Port-Verflechtung. Die Architektur sagt „Ports so lokal wie möglich",
  der neue Auslöser muss aber ohne Adapter-Import und ohne Gate-/VoI-Kopplung in
  den Use Case.
- **Evidenz-/Diskrepanz-Risiko:** Mittel. Die Änderung kann unabsichtlich den
  bestehenden Likelihood-Port mit Hypothesen-Erzeugung vermischen oder
  Re-Hypothesen zu früh/spät im Update-Ablauf auslösen.
- **Reconciliation-Aufwand:** Teil dieses Slice: getrennte Port-Signatur,
  Orchestrierungstests für Schwellwert-Auslösung, Nicht-Auslösung, Kandidaten-
  Übernahme und Negativtests gegen Gate-/VoI-/Aktionslogik im Port-Pfad.
  Graduation-Trigger: wenn `make gates` grün bleibt und die Closure-Notiz keine
  Drift benennt, gilt der Pfad als GF-Konvention für weitere
  Hypothesen-Adapter.

### Sub-Area: `adapters/outbound/llm-hypothesen-fake`

- **Modus:** GF
- **Konventionen-Dichte:** Hoch. Vorbilder sind die bestehenden
  port-spezifischen Fake-Adapter (`llm-fake`, `voi-fake`) hinter
  application-lokalen Ports; Layering wird über `ADR-0003`/`ARC-08` und
  `arch-check` abgesichert.
- **Phase-Reife:** Phase 3. Der Adapter wird neu aus dem Port-Vertrag heraus
  gebaut; Spezifikation und Architektur führen, Code folgt.
- **Evidenz-/Diskrepanz-Risiko:** Niedrig. Keine Alt-Implementierung; Risiko
  liegt vor allem darin, Fake-Qualität mit Modellqualität zu verwechseln.
- **Reconciliation-Aufwand:** Keiner im Slice beyond Implementierung und Tests.
  Graduation-Trigger: bleibt GF, solange der Fake deterministisch bleibt und
  echte Provider-Anforderungen in separaten Adapter-Slices landen.
