# Integration — belief-agent

**Status:** v0, intern. **Letzte Änderung:** 2026-07-07.

Diese Seite beschreibt, wie der aktuell vorhandene Core von `belief-agent` in
einem Kotlin-Multiplatform-Build eingebaut wird. Sie ist kein Release- oder
Stabilitätsversprechen: veröffentlichte Maven-/Gradle-Koordinaten und ein
produktiver CLI-Composition-Root sind noch nicht vorhanden. Erste JVM-Adapter fuer
LLM-Frameworks liegen als Repo-Module vor; provider-spezifische API-Keys und
Modellwahl bleiben Aufgabe des integrierenden Systems.

## 1. Integrationsmodell

`belief-agent` ist im aktuellen Stand eine Bibliothek aus Core-Modulen und
Adapter-Modulen:

| Modul | Rolle |
|---|---|
| `hexagon:domain` | fachliche Typen und reine Regeln: `BeliefState`, `BayesUpdate`, `KonfidenzGate`, `Budget`, `VoiSelektor` |
| `hexagon:application` | Use Cases und Ports: `BeliefAktualisieren`, `AktionsVorschlagen`, `AktionGaten`, `BeobachtungWaehlen`, `Entscheidungszyklus`, `KonfidenzgebundenerEntscheidungszyklus` |
| `adapters:outbound:*` | Beispiel-/Fake-Adapter fuer LLM, Aktionsvorschlaege, Beobachtung, Audit, Approval, VoI-Kandidaten und Konfidenz-Replay |
| `adapters:outbound:llm-langchain4j` | JVM-Adapter fuer LangChain4j `ChatModel` hinter `LlmPort` |
| `adapters:outbound:llm-koog` | JVM-Adapter fuer Koog `LLMClient` oder `PromptExecutor` hinter `LlmPort` |

Die Architektur ist hexagonal: Der Core definiert die Ports, Adapter
implementieren sie. Der Core importiert keine Adapter. Die Verdrahtung liegt
beim integrierenden System beziehungsweise spaeter beim CLI-Composition-Root.

Relevante Quellen:

- [`spec/architecture.md`](../../spec/architecture.md) beschreibt Core, Ports,
  Adapter und Abhaengigkeitsrichtung.
- [`docs/plan/adr/0001-hexagonal-llm-port.md`](../plan/adr/0001-hexagonal-llm-port.md)
  begruendet Ports & Adapter.
- [`docs/plan/adr/0003-hexslice-architektur.md`](../plan/adr/0003-hexslice-architektur.md)
  beschreibt die Modul-/Slice-Struktur.

## 2. Voraussetzungen

Der aktuelle Einbau ist nur fuer einen Build gedacht, der die Module direkt
einbindet, zum Beispiel innerhalb dieses Repos oder ueber einen Gradle
Composite Build. Es gibt noch keine veroeffentlichten Artefakt-Koordinaten.

In einem Modul, das den Core verdrahtet, werden mindestens diese Projektmodule
benoetigt:

```kotlin
dependencies {
    implementation(project(":hexagon:domain"))
    implementation(project(":hexagon:application"))

    // Optional: deterministische Stand-ins fuer Integrationstests oder Demos.
    implementation(project(":adapters:outbound:llm-fake"))
    implementation(project(":adapters:outbound:observation-fake"))
    implementation(project(":adapters:outbound:approval-fake"))
    implementation(project(":adapters:outbound:audit-memory"))
    implementation(project(":adapters:outbound:voi-fake"))
    implementation(project(":adapters:outbound:konfidenz-memory"))
    implementation(project(":adapters:outbound:llm-action-fake"))

    // Optional: echte LLM-Framework-Adapter. Provider-Konfiguration bleibt extern.
    implementation(project(":adapters:outbound:llm-langchain4j"))
    implementation(project(":adapters:outbound:llm-koog"))
}
```

Produktive Adapter sollten eigene Module sein, die die Ports aus
`hexagon:application` implementieren. Sie duerfen den Core nutzen, aber der Core
darf sie nicht importieren.

## 3. Ports Implementieren

Adapter binden an diese Ports:

| Port | Zweck |
|---|---|
| `LlmPort` | schaetzt Likelihoods `P(Evidenz | Hypothese)` fuer die Update-Pipeline |
| `AktionsVorschlagsPort` | liefert strukturierte rohe Aktionsvorschlaege fuer bekannte Hypothesen |
| `BeobachtungsPort` | liefert Beobachtungen aus Tests, Build, Logs, Mensch oder Repo |
| `BeobachtungsAuswahlPort` | liefert belief-abhaengige VoI-Kandidaten fuer die naechste Beobachtung |
| `KonfidenzPort` | speichert und laedt append-only Modell-Konfidenzen fuer eine fachliche Referenz |
| `HumanApprovalPort` | holt menschliche Freigabe fuer irreversible Aktionen ein |
| `AuditPort` | persistiert das append-only `EreignisProtokoll` |
| `UhrPort` | liefert monotone Zeitstempel fuer deterministische Ereignisse |

