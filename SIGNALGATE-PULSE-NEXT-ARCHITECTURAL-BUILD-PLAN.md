# SignalGate Pulse — Next Architectural Build Plan

**Status:** Draft — proposed next build authority  
**Branch:** `consumer-v1`  
**Product:** SignalGate Pulse

## Purpose

Establish the next controlled engineering build from the current branch reality, the adopted architecture/security work, the accumulated findings, and the refined Pulse product direction.

---

# 0. Governing Authority and Build Philosophy

## 0.1 Authority

The next build is governed by the current Pulse Source of Truth and the adopted Architecture Contract.

Where implementation reality, historical plans, or inherited Multi-Port behavior conflict with the current Source of Truth, the conflict must be resolved explicitly before destructive implementation work begins.

**Placeholder — insert final Source-of-Truth precedence/order when the new document is complete.**

## 0.2 Product direction

Pulse is no longer defined as purely:

> Set-And-Forget.

The intended product philosophy is:

> **Set-And-Forget-Until-You-Need-It.**

The ordinary experience remains simple and low-friction, while advanced controls, diagnostics, source management, and other intervention capabilities may live behind an intentionally low-visibility Advanced / Until-You-Need surface.

This does not mean every inherited Multi-Port capability belongs in Pulse.

## 0.3 Security-first rule

No optimization, UI improvement, cleanup, or migration may weaken:

- authoritative database semantics;
- screening decision integrity;
- explicit security-failure behavior;
- source activation safety;
- last-known-good preservation;
- Android screening response guarantees;
- protected-source semantics;
- privacy guarantees;
- release/CI security controls.

## 0.4 Automated-agent rule

Manus is an implementation agent, not the authority defining scope.

Each executable step must have an explicit scope manifest and must be independently enforced by CI.

The repository must mechanically reject unauthorized:

- file modifications;
- file creation;
- deletion;
- rename/copy changes;
- architectural mutation paths;
- dependency changes;
- schema changes;
- workflow changes;
- test weakening.

The separate **Manus CI Guardrails** specification defines this execution-control layer.

**Placeholder — final CI guardrail workflow path/status.**

---

# Phase 0 — Baseline Reconciliation and Protected-Scope Gate

**Objective:** Establish the exact current Pulse state before new implementation begins, while preventing accidental deletion of intentionally retained Multi-Port functionality.

This is a planning/control gate, not a feature-development phase.

## 0.1 Reconcile the new Source of Truth

Inputs:

1. new Pulse Source of Truth;
2. current repository;
3. current Security/DevOps build plan;
4. accumulated issue list;
5. current Architecture Contract;
6. protected-functionality inventory;
7. Manus CI guardrails.

Produce a reconciled implementation map.

Every known issue/capability receives:

```text
KEEP
REFACTOR
SECURE
OPTIMIZE
MIGRATE
MOVE
REPLACE
PURGE
DEFER
UNKNOWN
```

**UNKNOWN means DO NOT DELETE.**

### Exit criterion

No destructive cleanup item proceeds with an unresolved ownership or purpose question.

## 0.2 Multi-Port → Pulse capability inventory

Inventory the inherited swipe-right drawer and every function reachable through it.

The drawer is protected as an intentional Pulse Advanced / Until-You-Need surface until each capability is classified.

Each capability receives:

```text
KEEP
MIGRATE
MOVE
REPLACE
PURGE
DEFER
UNKNOWN
```

Do not infer:

```text
Multi-Port origin = purge
```

The swipe gesture itself is not a security boundary. Sensitive operations must enforce their own underlying authorization and validation.

**Placeholder — protected drawer/capability inventory path.**

### Exit criterion

The complete drawer/sub-function inventory exists and every item has an explicit disposition or remains protected as UNKNOWN.

## 0.3 Protected artifact inventory

Maintain a machine-readable inventory for artifacts that automated cleanup must not remove by inference.

Protection applies to:

- intentionally retained legacy files;
- advanced-menu functionality;
- security-critical components;
- architecture contracts;
- CI guardrails;
- other explicitly protected paths.

Protection levels:

```text
IMMUTABLE
PROTECTED
OBSERVED
NORMAL
```

### Exit criterion

CI can determine whether a proposed deletion violates the protected inventory.

## 0.4 Establish the implementation baseline

Record the current engineering baseline independently of the owner-managed ledger:

- commit;
- build configuration;
- dependency state;
- schema version;
- required CI workflows;
- existing test suites;
- architecture checks;
- known broken workflows;
- release-build state;
- unresolved items.

