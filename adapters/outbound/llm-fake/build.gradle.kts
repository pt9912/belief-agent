// adapters:outbound:llm-fake — deterministischer Fake-LLM-Adapter (ARC-08):
// implementiert den LLM-Port hinter dem Kern. Steht in welle-02 für das echte
// Sprachmodell (welle-05); rein deterministisch (LH-QA-03). Adapter-Rolle: darf
// application (Port) + domain importieren, keinen fremden Adapter (a-check).

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
