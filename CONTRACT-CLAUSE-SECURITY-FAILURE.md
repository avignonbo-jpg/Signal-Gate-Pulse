# Contract clause: SECURITY_FAILURE (screening-integrity failure)

> **Status: POLICY APPROVED by the owner on 2026-10-05 (ring through).** The clause text is ready to merge into `Architecture-Contract.md`. Sections 6 and 7 list known gaps and proposed follow-ups that are NOT yet approved or implemented. Based on source at commit `8fd768eaeec0fd63d8c849c626ba4e4426624eac` (same code as audited `a3bae8a`). Test evidence is in section 9: a CI run's per-class results (reported by the coding agent, not by me) and one scratch probe run. Items tagged **[CURRENT]** are read from source. Items tagged **[PROPOSED]** are not implemented and are not claims about the code.

## 1. Definition

`SECURITY_FAILURE` means: **Pulse could not produce a trustworthy screening decision for this call.**

It does **not** mean the caller is dangerous, malicious or spam. It is a failure of the screener, not a verdict on the caller. The word "security" in the name reflects the project's security-first rule that a failed check must never silently become a clean ALLOW. The code cannot tell whether a failure was an ordinary bug, a slow disk, or something hostile, and the clause makes no claim either way.

**Naming note [PROPOSED]:** the identifier is retained for compatibility. It appears in the audit table as stored strings (`decision`, `spamStatus`, `notes`), in `ScreeningAction`, `CallTier`, the `ScreeningDecision` init checks, and about a dozen test files. Renaming is a separate refactor and must first decide how already-stored rows keep matching (see §7).

## 2. Policy

When screening fails, **the call rings through** and the failure is recorded. Pulse never blocks, silences, or hides a call because screening failed.

Reason: a failure is unverified evidence. Blocking on it could silence a legitimate call (a doctor, a family member, an emergency) with no recourse. The cost of ringing through is at most one unwanted call.

Telecom meaning **[CURRENT]**, `SignalGateCallScreeningService.responsePolicy()` (`:236`): `disallowCall=false`, `silenceCall=false`, `skipCallLog=false`, `skipNotification=false`. This is identical to ALLOW and SCREEN at the Telecom layer. It is a separate explicit branch only so that it is a visible policy choice and not an accident of aliasing.

## 3. Invariants

| ID | Statement | Enforced at **[CURRENT]** | Tests (see §9 for results) |
|---|---|---|---|
| INV-003 (amended) | A screening failure is a distinct typed state. It is never reported as ALLOW or CLEAN_UNKNOWN, in the Telecom response decision, the domain model, or the audit record. | `ScreeningDecision.kt:46-50` (init checks tie `securityFailure` to both tier and action); `ScreeningDecision.kt:77-80` | `ScreeningDecisionConsequencesTest`, `ScreeningServiceEdgeExecutionTest` |
| INV-011 (proposed) | Every screening failure produces exactly one Telecom response, issued **before** any audit, notification or haptic work, and that response is ring-through. | `handleSecurityFailure` `:276` (respond first); `processScreeningCall` `:177` (respond before persist) | `ScreeningServiceDeadlineTest` (null handle, exception, timeout), `ScreeningServiceCallResponseMappingTest` |
| INV-012 (proposed) | A screening failure always requests an audit record. | `ScreeningDecision.kt:77-80` (`auditRequired = true`); `handleSecurityFailure` `:281-287` | `CallScreeningEngineSecurityFailureTest`, `ScreeningServiceDeadlineTest` |
| INV-013 (proposed) | A screening failure produces no review card, notification or haptic. | `ScreeningDecision.kt:77-80` (`NotificationPolicy.NONE`, `HapticPolicy.NONE`); `dispatchDecisionUx` `:343` (`NONE -> Unit`) | `ScreeningDecisionConsequencesTest` |

## 4. Flow: normal call vs failure

```
Telecom calls onScreenCall(details)
  |
  +-- handle null/blank? ----------------------------> Failure path A
  |
  +-- launch in service scope
        |
        resolve dependencies (engine, repositories, haptics, limiter)   <- NOT inside the 3,500 ms clock
        |
        withTimeout(3,500 ms) { engine.screenCall() }
        |        |                         |
        |        |                         +-- repository throws inside engine ----> Failure path D
        |        +-- clock expires --------------------------------------------> Failure path B
        |
        decision ready -> respond(Telecom response)   <- always first
        -> persist audit/review rows (NonCancellable, 2,000 ms bound)
        -> dispatch notification/haptic (best effort)

  any other exception escaping the guarded block (dependency resolution, response
  mapping) -----------------------------------------------------> Failure path C
```

## 5. Failure scenarios **[CURRENT]**

| # | Trigger | Detected at | What the caller experiences | What is recorded | Pulse UI effects |
|---|---|---|---|---|---|
| A | `Call.Details.handle` is null or blank | Service `:89-93` (log, then `onSecurityFailure("UNKNOWN_MALFORMED_HANDLE")`) | Phone rings normally; call appears in the system call log as usual | Audit row via `handleSecurityFailure`: number `UNKNOWN_MALFORMED_HANDLE`, `decision=SECURITY_FAILURE`, `spamStatus=SECURITY_FAILURE`, `notes=SECURITY_FAILURE`; written after the response, shielded and bounded at 2,000 ms (`:269-309`) | None |
| B | Decision does not finish inside 3,500 ms, **including when the timeout lands while the real engine is suspended in its repository call** (VERIFIED by probe, §9) | `processScreeningCall` `:171` throws `TimeoutCancellationException`; caught in `executeScreeningSafely` `:143-144` | Phone rings normally | Same as A, but with the real phone number | None |
| C | Any other exception escapes the guarded block, for example a dependency fails to resolve or a response cannot be built | `executeScreeningSafely` `:146-147` | Phone rings normally | Same as B | None |
| D | The engine's own lookup fails with an ordinary exception (`repository.getCallDecision` throws something other than a timeout) | `CallScreeningEngine.kt:85-91` catches, returns a `SECURITY_FAILURE` result (`:223-233`) | Phone rings normally. This is the ordinary path with a failure-typed decision, so the response still goes first | Audit row through the normal persist path, as required by the decision (`auditRequired = true`) | None (`NotificationPolicy.NONE`) |