**Placeholder — baseline commit and artifact references.**

### Phase 0 gate

Do not begin destructive cleanup or broad implementation until:

- Source of Truth reconciliation is complete;
- Multi-Port/Pulse capability inventory is complete;
- protected inventory exists;
- Manus scope enforcement is available;
- baseline is recorded.

---

# Phase 1 — Android Edge Execution Integrity

**Objective:** Close remaining Android ingress boundary defects without changing established security architecture.

## 1.1 CallScreeningService response guarantee

Harden `SignalGateCallScreeningService.onScreenCall()` so every invocation produces an explicit response.

Required properties:

- null/malformed handle cannot silently return;
- decision execution remains deterministic;
- response cannot depend on notification/haptic/persistence completion;
- service exceptions cannot become silent non-responses;
- response remains within the platform deadline.

Required conceptual order:

```text
extract + normalize
      ↓
bounded decision
      ↓
respondToCall()
      ↓
post-response consequences
```

Post-response work may include:

- audit;
- pending-card persistence;
- notification;
- haptic.

### Required verification

- null-handle test;
- invalid-handle test;
- slow-persistence test;
- persistence-exception test;
- service-exception test;
- explicit security-failure test;
- exactly-one-response behavior.

### Exit criterion

The Android service boundary demonstrates explicit response behavior under the defined failure and latency conditions.

## 1.2 Screening timing budget

The platform deadline is a ceiling, not the desired operating target.

Establish an internal Pulse response budget materially below the platform ceiling and verify the decision path against it.

Measure representative latency distributions rather than relying solely on a single passing example.

**Placeholder — final internal timing budget after measurement.**

### Exit criterion

The service has a documented, testable internal timing budget and mandatory tests enforce it.

## 1.3 Preserve deliberate process-start initialization semantics

Do not move `MainApplication` database initialization into asynchronous application startup merely to reduce perceived startup time.

The current architecture deliberately initializes required database state synchronously because Android may cold-start the process directly for `CallScreeningService` before an Activity exists.

The build plan must preserve that safety guarantee unless the Source of Truth explicitly changes it.

Startup-performance work must therefore target the actual measured bottleneck rather than removing the initialization guarantee.

### Current startup investigation status

**Placeholder — owner/ledger status.**

Known current direction:

> **Root cause identified / candidate fix identified / verification pending.**

Potential build-level candidate identified from the current investigation:

```kotlin
android {
    packagingOptions {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}
```

This is a **candidate packaging optimization**, not permission to change `MainApplication`, database initialization sequencing, or SQLCipher initialization semantics.

### Exit criterion

The startup issue has an explicit owner-managed disposition and no architectural regression has been introduced.

---

# Phase 2 — Source Identity and Lifecycle Integrity

**Objective:** Make source identity and lifecycle behavior explicit and non-ambiguous.

## 2.1 SourceType policy model

Replace identity inference from `priority == 100` with an explicit domain-level source discriminator.

Required semantics:

```text
MANUAL   — not deletable, not disableable
CONTACTS — not deletable, not disableable
FTC      — not deletable, disableable
FCC      — not deletable, disableable
USER     — deletable, disableable
```

The exact representation may be an enum/sealed domain type as approved by the Source of Truth.

`priority` must not remain a source-identity signal.

### Exit criterion

No production identity decision relies on priority.

## 2.2 Source deletion boundary

Preserve the repository-level deletion guard while ensuring all intended application paths use the authoritative policy boundary.

Room-generated DAO code is not itself the policy authority.

Add regression coverage preventing protected-source deletion through approved application paths.

## 2.3 Source sync semantics

Verify:

- disabled sources are skipped by automatic sync;
- deliberate manual refresh can be distinguished;
- lifecycle status represents accepted dataset state;
- network success alone cannot produce HEALTHY;
- failures preserve last-known-good state;
- manual/contacts sources do not incorrectly enter network-sync semantics.

## 2.4 FCC source behavior

Root-cause the current FCC synchronization behavior.

Determine whether the observed state results from:

- endpoint/configuration;
- fetch failure;
- parser/validation rejection;
- lifecycle-state persistence;
- other failure.

Fix only after the failure path is positively identified.

### Exit criterion

FCC behavior has a known cause and an explicit resolved/deferred state.

## 2.5 Contacts boundary

Verify that Contacts import is genuinely connected to the authoritative source/data path.

Contacts-related UI status must represent actual persisted state rather than fabricated synchronization status.

