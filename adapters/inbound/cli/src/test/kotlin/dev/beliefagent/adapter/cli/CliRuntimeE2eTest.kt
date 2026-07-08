package dev.beliefagent.adapter.cli

import dev.beliefagent.adapter.approvallocal.ApprovalAntwort
import dev.beliefagent.adapter.approvallocal.ApprovalAusgabe
import dev.beliefagent.adapter.approvallocal.ApprovalEingabe
import dev.beliefagent.adapter.approvallocal.ApprovalNonce
import dev.beliefagent.adapter.approvallocal.ApprovalNonceQuelle
import dev.beliefagent.adapter.approvallocal.InMemoryApprovalNonceStore
import dev.beliefagent.adapter.approvallocal.LocalApproval
import dev.beliefagent.application.belief.entscheidungszyklus.Zyklusergebnis
import dev.beliefagent.application.belief.gaten.ports.ApprovalAnfrage
import dev.beliefagent.application.belief.gaten.ports.HumanApprovalPort
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

    @Test
    fun lokales_approval_ohne_eingabe_bleibt_eskaliert_und_fuehrt_nicht_aus() {
        val runtime = CliRuntime.ausKonfiguration(
            StandardCliSzenarien.eskaliert().mitApproval(
                lokaleApprovalKonfiguration(eingabe = ApprovalEingabe { null }),
            ),
        )

        val ergebnis = runtime.starte()

        assertIs<Zyklusergebnis.Eskaliert>(ergebnis.zyklus)
        assertEquals(CliTerminal.ESKALIERT, ergebnis.terminal)
        assertTrue(ergebnis.sichtbareAusgabe.contains("approval=local"))
        assertTrue(ergebnis.sichtbareAusgabe.contains("executed=false"))
        assertTrue(ergebnis.sichtbareAusgabe.contains("executor_boundary=closed"))
        assertEquals(emptyList(), runtime.ausgefuehrteAktionen())
    }

    @Test
    fun lokales_approval_mit_passender_antwort_nutzt_bestehende_executor_grenze() {
        val runtime = CliRuntime.ausKonfiguration(
            StandardCliSzenarien.eskaliert().mitApproval(lokaleApprovalKonfiguration()),
        )

        val ergebnis = runtime.starte()

        val gehandelt = assertIs<Zyklusergebnis.Gehandelt>(ergebnis.zyklus)
        assertEquals(CliTerminal.GEHANDELT, ergebnis.terminal)
        assertTrue(ergebnis.sichtbareAusgabe.contains("approval=local"))
        assertTrue(ergebnis.sichtbareAusgabe.contains("executed=true"))
        assertTrue(ergebnis.sichtbareAusgabe.contains("executor_boundary=Zyklusergebnis.Gehandelt.freigabe.aktion"))
        assertEquals(listOf(gehandelt.freigabe.aktion), runtime.ausgefuehrteAktionen())
    }

    @Test
    fun lokales_approval_mit_falscher_nonce_bleibt_geschlossen() {
        val runtime = CliRuntime.ausKonfiguration(
            StandardCliSzenarien.eskaliert().mitApproval(
                lokaleApprovalKonfiguration(
                    eingabe = ApprovalEingabe { challenge ->
                        ApprovalAntwort(
                            nonce = "falsch",
                            identitaet = "operator",
                            kontextDigest = challenge.kontextDigest.wert,
                            bestaetigung = LocalApproval.BESTAETIGUNG,
                        )
                    },
                ),
            ),
        )

        val ergebnis = runtime.starte()

        assertIs<Zyklusergebnis.Eskaliert>(ergebnis.zyklus)
        assertEquals(CliTerminal.ESKALIERT, ergebnis.terminal)
        assertTrue(ergebnis.sichtbareAusgabe.contains("executed=false"))
        assertEquals(emptyList(), runtime.ausgefuehrteAktionen())
    }

    @Test
    fun lokales_approval_kann_nicht_mit_wiederverwendeter_nonce_erneut_ausfuehren() {
        val runtime = CliRuntime.ausKonfiguration(
            StandardCliSzenarien.eskaliert().mitApproval(lokaleApprovalKonfiguration()),
        )

        val ersterLauf = runtime.starte()
        val zweiterLauf = runtime.starte()

        val ersterGehandelt = assertIs<Zyklusergebnis.Gehandelt>(ersterLauf.zyklus)
        assertIs<Zyklusergebnis.Eskaliert>(zweiterLauf.zyklus)
        assertEquals(CliTerminal.GEHANDELT, ersterLauf.terminal)
        assertEquals(CliTerminal.ESKALIERT, zweiterLauf.terminal)
        assertEquals(listOf(ersterGehandelt.freigabe.aktion), runtime.ausgefuehrteAktionen())
    }

    @Test
    fun cli_argument_approval_local_ist_explizit_und_interaktiv_gebunden() {
        val ausgabe = cliDemoAusgabe(arrayOf("gehandelt", "approval=local"))

        assertTrue(ausgabe.contains("scenario=gehandelt"))
        assertTrue(ausgabe.contains("approval=local"))
        assertTrue(ausgabe.contains("terminal=gehandelt"))
    }

    @Test
    fun cli_argument_unbekannter_approval_kanal_bleibt_fail_closed() {
        val ausgabe = cliDemoAusgabe(arrayOf("eskaliert", "approval=remote"))

        assertTrue(ausgabe.contains("scenario=eskaliert"))
        assertTrue(ausgabe.contains("approval=remote"))
        assertTrue(ausgabe.contains("terminal=eskaliert"))
        assertTrue(ausgabe.contains("executed=false"))
        assertTrue(ausgabe.contains("executor_boundary=closed"))
    }

    @Test
    fun approval_kanalwahl_ohne_binding_bleibt_fail_closed() {
        val runtime = CliRuntime.ausKonfiguration(
            StandardCliSzenarien.eskaliert().mitApproval(
                CliApprovalKonfiguration.Kanalwahl(
                    kanal = CliApprovalKanalName.LOCAL,
                    kanaele = emptyMap(),
                ),
            ),
        )

        val ergebnis = runtime.starte()

        assertIs<Zyklusergebnis.Eskaliert>(ergebnis.zyklus)
        assertEquals(CliTerminal.ESKALIERT, ergebnis.terminal)
        assertTrue(ergebnis.sichtbareAusgabe.contains("approval=local"))
        assertTrue(ergebnis.sichtbareAusgabe.contains("executed=false"))
        assertEquals(emptyList(), runtime.ausgefuehrteAktionen())
    }

    @Test
    fun approval_kanalfehler_bleibt_fail_closed() {
        val runtime = CliRuntime.ausKonfiguration(
            StandardCliSzenarien.eskaliert().mitApproval(
                CliApprovalKonfiguration.Kanalwahl(
                    kanal = CliApprovalKanalName.LOCAL,
                    kanaele = mapOf(
                        CliApprovalKanalName.LOCAL to object : HumanApprovalPort {
                            override fun freigegeben(anfrage: ApprovalAnfrage): Boolean =
                                error("approval channel failed")
                        },
                    ),
                ),
            ),
        )

        val ergebnis = runtime.starte()

        assertIs<Zyklusergebnis.Eskaliert>(ergebnis.zyklus)
        assertEquals(CliTerminal.ESKALIERT, ergebnis.terminal)
        assertTrue(ergebnis.sichtbareAusgabe.contains("executed=false"))
        assertEquals(emptyList(), runtime.ausgefuehrteAktionen())
    }

    @Test
    fun approval_dispatcher_ruft_genau_einen_ausgewaehlten_kanal_auf() {
        val local = ZaehlenApproval(freigegeben = true)
        val remote = ZaehlenApproval(freigegeben = true)
        val runtime = CliRuntime.ausKonfiguration(
            StandardCliSzenarien.eskaliert().mitApproval(
                CliApprovalKonfiguration.Kanalwahl(
                    kanal = CliApprovalKanalName.LOCAL,
                    kanaele = mapOf(
                        CliApprovalKanalName.LOCAL to local,
                        CliApprovalKanalName("remote") to remote,
                    ),
                ),
            ),
        )

        val ergebnis = runtime.starte()

        assertIs<Zyklusergebnis.Gehandelt>(ergebnis.zyklus)
        assertEquals(1, local.aufrufe)
        assertEquals(0, remote.aufrufe)
        assertTrue(ergebnis.executor.ausgefuehrt)
    }

    private fun lokaleApprovalKonfiguration(
        nonce: String = "nonce-cli-test",
        eingabe: ApprovalEingabe = ApprovalEingabe { challenge ->
            ApprovalAntwort(
                nonce = challenge.nonce.wert,
                identitaet = "operator",
                kontextDigest = challenge.kontextDigest.wert,
                bestaetigung = LocalApproval.BESTAETIGUNG,
            )
        },
    ): CliApprovalKonfiguration.Kanalwahl =
        CliApprovalKonfiguration.Kanalwahl.local(
            nonceQuelle = ApprovalNonceQuelle { ApprovalNonce(nonce) },
            eingabe = eingabe,
            ausgabe = ApprovalAusgabe {},
            nonceStore = InMemoryApprovalNonceStore(),
        )

    private class ZaehlenApproval(private val freigegeben: Boolean) : HumanApprovalPort {
        var aufrufe = 0
            private set

        override fun freigegeben(anfrage: ApprovalAnfrage): Boolean {
            aufrufe += 1
            return freigegeben
        }
    }
}
