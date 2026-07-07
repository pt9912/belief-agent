package dev.beliefagent.adapter.cli

enum class CliSzenario(val id: String) {
    GEHANDELT("gehandelt"),
    ESKALIERT("eskaliert"),
    ABGELEHNT("abgelehnt"),
    SAMMELT_DANN_HANDELT("sammelt-dann-handelt"),
    ;

    fun konfiguration(): CliRuntimeKonfiguration = when (this) {
        GEHANDELT -> StandardCliSzenarien.gehandelt()
        ESKALIERT -> StandardCliSzenarien.eskaliert()
        ABGELEHNT -> StandardCliSzenarien.abgelehnt()
        SAMMELT_DANN_HANDELT -> StandardCliSzenarien.sammeltDannHandelt()
    }

    companion object {
        fun ausArgument(argument: String): List<CliSzenario> =
            if (argument == "all") {
                values().toList()
            } else {
                listOf(
                    values().firstOrNull { it.id == argument }
                        ?: throw IllegalArgumentException(
                            "Unbekanntes CLI-Szenario '$argument'. Erlaubt: ${werteText()} oder all.",
                        ),
                )
            }

        private fun werteText(): String = values().joinToString(", ") { it.id }
    }
}

fun cliDemoAusgabe(args: Array<String>): String {
    val szenarien = if (args.isEmpty()) {
        listOf(CliSzenario.GEHANDELT)
    } else {
        args.flatMap { CliSzenario.ausArgument(it) }
    }
    return szenarien.joinToString(separator = "\n\n") { szenario ->
        CliRuntime.ausKonfiguration(szenario.konfiguration()).starte().sichtbareAusgabe
    }
}

fun main(args: Array<String>) {
    println(cliDemoAusgabe(args))
}