Move raw `ContactsContract` / `ContentResolver` access behind the appropriate repository/data-source boundary.

### Exit criterion

Contacts behavior is proven end-to-end or a concrete defect is isolated and separately scoped.

---

# Phase 3 — Authoritative Persistence and Derived-Index Integrity

**Objective:** Preserve the database-as-truth invariant across all write paths.

## 3.1 Post-commit derived-index ordering

Enforce:

```text
DB transaction
      ↓
COMMIT
      ↓
rebuild derived indexes
```

Do not mutate Bloom state inside a transaction whose DB result may roll back.

Conceptually:

```text
insertEntriesAuthoritative()
        ↓
database commit
        ↓
rebuildDerivedIndexes()
```

An unready Bloom index remains a performance state, never an authority state.

## 3.2 Bloom readiness behavior

During any period in which the derived index is unavailable or rebuilding:

```text
Bloom unavailable
      ↓
authoritative Room path
```

The system must never treat stale/partial Bloom state as security truth.

### Required test

Failed transaction followed by decision lookup must prove:

- prior authoritative state remains intact;
- failed candidate state is absent;
- stale Bloom state cannot alter the authoritative result.

## 3.3 Mutation-boundary preservation

All security-rule mutations remain behind the approved authoritative mutation boundary.

No new repository, ViewModel, UI component, Android edge class, or synchronization helper may introduce an alternate DAO mutation path.

### Exit criterion

Architecture CI detects mutation methods by actual call sites, not merely imports.

## 3.4 SecurityRuleRepository scope review

Evaluate whether the current `SecurityRuleRepository` has become too broad.

Possible future boundaries include:

```text
SecurityRuleMutationRepository
SnapshotActivationService
SourceLifecycleRepository
```

Do not split mechanically.

Any split must preserve:

> one authoritative mutation boundary.

**Placeholder — retain / split / other architecture decision.**

---

# Phase 4 — Decision-Path Performance and Data Pipeline Hardening

**Objective:** Improve performance and resource safety without changing security semantics.

## 4.1 Prefix-matching hot path

The Bloom-positive path must not load every pattern into Kotlin merely to find the first matching prefix.

Move candidate selection into bounded SQL or another equivalently bounded authoritative strategy.

Desired semantic model:

```text
Bloom says "impossible"
    → no DB lookup

Bloom says "possible"
    → bounded authoritative query
    → exact result
```

The optimization must preserve existing priority and decision semantics.

### Exit criterion

Performance is improved without changing authoritative results.

## 4.2 Genuine streaming parser API

Replace whole-dataset accumulation with bounded processing.

Target shape:

```text
parse
  ↓
record
  ↓
batch
  ↓
authoritative repository
  ↓
discard batch
  ↓
next batch
```

Batch size should be configurable/testable and remain bounded.

### Required verification

A test double demonstrates that the parser never materializes the entire dataset.

## 4.3 Batched authoritative insertion

Use existing batch insertion capability rather than issuing one DB/Bloom operation per row.

Desired relationship:

```text
bounded parser batch
      ↓
one authoritative transaction
      ↓
commit
      ↓
derived-index rebuild
```

Do not reintroduce per-row Bloom mutation.

## 4.4 XLSX memory budgets

Supplement record-count limits with actual resource budgets:

- expanded shared-string byte budget;
- maximum cell length;
- existing shared-string count limit;
- existing row/resource limits.

The security principle is:

> Reject unsafe resource consumption, not merely excessive record counts.

### Exit criterion

Malformed or hostile XLSX input cannot consume unsafe memory merely by remaining below a record-count threshold.

---

# Phase 5 — Product Cleanup and Advanced / Until-You-Need Surface

**Objective:** Finish Pulse product cleanup only after the security/control-plane work is stable.

## 5.1 Orphan and unreachable code

Resolve only after complete artifact inspection.

For each orphan:

```text
KEEP
WIRE
REPLACE
PURGE
UNKNOWN
```

Current examples include:

- `PermissionSettingsScreen`;
- `TelemetryViewModel`;
- `BenchmarkResult`;
- `PermissionStatus`;
- `ThreatSource`.

Unknown purpose prohibits deletion.

## 5.2 Advanced drawer migration

Treat the swipe-right drawer as a legitimate Pulse product surface.

Required work:

