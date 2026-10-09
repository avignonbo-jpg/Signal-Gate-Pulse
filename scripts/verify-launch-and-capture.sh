#!/bin/bash
##############################################################################
# SignalGate Pulse — Emulator Launch Verification + Crash Log Capture
#
# Used by .github/workflows/crash-diagnostic.yml's "Run Emulator and Capture
# Crash Log" step.
#
# WHY THIS IS A SEPARATE SCRIPT, NOT INLINE IN THE WORKFLOW YAML:
# reactivecircus/android-emulator-runner@v2 executes each line of a multi-line
# `script:` input as its own independent `sh -c` invocation — there is no
# shared shell process and no shared variable state across lines. This broke
# two things when written inline:
#   1. A `for ... do ... done` loop split across lines fails to parse at all
#      ("Syntax error: end of file unexpected (expecting done)"), because each
#      line is handed to `sh -c` as if it were a complete, standalone script.
#   2. `LOGCAT_PID=$!` set on one line was never visible to `kill $LOGCAT_PID`
#      on a later line — each was a fresh process, so the variable was simply
#      unset by the time `kill` ran. Swallowed by `2>/dev/null || true`, so it
#      looked harmless, but the intended kill never actually happened.
# Fix for both: run the whole flow — starting logcat, launching the app,
# polling, and stopping logcat — inside ONE script invocation (one line in the
# YAML), so control flow and variables behave like a normal shell script.
#
# CALL-SCREENING VERIFICATION (added):
# Headless CI has no way to tap through the system ROLE_CALL_SCREENING
# picker dialog, so the role is granted directly via the `cmd role` shell
# command before any call is simulated. Two calls are then placed via the
# emulator console (`adb emu gsm call`), with a gap between them, to check
# whether onScreenCall fires for both — this was added to investigate a
# "screening works for one call, then silently stops" regression seen on a
# physical test device. Note: this AVD has no OEM battery manager, so it
# cannot reproduce an OEM background-kill cause even if that turns out to
# be the real explanation on the physical device — a pass here only rules
# out an app/Telecom-emulator-level cause, it doesn't confirm the OEM
# battery theory either way.
#
# PROCESS-KILL SCENARIO (added 2026-09-30): a third call, immediately
# followed by a hard `kill -9` of the app process, to test the one limit
# the NonCancellable fix (PROJECT_LEDGER.md 2026-09-24/26) explicitly does
# NOT cover — the process being killed outright rather than just the
# coroutine scope being cancelled. This is diagnostic only: the audit
# write being LOST here is the expected, accepted outcome, not a failure
# to fix. See the inline comment at that step for why `kill -9` is used
# instead of `am kill`.
#
# Usage:
#   verify-launch-and-capture.sh <component> <package> <workspace_dir>
#
#   <component>       Fully-qualified "package/class" component name. Must
#                      use the explicit class name, not the ".MainActivity"
#                      shorthand — see the note below on why.
#   <package>          The installed applicationId, used for the pidof poll.
#   <workspace_dir>    Where to copy the final full_logcat.txt (normally
#                      $GITHUB_WORKSPACE).
#
# NOTE on component name: the `pulse` product flavor's applicationId is
# com.signalgate.multipoint.pulse, while the Kotlin package is
# com.signalgate.pulse. AndroidManifest.xml has no `package=` attribute, so
# `android:name=".MainActivity"` resolves against the Kotlin package — NOT the
# flavor's applicationId. `adb shell am start -n <pkg>/.MainActivity` expands
# the leading dot against whatever package you give it, so a shorthand call
# would try to launch the non-existent class
# com.signalgate.multipoint.pulse.MainActivity ("Error type 3: Activity class
# ... does not exist"). Always pass the full class name explicitly:
# com.signalgate.multipoint.pulse/com.signalgate.pulse.MainActivity
##############################################################################

set -uo pipefail

COMPONENT="${1:?component (package/class) required}"
PACKAGE="${2:?package required}"
WORKSPACE_DIR="${3:?workspace dir required}"

echo "=== Clearing logcat and starting capture ==="
adb logcat -c
adb logcat -v threadtime > /tmp/full_logcat.txt &
LOGCAT_PID=$!

echo "=== Launching app ($COMPONENT) ==="
adb shell am start -n "$COMPONENT"

echo "=== Verifying process actually started (polling up to 15s) ==="
# ASSUMPTION: relies on `pidof` (toybox) being present on this emulator image.
# Present on google_apis x86_64 API 33 as of this writing. If a future run ever
# prints "pidof: not found" instead of a PID or nothing, swap the check below for:
#   adb shell ps -A | grep -q "$PACKAGE"
LAUNCH_OK=0
for i in $(seq 1 15); do
    if adb shell pidof "$PACKAGE" 2>/dev/null | grep -q '[0-9]'; then
        LAUNCH_OK=1
    fi
    sleep 1
done

if [ "$LAUNCH_OK" -eq 1 ]; then
    echo "App process confirmed running ($PACKAGE)"
else
    echo "::warning::adb shell pidof never saw $PACKAGE running during the 15s window"
fi

echo "=== Granting ROLE_CALL_SCREENING (no UI picker available in headless CI) ==="
adb shell cmd role add-role-holder android.app.role.CALL_SCREENING "$PACKAGE"
ROLE_HELD="$(adb shell dumpsys role | grep -A2 'android.app.role.CALL_SCREENING')"
echo "Role holders for CALL_SCREENING: $ROLE_HELD"
if ! echo "$ROLE_HELD" | grep -q "$PACKAGE"; then
    echo "::warning::$PACKAGE is not listed as CALL_SCREENING role holder after add-role-holder — onScreenCall will not fire below."
