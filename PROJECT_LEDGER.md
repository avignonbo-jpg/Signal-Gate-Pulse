SignalGate Pulse — Project Ledger
This is the single authoritative log of project state. Every AI session (any model, any tool) reads this file first and appends to it last. This replaces ad hoc tracking across chat history, memory, and ad hoc notes. The current branch-level governance is now anchored to the uploaded audit snapshot `SignalGatePulseAudit_20261025_SOURCE_OF_TRUTH (1).md`.

Current State
Authoritative branch: `consumer-v1`.
Authoritative source-of-truth document: `SignalGatePulseAudit_20261025_SOURCE_OF_TRUTH (1).md`.
Current architecture contract: `Architecture-Contract.md` (final target architecture, adopted by branch governance).

Governance note
The project contract is the binding architecture for all future work. If implementation reality and documentation disagree, the live source must be inspected directly and the discrepancy must be corrected in the contract and the ledger, not silently ignored.

Open Items
[ ] Delete 3 orphaned legacy XML layouts + 26 unused view IDs (call_shield_overlay_enhanced.xml, dialog_add_blocked_number.xml, overlay_call_blocked.xml) — carried over, not yet independently revalidated.
[x] Phase 0 mandatory execution evidence and generated Room schema version 3 — resolved and closed in the Phase 0 ledger entry; retained here only as historical reconciliation.
[ ] NEW: Migrate remaining BlocklistRepository callers (BlockedNumbersViewModel, ContactsViewModel, PendingCardViewModel) directly onto SecurityRuleRepository, per that class's own deprecation note.
[ ] Set up pre-push / CI-side YAML and whitespace validation filters — carried over from prior session, still not done.
[ ] Live call-test READ_PHONE_STATE necessity: confirm on a real device whether anything still needs this permission before removing PhoneStateReceiver, its manifest entries, or correcting the stale analytics/permission logic.
[x] Re-verify and merge the gray-zone notification/haptic implementation — completed and CI-verified in Phase 2.1–2.2; retained here as historical reconciliation.
[x] Resolve PermissionSettingsScreen reachability — resolved by wiring it into `NavGraph.kt` and making it reachable from the dashboard permission-health path.
[x] Resolve the stale `TelemetryViewModel` orphan — resolved by deliberate removal; no live class or Koin binding remains. Historical comments in `RecentCallsViewModel` refer to the former duplication but do not represent active code.
[ ] Fix app/build.gradle doc-comment exportSchema = false claim to match actual exportSchema = true (Contract §9.3)
[ ] Fix ShieldStatusGlow.kt to use .toArgb() instead of .hashCode() (Contract §9.4)
[ ] Extend check-architecture-drift.sh Rule 6 to scan ui/theme/ (Contract §9.6)
[ ] Add a dependency/CVE scan as a hard CI gate (Contract §10.3) — not currently enforced anywhere.
[ ] Build/re-verify test coverage per Contract §10.2 (five-tier matrix, fresh-install migration path, Keystore-invalidation instrumented test)
[ ] Contract audit 10.14 — clarify the Contacts source enablement contract: onboarding offers an optional READ_CONTACTS permission and a skip-import action, but skipping only advances and does not persist contact-source configuration.
[ ] Contract audit 10.15 — confirm whether a 14-day/250-entry retention requirement exists; no such policy was found in current production code or tests.

Removed this session (resolved or found stale)
Confirm which branch is the actual current source of truth on GitHub — resolved, see Current State.
Re-add Apache POI dependency with log4j exclusion — this was never actually needed. Verified this session: DataSyncEngine.kt's XLSX path uses native ZipInputStream + SAX parsing with no Apache POI dependency in the live branch.
Review and approve/reject Architecture-Contract-Amendments.md, then fold approved changes — already done. The Contract document's own closing note said Amendments 0–5 were "applied and approved".
Decide on Security/Ops review of Apache POI removal vs. keeping it — moot, folded into the POI correction above; there is no POI dependency to review.
PermissionSettingsScreen unreachable — resolved by direct source inspection and live routing: `NavGraph.kt` defines `Screen.PermissionSettings.route` and `ConsumerDashboardScreen` navigates there.
TelemetryViewModel orphaned — resolved by deliberate removal; no class or binding exists in the live branch. Remaining references are stale comments only.

Session Log
(Newest entry on top).

2026-10-26 — Correct stale contract statements about PermissionSettingsScreen and TelemetryViewModel
Who: Repository automation / architecture governance.
What: Corrected the two stale factual errors in the active architecture contract and ledger: `PermissionSettingsScreen` is live and routed in `NavGraph.kt`, and `TelemetryViewModel` has been deliberately removed; no live class or Koin binding remains. This preserves the architectural rule that old contradiction entries are corrected when direct source verification proves them false.
Files touched: `Architecture-Contract.md`; `PROJECT_LEDGER.md`.
Layers touched: architecture governance and project ledger only.
Contract consulted: yes — this session verifies the live branch state before correcting the prior stale entries.
Validation: Reviewed the live files directly (`NavGraph.kt`, `PermissionSettingsScreen.kt`, `RecentCallsViewModel.kt`, and the repo search results) and confirmed the stale issues were documentation drift, not active code issues.
Status: The contract and ledger now reflect the live branch reality.
Signature: Repository automation / architecture governance — 2026-10-26

2026-10-25 — Branch governance rollover to the uploaded audit snapshot as the controlling source-of-truth
Who: Repository automation / branch governance admin.
What: Adopted `SignalGatePulseAudit_20261025_SOURCE_OF_TRUTH (1).md` as the current governing source-of-truth document for `consumer-v1`, and updated the branch governance docs to mark `Source-Of-Truth.md` as historical rather than active.
Files touched: `SignalGatePulseAudit_20261025_SOURCE_OF_TRUTH (1).md`; `Source-Of-Truth.md`; `PROJECT_LEDGER.md`; `README.md`.
Layers touched: governance, documentation, and project ledger only.
Contract consulted: no code or architecture change; branch governance and the current `consumer-v1` file set were checked against the uploaded audit snapshot before the supersession notice was applied.
Validation: Reviewed the updated governance docs and ledger references; no application behavior or source code was changed.
Status: Active branch governance now points to the uploaded 2026-10-25 snapshot as the authoritative source-of-truth document.
Signature: Repository automation / branch governance admin — 2026-10-25