- remove Multi-Port branding;
- retain intentionally adopted capabilities;
- remove explicitly rejected Multi-Port capabilities;
- move appropriate intervention/diagnostic functions into the Advanced / Until-You-Need model;
- wire intentional access where approved;
- preserve discoverability appropriate to the final UX.

The product owner controls final feature disposition.

Manus may implement only capabilities explicitly authorized by the corresponding step manifest.

## 5.3 UI persistence boundaries

Complete migration of UI-owned persistence to application/data boundaries.

No Compose screen should independently create alternate persistence behavior.

## 5.4 Onboarding/EULA persistence

Use:

```text
UI
 ↓
ViewModel
 ↓
SettingRepository / AgreementRepository
 ↓
durable persistence
 ↓
success
 ↓
navigation
```

Persist agreement identity/version/timestamp coherently.

A failed persistence operation must prevent onboarding from falsely advancing.

## 5.5 UI correctness and design system

Address verified correctness issues such as:

- `ShieldStatusGlow` color conversion;
- remaining design-system defects;
- stale Multi-Port visual language;
- advanced-surface presentation.

Do not allow visual cleanup to reintroduce persistence or architecture violations.

## 5.6 Legacy resources

Delete legacy XML/layout/view resources only after:

1. complete target-file inspection;
2. reference search;
3. runtime/Android dependency assessment;
4. protected-inventory check;
5. explicit step authorization.

The historical PhoneStateReceiver incident remains the standing reason grep-only deletion is prohibited.

---

# Phase 6 — Manus CI Enforcement

**Objective:** Make the build plan mechanically enforceable for automated implementation.

The separate **Manus CI Guardrails** specification is the detailed execution contract.

## 6.1 Step scope manifests

Every executable step receives:

```text
phase
subphase
step
objective
allowed files
allowed patterns
allowed deletions
protected files
forbidden paths
forbidden operations
required tests
required checks
acceptance criteria
```

No manifest means no authorized Manus implementation.

## 6.2 Changed-file guard

CI fails for unauthorized:

- modification;
- creation;
- deletion;
- rename;
- copy;
- relevant path/mode changes.

## 6.3 Deletion guard

Deletion requires explicit authorization in the current step.

No deletion may be justified solely by:

- unused status;
- grep results;
- legacy naming;
- Multi-Port origin;
- compiler reachability.

## 6.4 Unknown-protection guard

Any `UNKNOWN` artifact is non-deletable until explicitly reclassified.

## 6.5 Architecture guard

CI checks actual mutation methods and actual call sites.

Initial security mutation methods include:

```text
insertEntry
insertEntries
deleteEntry
deleteEntriesBySourceId
updateEntry
```

Unauthorized mutation sites fail CI.

## 6.6 Edge-to-DAO guard

CI prevents new direct persistence access from:

- UI;
- Activity;
- Fragment;
- BroadcastReceiver;
- CallScreeningService;
- notification action handlers;
- other platform ingress.

## 6.7 Schema/dependency/workflow guards

Schema, dependency, and GitHub Actions changes require explicit step authorization.

Workflow changes receive additional checks for:

- permissions;
- action pinning;
- secrets;
- triggers;
- accidental security-gate weakening.

## 6.8 Test-integrity guard

CI detects attempts to weaken verification by:

- deleting tests;
- disabling tests;
- skipping tests;
- reducing meaningful assertions;
- changing security expectations without declared contract change;
- adding `continue-on-error` to required checks.

## 6.9 Required status

The Manus guardrail workflow becomes a required protected-branch status check.

Desired merge condition:

```text
Application CI
Security CI
Manus Guardrails
        ↓
      GREEN
        ↓
      MERGE
```

## 6.10 Scope expansion rule

If implementation reveals another file or behavior is required:

```text
STOP
 ↓
update/approve step scope
 ↓
resume
```

Do not silently expand scope for convenience.

Owner-managed ledger handling remains separate from this plan.

**Placeholder — final ledger/CI integration decision.**

---

# Phase 7 — Security CI, Dependency, and Workflow Hardening

**Objective:** Turn architectural guarantees into mandatory repository-level gates.

## 7.1 Mandatory test gating

Required tests must fail the workflow when they fail.

No required security test may silently pass because it was skipped or allowed to fail.

## 7.2 Instrumented security coverage

Mandatory coverage must include, as applicable:

- Android Keystore;
- SQLCipher open/close;
- Keystore invalidation;
- DB deletion/reset;
- Room migrations;
- fresh-install schema;
- CallScreeningService behavior;
- decision matrix;
- security failure;
- source lifecycle;
- Bloom-authority behavior.

