# Gates. Doc-Gate generisch (d-check.mk, Target doc-check); Code-Gates
# (lint/test/build) sind repo-spezifisch und wachsen mit dem Code. Nur
# existierende, laufende Targets in AGENTS.md / harness/README.md eintragen
# (keine halluzinierten Gates, Modul 13).

# Digest-Pin fuer strikte Reproduzierbarkeit (MR-004): d-check v0.37.1.
# Sticht den Tag aus d-check.mk (dort tool-generiert via --print-mk). Bei
# d-check-Upgrade neuen Digest setzen.
DCHECK_DIGEST := sha256:3bbdb19bb73200fa37e30eff961cd5429a44e9e945fff3fb65ba7dc4b3cd88dd

include d-check.mk

# a-check (Architektur-Gate, MR-005): v0.11.0 — fixed-root loest interne FQNs
# datei-mengen-bewusst gegen die realen Dateien unter roots auf und setzt damit
# Multi-Modul-KMP echt durch (slice-008; loest die v0.10.0-Guard-Reject-Klasse).
# a-check.mk ist tool-generiert (--print-mk); dessen Default-Digest laggt, daher
# hier der verifizierte v0.11.0-Digest-Pin. Bei a-check-Upgrade neuen Digest setzen.
A_CHECK_IMAGE := ghcr.io/pt9912/a-check@sha256:f53a06fe02973a0cd4771eccb3d22171619a5ddfcbca2fac9922ce3fe7520483

include a-check.mk

.PHONY: help gates
help: ## Targets anzeigen
	@grep -hE '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  %-14s %s\n", $$1, $$2}'

gates: doc-check build test coverage-gate arch-check ## alle aktuell lauffähigen Gates

.PHONY: arch-check
arch-check: a-check ## Architektur: Kern importiert kein Adapter/Framework (a-check, ADR-0001/0003)

# --- Code-Gates: KMP/HexSlice via multi-stage Dockerfile (Modul 14). ---
# Einstiegspunkt bleibt make; die Toolchain lebt im Dockerfile (kein Host-JDK,
# AGENTS.md §3.1). arch-check folgt, sobald a-check verdrahtet ist.
IMAGE ?= belief-agent

.PHONY: build
build: ## Reproduzierbarer Build aller Module (Dockerfile-Stage build)
	docker build --target build --iidfile harness/image-hash.txt -t $(IMAGE):build .
	@printf 'build-image: '; cat harness/image-hash.txt; echo

.PHONY: test
test: ## Deterministische Tests (LH-QA-03, Dockerfile-Stage test)
	docker build --target test -t $(IMAGE):test .

.PHONY: coverage
coverage: ## Test-Coverage messen (Kover; Report)
	@docker build --progress=plain --target coverage -t $(IMAGE):coverage . 2>&1 | grep -iE 'line coverage' || true

.PHONY: coverage-gate
coverage-gate: ## Coverage-Schwelle pruefen (Kover koverVerify; Schwelle ADR-0004)
	docker build --target coverage-gate -t $(IMAGE):coverage-gate .
