package dev.beliefagent.adapter.audit.file

/**
 * Fehler an der Persistenz-Grenze des Datei-Audit-Adapters (slice-041).
 *
 * **Fail-closed-Sichtbarkeit (LH-QA-02, LH-FA-AUD-002/003):** Der Adapter gibt
 * bei Schreib-/Lese-/Format-Fehlern **nie** still ein leeres Protokoll zurück,
 * sondern **wirft**. Der Fehlerkanal ist bewusst die Exception — der
 * `AuditPort`-Vertrag bleibt unverändert (kein `Result`-Umbau, §9 DR-F1). Die
 * geordnete Eskalation der Konsumenten (fail-closed im Write-Pfad, Read-Fail-
 * Handling) ist benannte Folgearbeit (slice-051/052); dieser Adapter garantiert
 * nur die **laute** Grenze.
 */
open class AuditPersistenzFehler(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/** Schreiben ins persistente Protokoll fehlgeschlagen (IO). */
class AuditSchreibFehler(
    message: String,
    cause: Throwable? = null,
) : AuditPersistenzFehler(message, cause)

/** Lesen des persistenten Protokolls fehlgeschlagen (IO). */
class AuditLeseFehler(
    message: String,
    cause: Throwable? = null,
) : AuditPersistenzFehler(message, cause)

/**
 * Deserialisierungs-/Integritätsfehler: unbekannter Typ-Tag, fehlendes
 * Pflichtfeld, defekter **Interior**-Datensatz, ungültiger Header oder eine
 * Ordnungsverletzung (Rück-Datierung) im gespeicherten Protokoll. Ein
 * abgeschnittener **Trailing**-Record ist **kein** Format-Fehler, sondern wird
 * toleriert und sichtbar gemeldet (§9 IDR-2).
 */
class AuditFormatFehler(
    message: String,
    cause: Throwable? = null,
) : AuditPersistenzFehler(message, cause)
