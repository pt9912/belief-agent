// adapters:outbound:llm-action-koog — echter Koog-Adapter (ARC-08, slice-043):
// implementiert AktionsVorschlagsPort hinter dem Kern, zweiter Framework-Pfad
// neben llm-action-langchain4j (slice-042). Provider-Modelle und API-Keys bleiben
// im Composition Root; dieses Modul bindet nur an Koog LLMClient/PromptExecutor.
// Framework (ai.koog:koog-agents) ist bereits adoptiert (llm-koog) → keine neue
// Toolchain-Fläche, kein Folge-ADR (ADR-0002, §9 F-5).

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
    implementation("ai.koog:koog-agents:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.3")

    testImplementation(kotlin("test"))
}

kover {
    reports {
        verify {
            rule {
                // Adapter (ADR-0006): dünne Framework-Grenze + strikter Parser,
                // voll lokal testbar (Stub-Runner, kein Netz/API-Key).
                minBound(90)
            }
        }
    }
}
