package dev.beliefagent.domain.belief

/**
 * Wirkungsklasse einer Aktion (LH-FA-ACT-001), eingestuft nach der **Reichweite
 * der Seiteneffekte** (LH-FA-ACT-002) — nicht nach Kosten oder „Größe". Die
 * Enum-Reihenfolge ist die aufsteigende Reichweite; ein Repository-Commit gilt
 * als **reversibler Checkpoint**, nur extern-wirksame Aktionen sind
 * **irreversibel** (Deploy, E-Mail, Zahlung, DB-Migration, externer API-Aufruf).
 */
enum class Wirkungsklasse(val irreversibel: Boolean) {
    NUR_LESEND(irreversibel = false),
    ARBEITSBEREICH_LOKAL(irreversibel = false),
    REPOSITORY_WIRKSAM(irreversibel = false),
    EXTERN_WIRKSAM(irreversibel = true),
}
