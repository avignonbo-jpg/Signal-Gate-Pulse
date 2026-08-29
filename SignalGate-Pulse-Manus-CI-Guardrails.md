
    "schema_version": "1.1",
    "governing_doc": "Architecture-Contract.md",
    "global_constraints": [
        "Touch only files listed in an issue's allowed_files; if a fix needs a file not listed, stop and report instead of expanding scope",
        "Never edit PROJECT_LEDGER.md history; append new entries only",
        "Never modify ScreeningDecision.kt's init invariant (securityFailure must tie to SECURITY_FAILURE tier/action)",
        "Never change NotificationPolicy, auditRequired, or reviewCardRequired values while fixing a HapticPolicy-only or comment-only issue",
        "Never add a new third-party dependency to close any single issue below without flagging it separately first",
        "Never delete a file unless explicitly listed under fix; orphan/unwired code defaults to wiring in, not deletion",
        "One issue id = one commit/change-set; do not batch unrelated issue ids into the same diff",
        "If an issue's fix requires changing production security/decision logic (not just tests, comments, or UI wiring), stop and report before editing rather than proceeding"
    ],
    "issues": [
        {
            "id": "0.6-gap",
            "risk_tier": "test-only",
            "allowed_files": [
                "android/app/src/test/kotlin/com/signalgate/pulse/ScreeningServiceCallResponseMappingTest.kt"
            ],
            "read_only_context": [
                "SignalGateCallScreeningService.kt",
                "ScreeningDecision.kt",
                "ScreeningAction.kt"
            ],
            "issue": "No test calls toCallResponse() or handleSecurityFailure(); all SECURITY_FAILURE tests stop at domain layer",
            "fix": "Add new Robolectric test file asserting toCallResponse(ScreeningAction.SECURITY_FAILURE).disallowCall==false",
            "forbidden_actions": [
                "Modifying SignalGateCallScreeningService.kt production code",
                "Modifying any existing test file",
                "Changing toCallResponse's actual mapping logic"
            ],
            "acceptance_criteria": "New test compiles, runs under RobolectricTestRunner, passes, and directly invokes toCallResponse and/or handleSecurityFailure",
            "status": "open"
        },
        {
            "id": "0.3-miscite",
            "risk_tier": "docs-only",
            "allowed_files": [
                "Architecture-Contract.md"
            ],
            "issue": "SourceDeletionCascadeTest cited as 0.3 evidence but calls sourceDao.deleteSource() directly, bypassing ProtectedSourceDeletionException guard",
            "fix": "Remove the citation; replace with a note that the file covers FK-cascade behavior only, not the protected-source guard",
            "forbidden_actions": [
                "Editing any .kt file",
                "Editing any section of the contract other than the 0.3 citation"
            ],
            "acceptance_criteria": "0.3 entry no longer cites SourceDeletionCascadeTest as guard-level evidence",
            "status": "open"
        },
        {
            "id": "0.1-gap",
            "risk_tier": "test-only",
            "allowed_files": [
                "android/app/src/test/kotlin/com/signalgate/pulse/logic/SecurityRuleRepositoryContactImportBoundaryTest.kt"
            ],
            "read_only_context": [
                "SecurityRuleRepository.kt",
                "BlocklistRepository.kt"
            ],
            "issue": "Contact-rule and imported-rule mutation routing untested; only manual allow/block/remove covered",
            "fix": "Add test proving contact-rule and imported-rule mutations route through SecurityRuleRepository, not a bypass path",
            "forbidden_actions": [
                "Modifying SecurityRuleRepository.kt or BlocklistRepository.kt production code \u2014 if a real routing bug is found, stop and report rather than fix"
            ],
            "acceptance_criteria": "New test passes and fails if a mutation path bypassing SecurityRuleRepository is (re)introduced",
            "status": "open"
        },
        {
            "id": "0.5-scope",
            "risk_tier": "docs-only",
            "allowed_files": [
                "Architecture-Contract.md"
            ],
            "issue": "Ambiguous whether 0.5 requires DataSyncEngine-only or all parsers (CSV+XLSX)",
            "fix": "Add one explicit sentence to the 0.5 entry stating the intended scope (parsing layer generally vs. DataSyncEngine specifically)",
            "forbidden_actions": [
                "Editing any .kt file",
                "Changing any other contract section"
            ],
            "acceptance_criteria": "0.5 entry states scope explicitly; no future audit needs to infer it",
            "status": "open"
        },
        {
            "id": "branch-fork-haptic",
            "risk_tier": "product-decision",
            "allowed_files": [
                "android/app/src/main/java/com/signalgate/pulse/logic/ScreeningDecision.kt",
                "android/app/src/main/java/com/signalgate/pulse/ui/notifications/PulseVibration.kt",
                "android/app/src/test/kotlin/com/signalgate/pulse/logic/ScreeningDecisionConsequencesTest.kt",
                "android/app/src/test/kotlin/com/signalgate/pulse/ScreeningServiceEdgeExecutionTest.kt",
                "android/app/src/test/kotlin/com/signalgate/pulse/ui/notifications/PulseTriggerLimiterTest.kt",
                "PROJECT_LEDGER.md"
            ],
            "issue": "HEURISTIC_BLOCK haptic fix (BLOCK_PULSE->NONE) not present in this branch lineage",
            "fix": "Change HEURISTIC_BLOCK's HapticPolicy from BLOCK_PULSE to NONE in ScreeningDecision.forTier; update comments and the three tests to match; append (do not rewrite) a PROJECT_LEDGER.md entry",
            "forbidden_actions": [
                "Changing HEURISTIC_BLOCK's NotificationPolicy, auditRequired, or reviewCardRequired",
                "Deleting HapticPolicy.BLOCK_PULSE or its VibrationEffect definition",
                "Editing any ledger content before this session's own new entry"
            ],
            "acceptance_criteria": "HEURISTIC_BLOCK haptic is NONE; all three tests updated and passing; BLOCK_PULSE remains defined but unreferenced by forTier",
            "status": "open"
        },
        {
            "id": "dashboard-dead-tap",
            "risk_tier": "ui-only",
            "allowed_files": [
                "android/app/src/main/java/com/signalgate/pulse/ui/screens/ConsumerDashboardScreen.kt"
            ],
            "read_only_context": [
                "ui/dashboard/DashboardViewModel.kt",
                "ui/navigation/NavGraph.kt"
            ],
            "issue": "Shield-inactive card text says 'Tap to restore protection' but the Box has no .clickable modifier",
            "fix": "Add .clickable that launches roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), matching the pattern already used in OnboardingWizardScreen.kt",
            "forbidden_actions": [
                "Adding a new permanent navigation destination",
                "Adding this to a settings/drawer menu",
                "Modifying DashboardViewModel.kt or NavGraph.kt"
            ],
            "acceptance_criteria": "Tapping the card when shield is inactive fires the role-request intent; card is unchanged when shield is active",
            "status": "open"
        },
        {
            "id": "orphan-permission-screen",
            "risk_tier": "ui-only",
            "allowed_files": [
                "android/app/src/main/java/com/signalgate/pulse/ui/navigation/NavGraph.kt",
                "android/app/src/main/java/com/signalgate/pulse/ui/navigation/Screen.kt",
                "android/app/src/main/java/com/signalgate/pulse/ui/screens/ConsumerDashboardScreen.kt"
            ],
            "issue": "Built screen with no NavGraph route, unreachable",
            "fix": "Add a route reachable only as a contextual link from the dashboard's shield-inactive card (see dashboard-dead-tap); do not add a permanent drawer/settings entry",
            "forbidden_actions": [
                "Deleting PermissionSettingsScreen.kt",
                "Adding a permanent nav-drawer or settings-menu entry point",
                "Rewriting PermissionSettingsScreen.kt's internal logic"
            ],
            "acceptance_criteria": "Screen is reachable via the dashboard card only when relevant; not present in any permanent menu",
            "status": "open"
        },
        {
            "id": "orphan-telemetry-vm",
            "risk_tier": "structural",
            "allowed_files": [
                "android/app/src/main/java/com/signalgate/pulse/ui/viewmodels/TelemetryViewModel.kt",
                "android/app/src/main/java/com/signalgate/pulse/ui/viewmodels/RecentCallsViewModel.kt",
                "android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt"
            ],
            "issue": "TelemetryViewModel registered in Koin, zero screen call sites, overlaps RecentCallsViewModel",
            "fix": "Merge TelemetryViewModel's transformation logic into RecentCallsViewModel, then delete TelemetryViewModel.kt and its Koin binding",
            "forbidden_actions": [
                "Changing CallLogRepository",
                "Modifying any screen/Composable file",
                "Leaving a dangling Koin binding after deletion"
            ],
            "acceptance_criteria": "TelemetryViewModel.kt no longer exists; RecentCallsViewModel covers its prior functionality; app compiles",
            "status": "open"
        },
        {
            "id": "gradle-comment-drift",
            "risk_tier": "comment-only",
            "allowed_files": [
                "android/app/build.gradle"
            ],
            "issue": "Comment claims exportSchema=false; actual value is true",
            "fix": "Correct the comment text to match the actual exportSchema value",
            "forbidden_actions": [
                "Changing exportSchema's actual value or any other build.gradle setting"
            ],
            "acceptance_criteria": "Comment matches actual configured value; no build behavior changes",
            "status": "open"
        },
        {
            "id": "shieldglow-color-bug",
            "risk_tier": "bugfix-minimal",
            "allowed_files": [
                "<exact ShieldStatusGlow.kt path \u2014 confirm before editing>"
            ],
            "issue": "Uses Color.hashCode() instead of .toArgb() for native Paint color",
            "fix": "Replace Color.hashCode() with .toArgb() at the identified call site only",
            "forbidden_actions": [
                "Changing animation timing, glow radius, or any other rendering logic in this file"
            ],
            "acceptance_criteria": "Native Paint color renders correctly; no other visual behavior changes",
            "status": "open"
        },
        {
            "id": "appmodule-osi-conflict",
            "risk_tier": "comment-only",
            "allowed_files": [
                "android/app/src/main/java/com/signalgate/pulse/di/AppModule.kt"
            ],
            "issue": "Doc comments use invented OSI labels (L2/L4/L6) conflicting with contract's Layer 1-7 scheme",
            "fix": "Rewrite the affected doc comments to reference Layer 1-7 by name per Architecture-Contract.md \u00a73",
            "forbidden_actions": [
                "Changing any Koin binding, module order, or dependency graph"
            ],
            "acceptance_criteria": "Comments reference Layer 1-7 only; no functional change; app compiles identically",
            "status": "open"
        },
        {
            "id": "drift-script-gap",
            "risk_tier": "tooling-only",
            "allowed_files": [
                "scripts/check-architecture-drift.sh"
            ],
            "issue": "Rule 6 does not scan ui/theme/ for cross-cutting purity",
            "fix": "Extend Rule 6's scanned path list to include ui/theme/",
            "forbidden_actions": [
                "Modifying Rules 1-5 or Rule 7",
                "Changing what Rule 6 flags, only where it looks"
            ],
            "acceptance_criteria": "Script still passes on current clean code; correctly flags a deliberately-introduced ui/theme/ violation in a dry run",
            "status": "open"
        },
        {
            "id": "no-cve-scan",
            "risk_tier": "ci-only",
            "allowed_files": [
                ".github/workflows/"
            ],
            "issue": "No dependency/CVE scanning enforced as CI gate",
            "fix": "Add a new workflow or job step (Dependabot config or OWASP Dependency-Check) as a required check",
            "forbidden_actions": [
                "Modifying existing test/build job steps",
                "Removing continue-on-error from unrelated jobs as part of this change"
            ],
            "acceptance_criteria": "New scan runs on PR/push and is a required status check; existing workflows unchanged",
            "status": "open"
        },
        {
            "id": "startup-no-baseline",
            "risk_tier": "measurement-only",
            "allowed_files": [],
            "issue": "Instrumentation exists but no real-device cold-start baseline captured",
            "fix": "Capture Activity-path and Telecom-triggered cold start on a representative low/mid-tier real device; produce a report",
            "forbidden_actions": [
                "Modifying StartupDiagnostics.kt or any production code as part of this task"
            ],
            "acceptance_criteria": "Report with per-checkpoint timings delivered; no code changed",
            "status": "open"
        }
    ]
}

















