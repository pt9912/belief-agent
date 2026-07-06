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
include("adapters:outbound:observation-fake") // Fake-Beobachtungsquelle (slice-010)
include("adapters:outbound:audit-memory") // In-Memory-Audit-Persistenz (slice-010)
include("adapters:outbound:approval-fake") // Fake-Human-Approval (slice-013)
include("adapters:outbound:voi-fake") // Fake-VoI-Kandidatenquelle (slice-016)
