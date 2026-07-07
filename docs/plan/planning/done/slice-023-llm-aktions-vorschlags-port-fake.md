# Slice slice-023: LLM-Aktions-Vorschlags-Port + Fake

**Status:** open → next → in-progress → done (siehe
[Planning-README](../README.md)).

**Welle:** `welle-05-llm-port` ([Roadmap](../in-progress/roadmap.md)).

**Bezug:** `LH-FA-LLM-002`, `LH-FA-LLM-003`, `LH-FA-ACT-001`,
`LH-FA-ACT-002`, `LH-FA-ACT-003`, `LH-FA-ACT-004`, `LH-QA-03`,
`LH-QA-04`; `ADR-0001`, `ADR-0003`, `ADR-0006`; `ARC-03`, `ARC-07`,
`ARC-08`.

**Autor:** pt9912. **Datum:** 2026-07-06.

---

## 1. Ziel

`LLM-Port-Adapter` liefert heute Likelihoods, aber keinen strukturierten Pfad für
`Aktion`-Vorschläge. Dieser Slice führt einen dedizierten Port und einen kleinen
Use-Case-Rand für `Aktion`-Vorschläge ein (`WIRKUNGSKLASSE`, `p_success`,
`stuetzendeEvidenz`) und hält diese Aufgabe klar von Update/Beobachtung,
Gate-Entscheidung und Ausführung getrennt. Die erste Realisierung bleibt
deterministisch mit `llm-action-fake` als nicht-produktivem Adapter.

## 2. Definition of Done

- [x] `LH-FA-LLM-002` erfüllt: der neue Port ist auf `Aktion`-Vorschläge begrenzt
  und führt keine Entscheidung, kein Update und keine Aktionsausführung aus.
- [x] `LH-FA-LLM-003` erfüllt: `p_success`/Konfidenz aus dem Modell wird über den
  in `slice-022` gelieferten Konfidenz-/Audit-Contract externalisiert,
  protokollierbar und überschreibbar gemacht; ohne externalisierte Konfidenz
  entsteht kein gate-fähiger Aktionsvorschlag.
- [x] `LH-FA-ACT-001`/`002` erfüllt: jeder Vorschlag wird genau einer
  `Wirkungsklasse` nach Seiteneffekt-Reichweite zugeordnet; unbekannte oder
  mehrdeutige Klassen werden verworfen.
- [x] `LH-FA-ACT-003` erfüllt: jeder gate-fähige Vorschlag trägt eine eigene
  Erfolgswahrscheinlichkeit `P(Aktion erreicht Ziel | aktueller Belief)`,
  getrennt von Hypothesenwahrscheinlichkeiten.
- [x] `LH-FA-ACT-004` erfüllt: jeder gate-fähige Vorschlag referenziert
  stützende Evidenz; Vorschläge ohne Evidenz werden verworfen.
- [x] `LH-QA-03` erfüllt: deterministische Tests für leere Rückgaben, kaputte
  Felder, Konsistenz zu bekannten Hypothesen, externe Konfidenzbindung,
  Wirkungsklassen-Zuordnung, Evidenzpflicht + Fake-Determinismus.
- [x] `LH-QA-04` erfüllt: neuer `ports/`-Vertrag + `aktions-vorschlags`-Use-Case-Rand ist
  strukturell nachvollziehbar verdrahtbar (ohne Kernlogik-Leak); der
  gemeinsam genutzte Konfidenz-Contract liegt nicht unter einem fremden lokalen
  Use-Case-Port.
- [x] `spec/architecture.md` ist auf den neuen Aktionsvorschlags-Port und dessen
  Abgrenzung vom bestehenden Likelihood-`LlmPort` aktualisiert.
- [x] `make gates` grün.
- [x] Doku-Update in `docs/user/integration.md` für vorgeschlagene Aktionen als
  eigenständige LLM-Aufgabe, ohne Ausführungsverantwortung.