Minimaler Uhr-Adapter:

```kotlin
import dev.beliefagent.application.belief.aktualisieren.ports.UhrPort
import dev.beliefagent.domain.belief.Zeitstempel

class FesteUhr(private val zeitstempel: Zeitstempel) : UhrPort {
    override fun jetzt(): Zeitstempel = zeitstempel
}
```

Ein produktiver `UhrPort` muss monotone, nicht fallende Zeitstempel liefern,
weil das Ereignisprotokoll Rueckdatieren zurueckweist.

`BeobachtungsAuswahlPort.kandidaten(belief: BeliefState)` erhaelt den
aktuellen Belief-Kontext. Adapter duerfen daraus andere strukturierte
`VoiKandidat`-Listen ableiten, zum Beispiel anhand der Top-2-Hypothesen. Die
VoI-Entscheidung selbst bleibt im Core: der Adapter liefert Beobachtung,
`erwarteteDiskriminierung` und Kosten; `BeobachtungWaehlen` waehlt danach
deterministisch ueber den Domain-Selektor. LLM-beeinflusste Werte muessen
explizit als Zahlen im Kandidaten stehen und duerfen keine stillen Defaults
verwenden.

`KonfidenzPort` ist der Contract fuer `LH-FA-LLM-003`: Modell-Konfidenz wird
als expliziter Zahlenwert mit Referenz, Quelle und Version externalisiert.
Overrides ueberschreiben keine alten Werte, sondern erscheinen als neue
append-only Versionen mit Begruendung. Ein Adapter muss beim Laden eine
konsistente Historie fuer die angefragte Referenz liefern; ungueltige Fixtures
oder Luecken duerfen nicht still korrigiert werden.

`KonfidenzgebundenerEntscheidungszyklus` konsumiert diese Historie am
Application-Rand. Er verwendet die neueste gueltige Version als
`Erfolgswahrscheinlichkeit` einer `KonfidenzgebundeneAktion` und ruft danach
den normalen `Entscheidungszyklus` mit `AktionGaten` auf. Fehlt eine gueltige
externalisierte Konfidenz, ist die Aktion nicht gate-faehig; der Zyklus
handelt fail-safe nicht. Das ersetzt keinen Aktionsvorschlags-Port und
erweitert `AktionGaten` nicht um LLM- oder Adapterwissen.

`AktionsVorschlagsPort` ist eine eigene LLM-Aufgabe. Ein Adapter liefert rohe
Vorschlaege mit Beschreibung, bekannter Hypothese, Wirkungsklasse,
`p_success`, Konfidenzreferenz und Evidenzreferenzen. `AktionsVorschlagen`
validiert diese Werte gegen den aktuellen `BeliefState` und einen
Evidenzindex, externalisiert `p_success` ueber `KonfidenzPort` und gibt nur
`KonfidenzgebundeneAktion`en zurueck. Der Use Case ruft kein Gate auf, erzeugt
keine `Aktionsfreigabe` und fuehrt nichts aus; kaputte oder unvollstaendige
Vorschlaege werden verworfen.

## 4. Core Verdrahten

Das integrierende System erzeugt die Ports/Adapter und gibt sie an die Use Cases
weiter. Der folgende Code nutzt die vorhandenen Fake-Adapter und zeigt den
aktuellen Einbau:

