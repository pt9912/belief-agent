# Slice slice-018: Schwellwert-Reconciliation — Code an Spec angleichen

**Status:** open → next → in-progress → done (siehe [Planning-README](../README.md)).

**Kontext:** Korrektur-Slice (zwischen welle-04 und welle-05), siehe
[Roadmap](../in-progress/roadmap.md); `ADR-0007`-Follow-up.

**Bezug:** `LH-FA-POL-003`, `LH-FA-POL-005`, `LH-FA-BEL-005`, `LH-QA-03`;
`ADR-0008` (Supersedes `ADR-0005`); `spezifikation.md` §3.

**Autor:** pt9912. **Datum:** 2026-07-06.

---

## 1. Ziel

Die Gate-/Resthypothese-Schwellen im Code weichen **weniger konservativ** von der
Spezifikations-Tabelle (§3) ab — auf der **Sicherheitsfunktion** (`MR-003`). Da die
Spec (Rang 2) die ADRs (Rang 4) sticht, wird der **Code an die Spec angeglichen**
(verschärft); `ADR-0005` (immutable) wird von **`ADR-0008`** superseded.

| Schwelle | vorher (Code) | nachher (Spec) |
|---|---|---|
| θ_repository-wirksam | 0,7 | **0,80** |
| θ_extern-wirksam | 0,9 | **0,95** |
| θ_other_block (Irreversibel-Sperre) | 0,5 | **0,10** |
| θ_rehyp (`STANDARD_SCHWELLWERT`) | 0,5 | **0,30** |

`resthypotheseSperrschwelle` wird von `STANDARD_SCHWELLWERT` **entkoppelt** (Spec:
θ_other_block = 0,10 ≠ θ_rehyp = 0,30). θ_esc bleibt 0,30 (`ADR-0007`) — deckt sich
nun mit θ_rehyp (Spec: „θ_esc Startwert = θ_rehyp").

## 2. Definition of Done

- [x] `ADR-0008` **Accepted**, `Supersedes ADR-0005`; ADR-Index aktualisiert.
- [x] Code spec-konform: `STANDARD_SCHWELLWERT` = 0,30; `GateSchwellen`-Defaults
      0,0/0,50/0,80/0,95; `resthypotheseSperrschwelle` = 0,10 (entkoppelt); KDocs
      (`KonfidenzGate`/`ReHypothesenAusloeser`/`Eskalationsbedingung`) nachgezogen.
- [x] Tests angepasst (3 Domänen-Grenzfälle: Gate-Sperre-Grenze 0,10, θ_rehyp 0,30);
      **kein** Deckungsverlust; Coverage domain 98,2 % ≥ 90 %; `make gates` grün.
- [x] Closure-Notiz (unten).

## 3. Plan (vor Code)

| Datei | Änderungs-Art | Begründung |
|---|---|---|
| `docs/plan/adr/0008-*.md` + `README.md` | neu/update | Supersedes `ADR-0005`, spec-konforme Werte |
| `.../belief/ReHypothesenAusloeser.kt` | update | `STANDARD_SCHWELLWERT` 0,5 → 0,30; KDoc |
| `.../belief/KonfidenzGate.kt` (`GateSchwellen`) | update | 0,80/0,95; `resthypotheseSperrschwelle` 0,10 entkoppelt; KDoc |
| `.../belief/eskalation/Eskalationsbedingung.kt` | update | KDoc-Drift-Note (θ_rehyp jetzt spec-konform) |
| `.../*Test.kt` (Gate/Aktion/Zyklus/ReHypothesen) | update | Grenzfälle an 0,10/0,30/0,80/0,95 |

## 4. Trigger

welle-04 done; `ADR-0007` deckte den breiteren Schwellwert-Drift als benannte
Spec-Lücke auf.

## 5. Closure-Trigger

DoD vollständig + Closure-Notiz; Datei nach `done/`.

## 6. Risiken und offene Punkte

- **Verhaltensänderung der Sicherheitsfunktion:** das Gate sperrt/eskaliert
  irreversible Aktionen künftig schon ab 10 % Resthypothese (statt 50 %) — konservativer,
  gewollt. Negativ-/Grenzfall-Tests entsprechend nachziehen (kein Deckungsverlust).
- **`ADR-0005`-Supersession:** erste Supersession im Repo; Referenzen auf `ADR-0005`
  in historischen Doku bleiben faktisch korrekt (Verweis-Politik beim doc-Gate prüfen).

## 7. Closure-Notiz (nach `done/`)

**Was funktionierte:** die Verschärfung war chirurgisch — nur **3 Domänen-Grenzfall-
Tests** brauchten Anpassung (Gate-Sperre-Grenze 0,5→0,10, θ_rehyp 0,5→0,30); Aktion-
Gaten- und Zyklus-Tests passten unverändert (ihre Werte lagen ohnehin klar über/unter
den neuen Schwellen). **Steering-Loop:** eine **Accepted-ADR (`ADR-0005`) durfte
superseded werden**, ohne sie zu mutieren (Hard Rule 3.5) — `ADR-0008` trägt
`Supersedes`, `ADR-0005` bleibt file-seitig „Accepted" (matrix-Regel trippt nicht).
**Regel geschärft:** bei Spec↔ADR-Konflikt sticht die **Source Precedence** (Spec Rang
2 > ADR Rang 4); eine ADR, die die Spec *lockert* statt schärft, ist ein Fehler —
gefunden nur, weil `ADR-0007` den Abgleich als benannte Spec-Lücke tracked hat.

## 8. Sub-Area-Modus-Begründung

Berührte Sub-Areas (`hexagon:domain/belief`, `docs/plan/adr`) sind Bestand — GF-geführt,
Doku führt. Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
