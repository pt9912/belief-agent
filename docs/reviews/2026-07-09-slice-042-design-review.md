# Review-Report: slice-042 Design-Review — 2026-07-09

**Review-Art:** Design — geprueft *wogegen*: Architektur (Layer-Grenzen, Port-
Vertrag, ADR-Vertraeglichkeit), **bevor** die Details festgezurrt sind (Modul 10).

**Gegenstand:** `docs/plan/planning/open/slice-042-llm-aktionsvorschlag-provider-adapter.md`
(Loesungs-Schnitt: echter Outbound-Adapter hinter `AktionsVorschlagsPort`,
`ARC-07`/`ARC-08`)

**Skill:** `.harness/skills/reviewer.md` @ v1.0
**Modell:** Claude Opus 4.8 (1M) · **Datum:** 2026-07-09

**Eingangs-Kontext** (Skill §Kontext-Eingang; Safety-Pfad → Ist-Code):

- Slice-Plan slice-042
- `spec/architecture.md` `ARC-03/07/08/09`; `ADR-0001/0002/0003/0006`
- `.../aktionsvorschlag/ports/AktionsVorschlagsPort.kt` (Vertrag),
  `.../dto/AktionsVorschlag.kt` (DTO-Felder),
  `.../aktionsvorschlag/AktionsVorschlagen.kt` (Use-Case-Validierung),
  `adapters/outbound/llm-action-fake/.../FakeAktionsVorschlagsPort.kt` (Fake-Parity),
  `adapters/outbound/llm-langchain4j` / `llm-koog` (Provider-Adapter-Praezedenz,
  `src/main`, Framework-Dep)
- **vorherige Findings:** slice-041 DR-F3 (Validierungs-Schicht) + Frischkontext-
  Write-Pfad-Befund

> **Kontext-Hinweis (Modul 8):** Dieser Design-Review nutzt die slice-041-Lehre
> (Schicht-Besitz von Validierung) als Pruefheuristik, ist aber ein eigener Lauf
> gegen slice-042-Code.

---

## Findings

### F-1 — Validierungs-Schicht: DoD 2 weist dem Adapter Semantik zu, die der Use Case fuehrt — fuer Evidenz ohne Port-Aenderung unmoeglich

