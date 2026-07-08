// adapters:inbound:cli — produktiv gedachter ARC-09-Composition-Root.
// Koin bleibt am Rand; der Kern bleibt framework- und adapterfrei.

plugins {
    kotlin("jvm")
    application
    id("org.jetbrains.kotlinx.kover")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":hexagon:domain"))
    implementation(project(":hexagon:application"))
    implementation(project(":adapters:outbound:llm-fake"))
    implementation(project(":adapters:outbound:observation-fake"))
    implementation(project(":adapters:outbound:audit-memory"))
    implementation(project(":adapters:outbound:approval-fake"))
    implementation(project(":adapters:outbound:approval-local"))
    implementation(project(":adapters:outbound:voi-fake"))
    implementation(project(":adapters:outbound:llm-hypothesen-fake"))
    implementation(project(":adapters:outbound:konfidenz-memory"))
    implementation(project(":adapters:outbound:llm-action-fake"))
    implementation("io.insert-koin:koin-core:4.2.2")

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("dev.beliefagent.adapter.cli.MainKt")
}

kover {
    reports {
        verify {
            rule {
                // Inbound/Composition-Root: sicherheitsrelevantes Wiring + Executor.
                minBound(90)
            }
        }
    }
}
