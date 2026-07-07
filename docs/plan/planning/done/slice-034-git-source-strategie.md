# Slice slice-034: Git-Source-Strategie fuer Beobachtungsadapter

**Status:** open -> next -> in-progress -> done (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung

**Bezug:** `LH-FA-OBS-001`, `LH-FA-OBS-006`, `LH-QA-02`, `LH-QA-03`, `LH-QA-04`;
`ADR-0001`, `ADR-0002`, `ADR-0003`; `ARC-08`.

**Autor:** pt9912. **Datum:** 2026-07-07.

---

## 1. Ziel

`observation-git-local` macht die Git-Datenquelle explizit konfigurierbar:
Replay-Fixture, lokale Git-CLI und eine gepinnte JVM-Git-Library sind getrennte
Strategien hinter derselben `GitStatusQuelle`-Boundary, ohne stillen Fallback.

## 2. Definition of Done

- [x] `observation-git-local` bietet eine explizite Source-Auswahl
  `fixture|cli|jgit`: jede Strategie erzeugt denselben `GitStatusSnapshot`-
  Vertrag fuer HEAD, Branch, Dirty-Status und geaenderte Dateien; unbekannte
  Modi und fehlende Voraussetzungen enden fail-closed.
- [x] Die oeffentliche Konfigurationsflaeche ist festgelegt und getestet:
  `GitSourceConfig` + Factory (oder aequivalenter benannter Contract) definiert
  `source=fixture|cli|jgit`, Default `fixture`, Pflichtparameter pro Modus
  (`fixturePath`, `repoRoot`, optional `gitBinary`) und die Fehlerform fuer
  ungueltige/fehlende Parameter. `slice-032` kann diesen Contract ohne eigene
  Git-Strategie-Logik konsumieren.
- [x] Runtime-/Dependency-Vertrag ist reproduzierbar: die JVM-Git-Library ist
  in `build.gradle.kts` versionsgepinnt; der CLI-Pfad prueft die lokale
  `git --version` und macht sie ueber einen festgelegten Diagnosekanal sichtbar
  (`GitStatusSnapshot`-Metadaten, Beobachtungs-Evidenz oder separates
  Diagnose-Ergebnis mit Testassertion). Der Contract dokumentiert, dass dieser
  Pfad ein installiertes Git-Tool im Image/Host voraussetzt.
- [x] Tests decken Moduswahl, keinen stillen Fallback, CLI-unavailable,
  JGit-Erfolgspfad, Fixture-Erfolgspfad und einen gemeinsamen Contract-Satz fuer
  clean, dirty, detached HEAD und rename ab; `make build`, `make test`,
  `make coverage-gate`, `make arch-check` und `make gates` bleiben gruen.
- [x] Closure-Notiz mit Lerneintrag vorhanden.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `adapters/outbound/observation-git-local/src/main/kotlin/**` | update | `GitSourceConfig`/Factory, Diagnosekanal, Source-Auswahl und zweite Live-Strategie hinter `GitStatusQuelle` ergaenzen, ohne `BeobachtungsPort` zu aendern. |
| `adapters/outbound/observation-git-local/build.gradle.kts` | update | JVM-Git-Library als explizit gepinnte Adapter-Abhaengigkeit aufnehmen. |
| `adapters/outbound/observation-git-local/src/test/**` | update | Config-Contract, Moduswahl, fehlende CLI, Diagnosekanal sowie JGit-/CLI-/Fixture-Paritaet deterministisch pruefen. |
| `docs/plan/planning/next/slice-032-code-agent-beobachtungsbinding.md` | update | Binding-Trigger auf den geschaerften Git-Source-Vertrag setzen. |
| `docs/plan/planning/in-progress/roadmap.md` | update | Follow-up als priorisierten Welle-05-Schnitt sichtbar machen. |

## 4. Trigger

`slice-031` ist in `done/` und hat `observation-git-local` geliefert; Review/
Planung hat die offene Runtime-Abhaengigkeit des CLI-Pfads auf ein installiertes
Git-Tool als separaten, priorisierten Follow-up identifiziert.

## 5. Closure-Trigger

DoD vollstaendig + Review abgeschlossen + `make gates` gruen + `slice-032`
bleibt mit Trigger auf `slice-034 done` nachvollziehbar + Slice nach `done/`
verschoben.

## 6. Risiken und offene Punkte

- JGit und Git-CLI koennen bei Status-/Rename-/Worktree-Details divergieren;
  der Slice beschraenkt den Vertrag deshalb auf den bereits gelieferten
  `GitStatusSnapshot`-Minimalumfang.
- Der CLI-Pfad bleibt absichtlich als Option erhalten, weil er den realen
  lokalen Checkout inklusive Git-Tool-Verhalten beobachtet; er darf aber nicht
  unbemerkt Default werden, wenn die Runtime-Abhaengigkeit fehlt.
- Default-Entscheidung fuer diesen Slice: `fixture` ist der einzige Default,
  weil er netz- und toolfrei reproduzierbar ist; `cli` und `jgit` sind opt-in.
- Neue Fehlerklassen fuer den Demo-Pfad werden erst in `slice-033` in die
  Negativfallmatrix aufgenommen, nachdem `slice-032` den Binding-Vertrag nutzt.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** Der vorhandene `GitStatusQuelle`-Port war die richtige
Schnittkante: `GitSourceConfig` und `GitStatusQuellenFactory` konnten
Fixture, CLI und JGit hinter demselben `GitStatusSnapshot`-Vertrag kapseln,
ohne `BeobachtungsPort` oder den Kern zu veraendern. Der Default bleibt
`fixture`; `cli` und `jgit` sind explizite Opt-ins.

**Was ging anders als geplant:** Die JGit-Rename-Paritaet brauchte einen
expliziten Diff-Pfad mit Rename-Detection, weil der reine JGit-Status sonst
Delete/Add statt `newPath` liefern kann. Der Slice haelt deshalb den
Minimalvertrag bewusst klein: HEAD, Branch, Dirty-Status und geaenderte
Dateien.

**Steering-Loop:** Runtime-Abhaengigkeiten brauchen im Adapter selbst einen
sichtbaren Diagnosekanal. Der CLI-Pfad prueft `git --version` und traegt die
Version in `GitStatusDiagnose`; JGit ist als gepinnte Gradle-Dependency
reproduzierbar.

**Review/Verification:** Der Abschluss ist mit
`docs/reviews/2026-07-07-slice-034-code-review.md` und
`docs/verifications/2026-07-07-slice-034-verification.md` gegen Modul 10 und
Modul 11 dokumentiert.

## 8. Sub-Area-Modus-Begründung

### Sub-Area: Outbound-Adapter-Ports (`adapters/outbound`, Git-Quelle)

- **Modus:** Hybrid
- **Konventionen-Dichte:** mittel. `ARC-08` und `BeobachtungsPort` sind etabliert; die konkrete Strategie-Auswahl zwischen Fixture, Git-CLI und JVM-Library ist noch kein stabiler Repo-Standard.
- **Phase-Reife:** Phase 2-3. `slice-031` hat den ersten realen Git-Beobachter geliefert, aber die Runtime-Abhaengigkeit des CLI-Pfads wurde erst im Review als Vertragsluecke sichtbar.
- **Evidenz-/Diskrepanz-Risiko:** mittel. CLI und JVM-Library koennen dieselbe Arbeitskopie unterschiedlich interpretieren; die Tests muessen den gemeinsamen Minimalvertrag absichern.
- **Reconciliation-Aufwand:** gering bis mittel. Abschluss dieses Slice setzt den Git-Source-Vertrag fuer `slice-032`; Fehlerklassen werden in `slice-033` verifiziert.

### Sub-Area: Build-/Runtime-Reproduzierbarkeit (Docker/Gradle)

- **Modus:** Hybrid
- **Konventionen-Dichte:** mittel. Docker-only Gates und gepinnte Toolchain sind ueber `ADR-0002`/Harness geregelt; die Entscheidung "OS-Git-Tool vs. gepinnte JVM-Lib" ist fuer diesen Adapter neu.
- **Phase-Reife:** Phase 3. Der Build ist reproduzierbar, aber die Git-CLI als Runtime-Voraussetzung ist noch nicht als expliziter Sensor-/Dokumentationsvertrag verankert.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Ein gruener JVM-Build kann einen spaeteren CLI-Lauf ohne installiertes Git nicht absichern.
- **Reconciliation-Aufwand:** gering. Dieser Slice dokumentiert und testet die Strategie-Auswahl; ein spaeterer CLI-Image-Pfad kann bei Bedarf mit eigenem Sensor nachgezogen werden.
