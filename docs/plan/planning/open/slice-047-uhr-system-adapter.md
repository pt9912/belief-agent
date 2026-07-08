# Slice slice-047: Echter UhrPort-Systemadapter

**Status:** open (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung.

**Bezug:** `LH-FA-OBS-006`, `LH-FA-AUD-004`, `LH-QA-02`, `LH-QA-03`,
`LH-QA-04`; `ADR-0001`, `ADR-0003`, `ADR-0006`, `ARC-07`, `ARC-08`,
`ARC-09`.

**Autor:** Codex. **Datum:** 2026-07-08.

---

## 1. Ziel

Ein echter Outbound-Adapter implementiert den bestehenden `UhrPort` mit einer
Systemzeit-Quelle und garantiert dabei den Port-Vertrag monoton nicht-fallender
`Zeitstempel`, ohne Domain/Application an `Clock`/Systemzeit zu koppeln oder den
deterministischen CLI-Default nebenbei umzubinden.

## 2. Definition of Done

- [ ] Neues Outbound-Adaptermodul (z. B. `uhr-system` oder enger benannt)
  implementiert `UhrPort` hinter `ARC-08`; `hexagon:*` importiert keine
  Systemzeit-/IO-Pakete und keinen Adapter.
- [ ] Der Adapter liefert `Zeitstempel` aus einer echten Systemzeit-Quelle und
  erfuellt den bestehenden Vertrag monoton nicht-fallend: wiederholte
  `jetzt()`-Aufrufe duerfen nie rueckwaerts laufen, auch wenn die zugrunde
  liegende Wanduhr kleinere Werte liefert.
- [ ] Tests sind hermetisch und deterministisch (`LH-QA-03`): injizierbare
  Rohzeitquelle, steigende Zeit, gleiche Zeit, Ruecksprung der Rohzeit,
  negativer/ungueltiger Rohwert und lange Aufrufserie sind abgedeckt.
- [ ] Fail-closed-/Fehlerverhalten ist sichtbar (`LH-QA-02`): ungueltige
  Rohzeitwerte duerfen keinen validen Ereignis- oder Beobachtungszeitstempel
  erzeugen; Fehler werden nicht still auf `0` oder den letzten Wert repariert,
  falls dadurch die Ursache unsichtbar wuerde.
- [ ] Build-/Arch-/Coverage-Integration ist vollstaendig:
  `settings.gradle.kts`, `.a-check.yml`, `Dockerfile`, Modul-`build.gradle.kts`
  und Kover-Gate sind ergaenzt (`ADR-0003`, `ADR-0006`).
- [ ] Nutzer-/Integrationsdoku und Verification-Artefakt beschreiben den
  Adapter, seine Monotonie-Garantie und die Abgrenzung zu Fake-/Testuhren; kein
  CLI-/Runtime-Default-Binding wird in diesem Slice geaendert.
- [ ] `make doc-check` und `make gates` sind gruen; Closure-Notiz benennt, ob
  ein bewusster CLI-/Runtime-Binding-Slice fuer die Systemuhr noetig ist.

## 3. Plan (vor Code)

| Datei / Komponente | Aenderungs-Art | Begruendung |
|---|---|---|
| `adapters/outbound/uhr-system` oder enger benannter Adapter | neu | Echter `UhrPort`-Adapter hinter `ARC-08`. |
| `.../src/main/kotlin/**` | neu | Systemzeit-Quelle, Monotonie-Wrapper, Rohzeit-Validierung und Fehlergrenze. |
| `.../src/test/kotlin/**` | neu | Deterministische Tests fuer steigende, gleiche, rueckwaerts laufende und ungueltige Rohzeit. |
| `settings.gradle.kts` | update | Neues Adaptermodul registrieren. |
| `.a-check.yml` | update | Adapterrolle und erlaubte Kante zum lokalen `UhrPort` aufnehmen; Core bleibt systemzeitfrei. |
| `Dockerfile` | update | Neues Modul in Build-, Test- und Coverage-Stages aufnehmen. |
| `docs/user/integration.md` | update | Systemuhr-Adapter und Monotonie-Vertrag dokumentieren. |
| `docs/reviews/*slice-047*` | neu | Review-Artefakt mit Fokus Monotonie, Determinismus und Adaptergrenze. |
| `docs/verifications/*slice-047*` | neu | Verification-Artefakt fuer DoD, Negativmatrix und Gates. |

## 4. Trigger

`slice-009` liegt in `done/` und hat `UhrPort` als use-case-lokalen Vertrag
eingefuehrt; der CLI-Composition-Root nutzt derzeit `MonotoneFakeUhr` als
deterministische Test-/Demo-Uhr. Kein Slice liegt in `in-progress/` (WIP-Limit
1). Vor Start wird entschieden, ob der Adapter als JVM-Systemzeitadapter oder
als multiplatform-faehige Zeitquelle gebaut wird; bei neuer Zeit-/Scheduler-
Dependency ist vor Code ein Design-Review oder eine ADR-Pruefung noetig.

## 5. Closure-Trigger

DoD vollstaendig + Review/Verification abgeschlossen + `make gates` gruen +
Closure-Notiz geschrieben + Slice nach `done/` verschoben.

## 6. Risiken und offene Punkte

- Eine echte Wanduhr kann durch NTP-/Systemkorrekturen rueckwaerts springen. Der
  Adapter muss den `UhrPort`-Vertrag monoton nicht-fallend schuetzen, sonst
  koennen append-only Ereignisfolgen beim Persistieren abbrechen.
- Tests duerfen keine echte Zeit, Sleeps oder Timing-Fenster benoetigen; die
  Rohzeitquelle muss injizierbar sein.
- CLI-/Runtime-Default-Binding bleibt getrennt. Der Adapter-Slice stellt die
  Faehigkeit bereit, aendert aber nicht nebenbei deterministische Demos oder
  hermetische Gates.
- Zeitformat-/Zeitzonenpolitik bleibt klein: Der Port nutzt `epochMillis`.
  Formatierung, lokale Zeitzonen und externe Zeitdienste sind nicht Scope.

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss fuellen. -->

## 8. Sub-Area-Modus-Begründung

### Sub-Area: `adapters/outbound/uhr-*`

- **Modus:** Hybrid
- **Konventionen-Dichte:** hoch fuer `UhrPort`, `Zeitstempel` und den
  Monotonie-Vertrag (`slice-009`, `ARC-07`), mittel fuer die neue reale
  Systemzeit-Adapterform.
- **Phase-Reife:** Phase 4 fuer Contract und Fake-Uhr im CLI-Root, Phase 2-3
  fuer den echten Systemzeitadapter.
- **Evidenz-/Diskrepanz-Risiko:** mittel bis hoch. Rueckwaerts laufende oder
  nicht deterministisch getestete Zeit kann Audit-/Beobachtungsreihenfolgen
  unbemerkt brechen.
- **Reconciliation-Aufwand:** Teil dieses Slice: Adaptermodul, Monotonie- und
  Negativmatrix, Build-/Arch-/Coverage-Integration. Graduation-Trigger:
  `make gates` gruen und Verification ohne offene Zeit-/Determinismusdrift.

### Sub-Area: `hexagon/application/belief/aktualisieren/ports`

- **Modus:** GF
- **Konventionen-Dichte:** hoch. `UhrPort` ist use-case-lokaler Contract und
  bleibt unveraendert; der Adapter implementiert nur den bestehenden Port.
- **Phase-Reife:** Phase 4. Der Port wird seit `slice-009` im
  Belief-Aktualisieren-Flow verwendet.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Eine Contract-Aenderung wuerde
  Belief-Aktualisierung, CLI-Runtime und Tests beruehren; dieser Slice plant
  keine Port-Aenderung.
- **Reconciliation-Aufwand:** keiner im Core. Falls der Port fuer Fehlerklassen
  erweitert werden muss, wird ein separater Contract-Slice geplant.

### Sub-Area: `adapters/inbound/cli` / Runtime-Binding

- **Modus:** GF
- **Konventionen-Dichte:** hoch. `ARC-09` verortet Runtime-Binding im
  Composition-Root; dieser Slice soll keinen CLI-Default und keine
  Demo-Zeitpolitik veraendern.
- **Phase-Reife:** Phase 4. CLI-Composition ist stabil und nutzt bewusst
  `MonotoneFakeUhr` fuer reproduzierbare Szenarien.
- **Evidenz-/Diskrepanz-Risiko:** mittel. Eine nebenbei geaenderte Uhr kann
  hermetische Tests, Demo-Ausgaben und Replay-Erwartungen instabil machen.
- **Reconciliation-Aufwand:** keiner in diesem Slice; eigener Binding-Slice,
  falls CLI-Flags, Defaults oder Runtime-Zeitpolitik geaendert werden.
