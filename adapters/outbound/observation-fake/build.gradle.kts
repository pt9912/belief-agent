// adapters:outbound:observation-fake — deterministische Fake-Beobachtungsquelle
// (ARC-08): implementiert den Beobachtungs-Port hinter dem Kern. Steht in
// welle-02 für echte Quellen (Test-/Build-Runner, Log-Scraper, …, welle-05);
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
