// adapters:outbound:observation-git-local — lokaler Git-Beobachter (ARC-08):
// uebersetzt lokalen Git-Status oder Replay-Fixtures in Repo-Beobachtungen.

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
                // Adapter (ADR-0006): lokaler Git-Wrapper + Parser, voll testbar.
                minBound(90)
            }
        }
    }
}
