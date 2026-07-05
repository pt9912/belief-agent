# Welle welle-02-evidenz-audit: Evidenz-Aufnahme + Audit

**Status:** done

**Zielmeilenstein:** kein Meilenstein-Bezug (M2 leitet sich später aus
welle-02..04 ab).

**Verantwortlich:** pt9912. **Datum:** 2026-07-04.

---

## 1. Welle-Ziel

Beobachtungen aus heterogenen Quellen aufnehmen (`LH-FA-OBS-001`), deterministisch
**deduplizieren** (`LH-FA-OBS-004`), den Belief State über die **nachvollziehbare
Bayes-Pipeline** fortschreiben (`LH-FA-OBS-002`) und jede Änderung/Entscheidung
als **unveränderliches, rekonstruierbares Ereignisprotokoll** festhalten
(`LH-FA-AUD-001`..`LH-FA-AUD-004`). Erster HexSlice-Ausbau über den Kern hinaus:
`hexagon:application` + Ports + `adapters:*`.

## 2. Trigger (Welle startet)

- welle-01 done (Belief-Kern + Bayes-Update vorhanden).

## 3. Closure-Trigger (Welle schließt)

- Alle Slices der Welle done.
- `make gates` grün.
- Eine Beobachtung erzeugt nachvollziehbar ein Belief-Update **und** einen
  Protokoll-Eintrag; der Belief State ist aus dem Protokoll rekonstruierbar
  (`LH-FA-AUD-002`).
- Closure-Notiz in `done/welle-02-evidenz-audit-results.md`.

## 4. Slices in dieser Welle

Angelegt 2026-07-04; `slice-008` am 2026-07-05 in Fundament/Pipeline/Quelle
zerlegt (Modul 5, zu groß → `slice-008`/`009`/`010`).

| Slice | Titel | Status | Bezug |
|---|---|---|---|
| `slice-005` | Domain: Beobachtung/Quelle/Evidenz + Ereignis-Typen | in-progress (geliefert) | `LH-FA-OBS-006`, `LH-FA-AUD-001` |
| `slice-006` | Dedup korrelierter Beobachtungen | in-progress (geliefert) | `LH-FA-OBS-004` |
| `slice-007` | Ereignisprotokoll + Belief-Rekonstruktion | in-progress (geliefert) | `LH-FA-AUD-001`/`002`/`003` |
| `slice-008` | Fundament: `hexagon:application`-Modul + Audit-Port + Multi-Modul-`arch-check` | open | `LH-FA-AUD-001`, `ARC-06` |
| `slice-009` | Belief-Update-Pipeline (`belief-aktualisieren`) + LLM-/Uhr-Port + Fake-LLM | in-progress (geliefert) | `LH-FA-OBS-002` |
| `slice-010` | Beobachtungs-Port + Quelle-Adapter + E2E-Persistenz | in-progress (geliefert) | `LH-FA-OBS-001` |

## 5. Abhängigkeiten

- Blockiert: welle-03 (Aktionen/Gate setzt auf dem Update-Zyklus + Audit auf).
- Wird blockiert von: welle-01 (done).

## 6. Out-of-Scope für diese Welle

- Konfidenz-Gate, Wirkungsklassen, menschliche Freigabe (welle-03).
- VoI, Eskalation (welle-04).
- Echtes LLM — Likelihoods kommen deterministisch über einen **Fake-Adapter**
  hinter dem LLM-Port (`LH-QA-03`); der echte Adapter folgt in welle-05.
- Zeit: kommt über einen **Uhr-Port** (Fake in Tests) — kein `Clock.systemUTC()`
  im Kern (Determinismus).

## 7. Closure-Notiz

**Abgeschlossen 2026-07-05** — Evidenz→Belief→Audit-E2E grün (`E2eTest`),
71 Tests, alle Slices `005`..`010` in `done/`. Lerneintrag:
[`done/welle-02-evidenz-audit-results.md`](done/welle-02-evidenz-audit-results.md).

## 8. Sub-Area-Modus-Begründung

Alle berührten Sub-Areas GF (frisches Repo, Doku führt, Code folgt; keine
Bestandsinventur). Neue Sub-Areas `hexagon:application` und `adapters:*` werden
bei Anlage als GF geführt. Modus-Deklaration siehe
[`../../../harness/conventions.md`](../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
