// hexagon:application — Use-Case-Schicht (HexSlice Application, ARC-02/ARC-07):
// command/handler/result + Ports. Hängt an hexagon:domain und importiert NIE
// einen Adapter — erzwungen durch die Modul-Grenze (nur domain-Dependency),
// a-check (application-/port-Rolle) und die commonMain-Source-Set-Sicht.
//
// slice-008 (Fundament): trägt vorerst nur den anwendungsweiten Audit-Port
// (Interface, ARC-06). Use-Case-Logik und ein Coverage-Gate kommen mit
// slice-009 — bis dahin gibt es hier keine coverbare Logik (Interface-only).

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(21)
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":hexagon:domain"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