fi

echo "=== Simulating call #1 ==="
adb emu gsm call 5551234567
sleep 3
adb emu gsm cancel 5551234567

echo "=== Waiting 20s between calls (gap for backgrounding/process changes) ==="
sleep 20

echo "=== Simulating call #2 ==="
adb emu gsm call 5559876543
sleep 3
adb emu gsm cancel 5559876543

sleep 2

##############################################################################
# PROCESS-KILL SCENARIO (added 2026-09-30):
#
# The NonCancellable fix (PROJECT_LEDGER.md 2026-09-24/26 entries) only
# shields persist() from serviceScope.cancel() -- the Job-cancellation race.
# Its own code comment is explicit that it does NOT protect against the
# hosting process being killed outright, and nothing in this CI has ever
# tested that distinct scenario. This block does, deliberately as a
# non-blocking diagnostic: a real OOM reclaim can land at any point during
# persist()'s ~sub-millisecond-to-a-few-ms window, so AUDIT_PERSIST_SUCCESS
# being absent here is the EXPECTED, accepted outcome, not a regression --
# this documents the known limit rather than asserting a pass/fail gate.
#
# `adb shell am kill` deliberately refuses to kill a process hosting an
# active/foreground-ish component, so it won't reliably reproduce a true
# OOM-style kill here. `kill -9` on the actual PID does, but requires root --
# available on this google_apis (non-Play） x86_64 image via `adb root`.
##############################################################################
echo "=== Simulating call #3 (process-kill scenario) ==="
SUCCESS_COUNT_BEFORE=$(grep -c "AUDIT_PERSIST_SUCCESS" /tmp/full_logcat.txt 2>/dev/null || echo 0)

adb emu gsm call 5555550123
adb root >/dev/null 2>&1
sleep 0.2
KILL_PID="$(adb shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r')"
if [ -n "$KILL_PID" ]; then
    echo "Killing $PACKAGE (pid $KILL_PID) mid-screening to simulate an OS process reclaim"
    adb shell kill -9 "$KILL_PID" 2>/dev/null || echo "::warning::kill -9 failed (adb root likely unavailable on this image) -- process-kill scenario not actually exercised this run"
else
    echo "::warning::Could not find PID for $PACKAGE before call #3 -- process-kill scenario not actually exercised this run"
fi
sleep 3
adb emu gsm cancel 5555550123 2>/dev/null || true
sleep 2

SUCCESS_COUNT_AFTER=$(grep -c "AUDIT_PERSIST_SUCCESS" /tmp/full_logcat.txt 2>/dev/null || echo 0)
if [ "$SUCCESS_COUNT_AFTER" -gt "$SUCCESS_COUNT_BEFORE" ]; then
    echo "PROCESS_KILL_TEST_RESULT: audit write SURVIVED the kill (better than the known/accepted limit -- investigate why, this is not necessarily a problem but is unexpected)"
else
    echo "PROCESS_KILL_TEST_RESULT: audit write LOST after kill -- expected, matches the documented NonCancellable-vs-process-death limit, not a regression"
fi

echo "=== WORKMANAGER STATE (community_sync) ==="
{
    echo "--- jobscheduler ---"
    adb shell dumpsys jobscheduler 2>/dev/null | grep -A 15 "$PACKAGE" | grep -B2 -A 12 -i "community_sync\|WorkManager" || echo "No matching jobscheduler entry found for $PACKAGE"
    echo "--- WorkManager service dump ---"
    adb shell dumpsys activity service com.signalgate.pulse.MainApplication 2>/dev/null | grep -A 20 -i "community_sync" || true
    adb shell dumpsys jobscheduler 2>/dev/null | grep -B5 -A 20 "androidx.work.impl.background.systemjob.SystemJobService" | grep -A 15 "$PACKAGE" || echo "No SystemJobService entries found for $PACKAGE"
} | tee "$WORKSPACE_DIR/workmanager_state.txt"
echo "=== END WORKMANAGER STATE ==="

kill "$LOGCAT_PID" 2>/dev/null || true
cp /tmp/full_logcat.txt "$WORKSPACE_DIR/full_logcat.txt"

echo "=== CRASH LOG ==="
{
    grep -iE "AndroidRuntime|FATAL|Exception|Caused by|signalgate|SignalGateScreening|koin" /tmp/full_logcat.txt \
        || echo "No crash lines found"
} | tee "$WORKSPACE_DIR/signalgate_diagnostic_logcat.txt"
echo "=== END CRASH LOG ==="

echo "=== SCREENING LOG (both simulated calls) ==="
{
    grep -iE "SignalGateScreening|onScreenCall|TELECOM_ROLE_DIAGNOSTIC|Start proc.*$PACKAGE|Process.*$PACKAGE.*died|AUDIT_PERSIST_SUCCESS|AUDIT_PERSIST_TIMEOUT|PROCESS_KILL_TEST_RESULT" /tmp/full_logcat.txt \
        || echo "No screening-related lines found"
} | tee "$WORKSPACE_DIR/signalgate_screening_log.txt"
echo "=== END SCREENING LOG ==="

if [ "$LAUNCH_OK" -ne 1 ]; then
    echo "::error::App process never started. 'am start' likely targeted a component/applicationId that isn't installed, or the process died before pidof could observe it. See the full_logcat artifact for what actually happened (or didn't)."
    exit 1
fi
