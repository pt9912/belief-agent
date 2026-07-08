# Integration — belief-agent

**Status:** v0, intern. **Letzte Änderung:** 2026-07-08.

Diese Seite beschreibt, wie der aktuell vorhandene Core von `belief-agent` in
einem Kotlin-Multiplatform-Build eingebaut wird. Sie ist kein Release- oder
Stabilitätsversprechen: veröffentlichte Maven-/Gradle-Koordinaten sind noch
nicht vorhanden. Ein produktiv gedachter CLI-Composition-Root liegt als
Repo-Modul vor und laeuft netzfrei gegen deterministische Adapter; echte
Provider-Konfiguration, API-Keys und Modellwahl bleiben Aufgabe des
integrierenden Systems.

## 1. Integrationsmodell

`belief-agent` ist im aktuellen Stand eine Bibliothek aus Core-Modulen und
Adapter-Modulen:

| Modul | Rolle |
|---|---|
| `hexagon:domain` | fachliche Typen und reine Regeln: `BeliefState`, `BayesUpdate`, `KonfidenzGate`, `Budget`, `VoiSelektor` |
| `hexagon:application` | Use Cases und Ports: `BeliefAktualisieren`, `AktionsVorschlagen`, `AktionGaten`, `BeobachtungWaehlen`, `Entscheidungszyklus`, `KonfidenzgebundenerEntscheidungszyklus` |
| `adapters:inbound:cli` | Koin-basierter `ARC-09`-Composition-Root, CLI-Einstieg und Executor-Grenze |
| `adapters:outbound:*` | Beispiel-/Fake-Adapter fuer LLM, Aktionsvorschlaege, Beobachtung, Audit, Approval, VoI-Kandidaten und Konfidenz-Replay |
| `adapters:outbound:llm-langchain4j` | JVM-Adapter fuer LangChain4j `ChatModel` hinter `LlmPort` |
| `adapters:outbound:llm-koog` | JVM-Adapter fuer Koog `LLMClient` oder `PromptExecutor` hinter `LlmPort` |

Die Architektur ist hexagonal: Der Core definiert die Ports, Adapter
implementieren sie. Der Core importiert keine Adapter. Die Verdrahtung liegt
beim integrierenden System oder im `adapters:inbound:cli`-Composition-Root.
Nur dieser Root darf Outbound-Adapter an Ports binden; fachliche
Adapter-zu-Adapter-Kopplung bleibt verboten und wird durch `a-check` getrennt.

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
    implementation(project(":adapters:inbound:cli"))

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

Der vorhandene CLI-Composition-Root kann im Repo netzfrei gestartet werden:

```sh
make cli-demo
```

Der Demo-Lauf nutzt deterministische Fake-Adapter und gibt ein terminales
CLI-Ergebnis aus. Ohne Argument bleibt der Default der positive Pfad
`gehandelt`.

```text
scenario=gehandelt
terminal=gehandelt
executed=true
executor_boundary=Zyklusergebnis.Gehandelt.freigabe.aktion
```

Die vorzeigbare Unsicherheitsgrenze laeuft ueber alle deterministischen
Szenarien:

```sh
make cli-demo-scenarios
```

Der Lauf enthaelt negative Pfade, in denen der Executor geschlossen bleibt:

```text
scenario=eskaliert
terminal=eskaliert
executed=false
reason=GateEskalation
executor_boundary=closed

scenario=abgelehnt
terminal=abgelehnt
executed=false
executor_boundary=closed
```

Für die vollständige Datenspur je Szenario (Inputwerte, Gate-Berechnungen,
Sammelpfad und Ausführungsgrenze) siehe
[`CLI-Entscheidungsnachweis`](./cli-entscheidungsnachweis.md).

Der CLI-Root kann den Approval-Adapter bewusst waehlen:

```sh
./gradlew :adapters:inbound:cli:run --args='eskaliert approval=local'
```

Ohne `approval=local` bleiben die Szenarien bei ihrer expliziten
Fake-Konfiguration. `approval=local` waehlt im CLI-Composition-Root den Kanal
`local`; dieser Kanal bindet `LocalApproval` an den `HumanApprovalPort`: Nonce,
Kontext-Digest, Identitaet und die Bestaetigung `FREIGEBEN` muessen zur
angezeigten Anfrage passen. EOF, leere/falsche Eingabe oder wiederverwendete
Nonce bleiben fail-closed und fuehren nicht aus.

Die Kanalwahl ist nur CLI-Composition-Konfiguration. Der Core sieht weiterhin
nur `HumanApprovalPort`; konkrete Kanalnamen sind kein Port-Vertrag.
`local` ist aktuell der einzige konkrete Kanal. Unbekannte Kanaele,
nicht konfigurierte Kanaele und Kanalfehler gelten als verweigerte Freigabe.
Remote-/UI-Kanaele und persistenter Approval-Audit bleiben Folgeslices.

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

