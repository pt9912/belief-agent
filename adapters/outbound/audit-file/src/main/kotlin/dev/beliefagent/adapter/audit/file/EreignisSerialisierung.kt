package dev.beliefagent.adapter.audit.file

import dev.beliefagent.domain.belief.AktionVorgeschlagen
import dev.beliefagent.domain.belief.ApprovalAngefragt
import dev.beliefagent.domain.belief.ApprovalErteilt
import dev.beliefagent.domain.belief.ApprovalFehler
import dev.beliefagent.domain.belief.ApprovalVerweigert
import dev.beliefagent.domain.belief.BeliefAktualisiert
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.BeobachtungErfasst
import dev.beliefagent.domain.belief.Ereignis
import dev.beliefagent.domain.belief.EskalationAngefordert
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.EvidenzReferenz
import dev.beliefagent.domain.belief.GateAbgelehnt
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseHinzugefuegt
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.KonfidenzExternalisiert
import dev.beliefagent.domain.belief.KonfidenzUeberschrieben
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Resthypothese
import dev.beliefagent.domain.belief.Zeitstempel

/**
 * Deterministisches, versioniertes **Textformat** für das append-only
 * Ereignisprotokoll (slice-041, `LH-QA-06` inspizierbar). Bewusst Stdlib/JDK,
 * keine Serialisierungs-Bibliothek (kein neues Plugin, ADR-0002, §9 DR-F2).
 *
 * **Zeilen-orientiert:** genau **ein Ereignis pro Zeile**, damit die Record-
 * Grenze das Newline ist — das macht Trailing-Truncation robust erkennbar
 * (§9 IDR-2). Eine Zeile ist `TAG` gefolgt von tab-separierten `key=value`-
 * Feldern. Werte sind escaped (`\`, Tab, Newline, CR), sodass kein Wert eine
 * Feld- oder Record-Grenze vortäuscht. Zahlen deterministisch über `toString`;
 * der verschachtelte [BeliefState] über indizierte Felder (`hn`, `h{i}.id`,
 * `h{i}.p`, `h{i}.evn`, `h{i}.ev{j}`, `restp`) — keine geschachtelten Delimiter.
 *
 * **Integrität:** [dekodiere] wirft [AuditFormatFehler] bei unbekanntem Tag,
 * fehlendem Pflichtfeld oder ungültigem Wert; die Domänen-Invarianten
 * (Normierung, Approval-Referenzen) prüfen die Konstruktoren erneut — ein
 * manipulierter Datensatz wird also **laut** abgewiesen (`LH-QA-02`). Die
 * Append-only-Ordnung reimplementiert dieses Format **nicht**; sie bleibt Sache
 * von `EreignisProtokoll.von(...)` (Domäne, §9 DR-F3).
 */
object EreignisSerialisierung {

    /** Magischer Header + Formatversion (Zeile 1 jeder Audit-Datei). */
    const val HEADER: String = "beliefaudit/v1"

    // --- Kodierung ---------------------------------------------------------

