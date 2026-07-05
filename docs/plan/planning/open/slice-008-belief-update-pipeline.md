# Slice slice-008: Belief-Update-Pipeline (application `belief-aktualisieren`)

**Status:** open → next → in-progress → done (siehe [Planning-README](../README.md)).

**Welle:** [`welle-02-evidenz-audit`](../welle-02-evidenz-audit.md).

**Bezug:** `LH-FA-OBS-001`, `LH-FA-OBS-002`, `LH-QA-03`, `LH-QA-04`; `ADR-0001`,
`ADR-0002`, `ADR-0003`; `ARC-02`, `ARC-06`, `ARC-07`, `ARC-08`.

**Autor:** pt9912. **Datum:** 2026-07-04.

---

## 1. Ziel

Die **nachvollziehbare Belief-Update-Pipeline** (`LH-FA-OBS-002`) als erste
**application-Slice** `belief-aktualisieren` (`hexagon:application`): nimmt eine
`Beobachtung` aus einer Quelle (`LH-FA-OBS-001`) entgegen, holt Likelihoods über
den **LLM-Port** (in welle-02 ein deterministischer **Fake-Adapter**), dedupt
(slice-006), ruft `BayesUpdate` (slice-003), schreibt das Ergebnis und die
Ereignisse ins Protokoll (slice-007). Bringt die ersten **Ports** (LLM-,
Beobachtungs-, Uhr-, Audit-Port) und die ersten **Adapter** (`adapters:*`).

## 2. Definition of Done

- [ ] `LH-FA-OBS-002` erfüllt: eine Beobachtung erzeugt nachvollziehbar ein
      Belief-Update **und** Protokoll-Einträge; E2E-nah gegen Fake-Adapter
      getestet (deterministisch, `LH-QA-03`).
- [ ] `LH-FA-OBS-001` erfüllt: mindestens eine Beobachtungsquelle als Adapter
      (Fake/Test), über den Beobachtungs-Port angebunden.
- [ ] Ports lokal beim Use-Case (HexSlice); Kern importiert keinen Adapter
      (`arch-check` grün, `ADR-0001`/`ADR-0003`).
- [ ] **Audit-Port** als **anwendungsweites** Interface (`hexagon/application/ports/`,
      Rolle `port`, `ARC-06`) — von slice-007 hierher verschoben (Weg C: Port
      gehört in die application-Schicht, nicht in die Domäne); der
      Audit-Persistenz-Adapter (`adapters/outbound/audit-*`) implementiert ihn
      und persistiert das `EreignisProtokoll` (slice-007).
- [ ] Uhr-Port liefert `Zeitstempel` (Fake-Uhr in Tests) — kein `Clock`-Aufruf
      im Kern.
- [ ] a-check-`resolution` für die neuen Module paket-spezifisch erweitert
      (v0.10.0-Guard); `make arch-check` grün.
- [ ] `make gates` grün.
- [ ] Closure-Notiz.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `hexagon/application/belief/belief-aktualisieren/**` | neu | Use-Case-Slice + lokale Ports (`ARC-02`, HexSlice) |
| `settings.gradle.kts` | update | neue Module `hexagon:application`, `adapters:*` |
| `adapters/outbound/llm-fake/**` | neu | Fake-LLM-Adapter (Likelihoods, `LH-QA-03`) |
| `adapters/outbound/observation-*/**`, `adapters/outbound/audit-*/**` | neu | Beobachtungsquelle + Audit-Persistenz |
| `.a-check.yml` | update | Multi-Modul-`resolution` (paket-spezifisch, Guard-konform) |

## 4. Trigger

`slice-005`/`006`/`007` done (Typen, Dedup, Protokoll vorhanden).

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz; Datei nach `done/`. Erfüllt zusammen mit
slice-007 den Welle-Closure-Trigger (Beobachtung → Update + Protokoll).

## 6. Risiken und offene Punkte

- **Multi-Modul-a-check:** neue Module = mehrere `resolution`-Roots über
  geteiltes `package_base` → v0.10.0-Guard bricht bei mehrdeutiger Auflösung ab;
  disjunkte Sub-Namespaces `dev.beliefagent.{domain,application,adapter}` +
  tiefe Globs (siehe `CO-001`-Historie). Ggf. eigener Vorschalt-Schritt.
- Erster application-/adapter-Ausbau: Gradle-Modul-Verdrahtung + DI (Koin am
  Rand) — Umfang beobachten, ggf. Slice teilen.

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss füllen. -->

## 8. Sub-Area-Modus-Begründung

Neue Sub-Areas `hexagon:application`, `adapters:*` — GF (frisch angelegt, Doku
führt, kein Bestandscode). Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
