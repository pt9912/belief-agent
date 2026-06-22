# Spezifikation — belief-agent

**Status:** Aktiv. **Letzte Änderung:** 2026-06-22.

**Bezug zum Lastenheft:** Diese Spezifikation präzisiert die in
`spec/lastenheft.md` formulierten Anforderungen (`LH-*`-IDs). Bei Konflikt
gewinnt das Lastenheft. Sie ist technisch verbindlich, aber ohne
Lastenheft-Änderung fortschreibbar; eine ADR darf sie schärfen.

Die hier gesetzten Schwellwerte sind **Startwerte** zu den offenen Punkten
`LH-OP-01`..`LH-OP-04` des Lastenhefts und über ADR-Schärfung anpassbar.

---

## 1. Algorithmen und Datenflüsse

### LH-FA-OBS-003.a — Bayesianisches Belief-Update

**Eingabe:** Prior-Belief `b` (Hypothesen `h_i` mit `p_i`, inkl.
Resthypothese `other`), Beobachtung `o` mit Likelihoods `L(o | h_i)` für
**jede** Hypothese inkl. `other` (`LH-FA-OBS-005`). **Ausgabe:** Posterior `b'`.
**Schritte:**

1. Unnormierte Posterior je Hypothese: `q_i = p_i · L(o | h_i)`.
2. Normierung: `p'_i = q_i / Σ_j q_j` (Posterior ∝ Prior × Likelihood).
3. Resthypothese bleibt erhalten und wird mit-aktualisiert; der bisherige
   Belief wird nicht verworfen (`LH-FA-OBS-003`).
4. Validierung: `| Σ p'_i − 1 | ≤ TOL_NORM`, `other` vorhanden — sonst
   ungültig (`LH-FA-BEL-002`, `LH-FA-BEL-004`).

**Komplexität:** O(n) über Hypothesen. **Fehlermodi:** `Σ q_j = 0` (keine
Hypothese erklärt `o`) → Resthypothese trägt Masse, ggf. Re-Hypothesenbildung.

### LH-FA-OBS-004.a — Deduplizierung korrelierter Beobachtungen

Beobachtungen mit hoher Ähnlichkeit zu bereits verarbeiteter Evidenz
werden nicht als unabhängige Evidenz mehrfach gewertet (Schutz vor
Scheingewissheit). Kriterium und Schwelle: offener Punkt `LH-OP-03`,
Startheuristik in §3.

### LH-FA-BEL-008.a — Unsicherheitsmaße

- **Entropie** `H(b) = − Σ p_i · log p_i`.
- **Top-2-Abstand** `Δ = p_(1) − p_(2)` der zwei wahrscheinlichsten
  Hypothesen (Grundlage der VoI-Auswahl).

### LH-FA-POL-002.a — Gate-Entscheidungsfunktion

**Eingabe:** Aktion mit Erfolgswahrscheinlichkeit `P_success` (getrennt
von der Diagnose-Wahrscheinlichkeit, `LH-FA-ACT-003`), Wirkungsklasse `k`,
aktuelle Resthypothese `p_other`. **Ausgabe:** `frei | ab | eskaliere`.
**Schritte:**

1. Schwelle `θ_k` der Wirkungsklasse `k` nachschlagen (§3).
2. Bei `k = extern-wirksam` und `p_other > θ_other_block`: **nie**
   freigeben (`LH-FA-POL-005`), unabhängig von `P_success`.
3. Bei `P_success ≥ θ_k`: freigeben; `k = extern-wirksam` zusätzlich nur
   mit menschlicher Freigabe (`LH-FA-POL-004`, `LH-OUT-04`).
4. Sonst: ablehnen; Eskalation, wenn günstige Beobachtungen erschöpft und
   Resthypothese hoch (`LH-FA-ESK-001`).

### LH-FA-VOI-002.a — Auswahl der nächsten Beobachtung

Bevorzugt die günstige Beobachtung mit der höchsten erwarteten
**Diskriminierung** der zwei wahrscheinlichsten Hypothesen; optional
gewichtet mit Gewinn/Kosten (`LH-FA-VOI-003`). Lokal/heuristisch zulässig
(`LH-FA-VOI-004`); keine global optimale Policy (`LH-OUT-01`).

