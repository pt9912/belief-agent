package dev.beliefagent.domain.belief

/**
 * Heterogene Beobachtungsquellen (LH-FA-OBS-001): mindestens Testergebnisse,
 * Build-Ergebnisse, Logs, menschliches Feedback und Repository-Inspektion.
 */
enum class Quelle {
    TEST,
    BUILD,
    LOG,
    MENSCH,
    REPO,
}
