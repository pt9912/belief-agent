package dev.beliefagent.adapter.audit.file

import dev.beliefagent.application.ports.AuditPort
import dev.beliefagent.domain.belief.Ereignis
import dev.beliefagent.domain.belief.EreignisProtokoll
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Persistenter, nicht-Memory Audit-Adapter (ARC-08, slice-041): speichert das
 * append-only [EreignisProtokoll] dauerhaft in **einer Datei** ([pfad]) über
 * `java.nio` und lädt es nach Prozess-Neustart rekonstruierbar (LH-FA-AUD-001/
 * 002). Ersetzt `MemoryAudit` dort, wo die Entscheidungsspur einen Neustart
 * überleben muss; das Format ist [EreignisSerialisierung] (`LH-QA-06`
 * inspizierbar).
 *
 * **Fail-closed an der Grenze (LH-QA-02, §9 DR-F1/DR-R1):** Schreib-, Lese- und
 * Formatfehler **werfen** ([AuditPersistenzFehler]) — der Adapter gibt bei einem
 * defekten Store **nie** still ein leeres Protokoll zurück. Der `AuditPort`-
 * Vertrag bleibt unverändert (kein `Result`).
 *
 * **Trailing-Truncation (§9 IDR-2):** Ein einzelner abgeschnittener **letzter**
 * Record (Crash während `anhaengen`) wird toleriert — die N-1 vollständigen
 * Records werden rekonstruiert und der Rest über [warnung] **sichtbar** gemeldet
 * (`LH-QA-06`). Ein **Interior**-Defekt (Zeile vor der letzten) bleibt ein lauter
 * [AuditFormatFehler].
 *
 * **Append-only** ist gegen die Adapter-API garantiert; die zeitliche Ordnung
 * erzwingt `EreignisProtokoll.von(...)` (Domäne), nicht dieser Adapter (§9 DR-F3).
 *
 * **Annahmen (dokumentiert, §9 IDR-3/IDR-4):** Single-Writer (ein Prozess/ein
 * Schreiber, heutige single-threaded Runtime); Tamper-Evidenz gegen ein
 * Out-of-Band-Umschreiben der Datei ist **out of scope** (Klartext-Format ohne
 * Hash-Chain). File-Locking und Tamper-Evidenz sind Folgeslices (slice-053/054).
 *
 * @param pfad Zieldatei des persistenten Protokolls.
 * @param warnung Sichtbarkeits-Kanal für tolerierte Trailing-Truncation
 *   (Default: `System.err`); injizierbar, damit die Sichtbarkeit deterministisch
 *   testbar ist.
 */
