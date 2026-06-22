# ADR-0002: Implementierungssprache und Plattform — Java auf der JVM (Micronaut nur am Rand)

**Status:** Proposed

**Datum:** 2026-06-22

**Autor:** belief-agent (Bootstrap → erster Workflow-Schritt)

**Bezug:** [`LH-RB-04`](../../../spec/lastenheft.md#lh-rb-04--zielplattform-jvm)

**Schärft:** — (Plattform-/Prozess-Entscheidung ohne eigenes Spec-Stratum;
die konkreten Toolchain-Details — JDK-Pin, Gradle-Layout — landen mit dem
ersten Code-Slice in den Build-Dateien und ggf. `spec/spezifikation.md`.)

---

## Kontext

`LH-RB-04` (Kann) nennt die **JVM** als Zielplattform und eine Realisierung
als **Java-/Micronaut-Framework** als vorgesehen. ADR-0001 verlangt einen
**framework-freien Belief-Kern** (Entscheidungslogik außerhalb des Modells
und außerhalb von Infrastruktur). Eine konkrete Sprach-, Build- und
Framework-Wahl ist die Vorbedingung (Trigger) für `welle-01-belief-kern`.

## Entscheidung

Wir wählen **Java (aktuelles LTS, Java 21) auf der JVM**, Build mit
**Gradle (Kotlin-DSL)**. **Micronaut** wird ausschließlich am Rand
eingesetzt — in der Runtime/DI-Schicht (ARC-09) und den Adaptern (ARC-08).
Der Belief-Kern (ARC-01 bis ARC-06) bleibt **framework- und
annotationsfrei** (ADR-0001): kein `io.micronaut.*`-Import im Kern.

## Verglichene Alternativen

### Option A — Plain Java/JVM ohne Framework

- Pro: minimale Abhängigkeiten; voller Kontrolle über die hexagonale Reinheit.
- Contra: DI-, Konfigurations- und Test-Verdrahtung von Hand; mehr Boilerplate
  an den Rändern (der Kern soll ohnehin framework-frei sein).

### Option B — Java + Spring Boot

- Pro: großes Ökosystem.
- Contra: schwergewichtiger, reflection-/proxy-lastig, langsamer Start;
  `LH-RB-04` nennt ausdrücklich Micronaut, nicht Spring.

### Option C — Kotlin auf der JVM

- Pro: prägnanter, Null-Safety.
- Contra: `LH-RB-04` nennt Java; zusätzlicher Sprach-/Team-Aufwand ohne
  fachlichen Mehrwert für den Belief-Kern.

### Option D — Java + Micronaut (gewählt)

- Pro: Konstruktor-DI (passt zur Port/Adapter-Verdrahtung), schneller Start,
  AOT-/GraalVM-fähig, geringe Reflection; entspricht `LH-RB-04`.
- Contra: erfordert Disziplin, den Kern framework-frei zu halten — abgesichert
  durch die Fitness Function unten.

### Option E — Nicht-JVM (Go/Python)

- Pro: —
- Contra: widerspricht `LH-RB-04`.

## Konsequenzen

- Positiv: Konstruktor-DI erleichtert die Verdrahtung austauschbarer Ports
  (`LH-QA-04`, `LH-FA-LLM-004`); JUnit ermöglicht deterministische Tests mit
  Fake-Adaptern (`LH-QA-03`).
- Negativ: Framework am Rand erfordert einen Wächter, dass der Kern frei
  bleibt; JVM-Startzeit ist ohne AOT spürbar (für ein Framework unkritisch).
- Folgepflicht: Gradle-Build-Skelett, JDK-Pin, `arch-check`-Regel (Kern
  importiert kein Framework-/Adapter-Paket — gemeinsam mit ADR-0001).

## Fitness Function (falls maschinell prüfbar)

Greift mit dem ersten Code-Slice (bis dahin „Nicht behauptet" im
Sensors-Roster).

| Tooling | Regel | Make-Target |
|---|---|---|
| ArchUnit | Kern-Pakete (ARC-01..06) importieren kein `io.micronaut.*` und kein Adapter-Paket | `make arch-check` |
| Gradle + JDK 21 (gepinnt) | reproduzierbarer Build und Testlauf | `make build`, `make test` |

## Re-Evaluierungs-Trigger

Wenn GraalVM-native-image verbindlich gefordert wird; wenn ein Nicht-JVM-Port
nötig wird; bei einem Micronaut-Major-Wechsel mit Breaking Changes.

## Geschichte

| Datum | Ereignis | Verweis |
|---|---|---|
| 2026-06-22 | Proposed | Bootstrap → Workflow-Übergang (Trigger welle-01) |
