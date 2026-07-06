# ADR-0007: Eskalations-Schwelle θ_esc = 0,30 (spec-konform), entkoppelt von der Gate-Sperre

**Status:** Accepted

**Datum:** 2026-07-06

**Autor:** belief-agent

**Bezug:** [`LH-FA-ESK-001`](../../../spec/lastenheft.md#lh-fa-esk-001--eskalationsbedingung), [`LH-FA-ESK-003`](../../../spec/lastenheft.md#lh-fa-esk-003--eskalations-kontext), [`LH-QA-03`](../../../spec/lastenheft.md#lh-qa-03--testbarkeit); `spezifikation.md` §3 (θ_esc = 0,30) und `LH-FA-POL-002.a` (`p_other ≥ θ_esc`).

**Schärft:** — (Threshold-/Policy-Entscheidung; die Schwelle lebt als
`Eskalationsbedingung.STANDARD_ESKALATIONS_SCHWELLE`-Default im Domänen-Kern und ist
je Aufruf konfigurierbar.)

---

## Kontext

Die **Eskalationsbedingung** (`ARC-05`, `LH-FA-ESK-001`) hält an und
eskaliert an einen Menschen, wenn günstige Beobachtungen erschöpft sind **und** die
Resthypothese hoch bleibt **und** das Gate geschlossen ist. „Resthypothese hoch"
misst gegen die Eskalations-Schwelle **θ_esc**. Als Resthypothese-basierter
Parameter der **Sicherheitsfunktion** (`MR-003`) ist sie ADR-pflichtig (Regelwerk
Modul 13, analog `ADR-0005`).

Ein sequentielles Code-Review der VoI-/Eskalations-Domäne fand: die Bedingung hatte
θ_esc an `ReHypothesenAusloeser.STANDARD_SCHWELLWERT` (= **0,5**) gekoppelt und mit **striktem
`>`** verglichen. Beides weicht von der Spezifikation ab — `spezifikation.md` §3 setzt
**θ_esc = 0,30** und `LH-FA-POL-002.a` verlangt **`p_other ≥ θ_esc`** (≥, nicht >).
Beide Abweichungen zeigen in Richtung **Unter-Eskalation** (die gefährliche Richtung
für einen Fail-safe-Pfad, `LH-QA-02`).

**Abgrenzung zu `ADR-0005`:** `ADR-0005` setzt die Resthypothese-**Sperr**-Schwelle
des Konfidenz-**Gates** (`LH-FA-POL-005`) bewusst auf **0,5** — das ist die Grenze, ab
der eine *irreversible Aktion* **blockiert** wird. Das ist ein **anderer** Zweck als
die Eskalation: die Eskalation soll den Menschen holen, *bevor* der Agent feststeckt,
und darf daher **früher** greifen. `ADR-0005` ist Accepted/immutable und wird hier
**nicht** berührt.

## Entscheidung

**θ_esc = 0,30** (spec-konform), Vergleich **`Resthypothese ≥ θ_esc`**
(`LH-FA-POL-002.a`), als **eigener** Default `STANDARD_ESKALATIONS_SCHWELLE` in
`Eskalationsbedingung` — **entkoppelt** von der Gate-Sperre (`ADR-0005`, 0,5) und
von `ReHypothesenAusloeser.STANDARD_SCHWELLWERT` (0,5). Konfigurierbar je Aufruf
(`schwelle`-Parameter, `require(schwelle in 0.0..1.0)` fail-closed).

## Verglichene Alternativen

### Option A — 0,5 beibehalten und per ADR segnen (verworfen)
- Pro: eine Konstante für alle Resthypothese-Schwellen.
- Contra: widerspricht der Spec (θ_esc = 0,30) ohne fachlichen Grund; koppelt
  Eskalation (früh holen) an die Irreversibel-Sperre (spät blockieren), obwohl die
  Spec sie bewusst trennt; unter-eskaliert im Band [0,30 … 0,50].

### Option B — θ_esc = 0,30, `≥`, eigener Default (gewählt)
- Pro: spec-konform; trennt Eskalation sauber von der Gate-Sperre; fail-safe
  (`LH-QA-02`, im Zweifel eskalieren); deterministisch testbar inkl. Grenzfall
  θ_esc genau erreicht (`LH-QA-03`).
- Contra: die Spec koppelt θ_esc an θ_rehyp (beide 0,30) — diese Kopplung ist
  code-seitig **noch nicht** realisiert, weil `STANDARD_SCHWELLWERT` (θ_rehyp im
  Code) mit **0,5** selbst vom Spec-θ_rehyp (0,30) driftet. → Follow-up.

## Konsequenzen

- Positiv: der Fail-safe-Pfad ist spec-konform und ADR-dokumentiert; das
  Unter-Eskalations-Fenster [0,30 … 0,50] ist geschlossen; θ_esc ist eine eigene,
  begründete Stelle.
- Negativ: die Resthypothese-Schwellen liegen jetzt an **drei** Stellen mit **zwei**
  Werten (θ_esc = 0,30 hier; θ_rehyp und Gate-Sperre = 0,5 via
  `STANDARD_SCHWELLWERT`/`ADR-0005`). Der Spec-Drift von `STANDARD_SCHWELLWERT`
  (0,5 vs. Spec-θ_rehyp 0,30) und der Gate-Sperre (0,5 vs. Spec-θ_other_block 0,10)
  bleibt **offen**.
- **Folge-/Reconciliation-Punkt (Follow-up, eigener Slice/ADR):** Spec-Tabelle §3
  ↔ Code/`ADR-0005` abgleichen — entweder `STANDARD_SCHWELLWERT` auf 0,30 ziehen
  (dann kann θ_esc θ_rehyp wieder teilen) und die Spec-θ_other_block/θ_rehyp-Werte
  an `ADR-0005` angleichen, oder die Spec-Tabelle als führend bestätigen. Nicht
  Teil dieser Entscheidung.

## Re-Evaluierungs-Trigger

Reale Feld-Daten (Kalibrierung, welle-05 LLM-Port); oder die
`STANDARD_SCHWELLWERT`-Reconciliation oben (dann θ_esc ggf. wieder an θ_rehyp
koppeln).

## Geschichte

| Datum | Ereignis | Verweis |
|---|---|---|
| 2026-07-06 | **Accepted** — θ_esc = 0,30/`≥`, entkoppelt; aus sequentiellem Review slice-014/015 (F1) | slice-015 |
