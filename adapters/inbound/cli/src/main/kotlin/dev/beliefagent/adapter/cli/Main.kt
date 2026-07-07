package dev.beliefagent.adapter.cli

fun main() {
    val ergebnis = CliRuntime.ausKonfiguration(StandardCliSzenarien.gehandelt()).starte()
    println(ergebnis.sichtbareAusgabe)
}
