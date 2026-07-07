# Slice slice-031: Konkrete Build-/Git-Beobachter fuer Code-Agent

**Status:** open → next → in-progress → done (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung

**Bezug:** `LH-FA-OBS-001`, `LH-FA-OBS-002`, `LH-FA-OBS-006`, `LH-QA-02`, `LH-QA-04`; `ADR-0001`,
`ADR-0003`, `LH-FA-POL-006`; `ARC-08`.

**Autor:** pt9912. **Datum:** 2026-07-07.

---

## 1. Ziel

Konkrete Build- und Git-Beobachter hinter `BeobachtungsPort` bereitstellen,
ohne `example/code-agent` bereits umzubinden: lokale Build-Artefakte und der
lokale Git-Checkout werden als realistische, netzfreie Quellen modelliert;
Fixtures dienen nur als deterministische Replay-/Testform der gleichen
Beobachterdaten.

## 2. Definition of Done

- [x] `adapters/outbound/observation-build-report` liefert einen konkreten
  `BuildReportBeobachter` hinter `BeobachtungsPort`: er liest lokale Build-/Test-
  Artefakte oder deren Replay-Fixture, fuehrt selbst keinen Build aus, nutzt kein Netz
  und erzeugt `Quelle.BUILD`-Beobachtungen mit monotonem Zeitstempel.
- [x] `adapters/outbound/observation-git-local` liefert einen konkreten
  `GitStatusBeobachter` hinter `BeobachtungsPort`: er liest den lokalen Checkout
  oder dessen Replay-Fixture (mind. HEAD/Branch, Dirty-Status, geaenderte Dateien),
  fuehrt kein `git fetch`/Remote-Kommando aus und erzeugt `Quelle.REPO`-Beobachtungen
  mit monotonem Zeitstempel.
- [x] Die Replay-/Fixture-Minimalformate sind lokal an den beiden Beobachtern getestet
  und erzeugen stabile Beobachtungen fuer je einen festen Build- und Git-Datensatz;
  Fehlerklassen/Fallback-Policy bleiben explizit fuer `slice-033` ausserhalb dieses Slice.
- [x] Root-Registrierung ist konsistent: neue Module sind in `settings.gradle.kts`
  und `.a-check.yml` verdrahtet; `Dockerfile` enumeriert die neuen Adapter in
  Build-Metadaten-COPY, Dependency-Resolve, `koverLog` und `koverVerify`, damit
  `make build`, `make test`, `make coverage-gate`, `make arch-check` und `make gates`
  die neuen Adapter wirklich erfassen und gruen bleiben.
- [x] Neue Beobachtungs-Adapter sind nicht in produktive oder andere bestehende
  Composition-Routen eingebunden:
  - Auditierbarkeit: `git diff --name-only` (bezogen auf Merge-Base) darf nur Dateien in
    `adapters/outbound/observation-build-report`, `adapters/outbound/observation-git-local`,
    `settings.gradle.kts`, `.a-check.yml`, `Dockerfile`,
    `docs/plan/planning/in-progress/roadmap.md` und den Planungsdateien dieses Split enthalten.
  - Auditierbarkeit: `observation-build-report` und `observation-git-local` duerfen
    in Nicht-Demo-Composition-Routen (`adapters/inbound/cli`, `example/langchain`,
    `example/koog`, weitere nicht-demospezifische Composition-Dateien) weder neue noch
    geaenderte Bindings aufweisen.
- [x] Closure-Notiz mit Lerneintrag vorhanden.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `adapters/outbound/observation-build-report` | neu | Konkreter Build-Beobachter: lokale Build-/Test-Artefakte oder Replay-Fixture in `Quelle.BUILD`-Beobachtungen uebersetzen, ohne Build-Ausfuehrung im Adapter. |
| `adapters/outbound/observation-git-local` | neu | Konkreter Git-Beobachter: lokalen Checkout oder Replay-Fixture in `Quelle.REPO`-Beobachtungen uebersetzen, ohne Remote-Zugriff. |
| `settings.gradle.kts` | update | Falls neue Adapter-Module eingeführt werden, im Root korrekt registrieren (`include`/`includeBuild`), damit Build/arch-check konsistent greifen. |
| `.a-check.yml` | update | Bei neuen oder neu verdrahteten Adapter-Modulen werden Modulgrenzen/Layer-Checks aktualisiert; bei bestehendem Modul: Adapter-Neubindung explizit prüfen. |
| `Dockerfile` | update | Neue Adapter in Build-Metadaten-COPY, Dependency-Resolve, `koverLog` und `koverVerify` aufnehmen, damit Coverage-Gate nicht an ihnen vorbeilaeuft. |
| `adapters/outbound/observation-*/src/test/**` | neu | Minimalformat und deterministische Build-/Git-Beobachtung lokal am Adapter absichern. |
| `docs/plan/planning/in-progress/roadmap.md` | update | Split `slice-031`..`033` in `welle-05-llm-port Stabilisierung` sichtbar machen. |
| `docs/plan/planning/next/slice-032-code-agent-beobachtungsbinding.md` | neu | Folgeslice fuer Demo-Binding, README und Make/Docker-Kontrakt. |
| `docs/plan/planning/next/slice-033-code-agent-fixture-fehlerverifikation.md` | neu | Folgeslice fuer Fehlerklassen, Negativfallmatrix und Verification-Artefakt. |

## 4. Trigger

`slice-029` und `slice-030` sind in `done/` (Composition-Root/CLI-Demo-Dokumentations-Stand),
und `welle-05-llm-port Stabilisierung` nennt den Split `slice-031`..`033` als
naechsten Wellenkontext in `docs/plan/planning/in-progress/roadmap.md`.
- `slice-029`/`slice-030` werden beim Start noch einmal auf Status `done` geprueft; bei
  Statusaenderung ist der Slice zu blockieren.

## 5. Closure-Trigger

DoD vollstaendig + Review abgeschlossen + `make gates` gruen + Folgeslices `slice-032`
und `slice-033` bleiben in `next/` oder `open/` nachvollziehbar + Slice nach `done/`
verschoben.

## 6. Risiken und offene Punkte

- Parser fuer Build-/Git-Evidenz ist format- und tooling-abhaengig; dieser Slice
  beschraenkt sich daher auf minimale lokale Beobachter plus Replay-Fixtures statt
  Shell-/Remote-Ausfuehrung.
- Demo-Binding, Make/Docker-Defaultpfade und Env-Kontrakt sind bewusst nach
  `slice-032` geschnitten.
- Fehlerklassen, Exit-Codes und Negativfallmatrix sind bewusst nach `slice-033`
  geschnitten.
- Wenn Observability-Garantien (`Quelle`, `Zeitstempel`) lückenhaft bleiben, droht
  Drift zu `LH-FA-OBS-006` und `LH-FA-OBS-002`.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** Die realistischeren Beobachter konnten als isolierte
Outbound-Adapter geliefert werden, ohne `example/code-agent` oder produktive
Composition-Routen umzubinden. `BuildReportBeobachter` liest lokale
Build-/Test-Reportdaten oder Replay-Fixtures; `GitStatusBeobachter` liest nur
lokale Git-Daten (`HEAD`, Branch, Dirty-Status, geaenderte Dateien) oder ein
Replay-Fixture. Beide liefern port-konforme Beobachtungen mit `Quelle.BUILD`
bzw. `Quelle.REPO`.

**Was ging anders als geplant:** Das Coverage-Gate lief nach Aufnahme der zwei
neuen Adapter zunaechst in einen Kotlin-Compiler-OOM, weil mehrere Test-
Kompilationen parallel liefen. Die Gate-Schwelle wurde nicht gesenkt; der
`coverage-gate`-Docker-Target wurde stattdessen mit `--max-workers=1`
deterministischer gemacht.

**Steering-Loop:** Neue Adaptermodule muessen bei Aufnahme ins Coverage-Gate
nicht nur fachlich getestet, sondern auch ressourcenseitig als Gate-Teilnehmer
stabilisiert werden. Serielle Coverage-Verifikation ist fuer die wachsende
Adapterliste robuster als parallele Kompilation im Docker-Gate.

**Folge-Slices:** `slice-032` bindet die Beobachter in `example/code-agent`
und definiert das Runtime-Image. `slice-033` schaerft Fehlerklassen und
Negativfall-Verifikation.

**Review/Verification:** Der Abschluss ist mit
`docs/reviews/2026-07-07-slice-031-code-review.md` und
`docs/verifications/2026-07-07-slice-031-verification.md` gegen Modul 10 und
Modul 11 dokumentiert.

## 8. Sub-Area-Modus-Begründung

### Sub-Area: Beispiel-Integration (code-agent demo + Beispiele)

- **Modus:** Hybrid
- **Konventionen-Dichte:** mittel. `spec/architecture.md` fordert Adapter hinter Ports für Quellen (`ARC-08`), die konkrete Fixture-/Adapter-Bindung in dieser Sub-Area ist bisher jedoch nicht als Doku/Template fest standardisiert.
- **Phase-Reife:** Phase 4. Die Demo-Pfad-Doku ist gefestigt (`slice-030` done), die neue Beobachtungsadapter-Bindung ist jedoch ein neuer produktionsnaher Schritt innerhalb der Example-Integration.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Der Slice bindet `example/code-agent` noch nicht um; Risiko entsteht erst bei `slice-032`.
- **Reconciliation-Aufwand:** gering bis mittel. Folge-Slices `slice-032` und `slice-033` liefern Binding und Fehlerverifikation.

### Sub-Area: Outbound-Adapter-Ports (`adapters/outbound`, Build/Repo-Quelle)

- **Modus:** BF
- **Konventionen-Dichte:** mittel. Bestehende Ports/Architektur sind in ADR und Architektur geregelt; konkrete, testbare Beobachter- und Fixture-Spezifikation für Build/Git-Quelle ist neu zu etablieren.
- **Phase-Reife:** Phase 1–2. Es existieren stabile Port-Konventionen, aber die konkrete Beobachtungs-Eingangsstruktur ist neu und nicht vollständig in existierende Konvention/Fixture-Standards gegossen.
- **Evidenz-/Diskrepanz-Risiko:** hoch. Parser-/Datenformat-Entscheidungen, Git-Checkout-Annahmen und Build-Artefaktpfade koennen das bestehende Demo-/Adapter-Verhalten indirekt beeinflussen.
- **Reconciliation-Aufwand:** 2 Folgeslices fuer Demo-Binding und Fehlerverifikation; spaeter ggf. Konventionsabgleich fuer Standard-JSON-Schema.
