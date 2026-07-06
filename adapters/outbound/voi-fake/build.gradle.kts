// adapters:outbound:voi-fake — deterministische Fake-Kandidatenquelle (ARC-08):
// implementiert den Beobachtungs-Auswahl-Port hinter dem Kern. Steht in welle-04
// für die LLM-gestützte Kandidaten-/Diskriminierungs-Schätzung (welle-05, ADR-0001);
// deterministisch (LH-QA-03). Adapter-Rolle: application (Port) + domain.

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
