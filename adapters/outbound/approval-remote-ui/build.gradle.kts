// adapters:outbound:approval-remote-ui — hermetisch testbarer Remote/UI-Approval-Kanal (ARC-08).
// Implementiert den HumanApprovalPort mit Transport-Abstraktion, Nonce,
// Identitaet und Kontextbindung ohne Live-Netzwerk in lokalen Gates.

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
                // Safety-relevanter Adapter: Remote/UI-Failure-Modes sind deterministisch getestet.
                minBound(90)
            }
        }
    }
}
