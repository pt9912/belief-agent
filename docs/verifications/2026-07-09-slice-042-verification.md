# Verification-Report: slice-042 — 2026-07-09

**Verification-Art:** DoD-/Spec-Verifikation gegen Slice-Plan, Code-/Doku-Artefakte
und Sensoren (Modul 11). Prüft *Code gegen DoD/Spec/Plan* — nicht „ist es gut?"
(Review, Modul 10). Eigene Rolle, eigener Eingangs-Kontext (Modul 8).

**Gegenstand:** `docs/plan/planning/in-progress/slice-042-llm-aktionsvorschlag-provider-adapter.md`

**Skill/Regelwerk:** `.harness/baseline/v1.4.0/regelwerk/modul-08-agentenrollen.md`,
`…/modul-11-verification.md` @ v1.4.0.

**Modell:** Claude Opus 4.8 (1M) · **Datum:** 2026-07-09

**Rollentrennung (Modul 8):** Der Verifier hat weder implementiert (Autor Codex)
noch den Code reviewt (`docs/reviews/2026-07-09-slice-042-{plan,design,code,code-safety}-review.md`);
frischer Kontext, andere Eingabe (DoD/Spec/Plan statt Plan/ADR).

**Prüfstand:** Working-Tree-Stand (Implementierung **noch uncommittet**: Modul
`adapters/outbound/llm-action-langchain4j/` untracked, Integrationsdateien
`.a-check.yml`/`Dockerfile`/`settings.gradle.kts`/`integration.md`/`roadmap.md`
modifiziert). Die Docker-Gates bauen den Working-Tree-Kontext, verifizieren also
genau diesen Stand; der Commit steht aus (Handoff unten).

---

## Eingangs-Kontext

- `spec/lastenheft.md`: `LH-FA-LLM-001/002/003/004`, `LH-FA-ACT-001..004`, `LH-FA-POL-006`, `LH-QA-02/03/04`
- `spec/architecture.md`: `ARC-03`, `ARC-07`, `ARC-08`, `ARC-09`
- `docs/plan/adr/0001…`, `0002…`, `0003…`, `0006-coverage-gate-scope.md`
- `docs/reviews/2026-07-09-slice-042-{plan,design,code,code-safety}-review.md`
- `adapters/outbound/llm-action-langchain4j/**` (Impl. + Test + `build.gradle.kts`)
- `hexagon/application/.../aktionsvorschlag/ports/AktionsVorschlagsPort.kt` (Vertrag, unverändert)
- `hexagon/application/.../aktionsvorschlag/AktionsVorschlagen.kt` (Use-Case-Validierungsrand)
- `adapters/outbound/llm-action-fake/.../FakeAktionsVorschlagsPort.kt` (Parität)
- `settings.gradle.kts`, `.a-check.yml`, `Dockerfile`, `docs/user/integration.md`

---

## Verification (DoD gegen Code)

