# Reviewer-Skill — belief-agent

* **Status:** Accepted
* **Version:** 1.0 (2026-07-09)
* **Bezug:** Modul 10 (`.harness/baseline/v1.4.0/regelwerk/modul-10-review-harness.md`
  §Worked Example), `AGENTS.md §3` (Harte Regeln), Source Precedence
  (`harness/README.md`), Rollentrennung Modul 8.
* **Gilt für:** die **Reviewer-Rolle** (Modul 8), die Review-Reports unter
  `docs/reviews/*` erzeugt — Plan-, Design- und Code-Review. Es gibt kein
  `agent-review`-`make`-Target; die Rolle ist agenten-/manuell ausgeführt, das
  Übergabe-Artefakt ist der Report.
* **Repo-Klasse:** Safety/Control (`MR-003`). Der Kern ist eine
  Sicherheitsfunktion (Konfidenz-Gate, Freigabe, Audit) — **Safety-Findings
  haben Vorrang**.

## Was diese Datei ist

Das repo-spezifische „worauf achtest du in belief-agent". Ohne sie driftet der
Reviewer zwischen Sessions: dieselbe Eingabe → andere Findings/Kategorien
(Modul 10). Sie ersetzt **nicht** die kanonischen Quellen — bei Konflikt gewinnt
die kanonische Quelle (Source Precedence: Lastenheft → Spezifikation →
Architektur → ADR → Roadmap → `docs/user/*` → README → AGENTS.md → harness).

## Drei Review-Arten — wogegen (Modul 10)

- **Plan-Review** — Slice-Plan gegen Spec (`LH-*`) + Accepted-ADRs, **vor** Code,
  ohne Diff (Eingabe: der Plan).
- **Design-Review** — Lösungs-Schnitt gegen Architektur: Layer-Grenzen, Ports,
  ADR-Verträglichkeit einer neuen Komponente, **bevor** Details festgezurrt sind.
- **Code-Review** — fertiger Diff gegen Plan + Konventionen (`AGENTS.md`
  Hard Rules).

Merkregel: je früher die Review-Art, desto billiger das Finding.

**Repo-Zusatz — Code-Safety-Review.** Berührt ein Diff eine Sicherheitsfunktion
(Konfidenz-Gate, Human-Approval, Audit/Ereignisprotokoll, Eskalation,
Executor-Grenze), läuft ein **eigener** Durchgang mit Fokus fail-closed /
Nicht-Umgehbarkeit; er wird als getrenntes Artefakt
`docs/reviews/<datum>-<slice>-code-safety-review.md` geführt (etablierte Praxis
`slice-035`..`040`).

## Kontext-Eingang (Pflicht)

Was der Reviewer **immer** mitbringt, bevor er Plan/Diff liest — ohne diesen
Block sieht er Code, aber nicht die Verträge, gegen die er prüft:

- **Prüfgegenstand:** Slice-Plan (Plan/Design) bzw. Diff **plus** Plan-Verweis (Code).
- `spec/lastenheft.md` — die referenzierten `LH-*`-IDs (Rang 1, abnahmebindend).
- `spec/spezifikation.md`, `spec/architecture.md` — Spec-Strata 2/3, `ARC-*`-Anker.
- ADRs, deren ID im Plan/PR/Commit vorkommt (`docs/plan/adr/`), **inkl.
  Supersedes-Kette** (z. B. `ADR-0008` supersedet `ADR-0005`).
- `AGENTS.md §3` (Hard Rules) + `harness/README.md` (Konfliktauflösungs-Klausel).
- **vorherige Findings** an gleichem Modul/Sub-Area (`docs/reviews/`, letzte ~5).
- **Bei Safety-Pfaden** der Ist-Code der berührten Ports/Regeln: `AuditPort`,
  `AktionGaten`/`KonfidenzGate`/`GateSchwellen`, `HumanApprovalPort`,
  `EreignisProtokoll`/`Rekonstruktion`, `Entscheidungszyklus`.
- Build-/Arch-Realität bei Modul-Änderung: `settings.gradle.kts`, `.a-check.yml`,
  `Dockerfile`.

