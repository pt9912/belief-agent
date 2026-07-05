# ADR-0005: Konfidenz-Gate — Default-Schwellwerte je Wirkungsklasse + Resthypothese-Sperre

**Status:** Accepted

**Datum:** 2026-07-05

**Autor:** belief-agent

**Bezug:** [`LH-FA-POL-003`](../../../spec/lastenheft.md#lh-fa-pol-003--schwellen-je-wirkungsklasse), [`LH-FA-POL-005`](../../../spec/lastenheft.md#lh-fa-pol-005--sperre-extern-wirksamer-aktionen-bei-hoher-resthypothese), [`LH-FA-POL-007`](../../../spec/lastenheft.md#lh-fa-pol-007--konfigurierbare-schwellwerte), [`LH-QA-03`](../../../spec/lastenheft.md#lh-qa-03--testbarkeit)

**Schärft:** — (Policy-/Gate-Entscheidung; die Schwellen leben als
`GateSchwellen`-Defaults im Domänen-Kern und sind konfigurierbar, `LH-FA-POL-007`.)

---

## Kontext

Das Konfidenz-Gate (`ARC-03`) gibt eine Aktion frei, lehnt sie ab
oder eskaliert — geprüft gegen die **Erfolgswahrscheinlichkeit der Aktion**
(`LH-FA-POL-002`) und eine **wirkungsklassen-abhängige Mindest-Konfidenz**
(`LH-FA-POL-003`). Für irreversible (extern-wirksame) Aktionen gilt zusätzlich
eine **Sperre bei hoher Resthypothese** (`LH-FA-POL-005`). Diese Schwellwerte
sind die Kern-Parameter der **Sicherheitsfunktion** (`MR-003`) — sie sind
ADR-pflichtig (Regelwerk Modul 13, analog `ADR-0004`): begründet, terminiert,
nicht als PR-Kommentar.

## Entscheidung

Wir setzen folgende **Default-Schwellen** (konfigurierbar über `GateSchwellen`,
`LH-FA-POL-007`) — Mindest-Erfolgswahrscheinlichkeit für die Freigabe, steigend
mit der Seiteneffekt-Reichweite:

| Wirkungsklasse | Mindest-Erfolgswahrscheinlichkeit |
|---|---|
| nur-lesend | **0,0** (keine wirksame Schwelle; Gate wird dennoch durchlaufen, `LH-FA-POL-006`) |
| arbeitsbereich-lokal | **0,5** |
| repository-wirksam | **0,7** (reversibler Checkpoint) |
| extern-wirksam | **0,9** (irreversibel) |

Plus die **Resthypothese-Sperr-Schwelle 0,5** (`LH-FA-POL-005`): liegt die
Resthypothese **echt darüber**, wird eine irreversible Aktion **eskaliert statt
freigegeben** — unabhängig von ihrer Erfolgswahrscheinlichkeit. Die 0,5 ist
bewusst identisch mit dem Re-Hypothesen-Schwellwert (`STANDARD_SCHWELLWERT` =
0,5): oberhalb 0,5 trägt „keine der genannten / unbekannt" die Mehrheit der
Masse — dann ist der Zustand zu unsicher für Irreversibles.

## Verglichene Alternativen

### Option A — Eine einzige Schwelle für alle Klassen

- Pro: einfachste Regel.
- Contra: verletzt `LH-FA-POL-003` (Schwelle *muss* von der Wirkungsklasse
  abhängen); behandelt einen Deploy wie einen Log-Read — untragbar für ein
  Safety/Control-Repo (`MR-003`).

### Option B — Schwellen ohne Resthypothese-Sperre

- Pro: weniger Parameter.
- Contra: verletzt `LH-FA-POL-005`; eine zugespitzte Top-Hypothese mit hoher
  Erfolgswahrscheinlichkeit könnte eine irreversible Aktion freigeben, obwohl
  die Diagnose insgesamt unsicher ist (hohe Resthypothese).

### Option C — Gestufte Schwellen + Resthypothese-Sperre (gewählt)

- Pro: erfüllt `LH-FA-POL-002`/`003`/`005`/`007`; fail-safe (im Zweifel
  eskalieren, `LH-QA-02`); konfigurierbar; Default-Werte konservativ zur
  Reichweite steigend.
- Contra: mehrere begründungsbedürftige Konstanten (dieses ADR).

## Konsequenzen

- Positiv: die Sicherheits-Schwellen sind an *einer* begründeten Stelle; das
  Gate ist deterministisch testbar (`LH-QA-03`) inkl. Grenzfällen. Der
  `GateSchwellen`-Konstruktor **erzwingt** Monotonie über die Wirkungsklassen
  und `resthypotheseSperrschwelle < 1` fail-closed — eine fehlkonfigurierte
  Safety-Inversion (extern-wirksam laxer als reversibel) bzw. eine abgeschaltete
  POL-005-Sperre ist **nicht konstruierbar** (Code-Review-Nachlauf).
- Negativ: die konkreten Werte sind eine Setzung; ohne Feld-Daten kalibriert.
- Folgepflicht: `GateSchwellen`-Defaults in `hexagon:domain`; deterministische
  Tests je Ausgang + Grenzfall; die menschliche Freigabe (`LH-FA-POL-004`) und
  die Nicht-Umgehbarkeit (`LH-FA-POL-006`) folgen im application-Schritt
  *aktion-gaten*.

## Fitness Function (falls maschinell prüfbar)

| Tooling | Regel | Make-Target |
|---|---|---|
| `GateSchwellen`-Konstruktor | Erfolgs-Schwellen monoton nicht-fallend über die Wirkungsklassen; `resthypotheseSperrschwelle < 1` (fail-closed) | `make test` |
| Kotlin-Tests (`KonfidenzGateTest`) | irreversible Aktion bei Resthypothese > Sperr-Schwelle wird **nie** freigegeben; invertierte/Sperre-abschaltende Config wird abgewiesen | `make test` |

## Re-Evaluierungs-Trigger

Sobald reale Aktions-Ergebnisse vorliegen (Kalibrierung der Erfolgs-
wahrscheinlichkeiten, welle-05 LLM-Port); oder wenn eine fünfte Wirkungsklasse
bzw. ein regulatorischer Freigabe-Zwang hinzukommt.

## Geschichte

| Datum | Ereignis | Verweis |
|---|---|---|
| 2026-07-05 | Proposed — Gate-Schwellen + Resthypothese-Sperre | slice-012 |
| 2026-07-05 | **Accepted** — nach Code-Review: `GateSchwellen` erzwingt Monotonie + Sperr-Schwelle `< 1` (fail-closed); ab hier immutable | Review slice-012 |