| DoD / Vertrag | Evidenz (verifiziert) | Status |
|---|---|---|
| **DoD 1** — Modul `llm-action-langchain4j` (JVM `src/main`) implementiert `AktionsVorschlagsPort` hinter `ARC-08`; Kern provider-/framework-frei; LangChain4j 1.17.1 (adoptiert) → kein Folge-ADR. | `LangChain4jAktionsVorschlagsPort : AktionsVorschlagsPort`. `build.gradle.kts:19` `dev.langchain4j:1.17.1` + `jackson-databind:2.21.3` (beide via `llm-langchain4j` bereits adoptiert). **a-check live: 0 Befunde** → Kern hat keine Kante zum Adapter, kein Framework-Import in `hexagon/*`. | **erfüllt** |
| **DoD 2** — Schicht-Trennung: **Adapter** = Wire-Integrität (erlaubte 6 Felder, Typ/Shape, endliche Zahlen; per-Vorschlag fail-closed verwerfen, valide bleiben); **Duplikate → Gesamt-Wurf**; **Semantik** bleibt im Use Case, nicht dupliziert. | `wireDefekt`: exakte Feldmenge `!= ERLAUBTE_FELDER` (fängt unbekannt **und** fehlend), `isTextual`/`isNumber && isFinite`/Array-of-textual. Defekt → `warnung()` + `null` (verworfen). `STRICT_DUPLICATE_DETECTION` + „Duplicate field"-Nachricht → `AktionsVorschlagAntwortFehler` (Gesamtantwort). Adapter mappt nur auf primitive `AktionsVorschlag`; **Test `reicht_semantisch_offene_rohwerte_...durch`** (pSuccess=5.0, unbek. Hypothese, „QUATSCH", leere Evidenz **durchgereicht**) belegt Nicht-Duplizierung. `vorschlaege(belief)` ohne Evidenz-Kontext → Adapter *kann* Evidenz nicht prüfen. | **erfüllt** |
| **DoD 3** — Fehler je Klasse: leer→`emptyList`; wire-defekter Einzelvorschlag→verworfen; Provider-Ausfall/unparsebar→**sichtbarer Wurf**, propagiert außerhalb des per-Vorschlag-`runCatching`. | `parse`: `isBlank→emptyList`; `!isArray→Wurf`; `mapNotNull{vorschlagOderVerwerfen}`. Use Case `AktionsVorschlagen.kt`: `port.vorschlaege()` steht in `ausfuehren` (**:47**, außerhalb) — der per-Vorschlag-`runCatching` ist `:57`; ein Adapter-Wurf propagiert also. Test `provider_ausfall_propagiert_sichtbar` (IllegalStateException tritt aus). „unreachable" ≠ „kein Vorschlag" (`LH-QA-02`). | **erfüllt** |
| **DoD 4** — Lokale Tests ohne Provider/Key: Normalisierung, Wire-Fehlerklassen, Prompt, Pass-Through, keine Gate-/Executor-Kopplung. | 20 Tests, Stub-`chat = { antwort }` (kein Netz/Key). Deckt: `normalisiert_gueltiges_json_array`, leer×2, `unparsebare/nicht_array/trailing×2/doppelte/provider_ausfall`, `unbekanntes_feld/fehlendes_pflichtfeld/falscher_typ/nicht_endliche_zahl/nicht_objekt/evidenz_nicht_string/json_null` (Verwurf), `reicht_semantisch_offene_rohwerte_...durch`, `prompt_enthaelt_hypothesen_und_regeln`, `sauberer_pass_meldet_keine_warnung`. Port gibt nur `List<AktionsVorschlag>` — keine `Aktionsfreigabe`/Executor. | **erfüllt** |
| **DoD 5** — Build-/Arch-/Coverage-Integration. | `settings.gradle.kts:30`; `.a-check.yml` Rolle `outbound_llm_action_langchain4j` (:37) + Kanten →application/→domain (:71-72) + Root (:122); `Dockerfile` an **allen 5** Stellen (`COPY` :32, `:dependencies` :37, `:test` :52, `:koverLog` :72, `:koverVerify` :88); `build.gradle.kts` `kover{ verify{ rule{ minBound(90) } } }`. **`make gates` grün, coverage-gate CACHED-grün inkl. `:llm-action-langchain4j:koverVerify`**. | **erfüllt** |
| **DoD 6** — Doku + Verification: nur Vorschlag (keine Freigabe/Ausführung/CLI-Default/Secrets); `doc-check`+`gates` grün; Closure-Notiz benennt zweiten Provider-Pfad. | `integration.md:337/346` beschreibt „rohe Aktionsvorschläge (nur Vorschlag, keine Freigabe/Ausführung)". **Kein Produktiv-Binding:** `.a-check.yml` bindet `inbound_cli`/`examples` nur an `outbound_llm_action_fake` (nicht den echten Adapter). Keine Secrets in Tests (Stub-Runner). `doc-check` grün. Koog-Parität als `slice-043` benannt (§4/§6/DR-F3). **Closure-Notiz: siehe Handoff** (planmäßig noch offen). | **erfüllt bis auf Closure-Notiz (siehe Handoff)** |

## Rückbindung der Review-Findings (Modul 8/10 → verifiziert im Code)

