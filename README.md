# belief-agent

## Was ist belief-agent?

`belief-agent` ist ein Agenten-Framework, das **Unsicherheit explizit
modelliert**: Statt Schritt für Schritt eine angenommene Wahrheit
auszuführen, führt es eine Wahrscheinlichkeitsverteilung über konkurrierende
Hypothesen (Belief State), beschafft gezielt Information und sichert
irreversible Aktionen durch Konfidenzschwellen ab. Leitsatz: **Der Agent
weiß, wann er nicht genug weiß.**

## Was kann ich heute tun?

Das Repository steht am **Ende des Harness-Bootstraps** (Phase 01): Spec,
Architektur, ADR-Fundament und Planungs-Harness stehen; Code folgt ab
Welle 1.

- `make gates` läuft grün (Doku-Referenz-Gate: `make doc-check` via d-check).
- `make help` zeigt die verfügbaren Targets.
- Lastenheft, Spezifikation, Architektur und Roadmap sind les- und
  navigierbar (siehe unten).

## Warum belief-agent?

Coding-Agenten scheitern selten an fehlender linearer Zustandsschätzung,
sondern an **schlechter Unsicherheitsrepräsentation**. Probleme in einem
Repository sind diskret und hypothesenförmig („Bug in Auth 55 % / Frontend
25 % / Gateway 20 %"), nicht kontinuierlich-gaußsch. An die Stelle eines
Kalman-Zustands tritt daher ein **Belief State über Hypothesen**.

## Kerngedanke

Das Sprachmodell ist **nicht der Agent**, sondern ein austauschbares Modul.
Seine implizite Konfidenz wird in explizite, prüfbare und gate-fähige Zahlen
überführt; erst wenn die Konfidenz die Risikoklasse der Aktion deckt, wird
gehandelt — andernfalls wird Information gesammelt oder eskaliert.

## Was macht es vertrauenswürdig?

- **Prozess:** [`AGENTS.md`](AGENTS.md) (Hard Rules),
  [`harness/README.md`](harness/README.md) (Source Precedence, Gates).
- **Verträge:** [`spec/lastenheft.md`](spec/lastenheft.md) (`LH-*`-IDs mit
  Akzeptanzkriterien), [`spec/spezifikation.md`](spec/spezifikation.md),
  [`spec/architecture.md`](spec/architecture.md).
- **Gates:** `make doc-check` (Doku-Referenzen via d-check, Image
  digest-gepinnt). Code-Gates wachsen mit dem Code.
- **Auditierbarkeit:** Entscheidungen in [`docs/plan/adr/`](docs/plan/adr/),
  Planung in [`docs/plan/planning/`](docs/plan/planning/).