## 7.3 Architecture drift enforcement

Expand the architecture checker from import-centric detection to actual mutation-site enforcement.

The goal is regression detection, not merely documentation.

## 7.4 Dependency and secret scanning

Add mandatory vulnerability and secret scanning with:

- defined severity threshold;
- CI failure policy;
- explicit exception ownership;
- no silent allowlists.

## 7.5 GitHub Actions hardening

Apply:

- least-privilege permissions;
- immutable action pinning where practical;
- controlled workflow changes;
- no security-gate weakening.

Resolve the known missing `verify-launch-and-capture.sh` workflow dependency.

---

# Phase 8 — Release Hardening

**Objective:** Validate the actual release artifact, not merely the debug application.

## 8.1 R8

Replace the broad package-wide keep rule with narrow justified rules.

Remove stale class-name rules and dead Fragment references.

Protect only components that genuinely require preservation, including manifest entry points and reflection-required classes.

Validate a minified release build on real hardware where practical.

## 8.2 Manifest and permission audit

For every permission:

```text
permission
 ↓
exact runtime consumer
 ↓
exact product feature
 ↓
runtime necessity
 ↓
Play-policy justification
```

Do not retain permissions merely because the inherited architecture once requested them.

## 8.3 Signing

Release signing must fail closed when credentials are unavailable.

Keys remain outside source control and logs.

## 8.4 SBOM / provenance

Produce:

- SBOM;
- artifact checksums;
- provenance;
- source commit linkage;
- workflow linkage.

Preserve these with the release candidate.

## 8.5 Release validation

Run:

- release build;
- R8 validation;
- instrumented tests;
- launch verification;
- exported-component review;
- manifest review;
- backup-exclusion review;
- privacy/logging review;
- required security scans.

---

# Phase 9 — Final Release Candidate Gate

A release candidate may advance only when:

- security invariants have evidence;
- the authoritative mutation boundary is enforced;
- Bloom is demonstrably non-authoritative;
- source datasets are atomic and last-known-good;
- partial/truncated source data is rejected;
- screening failure is explicit;
- all decision tiers are tested;
- gray-zone review is functional;
- Keystore/DB reset behavior is instrumented-tested;
- required CI tests cannot silently fail;
- Manus guardrails are green;
- dependency/secret scans are green or explicitly excepted;
- R8 release build is validated;
- SBOM/checksum/provenance/signing artifacts exist;
- manifest/exported-component/privacy review is complete;
- CallScreeningService response behavior is proven under null/invalid and slow-persistence conditions;
- Bloom post-commit behavior is proven with rollback/contamination testing;
- SourceType is the source-identity discriminator;
- permissions are individually justified;
- no unresolved protected-surface deletion ambiguity remains.

---

# Non-Goals and Deliberate Constraints

Do not:

- reintroduce asynchronous DB initialization merely to hide startup cost;
- make Bloom authoritative;
- allow notification/rate-limiting code to decide call disposition;
- allow UI or platform-edge code to become an alternate persistence path;
- make security failure indistinguishable from legitimate allow;
- reintroduce Apache POI solely to simplify parsing;
- use TLS pinning as a substitute for source-artifact authenticity;
- delete inherited Multi-Port functionality merely because it is inherited;
- delete unknown artifacts merely because they appear unused;
- weaken CI to make an automated build pass;
- expand Manus scope silently.

---

# Build Execution Model

```text
Source of Truth
      ↓
Phase / Subphase / Step
      ↓
Machine-readable scope manifest
      ↓
Manus implementation
      ↓
CI scope enforcement
      ↓
Architecture/security tests
      ↓
Normal build/test/lint
      ↓
Owner review
      ↓
Next step
```

Default execution unit:

```text
1 Manus PR = 1 build step
```

Any exception must be explicit and machine-visible.

---

# Final Engineering Principle

Pulse is not finished when the code builds.

Pulse is ready when the system's security properties remain true while:

- the process is cold;
- calls arrive before UI launch;
- persistence fails;
- derived indexes are rebuilding;
- sources fail or become stale;
- malformed data is supplied;
- large datasets are processed;
- concurrent mutations occur;
- automated agents modify the repository;
- dependencies change;
- R8 optimizes the release;
- and the final release artifact is installed on a real device.

The purpose of this build plan is therefore not maximum feature throughput.

It is **controlled convergence on a secure, maintainable Pulse architecture without losing intentionally retained functionality or allowing automated implementation to silently expand scope.**
