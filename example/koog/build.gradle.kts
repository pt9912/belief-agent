// example:koog — runnable integration example. It demonstrates the Koog
// adapter boundary without requiring provider credentials or network access.

plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":hexagon:domain"))
    implementation(project(":hexagon:application"))
    implementation(project(":adapters:outbound:audit-memory"))
    implementation(project(":adapters:outbound:llm-koog"))
    runtimeOnly("ai.koog:koog-agents:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

application {
    mainClass.set("dev.beliefagent.example.koog.MainKt")
}
