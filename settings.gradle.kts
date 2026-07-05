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
