package dev.beliefagent.application.belief.aktionsvorschlag.dto

/**
 * Roher, modellnaher Aktionsvorschlag (LH-FA-LLM-002).
 *
 * Die Felder bleiben bewusst primitive Contract-Werte: Der Use Case
 * `AktionsVorschlagen` validiert sie gegen Belief, bekannte Evidenz,
 * Wirkungsklassen und den externalisierten Konfidenz-Contract, bevor daraus
 * eine gate-faehige Aktion entstehen kann.
 */
data class AktionsVorschlag(
    val beschreibung: String,
    val hypotheseId: String,
    val wirkungsklasse: String,
    val pSuccess: Double,
    val konfidenzReferenz: String,
    val stuetzendeEvidenz: List<String>,
)
