# ADR-0006: Coverage-Gate-Scope — Gate auf application + Adapter erweitern (per Modul)

**Status:** Accepted

**Datum:** 2026-07-05

**Autor:** belief-agent

**Bezug:** [`LH-QA-03`](../../../spec/lastenheft.md#lh-qa-03--testbarkeit)

**Schärft:** — (Prozess-/Gate-Entscheidung; erweitert den *Scope* von
[`ADR-0004`](0004-coverage-gate.md), dessen Mechanismus + Domänen-Schwelle
unverändert bleiben.)

---

## Kontext

`ADR-0004` scopte das Coverage-Gate auf `hexagon:domain`. Seither tragen
`hexagon:application` (Use-Cases `belief-aktualisieren`, `aktion-gaten`) und die
Outbound-Adapter echte Logik — darunter der **sicherheitskritische, nicht
umgehbare `AktionGaten`** (`LH-FA-POL-006`). Dieser Code war **test-abgedeckt,
aber nicht gate-erzwungen**: eine spätere Änderung konnte seine Abdeckung still
senken, ohne dass ein Gate rot wird. Für ein Safety/Control-Repo (`MR-003`) ist
das eine Lücke — als „Coverage-Scope-Follow-up" seit der Evidenz/Audit-Welle
geführt.

Gemessene Ist-Line-Coverage (`koverLog`): `hexagon:application` **100 %**, alle
vier Adapter **100 %**, `hexagon:domain` **97,65 %**.

## Entscheidung

Das Coverage-Gate wird auf **alle logik-tragenden Module** erweitert, **per Modul**
konfiguriert — jedes Modul trägt seinen eigenen `kover { … verify }`-Block; **kein**
zentraler `subprojects`-Block (bewusst explizit, kein Auto-Inherit für neue Module):

| Schicht | Module | Line-Coverage-Minimum | M2-Bump |
|---|---|---|---|
| Kern | `hexagon:domain`, `hexagon:application` | **90 %** | → 95 % (mit `ADR-0004`) |
| Adapter | `adapters:outbound:*` | **90 % flach** | nein |

Adapter behalten **keine** M2-Hochschaltung: dünne, deterministische Fakes
(`LH-QA-03`) mit wenigen coverbaren Zeilen; ein 95 %-Bump würde bei trivialer
Delegation leicht durch einzelne synthetische Member kippen. 90 % fängt die grobe
Regression (ein ganz untestierter Adapter) ohne Fragilität.

Der Dockerfile-`coverage-gate` verifiziert alle Module explizit (`koverVerify` je
Modul-Task); generierte Member werden mitgezählt (kein Exclude, wie `ADR-0004`).

## Verglichene Alternativen

- **Nur `domain` (Status quo):** lässt den sicherheitskritischen `AktionGaten`
  ungegatet — verworfen.
- **Zentraler `subprojects { kover … }`-Block:** DRY, aber Auto-Inherit versteckt
  das Gate und gatet neue Module still mit — verworfen zugunsten expliziter
  per-Modul-Blöcke.
- **Adapter mit 95 %/eigenem hohen Wert:** fragil bei dünnen Fakes — verworfen
  zugunsten 90 % flach.
- **Adapter ungegatet:** die Adapter-Logik bliebe ungeschützt — verworfen (die
  Scope-Entscheidung war domain + application + adapters).

## Konsequenzen

- Positiv: Coverage-Regressionen in application (inkl. Sicherheitskern) und in den
  Adaptern werden im Gate sichtbar (`make gates`).
- Negativ / Disziplin: ein **neues** Modul ist erst gegatet, wenn es einen
  `kover`-Block **und** einen `koverVerify`-Eintrag im Dockerfile bekommt — keine
  zentrale Automatik, bewusster Trade-off der Explizitheit.
- Folgepflicht: erfüllt in diesem Change (5 Build-Dateien + Dockerfile-Stages
  `coverage`/`coverage-gate`).

## Fitness Function (falls maschinell prüfbar)

| Tooling | Regel | Make-Target |
|---|---|---|
| Kover `koverVerify` | Line-Coverage **je Modul** ≥ Schicht-Minimum (Kern 90 %, Adapter 90 %) | `make coverage-gate` |

## Re-Evaluierungs-Trigger

- M2: Kern-Module (`domain`, `application`) auf 95 % (mit `ADR-0004`).
- Falls ein künftiges dünnes Modul 90 % nicht legitim erreicht (synthetik-lastig):
  dokumentierter **per-Modul-Override** statt Absenkung der Schicht-Regel.

## Geschichte

| Datum | Ereignis | Verweis |
|---|---|---|
| 2026-07-05 | Proposed + **Accepted** — Coverage-Gate auf application + Adapter erweitert (per Modul, 90 % Floor; Adapter flach); erweitert `ADR-0004` | Coverage-Scope-Follow-up |
