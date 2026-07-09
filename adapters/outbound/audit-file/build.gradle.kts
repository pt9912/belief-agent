// adapters:outbound:audit-file — persistenter Datei-Audit-Adapter (ARC-08, slice-041):
// implementiert den Audit-Port (ARC-06) append-only über java.nio auf Datei.
// JVM-Ziel wie alle IO-tragenden Adapter (observation-build-report/-git-local,
// llm-langchain4j); keine neue Build-Abhängigkeit (Stdlib-Serialisierung,
// kein Plugin) — konform ADR-0002 (JVM-Ziel zuerst, Dep am Rand, §9 DR-F2).

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlinx.kover")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":hexagon:domain"))
    implementation(project(":hexagon:application"))

    testImplementation(kotlin("test"))
}

kover {
    reports {
        verify {
            rule {
                // Adapter-Floor (ADR-0006): Datei-IO + Textformat, hermetisch
                // testbar über java.nio-Temp-Dateien.
                minBound(90)
            }
        }
    }
}
