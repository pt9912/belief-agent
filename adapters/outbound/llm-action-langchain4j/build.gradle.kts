// adapters:outbound:llm-action-langchain4j — echter LangChain4j-Adapter (ARC-08,
// slice-042): implementiert AktionsVorschlagsPort hinter dem Kern. Provider-Modelle
// und API-Keys bleiben im Composition Root; dieses Modul bindet nur an ChatModel.
// Framework + JSON-Lib sind bereits adoptiert (llm-langchain4j) → keine neue
// Toolchain-Fläche, kein Folge-ADR (ADR-0002, §9 F-3).

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
                // Adapter (ADR-0006): dünne Framework-Grenze + strikter Parser,
                // voll lokal testbar (Stub-Runner, kein Netz/API-Key).
                minBound(90)
            }
        }
    }
}