## Klassifikation (für dieses Repo)

**HIGH** — blockiert Merge; eines der folgenden:

- **Gate-Umgehung:** ein Aktionspfad, der das Konfidenz-Gate auslässt oder von
  außerhalb umgeht (`LH-FA-POL-006`, Hard Rule 3.7).
- **Irreversible Aktion ohne Schutz:** extern-wirksame Aktion ohne harte Schwelle
  **und** menschliche Freigabe, abschaltbare Freigabe, oder Freigabe trotz hoher
  Resthypothese (`LH-FA-POL-004/005`, `LH-OUT-04`, Hard Rule 3.8).
- **fail-open statt fail-safe:** im Zweifel handeln statt sammeln/eskalieren;
  stiller Erfolg / leeres Ergebnis, wo ein sichtbarer Fehler/Eskalation gehört
  (`LH-QA-02`).
- **Audit-Invariante verletzt:** Überschreiben/Löschen/Rück-Datieren im
  Ereignisprotokoll, oder Mutation eines Eintrags statt neues Ereignis
  (`LH-FA-AUD-001`, `LH-FA-LLM-003`).
- **Belief-Invariante verletzt:** ein Belief State ohne Resthypothese oder ohne
  Normierung wird nicht zurückgewiesen (`LH-FA-BEL-002/003/004`).
- **Architektur-/ADR-Verstoß:** Kern importiert Adapter/Framework/DI; `commonMain`
  sieht ein Adapter-/`org.koin`-Paket; Abhängigkeit nach außen
  (`domain→application`, `application→adapter`) (`ADR-0001/0002/0003`,
  `a-check`-Rollen `domain/app/port/adapter`).
- **ADR-Immutabilität/Gate-Lockern:** Accepted-ADR inhaltlich überschrieben statt
  Folge-ADR mit `Supersedes` (Hard Rule 3.5); Coverage-/Linter-/Arch-Schwelle
  gesenkt ohne ADR (Hard Rule 3.6).
- **Suppression:** `@Suppress`/`//noqa`/arch-/coverage-Ausnahme ohne zentrale
  Begründung/ADR (Hard Rule 3.2).

**MEDIUM** — sollte vor Merge geklärt werden; eines der folgenden:

- **Unklarer Schicht-Besitz:** der Schnitt lässt offen, welche Schicht einen
  Vertrag/eine Entscheidungsspur besitzt (Layer-Kopplungs-Risiko) — z. B.
  Ereigniserzeugung über Gate/Dispatcher/Adapter verteilt.
- **Fehlende Fehlersemantik:** Port-Vertrag ohne Fehler-/Ergebnistyp dort, wo
  fail-closed gefordert ist (Sichtbarkeit hängt an undokumentiertem Wurf).
- **Domain-Invariante dupliziert:** Adapter reimplementiert eine Domänenregel
  (`EreignisProtokoll`/`Rekonstruktion`) statt sie zu nutzen (Doppel-Quelle,
  Drift-Risiko).
- **Fehlende Negativtests** bei neuem öffentlichem Vertrag/Safety-Pfad: kein Test
  für den geschlossenen/ablehnenden/eskalierenden Ast (`LH-QA-03`).
- **Fehlende ID-Referenz:** Plan/Diff nennt berührte `LH-*`/`ADR`/`ARC`-IDs nicht,
  obwohl ein öffentlicher Vertrag betroffen ist (Doku-Regel `AGENTS.md §5`).
- **Wiederholung** eines Musters, das schon zweimal LOW war.

**LOW** — nice-to-fix, blockiert nicht: stilistisch/lokal ohne Safety-Wirkung;
unvollständige ID-Abdeckung bei **Soll**-Anforderungen; einmalige Tippfehler;
ungenutzte Importe; Plan-interne Formulierungs-Inkonsistenz.

**INFO** — Hinweis ohne erwartete Aktion, oder etwas, das in eine **andere Rolle**
gehört (Verifier/Validator/Architect) — dann als INFO **mit Rollen-Verweis**.

## Was dieser Skill NICHT macht

