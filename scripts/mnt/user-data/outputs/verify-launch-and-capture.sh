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
# NOTE on component name: the `pulse` product flavor overrides applicationId
# to com.signalgate.multipoint.pulse, but AndroidManifest.xml has no `package=`
# attribute, so `android:name=".MainActivity"` resolves against build.gradle's
# `namespace` (com.signalgate.multipoint) — NOT the flavor's applicationId.
# `adb shell am start -n <pkg>/.MainActivity` expands the leading dot against
# whatever package you give it, so a shorthand call here would try to launch
# the non-existent class com.signalgate.multipoint.pulse.MainActivity
# ("Error type 3: Activity class ... does not exist"). Always pass the full
# class name explicitly: com.signalgate.multipoint.pulse/com.signalgate.multipoint.MainActivity
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

kill "$LOGCAT_PID" 2>/dev/null || true
cp /tmp/full_logcat.txt "$WORKSPACE_DIR/full_logcat.txt"
echo "=== CRASH LOG ==="
grep -E "AndroidRuntime|FATAL|Exception|Caused by|signalgate|koin" /tmp/full_logcat.txt || echo "No crash lines found"
echo "=== END CRASH LOG ==="

if [ "$LAUNCH_OK" -ne 1 ]; then
    echo "::error::App process never started. 'am start' likely targeted a component/applicationId that isn't installed, or the process died before pidof could observe it. See the full_logcat artifact for what actually happened (or didn't)."
    exit 1
fi
