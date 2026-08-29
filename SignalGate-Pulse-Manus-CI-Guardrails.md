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