- **DR-F1 / PR-F2** (Validierungs-Schicht): Adapter=Wire, Use Case=Semantik; im Code + Pass-Through-Test bestätigt. **verifiziert.**
- **DR-F2** (Fehler-Signalisierung): leer/verwerfen/werfen je Klasse; Tests belegen. **verifiziert.**
- **DR-F3 / PR-F1** (ADR-0002-Guard / Modul-Konvergenz): LangChain4j adoptiert, kein Folge-ADR; `ADR-0002` im `Bezug`; Modul/Rolle konkret. **verifiziert.**
- **CR-F1** (Duplikat-Granularität): reklassifiziert zu Gesamt-Wurf; `STRICT_DUPLICATE_DETECTION`, Test `doppelte_json_felder_werfen`. Beide Zweige fail-closed. **verifiziert.**
- **CR-F2** (KDoc-Link): Port-KDoc ohne Dokka-Link auf Parser-Member (Port-Vertrag unverändert/schlank). **verifiziert.**
- **SR-F1** (Trailing-Token-Nachsicht, Frischkontext-Fund): `parseJson` prüft `parser.nextToken() != null` → Trailing-Müll wirft; Tests `trailing_tokens_...`/`trailing_muell_nach_leerem_array_wirft`. **verifiziert** (der sicherheitskritischste Fix — sonst `[gültig]{leak}` still verworfen).
- **SR-F2** (null-Feldwert): Test `json_null_feldwert_wird_verworfen`. **verifiziert.**
- **CR-F4 / SR-F3** (roher Transport-Typ / DoS-Grenzen): bewusst Residuum (§6); Jackson-`StreamReadConstraints`-Defaults werfen fail-closed. **als Folgearbeit akzeptiert.**

## Sensors (computational — selbst ausgeführt gegen den Working Tree)

- `make gates` — **grün, EXIT 0** (doc-check · build · test · coverage-gate · arch-check).
- `arch-check` (a-check, **live**) — `gesamt: 0 Befund(e)`: Kern provider-/framework-frei, `llm-action-langchain4j→application/domain` konform.
- `coverage-gate` — `:adapters:outbound:llm-action-langchain4j:koverVerify` eingeschlossen, Adapter-Floor **90 %** gehalten.
- `test` — `:adapters:outbound:llm-action-langchain4j:test` (20 Tests, Stub-Runner, kein Netz/API-Key).
- `doc-check` — grün.

## Verdikt

**Keine DoD-Verletzung am Code.** `slice-042` ist gegen Plan, Spec (`LH-FA-LLM-*`,
`LH-FA-ACT-*`, `LH-FA-POL-006`, `LH-QA-*`) und Architektur (`ARC-07/08`,
`ADR-0001/0002/0003/0006`) verifiziert; alle sechs DoD-Punkte sind durch Code +
20 lokale Tests + live-Sensoren belegt. Die Schicht-Trennung (Adapter=Wire,
Use Case=Semantik) ist scharf und testbar; der Adapter erzeugt keine Freigabe,
öffnet keinen Executor-Pfad und ist nicht produktiv gebunden (`LH-FA-POL-006`).

## Handoff an Planner/Implementation (Modul 8)

- **Commit ausstehend:** Der verifizierte Stand liegt **uncommittet** im Working
  Tree (Modul untracked, Integrationsdateien modifiziert, Review-Docs untracked).
  Für die auditierbare Kette (Modul 1) muss der Closure-Commit **genau diesen**
  Stand festhalten; ein erneuter `make gates`-Lauf nach dem Commit ist ratsam
  (Working Tree == committeter Stand).
- **Offener Closure-Schritt (§5):** Closure-Notiz (§7) noch Platzhalter — kein
  DoD-Verletzung, sondern planmäßige Sequenz (erst Verifikation, dann
  Closure-Notiz, dann `done/`). Sie soll den zweiten Provider-Pfad benennen
  (Antwort steht: **slice-043** Koog-Parität).

## Verbleibende Risiken / Out-of-Scope

- Zweiter Framework-Pfad (Koog): `slice-043` (Parität), nicht dieser Slice.
- Transport-Fehler-Typ-Unterscheidbarkeit („unreachable" ≠ intern) und explizites
  Pinnen der Jackson-`StreamReadConstraints` (DoS-Grenzen): bewusstes Residuum
  (§6, CR-F4/SR-F3), fällig sobald ein Slice die Vorschläge-Konsumenten geordnet
  eskalieren lässt.
- Provider-/Modellwahl, API-Keys, Live-Netztests, CLI-Default-Binding: außerhalb
  dieses Slice (Composition-Root/Folgeslice).
