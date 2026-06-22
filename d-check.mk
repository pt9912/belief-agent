# d-check.mk — erzeugt von: d-check --print-mk (DC-FA-CLI-010).
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
doc-check:
	docker run --rm --network none -v "$(CURDIR):/repo:ro" $(DCHECK_IMAGE)
