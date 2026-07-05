# Slice slice-010: Beobachtungs-Port + Quelle-Adapter + Protokoll-/Audit-Persistenz (E2E)

**Status:** open → next → in-progress → done (siehe [Planning-README](../README.md)).

**Welle:** [`welle-02-evidenz-audit`](../welle-02-evidenz-audit.md).

**Bezug:** `LH-FA-OBS-001`, `LH-FA-OBS-002`, `LH-FA-AUD-002`; `ADR-0001`,
`ADR-0003`; `ARC-06`, `ARC-08`.

**Autor:** pt9912. **Datum:** 2026-07-05 (Zerlegung des ursprünglichen slice-008).

---

## 1. Ziel

Die **Beobachtungsaufnahme** (`LH-FA-OBS-001`) und die **volle Spur** end-to-end:
ein **Beobachtungs-Port** mit mindestens einem **Quelle-Adapter** speist die
Pipeline (slice-009); der Lauf Quelle → Uhr → Update → `EreignisProtokoll`
(slice-007) → **Audit-Persistenz** (implementiert den Audit-Port aus slice-008)
ist deterministisch nachvollziehbar, und der Belief State ist aus dem
persistierten Protokoll **rekonstruierbar** (`LH-FA-AUD-002`). Schließt welle-02.

## 2. Definition of Done

- [x] `LH-FA-OBS-001` erfüllt: `FakeBeobachtungsQuelle`
      (`adapters/outbound/observation-fake`) als Beobachtungsquelle über den
      **`BeobachtungsPort`** (`FakeBeobachtungsQuelleTest`).
- [x] `LH-FA-OBS-002` (vollständig) erfüllt: E2E `Quelle → BeliefAktualisieren →
      Ereignisse → MemoryAudit → Rekonstruktion` (`E2eTest`); rekonstruierter
      Belief == Live-Posterior (`LH-FA-AUD-002`, nutzt slice-007),
      deterministisch (`LH-QA-03`).
- [x] **Audit-Persistenz-Adapter** `MemoryAudit`
      (`adapters/outbound/audit-memory`) implementiert den `AuditPort`
      (slice-008), append-only im Speicher (`MemoryAuditTest`).
- [x] Ports korrekt platziert (`BeobachtungsPort` use-case-lokal, `AuditPort`
      anwendungsweit); Kern importiert keinen Adapter (`arch-check` grün über
      **5 Module**, `ADR-0001`/`ADR-0003`).
- [x] `make gates` grün (5 Gates; 71 Tests).
- [ ] Closure-Notiz (bei Welle-02-Closure).

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/application/.../ports/` (Beobachtungs-Port) | neu | Vertrag zur Quellen-Aufzählung (`ARC-07`) |
| `adapters/outbound/observation-*/**` | neu | Beobachtungsquelle als Adapter (`LH-FA-OBS-001`, `ARC-08`) |
| `adapters/outbound/audit-*/**` | neu | Audit-Persistenz → implementiert Audit-Port (`ARC-06`/`ARC-08`) |
| E2E-Test (application/adapter) | neu | Quelle → Update → Protokoll → Persistenz (`LH-FA-OBS-002`) |
| `settings.gradle.kts`, `Dockerfile`, `.a-check.yml` | update | neue Adapter-Module einhängen |

## 4. Trigger

`slice-008` done (Modul + Audit-Port) **und** `slice-009` done (Pipeline +
Uhr-Port).

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz; Datei nach `done/`. Erfüllt zusammen mit
slice-007 den **Welle-02-Closure-Trigger** (Beobachtung → Update + Protokoll,
rekonstruierbar) → danach Welle-Closure-Notiz in
`done/welle-02-evidenz-audit-results.md`.

## 6. Risiken und offene Punkte

- Mehrere Adapter-Module → `.a-check.yml`-`resolution` und Dockerfile erneut
  nachziehen (auf slice-008-Basis, mechanisch).
- E2E-Umfang: Composition-Root/DI-Verdrahtung — bei Überlänge weiter teilen.

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss füllen. -->

## 8. Sub-Area-Modus-Begründung

Neue Sub-Areas `adapters/outbound/observation-*`, `adapters/outbound/audit-*`
— GF (frisch angelegt, Doku führt, kein Bestandscode). Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
