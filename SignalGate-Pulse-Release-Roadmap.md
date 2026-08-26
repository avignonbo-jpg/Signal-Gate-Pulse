# SignalGate Pulse — Production-Readiness & Forward Roadmap

**Governing document:** `Architecture-Contract.md` (v4). Every step below either closes a numbered item in that contract or is explicitly marked as new, product-facing scope outside it. If any step here conflicts with the contract, the contract wins and must be amended first (§13).

**Framing note, carried over from the product's own README:** Pulse sells "set-and-forget" peace of mind, not configurability. Nothing in this roadmap should add user-facing complexity (settings, toggles, manual rules) unless it removes more friction than it adds. That constraint applies to Part 1 and Part 2 alike.

**Explicit scope boundary (per this session's instruction):** the *practical real-world accuracy* of the blocking mechanism itself — i.e., whether FTC/FCC datasets and the current heuristic actually catch real spam — is known to be weak and is **deliberately out of scope for Part 1**. Part 1 is infrastructure, security, and release hardening only. Detection-quality work is the headline of Part 2.

---

## Part 1 — Path to v1.0 (Production-Ready Release)

This part closes every remaining open item in Architecture-Contract.md v4 §10 and §11, in dependency order. Each step names its governing contract section so it's traceable.

### Step 1 — Close Phase 0.8 (mandatory test-gate completeness)

Phase 0's own exit condition. Nothing below should be considered "final" until this is done, because it's the thing that would catch a regression in everything else.

1.1. Enumerate every test file in `android/app/src/test` and `android/app/src/androidTest` against every Phase 0 sub-item (0.1–0.7) and confirm each has direct test coverage, not just adjacent coverage. Produce a simple traceability table (test file → contract item) and commit it to `PROJECT_LEDGER.md`.
1.2. Confirm `pulse-instrumented-tests.yml` and `pulse-ci.yml` (JVM tests) are both `push`/`pull_request` triggered on the target branch with no `continue-on-error: true` on any Phase 0–1 test job. If JVM tests still run advisory (per INV-010's noted open status), remove `continue-on-error` and fix whatever it was masking rather than leaving it soft.
1.3. Re-run both workflows on a clean commit and attach the resulting JUnit XML (not just a "green checkmark" screenshot) to the ledger entry that closes 0.8, the same way the 0.2 closure in this session was evidenced.
1.4. Close Phase 0.8 in the contract only after 1.1–1.3 are all done and evidenced.

### Step 2 — Confirm Phase 1 remaining items

Per `PROJECT_LEDGER.md`, 1.1 and 1.2 are CI-verified closed; 1.3 (gray-zone persistence) was implemented and is marked resolved in the contract (§10.5) but its own ledger entry ends mid-verification. Re-confirm before treating Phase 1 as fully closed:

2.1. Re-run `GrayZoneReviewabilityTest` in isolation and record the pass count in the ledger, closing the loop the 2026-08-19 ledger entry left open.
2.2. Confirm no other Phase 1 sub-item was silently left unstated — re-read the full Phase 1 list in the contract and check each against the ledger, not against memory of what "felt done."

### Step 3 — Resolve remaining Layer/orphan violations (Contract §10.1, 10.2, 10.3, 10.4, 10.13)

All four are small, independent, and safe to parallelize — none touch the security control plane.

3.1. **§10.1 — `PermissionSettingsScreen`.** Decide product intent first (this is a product decision, not just a wiring fix): is a standalone "Permission Health Check" screen still wanted, given Pulse's onboarding already walks the user through permissions once? If yes: add a `Screen.PermissionHealth` route, wire it into `NavGraph.kt`, and give it a real entry point (most natural: a card/link from `SettingsScreen` or `ConsumerDashboardScreen` when a required permission or role has been revoked post-onboarding). If no: delete the file and its Koin/onboarding references cleanly — don't leave a dead screen "just in case."
3.2. **§10.2 — `TelemetryViewModel`.** It duplicates `RecentCallsViewModel`'s job (both transform `CallLogRepository` data). Pick one: either merge `TelemetryViewModel`'s transformation logic into `RecentCallsViewModel` and delete the orphan, or, if there's a real distinct future consumer in mind (e.g., a richer analytics/telemetry surface), wire it to that screen now rather than leaving it speculative. Update the ViewModel-per-screen rule compliance note in the contract once resolved.
3.3. **§10.3 — build.gradle KSP comment drift** (`exportSchema = false` comment vs. actual `exportSchema = true`). Fix the comment to match reality, and while there, confirm the Room schema directory (`android/app/schemas`) is actually being generated and committed/uploaded — a schema export that's silently wrong is worse than one that's silently absent, because it looks safe.
3.4. **§10.4 — `ShieldStatusGlow` color conversion.** Replace `Color.hashCode()` with `.toArgb()` for the native `Paint` color. Add a one-line regression note (not a full test) since this is a rendering correctness fix, not a security one.
3.5. **§10.13 — `AppModule.kt` OSI-comment conflict.** Rewrite the `engineModule`/`repositoryModule` doc comments to reference Layer 1–7 by name (per contract §3) instead of the invented "L2/L4/L6" literal-OSI labels. This is a pure documentation fix — confirm no binding order or dependency actually changes, just the comment text.

### Step 4 — Phase 4 remainder (UI/onboarding completion)

Per contract §11 Phase 4, beyond the items already folded into Step 3:

4.1. Move EULA persistence out of Compose and behind `OnboardingViewModel`/an application boundary, per §4.2. Confirm this doesn't currently write to SharedPreferences or a Compose-local store directly.
4.2. Move Contacts Provider access (`ContactsViewModel`'s direct `ContactsContract`/`ContentResolver`/`Cursor` usage) behind a repository boundary, per §4.3 and the Layer 6 note in the contract's class ownership map. This is the one remaining direct-platform-access violation in Presentation layer.
4.3. Confirm no stale, unreferenced XML resources remain under `res/layout/` (contract §1's "no grandfathered exceptions" rule) — a `grep`-based sweep is sufficient; delete anything with zero references confirmed via full-file inspection, not just filename pattern matching.

### Step 5 — Real-device startup validation (new item, surfaced by `StartupDiagnostics` work)

Per the 2026-08-22 ledger entry, `StartupDiagnostics` instrumentation exists but has **no measured real-device baseline yet** — this is a genuine open gap for a call-screening app, where `CallScreeningService.onScreenCall()` has a real OS-enforced response window.

5.1. Install a release-config build on at least one representative low/mid-tier real device (not just the CI emulator) and capture:
   - Fresh-install cold start (Activity path) — every `StartupDiagnostics` checkpoint from `APPLICATION_ON_CREATE_BEGIN` to `ACTIVITY_FIRST_FRAME`.
   - Existing-database cold start (Activity path) — same checkpoints, warm database.
   - Cold-process, Telecom-triggered screening path (kill the app, place a call) — `SCREENING_SERVICE_ON_CREATE` through `SCREENING_DECISION_BEGIN`, since this is the path with an actual OS timeout risk, not just a UX-perceived one.
5.2. Classify every checkpoint gap as mandatory (cannot move/parallelize without correctness risk — e.g., Keystore init before SQLCipher open), deferrable (already deferred, e.g. Bloom rehydration — confirm it's actually not on the hot path in the real trace), parallelizable, cacheable, or a sequencing artifact worth removing.
5.3. If the Telecom-triggered cold-start path is close to or exceeds Android's screening response budget on a representative low-tier device, that is a release blocker, not a performance nice-to-have — file it as its own contract item before Phase 6 sign-off.

### Step 6 — Phase 5 (Security test and CI gate hardening)

6.1. Make unit tests fully mandatory in CI (should already be closed by Step 1.2 above — confirm here rather than duplicating work).
6.2. Add dependency/CVE scanning (e.g., Dependabot or OWASP Dependency-Check via a Gradle plugin) with an explicit severity policy: block release on any unpatched Critical/High in a direct dependency; document any accepted exception with an owner and a re-review date, per §12's Definition of Done.
6.3. Add secret scanning (e.g., gitleaks or GitHub's native secret scanning) as a required check. Confirm no credentials or API keys (including the FTC API key referenced in `ReliableSourceManager`'s comments — confirm it's CI-secret-only, never in source) are embedded in the built artifact, not just the repo.
6.4. Expand `check-architecture-drift.sh` to cover the still-open rules 8–10 noted in contract §9 (no direct DAO access from Platform/Edge as a standing grep rule, not just a fixed-at-one-point-in-time fact; `ui/theme/` cross-cutting purity scanning per §10.6).
6.5. Apply least-privilege GitHub Actions permissions (`permissions:` block per workflow, not repo-default) and pin third-party actions (e.g., `reactivecircus/android-emulator-runner`) to immutable commit SHAs rather than floating tags, where practical.

### Step 7 — Phase 6 (Release hardening and provenance)

7.1. Replace the current broad R8 keep rules (`-keep class com.signalgate.multipoint.** { *; }` plus stale class-name keeps, per contract §8) with narrowly scoped, individually justified rules. Prove the minified release build still correctly starts Koin, Room, WorkManager, Navigation, and `CallScreeningService` — this needs a real device/emulator run of the *release* variant, not just debug.
7.2. Build a signed release candidate using CI-held signing credentials only. Confirm the build fails closed (does not silently fall back to debug signing or skip signing) if credentials are absent.
7.3. Generate an SBOM and artifact checksums; retain build provenance tied to the exact source commit and workflow run ID.
7.4. Run release-build instrumentation and a manifest/exported-component review — confirm no component is unintentionally exported, and that `CallScreeningService`'s exported status matches what `ROLE_CALL_SCREENING` actually requires, nothing broader.
7.5. Verify Android Auto Backup exclusions correctly cover the encrypted database file and any Keystore-adjacent preference material, so a device restore can never reconstitute the passphrase-adjacent state insecurely.
7.6. Do a final privacy pass over Logcat output, notifications, and any crash-diagnostic upload path (`crash-diagnostic.yml` workflow) — confirm none of them can leak a phone number or call metadata even in a debug-triggered crash dump.

### Step 8 — Phase 7 gate (go/no-go)

Re-run the existing Release Candidate Gate checklist in the contract (§11 Phase 7) top to bottom once Steps 1–7 are done. Every currently-`[ ]` item should become `[x]` with a cited evidence source (CI run URL, ledger entry, or test artifact) — no item should flip to `[x]` on the strength of "it should be fine now."

---

## Part 2 — Post-v1 Roadmap (Future Releases)

Ordered by dependency and by how directly each addresses the acknowledged gap: **the current blocking mechanism is not yet practically effective.** v1.1 exists specifically to fix that; everything after it builds on a v1.0 that is secure and stable but not yet good at its actual job.

### v1.1 — Real-world detection efficacy (the actual point of the app)

This release should be scoped and planned separately, with its own contract addendum, before implementation starts — it changes Layer 4/5 behavior, not just infrastructure, and must be re-checked against INV-001 through INV-006 as it lands.

- Replace or supplement the FTC/FCC static datasets with a real-time-capable spam-reputation signal (carrier-provided reputation API, a maintained community-reported-spam feed, or both). Any new source must go through the existing `SourceLifecycleState`/`replaceSourceSnapshot()` machinery — do not build a second ingestion path.
- Implement genuine STIR/SHAKEN attestation reading from `Call.Details` rather than the current advisory-only `CallRiskEvaluator` scaffolding, if device/carrier support allows it — verify current Android API surface for this before committing to scope.
- Consider a lightweight on-device scoring model (not a network-dependent one, to preserve the "always-on, invisible" positioning) that combines attestation, source-match count, and call pattern (time of day, repeat-caller behavior) — this is additive to, not a replacement for, the existing deterministic tier system, and must remain advisory input only, per the existing L6 constraint already enforced for `CallRiskEvaluator`.
- Add a crowdsourced "mark as spam"/"not spam" feedback loop that feeds back into MANUAL-equivalent local state first, and only into any shared community list with explicit user consent and clear data-handling disclosure.
- Re-run the full Phase 0/1 test suite against the new decision paths before shipping — a detection-quality change is exactly the kind of change most likely to reintroduce an INV-001/INV-005 violation if it's rushed.

### v1.2 — Product depth within the "set-and-forget" constraint

- Digest quality-of-life: trends over time ("spam calls blocked this month"), not just a raw pending-card queue — read-only, no new manual-rule surface.
- Optional richer per-call detail (approximate area/carrier-level origin, if obtainable without a live-location-adjacent lookup) surfaced only in the digest, never in the notification itself, preserving the existing lock-screen privacy policy (INV-007).
- Revisit whether a narrow, opt-in "trusted contacts beyond phone contacts" mechanism (e.g., verified business callers) belongs in MANUAL, without reopening the removed general-purpose custom-source flow — the product decision documented this session (fixed MANUAL/FTC/FCC source model) should be treated as a real constraint, not just an implementation shortcut, unless a new product review explicitly revisits it.

### v1.3 — Platform and ecosystem

- Multi-device / account-linked sync of MANUAL rules (allow/block list, not call history) across a user's devices, if SignalGate's broader product suite calls for it — this is a genuinely new trust boundary (a remote sync endpoint) and needs its own security-boundary section added to the contract before implementation, not retrofitted after.
- Evaluate a companion Wear OS glance/notification surface for the digest — read-only, no new decision surface on a less-secure companion device.
- Revisit whether Pulse should consume other SignalGate-suite signals (per the README's positioning as one mode of a broader SignalGate product) — scope this as an explicit cross-product architecture review, not an incremental addition to this contract.

### Ongoing / every release — hardening cadence

These are recurring, not one-time, and should be scheduled rather than left to "whenever":

- **Dependency updates:** review and bump Room, Compose BOM, Koin, SQLCipher, and OkHttp on a fixed cadence (e.g., monthly), not only when CVE-scanning flags something.
- **CVE/secret scanning:** already mandatory per Part 1 Step 6 — keep it mandatory, don't let it regress to advisory under release-deadline pressure.
- **Privacy review:** re-run the Logcat/notification/crash-diagnostic review (Part 1 Step 7.6) any time a new logging statement, notification channel, or diagnostic workflow is added — this is a fast, cheap check that catches PII leaks before they ship, and skipping it is how INV-007 regresses.
- **Architecture Contract re-validation:** any session that touches Layer 4/5 (domain or application boundary) code must re-check against this contract before merging, per the contract's own §8/§13 — and if reality and the contract disagree, the discrepancy gets recorded and reconciled the way this session did, not silently resolved in either direction.
- **R8/release-config drift check:** re-verify narrowly-scoped keep rules haven't crept back toward a broad wildcard after a dependency bump forces a new keep rule — a one-off "just add `-keep class X.** { *; }` to make the crash go away" is exactly how Contract §8's current violation happened in the first place.
