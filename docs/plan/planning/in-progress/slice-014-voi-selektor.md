# Slice slice-014: VoI-Selektor — informativste Beobachtung wählen

**Status:** open → next → in-progress → done (siehe [Planning-README](../README.md)).

**Welle:** `welle-04-voi-eskalation` (aktiv, siehe [Roadmap](roadmap.md)).

**Bezug:** `LH-FA-VOI-002`, `LH-FA-VOI-003`, `LH-FA-VOI-004`, `LH-QA-03`;
`ADR-0001`, `ADR-0003`; `ARC-04`.

**Autor:** pt9912. **Datum:** 2026-07-05.

---

## 1. Ziel

Eine reine Domänen-Regel **`VoiSelektor`** (`hexagon:domain`, `ARC-04`) wählt aus
Kandidaten-Beobachtungen die **informativste**: die, welche die **zwei
wahrscheinlichsten** Hypothesen am stärksten trennt (`LH-FA-VOI-002`), abgewogen
nach erwartetem **Gewinn je Kosten** (`LH-FA-VOI-003`). Bewusst **lokal/heuristisch**
— keine global-optimale Policy (`LH-FA-VOI-004`). Deterministisch (`LH-QA-03`),
framework-frei. Jeder Kandidat trägt seine erwartete Diskriminierung + Kosten; in
welle-04 liefert ein Fake diese Schätzung, welle-05 externalisiert sie ans LLM.

## 2. Definition of Done

- [x] `LH-FA-VOI-002`: wählt aus mehreren Kandidaten die Beobachtung, die die zwei
      wahrscheinlichsten Hypothesen am stärksten trennt (erwartete Diskriminierung);
      Test (`waehlt_bei_gleichen_kosten_die_staerkste_top2_trennung`).
- [x] `LH-FA-VOI-003`: Abwägung **Gewinn je Kosten** (nicht roher Gewinn); Test mit
      teuer-aber-informativer vs. günstig-aber-schwacher Beobachtung
      (`waegt_gewinn_je_kosten_ab_nicht_rohen_gewinn`).
- [x] `LH-FA-VOI-004`: lokal/heuristisch, deterministisch; keine global-optimale
      Policy; im KDoc von `VoiSelektor` explizit als heuristisch deklariert; Tie-Break
      + Reihenfolge-Unabhängigkeit getestet.
- [x] Kern-lokal (`hexagon:domain/voi`), framework-frei (`ADR-0001`/`ADR-0003`);
      Coverage domain 97,81 % ≥ 90 % (`ADR-0004`/`ADR-0006`); `make gates` grün
      (doc-check 0/build/test/coverage-gate/arch-check 0 Befunde).
- [ ] Closure-Notiz (bei Welle-04-Closure).

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/domain/.../voi/VoiKandidat.kt` | neu | Kandidat: Beobachtung + erwartete Diskriminierung + Kosten (`LH-FA-VOI-003`) |
| `hexagon/domain/.../voi/VoiSelektor.kt` | neu | reine Regel: max Gewinn/Kosten über Top-2-Trennung (`LH-FA-VOI-002/004`, `ARC-04`) |
| `hexagon/domain/.../voi/*Test.kt` | neu | Top-2-Trennung, Gewinn/Kosten, Determinismus, Grenzfälle |

## 4. Trigger

welle-04 gestartet (welle-03 done) — `BeliefState` + Unsicherheitsmaße (welle-01)
vorhanden.

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz; Datei nach `done/`. Trägt mit slice-015/016 zum
Welle-04-Closure bei.

## 6. Risiken und offene Punkte

- Modellierung der „erwarteten Diskriminierung" ohne LLM: in welle-04 liefert ein
  deterministischer Kandidat/Fake die Schätzung; das LLM externalisiert sie in
  welle-05 (`ADR-0001`, `LH-FA-LLM`). Der Selektor bleibt rein rechnend.
- Grenzfälle: gleich-gute Kandidaten (deterministischer Tie-Break), leere
  Kandidatenliste (kein günstiger Zug → Signal an den Zyklus/Eskalation, slice-017).

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss füllen. -->

## 8. Sub-Area-Modus-Begründung

Neue Sub-Area `hexagon:domain/voi` — GF (frisch angelegt, Doku führt, kein
Bestandscode). Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
