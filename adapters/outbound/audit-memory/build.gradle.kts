// adapters:outbound:audit-memory — In-Memory-Audit-Adapter (ARC-08):
// implementiert den Audit-Port (slice-008) append-only im Speicher. Der E2E-
// Test verdrahtet die realen Fake-Adapter (die Composition macht in Produktion
// die cli-Root, welle-03; Testcode ist arch-check-befreit).

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlinx.kover")
}

kotlin {
    jvmToolchain(21)
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":hexagon:domain"))
            implementation(project(":hexagon:application"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(project(":adapters:outbound:llm-fake"))
            implementation(project(":adapters:outbound:observation-fake"))
        }
    }
}

kover {
    reports {
        verify {
            rule {
                // Adapter (ADR-0006): 90 % flach — dünner, deterministischer Fake
                // (Ist: 100 %), voll testbar; kein M2-Bump.
                minBound(90)
            }
        }
    }
}
