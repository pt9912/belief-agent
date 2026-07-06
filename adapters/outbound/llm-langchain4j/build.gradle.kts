// adapters:outbound:llm-langchain4j — echter LangChain4j-Adapter (ARC-08):
// implementiert den LLM-Port hinter dem Kern. Provider-spezifische Modelle und
// API-Keys bleiben im Composition Root; dieses Modul bindet nur an ChatModel.

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
    implementation("dev.langchain4j:langchain4j:1.17.1")
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
