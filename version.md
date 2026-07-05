# belief-agent — Release-Register

> Kanonischer, **auflösender** Link-Ziel-Ort für Erwähnungen **eigener**
> belief-agent-Releases — etwa [die jeweils aktuelle Version](#aktuell). Nur die
> aktuelle Version trägt einen expliziten Anker `#vX.Y.Z`; beim Release **wandert**
> er zur neuen Zeile, sodass feste Pins auf veraltete Versionen brechen
> (`anchor-missing`) und ein vergessener Bump auffällt.
>
> **Fremde** Versionen (Werkzeuge: d-check, a-check, Gradle, Kotlin, Kover)
> gehören **nicht** hierher — sie sind per Digest in `Makefile`/`Dockerfile`/
> `build.gradle.kts` gepinnt und in `harness/conventions.md`
> (`MR-004`/`MR-005`/`MR-006`) sowie `ADR-0002`/`ADR-0004` dokumentiert.

## Aktuell

Aktuelle Version: [`v0.1.0`](#v0.1.0) — 2026-07-04 (in Entwicklung,
unveröffentlicht; `build.gradle.kts` `0.1.0-SNAPSHOT`).

Stabil referenzierbar als `version.md#aktuell`. Noch kein Release-Tag; der Anker
wandert mit dem ersten veröffentlichten `v0.1.0`.

## Verlauf

| Version | Datum | Stand |
| --- | --- | --- |
| `v0.1.0` <a id="v0.1.0"></a> | 2026-07-04 | in Entwicklung — welle-01..03 done (Belief-Kern; Evidenz + Audit; Aktionen + Konfidenz-Gate/Sicherheitsfunktion); Coverage-Gate auf alle Module (`ADR-0006`); welle-04 (VoI + Eskalation) aufgesetzt |
