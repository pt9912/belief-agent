# Closure-Note-Reviewer-Skill — belief-agent

* **Status:** Accepted
* **Version:** 1.0 (2026-07-09)
* **Bezug:** Modul 11 (`.harness/baseline/v1.4.0/regelwerk/modul-11-verification.md`
  §Worked Example Schritt 5 — Doku-Konsistenz-Agent), Modul 1 §Closure
  (Lerneintrag-Pflicht), Modul 8 (Rollentrennung), `docs/plan/planning/README.md`
  (`done/`-Definition), `MR-009` (Welle-Closure = Lerneintrag), Source Precedence
  (`harness/README.md`).
* **Gilt für:** die **inferentielle (semantische) Prüfung der Closure-Notiz** eines
  Slice — die Schicht, die Floskeln fängt, die ein deterministischer Struktur-Check
  durchlässt (Modul 11 §Schritt 5). Sie läuft in der **Verifier/Doku-Konsistenz-Bahn**
  (Modul 8/11/15), **nicht** in der Reviewer-Bahn — siehe [§Abgrenzung](#was-dieser-skill-nicht-macht).
  Übergabe-Artefakt ist eine Finding-Liste, kein Umschreiben der Notiz.
* **Repo-Klasse:** Safety/Control (`MR-003`). Die Closure-Notiz ist der **Lerneintrag
  der Rückkante** (Modul 1): ohne ihn wird die Welle „nicht fertig, sondern nur weg".
  In einem Safety-Repo ist das die Entscheidungsspur des Harness über sich selbst —
  eine leere/floskelhafte Notiz ist Drift, kein Kosmetik-Mangel.

## Was diese Datei ist

Das repo-spezifische „woran erkennst du eine *echte* Closure-Notiz in belief-agent".
Der deterministische Teil (Sektion existiert, ist nicht der Platzhalter, hat ≥ 2 Sätze)
ist maschinell fassbar; **Inhalt vs. Floskel ist semantisch** und braucht diesen
inferentiellen Durchgang (Modul 11: „computational deckt nur die Struktur"). Sie
ersetzt **nicht** die kanonischen Quellen — bei Konflikt gewinnt die kanonische Quelle
(Source Precedence: Lastenheft → Spezifikation → Architektur → ADR → Roadmap →
`docs/user/*` → README → AGENTS.md → harness).

Hinweis: Dieses Repo hat **keinen** deterministischen `make verify-closure-notes`-Gate
(kein ADR-0011 wie im Kurs-Lab). Dieser Skill ist damit **derzeit die einzige**
Closure-Notiz-Prüfung — die Pflege-Sektion nennt den computational Komplement als
mögliche Folge-Fitness-Function (Umsetzung: Architect).

## Prüfgegenstand (wo die Notiz liegt)

Dieses Repo führt die Closure-Notiz als **Body-Sektion** (Option C, nicht Frontmatter):

- **Slice-Closure:** `## 7. Closure-Notiz (nach \`done/\`)` in jeder
  `docs/plan/planning/done/*.md` (Platzhalter vor Abschluss:
  `<!-- Erst nach Abschluss füllen. -->`).
- **Welle-Closure:** der Lerneintrag in `docs/plan/planning/done/<welle-id>-results.md`
  (`MR-009` — Wellen leben als Roadmap-Eintrag, der Lerneintrag als `…-results.md`).

Zwei etablierte, gleichwertige Formen (keine ist vorgeschrieben — Stil-Inkonsistenz
allein ist höchstens LOW):

- **Narrativ** (Präzedenz `slice-040`): „Abgeschlossen am … Implementiert wurde …
  Review-/Verification-Artefakte: … Ausgeführte Sensoren: … Keine Carveouts."
- **Strukturiert** (Präzedenz `slice-034`): `**Was funktionierte:** … **Was ging
  anders als geplant:** … **Steering-Loop:** … **Review/Verification:** …`

## Operationalisierung — vier Inhalts-Dimensionen (Modul 11 §Schritt 1)

Für jede geprüfte Closure-Notiz gilt die ADR-lose Aussage „trägt echten Lerneintrag"
prüfbar übersetzt in vier Dimensionen. Die Notiz ist **vollständig**, wenn D1–D3
getragen sind und D4 nicht verletzt ist:

| Dim | Anforderung | prüfbar als |
|---|---|---|
| **D1 Lernsignal** | ≥ 1 *konkreter* Inhalt: (a) was ging anders als geplant (z. B. „JGit lieferte Delete/Add statt `newPath`, deshalb expliziter Rename-Diff"), **oder** (b) ein konkretes Folge-Slice / eine Steering-Loop-Konsequenz, **oder** (c) eine konkrete Architektur-/Vertrags-Beobachtung. | inferentiell |
| **D2 Nachweis-Verankerung** | nennt die Review-/Verification-Artefakte (`docs/reviews/*`, `docs/verifications/*`) **und** die ausgeführten Sensoren/Gates (`make gates`/`make test`/`make doc-check`/…). | teils computational (Link-/Pfad-Existenz), teils inferentiell |
| **D3 Vollständigkeit** | Sektion ist gefüllt (nicht der Platzhalter), ≥ 2 nicht-leere Sätze. | computational |
| **D4 Floskel-Ausschluss** | *kein* syntaktisch-zwei-Sätze-aber-inhaltsleerer Text („war ganz okay, läuft jetzt", „fertig", „siehe PR", „n/a", „wird nachgereicht"). | inferentiell — der Kern dieses Skills |

D4 ist die Existenzberechtigung des Skills: „Fertig." fällt am Struktur-Check (D3),
aber „Lief alles glatt, keine Probleme." besteht D3 und **muss** hier fallen.

## Klassifikation (für dieses Repo)

**HIGH** — blockiert die `done/`-Verschiebung / bricht die Rückkante:

- **Closure-Notiz fehlt oder ist Platzhalter** in einem Slice, der in `done/` steht
  bzw. dorthin soll (`docs/plan/planning/README.md`: `done/` = „Closure-Notiz
  vorhanden"; Modul 1 §Closure).
- **Reine Floskel ohne jedes Lernsignal** (D1 gänzlich fehlend **und** D4 verletzt) —
  die Notiz existiert, trägt aber keinen auditierbaren Lerneintrag.
- **Welle-Closure ohne Lerneintrag** in `done/<welle-id>-results.md` (`MR-009`).

**MEDIUM** — sollte vor Closure geklärt werden:

- **D1 vorhanden, D2 fehlt:** keine Review-/Verification-Links **oder** keine
  Sensoren/Gates genannt — der Abschluss ist behauptet, aber nicht auf Belege
  verankert (Modul 11: Behauptung ohne Bestätigung).
- **Fehlender Steering-Loop-Eintrag**, obwohl der Slice ein Muster berührt, das schon
  ≥ zweimal aufgetreten ist (die Notiz verschweigt eine fällige Harness-Konsequenz).
- **Verweis auf nicht existierendes Artefakt:** genannter `docs/reviews/*`- oder
  `docs/verifications/*`-Pfad existiert nicht (toter Nachweis).

**LOW** — nice-to-fix, blockiert nicht:

- D1–D3 getragen, aber **knapp/schwach** (ein-Satz-Lernsignal ohne Konkretheit).
- Stil-Inkonsistenz zwischen den beiden etablierten Formen; Tippfehler in der Notiz.

**INFO** — gehört in eine **andere Rolle**, dann mit Rollen-Verweis:

- „DoD-Punkt X ist sachlich unerfüllt" → **Verifier** (Modul 11, gegen DoD/Spec).
- „Diff verstößt gegen Plan/ADR" → **Reviewer** (Modul 10).
- „append-only-/Belief-Invariante" → **Reviewer/Verifier**, nicht Closure-Note-Prüfung.

## Was dieser Skill NICHT macht

- **Keine Verifikation der DoD-Sachpunkte selbst.** Ob der Code die DoD erfüllt, prüft
  der **Verifier** gegen DoD/Spec (Modul 11, Artefakt `docs/verifications/*`). Dieser
  Skill prüft nur die *Closure-Notiz als Lerneintrag-Artefakt*.
- **Kein Review gegen Plan/ADR** — das ist der **Reviewer** (Modul 10,
  `reviewer.md`). Eine fehlende Closure-Notiz ist *kein* Review-Finding
  (Reviewer sieht sie nicht im Diff gegen Plan/ADR) — genau deshalb existiert diese
  getrennte Bahn (Modul 11 §DoD-Verletzung).
- **Kein Umschreiben der Notiz.** Der Implementer schreibt; dieser Skill *markiert*
  Floskeln und benennt die fehlende Dimension — er formuliert die Notiz nicht.
- **Keine ADR-Entscheidung** — der computational Komplement (unten) wird *vorgeschlagen*,
  nicht beschlossen (Architect).

Fällt etwas in diese Kategorien: ein INFO-Finding mit Verweis auf die zuständige Rolle.

## Output-Schema

Jedes Finding:

- `kategorie`: HIGH | MEDIUM | LOW | INFO
- `dimension`: D1 Lernsignal | D2 Nachweis | D3 Vollständigkeit | D4 Floskel | —
- `pfad`: `docs/plan/planning/done/<slice>.md:<zeile>` (die `## 7`-Sektion bzw. der
  betroffene Satz)
- `quelle`: Modul 1 §Closure · `MR-009` · `docs/plan/planning/README.md` · oder
  Rollen-Verweis (bei INFO)
- `befund`: 1–2 Sätze, beobachtbar, zitiert die floskelhafte/fehlende Stelle,
  **ohne** Ersatz-Formulierung
- `verifizierbar`: ja/nein — welcher Struktur-Check würde D3/D2-Pfad bestätigen;
  D1/D4 bleiben inferentiell (kein Gate-Ersatz)

**Wann laufen / wohin das Artefakt:**

- **Pro Slice, vor `done/`-Verschiebung** (Pre-completion Checklist, Modul 11
  §Schritt 7): die Finding-Liste ist Teil des Verifier-/Closure-Handoffs; HIGH
  blockiert die Verschiebung.
- **Als Sweep** über alle `docs/plan/planning/done/*.md` + `…-results.md`: eigenes
  Doku-Konsistenz-Artefakt `docs/verifications/<YYYY-MM-DD>-closure-notes-sweep.md`,
  ein Report pro Lauf, Folgeläufe als **neue Datei** (Auditierbarkeit) — Struktur wie
  [`docs/reviews/review-report.template.md`](../../docs/reviews/review-report.template.md),
  aber Closure-Dimensionen statt Review-Kategorien.

## Der Prüfer berichtet auch, was er nicht beanstandet hat

Eine **Negativbefund-Zeile pro geprüftem Slice** ist Pflicht — sonst ist „keine
Findings" nicht von „nicht geprüft" unterscheidbar. Beim Sweep: pro `done/`-Datei
eine Zeile „geprüft, D1–D4 getragen" oder das Finding.

## HIGH zuerst

Findings nach Kategorie ordnen, **HIGH zuerst** — nie in Datei-/Zeilen-Reihenfolge.

## Pflege (Steering-Loop)

Bei **dreimaligem** Auftreten desselben Floskel-/Lücken-Musters:

- Ist die Kategorie noch richtig? → Klassifikation hier schärfen.
- **Computational Komplement vorschlagen** (Modul 11 §Schritt 3/4): ein
  `tools/check_closure_notes.py` + `make verify-closure-notes`-Target, das D3
  (Sektion vorhanden, ≥ 2 Sätze, kein Platzhalter) und die bekannten D4-Floskeln
  deterministisch fängt — hängt an `make gates`/`verify`, nicht an `make doc-check`
  (Closure-Frage, keine Code-Architektur). Der inferentielle Rest (D4-Semantik)
  bleibt dieser Skill. Umsetzung/ADR-Bedarf: **Architect**, nicht dieser Skill.
- Trägt AGENTS.md die Pre-completion-Regel? → „Vor `done/`-Verschiebung: Closure-Notiz
  gegen D1–D4 prüfen" als AGENTS.md-Eintrag **vorschlagen** (Modul 11 §Schritt 7).

Diese Datei wird **nicht überschrieben, sondern versioniert** (Version + Geschichte
unten; konsistent zur ADR-Hard-Rule 3.5). Eine inhaltliche Änderung an einer
Accepted-Regel ist ein neuer Geschichts-Eintrag, keine stille Ersetzung.

## Geschichte

| Version | Datum | Änderung |
|---|---|---|
| 1.0 | 2026-07-09 | Initiale Fassung nach Modul 11 §Worked Example Schritt 5 (Doku-Konsistenz-Agent). Repo-spezifisch geerdet: Closure-Notiz als Body-Sektion `## 7` (Option C), zwei reale Formen (narrativ `slice-040` / strukturiert `slice-034`), vier Inhalts-Dimensionen D1–D4, Anker Modul 1 §Closure + `MR-009` + `planning/README.md` (kein ADR-0011 — Kurs-Lab, hier nicht vorhanden). Abgrenzung gegen Reviewer/Verifier (Modul 8/11). Computational Komplement als Steering-Loop-Vorschlag, nicht als Gate gebaut. Anlass: Anlage-Wunsch analog `reviewer.md` (slice-041 PR-F4-Kontext). |
