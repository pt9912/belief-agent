# Harness-Konventionen — belief-agent

## Purpose

Diese Datei deklariert die *repo-lokalen* Strukturregeln von `belief-agent`
gegenüber der adoptierten Harnesskonvention (Baseline). Sie ist der
Default-Ort für:

- **Adaptionen** ggü. der Baseline (mit Begründung und Auflösungs-Trigger).
- **ID-Schema-Deklaration** — welches Präfix-Schema dieses Repo nutzt.
- **Zusatzklassen-Deklarationen** für repo-spezifische Bindung-Klassen in
  der Sensors-Tabelle, die über die vier kanonischen hinausgehen.
- **Modus-Deklarationen** pro Sub-Area (Greenfield / Brownfield / Hybrid).

Bei Konflikt zwischen dieser Datei und einer kanonischen Quelle gilt die
kanonische Quelle (Source Precedence). Diese Datei ist konformitäts-
bringend für *Form*-Fragen, nicht autoritativ über Inhalt.

## Baseline

- **Konvention:** AI-Harness-Kurs (`pt9912/ai-harness-course`)
- **Stand:** v1.4.0 (Regelwerk- und Template-Set)
- **Datum der Adoption:** 2026-06-22 (initial v1.3.0); Bump v1.3.0 → v1.4.0 am
  2026-07-05 (`MR-007`).

## Adoptierte Konventions-Quellen

- **Extern (Lehrmaterial):** <https://github.com/pt9912/ai-harness-course/tree/v1.4.0/kurs/de>
- **Regelwerk (committet vendored, `MR-007`):** die Lese-Form ist das nach
  Modulen aufgeteilte Bundle, entpackt und committet unter
  `.harness/baseline/v1.4.0/regelwerk/` (Index `regelwerk/README.md`), samt
  `.harness/baseline/v1.4.0/SHA256SUMS`-Integritätsmanifest — netzlos auf jedem
  Checkout, offline verifizierbar per `tools/harness/fetch-baseline-cache.sh`
  (`--verify`). Bundle-Quelle: Release-Asset `lab-regelwerk.zip`, Tag `v1.4.0`.
- **In-Repo (verkörperte Form):** die wiederkehrenden Templates
  (`docs/plan/adr/NNNN-titel.template.md`, `docs/plan/planning/slice.template.md`,
  `docs/plan/planning/welle.template.md`, `docs/plan/carveouts/carveout.template.md`,
  `docs/reviews/review-report.template.md`) sowie die Gate-Baseline
  (`.d-check.yml`, `d-check.mk`, `Makefile`). Die Templates bleiben **co-located**
  und sind Autorenquelle — Upstream-MR-018 („keine co-located Templates", nur
  für Producer/Self-Hoster) wird bewusst **nicht** adoptiert (`MR-008`).

## Adaptions-Block

### MR-000 — Baseline-Aussage

- **Datum:** 2026-06-22
- **Geltungsbereich:** gesamtes Repo
- **Adaption:** *keine inhaltlichen Adaptionen ggü. Baseline-Default für
  Verzeichniskonvention, Lifecycle-Regeln (`open` → `next` → `in-progress`
  → `done`), Carveout-Disziplin und die ADR-/Carveout-/Slice-/MR-ID-Schemata
  (`ADR-<NNNN>`, `CO-<NNN>`, `slice-<NNN>`, `MR-<NNN>`).* Abweichungen vom
  Lastenheft-Default-Präfix und von der Source Precedence sind als eigene
  `MR-<NNN>` unten dokumentiert.
- **Begründung:** Initial-Setzung. Spätere Adaptionen werden als
  `MR-<NNN>` nachgetragen.
- **Auflösungs-Trigger:** permanent.

### MR-001 — Source Precedence mit eigener Spezifikations-Schicht

- **Datum:** 2026-06-22
- **Geltungsbereich:** `harness/README.md` §Source precedence
- **Adaption:** Die Source-Precedence-Tabelle führt `spec/spezifikation.md`
  als eigenen **Rang 2** zwischen Lastenheft (Rang 1) und Architektur
  (Rang 3). Der Kurs-Default setzt zwei Spec-Ränge; dieses Repo nutzt drei.
