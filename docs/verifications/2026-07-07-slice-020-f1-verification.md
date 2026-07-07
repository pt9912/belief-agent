# Verification-Report: slice-020 F-1 — 2026-07-07

**Verifikations-Art:** Fix-Verifikation gegen DoD/Spec und Review-Finding.

**Gegenstand:** Nachlauf zu `docs/reviews/2026-07-07-slice-020-code-review.md`
Finding F-1.

**Regelwerk:** `.harness/baseline/v1.4.0/regelwerk/modul-11-verification.md`

**Eingangs-Kontext:**

- `docs/plan/planning/done/slice-020-beobachtung-waehlen-belief-aware-llm-voi-fake.md`
- `docs/reviews/2026-07-07-slice-020-code-review.md`
- `spec/lastenheft.md` (`LH-FA-OBS-004`, `LH-FA-VOI-002`, `LH-QA-03`)
- `spec/spezifikation.md` (`LH-FA-VOI-002.a`)
- `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/beobachtungwaehlen/BeobachtungWaehlen.kt`
- `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/entscheidungszyklus/Entscheidungszyklus.kt`
- `hexagon/application/src/commonTest/kotlin/dev/beliefagent/application/belief/beobachtungwaehlen/BeobachtungWaehlenTest.kt`

---

## Behauptung Aus Pre-Completion

F-1 ist behoben: Bereits gesammelte Evidenz wird ueber `Beobachtung` statt ueber
den gesamten `VoiKandidat` ausgeschlossen. Dadurch kann dieselbe Beobachtung
nicht erneut gesammelt werden, wenn ein belief-aware Adapter sie spaeter mit
anderer `erwarteteDiskriminierung` oder anderen Kosten anbietet.

## Verifikation

| Vertrag | Beobachtung | Ergebnis |
|---|---|---|
| `LH-FA-OBS-004` keine Scheingewissheit durch Wiederholung | `BeobachtungWaehlen.waehle` filtert nach `it.beobachtung in bereitsGesammelt`; `Entscheidungszyklus` speichert verbrauchte `Beobachtung`-Werte. | erfuellt |
| `LH-FA-VOI-002` Auswahl bleibt VoI-basiert | Nach dem Beobachtungsfilter waehlt weiterhin `VoiSelektor` den besten verbleibenden Kandidaten. | erfuellt |
| `LH-QA-03` deterministische Tests | `schliesst_beobachtung_auch_bei_geaendertem_score_aus` reproduziert den Review-Fall: gleiche Beobachtung, anderer Score, Ergebnis `null` nach Konsumption. | erfuellt |
| Review F-1 | Der Befund war "Filterung als kompletter `VoiKandidat`". Dieser Typ taucht an der Konsumptionskante nicht mehr auf. | geschlossen |

## Sensors

- `make test` — gruen.
- `make gates` — gruen (`doc-check`, `build`, `test`, `coverage-gate`, `arch-check`).

## DoD-/Spec-Risiko

Keine neue DoD-Verletzung sichtbar. Der Fix veraendert nur die
Konsumptionsidentitaet von Kandidat auf Beobachtung; Port-Vertrag,
belief-aware Kandidatengenerierung und VoI-Auswahl bleiben unveraendert.

## Verdikt

**Verifiziert:** ja. F-1 ist aus Verifier-Sicht geschlossen.
