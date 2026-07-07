# Slice slice-032: Code-Agent bindet Build/Repo-Beobachtungen

**Status:** open -> next -> in-progress -> done (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung

**Bezug:** `LH-FA-OBS-001`, `LH-FA-OBS-002`, `LH-FA-OBS-006`, `LH-QA-02`, `LH-QA-04`; `ADR-0001`,
`ADR-0003`, `LH-FA-POL-006`; `ARC-08`.

**Autor:** pt9912. **Datum:** 2026-07-07.

---

## 1. Ziel

`example/code-agent` nutzt die in `slice-031` gelieferten und in `slice-034`
konfigurationsseitig geschaerften Beobachter `observation-build-report` und
`observation-git-local` als Demo-Eingang und macht den `make example-code-agent`-
Kontrakt reproduzierbar.

## 2. Definition of Done

- [x] `example/code-agent` ersetzt seine statischen Seed-Observations durch die
  Build-/Git-Beobachtungen aus `observation-build-report` und
  `observation-git-local`; Demo-Ausgabe zeigt Quelle, Zeitstempel, `scenario`,
  `terminal` und `executed` deterministisch.
- [x] `make example-code-agent` ist ausfuehrbar spezifiziert und umgesetzt:
  `Makefile` setzt Runtime-Image-Default-Fixture-Pfade (`/app/fixtures/*.fixture`)
  ueber die image-internen Make-Variablen `CODE_AGENT_IMAGE_BUILD_FIXTURE` /
  `CODE_AGENT_IMAGE_REPO_FIXTURE` und reicht sie als Docker-Build-Args
  `CODE_AGENT_BUILD_FIXTURE` / `CODE_AGENT_REPO_FIXTURE` weiter; `Dockerfile`
  uebernimmt diese Args als Env fuer den Runtime-Entrypoint.
- [x] `make example-code-agent` erzeugt ein direkt ausfuehrbares Runtime-Image
  `$(IMAGE):example-code-agent`; `docker run --rm $(IMAGE):example-code-agent`
  startet die Demo ohne Netz und nutzt die im Image enthaltenen Default-Fixtures,
  sofern keine expliziten Fixture-Env-Overrides gesetzt sind. Diese Defaults sind
  als nicht-leere `CODE_AGENT_BUILD_FIXTURE` / `CODE_AGENT_REPO_FIXTURE` im Image
  gebacken; der normale Runtime-Pfad gilt daher nicht als `fixture_env_missing`.
- [x] Architekturregeln erlauben die neue Example-Bindung explizit:
  `.a-check.yml` enthaelt die noetigen Kanten von `example/code-agent` zu
  `observation-build-report` und `observation-git-local`, ohne produktive
  Composition-Routen (`adapters/inbound/cli`) zu erweitern.
- [x] `example/code-agent/README.md` dokumentiert Runtime-Default-Pfade, explizite Image-interne Override-Pfade,
  Offline-Annahme und die Abgrenzung zu Fehlerklassen aus `slice-033`.
- [x] `make example-code-agent`, `docker run --rm $(IMAGE):example-code-agent`,
  `make doc-check` und `make gates` laufen gruen.
- [x] Closure-Notiz mit Lerneintrag vorhanden.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `example/code-agent/src/main/kotlin/dev/beliefagent/example/codeagent/*.kt` | update | Demo-Komposition auf Build-/Repo-Beobachtungsadapter umstellen, ohne Gate-/Executor-Policy zu verschieben. |
| `example/code-agent/fixtures/*.fixture` | neu | Stabile Default-Fixtures fuer `make example-code-agent` bereitstellen; Format folgt den Adapter-Parsern aus `slice-031`/`slice-034`. |
| `example/code-agent/build.gradle.kts` | update | Dependencies auf `observation-build-report` und `observation-git-local` aufnehmen. |
| `.a-check.yml` | update | Example-zu-Adapter-Kanten fuer `example/code-agent` erlauben, ohne produktive Composition-Routen zu erweitern. |
| `Makefile` | update | Image-interne Default-Fixture-Pfade setzen, Build-Args an `docker build --target example-code-agent` weiterreichen und das Runtime-Image unter `$(IMAGE):example-code-agent` erzeugen. |
| `Dockerfile` | update | `CODE_AGENT_BUILD_FIXTURE` / `CODE_AGENT_REPO_FIXTURE` als ARG/ENV in der `example-code-agent`-Stage verfuegbar machen; Default-Fixtures ins Image kopieren und `ENTRYPOINT`/`CMD` fuer `docker run --rm $(IMAGE):example-code-agent` setzen. |
| `example/code-agent/README.md` | update | Demo-Run und Fixture-Kontrakt dokumentieren. |

## 4. Trigger

`slice-031` ist in `done/` und liefert `observation-build-report` sowie
`observation-git-local`; `slice-034` ist in `done/` und hat
`GitSourceConfig`/`GitStatusQuellenFactory` als konsumierbaren Vertrag fuer
`fixture|cli|jgit` ohne stillen Fallback festgelegt. `slice-032` nutzt fuer
die Demo-Bindung diesen Vertrag und implementiert keine eigene Git-Strategie-
Auswahl.

## 5. Closure-Trigger

DoD vollstaendig + Review abgeschlossen + `make gates` gruen + Slice nach `done/`
verschoben.

## 6. Risiken und offene Punkte

- Der Docker-Target soll weiterhin als Build-Sensor nutzbar bleiben und zusaetzlich
  ein Runtime-Image liefern; Build-Args, Image-Fixtures und `ENTRYPOINT` muessen
  deshalb denselben Default-Kontrakt verwenden.
- `fixture_env_missing` aus `slice-033` darf nicht mit den gebackenen Runtime-Defaults
  kollidieren: der positive Image-Pfad hat nicht-leere Default-ENV, der negative
  Testpfad muss diese Defaults bewusst deaktivieren oder einen separaten Runner-Modus
  nutzen.
- Fehlerklassen fuer fehlende/leere/malformed Fixtures werden erst in `slice-033`
  vollstaendig verifiziert.
- Git-Source-Strategie und CLI-/JVM-Library-Abgrenzung sind Voraussetzung aus
  `slice-034`; dieser Slice waehlt nur den Demo-Default und dokumentiert ihn.

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** `example/code-agent` nutzt jetzt konkrete Build- und
Repo-Beobachtungsadapter als Demo-Eingang. Der Build-Pfad liest
`BuildReportBeobachter` aus `CODE_AGENT_BUILD_FIXTURE`, der Repo-Pfad nutzt
`GitStatusQuellenFactory(source=fixture)` aus `CODE_AGENT_REPO_FIXTURE`; die
Demo-Ausgabe zeigt `source`, `timestamp`, `scenario`, `terminal` und
`executed`.

**Was ging anders als geplant:** Die Fixture-Dateien sind `.fixture` statt
`.json`, weil die gelieferten Adapter aus `slice-031`/`slice-034` bewusst
key-value-Fixtures als stabilen Contract verwenden. Ausserdem musste der
Make-/Docker-Vertrag auf image-interne Fixture-Pfade (`/app/fixtures/*.fixture`)
geschaerft werden; Host-Fixtures sind Runtime-Overrides per Mount und Env, keine
Build-Arg-Defaults.

**Steering-Loop:** Runtime-Image-Faehigkeit braucht einen separaten Sensor:
`make example-code-agent` beweist den Build-Run, `docker run --rm
belief-agent:example-code-agent` beweist `ENTRYPOINT` und gebackene Default-ENV.
Beide Pfade bleiben von `fixture_env_missing` aus `slice-033` abgegrenzt, weil
die positiven Defaults nicht leer im Image liegen.

**Review/Verification:** Der Abschluss ist mit
`docs/reviews/2026-07-07-slice-032-code-review.md` und
`docs/verifications/2026-07-07-slice-032-verification.md` gegen Modul 10 und
Modul 11 dokumentiert.

## 8. Sub-Area-Modus-Begründung

### Sub-Area: Beispiel-Integration (code-agent demo + Beispiele)

- **Modus:** Hybrid
- **Konventionen-Dichte:** mittel. Bestehende Demo-Composition-Patterns sind vorhanden, der neue Build-/Repo-Fixture-Kontrakt ist aber neu.
- **Phase-Reife:** Phase 4. `slice-030` hat Demo-Szenarien stabilisiert; dieser Slice ersetzt nur die Eingangsquelle fuer `example/code-agent`.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Make/Docker-Ausfuehrung kann von lokalem Run abweichen, weil die Demo im Docker-Build laeuft.
- **Reconciliation-Aufwand:** gering. `slice-033` schliesst die Fehlerklassen-/Verifikationsluecke.
