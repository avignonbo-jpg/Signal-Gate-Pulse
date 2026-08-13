# SignalGate Pulse — Project Ledger

**This is the single authoritative log of project state.** Every AI session (any model, any tool) reads this file first and appends to it last. This replaces ad hoc tracking across chat history, memory, and scattered procedure docs — if it isn't in here, it didn't happen as far as the project is concerned.

Governing document: `Architecture-Contract.md` (v2 — Reconciled). This is now the **only** Architecture Contract for this project — see that document's top-of-file reconciliation note for why a second, competing lineage previously existed and why it's superseded.

---

## How to use this file (read this first, every session)

**At the start of any session working on this codebase:**
1. Read the "Current State" section below before writing or suggesting any code.
2. Read "Open Items" — do not re-solve something already listed as decided.
3. If asked to make an architectural change (new class, new layer assignment, new dependency), check it against `Architecture-Contract.md` before proceeding. If it conflicts, say so explicitly and stop — do not silently comply or silently ignore the contract.

**At the end of any session (before ending the conversation):**
1. Append a new entry to "Session Log" — even a short one. Include: date, what changed, which files, which layer(s) touched, and whether the Architecture Contract was consulted.
2. Update "Current State" if the change affects it (new branch as source of truth, new known-broken state, new dependency, etc.).
3. Move anything resolved out of "Open Items" into the session log entry; add anything newly discovered into "Open Items."

**Never silently skip this.** If the person doesn't ask for a ledger update, do it anyway before the conversation ends — this is a standing instruction, not a one-time request.

---

## Current State

