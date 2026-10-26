SignalGate Pulse — Project Ledger
This is the single authoritative log of project state. Every AI session (any model, any tool) reads this file first and appends to it last. This replaces ad hoc tracking across chat history, memory, and ad hoc notes. The current branch-level governance is now anchored to the uploaded audit snapshot `SignalGatePulseAudit_20261025_SOURCE_OF_TRUTH (1).md`.
Governing document: Architecture-Contract.md (v4 content, adopted 2026-08-25 as the canonical file — v3 is superseded, no separate Architecture-Contract-v4.md remains; its content was merged in during the v4 reconciliation).
Document map (added 2026-08-25, after this project hit real confusion from five overlapping governance files with no stated relationship between them) — every root-level .md file and what it's accountable for:
Architecture-Contract.md — the invariant-level truth. What must always be true, regardless of what's currently being worked on.
SECURITY-DEVOPS-BUILD-PLAN.md — the technical findings/evidence log for security-relevant work. Real CI run IDs, real commit references, per-item detail.
SignalGate-Pulse-Release-Roadmap.md — the execution sequence. Turns open items into an ordered Step 1-8 (Part 1, path to v1.0) plus a post-v1.0 roadmap (Part 2) that exists nowhere else. Explicitly states what is not yet proven.
SIGNALGATE-PULSE-NEXT-ARCHITECTURAL-BUILD-PLAN.md — the safety/process wrapper. Protects against unsafe or hasty execution of the above, especially by automated agents. Contains real placeholders and execution gates.
SIGNALGATE-PULSE-MANUS-CI-GUARDRAILS.md — extracted 2026-08-25 from the above document's Phase 6. The detailed automated-agent CI enforcement contract: step scope manifests, changed-file/deletion rules, and branch discipline.
SignalGatePulseAudit_20261025_SOURCE_OF_TRUTH (1).md — the current authoritative branch snapshot for `consumer-v1`. This is the governing factual source of truth for the branch and supersedes `Source-Of-Truth.md` for active governance.
Source-Of-Truth.md — retained as a historical branch audit/reference document. It remains readable for continuity, but it is not the active governing source-of-truth.
PROJECT_LEDGER.md (this file) — the append-only record of what actually happened, session by session.
If two of these ever visibly disagree, that's a signal to verify against live source directly, the same way the 2026-08-25 SourceType correction above got resolved — not to silently trust whichever document is older, louder, or more recently edited.
How to use this file (read this first, every session)
At the start of any session working on this codebase:
Read the "Current State" section below before writing or suggesting any code.
Read "Open Items" — do not re-solve something already listed as decided.
If asked to make an architectural change (new class, new layer assignment, new dependency), check it against Architecture-Contract.md before proceeding. If it conflicts, say so explicitly and stop.
At the end of any session (before ending the conversation):
Append a new entry to "Session Log" — even a short one. Include: date, what changed, which files, which layer(s) touched, and whether the Architecture Contract was consulted.
Update "Current State" if the change affects it (new branch as source of truth, new known-broken state, new dependency, etc.).
Move anything resolved out of "Open Items" into the session log entry; add anything newly discovered into "Open Items."
Never silently skip this. If the person doesn't ask for a ledger update, do it anyway before the conversation ends — this is a standing instruction, not a one-time request.
Current State
Authoritative branch: consumer-v1 — confirmed this session: the uploaded Signal-Gate-Pulse-consumer-v1 zip is byte-identical to the snapshot represented in `SignalGatePulseAudit_20261025_SOURCE_OF_TRUTH (1).md`.
Authoritative source-of-truth document: `SignalGatePulseAudit_20261025_SOURCE_OF_TRUTH (1).md` — adopted 2026-10-25 for active branch governance; it supersedes `Source-Of-Truth.md` as the branch's governing audit snapshot.
Last known-good build: confirmed green as of 2026-08-14/15 (see Session Log) — GitHub Actions CI passed against the merge commit before it was merged into consumer-v1: compilePulseDebugKotlin/assemblePulseDebug and the required validation jobs were green.
minSdkVersion: 29 — confirmed present in actual build.gradle this session (matches the 2026-07-15 entry's fix).
Known broken/incomplete:
The HEURISTIC_FLAG gray-zone persistence gap is resolved and CI-verified on 2026-08-19; Phase 2 notification/haptic, rate-limiting, notification-action, and privacy work is also CI-verified and closed.
check-architecture-drift.sh Rule 6 doesn't scan ui/theme/, so the Cross-Cutting purity claim for Color/Theme/SignalGateTheme/Effects is currently unenforced. See Contract §9.6. Still open — unaddressed by the current audit snapshot.
check-architecture-drift.sh invocation is CI-enforced and was confirmed green in the Phase 1–3 mandatory workflows; the former open note is historical and resolved.
The gray-zone notification/haptic/rate-limiter feature was merged and CI-verified in Phase 2.1–2.2; its ordering was rechecked against the current CallActionReceiver and persisted-review boundaries.
Architecture Contract version: v3 — Reconciled and adopted. The committed Architecture-Contract.md contains the reconciled v3 structure, 10 security invariants, Phase 0–7 roadmap, Definition of Done, and the layer boundaries that govern current work.
Security control-plane status: Phase 0 is formally closed and CI-verified. Phase 1 Decision Engine Integrity is formally closed and CI-verified. Phase 2 is formally closed and CI-verified. Phase 3 and later phases continue to be tracked in the session log and open items.
Unverified note carried over from a parallel session's ledger entry, not yet confirmed: that entry stated "Architecture Contract version: v2 — Reconciled (this entry). Amendments 0–5 are applied and approved"; no current source-of-truth file has reissued that claim.
Open Items
[ ] Delete 3 orphaned legacy XML layouts + 26 unused view IDs (call_shield_overlay_enhanced.xml, dialog_add_blocked_number.xml, overlay_call_blocked.xml) — carried over, not yet independently revalidated.
[x] Phase 0 mandatory execution evidence and generated Room schema version 3 — resolved and closed in the Phase 0 ledger entry; retained here only as historical reconciliation.
[ ] NEW: Migrate remaining BlocklistRepository callers (BlockedNumbersViewModel, ContactsViewModel, PendingCardViewModel) directly onto SecurityRuleRepository, per that class's own deprecation note.
[ ] Set up pre-push / CI-side YAML and whitespace validation filters — carried over from prior session, still not done.
[ ] Live call-test READ_PHONE_STATE necessity: confirm on a real device whether anything still needs this permission before removing PhoneStateReceiver, its manifest entries, or correcting the stale analytics/permission logic.
[x] Re-verify and merge the gray-zone notification/haptic implementation — completed and CI-verified in Phase 2.1–2.2; retained here as historical reconciliation.
[ ] Resolve PermissionSettingsScreen unreachability (Contract §9.1) — wire into Screen.kt/NavGraph.kt or delete.
[ ] Resolve orphaned TelemetryViewModel (Contract §9.2) — wire into CallLogScreen or delete class + Koin binding.
[ ] Fix app/build.gradle doc-comment exportSchema = false claim to match actual exportSchema = true (Contract §9.3)
[ ] Fix ShieldStatusGlow.kt to use .toArgb() instead of .hashCode() (Contract §9.4)
[ ] Extend check-architecture-drift.sh Rule 6 to scan ui/theme/ (Contract §9.6)
[ ] Add a dependency/CVE scan as a hard CI gate (Contract §10.3) — not currently enforced anywhere.
[ ] Build/re-verify test coverage per Contract §10.2 (five-tier matrix, fresh-install migration path, Keystore-invalidation instrumented test)
[ ] Contract audit 10.14 — clarify the Contacts source enablement contract: onboarding offers an optional READ_CONTACTS permission and a skip-import action, but skipping only advances and does not persist contact-source configuration.
[ ] Contract audit 10.15 — confirm whether a 14-day/250-entry retention requirement exists; no such policy was found in current production code or tests.
Removed this session (resolved or found stale — see correction notes)
Confirm which branch is the actual current source of truth on GitHub — resolved, see Current State.
Re-add Apache POI dependency with log4j exclusion — this was never actually needed. Verified this session: DataSyncEngine.kt's XLSX path uses native ZipInputStream + SAX parsing with no Apache POI dependency in the live branch.
Review and approve/reject Architecture-Contract-Amendments.md, then fold approved changes — already done. The Contract document's own closing note said Amendments 0–5 were "applied and approved".
Decide on Security/Ops review of Apache POI removal vs. keeping it — moot, folded into the POI correction above; there is no POI dependency to review.
Session Log
(Newest entry on top).

2026-10-25 — Branch governance rollover to the uploaded audit snapshot as the controlling source-of-truth
Who: Repository automation / branch governance admin.
What: Adopted `SignalGatePulseAudit_20261025_SOURCE_OF_TRUTH (1).md` as the current governing source-of-truth document for `consumer-v1`, and updated the branch governance docs to mark `Source-Of-Truth.md` as historical rather than active.
Files touched: `SignalGatePulseAudit_20261025_SOURCE_OF_TRUTH (1).md`; `Source-Of-Truth.md`; `PROJECT_LEDGER.md`; `README.md`.
Layers touched: governance, documentation, and project ledger only.
Contract consulted: no code or architecture change; branch governance and the current `consumer-v1` file set were checked against the uploaded audit snapshot before the supersession notice was applied.
Validation: Reviewed the updated governance docs and ledger references; no application behavior or source code was changed.
Status: Active branch governance now points to the uploaded 2026-10-25 snapshot as the authoritative source-of-truth document.
Signature: Repository automation / branch governance admin — 2026-10-25

2026-10-22 — WP-2 validation hardening follow-up
Who: Manus AI.
What: Reapplied only the still-valid WP-2 hardening to the current consumer-v1 source of truth. The pre-WP-2 README and script comments were stale and are now corrected in the merged WP-2 cleanup; the relevant validations were re-run without changing production behavior.
Files touched: scripts/check-test-integrity.sh; scripts/check-architecture-drift.sh; scripts/check-ledger-updated.sh; android/app/src/main/java/com/signalgate/pulse/workers/CommunitySyncWorker.kt; PROJECT_LEDGER.md.
Layers touched: validation scripts, source-of-truth documentation, and comments only.
Contract consulted: no contract change; applicationId/Kotlin package identity and existing architecture boundaries were checked against the current source.
Validation: sandbox-only static hardening; x86_64 CI pending; no device run. No local unit-test or assemble result is claimed from this phase.
Status: WP-2 hardening is prepared for explicit staging and remote validation; CI pending; no device run.
Signature: Manus AI — 2026-10-22
2026-10-18 — Real-engine timeout probe confirms Scenario B handling
Who: Manus AI, following the open runtime question about cancellation while `CallScreeningEngine` is suspended in `DataSourceRepository.getCallDecision()`.
What: Ran a temporary, untracked timeout probe against commit `8fd768eaeec0fd63d8c849c626ba4e4426624eac`, which is an ancestor of the current live `consumer-v1` head `d27037961452c833a0a827b2490f5d5f315e5d7ca`.
Files touched: `PROJECT_LEDGER.md` only. The probe was scratch-only and was deleted; no production or permanent test source changed. The relevant `SignalGateCallScreeningService.kt` and `CallScreeningEngine.kt` code paths were reviewed before recording the result.
Layers touched: JVM timeout verification and project ledger; no application behavior was changed.
Contract consulted: yes — the existing response/deadline behavior and the live service/engine paths were checked before recording the result.
Validation: temporary Gradle unit-test run completed with 2 tests, 0 failures, 0 errors, and 0 skipped. The original Robolectric runner could not initialize its pinned Conscrypt 2.5.2 native library in this sandbox, so we used the direct timeout probe.
Status: Scenario B confirmed. A decision timeout reaches `executeScreeningSafely`, triggers the security-failure path once, and does not produce a second normal Telecom response. This resolves the timeout-edge question without altering the branch.
Signature: Manus AI — 2026-10-18

2026-10-13 — Rethrow CancellationException in CommunitySyncWorker
Who: Manus AI.
What: Added a CancellationException catch before the generic Exception catch in CommunitySyncWorker.doWork() and rethrew cancellation so WorkManager cancellation is not converted into Result.failure().
Files touched: android/app/src/main/java/com/signalgate/pulse/workers/CommunitySyncWorker.kt; android/app/src/test/kotlin/com/signalgate/pulse/workers/CommunitySyncWorkerTest.kt; PROJECT_LEDGER.md.
Layers touched: Layer 1 WorkManager worker edge handling, JVM/Robolectric tests, and project ledger.
Contract consulted: yes — Architecture-Contract.md Layer 1 worker ownership, the cancellation-rethrow task specification, and the existing CommunitySyncWorker scheduling/unique-name contract. No public API or source-of-truth governance changed.
Validation: Current-base green CI run `38127582107` on `e577ec8fa78c73349e7f2f1311c9dc64c6dbde54` established a baseline of 135 tests, 0 failures, 0 errors, and 0 skipped. Direct-construction targeted tests were also green in the sandbox.
Status: CI pending on the draft PR; sandbox verification completed with the existing JDK 21 and Android SDK. No device run. WP-7 through WP-11 remain open. The community/GitHub blocklist question remains open but not relevant to this patch.
Signature: Manus AI — 2026-10-13