    /** Kodiert [ereignis] als **eine** Zeile (ohne abschließendes Newline). */
    fun kodiere(ereignis: Ereignis): String =
        when (ereignis) {
            is HypotheseHinzugefuegt -> zeile(
                "HYP_HINZUGEFUEGT",
                "ts" to ts(ereignis.zeitstempel),
                "id" to esc(ereignis.hypothese.wert),
            )

            is BeobachtungErfasst -> zeile(
                "BEOBACHTUNG_ERFASST",
                "ts" to ts(ereignis.zeitstempel),
                "quelle" to esc(ereignis.beobachtung.quelle.name),
                "bts" to ts(ereignis.beobachtung.zeitstempel),
                "evidenz" to esc(ereignis.beobachtung.evidenz.beschreibung),
            )

            is BeliefAktualisiert -> kodiereBelief(ereignis)

            is AktionVorgeschlagen -> zeile(
                "AKTION_VORGESCHLAGEN",
                "ts" to ts(ereignis.zeitstempel),
                "beschreibung" to esc(ereignis.beschreibung),
            )

            is GateAbgelehnt -> zeile(
                "GATE_ABGELEHNT",
                "ts" to ts(ereignis.zeitstempel),
                "grund" to esc(ereignis.grund),
            )

            is EskalationAngefordert -> zeile(
                "ESKALATION_ANGEFORDERT",
                "ts" to ts(ereignis.zeitstempel),
                "grund" to esc(ereignis.grund),
            )

            is ApprovalAngefragt -> zeile(
                "APPROVAL_ANGEFRAGT",
                "ts" to ts(ereignis.zeitstempel),
                "digest" to esc(ereignis.anfrageKontextDigest),
                "kanal" to esc(ereignis.kanal),
                "nonce" to esc(ereignis.nonceReferenz),
            )

            is ApprovalErteilt -> zeile(
                "APPROVAL_ERTEILT",
                "ts" to ts(ereignis.zeitstempel),
                "digest" to esc(ereignis.anfrageKontextDigest),
                "kanal" to esc(ereignis.kanal),
                "nonce" to esc(ereignis.nonceReferenz),
                "antwort" to esc(ereignis.antwortReferenz),
                "identitaet" to esc(ereignis.identitaetsReferenz),
                "grund" to esc(ereignis.ergebnisGrund),
            )

            is ApprovalVerweigert -> zeile(
                "APPROVAL_VERWEIGERT",
                "ts" to ts(ereignis.zeitstempel),
                "digest" to esc(ereignis.anfrageKontextDigest),
                "kanal" to esc(ereignis.kanal),
                "nonce" to esc(ereignis.nonceReferenz),
                "antwort" to optOut(ereignis.antwortReferenz),
                "identitaet" to optOut(ereignis.identitaetsReferenz),
                "grund" to esc(ereignis.ergebnisGrund),
            )

            is ApprovalFehler -> zeile(
                "APPROVAL_FEHLER",
                "ts" to ts(ereignis.zeitstempel),
                "digest" to esc(ereignis.anfrageKontextDigest),
                "kanal" to esc(ereignis.kanal),
                "nonce" to esc(ereignis.nonceReferenz),
                "antwort" to optOut(ereignis.antwortReferenz),
                "identitaet" to optOut(ereignis.identitaetsReferenz),
                "grund" to esc(ereignis.ergebnisGrund),
            )

            is KonfidenzExternalisiert -> zeile(
                "KONFIDENZ_EXTERNALISIERT",
                "ts" to ts(ereignis.zeitstempel),
                "referenz" to esc(ereignis.referenz),
                "wert" to ereignis.wert.toString(),
                "quelle" to esc(ereignis.quelle),
                "version" to ereignis.version.toString(),
            )

            is KonfidenzUeberschrieben -> zeile(
                "KONFIDENZ_UEBERSCHRIEBEN",
                "ts" to ts(ereignis.zeitstempel),
                "referenz" to esc(ereignis.referenz),
                "alt" to ereignis.alterWert.toString(),
                "neu" to ereignis.neuerWert.toString(),
                "begruendung" to esc(ereignis.begruendung),
                "version" to ereignis.version.toString(),
            )
        }

    private fun kodiereBelief(ereignis: BeliefAktualisiert): String =
        buildString {
            append("BELIEF_AKTUALISIERT")
            feld("ts", ts(ereignis.zeitstempel))
            feld("restp", ereignis.belief.resthypothese.wahrscheinlichkeit.toString())
            feld("hn", ereignis.belief.hypothesen.size.toString())
            ereignis.belief.hypothesen.forEachIndexed { i, h ->
                feld("h$i.id", esc(h.id.wert))
                feld("h$i.p", h.wahrscheinlichkeit.toString())
                feld("h$i.evn", h.stuetzendeEvidenz.size.toString())
                h.stuetzendeEvidenz.forEachIndexed { j, ev ->
                    feld("h$i.ev$j", esc(ev.wert))
                }
            }
        }

    // --- Dekodierung -------------------------------------------------------

    /**
     * Dekodiert **eine** Zeile zurück in ein [Ereignis]. Wirft
     * [AuditFormatFehler] bei unbekanntem Tag, fehlendem/ungültigem Feld oder
     * verletzter Domänen-Invariante (Konstruktor-`require`).
     */
    fun dekodiere(zeile: String): Ereignis {
        val felder = zeile.split(FELD_TRENNER)
        val tag = felder.firstOrNull().orEmpty()
        if (tag.isEmpty()) throw AuditFormatFehler("Leere oder namenlose Ereigniszeile")
        val map = LinkedHashMap<String, String>()
        for (i in 1 until felder.size) {
            val token = felder[i]
            val gleich = token.indexOf('=')
            if (gleich < 0) throw AuditFormatFehler("Feld ohne '=': '$token' (Tag $tag)")
            map[token.substring(0, gleich)] = token.substring(gleich + 1)
        }
        return try {
            baue(tag, map)
        } catch (e: IllegalArgumentException) {
            // Verletzte Domänen-Invariante (z. B. Normierung, Approval-Referenz,
            // Enum-Wert) → sichtbarer Format-/Integritätsfehler statt Teilhistorie.
            throw AuditFormatFehler("Ungültiger Datensatz (Tag $tag): ${e.message}", e)
        }
    }

