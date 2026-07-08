# Review-Report: slice-038 Design - 2026-07-08

**Review-Art:** Design - Design-Review gegen Architektur, Layer-Grenzen,
Port-Schnitt und ADR-Vertraeglichkeit.

**Gegenstand:** `docs/plan/planning/done/slice-038-approval-kanalwahl.md`

**Skill:** `.harness/baseline/v1.4.0/regelwerk/modul-10-review-harness.md`
@ `v1.4.0` . **Modell:** Codex GPT-5 . **Datum:** 2026-07-08

**Eingangs-Kontext** (die Vertraege, gegen die geprueft wurde):

- `docs/plan/planning/done/slice-038-approval-kanalwahl.md`
- `.harness/baseline/v1.4.0/regelwerk/modul-10-review-harness.md`
- `spec/architecture.md`
- `docs/plan/adr/0001-hexagonal-llm-port.md`
- `docs/plan/adr/0003-hexslice-architektur.md`
- `spec/lastenheft.md` zu `LH-FA-POL-004`, `LH-FA-POL-006`, `LH-OUT-04`,
  `LH-QA-02`, `LH-QA-03`, `LH-QA-04`, `LH-OP-04`
- `docs/plan/planning/done/slice-037-cli-approval-binding.md`
- bestehende Approval-/CLI-Struktur:
  `adapters/inbound/cli`, `adapters/outbound/approval-local`,
  `hexagon/application/.../gaten/ports`, `.a-check.yml`
- `AGENTS.md` Hard Rules aus Session-Kontext

---

## Findings

### F-1 - Kanalwahl-Grenze ist architektonisch nicht eindeutig geschnitten

- `kategorie`: MEDIUM
- `quelle`: ARC-09 / ADR-0003
- `pfad`: `docs/plan/planning/done/slice-038-approval-kanalwahl.md:44`
- `befund`: Der Design-Schnitt laesst die Kanalwahl wahlweise im
  Application-Port-Bereich oder CLI-nah entstehen. Dadurch ist nicht
  eindeutig, ob konkrete Kanaladressierung Teil des Core-Vertrags oder reines
  `ARC-09`-Composition-Wiring ist.
- `verifizierbar`: nein - die Unklarheit ist ein Design-Befund; `make
  arch-check` kann erst konkrete Importverletzungen pruefen.

## Negativbefunde

- geprueft, ohne Befund: `ADR-0001`. Der Plan sieht keinen Core-Import eines
  konkreten Adapters und keinen neuen Ausfuehrungspfad vor.
- geprueft, ohne Befund: `ADR-0003` zur Rollenrichtung. `adapters/inbound/cli`
  bleibt der vorgesehene Composition-Root fuer Adapter-Wiring; fachliche
  Adapter-zu-Adapter-Kopplung soll verboten bleiben.
- geprueft, ohne Befund: `ARC-07`. `HumanApprovalPort` bleibt der fachliche
  Port fuer die Freigabe; der Plan ersetzt ihn nicht durch direkten
  Executor- oder Kanalaufruf.
- geprueft, ohne Befund: `ARC-08`. `local` bleibt der einzige konkrete
  vorhandene Approval-Kanal; Remote-/UI-Adapter werden nicht in diesen
  Design-Schnitt gezogen.
- geprueft, ohne Befund: `ARC-09`. Der Plan verortet das bewusste Binding und
  die CLI-Integration grundsaetzlich im Composition-Root.
- geprueft, ohne Befund: Gate-Nicht-Umgehbarkeit. Kanalwahl soll nur den
  Approval-Pfad konfigurieren und keine Freigabe ohne `HumanApprovalPort`
  erzeugen.
- geprueft, ohne Befund: Fail-closed-Design. Unbekannter, fehlender oder
  fehlerhafter Kanal sowie ungueltige Kontextbindung sollen keine Freigabe
  propagieren.
- geprueft, ohne Befund: Testdesign. Negativmatrix fuer unbekannten Kanal,
  fehlende Kanalbindung, Kanalfehler und erfolgreichen `local`-Dispatch ist
  vorgesehen.
- geprueft, ohne Befund: Folgegrenzen. Remote-/UI-Kanaladapter und
  persistenter Approval-Audit bleiben separate Slices.
- geprueft, ohne Befund: `make doc-check` fuer den Review-Artefaktstand.
- geprueft, ohne Befund: `make gates` fuer den Review-Artefaktstand.

## Summary

| Kategorie | Anzahl |
|---|---:|
| HIGH | 0 |
| MEDIUM | 1 |
| LOW | 0 |
| INFO | 0 |

## Verdikt

**Merge-blockierend:** ja - MEDIUM sollte vor Implementation/Promotion
geklaert werden, damit konkrete Kanalwahl nicht unabsichtlich in den
Application-Core wandert.

**Uebergabe:** Design-Handoff mit einem offenen Finding. Der Report ersetzt
keine Verification und keine spaetere Code-/Safety-Review des
Implementationsdiffs.