SignalGate Pulse — Manus CI Guardrails
Status: Extracted, 2026-08-25 — content moved verbatim from SIGNALGATE-PULSE-NEXT-ARCHITECTURAL-BUILD-PLAN.md's Phase 6 ("Manus CI Enforcement") and its directly related cross-references (the Build Execution Model, the Manus-specific Non-Goals, and the Phase 9 gate criterion that references it). Nothing here is new content — this is a relocation, not a rewrite, done because the source document explicitly named this "the detailed execution contract" as a separate thing from itself, and because this section alone was substantial enough to deserve independent existence rather than staying buried at Phase 6 of an 1,011-line document.
What this document is for: making the rest of the build plan mechanically enforceable when an automated agent (Manus, or any future automated implementer) is the one doing the work, rather than relying on the agent correctly interpreting scope from prose alone. Everything below describes required CI behavior and process shape. It does not yet contain the actual GitHub Actions workflow YAML, the machine-readable manifest schema, or other concrete implementation artifacts — see the placeholder section at the end for what's genuinely still undecided, separate from what's specified but not yet built.
Objective: Make the build plan mechanically enforceable for automated implementation.
1. Step scope manifests
Every executable step receives a manifest containing:
Code
No manifest means no authorized Manus implementation.
2. Changed-file guard
CI fails for unauthorized:
modification
creation
deletion
rename
copy
relevant path/mode changes
3. Deletion guard
Deletion requires explicit authorization in the current step.
No deletion may be justified solely by:
unused status
grep results
legacy naming
Multi-Port origin
compiler reachability
4. Unknown-protection guard
Any UNKNOWN artifact is non-deletable until explicitly reclassified. (See the source document's protected-artifact inventory concept — an item without a determined KEEP/REFACTOR/SECURE/PURGE classification defaults to protected, not to available-for-deletion.)
5. Architecture guard
CI checks actual mutation methods and actual call sites, not just import statements — this is the fix for the structural weakness already tracked elsewhere (the drift script is currently grep/import-based, not call-site-based).
Initial security mutation methods include:
Code
Unauthorized mutation sites fail CI.
6. Edge-to-DAO guard
CI prevents new direct persistence access from:
UI
Activity
Fragment
BroadcastReceiver
CallScreeningService
notification action handlers
other platform ingress
7. Schema/dependency/workflow guards
Schema, dependency, and GitHub Actions changes require explicit step authorization.
Workflow changes receive additional checks for:
permissions
action pinning
secrets
triggers
accidental security-gate weakening
8. Test-integrity guard
CI detects attempts to weaken verification by:
deleting tests
disabling tests
skipping tests
reducing meaningful assertions
changing security expectations without a declared contract change
adding continue-on-error to required checks
9. Required status
The Manus guardrail workflow becomes a required protected-branch status check.
Desired merge condition:
Code
10. Scope expansion rule
If implementation reveals another file or behavior is required:
Code
Do not silently expand scope for convenience.
Owner-managed ledger handling remains separate from this plan.
Build Execution Model (directly related — the workflow this whole document exists to enforce)
Code
Default execution unit:
Code
Any exception must be explicit and machine-visible.
Related non-goals (from the source document, Manus-specific subset)
Do not:
weaken CI to make an automated build pass
expand Manus scope silently
Related release gate criterion (from the source document's Phase 9)
A release candidate may not advance unless, among the rest of that phase's criteria: Manus guardrails are green.
What's genuinely still placeholder — not specified anywhere yet
These are real gaps, not filled in by this extraction because they didn't exist in the source material to extract:
The actual GitHub Actions workflow file implementing guards 2 (changed-file), 3 (deletion), 5 (architecture/mutation-site), 6 (edge-to-DAO), 7 (schema/dependency/workflow), and 8 (test-integrity) as real, runnable checks.
The machine-readable schema for the step-scope manifest described in §1 — field names are listed, but no concrete format (JSON/YAML schema, file location, validation tooling) exists yet.
The protected-artifact inventory itself (§4 depends on one existing) — referenced by the source document, not yet built as an actual tracked file.
How this workflow becomes a required protected-branch status check in practice (§9) — the GitHub branch-protection configuration itself.
If/when this gets built out, it belongs in this file, not folded back into the main build plan or contract — that's the whole reason for the split.