- **Authoritative branch:** `consumer-v1` — confirmed this session: the uploaded `Signal-Gate-Pulse-consumer-v1` zip is byte-identical to the snapshot `SignalGate-Pulse-Source-of-Truth-Audit.md` was written against. No further reconciliation against divergent branches (`pulse_zip`, `patch-N`, etc.) is outstanding as of this entry — treat drift concerns from the 2026-07-15 branch-reconciliation entry as resolved unless new divergence is observed.
- **Last known-good build:** not yet confirmed green in this session — CI status wasn't independently re-run, only source was statically verified. Do not treat this entry as a CI confirmation.
- **minSdkVersion:** 29 — confirmed present in actual `build.gradle` this session (matches the 2026-07-15 entry's fix).
- **Known broken/incomplete:**
  - `HEURISTIC_FLAG` (gray-zone) calls never produce a `PendingCardEntity`, despite `CallScreeningEngine.kt`'s own doc comment promising digest review for every gray-zone call. See Architecture Contract §9.5. This is newly discovered this session, not previously tracked anywhere.
  - `check-architecture-drift.sh` Rule 6 doesn't scan `ui/theme/`, so the Cross-Cutting purity claim for `Color`/`Theme`/`SignalGateTheme`/`Effects` is currently unenforced. See Contract §9.6.
  - A gray-zone notification/haptic/rate-limiter feature (`PulseHapticsController`, `PulseVibration`, `PulseTriggerLimiter`, extended `CallActionReceiver` actions) was built in a prior session but **never merged**, and is **held** pending the §9.5 fix — see Contract §10.1. Do not treat its existence as authorization to continue building on top of it.
- **Architecture Contract version:** v2 — Reconciled (this entry). Amendments 0–5 are **applied**, not pending — see correction below; the Amendments Log in this file previously said otherwise and was wrong.

---

## Open Items

- [ ] Delete 3 orphaned legacy XML layouts + 26 unused view IDs (`call_shield_overlay_enhanced.xml`, `dialog_add_blocked_number.xml`, `overlay_call_blocked.xml`) — carried over, not yet independently re-verified this session
- [ ] Set up pre-push / CI-side YAML and whitespace validation filters — carried over from prior session, still not done
- [ ] `SecurityUtilsTest.kt` is a placeholder stub, not real test code — needs actual implementation or removal (see Contract §10.2)
- [ ] Live call-test `READ_PHONE_STATE` necessity: confirm on a real device whether anything still needs this permission before removing `PhoneStateReceiver`, its manifest entries, or correcting the stale onboarding description — see TODO(call-testing) in `OnboardingViewModel.kt`. **Do not delete `PhoneStateReceiver` based on reference-search evidence alone — see the 2026-07-15 incident below.**
- [ ] Fix `CallScreeningEngine`/`SignalGateCallScreeningService` so `HEURISTIC_FLAG` calls produce a `PendingCardEntity` (Contract §9.5) — this is now the top-priority code item; it blocks the held gray-zone notification/haptic work (§10.1) and should be fixed before that work resumes
- [ ] Resolve `PermissionSettingsScreen` unreachability (Contract §9.1) — wire into `Screen.kt`/`NavGraph.kt` or delete
- [ ] Resolve orphaned `TelemetryViewModel` (Contract §9.2) — wire into `CallLogScreen` or delete class + Koin binding
- [ ] Fix `app/build.gradle` doc-comment `exportSchema = false` claim to match actual `exportSchema = true` (Contract §9.3)
- [ ] Fix `ShieldStatusGlow.kt` to use `.toArgb()` instead of `.hashCode()` (Contract §9.4)
- [ ] Extend `check-architecture-drift.sh` Rule 6 to scan `ui/theme/` (Contract §9.6)
- [ ] Add a dependency/CVE scan as a hard CI gate (Contract §10.3) — not currently enforced anywhere
- [ ] Build/re-verify test coverage per Contract §10.2 (five-tier matrix, fresh-install migration path, Keystore-invalidation instrumented test)

### Removed this session (resolved or found stale — see correction notes)
- ~~Confirm which branch is the actual current source of truth on GitHub~~ — resolved, see Current State
- ~~Re-add Apache POI dependency with `log4j` exclusion~~ — **this was never actually needed.** Verified this session: `DataSyncEngine.kt`'s XLSX path uses native `ZipInputStream` + SAX parsing with no Apache POI dependency, and no POI import exists anywhere in the current source. This was a deliberate fix (avoiding POI's `MethodHandle`/D8 incompatibility below API 26) already in place, not an unfinished migration. The prior Open Item description was stale — it described the pre-fix state as if still current.
- ~~Review and approve/reject `Architecture-Contract-Amendments.md`, then fold approved changes~~ — **already done.** The Contract document's own closing note said Amendments 0–5 were "applied and approved" as of 2026-07-15, but this file's Open Items and Amendments Log both still said "pending." That was an internal inconsistency between this file and the Contract, not an actual open task. See Amendments Log correction below.
- ~~Decide on Security/Ops review of Apache POI removal vs. keeping it~~ — moot, folded into the POI correction above; there is no POI dependency to review.

---

## Session Log

*(Newest entry on top.)*

### 2026-08-13 — Layer 2 "root of trust" callout added, following Physical-vs-Platform/Edge discussion
- **Who:** Claude (Sonnet)
- **What:** Person asked for a deeper comparison of Track 1's "Physical" layer versus this contract's "Platform/Edge" layer, and which framing is better. Established the two schemes don't disagree everywhere — the real localized disagreements are (a) where bootstrap/receivers/workers sit (Track 1: mid-stack "Session"; this contract: bottom, "Platform/Edge") and (b) where decision engines sit (Track 1: top, "Application"; this contract: middle, "Domain"). Assessed Track 1's root-of-trust framing for the Keystore/passphrase material as more honest to what OSI numbering is supposed to convey, but recommended against re-flipping this contract's numbering — that would recreate the exact Amendment-0 problem (CI labels drifting from contract prose) the prior reconciliation fixed. Person agreed with the smaller fix: made `SecureDatabase`/`SecurityUtils` (the Keystore-wrapped SQLCipher passphrase) an explicit, visually distinct "root of trust" sub-item within Layer 2, rather than one bullet of equal weight among six. No layer reassignment, no renumbering — purely a documentation-weight fix within the existing Layer 2 scope.
- **Files touched:** `Architecture-Contract.md` (§4 Layer 2 restructured with root-of-trust callout; reconciliation note updated)
- **Contract consulted:** yes — this session's purpose was a Contract content discussion
- **Follow-up needed:** none — this was a documentation-emphasis fix, not a code or enforcement change; `check-architecture-drift.sh` needs no update since Rule 3 (Layer 2 → Room import ban) already covers `SecureDatabase`/`SecurityUtils` at the file level, and file-level enforcement doesn't distinguish "root of trust" from other Layer 2 members