- **Keine Lösungsvorschläge** („schreib das so") — Reviewer kategorisiert,
  Implementer/Architect entscheidet. Einen offenen **Optionen-Raum** darf der
  Befund *beobachtend benennen* (was ist unentschieden), nicht *vorschreiben*.
- Kein Refactoring-Vorschlag über den Diff/Plan hinaus.
- **Keine Verifikation** gegen DoD/Spec — das ist Verifier (Modul 11), eigenes
  Artefakt `docs/verifications/*`.
- **Keine Validation** gegen realen Bedarf — das ist Validator.
- **Keine ADR-Entscheidung** — Architect schreibt (Folge-)ADR; Reviewer prüft nur
  auf Konsistenz.

Fällt etwas in diese Kategorien: ein INFO-Finding mit Verweis auf die zuständige
Rolle.

## Output-Schema

Jedes Finding:

- `kategorie`: HIGH | MEDIUM | LOW | INFO
- `quelle`: `LH-*`-ID, `ADR-NNNN`, `ARC-NN`, Hard-Rule-Name oder „Maintainability"
- `pfad`: `Datei:Zeile`
- `befund`: 1–2 Sätze, beobachtbar, **ohne Lösungsvorschlag**
- `verifizierbar`: ja/nein — welcher `make`-Gate-Lauf würde es bestätigen?
  (`make arch-check` · `make test` · `make coverage-gate` · `make doc-check` ·
  `make build`; range-basiert `make doc-immutable` · `make doc-commits`)

Zusätzlich am Ende **eine Negativbefund-Zeile pro betrachtetem Bereich**. Report
aus `docs/reviews/review-report.template.md` kopieren; Ablage
`docs/reviews/<YYYY-MM-DD>-<slice-oder-diff-ref>-<art>.md`, ein Report pro Lauf —
Folgeläufe als **neue Datei**, keine Überschreibung (Auditierbarkeit).

## Reviewer berichtet auch, was er nicht gefunden hat

Negativbefund-Zeile pro betrachteter Sub-Area ist **Pflicht** — sonst ist „keine
Findings" nicht von „nicht geprüft" unterscheidbar. Für belief-agent mindestens,
soweit berührt: `hexagon/domain`, `hexagon/application` (+ betroffene Ports),
die betroffenen `adapters/*`, sowie `spec/` bzw. `docs/plan/adr/` bei
Vertragsbezug. Dieser Block ist der Teil, den ein Reviewer am ehesten weglässt —
er wird hier eingefordert.

## HIGH zuerst

Findings nach Kategorie ordnen und darstellen, **HIGH zuerst** — nie in
Datei-/Zeilen-Reihenfolge. Der Implementer arbeitet sonst sequenziell ab und
bleibt am LOW hängen.

## Pflege (Steering-Loop)

Bei **dreimaligem** Auftreten desselben Findings:

- Ist die Kategorie noch richtig? → Klassifikation hier schärfen.
- Gibt es einen ADR-/`AGENTS.md`-Eintrag, der es verhindert hätte? → Folge-ADR
  oder `AGENTS.md`-Update **vorschlagen** (Umsetzung: Architect).
- Gibt es eine Fitness Function / ein Gate, das es prüfen würde? → Gate ergänzen
  (Modul 13).

Diese Datei wird **nicht überschrieben, sondern versioniert** (Version + Geschichte
unten; konsistent zur ADR-Hard-Rule 3.5). Eine inhaltliche Änderung an einer
Accepted-Regel ist ein neuer Geschichts-Eintrag, keine stille Ersetzung.

## Geschichte

| Version | Datum | Änderung |
|---|---|---|
| 1.0 | 2026-07-09 | Initiale Fassung nach Modul 10 §Worked Example (6 Schritte). Repo-spezifische HIGH/MEDIUM-Anker (Gate-Nicht-Umgehbarkeit, Freigabe, append-only-Audit, Hexagonal-/KMP-Reinheit, Belief-Invarianten), Safety-Review-Zusatz, Output-Schema an die realen `make`-Gates gebunden. Anlass: `slice-041`-Plan-Review, Finding F-4 (fehlender Reviewer-Skill). |
