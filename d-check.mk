# d-check.mk — erzeugt von: d-check --print-mk (DC-FA-CLI-010), danach
# repo-lokal um das `doc-trace`-Target (RTM) erweitert.
#
# Einbinden: "include d-check.mk" im eigenen Makefile; eine eigene
# .d-check.yml danebenlegen. Keine Recipe-/Skript-Kopie — der Image-Pin
# lebt in d-check.
#
# DCHECK_IMAGE ist überschreibbar. Für strikte Reproduzierbarkeit den
# Digest aus den Release-Notes pinnen:
#   DCHECK_IMAGE = ghcr.io/pt9912/d-check@sha256:<digest>
# Digest-Pin für strikte Reproduzierbarkeit (MR-004 in harness/conventions.md):
# d-check v0.23.0. Bei Upgrade neuen Digest aus den Release-Notes setzen.
DCHECK_IMAGE ?= ghcr.io/pt9912/d-check@sha256:68951f5a3dd7ad3404e1996d45327f3df2585c0ef2b0b6bde7ccf790da4ddf6a

.PHONY: doc-check
doc-check: ## Doku-Referenzen pruefen (Module laut .d-check.yml)
	docker run --rm --network none -v "$(CURDIR):/repo:ro" $(DCHECK_IMAGE)

# Repo-lokale Erweiterung (nicht aus --print-mk): Requirements Traceability
# Matrix. Read-only Report, kein Gate -> NICHT in `gates`. Format anhaengbar,
# z. B.: make doc-trace TRACE_FLAGS=--json
TRACE_FLAGS ?=
.PHONY: doc-trace
doc-trace: ## RTM: Anforderung -> ADRs/Slices + Waisen (d-check --trace)
	docker run --rm --network none -v "$(CURDIR):/repo:ro" $(DCHECK_IMAGE) --trace $(TRACE_FLAGS)