- [x] Closure-Notiz mit Steering-Loop-Eintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/aktionsvorschlag/ports/AktionsVorschlagsPort.kt` | neu | Port-Vertrag für strukturierte Aktionsvorschläge (`ARC-07`, `LH-FA-LLM-002`). |
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/aktionsvorschlag/AktionsVorschlagen.kt` | neu | Kleiner Application-Use-Case zur Validierung/Umsetzung der Port-Ausgabe in eine Domain-`Aktion`, aber ohne Gate-Freigabe oder Ausführung (`ARC-03` bleibt getrennt). |
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/aktionsvorschlag/dto/AktionsVorschlag.kt` | neu | Domänennahe Rohstruktur für `Aktion`-Vorschlag + externe Konfidenzreferenz aus dem gemeinsamen Konfidenz-Contract; kein Ersatz für Domain-`Aktion`. |
| `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/ports/KonfidenzPort.kt` und `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/ports/ExternalisierteKonfidenz.kt` (aus `slice-022`) | use | `p_success` nur nach Externalisierung/Protokollierbarkeit als `Erfolgswahrscheinlichkeit` übernehmen (`LH-FA-LLM-003`); gemeinsamen Business-Area-Port und dessen Contract-Typen verwenden, keine fremden use-case-lokalen DTOs importieren und keinen zweiten Konfidenz-Port im `aktionsvorschlag`-Slice anlegen. |
| `adapters/outbound/llm-action-fake/**` | neu | Deterministischer Fake für Aktionen-vorschlagen (`LH-FA-LLM-002`/`LH-QA-03`). |
| `adapters/outbound/llm-action-fake/build.gradle.kts` | neu | Adapter-Modul mit eigenem Kover-Block gemäß `ADR-0006`. |
| `settings.gradle.kts` | update | `adapters:outbound:llm-action-fake` als explizites Modul aufnehmen. |
| `Dockerfile` | update | Dependency-, Coverage- und `coverage-gate`-Tasks für das neue Adapter-Modul explizit ergänzen (`ADR-0006`). |
| `.a-check.yml` | update | neuen Adapter-Root in die Architekturprüfung aufnehmen (`ARC-08`). |
| `hexagon/application/src/commonTest/...` | neu | Tests zu Validierung, deterministische Vorschläge, leere/inkonsistente Antworten. |
| `adapters/outbound/llm-action-fake/src/commonTest/...` | neu | Fake-Contract und Parser/Normalisierungstest. |
| `spec/architecture.md` | update | `ARC-07`/`ARC-08` nachziehen: Aktionsvorschlag als eigener LLM-Aufgaben-Port, nicht als Erweiterung des Likelihood-`LlmPort`; Port-Konsumenten-Liste aktualisieren. |
| `docs/user/integration.md` | update | Vertrag der neuen Aktions-Vorschlags-Pipeline dokumentieren. |

## 4. Trigger

`slice-019` liegt in `done/`, damit LLM-Framework-Adapter grundsätzlich hinter
Ports verdrahtet werden können. Dieser Slice startet erst, wenn `slice-022`
in `done/` liegt und den Business-Area-geteilten Konfidenz-/Audit-Contract
bereitstellt. `slice-020` ist keine harte Voraussetzung; falls die Umsetzung
belief-abhaengige VoI-Kandidaten wiederverwendet, muss `slice-020` vorher in
`done/` liegen oder diese Kopplung wird aus dem Slice entfernt.

Ohne `slice-022` wird `p_success` nicht gate-fähig externalisiert.

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz + Slice in `done/`.

## 6. Risiken und offene Punkte

- Eine klare Schnittstelle für `p_success` kann ungewollt zum `Gate`/`Entscheidungs`
  driften; der Port muss nur Vorschlag, nicht Freigabe erzeugen.
- Aktionsvorschlag und Domäne haben unterschiedliche Semantik; falsche Namens-/
  Evidenz-Kopplung erschwert spätere Governance.
- Der Konfidenz-Contract darf nicht aus einem fremden use-case-lokalen
  `ports/`-Verzeichnis importiert werden; bei Bedarf ist er in
  `application/belief/ports` als Business-Area-geteilter Port zu führen
  (`ADR-0003`). Das gilt auch für referenzierte Contract-Typen wie die
  externalisierte Konfidenzreferenz.
- Roh-DTOs könnten Domain-`Aktion`-Invarianten umgehen; der Use Case muss über den
  bestehenden Domain-Typ und dessen Evidenz-/Wirkungsklassen-Regeln normalisieren.
- Neues Adapter-Modul kann aus `arch-check`/`coverage-gate` fallen, wenn Root und
  Dockerfile-Tasks nicht explizit ergänzt werden (`ADR-0006`).
- Fehlende Verfügbarkeitssignale (z. B. keine Vorschläge) dürfen nicht als
  implizite Freigabe interpretiert werden; fail-closed Verhalten ist Pflicht.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** Der Schnitt ueber einen lokalen
`AktionsVorschlagsPort` und den Use Case `AktionsVorschlagen` hielt die
LLM-Aufgabe klar vor dem Gate: Rohvorschlaege werden gegen bekannte
Hypothesen, Wirkungsklassen und Evidenz normalisiert, `p_success` wird ueber
den bestehenden Konfidenz-Contract externalisiert, und erst daraus entsteht
eine `KonfidenzgebundeneAktion`. Der Fake-Adapter liess sich analog zu den
Hypothesen-/Konfidenz-Fakes klein und gate-frei halten.

**Was ist offen geblieben:** Produktive Provider-Prompts, echte
Aktionsausfuehrung und die CLI-/Composition-Root-Verdrahtung bleiben bewusst
ausserhalb dieses Slice. Die Ausfuehrungsgrenze bleibt bei
`Aktionsfreigabe.Freigegeben` und wird in `slice-024` verdrahtet.

**Steering-Loop:** Modellabgeleitete Aktionswerte brauchen immer eine stabile,
eindeutige Konfidenzreferenz pro Vorschlag. Doppelte Referenzen werden jetzt
fail-safe verworfen; kuenftige Provider-Adapter muessen dieselbe Eindeutigkeit
vor Prompt-/Parser-Erweiterungen testen.

**Folge-Slices:** Produktiver `ARC-09`-Composition-Root (slice-024) für das
  Vorschlags- und Ausführungs-Flussmuster.

## 8. Sub-Area-Modus-Begründung

### Sub-Area: `hexagon/domain/belief/Aktion`

- **Modus:** Hybrid
- **Konventionen-Dichte:** Hoch für die bestehenden Domain-Invarianten:
  `Aktion`, `Wirkungsklasse`, `Erfolgswahrscheinlichkeit` und
  `stuetzendeEvidenz` sind bereits durch `LH-FA-ACT-001`..`004` und Tests
  geführt. Neu ist die Übersetzung von LLM-Rohvorschlägen in diese Typen.
- **Phase-Reife:** Phase 4 für die Domain-Typen, Phase 3 für die neue
  Vorschlags-Normalisierung. Die Spec führt; der neue Use Case muss zeigen, dass
  er keine parallelen Action-Invarianten neben der Domäne etabliert.
- **Evidenz-/Diskrepanz-Risiko:** Mittel. Ein DTO kann Wirkungsklasse,
  `p_success` oder Evidenz anders interpretieren als `Aktion`; das würde
  `LH-FA-ACT-002`/`003`/`004` verwässern.
- **Reconciliation-Aufwand:** Teil dieses Slice: Rohvorschläge werden nur über
  Domain-`Aktion` gate-fähig; Negativtests gegen fehlende Evidenz, ungültige
  Wirkungsklasse und nicht externalisierte Konfidenz. Graduation-Trigger:
  Closure ohne Drift zwischen DTO und Domain-Typ.

### Sub-Area: `hexagon/application/belief/aktionsvorschlag`

- **Modus:** GF
- **Konventionen-Dichte:** Hoch. Lokale Ports und Use-Case-Ränder folgen
  `ARC-07`/`ADR-0003`; `slice-022` liefert den Business-Area-geteilten
  Konfidenz-Contract, der hier nur konsumiert wird.
- **Phase-Reife:** Phase 3. Der Use Case wird neu aus bestehenden
  Spec-/Architekturankern heraus gebaut.
- **Evidenz-/Diskrepanz-Risiko:** Niedrig bis mittel. Niedrig für die neue
  Port-Form, mittel an der Safety-Grenze: der Use Case darf keine
  `Aktionsfreigabe` erzeugen und nicht `AktionGaten` ersetzen.
- **Reconciliation-Aufwand:** Teil dieses Slice: Tests sichern die Grenze zu
  Gate/Freigabe/Ausführung. Graduation-Trigger: `make gates` grün und
  Closure-Notiz ohne neue Safety-Drift.

### Sub-Area: `hexagon/application/gaten`

- **Modus:** BF/Hybrid
- **Konventionen-Dichte:** Hoch. `AktionGaten` und `Aktionsfreigabe` tragen die
  nicht-umgehbare Gate-Grenze (`ARC-03`, `LH-FA-POL-006`); diese Grenze darf der
  neue Vorschlags-Use-Case nur konsumieren, nicht verschieben.
- **Phase-Reife:** Phase 4. Bestehender Sicherheitsfluss ist implementiert und
  gate-getestet; der neue Slice berührt ihn indirekt über gate-fähige Aktionen.
- **Evidenz-/Diskrepanz-Risiko:** Mittel. Ein Vorschlags-Use-Case könnte
  semantisch als Entscheidung missverstanden werden oder eine Freigabe vorwegnehmen.
- **Reconciliation-Aufwand:** Keine Codeänderung an `aktion-gaten` geplant;
  Negativtests im neuen Use Case sichern, dass keine `Aktionsfreigabe` entsteht.
  Falls dafür `aktion-gaten` geändert werden müsste, wird der Slice re-geschnitten.

### Sub-Area: `adapters/outbound/llm-action-fake`

- **Modus:** GF
- **Konventionen-Dichte:** Hoch. Vorbilder sind die bestehenden
  port-spezifischen Fake-Adapter; `ADR-0006` verlangt explizite
  Coverage-Gate-Einbindung je neuem Adapter.
- **Phase-Reife:** Phase 3. Neuer Adapter folgt dem Port-Vertrag und wird direkt
  mit Build-/Arch-/Coverage-Gates verdrahtet.
- **Evidenz-/Diskrepanz-Risiko:** Niedrig. Keine Alt-Implementierung; Risiko ist
  vor allem, Fake-Parsing als echte Modellqualität zu interpretieren.
- **Reconciliation-Aufwand:** Teil dieses Slice: `settings.gradle.kts`,
  Adapter-Builddatei, `Dockerfile` und `.a-check.yml` ergänzen. Graduation-Trigger:
  `make gates` grün mit dem neuen Modul im Scope.
