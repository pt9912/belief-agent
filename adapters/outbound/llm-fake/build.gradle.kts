// adapters:outbound:llm-fake — deterministischer Fake-LLM-Adapter (ARC-08):
// implementiert den LLM-Port hinter dem Kern. Steht in welle-02 für das echte
// Sprachmodell (welle-05); rein deterministisch (LH-QA-03). Adapter-Rolle: darf
// application (Port) + domain importieren, keinen fremden Adapter (a-check).

plugins {
    kotlin("multiplatform")
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
        }
    }
}
