# ADR-0008: Gate-/Resthypothese-Schwellwerte spec-konform (verschärft)

**Status:** Accepted

**Datum:** 2026-07-06

**Autor:** belief-agent

**Supersedes:** `ADR-0005`

**Bezug:** [`LH-FA-POL-003`](../../../spec/lastenheft.md#lh-fa-pol-003--schwellen-je-wirkungsklasse), [`LH-FA-POL-005`](../../../spec/lastenheft.md#lh-fa-pol-005--sperre-extern-wirksamer-aktionen-bei-hoher-resthypothese), [`LH-FA-POL-007`](../../../spec/lastenheft.md#lh-fa-pol-007--konfigurierbare-schwellwerte), [`LH-QA-03`](../../../spec/lastenheft.md#lh-qa-03--testbarkeit); `spezifikation.md` §3.

**Schärft:** — (Threshold-/Policy-Entscheidung; die Werte leben als `GateSchwellen`-
und `ReHypothesenAusloeser`-Defaults im Domänen-Kern, konfigurierbar `LH-FA-POL-007`.)

---

## Kontext

`ADR-0005` setzte die Gate-/Resthypothese-Schwellen (Erfolgs-Schwellen je
Wirkungsklasse, Resthypothese-Sperre) als „begründete Setzung, ohne Feld-Daten
kalibriert". Ein Review (welle-04) deckte auf, dass diese Werte **durchgängig
weniger konservativ** sind als die **Spezifikations-Tabelle** (`spezifikation.md`
§3) — auf der **Sicherheitsfunktion** (`MR-003`):

| Schwelle | Spec §3 | `ADR-0005`/Code | Richtung |
|---|---|---|---|
| θ_repository-wirksam | 0,80 | 0,7 | Code laxer |
| θ_extern-wirksam | 0,95 | 0,9 | Code laxer |
| θ_other_block (Irreversibel-Sperre, `LH-FA-POL-005`) | **0,10** | **0,5** | Code **5× laxer** |
| θ_rehyp (`STANDARD_SCHWELLWERT`, `LH-FA-BEL-005`) | 0,30 | 0,5 | Code träger |

Die **Source Precedence** (`harness/README.md`) stellt `spezifikation.md` (Rang 2)
**über** ADRs (Rang 4). Eine ADR darf die Spec **schärfen**, nicht **lockern**
(`MR-001`); `ADR-0005` hat sie faktisch gelockert. Für ein Safety/Control-Repo ist
die konservativere Spec-Setzung das richtige Default, solange keine Feld-Daten eine
Lockerung begründen.

## Entscheidung

**Der Code wird an die Spezifikations-Tabelle §3 angeglichen** (verschärft);
`ADR-0005` wird superseded:

| Parameter | Wert |
|---|---|
| nur-lesend | 0,0 |
| arbeitsbereich-lokal | 0,50 |
| repository-wirksam | **0,80** |
| extern-wirksam | **0,95** |
| Resthypothese-Sperre θ_other_block (`LH-FA-POL-005`) | **0,10** |
| θ_rehyp (`STANDARD_SCHWELLWERT`, `LH-FA-BEL-005`) | **0,30** |

Die Resthypothese-Sperre wird von `STANDARD_SCHWELLWERT` **entkoppelt** — die Spec
trennt θ_other_block (0,10) und θ_rehyp (0,30). θ_esc bleibt 0,30 (`ADR-0007`) und
deckt sich nun mit θ_rehyp (Spec: „θ_esc Startwert = θ_rehyp"). Die fail-closed-
Invarianten aus `ADR-0005` (Monotonie der Erfolgs-Schwellen, Sperre echt `< 1`)
gelten unverändert; 0,0 ≤ 0,50 ≤ 0,80 ≤ 0,95 und 0,10 `< 1`.

## Verglichene Alternativen

- **Spec an Code angleichen (verworfen):** die Spec-Tabelle auf die laxeren Code-
  Werte senken. Contra: kehrt die Source Precedence um (die niedriger-rangige
  Implementierung diktiert den höher-rangigen Vertrag) und hält eine permissive
  Irreversibel-Sperre (0,5) auf der Sicherheitsfunktion — ohne Feld-Daten untragbar.
- **`ADR-0005`-Werte beibehalten (verworfen):** widerspricht Spec + Source Precedence;
  die Abweichung war zudem un-dokumentiert (Spec-Tabelle nie an `ADR-0005` angeglichen).

## Konsequenzen

- Positiv: die Sicherheits-Schwellen sind **spec-konform und konservativer**; eine
  irreversible Aktion wird bereits ab 10 % (statt 50 %) Resthypothese
  gesperrt/eskaliert.
- Negativ/Verhaltensänderung: das Gate gibt irreversible Aktionen seltener frei;
  Grenzfall-/Negativ-Tests (Gate, aktion-gaten, Entscheidungszyklus, Re-Hypothesen)
  wurden an die neuen Schwellen angepasst — ohne Deckungsverlust.
- Kalibrierung bleibt offen (wie `ADR-0005`): Re-Evaluierung mit realen
  Aktions-Ergebnissen (welle-05).

## Fitness Function

| Tooling | Regel | Make-Target |
|---|---|---|
| `GateSchwellen`-Konstruktor | Erfolgs-Schwellen monoton; Sperre `< 1` (fail-closed) | `make test` |
| Kotlin-Tests | irreversible Aktion bei Resthypothese `> 0,10` nie freigegeben; Freigabe-Schwellen 0,80/0,95 | `make test` |

## Re-Evaluierungs-Trigger

Reale Aktions-Ergebnisse (Kalibrierung, welle-05 LLM-Port).

## Geschichte

| Datum | Ereignis | Verweis |
|---|---|---|
| 2026-07-06 | **Accepted** — Schwellen spec-konform verschärft, `Supersedes ADR-0005`; aus welle-04-Review-Follow-up | slice-018 |