class DateiAudit(
    private val pfad: Path,
    private val warnung: (String) -> Unit = { System.err.println(it) },
) : AuditPort {

    override fun anhaengen(ereignis: Ereignis) {
        val zeile = EreignisSerialisierung.kodiere(ereignis)
        try {
            pfad.parent?.let { Files.createDirectories(it) }
            // Ein unvollständiger (nicht mit '\n' abgeschlossener) Rest ist ein
            // Crash-Fragment (§9 IDR-2), das nie committet wurde — vor dem Anhängen
            // fallen lassen, sonst verklebt es mit dem neuen Record zu einer Zeile
            // und korrumpiert den Store dauerhaft (Code-Safety-Review HIGH-1).
            entferneUnvollstaendigenRest()
            val braucheHeader = !Files.exists(pfad) || Files.size(pfad) == 0L
            val nutzlast = buildString {
                if (braucheHeader) {
                    append(EreignisSerialisierung.HEADER)
                    append('\n')
                }
                append(zeile)
                append('\n')
            }
            Files.write(
                pfad,
                nutzlast.toByteArray(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
            )
        } catch (e: IOException) {
            throw AuditSchreibFehler("Audit-Schreiben fehlgeschlagen: $pfad", e)
        }
    }

    /**
     * Kürzt einen unvollständigen Record am Dateiende (Bytes ohne abschließendes
     * '\n') bis einschließlich des letzten '\n'. Das Newline ist der Commit-Marker:
     * ein Record ohne '\n' ist ein Crash-Fragment (§9 IDR-2), das nie committet
     * wurde und daher — konsistent zum Lesepfad, der es verwirft und meldet — vor
     * dem nächsten Anhängen fallen gelassen wird. Schneller Pfad: endet die Datei
     * bereits mit '\n' (Normalfall nach jedem sauberen Append), passiert nichts.
     * Wird tatsächlich ein Fragment verworfen, meldet der Adapter das über
     * [warnung] — symmetrisch zum Lesepfad, damit der Drop **sichtbar** ist
     * (`LH-QA-06`), auch wenn die Bytes nie committet waren.
     */
    private fun entferneUnvollstaendigenRest() {
        if (!Files.exists(pfad)) return
        val groesse = Files.size(pfad)
        if (groesse == 0L) return
        val endetMitNewline = Files.newByteChannel(pfad, StandardOpenOption.READ).use { kanal ->
            val puffer = ByteBuffer.allocate(1)
            kanal.position(groesse - 1)
            kanal.read(puffer)
            puffer.get(0) == NEWLINE
        }
        if (endetMitNewline) return
        val inhalt = Files.readAllBytes(pfad)
        var grenze = inhalt.size
        while (grenze > 0 && inhalt[grenze - 1] != NEWLINE) grenze--
        Files.newByteChannel(pfad, StandardOpenOption.WRITE).use { it.truncate(grenze.toLong()) }
        warnung(
            "Audit: unvollständiges Crash-Fragment vor Append verworfen " +
                "(${inhalt.size - grenze} Bytes, nie committet): $pfad",
        )
    }

    override fun lade(): EreignisProtokoll {
        val bytes = try {
            if (!Files.exists(pfad)) return EreignisProtokoll.LEER
            Files.readAllBytes(pfad)
        } catch (e: IOException) {
            throw AuditLeseFehler("Audit-Lesen fehlgeschlagen: $pfad", e)
        }
        if (bytes.isEmpty()) return EreignisProtokoll.LEER // legitim leer ≠ Fehler

        val text = String(bytes, StandardCharsets.UTF_8)
        val hatAbschluss = text.endsWith('\n')
        // Record-Grenze = Newline. Ein abschließendes '\n' erzeugt ein leeres
        // Trailing-Element, das kein Record ist; ohne '\n' ist das letzte Element
        // der (potenziell) abgeschnittene Trailing-Record.
        val zeilen = text.split('\n').let { if (hatAbschluss) it.dropLast(1) else it }

        if (zeilen.isEmpty() || zeilen[0] != EreignisSerialisierung.HEADER) {
            throw AuditFormatFehler(
                "Ungültiger oder fehlender Audit-Header (erwartet '${EreignisSerialisierung.HEADER}'): $pfad",
            )
        }

        val nachHeader = zeilen.drop(1)
        val (vollstaendige, abgeschnitten) =
            if (hatAbschluss || nachHeader.isEmpty()) {
                nachHeader to null
            } else {
                nachHeader.dropLast(1) to nachHeader.last()
            }

        val ereignisse = vollstaendige.map { EreignisSerialisierung.dekodiere(it) }

        if (abgeschnitten != null) {
            warnung(
                "Audit: abgeschnittener Trailing-Record verworfen (${abgeschnitten.length} Zeichen), " +
                    "${ereignisse.size} vollständige Ereignisse rekonstruiert: $pfad",
            )
        }

        return try {
            // Ordnung/Append-only als Domain-Invariante (kein Rück-Datieren);
            // eine Verletzung im gespeicherten Store ist ein lauter Integritätsfehler.
            EreignisProtokoll.von(ereignisse)
        } catch (e: IllegalArgumentException) {
            throw AuditFormatFehler("Ordnungsverletzung im persistierten Protokoll: ${e.message}", e)
        }
    }

    private companion object {
        private val NEWLINE: Byte = '\n'.code.toByte()
    }
}
