# Review-Report: slice-020 — 2026-07-07

**Review-Art:** Code

**Gegenstand:** Diff `HEAD~2..HEAD` (`2499bc4`, `d8657d5`)

**Skill:** `.harness/baseline/v1.4.0/regelwerk/modul-10-review-harness.md`
(`.harness/skills/reviewer.md` existiert in diesem Repo noch nicht) ·
**Modell:** GPT-5 Codex · **Datum:** 2026-07-07

**Eingangs-Kontext:**

- `docs/plan/planning/done/slice-020-beobachtung-waehlen-belief-aware-llm-voi-fake.md`
- `spec/lastenheft.md` (`LH-FA-VOI-002`, `LH-QA-03`, Guardrails `LH-FA-LLM-002`/`003`)
- `spec/spezifikation.md` (`LH-FA-VOI-002.a`)
- `spec/architecture.md` (`ARC-04`, `ARC-07`, `ADR-0001`, `ADR-0003`)
- `AGENTS.md` (Hard Rules)
- `.harness/baseline/v1.4.0/regelwerk/modul-10-review-harness.md`

---

## Findings

### F-1 — Wiederholte Beobachtung bei geaendertem Kandidaten-Score

- `kategorie`: MEDIUM
- `quelle`: `LH-FA-OBS-004`, `LH-FA-VOI-002`, Maintainability
- `pfad`: `hexagon/application/src/commonMain/kotlin/dev/beliefagent/application/belief/beobachtungwaehlen/BeobachtungWaehlen.kt:32`
- `befund`: Bereits gewaehlte Eintraege werden als kompletter `VoiKandidat` gefiltert. Bei belief-abhaengiger Kandidaten-Generierung kann dieselbe `Beobachtung` mit geaenderter `erwarteteDiskriminierung` oder geaenderten Kosten erneut erscheinen und dadurch erneut gesammelt werden, obwohl der Entscheidungszyklus Wiederholung derselben Beobachtung ausschliessen soll.
- `verifizierbar`: ja — ein Unit-Test mit gleicher `Beobachtung`, aber zwei unterschiedlichen `VoiKandidat`-Scores wuerde den Fehler reproduzieren.

## Negativbefunde

- geprüft, ohne Befund: `hexagon/application/.../ports/BeobachtungsAuswahlPort.kt` — Port bleibt domain-only und importiert keine Adapter.
- geprüft, ohne Befund: `hexagon/application/.../entscheidungszyklus/Entscheidungszyklus.kt` — Belief-Kontext wird an `BeobachtungWaehlen` weitergereicht; keine neue Gate-Umgehung sichtbar.
- geprüft, ohne Befund: `adapters/outbound/voi-fake/**` — Adapter implementiert den Port ohne Provider-/LLM-Abhaengigkeit.
- geprüft, ohne Befund: `example/langchain/**` und `example/koog/**` — Beispiele nutzen die neue Signatur ohne Core-Abhaengigkeit auf Adapter.
- geprüft, ohne Befund: `docs/user/integration.md` und Planning-Doku — Integratorvertrag und Slice-Closure sind dokumentiert.

## Summary

| Kategorie | Anzahl |
|---|---:|
| HIGH | 0 |
| MEDIUM | 1 |
| LOW | 0 |
| INFO | 0 |

## Verdikt

**Merge-blockierend:** ja — MEDIUM sollte vor Merge geklaert werden, weil der
Befund die zugesicherte Konsumption gegen Scheingewissheit betrifft.

**Übergabe:** Finding geht an die Implementation. Der Report ersetzt keine
Verifikation; `make gates` war zum Review-Zeitpunkt gruen.