Für die vorhandene Koin-Verdrahtung kann das CLI-Modul direkt genutzt werden:

```kotlin
import dev.beliefagent.adapter.cli.CliRuntime
import dev.beliefagent.adapter.cli.StandardCliSzenarien

val ergebnis = CliRuntime
    .ausKonfiguration(StandardCliSzenarien.gehandelt())
    .starte()

check(ergebnis.terminal == CliTerminal.GEHANDELT)
check(ergebnis.executor.ausgefuehrt)
```

Die Runtime verbindet `AktionsVorschlagen`, `KonfidenzPort`,
`KonfidenzgebundenerEntscheidungszyklus`, `BeobachtungWaehlen`,
`BeliefAktualisieren`, `AktionGaten`, `HumanApprovalPort` und den Executor.
Der Executor fuehrt nur bei `Zyklusergebnis.Gehandelt` aus und konsumiert
dabei ausschliesslich `freigabe.aktion`.

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

Im CLI-Modul liegt diese Grenze in `CliExecutor`: `Gehandelt` fuehrt genau die
in `freigabe.aktion` enthaltene Aktion aus; `Eskaliert` und `Abgelehnt` bleiben
ohne Ausfuehrungsadapter-Aufruf. Die Contract-Tests decken diese positiven und
negativen Pfade ab.

Extern-wirksame Aktionen brauchen immer beides:

1. die harte Konfidenzschwelle,
2. explizite menschliche Freigabe ueber `HumanApprovalPort`.

Der `HumanApprovalPort` erhaelt dafuer eine `ApprovalAnfrage` aus der konkreten
`Aktion` und dem aktuellen `BeliefState`. Ein echter Adapter muss diese Anfrage
als Entscheidungs-Kontext behandeln; Nonce, Identitaet und Einmaligkeit sind
Teil des lokalen Adaptervertrags. Der CLI-Composition-Root bindet Approval ueber
eine fail-closed Kanalwahl: `approval=local` waehlt den einzigen aktuell
nutzbaren Kanal, unbekannte oder ungebundene Kanaele geben nicht frei, und ein
Kanalfehler wird nicht als Zustimmung interpretiert. Der Fake bleibt der
deterministische Szenario-/Default-Pfad.

Bei hoher Resthypothese wird nicht gehandelt, sondern gesammelt oder eskaliert.
Bei erschoepftem Budget eskaliert der Zyklus fail-safe.

## 7. Code-Agent einbauen

In einem Coding-Agenten wird `belief-agent` als Entscheidungs-Controller eingesetzt:

1. Beobachtungen sammeln (`BeobachtungsPort` → `Beobachtung`)
2. Belief aktualisieren (`BeliefAktualisieren`)
3. Aktionsvorschlaege holen (`AktionsVorschlagsPort`)
4. `AktionsVorschlagen` aufrufen (Validierung + Konfidenz-Externalisierung)
5. `KonfidenzgebundenerEntscheidungszyklus.entscheide(...)`
6. Nur bei `Gehandelt` durch den Executor ausfuehren

```mermaid
flowchart TD
    A[Code-Agent Step Start] --> B[Beobachtungen einsammeln]
    B --> C[BeliefAktualisieren(BeliefState, Beobachtungen)]
    C --> D[AktionsVorschlagen(AktionsVorschlagPort)]
    D --> E[KonfidenzgebundenerEntscheidungszyklus]
    E -->|Gehandelt| F[Executor: Aktion nur hier ausfuehren]
    E -->|Eskaliert| G[Human Review + Kontext (Belief, Evidenz, Grund)]
    E -->|Abgelehnt| H[Keine Aktion, no-op/weitersammeln]
```

```kotlin
class CodeAgentController(
    private val vorschlaege: AktionsVorschlagen,
    private val zyklus: KonfidenzgebundenerEntscheidungszyklus,
    private val uhr: UhrPort,
    private val evidenceIndex: Map<EvidenzReferenz, Beobachtung>,
    private val budget: Budget,
    private val execute: (Aktion) -> Unit,
    private val escalate: (Eskalation) -> Unit,
) {
    fun step(prior: BeliefState): BeliefState {
        val priorisierte = vorschlaege.ausfuehren(
            AktionsVorschlagenBefehl(
                belief = prior,
                bekannteEvidenz = evidenceIndex,
                zeitstempel = uhr.jetzt(),
            ),
        )

        val vorschlag = priorisierte.firstOrNull() ?: return prior
        return when (val ergebnis = zyklus.entscheide(vorschlag.aktion, prior, budget)) {
            is Zyklusergebnis.Gehandelt -> {
                execute(ergebnis.freigabe.aktion)
                ergebnis.belief
            }
            is Zyklusergebnis.Eskaliert -> {
                escalate(ergebnis.eskalation)
                ergebnis.eskalation.belief
            }
            is Zyklusergebnis.Abgelehnt -> ergebnis.belief
        }
    }
}
```

