pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "belief-agent"

// HexSlice-Module (ADR-0003). Reihenfolge = Abhängigkeitsrichtung nach innen.
// Weitere Slices (application/<use-case>) und Adapter (inbound/outbound)
// kommen als eigene Module hinzu.
include("hexagon:domain")
include("hexagon:application") // Use-Case-Schicht + Ports (slice-008 Fundament)
include("adapters:outbound:llm-fake") // Fake-LLM-Adapter (slice-009)
include("adapters:outbound:llm-langchain4j") // LangChain4j-LLM-Adapter (ARC-08)
include("adapters:outbound:llm-koog") // Koog-LLM-Adapter (ARC-08)
include("adapters:outbound:observation-fake") // Fake-Beobachtungsquelle (slice-010)
include("adapters:outbound:observation-build-report") // Lokaler Build-Beobachter (ARC-08)
include("adapters:outbound:observation-git-local") // Lokaler Git-Beobachter (ARC-08)
include("adapters:outbound:audit-memory") // In-Memory-Audit-Persistenz (slice-010)
include("adapters:outbound:approval-fake") // Fake-Human-Approval (slice-013)
include("adapters:outbound:approval-local") // Lokaler Human-Approval-Adapter (ARC-08)
include("adapters:outbound:approval-remote-ui") // Remote/UI-Human-Approval-Kanal (ARC-08)
include("adapters:outbound:voi-fake") // Fake-VoI-Kandidatenquelle (slice-016)
include("adapters:outbound:llm-hypothesen-fake") // Fake-Hypothesen-Port (ARC-08)
include("adapters:outbound:konfidenz-memory") // Memory-/Replay-Konfidenz-Port (ARC-08)
include("adapters:outbound:llm-action-fake") // Fake-Aktionsvorschlags-Port (ARC-08)
include("adapters:inbound:cli") // ARC-09 Composition Root + CLI-Einstieg
include("example:langchain") // Lauffaehiges Integrationsbeispiel, kein Produktiv-Adapter
include("example:koog") // Lauffaehiges Koog-Integrationsbeispiel, kein Produktiv-Adapter
include("example:code-agent") // Laufzeitnahes Code-Agent-Composition-Beispiel