```kotlin
import dev.beliefagent.adapter.approval.FakeApproval
import dev.beliefagent.adapter.llm.FakeLlm
import dev.beliefagent.adapter.voi.FakeKandidatenquelle
import dev.beliefagent.application.belief.aktualisieren.BeliefAktualisieren
import dev.beliefagent.application.belief.aktualisieren.ports.UhrPort
import dev.beliefagent.application.belief.beobachtungwaehlen.BeobachtungWaehlen
import dev.beliefagent.application.belief.entscheidungszyklus.Entscheidungszyklus
import dev.beliefagent.application.belief.entscheidungszyklus.Zyklusergebnis
import dev.beliefagent.application.belief.gaten.AktionGaten
import dev.beliefagent.domain.belief.Aktion
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Erfolgswahrscheinlichkeit
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Resthypothese
import dev.beliefagent.domain.belief.Wirkungsklasse
import dev.beliefagent.domain.belief.Zeitstempel
import dev.beliefagent.domain.eskalation.Budget
import dev.beliefagent.domain.voi.VoiKandidat

val prior = BeliefState.of(
    listOf(
        Hypothese(HypotheseId("regression"), 0.4),
        Hypothese(HypotheseId("flaky"), 0.4),
    ),
    Resthypothese(0.2),
)

val stuetzendeEvidenz = Beobachtung(
    Quelle.REPO,
    Zeitstempel(1L),
    Evidenz("regression moeglich"),
)
val naechsteBeobachtung = Beobachtung(
    Quelle.LOG,
    Zeitstempel(2L),
    Evidenz("regression bestaetigt"),
)

val aktion = Aktion(
    beschreibung = "Release ausloesen",
    wirkungsklasse = Wirkungsklasse.EXTERN_WIRKSAM,
    erfolgswahrscheinlichkeit = Erfolgswahrscheinlichkeit(0.96),
    stuetzendeEvidenz = listOf(stuetzendeEvidenz),
)

val uhr = object : UhrPort {
    override fun jetzt(): Zeitstempel = Zeitstempel(2L)
}

val zyklus = Entscheidungszyklus(
    beobachtungWaehlen = BeobachtungWaehlen(
        FakeKandidatenquelle(
            listOf(
                VoiKandidat(
                    beobachtung = naechsteBeobachtung,
                    erwarteteDiskriminierung = 0.7,
                    kosten = 1.0,
                ),
            ),
        ),
    ),
    beliefAktualisieren = BeliefAktualisieren(FakeLlm(), uhr),
    aktionGaten = AktionGaten(FakeApproval(freigabe = true)),
)

when (val ergebnis = zyklus.entscheide(aktion, prior, Budget(maxSchritte = 3))) {
    is Zyklusergebnis.Gehandelt -> {
        // Nur in diesem Zweig darf ein Executor die Aktion ausfuehren.
        val freigegebeneAktion = ergebnis.freigabe.aktion
    }
    is Zyklusergebnis.Eskaliert -> {
        // Menschliche Entscheidung mit Kontext: Belief, Evidenz und Grund.
        val eskalation = ergebnis.eskalation
    }
    is Zyklusergebnis.Abgelehnt -> {
        // Nicht handeln; keine Eskalation erforderlich.
        val grund = ergebnis.grund
    }
}
```

## 5. LLM-Frameworks und LangChain-Basierte Tools Anbinden

Direkte JVM-Adapter sind als eigene Outbound-Module vorhanden:

| Modul | Framework-Grenze | Zweck |
|---|---|---|
| `adapters:outbound:llm-langchain4j` | `LangChain4jLlmPort.fromChatModel(chatModel)` | `ChatModel` liefert strukturierte Likelihoods |
| `adapters:outbound:llm-koog` | `KoogLlmPort.fromLlmClient(client, model)` | Koog `LLMClient` liefert strukturierte Likelihoods |
| `adapters:outbound:llm-koog` | `KoogLlmPort.fromPromptExecutor(executor, model)` | Koog `PromptExecutor` liefert strukturierte Likelihoods |

Alle drei Einstiegspunkte bleiben hinter `LlmPort`. Der Adapter erstellt den
Prompt, liest JSON mit `proHypothese` und `resthypothese`, validiert exakt die
bekannten Hypothesen aus dem `BeliefState` und gibt nur `Likelihoods` an die
Update-Pipeline weiter. Unvollstaendige, unbekannte oder nicht-endliche Werte
werden zurueckgewiesen; dadurch entsteht keine Freigabe und kein
Aktionsausfuehrungspfad.

Der Integrationssatz bleibt:

```text
belief-agent orchestriert, LangChain4j/Koog liefert strukturierte Einschaetzungen.
```

Wenn ein vorhandenes Tool intern LangChain nutzt, bleibt die Grenze dieselbe:
`belief-agent` orchestriert, LangChain liefert strukturierte Einschaetzungen.
Das LangChain-Tool wird als Outbound-Abhaengigkeit hinter einem Port
angebunden, nicht als Ersatz fuer den Entscheidungszyklus.

```text
belief-agent Core
  Entscheidungszyklus
    -> Port aus hexagon:application
      -> adapters/outbound/langchain-tool
        -> HTTP / CLI / MCP / gRPC
          -> LangChain-basiertes Tool
```

Geeignete Rollen fuer ein LangChain-basiertes Tool:

| Rolle | Port | Rueckgabe |
|---|---|---|
| Likelihood-Schaetzung | `LlmPort` | `Likelihoods` je Hypothese plus Resthypothese |
| Evidenzsammlung | `BeobachtungsPort` | `List<Beobachtung>` |
| Auswahl naechster Beobachtungen | `BeobachtungsAuswahlPort` | `BeliefState -> List<VoiKandidat>` |
| Aktionsvorschlag | `AktionsVorschlagsPort` + `AktionsVorschlagen` | `KonfidenzgebundeneAktion`, niemals Freigabe oder Ausfuehrung |

Das Tool sollte strukturierte Daten liefern. Freitext wird am Adapter-Rand
validiert und erst danach in Domänentypen uebersetzt.