    private fun baue(tag: String, map: Map<String, String>): Ereignis =
        when (tag) {
            "HYP_HINZUGEFUEGT" -> HypotheseHinzugefuegt(
                zeitstempel = tsFeld(map, tag),
                hypothese = HypotheseId(reqS(map, "id", tag)),
            )

            "BEOBACHTUNG_ERFASST" -> BeobachtungErfasst(
                zeitstempel = tsFeld(map, tag),
                beobachtung = Beobachtung(
                    quelle = Quelle.valueOf(reqS(map, "quelle", tag)),
                    zeitstempel = Zeitstempel(reqLong(map, "bts", tag)),
                    evidenz = Evidenz(reqS(map, "evidenz", tag)),
                ),
            )

            "BELIEF_AKTUALISIERT" -> baueBelief(map, tag)

            "AKTION_VORGESCHLAGEN" -> AktionVorgeschlagen(
                zeitstempel = tsFeld(map, tag),
                beschreibung = reqS(map, "beschreibung", tag),
            )

            "GATE_ABGELEHNT" -> GateAbgelehnt(
                zeitstempel = tsFeld(map, tag),
                grund = reqS(map, "grund", tag),
            )

            "ESKALATION_ANGEFORDERT" -> EskalationAngefordert(
                zeitstempel = tsFeld(map, tag),
                grund = reqS(map, "grund", tag),
            )

            "APPROVAL_ANGEFRAGT" -> ApprovalAngefragt(
                zeitstempel = tsFeld(map, tag),
                anfrageKontextDigest = reqS(map, "digest", tag),
                kanal = reqS(map, "kanal", tag),
                nonceReferenz = reqS(map, "nonce", tag),
            )

            "APPROVAL_ERTEILT" -> ApprovalErteilt(
                zeitstempel = tsFeld(map, tag),
                anfrageKontextDigest = reqS(map, "digest", tag),
                kanal = reqS(map, "kanal", tag),
                nonceReferenz = reqS(map, "nonce", tag),
                antwortReferenz = reqS(map, "antwort", tag),
                identitaetsReferenz = reqS(map, "identitaet", tag),
                ergebnisGrund = reqS(map, "grund", tag),
            )

            "APPROVAL_VERWEIGERT" -> ApprovalVerweigert(
                zeitstempel = tsFeld(map, tag),
                anfrageKontextDigest = reqS(map, "digest", tag),
                kanal = reqS(map, "kanal", tag),
                nonceReferenz = reqS(map, "nonce", tag),
                antwortReferenz = optS(map, "antwort", tag),
                identitaetsReferenz = optS(map, "identitaet", tag),
                ergebnisGrund = reqS(map, "grund", tag),
            )

            "APPROVAL_FEHLER" -> ApprovalFehler(
                zeitstempel = tsFeld(map, tag),
                anfrageKontextDigest = reqS(map, "digest", tag),
                kanal = reqS(map, "kanal", tag),
                nonceReferenz = reqS(map, "nonce", tag),
                antwortReferenz = optS(map, "antwort", tag),
                identitaetsReferenz = optS(map, "identitaet", tag),
                ergebnisGrund = reqS(map, "grund", tag),
            )

            "KONFIDENZ_EXTERNALISIERT" -> KonfidenzExternalisiert(
                zeitstempel = tsFeld(map, tag),
                referenz = reqS(map, "referenz", tag),
                wert = reqDouble(map, "wert", tag),
                quelle = reqS(map, "quelle", tag),
                version = reqInt(map, "version", tag),
            )

            "KONFIDENZ_UEBERSCHRIEBEN" -> KonfidenzUeberschrieben(
                zeitstempel = tsFeld(map, tag),
                referenz = reqS(map, "referenz", tag),
                alterWert = reqDouble(map, "alt", tag),
                neuerWert = reqDouble(map, "neu", tag),
                begruendung = reqS(map, "begruendung", tag),
                version = reqInt(map, "version", tag),
            )

            else -> throw AuditFormatFehler("Unbekannter Ereignis-Tag: '$tag'")
        }