## 2. Datenstrukturen und Schemas

### Belief State

```json
{
  "hypotheses": [
    {"id": "h1", "label": "Bug in Auth", "p": 0.50, "evidence": ["ev-1", "ev-3"]},
    {"id": "other", "label": "keine der genannten / unbekannt", "p": 0.20, "evidence": []}
  ],
  "normalized": true,
  "uncertainty": {"entropy": 1.21, "top2_margin": 0.25}
}
```

### Ereignis (Audit-Log, `LH-FA-AUD-001`)

```json
{
  "seq": 42,
  "type": "belief_updated",
  "timestamp": "2026-06-22T12:00:00Z",
  "source": "observation:test-run",
  "payload": {"observation": "ev-7", "before": "...", "after": "..."}
}
```

## 3. Defaults und Konstanten

Startwerte zu `LH-OP-01`/`LH-OP-02` — pro Wirkungsklasse die geforderte
Mindest-Erfolgswahrscheinlichkeit (`LH-FA-POL-003`); konfigurierbar
(`LH-QA-05`), schärfbar per ADR.

| Name | Wert | Begründung |
|---|---|---|
| `θ_nur-lesend` | 0.0 | nur-lesend ohne Schwelle (`LH-FA-POL-003`) |
| `θ_arbeitsbereich-lokal` | 0.50 | niedrige Schwelle, reversibel im Arbeitsbereich |
| `θ_repository-wirksam` | 0.80 | mittlere Schwelle; Commit = reversibler Checkpoint (`LH-FA-ACT-002`) |
| `θ_extern-wirksam` | 0.95 | harte Schwelle; zusätzlich menschliche Freigabe (`LH-FA-POL-004`) |
| `θ_other_block` | 0.10 | Resthypothese-Grenze, ab der extern-wirksame Aktionen blockiert sind (`LH-FA-POL-005`) |
| `θ_rehyp` | 0.30 | Resthypothese-Grenze, ab der Re-Hypothesenbildung anstößt (`LH-FA-BEL-005`) |
| `TOL_NORM` | 1e-9 | Normierungs-Toleranz (`LH-FA-BEL-002`) |
| `BUDGET_STEPS` | 20 | Default-Budget Informationssammlung vor Eskalation (`LH-FA-ESK-004`) |

## 4. Fehler-Codes und Logging-Felder

| Code | Bedingung | Aktion |
|---|---|---|
| E-BEL-001 | Belief State ohne Resthypothese | zurückweisen (`LH-FA-BEL-004`) |
| E-BEL-002 | `\| Σp − 1 \| > TOL_NORM` | zurückweisen (`LH-FA-BEL-002`) |
| E-POL-001 | extern-wirksame Aktion ohne menschliche Freigabe | ablehnen (`LH-OUT-04`) |
| E-ESK-001 | Budget erschöpft | Eskalations-Zustand (kein Fehlerstatus, `LH-FA-ESK-002`) |

## 5. Metriken und Tracing-Felder

| Span | Pflicht-Attribute | Quelle |
|---|---|---|
| `belief.update` | `hypothesis_count`, `entropy`, `top2_margin`, `residual_p` | ARC-02 |
| `gate.decide` | `action_class`, `p_success`, `threshold`, `decision`, `residual_p` | ARC-03 |
| `voi.select` | `candidate_count`, `expected_discrimination` | ARC-04 |
| `escalation.raise` | `reason`, `closed_gate`, `residual_p` | ARC-05 |

## 6. Externe Verträge

| System | Version | Vertrag-Datei |
|---|---|---|
| LLM-Port | v0 (intern) | folgt mit erstem LLM-Adapter-Slice |
| Beobachtungs-Ports | v0 (intern) | folgt mit erstem Quellen-Adapter-Slice |

## 7. Historie

| Datum | Änderung | ADR |
|---|---|---|
| 2026-06-22 | Initiale Outline (Bootstrap) | — |
