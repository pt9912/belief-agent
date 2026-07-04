# Slice slice-005: Domain — Beobachtung/Quelle/Evidenz + Ereignis-Typen

**Status:** open → next → in-progress → done (siehe [Planning-README](../README.md)).

**Welle:** [`welle-02-evidenz-audit`](../welle-02-evidenz-audit.md).

**Bezug:** `LH-FA-OBS-006`, `LH-FA-AUD-001`, `LH-QA-03`; `ADR-0001`, `ADR-0003`;
`ARC-01`.

**Autor:** pt9912. **Datum:** 2026-07-04.

---

## 1. Ziel

Die puren Domänentypen für Evidenz und Protokoll in `hexagon:domain`:
`Beobachtung` (mit `Quelle` und `Zeitstempel`, `LH-FA-OBS-006`), `Quelle`
(Test/Build/Log/Mensch/Repo, `LH-FA-OBS-001`-Aufzählung), `Evidenz` und die
`Ereignis`-Typen für das Protokoll (`LH-FA-AUD-001`: Hypothese hinzugefügt,
Beobachtung erfasst, Belief aktualisiert, Aktion vorgeschlagen, Gate abgelehnt,
Eskalation angefordert). Noch **ohne** Protokoll-Logik (slice-007) und Dedup
(slice-006).

## 2. Definition of Done

- [x] `LH-FA-OBS-006` erfüllt: `Beobachtung` trägt `Quelle` + `Zeitstempel`;
      `BeobachtungTest`.
- [x] `LH-FA-AUD-001` erfüllt: `Ereignis` (sealed) deckt die sechs
      Ereignisarten ab, je mit `Zeitstempel` (`LH-FA-AUD-004`); `EreignisTest`.
- [x] Typen in `hexagon:domain` (`commonMain`), framework-frei; `Zeitstempel`
      reiner, ordnender Wert (kein `Clock`-Aufruf im Typ).
- [x] `make gates` grün (5 Gates inkl. `arch-check`/`coverage-gate`).
- [ ] Closure-Notiz (bei Welle-02-Closure).

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/domain/.../Beobachtung.kt`, `Quelle.kt`, `Evidenz.kt` | neu | Evidenz-Domäne (`ARC-01`) |
| `hexagon/domain/.../Ereignis.kt` | neu | Ereignis-Typen fürs Protokoll (`LH-FA-AUD-001`) |
| `hexagon/domain/.../*Test.kt` | neu | deterministische Konstruktionstests (`LH-QA-03`) |

## 4. Trigger

welle-02-Start (welle-01 done).

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz; Datei nach `done/`.

## 6. Risiken und offene Punkte

- `Zeitstempel`-Repräsentation: reiner Wert (z. B. `Instant`-Wrapper / epoch);
  Quelle der Zeit ist der Uhr-Port (slice-008), nicht der Typ.
- Protokoll-/Dedup-Logik bewusst später (slice-006/007).

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss füllen. -->

## 8. Sub-Area-Modus-Begründung

Alle berührten Sub-Areas GF (frisches Repo, Doku führt; siehe Kurs Modul 5).
Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
