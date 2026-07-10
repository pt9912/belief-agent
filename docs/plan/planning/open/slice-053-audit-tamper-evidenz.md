# Slice slice-053: Audit-Tamper-Evidenz (Hash-Chain/Signatur)

**Status:** open (siehe [Planning-README](../README.md)).

**Welle:** welle-05-llm-port Stabilisierung (Audit-Persistenz-Folgeslices).

**Bezug:** [`LH-FA-AUD-001`](../../../../spec/lastenheft.md#lh-fa-aud-001--unveränderliches-ereignisprotokoll), [`LH-FA-AUD-003`](../../../../spec/lastenheft.md#lh-fa-aud-003--auditierbare-entscheidungsspur), [`LH-QA-03`](../../../../spec/lastenheft.md#lh-qa-03--testbarkeit), [`LH-QA-06`](../../../../spec/lastenheft.md#lh-qa-06--beobachtbarkeit);
[`ADR-0002`](../../adr/0002-implementierungssprache-jvm-java.md), [`ADR-0003`](../../adr/0003-hexslice-architektur.md); `ARC-06`.

**Autor:** Claude. **Datum:** 2026-07-09.

---

## 1. Ziel

Der persistente Audit-Store erhält **Tamper-Evidenz** (Hash-Chain je Record über
den Vorgänger oder Signatur), sodass ein **ordnungserhaltendes** Out-of-Band-
Umschreiben (inneren Record löschen/ändern, Rest gültig geordnet neu schreiben)
erkennbar wird. Damit gilt die [`LH-FA-AUD-001`](../../../../spec/lastenheft.md#lh-fa-aud-001--unveränderliches-ereignisprotokoll)-Unveränderlichkeit auch gegen
einen Dateisystem-Akteur, nicht nur gegen die Adapter-API.

## 2. Definition of Done

- [ ] Der Store trägt eine verkettete Integritätssicherung (Hash-Chain über den
  Vorgänger-Record oder Signatur); `lade()` erkennt und meldet eine gebrochene
  Kette (Out-of-Band-Manipulation) sichtbar ([`LH-FA-AUD-001`](../../../../spec/lastenheft.md#lh-fa-aud-001--unveränderliches-ereignisprotokoll)/`003`).
- [ ] Inspizierbarkeit ([`LH-QA-06`](../../../../spec/lastenheft.md#lh-qa-06--beobachtbarkeit)) bleibt gewahrt oder wird bewusst gegen die
  Tamper-Evidenz abgewogen — Format-Entscheidung, ggf. Folge-ADR ([`ADR-0002`](../../adr/0002-implementierungssprache-jvm-java.md)).
- [ ] Deterministische Tests ([`LH-QA-03`](../../../../spec/lastenheft.md#lh-qa-03--testbarkeit)): intakte Kette lädt; manipulierter
  innerer Record wird erkannt; `make gates` grün.

## 3. Plan (vor Code)

| Datei / Komponente | Aenderungs-Art | Begruendung |
|---|---|---|
| `adapters/outbound/audit-file/**` | update | Record-Format um Hash-Chain/Signatur erweitern; Kettenprüfung beim Laden. |
| `docs/plan/adr/*` | ggf. neu | Falls Format-/Krypto-Abhängigkeit die Toolchain berührt ([`ADR-0002`](../../adr/0002-implementierungssprache-jvm-java.md)). |
| `.../src/test/**` | neu | Ketten-intakt/-gebrochen-Matrix. |

## 4. Trigger

Aus [`slice-041`](../done/slice-041-dauerhafte-audit-datenbank.md) §9 (IDR-3):
**Architect-Entscheidung** — fällig, wenn das Threat-Model dieses Safety/Control-
Stores Widerstand gegen Dateisystem-Zugriff verlangt (typisch zusammen mit
Compliance-Export/Retention/Pfadpolitik). Setzt den persistenten `audit-file`-
Adapter aus `slice-041` voraus.

## 5. Closure-Trigger

DoD vollständig + Review/Verification abgeschlossen + `make gates` grün +
Closure-Notiz geschrieben + Slice nach `done/` verschoben.

## 6. Risiken und offene Punkte

- Tamper-Evidenz steht in Spannung zur Klartext-Inspizierbarkeit ([`LH-QA-06`](../../../../spec/lastenheft.md#lh-qa-06--beobachtbarkeit)):
  ein signiertes/verkettetes Format ist weniger trivial per Editor lesbar — die
  Abwägung gehört in den Slice (ggf. Folge-ADR).
- Schlüssel-/Signaturverwaltung kann eine neue Abhängigkeit/Pfadpolitik einführen
  ([`ADR-0002`](../../adr/0002-implementierungssprache-jvm-java.md)-Guard) und ist selbst Folgearbeit, falls sie über lokale
  Hash-Ketten hinausgeht.

## 7. Closure-Notiz (nach `done/`)

<!-- Erst nach Abschluss fuellen. -->

## 8. Sub-Area-Modus-Begründung

### Sub-Area: Audit-Store-Integrität (`adapters/outbound/audit-file`)

- **Modus:** Hybrid
- **Konventionen-Dichte:** niedrig-mittel. Append-only-Regel steht; Tamper-Evidenz
  ist neu und im Repo nicht vorverankert.
- **Phase-Reife:** Phase 2-3. Setzt den `slice-041`-Store voraus.
- **Evidenz-/Diskrepanz-Risiko:** hoch, aber threat-model-abhängig — ohne
  Dateisystem-Angreifer im Modell ist es INFO, mit ist es Muss.
- **Reconciliation-Aufwand:** ein Slice; ggf. Folge-ADR für Format/Krypto.
