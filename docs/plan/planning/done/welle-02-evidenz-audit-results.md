# Welle-02 Evidenz + Audit — Ergebnisse

**Abgeschlossen:** 2026-07-05. **Zielmeilenstein:** kein direkter Meilenstein
(M2 leitet sich aus welle-02..04 ab).

Verweis: [`welle-02-evidenz-audit`](../welle-02-evidenz-audit.md).

---

## Geliefert

| Slice | Deliverable | Anforderungen |
|---|---|---|
| `slice-005` | Domänentypen `Beobachtung`/`Quelle`/`Evidenz` + sealed `Ereignis` (`hexagon:domain`) | `LH-FA-OBS-001`, `LH-FA-OBS-006`, `LH-FA-AUD-001`/`004` |
| `slice-006` | Dedup korrelierter Beobachtungen (`Dedup`, Signatur `Quelle`+`Evidenz`) | `LH-FA-OBS-004` |
| `slice-007` | Append-only `EreignisProtokoll` + `Rekonstruktion` (Replay → Belief) | `LH-FA-AUD-001`/`002`/`003` |
| `slice-008` | Fundament: `hexagon:application`-Modul + anwendungsweiter `AuditPort`; Multi-Modul-`arch-check` (a-check v0.11.0) | `LH-FA-AUD-001`; `ARC-06` |
| `slice-009` | Pipeline `belief-aktualisieren` + LLM-/Uhr-Port + Fake-LLM (`llm-fake`) | `LH-FA-OBS-002` |
| `slice-010` | Beobachtungs-Port + Quelle-Adapter (`observation-fake`) + Audit-Persistenz (`audit-memory`) + E2E-Spur | `LH-FA-OBS-001`; `LH-FA-AUD-002` |

## Closure-Trigger — Nachweis

- ✅ Alle Slices `005`..`010` `done`.
- ✅ `make gates` grün (5 Gates: `doc-check` + `build` + `test` + `coverage-gate` + `arch-check`).
- ✅ E2E: eine Beobachtung erzeugt nachvollziehbar ein Belief-Update **und**
  Protokoll-Einträge; der Belief State ist aus dem persistierten Protokoll
  rekonstruierbar (`E2eTest`, `LH-FA-OBS-002`/`LH-FA-AUD-002`).
- ✅ Diese Closure-Notiz.

## Kennzahlen

- **71 deterministische Tests** (`LH-QA-03`), alle grün.
- **Line-Coverage `hexagon:domain` 97,37 %** (Kover); Gate ≥ 90 % (`ADR-0004`).
- **5 Gradle-Module** (domain, application, 3 outbound-Adapter), Docker-only, digest-gepinnt.
- **HexSlice ausgebaut:** domain + erster application-Use-Case + erste outbound-Adapter (LLM/Quelle/Audit).

## Architektur-/Prozess-Entscheidungen der Welle

- **Weg C** (slice-007→008): Audit-Port als **anwendungsweiter Port** in der
  application-Schicht, kein Domänentyp.
- **slice-008 zerlegt** (Regelwerk Modul 5, zu groß) in Fundament/Pipeline/Quelle
  (008/009/010) — Schnitt nach Lieferwert.
- **a-check v0.10.0 → v0.11.0** (`MR-005`): Multi-Modul-KMP-Resolution
  datei-mengen-bewusst — durch einen an den a-check-Agenten übergebenen Fix-Prompt.
- **Regelwerk v1.3.0 → v1.4.0 committet vendored** (`MR-007`); Upstream-MR-018
  (keine Templates) bewusst nicht adoptiert (`MR-008`).

## Offene Punkte / Follow-ups

- **Coverage-Scope:** Gate ist domain-only; application/adapter-Logik ist
  test-abgedeckt, aber nicht gate-scoped → Coverage-Scope-ADR, wenn mehr
  application-Logik existiert.
- **Composition-Root / DI:** cli (Koin) nach welle-03 (Entscheidungszyklus)
  verschoben; die E2E-Orchestrierung lebt vorerst im arch-check-befreiten Testcode.
- **Lifecycle-„Buffet":** delivered Slices lagen bis Welle-Closure gebündelt in
  `in-progress/` (Modul 5 WIP=1 / Lerneintrag je Slice) — dokumentiert in
  `MR-007`; Konventions-Entscheidung offen.

## Steering-Loop-Lernen

- **Negativ-Test-Pflicht für Arch-Gates:** drei a-check-Configs waren still
  **falsch-grün** — erst der Negativ-Test (`domain→application` **muss** 1 Befund
  geben) entlarvte sie. Nie eine arch-Config committen, die den Negativ-Test
  nicht besteht (Modul 13).
- **Fix statt Carveout:** ein präziser Bug-Prompt an das Upstream-Tool
  (a-check → v0.11.0) löste die Multi-Modul-Klasse endgültig — besser als der
  geplante Carveout.
- **Slice-Sizing greift:** die frühzeitige Zerlegung von slice-008 hielt jeden
  Schnitt in einer Review-Sitzung prüfbar.
- **Vendored Regelwerk:** das Betriebsregelwerk muss aus dem Checkout lesbar sein
  — der Session-Bootstrap „lies das relevante Modul" setzt einen netzlosen,
  integritäts-verifizierten Bestand voraus (`MR-007`).
