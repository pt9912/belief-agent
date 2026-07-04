# Welle welle-02-evidenz-audit: Evidenz-Aufnahme + Audit

**Status:** in-progress

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

In `open/` angelegt (2026-07-04):

| Slice | Titel | Status | Bezug |
|---|---|---|---|
| `slice-005` | Domain: Beobachtung/Quelle/Evidenz + Ereignis-Typen | open | `LH-FA-OBS-006`, `LH-FA-AUD-001` |
| `slice-006` | Dedup korrelierter Beobachtungen | open | `LH-FA-OBS-004` |
| `slice-007` | Ereignisprotokoll + Belief-Rekonstruktion + Audit-Port | open | `LH-FA-AUD-001`, `LH-FA-AUD-002`, `LH-FA-AUD-003` |
| `slice-008` | Belief-Update-Pipeline (application `belief-aktualisieren`) + Ports + Fake-Adapter | open | `LH-FA-OBS-001`, `LH-FA-OBS-002` |

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

(Erst nach Welle-Abschluss füllen; Verweis auf
`done/welle-02-evidenz-audit-results.md`.)

## 8. Sub-Area-Modus-Begründung

Alle berührten Sub-Areas GF (frisches Repo, Doku führt, Code folgt; keine
Bestandsinventur). Neue Sub-Areas `hexagon:application` und `adapters:*` werden
bei Anlage als GF geführt. Modus-Deklaration siehe
[`../../../harness/conventions.md`](../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
