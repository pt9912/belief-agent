// hexagon:application — Use-Case-Schicht (HexSlice Application, ARC-02/ARC-07):
// command/handler/result + Ports. Hängt an hexagon:domain und importiert NIE
// einen Adapter — erzwungen durch die Modul-Grenze (nur domain-Dependency),
// a-check (application-/port-Rolle) und die commonMain-Source-Set-Sicht.
//
// Trägt Use-Case-Logik (belief-aktualisieren, aktion-gaten) + Ports. Coverage-
// Gate per Modul (ADR-0006), Kern-Schicht wie die Domäne: 90 % Line-Coverage ab
// M1, 95 % bei M2 (Ist: 100 %).

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
                // Kern-Schicht (ADR-0004/ADR-0006): 90 % ab M1, 95 % bei M2.
                minBound(90)
            }
        }
    }
}