    private fun baueBelief(map: Map<String, String>, tag: String): BeliefAktualisiert {
        val anzahl = reqInt(map, "hn", tag)
        if (anzahl < 0) throw AuditFormatFehler("Negative Hypothesen-Anzahl 'hn=$anzahl' (Tag $tag)")
        val hypothesen = (0 until anzahl).map { i ->
            val evAnzahl = reqInt(map, "h$i.evn", tag)
            if (evAnzahl < 0) throw AuditFormatFehler("Negative Evidenz-Anzahl 'h$i.evn=$evAnzahl' (Tag $tag)")
            Hypothese(
                id = HypotheseId(reqS(map, "h$i.id", tag)),
                wahrscheinlichkeit = reqDouble(map, "h$i.p", tag),
                stuetzendeEvidenz = (0 until evAnzahl).map { j ->
                    EvidenzReferenz(reqS(map, "h$i.ev$j", tag))
                },
            )
        }
        return BeliefAktualisiert(
            zeitstempel = tsFeld(map, tag),
            belief = BeliefState.of(hypothesen, Resthypothese(reqDouble(map, "restp", tag))),
        )
    }

    // --- Feld-Helfer -------------------------------------------------------

    private const val FELD_TRENNER = "\t"

    private fun zeile(tag: String, vararg felder: Pair<String, String>): String =
        buildString {
            append(tag)
            for ((schluessel, wert) in felder) feld(schluessel, wert)
        }

    private fun StringBuilder.feld(schluessel: String, wert: String) {
        append(FELD_TRENNER)
        append(schluessel)
        append('=')
        append(wert)
    }

    private fun ts(zeitstempel: Zeitstempel): String = zeitstempel.epochMillis.toString()

    private fun rohFeld(map: Map<String, String>, schluessel: String, tag: String): String =
        map[schluessel] ?: throw AuditFormatFehler("Pflichtfeld '$schluessel' fehlt (Tag $tag)")

    private fun reqS(map: Map<String, String>, schluessel: String, tag: String): String =
        unesc(rohFeld(map, schluessel, tag))

    private fun optS(map: Map<String, String>, schluessel: String, tag: String): String? {
        val roh = rohFeld(map, schluessel, tag)
        if (roh == NULL_MARKER) return null
        if (!roh.startsWith(PRESENT_PREFIX)) {
            throw AuditFormatFehler("Optionales Feld '$schluessel' ohne Präsenz-Marker (Tag $tag)")
        }
        return unesc(roh.substring(PRESENT_PREFIX.length))
    }

    private fun tsFeld(map: Map<String, String>, tag: String): Zeitstempel =
        Zeitstempel(reqLong(map, "ts", tag))

    private fun reqLong(map: Map<String, String>, schluessel: String, tag: String): Long =
        rohFeld(map, schluessel, tag).toLongOrNull()
            ?: throw AuditFormatFehler("Feld '$schluessel' ist kein Long (Tag $tag)")

    private fun reqInt(map: Map<String, String>, schluessel: String, tag: String): Int =
        rohFeld(map, schluessel, tag).toIntOrNull()
            ?: throw AuditFormatFehler("Feld '$schluessel' ist kein Int (Tag $tag)")

    private fun reqDouble(map: Map<String, String>, schluessel: String, tag: String): Double =
        rohFeld(map, schluessel, tag).toDoubleOrNull()
            ?: throw AuditFormatFehler("Feld '$schluessel' ist kein Double (Tag $tag)")

    // --- Nullable-Marker + Escaping ---------------------------------------

    private const val NULL_MARKER = "null"
    private const val PRESENT_PREFIX = "s:"

    /** Kodiert einen optionalen String: `null` oder `s:<escaped>`. */
    private fun optOut(wert: String?): String =
        if (wert == null) NULL_MARKER else PRESENT_PREFIX + esc(wert)

    private fun esc(roh: String): String =
        buildString(roh.length) {
            for (c in roh) when (c) {
                '\\' -> append("\\\\")
                '\t' -> append("\\t")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(c)
            }
        }

    private fun unesc(kodiert: String): String =
        buildString(kodiert.length) {
            var i = 0
            while (i < kodiert.length) {
                val c = kodiert[i]
                if (c != '\\') {
                    append(c)
                    i++
                    continue
                }
                if (i + 1 >= kodiert.length) {
                    throw AuditFormatFehler("Unvollständige Escape-Sequenz am Feldende: '$kodiert'")
                }
                when (val n = kodiert[i + 1]) {
                    '\\' -> append('\\')
                    't' -> append('\t')
                    'n' -> append('\n')
                    'r' -> append('\r')
                    else -> throw AuditFormatFehler("Unbekannte Escape-Sequenz '\\$n' in '$kodiert'")
                }
                i += 2
            }
        }
}
