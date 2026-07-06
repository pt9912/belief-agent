// adapters:outbound:llm-koog — echter Koog-Adapter (ARC-08):
// implementiert den LLM-Port hinter dem Kern. Koog-LLMClient oder
// PromptExecutor werden im Composition Root konfiguriert und hier nur genutzt.

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
                // Adapter (ADR-0006): duenne Framework-Grenze, voll lokal testbar.
                minBound(90)
            }
        }
    }
}
