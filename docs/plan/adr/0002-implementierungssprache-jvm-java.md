# ADR-0002: Implementierungssprache und Plattform — Kotlin Multiplatform (JVM-Ziel zuerst)

**Status:** Proposed

**Datum:** 2026-06-22

**Autor:** belief-agent (Bootstrap → erster Workflow-Schritt)

**Bezug:** [`LH-RB-04`](../../../spec/lastenheft.md#lh-rb-04--zielplattform-jvm)

**Schärft:** — (Plattform-/Prozess-Entscheidung ohne eigenes Spec-Stratum;
die konkreten Toolchain-Details — JDK-Pin, Gradle-/KMP-Source-Set-Layout —
landen mit dem ersten Code-Slice in den Build-Dateien und ggf.
`spec/spezifikation.md`.)

---

## Kontext

`LH-RB-04` (Kann) nennt die **JVM** als Zielplattform; die konkrete Sprach-,
Build- und Framework-Wahl delegiert das Lastenheft als „Wie" an
`spec/spezifikation.md` und damit an diese ADR. ADR-0001 verlangt einen
**framework-freien Belief-Kern** (Entscheidungslogik außerhalb des Modells
und außerhalb von Infrastruktur). Eine konkrete Sprach-, Build- und
Framework-Wahl ist die Vorbedingung (Trigger) für `welle-01-belief-kern`.

Die hexagonale Reinheit aus ADR-0001 („Kern importiert nie einen Adapter")
ist mit einem flachen Java-/Kotlin-JVM-Projekt nur **per Paketkonvention**
und einer nachgelagerten Fitness Function absicherbar. Wird der reine Kern
(ARC-01..06) dagegen in ein plattform-gemeinsames, abhängigkeitsarmes
Source-Set gelegt, ist die Framework- und Adapter-Freiheit **strukturell**
erzwungen (das Source-Set sieht die Framework-/Adapter-Artefakte gar nicht),
nicht bloß per Regel. Genau das leistet ein Multiplatform-`commonMain`.

## Entscheidung

Wir wählen **Kotlin mit Kotlin Multiplatform (KMP)** als Build-Modell,
Build mit **Gradle (Kotlin-DSL)**, JVM-Toolchain auf **JDK 21**. Die
Source-Sets binden die Architekturschichten:

- **`commonMain`** — reiner Belief-Kern (ARC-01 Domain/Types bis ARC-06
  Audit): **framework- und plattformfrei**, nur Kotlin-Stdlib. Kein
  DI-/Framework-Paket, kein Adapter-Paket (ADR-0001).
- **`commonTest`** — deterministische Kern-Tests mit Fake-Adaptern
  (`LH-QA-03`).
- **`jvmMain`** — Ports-Verdrahtung, Adapter (ARC-07/ARC-08) und
  Orchestrator/Runtime (ARC-09). Als **DI am Rand** wird **Koin**
  eingesetzt — eine **KMP-fähige** DI, damit ein späteres zweites Ziel
  ohne DI-Wechsel auskommt. Die DI-Verdrahtung bleibt im Rand (ARC-09); der
  Kern (`commonMain`) bleibt DI- und framework-frei (ADR-0001).
- **`jvmTest`** — Integrations-/Adapter-Tests auf der JVM.

**Zunächst wird ausschließlich das JVM-Ziel (`jvm()`) gebaut und getestet.**
Damit bleibt `LH-RB-04` (Zielplattform JVM) erfüllt; weitere KMP-Ziele
(Native/JS/WASM) sind durch die `commonMain`-Struktur **vorbereitet**, aber
erst mit eigenem Trigger *und* Aufweitung von `LH-RB-04` aktiv (siehe
Re-Evaluierungs-Trigger). Diese ADR weitet das Lastenheft **nicht** aus.

## Verglichene Alternativen

### Option A — Plain Java/JVM ohne Framework

- Pro: minimale Abhängigkeiten; volle Kontrolle über die hexagonale Reinheit.
- Contra: Kern-Reinheit nur per Paketkonvention; DI-, Konfigurations- und
  Test-Verdrahtung von Hand; mehr Boilerplate an den Rändern.

### Option B — Java + Spring Boot

- Pro: großes Ökosystem.
- Contra: schwergewichtig, reflection-/proxy-lastig, langsamer Start —
  gegenläufig zu einem schlanken Rand um einen framework-freien Kern.

### Option C — Kotlin/JVM, single-target (kein KMP)

- Pro: prägnant, Null-Safety; einfacheres Gradle-Layout als KMP.
- Contra: hexagonale Reinheit weiterhin nur konventions-, nicht
  source-set-erzwungen; eine spätere Ziel-Erweiterung (Native/JS/WASM)
  erzwänge einen Umbau des Projektlayouts.

### Option D — Kotlin Multiplatform, JVM-Ziel zuerst, DI mit Koin (gewählt)

- Pro: `commonMain` erzwingt den framework-/plattformfreien Kern
  **strukturell** (ADR-0001); deterministische Kern-Tests in `commonTest`
  (`LH-QA-03`); spätere Ziel-Erweiterung ohne Kern-Umbau (`LH-QA-04`); das
  JVM-Ziel erfüllt `LH-RB-04`; **Koin ist KMP-fähig**, damit bleibt die
  DI-Wahl bei einem zweiten Ziel unverändert; Kotlin-Null-Safety/Prägnanz.
- Contra: KMP-Build-/Gradle-Konfiguration aufwändiger als plain JVM; eine
  DI am Rand erfordert weiter Disziplin, dass der Kern DI-frei bleibt
  (Fitness Function unten); **Koin ist eine Laufzeit-DI** (kein
  Compile-Zeit-AOT wie ein annotationsbasiertes JVM-Framework).

### Option E — Voll-KMP mit mehreren Zielen sofort

- Pro: unmittelbare Multiplatform-Reichweite.
- Contra: `LH-RB-04` nennt derzeit **nur** die JVM — mehrere Ziele jetzt
  bräuchten eine Lastenheft-Aufweitung **ohne aktuellen fachlichen Bedarf**
  (YAGNI). Deshalb Ziele bewusst zurückgestellt.

### Option F — Nicht-JVM (Go/Python)

- Pro: —
- Contra: widerspricht `LH-RB-04`.

## Konsequenzen

- Positiv: strukturell (per Source-Set-Sichtbarkeit) erzwungene hexagonale
  Reinheit; deterministische Kern-Tests in `commonTest` (`LH-QA-03`);
  spätere Multiplatform-Reichweite ohne Kern- **und ohne DI-Umbau**
  (`LH-QA-04`, `LH-FA-LLM-004`) — Koin ist bereits KMP-fähig; Kotlin-Null-Safety.
- Negativ: KMP-Gradle-Konfiguration aufwändiger als ein flaches JVM-Projekt;
  Koin ist eine Laufzeit-DI (kein Compile-Zeit-AOT); die DI-Verdrahtung
  bleibt am Rand (ARC-09), nie im Kern.
- Folgepflicht: KMP-Gradle-Build-Skelett (`commonMain`/`commonTest`/
  `jvmMain`/`jvmTest`), JDK-21-Pin, `arch-check`-Regel über das
  Harness-Arch-Gate **`a-check`** (Kern importiert kein DI-/Framework-Paket
  und kein Adapter-Paket) — gemeinsam mit ADR-0001.

## Fitness Function (falls maschinell prüfbar)

Greift mit dem ersten Code-Slice (bis dahin „Nicht behauptet" im
Sensors-Roster). Das `a-check`-Gate wird analog zu `d-check`
(digest-gepinnt) mit dem ersten Code-Slice adoptiert.

| Tooling | Regel | Make-Target |
|---|---|---|
| `a-check` (Harness-Arch-Gate, sprach-/ziel-agnostisch) | `commonMain` (ARC-01..06) importiert kein DI-/Framework-Paket (`org.koin.*`) und kein Adapter-Paket; Kern-Reinheit zusätzlich durch Source-Set-Sichtbarkeit erzwungen | `make arch-check` |
| Gradle (Kotlin-DSL) + JDK 21 (gepinnt), KMP-`jvm()`-Ziel | reproduzierbarer Build und Testlauf (`commonTest` + `jvmTest`) | `make build`, `make test` |

## Re-Evaluierungs-Trigger

- Wenn ein konkretes **Nicht-JVM-Ziel** (Native/JS/WASM) fachlich gefordert
  wird: `LH-RB-04` aufweiten (Lastenheft-Version + Begründung) und das Ziel
  im KMP-Build aktivieren. Da `koin-core` KMP-fähig ist, bleibt die DI-Wahl
  dabei unverändert; die Verdrahtung kann bei Bedarf nach `commonMain`
  wandern, der Kern bleibt DI-frei.
- Bei einem **Koin-Major-Wechsel** mit Breaking Changes; wenn
  GraalVM-native-image verbindlich gefordert wird.

## Geschichte

| Datum | Ereignis | Verweis |
|---|---|---|
| 2026-06-22 | Proposed (Entscheidung: Java auf der JVM, Micronaut am Rand) | Bootstrap → Workflow-Übergang (Trigger welle-01) |
| 2026-06-23 | Bezug an Lastenheft v0.4 angepasst: `LH-RB-04` nennt nur noch die JVM; Sprach-/Framework-Begründung auf technische Merit umgestellt (Status bleibt Proposed) | Review spec/architecture |
| 2026-07-04 | Entscheidung von „Java auf der JVM" auf **Kotlin Multiplatform (JVM-Ziel zuerst)** geändert (Status bleibt Proposed): `commonMain` erzwingt den framework-freien Kern strukturell; das JVM-Ziel erfüllt `LH-RB-04` unverändert; DI am Rand auf **Koin** (KMP-fähig) statt Micronaut; `arch-check` über das Harness-Tool `a-check` statt ArchUnit; weitere KMP-Ziele zurückgestellt (Re-Eval-Trigger). Dateiname (`…-jvm-java`) als numerische ADR-Identität beibehalten. | Diskussion Sprachwahl |
