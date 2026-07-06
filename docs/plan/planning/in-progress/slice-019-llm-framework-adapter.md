# Slice slice-019: LLM-Framework-Adapter — LangChain4j + Koog

**Status:** open → next → in-progress → done (siehe [Planning-README](../README.md)).

**Welle:** `welle-05-llm-port` ([Roadmap](roadmap.md)).

**Bezug:** `LH-FA-LLM-001`, `LH-FA-LLM-002`, `LH-FA-LLM-003`,
`LH-FA-LLM-004`, `LH-QA-03`, `LH-QA-04`; `ADR-0001`, `ADR-0003`,
`ADR-0006`; `ARC-07`, `ARC-08`.

**Autor:** pt9912. **Datum:** 2026-07-06.

---

## 1. Ziel

Zwei echte JVM-Outbound-Adapter binden LLM-Frameworks hinter den bestehenden
`LlmPort`: `llm-langchain4j` nutzt LangChain4j `ChatModel`, `llm-koog` nutzt Koog
`LLMClient` oder `PromptExecutor`. Beide Adapter holen ausschliesslich
strukturierte Likelihoods ein: `belief-agent` orchestriert und gated, das
Framework liefert nur explizite Einschaetzungen.

Der Slice liefert bewusst keinen Provider-spezifischen Composition-Root und
keine Aktionsausfuehrung. API-Keys, Modellwahl und Tool-Ausfuehrung bleiben am
Integrationsrand; Gate und menschliche Freigabe bleiben im Core.

## 2. Definition of Done

- [ ] `LH-FA-LLM-001`/`004`: LangChain4j und Koog sind austauschbare
      Outbound-Adapter hinter `LlmPort`; `hexagon:*` importiert keine
      Framework-Pakete (`arch-check` gruen).
- [ ] `LH-FA-LLM-002`/`003`: Adapter fordern strukturierte
      `proHypothese`-/`resthypothese`-Likelihoods an, validieren exakt die
      bekannten Hypothesen und weisen unvollstaendige, unbekannte oder
      nicht-endliche Werte fail-closed zurueck.
- [ ] LangChain4j-Adaptermodul `adapters:outbound:llm-langchain4j` mit
      `LangChain4jLlmPort.fromChatModel(...)`, JSON-Parser und lokalen Tests
      ohne Provider/API-Key.
- [ ] Koog-Adaptermodul `adapters:outbound:llm-koog` mit
      `KoogLlmPort.fromLlmClient(...)` und `KoogLlmPort.fromPromptExecutor(...)`,
      JSON-Parser und lokalen Tests ohne Provider/API-Key.
- [ ] Build-Metadaten verdrahtet (`settings.gradle.kts`, `Dockerfile`,
      `.a-check.yml`); per-Modul-Coverage-Gate fuer beide Adapter (`ADR-0006`).
- [ ] Nutzer-Doku aktualisiert: `docs/user/integration.md` beschreibt
      LangChain4j und Koog als LLM-Adapter-Optionen; Beispielstatus bleibt klar.
- [ ] `make gates` gruen.

## 3. Plan (vor Code)

| Datei / Komponente | Änderungs-Art | Begründung |
|---|---|---|
| `adapters/outbound/llm-langchain4j/**` | neu | LangChain4j-Adapter hinter `LlmPort` (`ARC-08`, `LH-FA-LLM`) |
| `adapters/outbound/llm-koog/**` | neu | Koog-Adapter hinter `LlmPort` via `LLMClient`/`PromptExecutor` (`ARC-08`, `LH-FA-LLM`) |
| `settings.gradle.kts`, `Dockerfile`, `.a-check.yml` | update | Module in Build, Coverage und Arch-Gate einhaengen (`ADR-0003`, `ADR-0006`, `MR-005`) |
| `docs/user/integration.md` | update | Einbaupfad fuer echte LLM-Framework-Adapter dokumentieren |
| `docs/plan/planning/in-progress/roadmap.md` | update | Welle 05 aktivieren und Slice-Spur herstellen |

## 4. Trigger

`slice-018` done; Roadmap-Resume-Punkt fuer `welle-05` erreicht. Der lokale
`LlmPort` existiert seit `slice-009`, der Entscheidungszyklus seit `slice-017`.

## 5. Closure-Trigger

DoD vollstaendig + Closure-Notiz; Datei bleibt bis zur Welle-05-Closure in
`in-progress/` und wandert danach nach `done/`.

## 6. Risiken und offene Punkte

- Framework-APIs sind versionssensitiv: Adapter muessen gegen echte
  LangChain4j-/Koog-Artefakte kompilieren, nicht nur gegen Skizzen.
- LLM-Ausgaben sind unzuverlaessig: Parser und Mapping muessen fail-closed sein;
  kein Freitext darf ungeprueft in `Likelihoods` uebernommen werden.
- Dieser Slice liefert noch keine Modellkalibrierung, kein Golden-Set und keinen
  produktiven CLI-Composition-Root. Diese Punkte bleiben Folge-Slices in Welle 05.
- Provider-spezifische API-Keys duerfen nicht in Tests oder Doku-Beispiele
  geraten; Tests nutzen lokale Runner/Stubs.

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss füllen. -->

## 8. Sub-Area-Modus-Begründung

Neue Sub-Areas `adapters/outbound/llm-langchain4j`, `adapters/outbound/llm-koog`
und `docs/user` — GF (frisch angelegt beziehungsweise erstmals als Nutzer-Doku
gefuehrt, Spec/Architektur fuehren vor Code). Modus-Deklaration siehe
[`../../../../harness/conventions.md`](../../../../harness/conventions.md)
§Modus-Deklaration pro Sub-Area.