In every scenario the caller's experience is the same as an allowed call. The only trace is the audit row (if the write completes) and the log lines.

## 6. Known gaps (do not describe these as guarantees)

1. **[CURRENT] The first stretch is unbounded.** Dependency resolution runs before the 3,500 ms clock starts. If it hangs, no response is produced by Pulse and none of scenarios A to D triggers. What Telecom then does depends on the platform's own limit, which this audit has not verified. This is policy decision #3 and is not settled by this clause.
2. **[CURRENT] The audit row can be lost.** The write is bounded at 2,000 ms and a timeout is only logged (`AUDIT_PERSIST_TIMEOUT`). `NonCancellable` protects against service teardown, not against the process being killed. The process-kill diagnostic exists in CI but its result is UNKNOWN.
3. **[CURRENT] No user-visible failure signal in code.** Nothing in the paths above shows the user that a screening failure happened. Whether the Recent Calls screen displays `SECURITY_FAILURE` rows, and how, was not checked for this draft.
4. **[VERIFIED at the primitive level] Cancellation handling in the engine.** The engine's `catch (e: Exception)` also catches coroutine cancellation, but a timeout that fires while the real engine is suspended still surfaces as `TimeoutCancellationException` at `withTimeout`, so it is classified as scenario B and the engine's swallowed result is discarded. Evidence: probe 1 in §9. Still **not** verified end to end: the full service wiring (`executeScreeningSafely` + `processScreeningCall`) with a real engine, because probe 2 could not run (see §9).
5. **[CURRENT] Not a threat detector.** Scenarios A to D can be triggered by non-hostile causes. Do not use the count of failure rows as an attack signal.

## 7. Proposed follow-ups (each needs its own approval)

1. **[PROPOSED] Visible failure state.** Show screening failures in Recent Calls with plain wording ("Could not screen this call"), so the user can see ring-through happened because screening failed.
2. **[PROPOSED] Failure counter in Settings or Diagnostics.** A simple count of failures in the last 7 days, to surface a systemic problem.
3. **[PROPOSED] Bound the pre-clock segment** (decision #3), then add it to scenario C.
4. **[PROPOSED] Rename, if wanted.** Do it as one task that changes identifiers only, keeps the stored string values stable (or adds a read-side alias for old rows), and leaves ledger and contract history untouched. CI guardrails currently forbid modifying the `ScreeningDecision.kt` init invariant, so the guardrail wording needs an explicit exception first.

## 8. If the policy changes to "block on failure"

These would have to change together, and the clause would need rewriting:
- `responsePolicy()` mapping for `SECURITY_FAILURE` (`:236`) and the test `ScreeningServiceCallResponseMappingTest` that asserts all four flags are false.
- INV-011 (response is ring-through) and the Reason in §2.
- A recourse path for blocked legitimate calls, since there is currently none for a failure-blocked call (no review card is created for this tier).

## 9. Evidence (as reported to the owner by the coding agent; I did not run any of it)

**Probe 1, plain JVM, VERIFIED-BUILD-OUTPUT.** At commit `8fd768eaeec0fd63d8c849c626ba4e4426624eac`, scratch test `EngineTimeoutJvmProbeTest` (real `CallScreeningEngine`, mocked repository that suspends 10 s, wrapped in `withTimeout(3_500)`): `BUILD SUCCESSFUL`, 1 test, 0 failures, printed `PROBE1_OUTCOME=THROWS_TimeoutCancellationException`. It ran in a separate detached worktree (not the agent's `task1b-federal-rows` checkout), and the agent reports `consumer-v1` currently points to that same SHA. The scratch file was deleted and nothing was committed.

**Existing tests, from CI, VERIFIED-BUILD-OUTPUT.** Pulse Consumer CI run `37296767701` (head branch `consumer-v1`, head SHA `8fd768eaeec0fd63d8c849c626ba4e4426624eac`, per the agent's report of the run record) succeeded, including "Run Unit Tests (Pulse)". Per-class results from its `test-results` artifact:

| Test class | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| ScreeningServiceDeadlineTest | 6 | 0 | 0 | 0 |
| ScreeningServiceCallResponseMappingTest | 1 | 0 | 0 | 0 |
| ScreeningServiceEdgeExecutionTest | 22 | 0 | 0 | 0 |
| ScreeningServiceTimingBudgetTest | 1 | 0 | 0 | 0 |
| CallScreeningEngineSecurityFailureTest | 1 | 0 | 0 | 0 |
| ScreeningDecisionConsequencesTest | 7 | 0 | 0 | 0 |
| **Total** | **38** | **0** | **0** | **0** |

**Not verified:**
- Probe 2 (production wiring with a real engine under a timeout) was not run; local Robolectric execution stalled on a sandbox download. The mechanism it would check is covered by probe 1; the wiring itself is covered only by source reading and by the existing tests, which inject a `screen` lambda instead of the real engine.
- Passing tests show that the tested behavior held in CI; they do not show behavior on a device.
