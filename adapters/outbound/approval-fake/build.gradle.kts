// adapters:outbound:approval-fake — deterministischer Fake-Approval-Adapter
// (ARC-08): implementiert den Human-Approval-Port hinter dem Kern. Steht in
// welle-03 für die echte menschliche Freigabe (interaktiver Adapter später);
// deterministisch (LH-QA-03). Adapter-Rolle: application (Port) + domain.

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
