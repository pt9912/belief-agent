# Gates. Doc-Gate generisch (d-check.mk, Target doc-check); Code-Gates
# (lint/test/build) sind repo-spezifisch und wachsen mit dem Code. Nur
# existierende, laufende Targets in AGENTS.md / harness/README.md eintragen
# (keine halluzinierten Gates, Modul 13).

# Digest-Pin fuer strikte Reproduzierbarkeit (MR-004): d-check v0.37.1.
# Sticht den Tag aus d-check.mk (dort tool-generiert via --print-mk). Bei
# d-check-Upgrade neuen Digest setzen.
DCHECK_DIGEST := sha256:3bbdb19bb73200fa37e30eff961cd5429a44e9e945fff3fb65ba7dc4b3cd88dd

include d-check.mk

.PHONY: help gates
help: ## Targets anzeigen
	@grep -hE '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  %-14s %s\n", $$1, $$2}'

gates: doc-check ## alle aktuell lauffähigen Gates (Code-Gates ergänzen)
