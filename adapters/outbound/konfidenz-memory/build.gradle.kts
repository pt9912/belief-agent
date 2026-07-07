// adapters:outbound:konfidenz-memory - deterministischer Memory-/Replay-Adapter
// fuer externalisierte Modell-Konfidenzen (ARC-08, LH-FA-LLM-003). Adapter-
// Rolle: application contract; keine Gate-Entscheidung im Adapter.

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlinx.kover")
}

kotlin {
    jvmToolchain(21)
    jvm()

    sourceSets {
        commonMain.dependencies {
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
                // Adapter (ADR-0006): 90 % flach - duenner, deterministischer
                // Memory/Fake; voll testbar und ohne externen Provider.
                minBound(90)
            }
        }
    }
}
