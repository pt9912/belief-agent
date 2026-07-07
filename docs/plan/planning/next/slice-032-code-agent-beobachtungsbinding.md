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

- [ ] `example/code-agent` ersetzt seine statischen Seed-Observations durch die
  Build-/Git-Beobachtungen aus `observation-build-report` und
  `observation-git-local`; Demo-Ausgabe zeigt Quelle, Zeitstempel, `scenario`,
  `terminal` und `executed` deterministisch.
- [ ] `make example-code-agent` ist ausfuehrbar spezifiziert und umgesetzt:
  `Makefile` setzt repo-relative Default-Fixture-Pfade und reicht
  `CODE_AGENT_BUILD_FIXTURE` / `CODE_AGENT_REPO_FIXTURE` als Docker-Build-Args weiter;
  `Dockerfile` uebernimmt diese Args als Env fuer `:example:code-agent:run`.
- [ ] `make example-code-agent` erzeugt ein direkt ausfuehrbares Runtime-Image
  `$(IMAGE):example-code-agent`; `docker run --rm $(IMAGE):example-code-agent`
  startet die Demo ohne Netz und nutzt die im Image enthaltenen Default-Fixtures,
  sofern keine expliziten Fixture-Env-Overrides gesetzt sind. Diese Defaults sind
  als nicht-leere `CODE_AGENT_BUILD_FIXTURE` / `CODE_AGENT_REPO_FIXTURE` im Image
  gebacken; der normale Runtime-Pfad gilt daher nicht als `fixture_env_missing`.
- [ ] Architekturregeln erlauben die neue Example-Bindung explizit:
  `.a-check.yml` enthaelt die noetigen Kanten von `example/code-agent` zu
  `observation-build-report` und `observation-git-local`, ohne produktive
  Composition-Routen (`adapters/inbound/cli`) zu erweitern.
- [ ] `example/code-agent/README.md` dokumentiert Default-Pfade, explizite Override-Pfade,
  Offline-Annahme und die Abgrenzung zu Fehlerklassen aus `slice-033`.
- [ ] `make example-code-agent`, `docker run --rm $(IMAGE):example-code-agent`,
  `make doc-check` und `make gates` laufen gruen.
- [ ] Closure-Notiz mit Lerneintrag vorhanden.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `example/code-agent/src/main/kotlin/dev/beliefagent/example/codeagent/*.kt` | update | Demo-Komposition auf Build-/Repo-Beobachtungsadapter umstellen, ohne Gate-/Executor-Policy zu verschieben. |
| `example/code-agent/fixtures/*.json` | neu | Stabile Default-Fixtures fuer `make example-code-agent` bereitstellen. |
| `example/code-agent/build.gradle.kts` | update | Dependencies auf `observation-build-report` und `observation-git-local` aufnehmen. |
| `.a-check.yml` | update | Example-zu-Adapter-Kanten fuer `example/code-agent` erlauben, ohne produktive Composition-Routen zu erweitern. |
| `Makefile` | update | Default-Fixture-Pfade setzen, Build-Args an `docker build --target example-code-agent` weiterreichen und das Runtime-Image unter `$(IMAGE):example-code-agent` erzeugen. |
| `Dockerfile` | update | `CODE_AGENT_BUILD_FIXTURE` / `CODE_AGENT_REPO_FIXTURE` als ARG/ENV in der `example-code-agent`-Stage verfuegbar machen; Default-Fixtures ins Image kopieren und `ENTRYPOINT`/`CMD` fuer `docker run --rm $(IMAGE):example-code-agent` setzen. |
| `example/code-agent/README.md` | update | Demo-Run und Fixture-Kontrakt dokumentieren. |

## 4. Trigger

`slice-031` ist in `done/` und liefert `observation-build-report` sowie
`observation-git-local`; `slice-034` ist in `done/` und hat die Git-Source-
Strategie fuer Fixture/CLI/JVM-Library ohne stillen Fallback festgelegt.

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

**Wird nach `done/` ergaenzt.**

## 8. Sub-Area-Modus-Begründung

### Sub-Area: Beispiel-Integration (code-agent demo + Beispiele)

- **Modus:** Hybrid
- **Konventionen-Dichte:** mittel. Bestehende Demo-Composition-Patterns sind vorhanden, der neue Build-/Repo-Fixture-Kontrakt ist aber neu.
- **Phase-Reife:** Phase 4. `slice-030` hat Demo-Szenarien stabilisiert; dieser Slice ersetzt nur die Eingangsquelle fuer `example/code-agent`.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Make/Docker-Ausfuehrung kann von lokalem Run abweichen, weil die Demo im Docker-Build laeuft.
- **Reconciliation-Aufwand:** gering. `slice-033` schliesst die Fehlerklassen-/Verifikationsluecke.
