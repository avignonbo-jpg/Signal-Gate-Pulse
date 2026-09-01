# Manus Handoff — Pulse Pre-Release Security Assurance

**Date:** 2026-08-31  
**Repository:** `avignonbo-jpg/Signal-Gate-Pulse`  
**Branch:** `consumer-v1`  
**Remote head when updated:** `9631f68` — `Record disabled source verification evidence`

## Purpose

This document is a durable handoff for the next Manus AI entity continuing Pulse pre-release security-assurance work. It summarizes the approved direction, completed changes, CI evidence, remaining gates, and execution discipline. Governing documents remain authoritative.

## Governing documents

Before further changes, read `PROJECT_LEDGER.md`, `Architecture-Contract.md`, `SECURITY-DEVOPS-BUILD-PLAN.md`, `SignalGate-Pulse-Release-Roadmap.md`, `SIGNALGATE-PULSE-NEXT-ARCHITECTURAL-BUILD-PLAN.md`, `SignalGate-Pulse-Manus-CI-Guardrails.md`, `Source-Of-Truth.md`, and the owner-approved `/home/ubuntu/plan.md` when available. If they disagree, inspect live source and CI, follow the architecture contract, and append the reconciliation to the ledger. Use one issue/one change-set discipline and commit the ledger as work progresses.

## Owner-approved direction

Keep genuinely new detection intelligence out of v1.0, but make reliability of Pulse’s existing signature feature a pre-release blocker. The sequence is: reconcile baseline; close Phase 4.0 edge/control-plane work; satisfy the named Pre-Release Screening Assurance Gate; then continue the existing v1.0 roadmap. Defer cloud reputation, crowdsourcing, behavioral scoring, expanded STIR/SHAKEN, new screening modes, and multi-device sync to separately governed v1.1+ scope.

The screening path must remain safe when the process is cold, persistence is slow or fails, sources are stale/invalid/disabled, Bloom is rebuilding, Telecom input is malformed, Android differs from Robolectric, and R8/release packaging is applied.

## Completed and verified work

### Response-path testability

Commit `cfd713d` added a pure `TelecomResponsePolicy`, preserved production `CallResponse.Builder` behavior and explicit `SECURITY_FAILURE` ring-through policy, and added injectable response factories for JVM tests. The four original Robolectric failures were caused by an unimplemented `setDisallowCall(false)` method, not by production policy.

### CI artifact workflow

Commit `ba571df` made lint run under `if: always()`, narrowed test/lint artifact paths, and used `if-no-files-found: warn`. The empty `pulse-cold.yml` was deleted upstream in `be63c3e`; `.yml` was not the issue—the empty workflow trigger block was.

### Snapshot and Bloom integrity

Commit `adf480a` added combined instrumented coverage proving that a failed replacement preserves the prior authoritative `BLOCK` decision and does not allow a candidate-only number to influence the Bloom-backed decision path. Instrumented run `33451970789`, job `99683671131`, passed successfully.

### Bounded pattern matching

Commit `5a871c2` replaced load-all-patterns-then-scan behavior with `UnifiedEntryDao.findMatchingBlockPatternsWithPriority(normalized)`, filtering `:normalized LIKE ue.phoneNumber || '%'` in SQLite while preserving enabled-source and priority ordering. Bloom remains only a read-skip optimization; SQLite remains authoritative. Consumer CI run `33452532389`, job `99685396814`, passed all 84 JVM tests, lint, architecture checks, and artifact uploads.

### XLSX expansion budgets

Commit `0544431` added hard parser limits for expanded shared-string UTF-8 bytes (64 MiB default) and per-cell bytes (64 KiB default), with typed SAX failures and regression tests. Consumer CI run `33453014941`, job `99686932429`, passed all 84 JVM tests, lint, architecture checks, and artifact uploads.

### CSV bounded batches

Commit `8feda19` preserved synchronous `SecureCsvParser.streamRows()` compatibility, added `streamRowsSuspend()`, and added `DataSyncEngine.streamCsvFile(..., onBatch)` with configurable bounded batches. The compatibility `parseCsvFile()` remains a collector; the new API emits and clears batches incrementally. Regression coverage verifies `[2, 1]` delivery for three records with batch size two. Consumer CI run `33453582541`, job `99688657880`, passed all 84 JVM tests, lint, architecture checks, and artifact uploads.

### Disabled-source automatic sync

Commit `9889080` changed `ReliableSourceManager.syncAllFederalSources()` to inspect persisted source rows and skip sources explicitly disabled. Missing rows remain eligible for first-run seeding; explicit `syncSource(sourceId)` remains a deliberate manual refresh path. Consumer CI run `33453969699` completed successfully.

