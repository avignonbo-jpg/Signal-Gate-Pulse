## Phase 0 — Security Control-Plane Integrity Gate

> **Objective:** Prove that security state cannot diverge between authoritative persistence, derived indexes, external source data, and edge behavior.

Phase 0 — Security Control-Plane Integrity Gate Objective: prove that security state cannot diverge between authoritative persistence, derived indexes, external source data, and edge behavior.

### 0.8 — Add regression tests for all Phase 0 invariants

Add regression tests for all Phase 0 invariants — **✅ COMPLETE**. Phase 0 only exits once tests exist and pass in mandatory CI — not when the code merely compiles and appears safe.

Required scope includes the startup-ordering regression guard `StartupTimingTest.kt`, which must be included in the mandatory instrumented CI gate alongside the other Phase 0 invariants. The live workflow runs `:app:connectedPulseDebugAndroidTest`, which executes all `androidTest` classes under the app module, including `StartupTimingTest.kt`.

- [x] One approved rule mutation boundary exists for manual and source-snapshot mutations; mutation-boundary behavior passed in mandatory CI
- [x] Direct feature-level DAO mutation from Layer 1/6 is removed for the audited edge paths; source sync status writes are now confined to the accepted-snapshot boundary
- [x] Database is documented and tested as authoritative (BloomAuthoritativeDecisionTest passed in mandatory CI)
- [x] Bloom state is derived/rebuildable (BloomAuthoritativeDecisionTest passed in mandatory CI)
- [x] Source activation is transactional and migration-tested (MigrationTest and SourceActivationTransactionTest passed in mandatory CI)
- [x] Last-known-good preservation is covered and passed in mandatory CI (SourceActivationTransactionTest)
- [x] Parser partial-result paths reject hard-limit violations and CSV/XLSX regression tests passed in mandatory CI
- [x] Security failure is explicit and the regression test passed in mandatory CI
- [x] Startup ordering is regression-tested in the mandatory instrumented gate (`StartupTimingTest.kt` runs under `connectedPulseDebugAndroidTest`)
- [x] Mandatory JVM and instrumented CI workflows executed the complete expanded Phase 0 behavioral matrix successfully
