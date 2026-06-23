# Lastenheft — belief-agent

| Feld | Wert |
|------|------|
| **Titel** | Lastenheft `belief-agent` |
| **Projekt** | belief-agent |
| **Version** | 0.4 (Entwurf) |
| **Datum** | 2026-06-23 |
| **Status** | in Abstimmung |
| **Art** | Lastenheft — ausschließlich Anforderungen (das *Was*, nicht das *Wie*) |

---

## 1. Zweck und Zielsetzung

`belief-agent` ist ein Agenten-Framework, das **Unsicherheit explizit modelliert, Evidenz aktiv
sammelt und irreversible Aktionen durch Konfidenzschwellen absichert**. Ziel ist ein autonomer
Software-Agent, der nicht Schritt für Schritt eine angenommene Wahrheit ausführt, sondern eine
Verteilung über konkurrierende Hypothesen führt, gezielt Information beschafft und erst handelt,
wenn die Konfidenz die Risikoklasse der Aktion deckt — andernfalls eskaliert.

Leitsatz: **Der Agent weiß, wann er nicht genug weiß.**

## 2. Ausgangslage und Motivation

Coding-Agenten scheitern selten an fehlender linearer Zustandsschätzung, sondern an **schlechter
Unsicherheitsrepräsentation**. Probleme in einem Repository sind diskret und hypothesenförmig
(„Bug in Auth 55 % / Frontend 25 % / Gateway 20 %"), nicht kontinuierlich-gaußsch. Daher tritt an
die Stelle eines Kalman-Zustands ein **Belief State über Hypothesen**. Das Sprachmodell ist dabei
nicht der Agent, sondern ein austauschbares Modul, dessen implizite Konfidenz in explizite,
prüfbare und gate-fähige Zahlen überführt wird.

## 3. Geltungsbereich und Abgrenzung

Das Lastenheft beschreibt die fachlichen Anforderungen an den Kern des Frameworks: Belief-Modell,
Beobachtung/Evidenz, Aktionsklassifikation, Entscheidungspolitik, Informationsbeschaffung,
Eskalation, Audit und die Rolle des Sprachmodells. Technische Realisierung (Sprache, Frameworks,
Klassendesign) ist **nicht** Gegenstand dieses Dokuments (siehe `spec/spezifikation.md`). Explizite
Nicht-Ziele sind in Abschnitt 9 als Anforderungen festgehalten.

## 4. Begriffe (Glossar)

| Begriff | Bedeutung |
|---------|-----------|
| **Belief State** | Aktueller Wissensstand als normierte Wahrscheinlichkeitsverteilung über konkurrierende Hypothesen. |
| **Hypothese** | Eine mögliche Erklärung/Ursache mit zugeordneter Wahrscheinlichkeit und stützender Evidenz. |
| **Resthypothese (`other`)** | Pflicht-Hypothese „keine der genannten / unbekannt", die die nicht zugeordnete Wahrscheinlichkeitsmasse trägt. |
| **Beobachtung** | Ein Sachverhalt aus der Umgebung (Test-, Build-Ergebnis, Log, menschliches Feedback, Repository-Inspektion), der zu Evidenz wird. |
| **Evidenz** | Eine Beobachtung in ihrer Wirkung auf den Belief State. |
| **Belief-Update** | Bayesianische Neugewichtung der Hypothesen anhand einer Beobachtung. |
| **Aktion** | Eine vom Agenten ausführbare Operation (lesen, suchen, testen, ändern, committen, deployen …). |
| **Wirkungsklasse** | Einstufung einer Aktion nach Reichweite ihrer Seiteneffekte. |
| **Erfolgswahrscheinlichkeit** | P(Aktion erreicht Ziel \| aktueller Belief) — die für Gates maßgebliche Größe. |
| **Konfidenz-Gate** | Vorab-Prüfung, die eine Aktion freigibt, ablehnt oder eskaliert. |
| **Value of Information (VoI)** | Erwarteter Erkenntnisgewinn einer Beobachtung; Grundlage der Auswahl der nächsten Beobachtung. |
| **Günstige Beobachtung** | Eine Beobachtung mit geringen Kosten (Zeit/Aufwand/Risiko) relativ zu ihrem erwarteten Informationsgewinn. |
| **Eskalation** | Geordnete Übergabe an einen Menschen, wenn der Agent ohne ausreichende Konfidenz bleibt. |

## 5. Schlüsselwörter und Kennungsschema

**Priorität** der Anforderungen:
- **Muss** — zwingend (MUST).
- **Soll** — wichtig, begründete Abweichung möglich (SHOULD).
- **Kann** — optional (MAY).

**Kennungsschema (kanonisch, RTM-fähig):**

- **Funktionale Anforderungen:** `LH-FA-<BEREICH>-<NNN>`. Bereiche: `BEL` Belief-Kern, `OBS`
  Beobachtung/Evidenz, `ACT` Aktionen/Wirkungsklassen, `POL` Politik/Gates, `VOI` Value of
  Information, `ESK` Eskalation, `AUD` Audit/Ereignisse, `LLM` Sprachmodell.
- **Qualitätsanforderungen:** `LH-QA-<NN>` (nichtfunktional).
- **Randbedingungen/Annahmen** `LH-RB-<NN>`, **Nicht-Ziele** `LH-OUT-<NN>`, **offene Punkte**
  `LH-OP-<NN>` — Constraints, Abgrenzungen und TODOs (keine FA/QA-Anforderungen, nicht RTM-relevant).

Kennungen sind stabil und werden nicht wiederverwendet. Jede Anforderung ist als eigene Überschrift
`#### <ID> — <Titel>` (FA) bzw. `### <ID> — <Titel>` (QA) geführt (Rückverfolgbarkeit,
RTM-Fähigkeit); die Priorität steht als `**Prio:**`-Feld im Anforderungstext.

---

## 6. Funktionale Anforderungen

### 6.1 Belief-Kern (LH-FA-BEL)

#### LH-FA-BEL-001 — Belief State über konkurrierende Hypothesen

**Prio:** Muss. Das System muss zu jeder Aufgabe eine Menge konkurrierender Hypothesen mit
zugeordneten Wahrscheinlichkeiten führen (Belief State), statt mit einer einzelnen angenommenen
Wahrheit zu arbeiten.

#### LH-FA-BEL-002 — Normierung der Hypothesen-Wahrscheinlichkeiten

**Prio:** Muss. Die Wahrscheinlichkeiten aller Hypothesen müssen zu jedem Zeitpunkt normiert sein
(Summe = 1 innerhalb einer definierten Toleranz).

#### LH-FA-BEL-003 — Pflicht-Resthypothese

**Prio:** Muss. Der Belief State muss jederzeit eine Resthypothese („keine der genannten /
unbekannt") mit eigener Wahrscheinlichkeitsmasse enthalten.

#### LH-FA-BEL-004 — Zurückweisung ungültiger Belief States

**Prio:** Muss. Ein Belief State ohne Resthypothese oder ohne Normierung muss als ungültig
zurückgewiesen werden. *Abnahme:* Es darf kein gültiger Belief State ohne Resthypothese und keiner
ohne Normierung (Summe = 1 innerhalb der Toleranz, vgl. LH-FA-BEL-002) existieren können.

#### LH-FA-BEL-005 — Re-Hypothesenbildung bei hoher Resthypothese

**Prio:** Muss. Überschreitet die Resthypothese einen konfigurierbaren Schwellwert, muss das System
die Erzeugung neuer bzw. verfeinerter Hypothesen anstoßen.

#### LH-FA-BEL-006 — Dynamischer Hypothesenraum

**Prio:** Muss. Der Hypothesenraum muss dynamisch erweiterbar und verfeinerbar sein; er darf nicht
statisch fixiert sein (neue Beobachtungen können neue Hypothesen hervorbringen).

#### LH-FA-BEL-007 — Rückverfolgbarkeit Hypothese → Evidenz

**Prio:** Muss. Jede Hypothese muss die sie stützende Evidenz referenzierbar machen
(Rückverfolgbarkeit Hypothese → Evidenz).

#### LH-FA-BEL-008 — Unsicherheitsmaße

**Prio:** Soll. Das System soll Unsicherheitsmaße (z. B. Entropie, Abstand der zwei
wahrscheinlichsten Hypothesen) berechnen und bereitstellen.

### 6.2 Beobachtung und Evidenz (LH-FA-OBS)

#### LH-FA-OBS-001 — Heterogene Beobachtungsquellen

**Prio:** Muss. Das System muss heterogene Beobachtungsquellen als Evidenz aufnehmen können,
mindestens: Testergebnisse, Build-Ergebnisse, Logs, menschliches Feedback, Repository-Inspektion.

#### LH-FA-OBS-002 — Nachvollziehbares Belief-Update je Beobachtung

**Prio:** Muss. Jede Beobachtung muss den Belief State über ein nachvollziehbares Update verändern
(Zyklus: Belief → Beobachtung → Update → Aktion).

#### LH-FA-OBS-003 — Bayesianisches, nicht-überschreibendes Update

**Prio:** Muss. Belief-Updates müssen bayesianisch erfolgen (Posterior ∝ Prior × Likelihood) und
dürfen den bisherigen Belief nicht überschreiben/verwerfen.

#### LH-FA-OBS-004 — Deduplizierung korrelierter Beobachtungen

**Prio:** Muss. Das System muss korrelierte oder redundante Beobachtungen erkennen und darf sie
nicht als unabhängige Evidenz mehrfach zählen (Deduplizierung gegen Scheingewissheit).

#### LH-FA-OBS-005 — Likelihood unter der Resthypothese

**Prio:** Muss. Bei jeder Beobachtung muss auch die Resthypothese bewertet werden (Likelihood der
Evidenz unter „unbekannt").

#### LH-FA-OBS-006 — Zeitstempel und Quelle je Beobachtung

**Prio:** Soll. Jede Beobachtung soll mit Zeitstempel und Quelle erfasst werden.

### 6.3 Aktionen und Wirkungsklassen (LH-FA-ACT)

#### LH-FA-ACT-001 — Vier Wirkungsklassen

**Prio:** Muss. Das System muss jede Aktion genau einer von vier Wirkungsklassen zuordnen:
**nur-lesend**, **arbeitsbereich-lokal**, **repository-wirksam**, **extern-wirksam**.

#### LH-FA-ACT-002 — Einstufung nach Seiteneffekt-Reichweite

**Prio:** Muss. Die Einstufung muss auf der Reichweite der Seiteneffekte beruhen, nicht auf Kosten
oder „Größe". Ein Repository-Commit gilt als reversibler Checkpoint; extern-wirksame Aktionen
(Deploy, E-Mail, Zahlung, DB-Migration, externer API-Aufruf) bilden die kritische Klasse.

#### LH-FA-ACT-003 — Erfolgswahrscheinlichkeit je Aktion

**Prio:** Muss. Jede vorgeschlagene Aktion muss eine eigene Erfolgswahrscheinlichkeit P(Aktion
erreicht Ziel \| aktueller Belief) tragen — getrennt von der Wahrscheinlichkeit der zugrunde
liegenden Diagnose.

#### LH-FA-ACT-004 — Rückverfolgbarkeit Aktion → Evidenz

**Prio:** Muss. Jede vorgeschlagene Aktion muss die sie stützende Evidenz referenzieren
(Rückverfolgbarkeit Aktion → Evidenz).

### 6.4 Entscheidungspolitik und Konfidenz-Gates (LH-FA-POL)

#### LH-FA-POL-001 — Konfidenz-Gate vor jeder Aktion

**Prio:** Muss. Vor Ausführung jeder Aktion muss ein Konfidenz-Gate die Aktion **freigeben,
ablehnen oder eskalieren**.

#### LH-FA-POL-002 — Gate prüft Erfolgswahrscheinlichkeit

**Prio:** Muss. Das Gate muss gegen die **Erfolgswahrscheinlichkeit der Aktion** prüfen — nicht
gegen die Wahrscheinlichkeit der Diagnose- bzw. Top-Hypothese.

#### LH-FA-POL-003 — Schwellen je Wirkungsklasse

**Prio:** Muss. Die geforderte Mindest-Konfidenz muss von der Wirkungsklasse abhängen: nur-lesend
ohne wirksame Schwelle (das Gate wird gemäß LH-FA-POL-001 und LH-FA-POL-006 dennoch durchlaufen und
gibt ohne Konfidenzbedingung frei, wird also nicht übersprungen); arbeitsbereich-lokal niedrige
Schwelle; repository-wirksam mittlere Schwelle; extern-wirksam harte Schwelle.

#### LH-FA-POL-004 — Menschliche Freigabe für extern-wirksame Aktionen

**Prio:** Muss. Extern-wirksame Aktionen müssen zusätzlich zur Konfidenzschwelle eine explizite
menschliche Freigabe erfordern; für extern-wirksame (irreversible) Aktionen ist diese Freigabe
zwingend und darf nicht abschaltbar sein (fail-safe, konsistent zu LH-OUT-04). Konfigurierbar ist
ausschließlich ihre Form und ihr Kanal (siehe LH-OP-04), nicht ihr Entfall.

#### LH-FA-POL-005 — Sperre extern-wirksamer Aktionen bei hoher Resthypothese

**Prio:** Muss. Solange die Resthypothese über ihrem Schwellwert liegt, darf **keine**
extern-wirksame (irreversible) Aktion freigegeben werden — unabhängig davon, wie zugespitzt die
übrigen Hypothesen sind.

#### LH-FA-POL-006 — Nicht umgehbares Gate

**Prio:** Muss. Das Gate muss außerhalb der Aktion liegen und darf von der Aktion nicht umgangen
oder ausgelassen werden können (verpflichtende, nicht umgehbare Prüfung).

#### LH-FA-POL-007 — Konfigurierbare Schwellwerte

**Prio:** Soll. Alle Schwellwerte sollen konfigurierbar sein.

### 6.5 Value of Information (LH-FA-VOI)

#### LH-FA-VOI-001 — Information vor Handlung bei Unsicherheit

**Prio:** Muss. Bei hoher Unsicherheit und teurer oder irreversibler Zielaktion muss das System
zunächst Information sammeln, bevor es handelt.

#### LH-FA-VOI-002 — Diskriminierung der zwei wahrscheinlichsten Hypothesen

**Prio:** Muss. Bei der Wahl der nächsten Beobachtung muss das System diejenige günstige
Beobachtung bevorzugen, die die zwei wahrscheinlichsten Hypothesen am stärksten trennt (erwartete
Diskriminierung), statt beliebige zusätzliche Evidenz zu sammeln.

#### LH-FA-VOI-003 — Gewinn-Kosten-Abwägung

**Prio:** Soll. Der erwartete Informationsgewinn soll gegen die Kosten der Beobachtung abgewogen
werden (Gewinn je Kosten).

#### LH-FA-VOI-004 — Lokale/heuristische VoI-Bewertung

**Prio:** Muss. Die VoI-Bewertung darf lokal/heuristisch erfolgen; eine global optimale Policy ist
nicht gefordert.

### 6.6 Eskalation (LH-FA-ESK)

#### LH-FA-ESK-001 — Eskalationsbedingung

**Prio:** Muss. Wenn (a) die verfügbaren günstigen Beobachtungen erschöpft sind, (b) die
Resthypothese hoch bleibt und (c) das Aktions-Gate geschlossen bleibt, muss das System anhalten und
an einen Menschen eskalieren.

#### LH-FA-ESK-002 — Eskalation als definierter Zustand

**Prio:** Muss. Eine solche Eskalation muss als korrektes, erwartetes Verhalten behandelt werden —
nicht als Fehler oder Ausnahmezustand. *Abnahme:* Eskalation erzeugt keinen Fehlerstatus, sondern
einen definierten Eskalations-Zustand.

#### LH-FA-ESK-003 — Eskalations-Kontext

**Prio:** Muss. Eine Eskalation muss den aktuellen Belief State, die gesammelte Evidenz und den
Grund (welches Gate, welche Schwelle, Stand der Resthypothese) mitliefern.

#### LH-FA-ESK-004 — Budget gegen Endlosschleifen

**Prio:** Soll. Das System soll Endlosschleifen der Informationssammlung durch ein Budget
(Schritte/Kosten/Zeit) begrenzen und bei dessen Erschöpfung eskalieren.

### 6.7 Audit und Ereignisprotokoll (LH-FA-AUD)

#### LH-FA-AUD-001 — Unveränderliches Ereignisprotokoll

**Prio:** Muss. Sämtliche Belief-Änderungen und Entscheidungen müssen als unveränderliche,
geordnete Ereignisfolge protokolliert werden — mindestens: Hypothese hinzugefügt, Beobachtung
erfasst, Belief aktualisiert, Aktion vorgeschlagen, Gate abgelehnt, Eskalation angefordert.

#### LH-FA-AUD-002 — Rekonstruierbarkeit des Belief States

**Prio:** Muss. Aus dem Ereignisprotokoll muss der Belief State zu jedem vergangenen Zeitpunkt
rekonstruierbar sein.

#### LH-FA-AUD-003 — Auditierbare Entscheidungsspur

**Prio:** Muss. Die Entscheidungsspur muss ein auditierbares Protokoll sein und darf nicht in
verstecktem, nicht prüfbarem modellinternen Reasoning verborgen bleiben.

#### LH-FA-AUD-004 — Zeitstempel und Quelle je Ereignis

**Prio:** Soll. Jedes Ereignis soll Zeitstempel sowie auslösende Quelle/Aktion enthalten.

### 6.8 Rolle des Sprachmodells (LH-FA-LLM)

#### LH-FA-LLM-001 — Sprachmodell als austauschbares Modul

**Prio:** Muss. Das Sprachmodell muss als austauschbares Modul eingebunden sein und darf nicht der
Agent selbst sein; Entscheidungs- und Kontrolllogik müssen außerhalb des Modells liegen.

#### LH-FA-LLM-002 — Abgegrenzte Modell-Aufgaben

**Prio:** Muss. Das System muss das Modell nur für klar abgegrenzte Aufgaben einsetzen: Hypothesen
erzeugen/verfeinern, Likelihoods schätzen, Aktionen vorschlagen.

#### LH-FA-LLM-003 — Externalisierung der Modell-Konfidenz

**Prio:** Muss. Die vom Modell gelieferte (implizite) Konfidenz muss in explizite, protokollierte
und überschreibbare Zahlen überführt werden, die den Gates unterliegen. Ein Überschreiben erfolgt
als neues, protokolliertes Ereignis (vgl. LH-FA-AUD-001) und nicht als Mutation eines bestehenden
Eintrags.

#### LH-FA-LLM-004 — Anbieter-Austauschbarkeit

**Prio:** Soll. Das System soll ohne Bindung an einen bestimmten Modellanbieter betreibbar sein
(Anbieter-Austauschbarkeit).

---

## 7. Nichtfunktionale Anforderungen (LH-QA)

### LH-QA-01 — Nachvollziehbarkeit

**Prio:** Muss. Jede ausgeführte Aktion muss auf Belief State und Evidenz zurückführbar sein.

### LH-QA-02 — Konservatives Standardverhalten (fail-safe)

**Prio:** Muss. Im Zweifel handelt das System nicht, sondern sammelt Information oder eskaliert.

### LH-QA-03 — Testbarkeit

**Prio:** Muss. Belief-Updates, Gates und VoI-Auswahl müssen bei gegebenen Likelihoods
deterministisch testbar sein.

### LH-QA-04 — Erweiterbarkeit

**Prio:** Soll. Neue Beobachtungsquellen und Aktionstypen sollen ergänzbar sein, ohne den Kern zu
ändern.

### LH-QA-05 — Konfigurierbarkeit

**Prio:** Soll. Schwellwerte, Budgets und die Zuordnung der Wirkungsklassen sollen konfigurierbar
sein.

### LH-QA-06 — Beobachtbarkeit

**Prio:** Soll. Das Ereignisprotokoll soll exportier- und inspizierbar sein.

### LH-QA-07 — Performance

**Prio:** Kann. Belief-Update und Gate-Entscheidung sollten interaktive Latenzen nicht spürbar
verschlechtern.

## 8. Randbedingungen und Annahmen (LH-RB)

### LH-RB-01 — Sprachmodell als Port verfügbar

**Prio:** Muss. Ein Sprachmodell steht als anbindbares Modul (Port) zur Verfügung.

### LH-RB-02 — Versionskontrolle vorausgesetzt

**Prio:** Muss. Das betreute Repository steht unter Versionskontrolle (begründet die Einstufung von
Commit als Checkpoint und die Wirkungsklassen).

### LH-RB-03 — Umgang mit verrauschten Beobachtungen

**Prio:** Soll. Beobachtungen können verrauscht, unvollständig oder korreliert sein; das System soll
damit umgehen.

### LH-RB-04 — Zielplattform JVM

**Prio:** Kann. Zielplattform ist die JVM. Die konkrete Sprach- und Framework-Wahl ist als „Wie"
nicht Gegenstand dieses Lastenhefts und wird in `spec/spezifikation.md` festgelegt.

## 9. Nicht-Ziele / Abgrenzung (LH-OUT)

### LH-OUT-01 — Keine global optimale POMDP-Policy

**Prio:** Muss. Es wird **keine** global optimale POMDP-Policy berechnet; lokale, heuristische
Entscheidungen sind ausreichend.

### LH-OUT-02 — Kein Kalmanfilter

**Prio:** Muss. Es wird **kein** Kalmanfilter bzw. keine kontinuierliche linear-gaußsche
Zustandsschätzung eingesetzt; an deren Stelle tritt der diskrete Hypothesen-Belief.

### LH-OUT-03 — Kein handspezifiziertes Weltmodell

**Prio:** Muss. Es wird **kein** vollständiges, von Hand spezifiziertes Domänen-Weltmodell
gefordert; das implizite Weltmodell liefert das Sprachmodell.

### LH-OUT-04 — Keine irreversible Aktion ohne Schwelle und Freigabe

**Prio:** Muss. Das System führt **keine** extern-wirksame Aktion ohne Erfüllung der harten
Schwelle und der vorgesehenen menschlichen Freigabe aus.

---

## 10. Offene Punkte

| Kennung | Beschreibung |
|---------|--------------|
| LH-OP-01 | Konkrete Schwellwerte je Wirkungsklasse (Startwerte) festzulegen. |
| LH-OP-02 | Schwellwert(e) der Resthypothese, ab dem Re-Hypothesenbildung (LH-FA-BEL-005), Sperre extern-wirksamer Aktionen (LH-FA-POL-005) bzw. Eskalation (LH-FA-ESK-001) ausgelöst wird; offen, ob ein gemeinsamer oder je Zweck eigener Schwellwert gilt. |
| LH-OP-03 | Kriterien für die Deduplizierung korrelierter Beobachtungen (LH-FA-OBS-004). |
| LH-OP-04 | Form und Kanal der menschlichen Freigabe/Eskalation (LH-FA-POL-004, LH-FA-ESK-003). |
| LH-OP-05 | Toleranz der Normierung (Summe = 1) für LH-FA-BEL-002 und LH-FA-BEL-004 festzulegen. |

## 11. Historie

| Version | Datum | Änderung |
|---------|-------|----------|
| 0.1 | 2026-06-22 | Initiale Fassung (Anforderungen als Tabellen). |
| 0.2 | 2026-06-22 | Format-Migration: Anforderungen als Überschriften `### <ID> — <Titel>` (RTM-/Anker-fähig); Inhalt, IDs und Priorität unverändert. Pflichtenheft-Verweise auf `spec/spezifikation.md` konkretisiert. |
| 0.3 | 2026-06-22 | ID-Schema auf kanonisch `LH-FA-<BEREICH>-<NNN>` / `LH-QA-<NN>` ausgerichtet (RTM-/`ids`-/`suggest-config`-kompatibel); Constraint-/Non-Goal-/Open-Point-Familien `LH-RB`/`LH-OUT`/`LH-OP` beibehalten. Inhalt und Priorität unverändert. |
| 0.4 | 2026-06-23 | Review-Konsistenz (keine inhaltliche Neuanforderung): Freigabe für extern-wirksame Aktionen als nicht abschaltbar präzisiert (LH-FA-POL-004, fail-safe konform zu LH-OUT-04 — Verschärfung, kein Gate-Lockern); Modalverben an `Prio: Soll` angeglichen (LH-FA-POL-007, LH-QA-04, LH-QA-05, LH-QA-06, LH-RB-03); Gate-Durchlauf bei nur-lesend klargestellt (LH-FA-POL-003); konkrete Sprach-/Framework-Nennung aus LH-RB-04 entfernt (Was/Wie-Trennung); Resthypothesen-Schwelle um LH-FA-POL-005 ergänzt (LH-OP-02) und Normierungs-Toleranz als LH-OP-05 aufgenommen; Glossar um „günstige Beobachtung" erweitert; Abnahme von LH-FA-BEL-004 und Audit-Hinweis in LH-FA-LLM-003 ergänzt. |
