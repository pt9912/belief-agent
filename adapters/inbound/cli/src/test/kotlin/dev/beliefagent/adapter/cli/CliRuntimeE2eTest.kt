package dev.beliefagent.adapter.cli

import dev.beliefagent.application.belief.entscheidungszyklus.Zyklusergebnis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CliRuntimeE2eTest {

    @Test
    fun terminal_gehandelt_fuehrt_genau_die_freigegebene_aktion_aus() {
        val runtime = CliRuntime.ausKonfiguration(StandardCliSzenarien.gehandelt())

        val ergebnis = runtime.starte()

        val gehandelt = assertIs<Zyklusergebnis.Gehandelt>(ergebnis.zyklus)
        assertEquals(CliTerminal.GEHANDELT, ergebnis.terminal)
        assertTrue(ergebnis.sichtbareAusgabe.contains("scenario=gehandelt"))
        assertTrue(ergebnis.sichtbareAusgabe.contains("terminal=gehandelt"))
        assertTrue(ergebnis.sichtbareAusgabe.contains("executed=true"))
        assertTrue(ergebnis.sichtbareAusgabe.contains("executor_boundary=Zyklusergebnis.Gehandelt.freigabe.aktion"))
        assertTrue(ergebnis.executor.ausgefuehrt)
        assertEquals(listOf(gehandelt.freigabe.aktion), runtime.ausgefuehrteAktionen())
    }

    @Test
    fun terminal_eskaliert_bleibt_fail_closed_ohne_ausfuehrung() {
        val runtime = CliRuntime.ausKonfiguration(StandardCliSzenarien.eskaliert())

        val ergebnis = runtime.starte()

        assertIs<Zyklusergebnis.Eskaliert>(ergebnis.zyklus)
        assertEquals(CliTerminal.ESKALIERT, ergebnis.terminal)
        assertTrue(ergebnis.sichtbareAusgabe.contains("scenario=eskaliert"))
        assertTrue(ergebnis.sichtbareAusgabe.contains("terminal=eskaliert"))
        assertTrue(ergebnis.sichtbareAusgabe.contains("executed=false"))
        assertTrue(ergebnis.sichtbareAusgabe.contains("reason=GateEskalation"))
        assertTrue(ergebnis.sichtbareAusgabe.contains("executor_boundary=closed"))
        assertEquals(emptyList(), runtime.ausgefuehrteAktionen())
    }

    @Test
    fun terminal_abgelehnt_bleibt_fail_closed_ohne_ausfuehrung() {
        val runtime = CliRuntime.ausKonfiguration(StandardCliSzenarien.abgelehnt())

        val ergebnis = runtime.starte()

        assertIs<Zyklusergebnis.Abgelehnt>(ergebnis.zyklus)
        assertEquals(CliTerminal.ABGELEHNT, ergebnis.terminal)
        assertTrue(ergebnis.sichtbareAusgabe.contains("scenario=abgelehnt"))
        assertTrue(ergebnis.sichtbareAusgabe.contains("terminal=abgelehnt"))
        assertTrue(ergebnis.sichtbareAusgabe.contains("executed=false"))
        assertTrue(ergebnis.sichtbareAusgabe.contains("executor_boundary=closed"))
        assertEquals(emptyList(), runtime.ausgefuehrteAktionen())
    }

    @Test
    fun terminal_gehandelt_kann_vorher_einen_sammel_schritt_durchlaufen() {
        val runtime = CliRuntime.ausKonfiguration(StandardCliSzenarien.sammeltDannHandelt())

        val ergebnis = runtime.starte()

        val gehandelt = assertIs<Zyklusergebnis.Gehandelt>(ergebnis.zyklus)
        assertEquals(CliTerminal.GEHANDELT, ergebnis.terminal)
        assertTrue(ergebnis.sichtbareAusgabe.contains("scenario=sammelt-dann-handelt"))
        assertTrue(gehandelt.belief.resthypothese.wahrscheinlichkeit < 0.95)
        assertEquals(listOf(gehandelt.freigabe.aktion), runtime.ausgefuehrteAktionen())
    }

    @Test
    fun cli_demo_default_bleibt_gehandelt() {
        val ausgabe = cliDemoAusgabe(emptyArray())

        assertTrue(ausgabe.contains("scenario=gehandelt"))
        assertTrue(ausgabe.contains("terminal=gehandelt"))
        assertTrue(ausgabe.contains("executed=true"))
    }

    @Test
    fun cli_demo_all_zeigt_unsicherheitsgrenzen() {
        val ausgabe = cliDemoAusgabe(arrayOf("all"))

        assertTrue(ausgabe.contains("scenario=gehandelt"))
        assertTrue(ausgabe.contains("scenario=eskaliert"))
        assertTrue(ausgabe.contains("terminal=eskaliert"))
        assertTrue(ausgabe.contains("reason=GateEskalation"))
        assertTrue(ausgabe.contains("scenario=abgelehnt"))
        assertTrue(ausgabe.contains("terminal=abgelehnt"))
        assertTrue(ausgabe.contains("executed=false"))
        assertTrue(ausgabe.contains("scenario=sammelt-dann-handelt"))
    }

    @Test
    fun executor_hat_keinen_pfad_fuer_ablehnung_oder_eskalation() {
        val ausfuehrung = RecordingAktionsAusfuehrungsAdapter()
        val executor = CliExecutor(ausfuehrung)

        val abgelehnt = Zyklusergebnis.Abgelehnt("kein Gate", StandardCliSzenarien.abgelehnt().prior)
        val eskaliert = CliRuntime.ausKonfiguration(StandardCliSzenarien.eskaliert()).starte().zyklus

        assertEquals(ExecutorErgebnis(false, CliTerminal.ABGELEHNT), executor.verarbeite(abgelehnt))
        assertEquals(ExecutorErgebnis(false, CliTerminal.ESKALIERT), executor.verarbeite(eskaliert))
        assertEquals(emptyList(), ausfuehrung.ausgefuehrteAktionen())
    }
}
