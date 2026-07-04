package dev.beliefagent.domain.belief

/**
 * Zeitstempel als **reiner Wert** (Millisekunden seit Epoch) — die Quelle der
 * Zeit ist ein Uhr-Port (slice-008), nie ein `Clock`-Aufruf im Typ. Ordnend,
 * damit das Ereignisprotokoll (slice-007) geordnet bleibt (LH-FA-AUD-001).
 */
@JvmInline
value class Zeitstempel(val epochMillis: Long) : Comparable<Zeitstempel> {
    override fun compareTo(other: Zeitstempel): Int = epochMillis.compareTo(other.epochMillis)
}
