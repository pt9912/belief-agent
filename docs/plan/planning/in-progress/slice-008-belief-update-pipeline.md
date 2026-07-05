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

- [x] `hexagon:application`-Modul existiert (Gradle-Modul, Dependency auf
      `hexagon:domain`) und baut im **Multi-Modul-Dockerfile** (`assemble`/
      `allTests` über beide Module; `make gates` grün).
- [x] **Audit-Port** als **anwendungsweites** Interface
      (`hexagon/application/…/ports/AuditPort.kt`, Rolle `port`, `ARC-06`):
      Vertrag `anhaengen`/`lade` zum Persistieren des `EreignisProtokoll`
      (slice-007); rein, importiert nur Domäne, framework-frei (`ADR-0001`/`ADR-0003`).
- [x] `.a-check.yml`-`resolution` auf **Multi-Modul** erweitert (je Modul ein
      Root, geteiltes `package_base`); **a-check v0.11.0** löst FQNs
      datei-mengen-bewusst auf → `make arch-check` grün **und echt durchsetzend**
      über `domain` + `application` (negativ-getestet: `domain→application` = 1
      Befund). Retired die `CO-001`-Klasse **ohne** Carveout (a-check-Fix per
      übergebenem Prompt).
- [x] `make gates` grün (5 Gates; Coverage-Gate auf `hexagon:domain`, da
      `hexagon:application` in slice-008 interface-only ist).
- [x] Closure-Notiz (bei Welle-02-Closure).

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

- **Multi-Modul-a-check (Hauptrisiko) — AUFGELÖST.** v0.10.0 rejectete jede
  Multi-Root-Config (Exit 2), alle Umgehungen waren falsch-grün (negativ-
  getestet). Über einen an den a-check-Code-Agenten übergebenen Fix-Prompt
  entstand **v0.11.0**: der Resolver löst interne FQNs datei-mengen-bewusst
  gegen die realen Dateien unter `roots` auf (nicht am Wurzel-Präfix). Zwei
  Modul-Roots über geteiltem `package_base` reichen jetzt und setzen **echt
  durch** — kein Carveout, `CO-001`-Klasse endgültig aufgelöst.
- **Coverage-Gate:** ein Interface trägt keine coverbare Logik; ggf.
  `application`-Modul im Kover-Scope so führen, dass die Schwelle (`ADR-0004`)
  nicht durch Interface-Grundlast kippt (dokumentieren, nicht wegfiltern).

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** `hexagon:application`-Modul + anwendungsweiter
`AuditPort`; Multi-Modul-Dockerfile. **Steering-Loop (Kern-Lehre der Welle):**
a-check v0.10.0 konnte Multi-Modul-KMP nicht durchsetzen (Guard-Reject bzw.
**falsch-grün**, per Negativ-Test entlarvt); ein Fix-Prompt an den a-check-Agenten
führte zu **v0.11.0** (datei-mengen-bewusste Auflösung) → arch-check echt
durchsetzend, **kein Carveout** (`CO-001`-Klasse endgültig gelöst). **Regel
geschärft:** nie eine arch-Config committen, die den Negativ-Test nicht besteht
(Modul 13). **Offen:** —.

## 8. Sub-Area-Modus-Begründung

Neue Sub-Area `hexagon:application` — GF (frisch angelegt, Doku führt, kein
Bestandscode). Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
