// example:langchain — runnable integration example. It demonstrates the
// port boundary around a LangChain-based tool without adding a concrete
// LangChain/HTTP dependency to the core build.

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
    implementation(project(":adapters:outbound:llm-langchain4j"))
    runtimeOnly("dev.langchain4j:langchain4j:1.17.1")
}

application {
    mainClass.set("dev.beliefagent.example.langchain.MainKt")
}
