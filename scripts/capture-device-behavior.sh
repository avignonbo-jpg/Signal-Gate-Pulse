#!/usr/bin/env bash
# capture-device-behavior.sh
# Captures Signal Gate Pulse behavior on an Android device over a timed window:
#   - app, sync-worker, and screening logs (redacted)
#   - a sampled timeline of battery saver, battery exemption, screening role,
#     and scheduled jobs
#   - start and end snapshots of permissions, role, standby state, and process exits
# Deliberately NOT captured: call-log content, contacts, SMS, the radio buffer,
# and full bugreports (unless you pass --bugreport). Phone-number-like strings
# are redacted from everything written.
#
# Usage: ./capture-device-behavior.sh [-s SERIAL] [-d SECONDS] [-i SAMPLE_SECONDS] [--bugreport] [--raw] [--self-test]
# --raw turns off redaction (developer test devices only). Output then contains real numbers. Keep the folder private.
set -uo pipefail

PKG="com.signalgate.multipoint.pulse"
DURATION=900
SAMPLE=30
SERIAL=""
BUGREPORT=0
RAW=0
SELF_TEST=0

while [ $# -gt 0 ]; do
  case "$1" in
    -s) SERIAL="$2"; shift 2 ;;
    -d) DURATION="$2"; shift 2 ;;
    -i) SAMPLE="$2"; shift 2 ;;
    --bugreport) BUGREPORT=1; shift ;;
    --raw) RAW=1; shift ;;
    --self-test) SELF_TEST=1; shift ;;
    -h|--help) sed -n '2,12p' "$0"; exit 0 ;;
    *) echo "Unknown option: $1"; exit 2 ;;
  esac
done

# Redact phone-number-like strings (7+ digits, optional leading +) everywhere we write.
redact() { if [ "$RAW" -eq 1 ]; then cat; else sed -E 's/\+?[0-9]{7,}/[REDACTED-NUMBER]/g'; fi; }

if [ "$SELF_TEST" -eq 1 ]; then
  RAW=0
  failures=0

  result=$(printf '%s' 'Call 1234567890' | redact)
  if [ "$result" = 'Call [REDACTED-NUMBER]' ]; then
    echo 'PASS: 10-digit number masked'
  else
    echo "FAIL: 10-digit number was not masked: $result"
    failures=$((failures + 1))
  fi

  result=$(printf '%s' 'Call +442079460018' | redact)
  if [ "$result" = 'Call [REDACTED-NUMBER]' ]; then
    echo 'PASS: international number masked'
  else
    echo "FAIL: international number was not masked: $result"
    failures=$((failures + 1))
  fi

  result=$(printf '%s' 'ALLOW tier=CLEAN_UNKNOWN' | redact)
  if [ "$result" = 'ALLOW tier=CLEAN_UNKNOWN' ]; then
    echo 'PASS: ordinary text unchanged'
  else
    echo "FAIL: ordinary text changed: $result"
    failures=$((failures + 1))
  fi

  [ "$failures" -eq 0 ]
  exit $?
fi

command -v adb >/dev/null 2>&1 || { echo "adb not found. Install platform-tools first."; exit 1; }
ADB=(adb)
[ -n "$SERIAL" ] && ADB=(adb -s "$SERIAL")

STATE=$("${ADB[@]}" get-state 2>/dev/null || true)
[ "$STATE" = "device" ] || { echo "No authorized device found (state: ${STATE:-none}). Check 'adb devices'."; exit 1; }

STAMP=$(date +%Y%m%d-%H%M%S)
OUT="captures/${STAMP}"
mkdir -p "$OUT"
echo "Writing to $OUT (duration ${DURATION}s, sampling every ${SAMPLE}s, redaction $([ "$RAW" -eq 1 ] && echo OFF || echo ON))"

# Run a shell command on the device, redact, and save to a file. Never fails the script.
snap() {
  local name="$1"; shift
  "${ADB[@]}" shell "$@" 2>&1 | redact > "$OUT/$name" || true
}

