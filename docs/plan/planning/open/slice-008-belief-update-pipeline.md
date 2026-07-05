# Slice slice-008: Fundament — `hexagon:application`-Modul + Audit-Port + Multi-Modul-arch-check

**Status:** open → next → in-progress → done (siehe [Planning-README](../README.md)).

**Welle:** [`welle-02-evidenz-audit`](../welle-02-evidenz-audit.md).

**Bezug:** `LH-FA-AUD-001`, `LH-QA-04`; `ADR-0001`, `ADR-0003`; `ARC-06`, `ARC-07`.

**Autor:** pt9912. **Datum:** 2026-07-05 (Zerlegung des ursprünglichen slice-008).

---

## 1. Ziel

Das **Fundament** für den ersten HexSlice-Ausbau über den Kern hinaus: das
Gradle-Modul `hexagon:application` steht, der **anwendungsweite Audit-Port**
(`ARC-06`) lebt in seinem korrekten Zuhause (`hexagon/application/.../ports/`,
Rolle `port`), der Dockerfile-Build umfasst mehrere Module, und die
**a-check-`resolution`** ist paket-spezifisch auf das Multi-Modul-Layout
erweitert. Bewusst **ohne** Use-Case-/Adapter-Logik — die kommt in slice-009
(Pipeline) und slice-010 (Quelle + E2E) darauf.

Der Slice **retired isoliert** das Multi-Modul-`arch-check`-Risiko (v0.10.0-Guard,
`CO-001`-Klasse), bevor Pipeline-Logik es überlagert.

## 2. Definition of Done

- [ ] `hexagon:application`-Modul existiert (Gradle-Modul, Dependency auf
      `hexagon:domain`) und baut im **Multi-Modul-Dockerfile**
      (`build`/`test`/`coverage` über `domain` + `application`).
- [ ] **Audit-Port** als **anwendungsweites** Interface
      (`hexagon/application/.../ports/`, Rolle `port`, `ARC-06`): Vertrag zum
      Persistieren des `EreignisProtokoll` (slice-007); rein, framework-frei
      (`ADR-0001`/`ADR-0003`).
- [ ] `.a-check.yml`-`resolution` paket-spezifisch auf Multi-Modul erweitert
      (disjunkte Sub-Namespaces, v0.10.0-Guard-konform); `make arch-check` grün
      über `domain` + `application` (retired die `CO-001`-Klasse).
- [ ] `make gates` grün (Coverage-Gate berücksichtigt das neue Modul,
      Interface-Only-Realität dokumentiert).
- [ ] Closure-Notiz (bei Welle-02-Closure).

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `settings.gradle.kts` | update | `include("hexagon:application")` |
| `hexagon/application/build.gradle.kts` | neu | KMP-Modul, Dependency auf `hexagon:domain` |
| `hexagon/application/.../ports/AuditPort.kt` | neu | anwendungsweiter Audit-Port (`ARC-06`) |
| `Dockerfile` | update | `build`/`test`/`coverage` über `domain` + `application` |
| `.a-check.yml` | update | Multi-Modul-`resolution` (paket-spezifisch, Guard-konform) |

## 4. Trigger

`slice-007` done (das `EreignisProtokoll`, das der Audit-Port persistiert,
existiert).

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz; Datei nach `done/`. Enabler für slice-009/010.

## 6. Risiken und offene Punkte

- **Multi-Modul-a-check (Hauptrisiko):** mehrere `resolution`-Roots über
  geteiltes `package_base` → v0.10.0-Guard bricht bei mehrdeutiger Auflösung
  mit Exit 2 ab (`CO-001`-Historie). Auflösung: disjunkte Sub-Namespaces
  `dev.beliefagent.{domain,application}` + Globs tiefer als die Roots. Bei
  hartnäckigem Guard-Bruch: Carveout mit Auflösungs-Trigger statt roter Merge.
- **Coverage-Gate:** ein Interface trägt keine coverbare Logik; ggf.
  `application`-Modul im Kover-Scope so führen, dass die Schwelle (`ADR-0004`)
  nicht durch Interface-Grundlast kippt (dokumentieren, nicht wegfiltern).

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss füllen. -->

## 8. Sub-Area-Modus-Begründung

Neue Sub-Area `hexagon:application` — GF (frisch angelegt, Doku führt, kein
Bestandscode). Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