- **Begründung:** `belief-agent` nutzt die Spec-Stratifizierung explizit mit
  drei Spec-Dateien. Damit die ADR-Schärfungs-Regel ("ADR darf
  Spezifikation schärfen, nicht Lastenheft") strukturell sichtbar ist, muss
  die Spezifikation als eigener Rang erscheinen.
- **Auflösungs-Trigger:** permanent.

### MR-002 — Constraint-/Non-Goal-/Open-Point-Familien zusätzlich zu FA/QA

- **Datum:** 2026-06-22
- **Geltungsbereich:** `spec/lastenheft.md`, alle Referenzen auf
  Lastenheft-IDs.
- **Adaption:** Das Lastenheft folgt dem kanonischen Schema
  `LH-FA-<BEREICH>-<NNN>` (funktionale Anforderungen; Bereiche `BEL`, `OBS`,
  `ACT`, `POL`, `VOI`, `ESK`, `AUD`, `LLM`) und `LH-QA-<NN>`
  (Qualitätsanforderungen) — damit RTM (`--trace`), `ids`-Auto-Ableitung und
  `suggest-config` nativ greifen. **Zusätzlich** führt es drei Familien, die
  der Baseline-Template-Default nicht kennt: `LH-RB-<NN>`
  (Randbedingungen/Annahmen), `LH-OUT-<NN>` (Nicht-Ziele), `LH-OP-<NN>`
  (offene Punkte). Diese tragen **keine** FA/QA-Gestalt und sind damit
  bewusst nicht RTM-relevant (Constraints/Abgrenzungen/TODOs, keine
  slice-belegbaren Anforderungen). Verfeinerungen einzelner FA-IDs in der
  Spezifikation tragen `LH-FA-<BEREICH>-<NNN>.<Buchstabe>`; Architektur-/
  Spezifikations-Struktur-IDs nutzen `ARC-<NN>` bzw. `SPEC-<NN>`.
- **Begründung:** FA/QA-Ausrichtung folgt dem Kurs-Standard und macht den
  RTM-/Konfigurations-Werkzeugkasten von d-check nutzbar. Die drei
  Zusatzfamilien halten Randbedingungen, Nicht-Ziele und offene Punkte als
  stabile, anker-bare IDs, ohne sie als Anforderungen zu zählen.
- **Auflösungs-Trigger:** permanent.

### MR-003 — Repo-Klasse Safety/Control

- **Datum:** 2026-06-22
- **Geltungsbereich:** gesamtes Repo (Hard Rules in `AGENTS.md`,
  Safety-Sektion in `harness/README.md`).
- **Adaption:** `belief-agent` wird als **Safety/Control**-Repo geführt,
  nicht als reines Referenz/Tooling-Repo. Die Sicherheitsfunktion ist
  nicht Hardware, sondern das **Konfidenz-Gate**: irreversible
  (extern-wirksame) Aktionen sind nur nach harter Schwelle *und*
  menschlicher Freigabe zulässig (`LH-FA-POL-004`, `LH-FA-POL-005`, `LH-OUT-04`).
  Hard Rules werden entsprechend scharf gesetzt (Gate nicht umgehbar,
  fail-safe-Default, kein extern-wirksames Handeln bei hoher
  Resthypothese).
- **Begründung:** Der gesamte Zweck des Frameworks ist das Absichern
  irreversibler Aktionen durch Konfidenz. Eine Referenz-Klasse würde die
  Hard Rules zu weich setzen; die fachliche Domäne ist sicherheitsgerichtet.
- **Auflösungs-Trigger:** permanent.

### MR-004 — d-check-Gate auf aktueller Version, Config generiert

- **Datum:** 2026-06-22
- **Geltungsbereich:** `.d-check.yml`, `d-check.mk`.
- **Adaption:** Statt des im Template gepinnten d-check `v0.8.0` wird
  aktuell **`v0.37.1`** adoptiert. `d-check.mk` ist tool-generiert
  (`d-check --print-mk`); der Digest-Pin lebt als `DCHECK_DIGEST`
  (`sha256:3bbdb19b…`) im `Makefile` und sticht den Tag.
  Die `.d-check.yml` wird mit `d-check --suggest-config ai-harness`
  (repo-bewusst) generiert statt von Hand. Das `ids`-Modul nutzt für die
  Lastenheft-Anforderungen die kanonische Regex `LH-(FA-[A-Z]+|QA)-\d+`
  (RTM-/suggest-config-kompatibel, `MR-002`); `link-policy: prose`
  (Inline-Code-IDs bleiben befreit, Prosa-IDs sind linkpflichtig). Die
  Constraint-/Non-Goal-/Open-Point-IDs (`LH-RB`/`LH-OUT`/`LH-OP`) tragen
  bewusst keine Linkpflicht.
- **Begründung:** Die aktuelle d-check-Version bringt eigene Generatoren
  (`--print-config`, `--print-mk`, `--suggest-config ai-harness`) und das
  offline-Gate (`--network none`) mit; die Config soll werkzeug-generiert
  und damit driftarm bleiben. Die LH-Regex-Anpassung folgt zwingend aus
  `MR-002`.
- **Auflösungs-Trigger:** Re-Pin bei d-check-Upgrade (`d-check.mk` neu via
  `--print-mk`, neuer `DCHECK_DIGEST` im `Makefile`); ansonsten permanent.

### MR-005 — a-check-Arch-Gate (v0.10.0, HexSlice-Rollen)

- **Datum:** 2026-07-04
- **Geltungsbereich:** `.a-check.yml`, `a-check.mk`, `Makefile` (`arch-check`).
- **Adaption:** `make arch-check` läuft über **a-check v0.10.0**
  (`ghcr.io/pt9912/a-check`, digest-gepinnt `sha256:0932cb1d…`). `a-check.mk`
  ist tool-generiert (`--print-mk`); dessen Default-Digest laggt (v0.9.0),
  daher lebt der Pin als `A_CHECK_IMAGE` im `Makefile` und sticht.
  `.a-check.yml` bildet die HexSlice-Rollen ab (domain/application/adapters,
  Kanten nach innen) plus `tech`-Regeln (Framework/DI nur am Adapter-Rand).
- **Begründung:** a-check ist das für HexSlice gebaute Arch-Gate (`ADR-0003`).
  v0.10.0 bringt den fail-closed-Guard gegen mehrdeutige Mehr-Wurzel-Auflösung
  (der von uns gemeldete KMP-Falsch-negativ) — löste `CO-001` auf.
- **Auflösungs-Trigger:** Re-Pin bei a-check-Upgrade; `resolution` beim
  Multi-Modul-Ausbau (welle-02) paket-spezifisch erweitern (der Guard erzwingt
  Korrektheit).

### MR-006 — d-check-Modul-Auswahl erweitert (v0.37.1)

- **Datum:** 2026-07-04
- **Geltungsbereich:** `.d-check.yml`, `d-check.mk`-Targets.
- **Adaption:** Zusätzlich zu `links/anchors/ids/matrix/codepaths` sind im
  hermetischen `make doc-check` aktiv: **spans** (offene Code-Spans /
  verschachtelte Links), **hostpaths** (Host-Pfad-Leak; `prefixes`
  `home/Users/Development`), **tracked** (Link-Ziele git-getrackt),
  **planning** (Roadmap-↔-Slice-Lifecycle: Ruhe-Marker „Keine aktive Welle"
  gdw. kein `slice-*` in `in-progress/`). Als **git-Range-Targets** (nicht im
  Default-Gate, brauchen `RANGE=base..head`): **vcs** (`make doc-immutable` —
  Accepted-ADRs immutabel, maschinelle Durchsetzung von Hard Rule 3.5) und
  **commits** (`make doc-commits` — Traceability-Kennung je Commit:
  `LH`/`ADR`/`MR`/`CO`/`slice`).
- **Begründung:** Analog zur d-check-Eigen-Config (Dogfooding-Referenz). Nicht
  übernommen: `versions` (keine `version.md`), `diagrams`/`immutable`/`pins`/
  `external`.
- **Auflösungs-Trigger:** permanent; Modulwahl bei d-check-Upgrade
  re-evaluieren.

### MR-007 — Regelwerk-Lese-Form committet vendored + Baseline-Bump v1.4.0

- **Datum:** 2026-07-05
- **Geltungsbereich:** `.harness/baseline/`, `tools/harness/fetch-baseline-cache.sh`,
  `AGENTS.md` §1, `harness/README.md` §Guides, `.d-check.yml` (`scan.ignore`),
  `.gitignore`, §Baseline oben.
- **Adaption:** Die Lese-Form des adoptierten Regelwerks wechselt von „pro
  Session das Remote-ZIP holen" auf **committet vendored**:
  `.harness/baseline/v1.4.0/regelwerk/` (+ `SHA256SUMS`-Integritätsmanifest),
  netzlos auf jedem Checkout präsent und offline verifizierbar
  (`tools/harness/fetch-baseline-cache.sh --verify`). Zugleich **Bump des
  adoptierten Stands v1.3.0 → v1.4.0** (das für beide Adopter geltende
  Regelwerk; belief-agent zieht nach). Realisiert als eigene belief-agent-
  Adaption nach dem Muster der Referenz d-check (dortiges `MR-019`) — nicht
  importiert.
- **Begründung:** `AGENTS.md` §1 verlangt, das aufgaben-relevante Regelwerk-
  Modul **einmal pro Session zu lesen** — das setzt einen aus dem Checkout
  lesbaren, netzlosen, integritäts-verifizierten Bestand voraus (konsistent mit
  der digest-gepinnten, netzlosen Tool-Haltung, `MR-004`/`MR-006`). Die
  Remote-ZIP-Form erfüllte das nicht.
- **Reviewter Umfang / offener Punkt:** Dieser MR deckt **Lese-Form** und
  **Versions-Pin** ab. Der v1.3.0 → v1.4.0-Delta-Scan bestätigte, dass die
  Template-Kopier-Vorgabe in v1.4.0 fortbesteht (`modul-02` Bootstrap) → kein
  Konflikt mit den co-located Templates (`MR-008`). **Offen** (eigener
  Trigger): vollständige v1.4.0-Konformitäts-Durchsicht aller Module — v. a.
  die Lifecycle-Disziplin (Modul 5 WIP=1 / Lerneintrag je Slice; „delivered
  bleibt in `in-progress/` bis Welle-Closure" ist eine noch undokumentierte
  Abweichung).
- **Auflösungs-Trigger:** Lese-Form permanent; Versions-Pin beim nächsten
  Baseline-Bump nachziehen; Konformitäts-Durchsicht als eigener Slice/MR.

### MR-008 — Upstream-MR-018 („keine co-located Templates") nicht adoptiert

- **Datum:** 2026-07-05
- **Geltungsbereich:** Template-Haltung; §Adoptierte Konventions-Quellen,
  `tools/harness/fetch-baseline-cache.sh`.
- **Adaption:** Die Referenz d-check verzichtet auf co-located Templates
  (dortiges `MR-018`), **weil d-check Producer/Self-Hoster des Kurs-/Template-
  Materials ist**. belief-agent ist **Consumer**, nicht Producer; damit gilt
  der Baseline-**Default**: Skelett-Vorlagen liegen co-located im Repo und sind
  Autorenquelle (beim Anlegen neuer Artefakte kopieren, nicht frei
  formulieren). v1.4.0 schreibt diese Kopier-Vorgabe selbst fort (`modul-02`
  Bootstrap-Sequenz). Das fetch-Skript stagt daher **kein** `lab-templates.zip`.
- **Begründung:** MR-018 ist eine rollen-spezifische Producer-Adaption, kein
  Regelwerk-Stand-Fortschritt. Übernahme wäre ein Fehl-Import; die bewusste
  Nicht-Adoption wird dokumentiert, damit sie nicht als Versäumnis erscheint.
- **Auflösungs-Trigger:** permanent, solange belief-agent Consumer (nicht
  Producer) der Baseline ist.

## Zusatzklassen-Deklaration für Sensors-Bindung

Über die vier kanonischen Bindung-Klassen (ADR, Carveout, Schwelle,
Reproduzierbarkeit) hinaus nutzt dieses Repo:

| Klasse | Form | Bedeutung | Beispiel |
|---|---|---|---|
| Anforderungs-Bindung | `LH-FA-<BEREICH>-<NNN>` / `LH-QA-<NN>` | Gate prüft eine bestimmte Lastenheft-Anforderung direkt | `LH-FA-BEL-002` für ein Normierungs-Gate (Σp = 1) |
| Modell-Version-Bindung | `Modell <name>@<version>` | Replay-/Eval-Gate hängt an einer fixierten LLM-Port-Modellversion | `LH-FA-LLM`-Evals gegen Golden Set |

## Modus-Deklaration pro Sub-Area

| Sub-Area (Pfad / Modul) | Modus | Begründung | Graduation-Bedingung / Folge-Slice |
|---|---|---|---|
| `*` (Default für gesamtes Repo) | Greenfield | Frisches Repo ohne Bestandscode; Doku führt, Code folgt. | n/a (GF) |
| `spec/`, `harness/`, `docs/plan/` (Konventionen, Spec, Architektur, ADR, Planung) | Greenfield | Beim Bootstrap zuerst angelegte Doku-Sub-Areas; alle vier Doku-führt. | n/a (GF) |

> Künftige Code-Sub-Areas (Belief-Kern, Evidenz/Update, Aktionen/Gates,
> VoI, Eskalation, Audit/Event-Log, LLM-Port) werden bei ihrer Anlage als
> GF geführt, solange die Spec sie vor dem Code beschreibt. Ein Wechsel
> nach BF (z. B. übernommener Bestandscode) bekäme eine eigene
> Modus-Aussage mit Graduation-Plan.

## Glossar (optional)

Repo-spezifische Begriffe stehen vollständig im Lastenheft (`spec/lastenheft.md`
§4); hier keine Wiederholung.