## 7.1 Konkrete Port-Zuordnung im aktuellen Stand

Aktuelle konkrete Implementierungen im Repo (noch nicht alles produktiv):

| Port | Konkrete Implementierung im Repo | Bemerkung |
|---|---|---|
| `AktionsVorschlagsPort` | `dev.beliefagent.adapter.llmaction.FakeAktionsVorschlagsPort` | Fake-Port für strukturiertes Rohvorschlag-Mapping |
| `BeobachtungsAuswahlPort` | `dev.beliefagent.adapter.voi.FakeKandidatenquelle` | Deterministischer VOI-Port |
| `HypothesenPort` | `dev.beliefagent.adapter.llmhypothesen.FakeHypothesenPort` | Deterministischer Re-Hypothesen-Port |
| `HumanApprovalPort` | `dev.beliefagent.adapter.approval.FakeApproval`; `dev.beliefagent.adapter.approvallocal.LocalApproval` | Fake bleibt CLI-Default; lokaler Adapter bindet `ApprovalAnfrage` an Nonce, Identitaet und Kontext-Digest |
| `KonfidenzPort` | `dev.beliefagent.adapter.konfidenz.MemoryKonfidenzPort` | In-Memory, persistenznah (Replay) |
| `AuditPort` | `dev.beliefagent.adapter.audit.MemoryAudit` | In-Memory, append-only |
| `LlmPort` | `dev.beliefagent.adapter.llm.langchain4j.LangChain4jLlmPort` / `dev.beliefagent.adapter.llm.koog.KoogLlmPort` | echte LLM-Provider-Boundary für Likelihoods |

Wichtig: Für produktive Ausführung sind `HumanApprovalPort`, persistente
`KonfidenzPort`/`AuditPort` und die anderen vier Ports (außer `LlmPort`) aktuell
noch als Fake-/Memory- oder lokal-injizierbare Adapter im Repo enthalten.
`LocalApproval` ist bewusst nur ueber die CLI-Kanalwahl `approval=local`
gebunden; weitere Approval-Kanaele sind noch nicht implementiert.

```kotlin
val actionPort: AktionsVorschlagsPort = FakeAktionsVorschlagsPort(config.aktionsVorschlaege)
val voiPort: BeobachtungsAuswahlPort = FakeKandidatenquelle(config.voiKandidaten)
val hypothesisPort: HypothesenPort = FakeHypothesenPort()
val approvalPort: HumanApprovalPort = FakeApproval(freigabe = true)
val konfidenzPort: KonfidenzPort = MemoryKonfidenzPort.leer()
val auditPort: AuditPort = MemoryAudit()

val beliefAktualisieren = BeliefAktualisieren(llm, uhr, hypothesisPort)
val vorschlaege = AktionsVorschlagen(actionPort, konfidenzPort, auditPort)
val waehlen = BeobachtungWaehlen(voiPort)
val aktionGaten = AktionGaten(approvalPort)
val zyklus = Entscheidungszyklus(waehlen, beliefAktualisieren, aktionGaten)
val konfidenzEntscheidungszyklus = KonfidenzgebundenerEntscheidungszyklus(zyklus, konfidenzPort)

val controller = CodeAgentController(
    vorschlaege = vorschlaege,
    zyklus = konfidenzEntscheidungszyklus,
    uhr = uhr,
    evidenceIndex = evidenceIndex,
    budget = Budget(maxSchritte = 3),
    execute = ::execute,
    escalate = ::escalate,
)
```

Wichtig fuer Code-Agenten:

- **`AktionsVorschlagsPort` darf nur strukturierte Aktionen liefern**, keine direkten Werkzeugaufrufe.
- **`HumanApprovalPort` für irreversible Aktionen** als sicheren Mensch-Check ausrüsten; `LocalApproval` erzwingt Nonce/Identität/Einmaligkeit auf Basis der `ApprovalAnfrage`, die CLI-Kanalwahl bleibt fail-closed, persistenter Approval-Audit bleibt Folgescope.
- **`AuditPort` persistent** führen: bei jedem Schritt Belief-/Eskalationskontext speichern.
- **`KonfidenzPort` append-only** betreiben, damit Replay und Overrides nachvollziehbar bleiben.

## 8. Audit Anbinden

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

## 9. Noch Nicht Stabil

Diese Punkte sind bewusst noch nicht als Nutzervertrag festgelegt:

- Artefakt-Koordinaten und Release-Prozess.
- Provider-spezifische LLM-Composition mit API-Key-/Modell-Konfiguration und
  echte externe Provider-Bindungen.
- Produktives CLI-/Remote-/UI-Binding des lokalen Approval-Adapters samt
  persistentem Approval-Audit.
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
make cli-demo
make example-langchain
make example-koog
```

`make gates` umfasst Dokumentations-, Build-, Test-, Coverage- und
Architekturpruefungen.
