# Versionen — belief-agent

Single Source of Truth für die Projekt-Version und die gepinnten Werkzeug-/
Toolchain-Versionen. **Digest-Pins** (`@sha256:…`) sind die verbindliche
Reproduzierbarkeits-Grenze; die Versions-Tags dienen der Lesbarkeit.

## Aktuell

v0.1.0-SNAPSHOT

Belief-agent-Projektversion; Welle-02 (Evidenz + Audit) in Arbeit.

## Gepinnte Werkzeuge

| Werkzeug | Version | Pin / Ort |
|---|---|---|
| d-check | v0.37.1 | `ghcr.io/pt9912/d-check@sha256:3bbdb19b…` (`Makefile` `DCHECK_DIGEST`, `MR-004`) |
| a-check | v0.10.0 | `ghcr.io/pt9912/a-check@sha256:0932cb1d…` (`Makefile` `A_CHECK_IMAGE`, `MR-005`) |
| Gradle | 8.14.5 | `gradle:8.14-jdk21@sha256:dae150d9…` (`Dockerfile`) |
| Kotlin | 2.4.0 | `build.gradle.kts` |
| JDK | 21 | `Dockerfile` / `jvmToolchain(21)` (`ADR-0002`) |
| Kover | 0.9.8 | `build.gradle.kts` (`ADR-0004`) |
| Koin | 4.2.2 | geplant am Adapter-Rand (`ADR-0002`, welle-02+) |

## Historie

| Datum | Änderung |
|---|---|
| 2026-07-04 | Initial: Projekt v0.1.0-SNAPSHOT; d-check v0.37.1, a-check v0.10.0, Gradle 8.14.5, Kotlin 2.4.0, Kover 0.9.8 gepinnt. |