Beispiel fuer eine `LlmPort`-Antwort des Tools:

```json
{
  "proHypothese": {
    "regression": 0.85,
    "flaky": 0.15
  },
  "resthypothese": 0.2
}
```

Der Adapter prueft mindestens:

- alle Hypothesen aus dem `BeliefState` sind enthalten,
- keine unbekannten Hypothesen werden geliefert,
- alle Werte sind endlich und `>= 0`,
- die Resthypothese ist enthalten,
- ungueltige oder unvollstaendige Antworten fuehren nicht zu einer Freigabe.

LangChain-basierte Tools können weiterhin externe Tool-Schnittstellen (HTTP/CLI/MCP/etc.)
verwenden. In dieser Basisimplementierung ist die produktive Integration bereits als
`LangChain4jLlmPort` umgesetzt (`adapters:outbound:llm-langchain4j`) und sollte
als Referenz für reale LLM-Anbindungen genutzt werden.

Ein LangChain-Tool darf Aktionen vorschlagen, aber nicht selbst ausfuehren. Die
Ausfuehrung bleibt an `Aktionsfreigabe.Freigegeben` gebunden:

```text
LangChain-Tool: "Deploy vorschlagen, p_success=0.96"
belief-agent:  Belief aktualisieren, Gate pruefen, ggf. Freigabe einholen
Executor:      nur bei Aktionsfreigabe.Freigegeben ausfuehren
```

Diese Grenze ist sicherheitsrelevant. Sobald das LangChain-Tool selbst
extern-wirksame Tools ausfuehrt, laeuft es am Konfidenz-Gate vorbei und ist
keine gueltige `belief-agent`-Integration.

Ein konkretes, im Build verdrahtetes Beispiel liegt unter
[`example/langchain`](../../example/langchain/README.md). Das entsprechende
Koog-Beispiel liegt unter [`example/koog`](../../example/koog/README.md).

## 6. Sicherheitsregeln fuer Executor

Ein Executor darf Aktionen nur ausfuehren, wenn er eine
`Aktionsfreigabe.Freigegeben` aus dem application-Schritt `AktionGaten` oder aus
dem `Entscheidungszyklus` erhaelt. Eine direkte
`GateEntscheidung.Freigabe` aus der Domänenregel `KonfidenzGate` ist dafuer
nicht ausreichend, weil dort die menschliche Freigabe fuer irreversible Aktionen
nicht enthalten ist.

Extern-wirksame Aktionen brauchen immer beides:

1. die harte Konfidenzschwelle,
2. explizite menschliche Freigabe ueber `HumanApprovalPort`.

Bei hoher Resthypothese wird nicht gehandelt, sondern gesammelt oder eskaliert.
Bei erschoepftem Budget eskaliert der Zyklus fail-safe.

## 7. Audit Anbinden

`BeliefAktualisieren` liefert Ereignisse im Ergebnis. Ein Integrator persistiert
sie ueber `AuditPort`:

```kotlin
import dev.beliefagent.adapter.audit.MemoryAudit
import dev.beliefagent.application.belief.aktualisieren.BeliefAktualisieren
import dev.beliefagent.application.belief.aktualisieren.BeliefAktualisierenBefehl

val audit = MemoryAudit()
val update = BeliefAktualisieren(llm, uhr)
    .ausfuehren(BeliefAktualisierenBefehl(prior, listOf(naechsteBeobachtung)))

update.ereignisse.forEach(audit::anhaengen)
```

`MemoryAudit` ist nur ein In-Memory-Adapter. Ein produktiver Adapter muss das
append-only-Verhalten erhalten und darf vergangene Ereignisse nicht
ueberschreiben.

## 8. Noch Nicht Stabil

Diese Punkte sind bewusst noch nicht als Nutzervertrag festgelegt:

- Artefakt-Koordinaten und Release-Prozess.
- Produktiver CLI-Composition-Root.
- Provider-spezifische LLM-Composition mit API-Key-/Modell-Konfiguration und
  belief-abhaengige VoI-Kandidaten.
- Echter Approval-Adapter mit einmaliger, an den Entscheidungskontext gebundener
  Freigabe.
- Dauerhafte Audit-Persistenz.

Die Spezifikation markiert externe Port-Vertraege derzeit als `v0 (intern)`.
Integrationen sollten deshalb auf Modul-/Quellstand pinnen und API-Aenderungen
bis zur Stabilisierung einkalkulieren.

## 9. Lokale Pruefung

Nach Aenderungen an Integrationscode oder Adaptervertraegen sollen die Gates
ueber `make` laufen:

```sh
make doc-check
make gates
make example-langchain
make example-koog
```

`make gates` umfasst Dokumentations-, Build-, Test-, Coverage- und
Architekturpruefungen.
