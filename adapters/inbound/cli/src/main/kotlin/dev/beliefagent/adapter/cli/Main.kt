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
    val argumente = CliArgumente.parse(args)
    return argumente.szenarien.joinToString(separator = "\n\n") { szenario ->
        val konfiguration = argumente.approval
            ?.let { szenario.konfiguration().mitApproval(it) }
            ?: szenario.konfiguration()
        CliRuntime.ausKonfiguration(konfiguration).starte().sichtbareAusgabe
    }
}

fun main(args: Array<String>) {
    println(cliDemoAusgabe(args))
}

private data class CliArgumente(
    val szenarien: List<CliSzenario>,
    val approval: CliApprovalKonfiguration?,
) {
    companion object {
        fun parse(args: Array<String>): CliArgumente {
            val approval = args
                .firstOrNull { it.startsWith("approval=") }
                ?.substringAfter("=")
                ?.let(::approvalAusArgument)
            val szenarioArgumente = args.filterNot { it.startsWith("approval=") }
            val szenarien = if (szenarioArgumente.isEmpty()) {
                listOf(CliSzenario.GEHANDELT)
            } else {
                szenarioArgumente.flatMap { CliSzenario.ausArgument(it) }
            }
            return CliArgumente(szenarien = szenarien, approval = approval)
        }

        private fun approvalAusArgument(argument: String): CliApprovalKonfiguration = when (argument) {
            "fake" -> CliApprovalKonfiguration.Fake(false)
            else -> CliApprovalKonfiguration.Kanalwahl.auswahl(argument)
        }
    }
}