### 2026-08-13 — Governance reconciliation: two Architecture Contract lineages merged into one
- **Who:** Claude (Sonnet)
- **What:** Discovered this project had two independently-maintained Architecture Contract lineages in simultaneous use — this one (Perplexity-originated, CI-enforced via `check-architecture-drift.sh`, Layer 7=UI...Layer 1=Platform-Edge numbering) and a separate one produced in a different session (`SignalGate-Pulse-Architecture-Contract.md`, prose-only/no CI enforcement, Layer 7=Application...Layer 1=Physical numbering — opposite direction, same underlying app). Adopted this lineage's numbering as canonical, since it's the only one an actual machine check enforces. Merged in every class the other lineage's audit had confirmed exists but this map was missing: `PendingCardDao`, `PendingCardEntity`, `DigestScreen`, `PendingCardViewModel`, `NotificationChannelManager`, `KeystoreInvalidatedException`, `DatabaseResetEvent`, `SyncBootReceiver`. Added a new §9 "Known Violations" section (this lineage had none) carrying forward the other lineage's tracked findings (`PermissionSettingsScreen` unreachable, orphaned `TelemetryViewModel`, `exportSchema` doc mismatch, `ShieldStatusGlow` `.hashCode()` bug) plus one new finding from this session: `CallScreeningEngine`'s own doc comment promises gray-zone calls get digest review, but the actual write path only creates a `PendingCardEntity` for `HEURISTIC_BLOCK` — gray-zone calls never reach the digest despite the domain layer's own documented contract. Also found `check-architecture-drift.sh` Rule 6 doesn't scan `ui/theme/` despite the Contract claiming those classes as cross-cutting-pure. Corrected this file's Open Items: removed a stale Apache POI re-add task (verified current `DataSyncEngine.kt` already uses native SAX parsing, no POI dependency exists), and corrected the Amendments Log below, which still said "Pending review" for five amendments the Contract's own closing note already called "applied and approved" — that was this file drifting from the Contract, not an actual open task. Added a new §10 "Feature Completion & Test Coverage" section to the Contract (this lineage had no phased roadmap equivalent), scoping the previously-built (unmerged, held) gray-zone notification/haptic/rate-limiter feature explicitly behind the newly-found digest gap fix.
- **Files touched:** `Architecture-Contract.md` (v2, reconciled — full rewrite of §4 class map with `[+]` markers, new §9, new §10), `PROJECT_LEDGER.md` (this entry; Open Items and Amendments Log corrected)
- **Contract consulted:** yes — this session's entire purpose was reconciling the contract against a second lineage and against verified source
- **Follow-up needed:** the §9.5 digest gap is now the top-priority code fix — it blocks the held gray-zone feature from resuming per §10.1. `check-architecture-drift.sh` itself was not yet edited to fix the Rule 6 gap (§9.6) — that's a follow-up, not done in this pass.

