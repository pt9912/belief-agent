package dev.beliefagent.domain.belief

/**
 * Eine Beobachtung: Evidenz aus einer [Quelle] zu einem [Zeitstempel]
 * (LH-FA-OBS-006 — Quelle und Zeitstempel je Beobachtung). Reiner Domänentyp;
 * die Aufnahme über den Beobachtungs-Port und die Update-Pipeline sind
 * Out-of-Scope dieses Slices (slice-008).
 */
data class Beobachtung(
    val quelle: Quelle,
    val zeitstempel: Zeitstempel,
    val evidenz: Evidenz,
)
