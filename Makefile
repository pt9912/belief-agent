# Gates. Doc-Gate generisch (d-check.mk, Target doc-check); Code-Gates
# (lint/test/build) sind repo-spezifisch und wachsen mit dem Code. Nur
# existierende, laufende Targets in AGENTS.md / harness/README.md eintragen
# (keine halluzinierten Gates, Modul 13).
include d-check.mk

.PHONY: help gates
help: ## Targets anzeigen
	@grep -hE '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  %-14s %s\n", $$1, $$2}'

gates: doc-check ## alle aktuell lauffähigen Gates (Code-Gates ergänzen)