PIDS=()
cleanup() {
  for p in "${PIDS[@]:-}"; do kill "$p" 2>/dev/null || true; done
  echo "Stopping captures and taking end snapshots..."
  end_snapshots
  echo "Done. Output: $OUT"
  exit 0
}
trap cleanup INT TERM

# ---------- snapshots (start and end) ----------
device_info() {
  snap device_info.txt sh -c 'getprop ro.product.model; getprop ro.build.version.release; getprop ro.build.fingerprint; date'
  snap package_info.txt dumpsys package "$PKG"
}

role_state() {
  snap role_holders.txt cmd role get-role-holders android.app.role.CALL_SCREENING
  snap role_holders_fallback.txt dumpsys role
}

power_state() {
  snap battery_saver.txt settings get global low_power
  snap battery_optimization.txt cmd deviceidle whitelist
  snap deviceidle_summary.txt dumpsys deviceidle
}

jobs_state() {
  snap jobs.txt dumpsys jobscheduler
  snap workmanager_state.txt sh -c "dumpsys jobscheduler | grep -A40 -i '$PKG' || true"
}

permissions_state() {
  snap appops.txt appops get "$PKG"
  snap standby_bucket.txt cmd usagestats get-standby-bucket "$PKG"
}

process_state() {
  snap process_exits.txt dumpsys activity exit-info "$PKG"
  snap process_running.txt sh -c "pidof $PKG || echo not-running"
}

network_state() {
  snap network.txt dumpsys connectivity
}

end_snapshots() {
  power_state; jobs_state; permissions_state; process_state; role_state; network_state
  "${ADB[@]}" shell "dumpsys telecom" 2>&1 | redact > "$OUT/telecom_end.txt" || true
}

device_info
role_state
power_state
jobs_state
permissions_state
process_state
network_state
snap telecom_start.txt dumpsys telecom

# ---------- continuous captures ----------
# Full logcat for the app and system crash/ANR/events buffers.
# Radio buffer excluded (it can contain call-related identifiers).
"${ADB[@]}" logcat -c 2>/dev/null || true
"${ADB[@]}" logcat -v threadtime -b main,system,crash,events 2>&1 | redact > "$OUT/logcat_all.txt" &
PIDS+=($!)

# Focused view of the tags we care about, for quick reading.
"${ADB[@]}" logcat -v threadtime 2>&1 \
  | grep --line-buffered -E "SignalGate|ReliableSourceManager|CommunitySyncWorker|WM-WorkerWrapper|WorkManager|Telecom|TelecomFramework|CallScreening|ANR|FATAL|AndroidRuntime|StrictMode" \
  | redact > "$OUT/logcat_focused.txt" &
PIDS+=($!)

# Sampled timeline: one line per sample with timestamp and the key state values.
(
  while true; do
    ts=$(date '+%Y-%m-%d %H:%M:%S')
    saver=$("${ADB[@]}" shell settings get global low_power 2>/dev/null | tr -d '\r')
    alive=$("${ADB[@]}" shell pidof "$PKG" 2>/dev/null | tr -d '\r')
    exempt=$("${ADB[@]}" shell cmd deviceidle whitelist 2>/dev/null | grep -c "$PKG" || true)
    echo "$ts battery_saver=${saver:-unknown} battery_exempt=${exempt} pid=${alive:-none}" >> "$OUT/timeline.txt"
    sleep "$SAMPLE"
  done
) &
PIDS+=($!)

# Optional full bugreport: contains much more data, including call details.
if [ "$BUGREPORT" -eq 1 ]; then
  echo "Bugreport requested. It contains extensive system data; keep the folder private."
  (sleep "$DURATION"; "${ADB[@]}" bugreport "$OUT/bugreport" >/dev/null 2>&1) &
  PIDS+=($!)
fi

echo "Capturing. Trigger your test now (for example Sync now on FCC). Ctrl-C to stop early."
sleep "$DURATION"
cleanup