The uploaded `test-results(5).zip` independently contained 84 tests with zero failures/errors and five passing `DataSyncEngineXlsxLimitTest` cases. The latest ledger evidence was committed and pushed in `9631f68`.

## Current open work

1. **XLSX authoritative snapshot activation:** The suspend-aware XLSX batch transport and its bounded-batch regression are already present and must not be reimplemented. The remaining work is to add the `DataSyncEngine` wrapper that feeds XLSX batches through `SecurityRuleRepository.replaceSourceSnapshotBatched()` and to add equivalent all-or-nothing activation coverage.
2. **Bloom pre-commit staleness window:** `DataSourceRepository.insertEntriesAuthoritative()` must invalidate `bloomReady` before any Room work, and the post-write/pre-rebuild behavior must be regression-tested. This is the current highest-priority production security fix.
3. **Phase 4.0 real-device evidence:** Cold start, process death, concurrency, and release/minified Telecom behavior remain owner-only release blockers requiring a representative physical device. Emulator or Robolectric results must not be reported as equivalent evidence.
4. **Privacy and operational-surface evidence:** Review logs, notifications, crash artifacts, sync errors, and debug/audit surfaces for unnecessary call metadata or raw phone numbers, then record the owner’s real-device findings.

## Security-first priority order

The next work should follow dependency and blast-radius order rather than feature count:

1. **Close Telecom failure choreography first.** Add service-entry coverage for unexpected engine/response-path exceptions and prove exactly one explicit response, including malformed handles, timeout, persistence failure, and UX failure. This is the highest-risk ingress behavior because a missing or duplicate Telecom response affects the live call outcome.
2. **Wire repository-backed whole-candidate activation.** Connect the bounded CSV batch API to the authoritative snapshot boundary so batches are an implementation detail, not independently activated partial snapshots. Prove failure discards the entire candidate and post-commit Bloom rebuild occurs once.
3. **Add equivalent XLSX batch transport.** Preserve two-pass shared-string resolution, hard row/byte/string limits, and full-candidate discard semantics while eliminating the list-returning dataset materialization.
4. **Root-cause FCC sync behavior.** Instrument `Never/0/Unknown`, verify endpoint/fallback liveness, and document whether the source is fixed or intentionally unavailable before changing product behavior.
5. **Complete privacy and device evidence.** Review operational surfaces, then validate Telecom cold-start and minified/release behavior on a representative physical device.
6. **Only after those blockers, resume lower-risk product cleanup and release hardening.** Keep new detection intelligence and new screening actions outside this gate.

Do not reorder item 1 behind parser or UI work: parser safety protects ingestion, but exactly-one-response and failure choreography protect the phone’s signature behavior at the point of consequence.

## Current execution-plan addendum (2026-09-01)

`CLAUDE-DEVSECOPS-BUILD-PLAN.md` is the current task-level execution plan for this continuation. Its numbered task order, per-task `allowed_files`, `forbidden_actions`, acceptance criteria, and human gates govern the listed work; the architecture contract and append-only ledger remain authoritative where the plan is silent.

## Immediate continuation procedure

First verify `git fetch origin consumer-v1`, the clean working tree, and the remote head. Read the governing files and append a ledger note before or with the next scoped change. Inspect the allowed file scope in the CI guardrails. Make one issue-specific change, run `git diff --check`, inspect the full diff, commit, push, and record the exact CI run and artifact evidence. Do not mark a gate complete from compilation alone. If the Android SDK is unavailable locally, use mandatory GitHub Actions as the Android build/test/lint authority.

## Non-negotiable constraints

- SQLCipher/database state is authoritative; Bloom is derived and disposable.
- No partial or unverified snapshot may become active.
- `SECURITY_FAILURE` remains distinct from `ALLOW` and `CLEAN_UNKNOWN`.
- No alternate DAO mutation paths or weakened required CI gates.
- Required persistence precedes optional UX; throttling cannot suppress audit/review state.
- No raw phone numbers or unnecessary call metadata in operational logs, notifications, or diagnostics.
- Do not use `unitTests.returnDefaultValues = true` to hide framework behavior.
- Do not delete inherited functionality based only on grep/reference absence.
- Do not introduce a new third-party dependency for a single issue without review.
- Do not silently expand an issue’s allowed file scope.

## Handoff completion criterion

The next Manus entity should verify this handoff against the live remote branch, read the governing files, append its own ledger entry, and continue from the current remote state. This handoff is a navigation aid; live source, CI evidence, the architecture contract, and the append-only ledger remain authoritative.
