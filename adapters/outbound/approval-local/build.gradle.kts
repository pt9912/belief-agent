// adapters:outbound:approval-local — lokaler Human-Approval-Adapter (ARC-08).
// Implementiert den HumanApprovalPort mit Nonce, Identitaet und Kontextbindung.

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
                // Safety-relevanter Adapter: Negativmatrix ist vollständig testbar.
                minBound(90)
            }
        }
    }
}
