// hexagon:domain — fachlicher Kern (HexSlice Domain, ARC-01): Entities,
// Value Objects, Domain-Events/-Services. Framework-/plattformfrei, nur
// Kotlin-Stdlib. Importiert NICHTS aus application/ oder adapters/ — erzwungen
// durch die Modul-Grenze (dieses Modul hat keine solche Abhängigkeit),
// zusätzlich durch a-check (domain-Rolle) und die commonMain-Source-Set-Sicht.

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlinx.kover")
}

kotlin {
    jvmToolchain(21)
    jvm()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
