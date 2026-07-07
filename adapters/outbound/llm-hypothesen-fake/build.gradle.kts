// adapters:outbound:llm-hypothesen-fake — deterministischer Fake fuer
// Hypothesen-Kandidaten (ARC-08): implementiert den HypothesenPort hinter dem
// Kern. Adapter-Rolle: application (Port) + domain; keine fremden Adapter.

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
                // Adapter (ADR-0006): 90 % flach — duenner, deterministischer
                // Fake; voll testbar und ohne externen Provider.
                minBound(90)
            }
        }
    }
}
