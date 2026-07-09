# Review-Report: slice-041 Code-/Safety-Review **Rerun** (Frischkontext) — 2026-07-09

**Review-Art:** Code-Safety **Rerun** — gezielte Nachprüfung, ob der
merge-blockierende **HIGH-1** aus dem Erst-Lauf (`…-code-safety-review.md`) behoben
ist und ob der Fix neue Probleme einführt. Neuer, unabhängiger Frischkontext-Lauf
(Modul 8) — hat den Fix **nicht** geschrieben.

**Gegenstand:** `adapters/outbound/audit-file/` nach dem HIGH-1-Fix
(`DateiAudit.entferneUnvollstaendigenRest()` + drei neue `resume_*`-Tests).

**Skill:** `.harness/skills/reviewer.md` @ v1.0 <!-- d-check:ignore (Skill-Pfad) -->
**Modell:** claude-opus-4-8[1m] (Frischkontext-Subagent) · **Datum:** 2026-07-09

---

## Findings

**HIGH-1 bestätigt behoben, keine neuen HIGH/MEDIUM.** Ein neuer LOW-Befund.

### LOW-1 — Write-Pfad-Heilung verwarf das Crash-Fragment still (Sichtbarkeits-Asymmetrie zum Lesepfad)

- `kategorie`: LOW
- `quelle`: `LH-QA-06` (Sichtbarkeit/Inspizierbarkeit), Konsistenz zu §9 IDR-2
- `pfad`: `DateiAudit.kt` — `entferneUnvollstaendigenRest` (Write) vs. Trailing-Toleranz im Lesepfad
- `befund`: Trifft `lade()` zuerst auf ein Trailing-Fragment, wird es über `warnung`
  sichtbar gemeldet; trifft `anhaengen()` zuerst darauf (typischer Resume: Neustart
  → anhängen ohne vorheriges Laden), wurde das Fragment **still** gekürzt. Verworfen
  werden nie-committete Bytes (keine Append-only-/Rekonstruierbarkeits-Verletzung →
  LOW). Reine Sichtbarkeits-Asymmetrie.
- `verifizierbar`: ja — `make test`.
- **Disposition (Implementation, nach diesem Lauf):** behoben — der Heal-Pfad meldet
  den Fragment-Drop jetzt über denselben `warnung`-Kanal (symmetrisch zum Lesepfad).
  Bestätigt durch `resume_meldet_verworfenes_fragment_sichtbar` + Gegenprobe
  `sauberer_append_meldet_keine_warnung`.

## Negativbefunde

- **HIGH-1 Regelfall (abgeschnittener Tag) — behoben:** Rückwärtslauf zum letzten
  rohen `\n` kürzt `HEADER\nR1\nR2\nGATE_ABGE` → `HEADER\nR1\nR2\n`; sauberer Append,
  keine verklebte Zeile. Test `resume_append_nach_trailing_truncation…`.
- **HIGH-1 Silent-Zweig (`=`-haltiges Mid-Value-Fragment) — behoben:** Kürzung vor
  Append verhindert die LinkedHashMap-Last-Wins-Fabrikation. Test
  `resume_nach_mittiger_wert_truncation…`. Beide HIGH-1-Anker geschlossen.
- **Kein committeter Record geht verloren:** Rückwärtslauf nur bei fehlendem
  Trailing-`\n`, entfernt nur Bytes **nach** dem letzten rohen `\n`. Stütze: `esc()`
  escaped jedes rohe `\n`/`\r`/`\t`/`\\` im Wert → rohes `\n` existiert nur als
  Record-Terminator → Grenze landet immer auf echter Record-Grenze.
- **Edge Cases geheilt:** leere/nicht-existente Datei (Guards → Header frisch),
  Exakt-`\n`-Ende (Schnellpfad), torn Header ohne `\n` (`grenze=0` → `truncate(0)` →
  frischer Header, Test `resume_nach_abgeschnittenem_header…`), Interior-Defekt +
  Trailing-Fragment (nur Fragment gekürzt, Interior wirft weiter laut).
- **Fail-closed durchgehalten:** `entferneUnvollstaendigenRest` läuft im `try` von
  `anhaengen`; jede `IOException` → `AuditSchreibFehler`; keine interne Schluckstelle.
  Lesepfad unverändert fail-closed.
- **MEDIUM-1 weiterhin offen, nicht still verändert:** `anhaengen` liest den letzten
  Zeitstempel nach wie vor nicht; Ordnung ausschließlich am Load über
  `EreignisProtokoll.von(...)` erzwungen — konform §9 DR-F3, korrekt als Rückkante
  Review→Plan geführt.

## Summary

| Kategorie | Anzahl |
|---|---|
| HIGH | 0 (HIGH-1 behoben) |
| MEDIUM | 0 neu (MEDIUM-1 unverändert offen — DR-F3-Spannung) |
| LOW | 1 (LOW-1 — nach diesem Lauf behoben) |
| INFO | 1 unverändert (INFO-1 unbegrenztes `readAllBytes`) |

## Verdikt

**Ursprünglicher HIGH-1-Blocker aufgehoben: JA.** Der Fix kürzt vor jedem Append
zuverlässig bis zur letzten echten Record-Grenze (garantiert durch vollständiges
Newline-Escaping); beide HIGH-1-Zweige erzeugen keine Verklebung mehr, kein
committeter Record geht verloren, fail-closed und Commit-Marker-Modell bleiben
gewahrt. MEDIUM-2 (Resume-Tests) adressiert. **Offen für die Zeit vor produktiver
Bindung:** MEDIUM-1 (Write-Ordnungs-Divergenz — Architect/Planner, DR-F3-Spannung).
INFO-1 akzeptierte Grenze. LOW-1 nach diesem Lauf behoben (Warn-Symmetrie).

## Geschichte

| Version | Datum | Änderung |
|---|---|---|
| 1 | 2026-07-09 | Rerun nach HIGH-1-Fix. HIGH-1 bestätigt behoben; MEDIUM-2 adressiert; LOW-1 neu (danach behoben); MEDIUM-1/INFO-1 unverändert. |