### 2026-07-15 — Amendments 1–5 applied to Architecture-Contract.md
- **Who:** Claude (Sonnet)
- **What:** Person reviewed the 5 proposed amendments and approved all, on the condition that each be independently checked against "would I make this change on my own project" rather than approved by default. On review: Amendments 0, 1, 2, 3, 4 approved without reservation. Amendment 5 (Build Integrity, Section 8) approved with one flagged caveat — it governs build/dependency config rather than code architecture, and is slightly outside this contract's core OSI-layer scope; kept in this document for now since it directly addresses two real build failures from this project, but noted as a candidate to split into a separate `BUILD-GOVERNANCE.md` later. Applied to `Architecture-Contract.md`: added explicit XML-layout rule to Section 1, added `Cross-Cutting` row to the Section 3 layer table, added `AppModule` to Layer 1, moved `DataSyncEngine`/`ReliableSourceManager` from Layer 4 to a newly-created Layer 5 section, added `CallInfo` to Layer 4, added the full `Cross-Cutting (no layer ownership)` subsection with its class list, added Section 8 (Build Integrity). Verified post-edit: each class appears exactly once across the document, all 8 sections present and in order.
- **Files touched:** `Architecture-Contract.md`
- **Contract consulted:** yes — this session's entire purpose was amending the contract itself
- **Follow-up needed:** *(2026-08-13 correction: this was marked "none currently open" originally, but this file's own Open Items and Amendments Log continued to list these amendments as pending for the next four weeks of entries — a real process failure, not a follow-up that was simply never logged. See 2026-08-13 entry.)*

### 2026-07-15 — Architecture-Contract.md created as an actual file; SESSION-START-BLOCK.md strengthened
- **Who:** Claude (Sonnet)
- **What:** Discovered `Architecture-Contract.md` had never actually existed as a file — it only ever existed as text pasted into a conversation. Created it now, verbatim from the pasted content (one truncated closing sentence completed by inference — "...persistence stores, and security enforces" — flagged to the person for verification against their original source). Added two hardening rules to `SESSION-START-BLOCK.md`: (1) `Architecture-Contract.md` must be read in full before any work of any kind on this project, not just architecture-relevant work; (2) before deleting/replacing/rewriting any existing file, its full contents (including comments/TODOs) must be viewed first — grep/reference-search evidence alone is insufficient, per the `PhoneStateReceiver` incident logged in the prior entry.
- **Files touched:** `Architecture-Contract.md` (created), `SESSION-START-BLOCK.md` (strengthened)
- **Contract consulted:** n/a — this session's work was creating/hardening the governance documents themselves
- **Follow-up needed:** person should verify the completed closing sentence of Section 7 against their original contract source if they have it

### 2026-07-15 — PhoneStateReceiver: incorrect deletion, corrected, call-test flag added
- **Who:** Claude (Sonnet)
- **What:** Asked whether `CallActionReceiver`, `PhoneStateReceiver`, `KoinWorkerFactory`, `SecurityUtils` belonged to a different app (Multi-Path) and should be removed from Pulse. Cross-referenced manifest registrations and call sites: `CallActionReceiver`, `KoinWorkerFactory`, `SecurityUtils` are all actively wired into Pulse and were kept. `PhoneStateReceiver` had zero code references anywhere, so it was deleted — along with its manifest `<receiver>` block, the `READ_PHONE_STATE` permission, and the corresponding `OnboardingViewModel` permission-list entry — **without first reading the file's own header comment**, which explained the receiver is a deliberate, documented no-op ("landmine-defusal" for a dead `PostCallNotifier`/`SharedPreferences` bridge) and explicitly asked that its manifest presence be a deliberate human review decision, not a side effect of dead-code cleanup. Person caught this and asked to keep it. All three changes were reverted byte-for-byte against the pristine pre-edit source (verified via diff). One addition retained: a `TODO(call-testing)` comment added to the `READ_PHONE_STATE` entry in `OnboardingViewModel.kt`, flagging that its onboarding description describes the old pre-`CallScreeningService` detection mechanism and that live call testing is needed before this permission or receiver is touched again.
- **Files touched:** `PhoneStateReceiver.kt` (deleted, then restored — net: unchanged), `AndroidManifest.xml` (edited, then restored — net: unchanged), `OnboardingViewModel.kt` (net change: one TODO comment added, no logic changed)
- **Contract consulted:** no — this was a dead-code judgment call, not a layer-ownership question; in hindsight, reading the target file's own comments in full before removal should be treated as mandatory practice, not optional
- **Follow-up needed:** live call-test `READ_PHONE_STATE` necessity before any future removal attempt (see Open Items)

### 2026-07-15 — Full codebase audit + Architecture Contract review
- **Who:** Claude (Sonnet)
- **What:** Produced full source-of-truth documentation of the reconciled codebase (67 Kotlin files catalogued, all build.gradle dependencies listed, all 3 CI workflows documented, nav graph + resource ID cross-check performed). Reviewed the Architecture Contract handed off from a prior Perplexity session against actual code; found Layer 5 (Application) had zero assigned classes despite being defined, and two classes (`DataSyncEngine`, `ReliableSourceManager`) were misfiled under Layer 4. Drafted amendments (see `Architecture-Contract-Amendments.md`).
- **Files touched:** none (documentation/audit only, no source changes)
- **Contract consulted:** yes — this session's entire purpose was reconciling the contract against reality
- **Follow-up needed:** amendments need human review before being folded into the canonical contract; orphaned XML layouts identified but not yet deleted

### 2026-07-15 — Branch reconciliation + minSdk fix
- **Who:** Claude (Sonnet)
- **What:** Traced a `csvParser.parse()` compile error to a real API mismatch between `DataSyncEngine.kt` and `SecureCsvParser`. Discovered extensive branch divergence across `consumer-v1`, `consumer-v1_rename-v1`, `pulse_zip`, and several `patch-N` branches (likely caused by a GitHub ruleset silently renaming rejected pushes). Found `SourcesViewModel.kt` and other Phase 4.x work existed in local zip exports but had never been committed to any branch. Reconciled two divergent lines of work (Phase-tracked line + independent XLSX-streaming-fix line) into one merged snapshot. Raised `minSdkVersion` from 24 to 29 to resolve an Apache POI/D8 dexing incompatibility (`MethodHandle.invoke` unsupported below API 26).
- **Files touched:** `DataSyncEngine.kt`, `MainApplication.kt`, `build.gradle` (project-level), `SignalGateCallScreeningService.kt`, `CommunitySyncWorker.kt`, plus 2 test files added (`MigrationTest.kt`, `KoinModuleTest.kt`)
- **Contract consulted:** no — contract wasn't available yet at time of this work
- **Follow-up needed:** push reconciled state to actual `consumer-v1`, confirm CI passes
- **2026-08-13 correction note:** the *symptom* this entry fixed (POI/D8 incompatibility) was later resolved a second, more durable way — replacing POI with native SAX parsing entirely, per the 2026-08-13 entry above. `minSdkVersion` 29 remains correct and unrelated to that later fix; it's a separate, still-valid constraint (`RoleManager.ROLE_CALL_SCREENING` itself requires API 29+).

---

## Amendments Log

| Date proposed | Amendment | Status |
|---|---|---|
| 2026-07-15 | Relabel `check-architecture-drift.sh` to match this contract's layer numbering | **Applied** *(corrected 2026-08-13 — previously shown as "Pending review," which contradicted this Contract's own closing note)* |
| 2026-07-15 | Reassign `DataSyncEngine`, `ReliableSourceManager` from Layer 4 → Layer 5 | **Applied** *(corrected 2026-08-13)* |
| 2026-07-15 | Add missing classes to ownership map (`AppModule`, `CallInfo`, etc.) | **Applied** *(corrected 2026-08-13 — further classes added on top this session, see §4 `[+]` markers in the Contract)* |
| 2026-07-15 | Add "Cross-Cutting" category for data models/utils | **Applied** *(corrected 2026-08-13)* |
| 2026-07-15 | Close XML-layout loophole explicitly | **Applied** *(corrected 2026-08-13)* |
| 2026-07-15 | Add Build Integrity clause (Section 8) | **Applied**, scope caveat still standing *(corrected 2026-08-13)* |
