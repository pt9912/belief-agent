// Root-Konvention für alle HexSlice-Module (ADR-0003).
// Kotlin Multiplatform (ADR-0002), JVM-Ziel zuerst (LH-RB-04). Die Toolchain
// lebt im multi-stage Dockerfile — kein Host-JDK/-Gradle (AGENTS.md §3.1).

plugins {
    kotlin("multiplatform") version "2.4.0" apply false
}

subprojects {
    group = "dev.beliefagent"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
