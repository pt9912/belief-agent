// adapters:outbound:observation-build-report — lokaler Build-Beobachter (ARC-08):
// uebersetzt Build-/Test-Reports oder Replay-Fixtures in Beobachtungen.

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlinx.kover")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":hexagon:domain"))
    implementation(project(":hexagon:application"))

    testImplementation(kotlin("test"))
}

kover {
    reports {
        verify {
            rule {
                // Adapter (ADR-0006): lokaler Parser + Port-Adapter, voll testbar.
                minBound(90)
            }
        }
    }
}