- `kategorie`: MEDIUM
- `quelle`: `ARC-07` (Port-Vertrag/Lokalitaet), `ADR-0003` (Layer-Platzierung),
  Maintainability; Skill §Klassifikation („Domain-Invariante dupliziert")
- `pfad`: `docs/plan/planning/open/slice-042-llm-aktionsvorschlag-provider-adapter.md:29`
  (vgl. `AktionsVorschlagen.kt:59/63/66/68/71`, `AktionsVorschlagsPort.kt:14`)
- `befund`: DoD 2 verlangt vom **Adapter** fail-closed bei „unbekannten
  Hypothesen, ungueltigen Wirkungsklassen, fehlender Evidenz, nicht-endlichen
  Zahlen, Werten ausserhalb `[0,1]`". Der **Use Case** `AktionsVorschlagen`
  validiert diese Semantik bereits: unbekannte Hypothese (`:59`), Wirkungsklasse
  (`:63` `valueOf`), Evidenz-Aufloesung + Nicht-Leere (`:66/68`), Konfidenz-Bereich
  (`:71` `externalisieren`). Zwei Punkte:
  (a) **Duplikat/Drift-Risiko** fuer Hypothese/Wirkungsklasse/Bereich — Doppel-
  Quelle in Adapter und Use Case.
  (b) **Unmoeglich** fuer „fehlende Evidenz": der Port `vorschlaege(belief: BeliefState)`
  (`:14`) erhaelt **keine** bekannte Evidenz (`bekannteEvidenz` lebt im
  `AktionsVorschlagenBefehl`, nicht am Port), und §8 (`:124`) schliesst eine
  Port-Aenderung aus. Der Adapter kann Evidenzreferenzen also gar nicht gegen
  bekannte Evidenz pruefen.
  Beobachtbarer sauberer Schnitt (nicht vorgeschrieben): Adapter = **Wire-/
  Deserialisierungs-Integritaet** (unbekannte/doppelte JSON-Felder, Typ/Shape,
  nicht-endliche Zahlen — DoD 2 erster Teil); Semantik (Hypothese/Evidenz/
  Wirkungsklasse/Bereich) bleibt im Use Case. Spiegelt slice-041 DR-F3.
- `verifizierbar`: ja — Code-Review, ob der Adapter nur Wire-Format prueft und
  Semantik dem Use Case ueberlaesst; die Port-Signatur belegt den fehlenden
  Evidenz-Kontext; `make arch-check`/`make test`.

### F-2 — Fehler-Signalisierung „verworfen ODER sichtbar" unbestimmt; Fake-Parity ist stilles Leer

- `kategorie`: LOW
- `quelle`: `LH-QA-02`, Maintainability; Skill §Klassifikation
- `pfad`: `docs/plan/planning/open/slice-042-llm-aktionsvorschlag-provider-adapter.md:33`
- `befund`: DoD 2 laesst offen, ob Schema-Verstoesse „fail-closed **verworfen**
  ODER als Adapterfehler **sichtbar**" enden. Der Port-Aufruf liegt **ausserhalb**
  des per-Vorschlag-`runCatching` (`AktionsVorschlagen.kt:47`), ein Adapter-Wurf
  propagiert dort also — anders als beim (schluckenden) per-Vorschlag-Pfad. Die
  Fake-Praezedenz gibt bei Fehlern **still `emptyList()`** zurueck
  (`FakeAktionsVorschlagsPort.kt:38-40`). Beide Zweige sind hier fail-safe (kein
  Vorschlag ⇒ keine gate-faehige Aktion, `LH-QA-02`; **kein** Pflicht-Audit-Verlust
  wie bei slice-041), aber die Disjunktion sollte je Fehlerklasse (leere Antwort
  vs. Provider-Ausfall vs. Einzel-Vorschlag-Schemabruch) bestimmt werden, damit
  „Provider unreachable" nicht ununterscheidbar von „kein Vorschlag" bleibt.
- `verifizierbar`: ja — Tests fuer „leere Antwort" / „kaputtes JSON" / „Provider-
  Ausfall" zeigen das gewaehlte Verhalten je Klasse.

### F-3 — `ADR-0002`-Dependency-Guard bei nicht bereits adoptiertem Framework nicht benannt

- `kategorie`: INFO
- `quelle`: `ADR-0002` (Dependency am Rand), `ADR-0003`
- `pfad`: `docs/plan/planning/open/slice-042-llm-aktionsvorschlag-provider-adapter.md:26`
- `befund`: `dev.langchain4j:1.17.1` und `ai.koog:1.0.0` sind bereits als
  Adapter-Deps adoptiert; ein `llm-action-langchain4j`/`-koog`-Modul fuegt keine
  **neue** Toolchain-Flaeche hinzu (kein Folge-ADR). Faellt die (noch offene, F-1
  im Plan-Review) Framework-Wahl auf ein **nicht** adoptiertes Framework, greift
  der `ADR-0002`-Guard (Folge-ADR vor Code) — der Plan benennt diese Rueckfallebene
  nicht (slice-041 tat es nach Review).
- `verifizierbar`: ja — `build.gradle.kts`-Diff zeigt, ob eine neue Framework-Dep
  entsteht.

## Negativbefunde

- geprueft, ohne Befund: **Source-Set/Build** korrekt — `src/main` (JVM) wie die
  Provider-Adapter-Praezedenz; `.a-check.yml` + Modul-`build.gradle.kts` + Kover
  in DoD 4 (slice-041 DR-F2/IPR-2-Lehre angewandt).
- geprueft, ohne Befund: **Kern-Reinheit** — `hexagon:*` importiert keine
  Provider-/Framework-Pakete (DoD 1); Adapter implementiert nur den Port.
- geprueft, ohne Befund: **Port unveraendert** ist der richtige Schnitt — der
  Adapter liefert Rohvorschlaege, der Use-Case-Validierungsrand bleibt intakt
  (setzt F-1 voraus: Adapter darf die Use-Case-Semantik nicht duplizieren).
- geprueft, ohne Befund: **Executor-/Gate-Grenze** (`LH-FA-POL-006`, `ARC-03`/
  `ARC-09`) sauber ausserhalb des Adapters gehalten.
- geprueft, ohne Befund: **Strikte Parser-Grenze** gegen adversariale
  LLM-Ausgabe ist als Kern-DoD adressiert (§6, `:82`) — richtiger Safety-Fokus.

## Ausgefuehrte Sensoren

- `Read`/`grep` ueber Port/DTO/Use-Case/Fake, Provider-Adapter-`build.gradle.kts`,
  Port-Signatur (Evidenz-Kontext), Use-Case-Konsum; WIP-Check.
- `make doc-check` — PASS (`d-check`: 0 Befunde; validiert beide slice-042-Reports).

## Summary

| Kategorie | Anzahl |
|---|---:|
| HIGH | 0 |
| MEDIUM | 1 |
| LOW | 1 |
| INFO | 1 |

## Verdikt

**Merge-blockierend:** ja — **1 MEDIUM** (F-1 Validierungs-Schicht). Vor
Implementierungsstart zu klaeren: welche Schicht welche Validierung besitzt, und
die Aufloesung der DoD-2-vs-§8-Spannung (Semantik im Adapter vs. kein Port-Wechsel).
F-2/F-3 sind vor Closure zu klaeren, nicht blockierend.

**Uebergabe:** Findings an Planung/Design-Klaerung (Rueckkante Design → Plan; ein
§9-Block wie in slice-041 traegt die Aufloesung sauber). Reviewer kategorisiert —
Wahl/Umsetzung bei Architect/Implementation. Keine Verifikation (Modul 11).
