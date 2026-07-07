# Slice slice-021: Hypothesen-Kandidaten und Uebernahme-Regel

**Status:** open → next → in-progress → done (siehe
[Planning-README](../README.md)).

**Welle:** `welle-05-llm-port` ([Roadmap](../in-progress/roadmap.md)).

**Bezug:** `LH-FA-LLM-003`, `LH-FA-BEL-006`, `LH-FA-BEL-007`, `LH-QA-03`;
`ADR-0001`; `ARC-07`.

**Autor:** pt9912. **Datum:** 2026-07-07.

---

## 1. Ziel

Dieser Slice liefert den reinen Domänenvertrag für dynamische
Hypothesen-Kandidaten und deren kontrollierte Übernahme in einen normierten
`BeliefState`; Port, Use-Case-Anbindung und Adapter bleiben Folge-Slices.

## 2. Definition of Done

- [ ] `LH-FA-BEL-006`/`LH-FA-BEL-007` sind domänenseitig vorbereitet:
  Kandidaten tragen Hypothesen-ID, expliziten Score und eine referenzierbare
  stützende Evidenz/Beobachtung.
- [ ] `LH-FA-LLM-003` ist als Invariante abgebildet: fehlender oder ungültiger
  Score und fehlende Evidenz machen einen Kandidaten ungültig; deterministische
  Tests decken diese Fälle ab.
- [ ] Eine reine Domänenregel übernimmt gültige neue/verfeinerte Kandidaten in
  einen normierten `BeliefState`, ohne Application-Port, Adapter oder
  statische Kandidatenliste im Kern zu koppeln.
- [ ] `make gates` grün; Closure-Notiz mit Steering-Loop-Eintrag.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/domain/src/commonMain/kotlin/dev/beliefagent/domain/belief/HypothesenKandidat.kt` | neu | Kandidatenvertrag mit explizitem Score und Evidenzreferenz |
| `hexagon/domain/src/commonMain/kotlin/dev/beliefagent/domain/belief/HypothesenraumErweitern.kt` | neu | Gültige Kandidaten kontrolliert in einen normierten Belief State übernehmen |
| `hexagon/domain/src/commonMain/kotlin/dev/beliefagent/domain/belief/Hypothese.kt` / Beleg-Typ | update / neu | Übernommene Hypothesen bleiben auf stützende Evidenz referenzierbar |
| `hexagon/domain/src/commonMain/kotlin/dev/beliefagent/domain/belief/Ereignis.kt` | update, falls nötig | Ereignisse nur erweitern, wenn Traceability sonst nicht rekonstruierbar bleibt |
| `hexagon/domain/src/commonTest/...` | neu / update | Kandidaten-Invarianten, Normalisierung, neue/verfeinerte Hypothesen und Negativfälle |

## 4. Trigger

`slice-019` und `slice-020` sind abgeschlossen; dieser Slice startet erst, wenn
er nach dem Planning-Split als nächster kleiner Lieferwert priorisiert ist.

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz + Slice in `done/`.

## 6. Risiken und offene Punkte

- Gefahr: Kandidaten-Konfidenz wird als implizite Modellwahrheit
  missverstanden; nur explizite, strukturierte Scores übernehmen oder Kandidat
  verwerfen (`LH-FA-LLM-003`).
- Gefahr: Hypothesen-Erweiterung erzwingt zu früh Application-/Adapter-Wissen in
  der Domäne; dieser Slice bleibt bewusst port- und adapterfrei.
- Offen: Ob `Ereignis.kt` für Rekonstruktion erweitert werden muss, entscheidet
  die Domäneninventur im Slice. Falls die Änderung mehr als eine kleine
  Traceability-Ergänzung ist, entsteht ein Follow-up.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** TODO.

**Was ist offen geblieben:** TODO.

**Steering-Loop:** TODO.

**Folge-Slices:** `slice-025` (Application-Port + Re-Hypothesen-Flow) und
`slice-026` (`llm-hypothesen-fake` Adapter).

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
  Ereignisse (`HypotheseHinzugefuegt`) und `Hypothese`-Werte nicht genug Kontext
  tragen, um Kandidaten-Traceability und spätere Rekonstruktion sauber zu
  verbinden.
- **Reconciliation-Aufwand:** Teil dieses Slice: minimaler Kandidatenvertrag mit
  Evidenzreferenz, explizitem Kandidaten-Score, Invarianten und Übernahme-Regel.
  Graduation-Trigger: nach grünem Gate und Closure-Notiz wird die Kandidatenform
  als GF-Basis für spätere Application- und Adapter-Slices genutzt.
