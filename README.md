# belief-agent

## Was ist belief-agent?

`belief-agent` ist ein Agenten-Framework, das **Unsicherheit explizit
modelliert**: Statt Schritt für Schritt eine angenommene Wahrheit
auszuführen, führt es eine Wahrscheinlichkeitsverteilung über konkurrierende
Hypothesen (Belief State), beschafft gezielt Information und sichert
irreversible Aktionen durch Konfidenzschwellen ab. Leitsatz: **Der Agent
weiß, wann er nicht genug weiß.**

## Was kann ich heute tun?

**Stand:** Version [`v0.1.0`](version.md#aktuell) (in Entwicklung). Der
Belief-Kern, der Entscheidungszyklus, ein netzfreier CLI-Composition-Root und
die Human-Approval-Kanäle (lokal + Remote/UI, fail-closed, mit append-only
Approval-Audit) sind lauffaehig; echte LLM-Provideradapter
(Aktionsvorschlag/Hypothesen) und dauerhafte Persistenz (Audit-DB, Konfidenz,
Uhr) bleiben Stabilisierungsarbeit.

- Lauffähiger **Belief-Kern** in `hexagon:domain` (Kotlin Multiplatform,
  HexSlice-Architektur): normierter Belief State mit Pflicht-Resthypothese,
  nicht-überschreibendes Bayes-Update, Unsicherheitsmaße, Re-Hypothesen-Auslöser.
- `make gates` läuft grün — **fünf Gates**: `doc-check` · `build` · `test` ·
  `coverage-gate` · `arch-check` (alles im Docker, kein Host-JDK).
- `make cli-demo-scenarios` zeigt deterministisch, wann der Agent nicht
  handelt: `terminal=eskaliert` und `terminal=abgelehnt` bleiben
  `executed=false`, während `sammelt-dann-handelt` erst Information sammelt
  und dann freigibt.
- `make cli-demo-approval-local` und `make cli-demo-approval-remote-ui`
  demonstrieren die Human-Approval-Kanäle; ohne Eingabe (EOF) bleiben sie
  fail-closed (`executed=false`).
- `make help` zeigt die Targets; `make build`/`make test` bauen/testen im Docker.
- Lastenheft, Spezifikation, Architektur und Roadmap sind les- und
  navigierbar (siehe unten).

## Warum belief-agent?

Coding-Agenten scheitern selten an fehlender linearer Zustandsschätzung,
sondern an **schlechter Unsicherheitsrepräsentation**. Probleme in einem
Repository sind diskret und hypothesenförmig („Bug in Auth 55 % / Frontend
25 % / Gateway 20 %"), nicht kontinuierlich, linear und gaußverteilt. An die
Stelle eines Kalman-Zustands tritt daher ein **Belief State über Hypothesen**.

Mit „Kalman-Zustand" ist hier bewusst die Klasse kontinuierlicher,
linear-gaußscher Schätzverfahren gemeint: ein Zustand wird als Vektor mit
Unsicherheit fortgeschrieben, typischerweise für Messreihen wie Position,
Geschwindigkeit oder Sensorwerte. `belief-agent` modelliert Repository- und
Agentenprobleme stattdessen als konkurrierende diskrete Erklärungen mit
Wahrscheinlichkeiten. Der Belief State ist deshalb eine normierte Verteilung
über Hypothesen plus Pflicht-Resthypothese `other`; Beobachtungen gewichten
diese Hypothesen bayesianisch neu. Diese Abgrenzung ist ein expliziter
Out-of-Scope-Vertrag: kein Kalmanfilter, sondern diskreter Hypothesen-Belief
([`LH-OUT-02`](spec/lastenheft.md#lh-out-02--kein-kalmanfilter); Glossar:
[`Belief State`](spec/lastenheft.md#4-begriffe-glossar)).

## Kerngedanke

Das Sprachmodell ist **nicht der Agent**, sondern ein austauschbares Modul.
Seine implizite Konfidenz wird in explizite, prüfbare und gate-fähige Zahlen
überführt; erst wenn die Konfidenz die Risikoklasse der Aktion deckt, wird
gehandelt — andernfalls wird Information gesammelt oder eskaliert.

```sh
make cli-demo-scenarios
```

Der Demo-Target laeuft ohne Netzwerk gegen deterministische Adapter und gibt
unter anderem aus:

```text
scenario=eskaliert
approval=fake
terminal=eskaliert
executed=false
reason=GateEskalation
executor_boundary=closed
```

## Was macht es vertrauenswürdig?

- **Prozess:** [`AGENTS.md`](AGENTS.md) (Hard Rules),
  [`harness/README.md`](harness/README.md) (Source Precedence, Gates).
- **Verträge:** [`spec/lastenheft.md`](spec/lastenheft.md) (`LH-*`-IDs mit
  Akzeptanzkriterien), [`spec/spezifikation.md`](spec/spezifikation.md),
  [`spec/architecture.md`](spec/architecture.md).
- **Gates:** `make gates` — `doc-check` (d-check), `build`/`test`/`coverage-gate`
  (Kotlin/Kover im Docker), `arch-check` (a-check, HexSlice-Reinheit); alle
  reproduzierbar und digest-gepinnt. Versionen: [`version.md`](version.md).
- **Auditierbarkeit:** Entscheidungen in [`docs/plan/adr/`](docs/plan/adr/),
  Planung in [`docs/plan/planning/`](docs/plan/planning/).

## Lizenz

MIT — siehe [`LICENSE`](LICENSE).
