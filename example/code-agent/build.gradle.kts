// example:code-agent — runnable Composition-Beispiel fuer einen vollstaendigen
// Code-Agenten-Controller mit den Kern-Ports aus dem Produktiv-Contract.

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
    implementation(project(":adapters:outbound:llm-fake"))
    implementation(project(":adapters:outbound:konfidenz-memory"))
    implementation(project(":adapters:outbound:audit-memory"))
    implementation(project(":adapters:outbound:observation-build-report"))
    implementation(project(":adapters:outbound:observation-git-local"))
}

application {
    mainClass.set("dev.beliefagent.example.codeagent.MainKt")
}
