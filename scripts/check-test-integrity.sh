#!/bin/bash
##############################################################################
# SignalGate Pulse — Test Integrity Check
#
# Detects mechanically-checkable attempts to weaken verification between a
# base ref and the current ref:
#   1. A tracked test file (src/test or src/androidTest) was deleted.
#   2. A surviving test file's @Test method count went down.
#   3. @Ignore / @Disabled was newly added to a test file.
#   4. continue-on-error: true, || true, or set +e was newly added to a
#      workflow.
#   5. The WP-2 workflow is missing a required blocking gate or exact total.
#
# Source annotation counts are a change-detection heuristic. Gradle/JUnit XML
# results remain the authoritative execution result.
#
# Usage: check-test-integrity.sh <base_ref> <current_ref>
##############################################################################

set -uo pipefail

BASE_REF="${1:?base ref required}"
CURRENT_REF="${2:?current ref required}"
FAILED=0

fail() {
    echo "::error::$1"
    FAILED=1
}

is_test_path() {
    case "$1" in
        android/app/src/test/*|android/app/src/androidTest/*) return 0 ;;
        *) return 1 ;;
    esac
}

count_tests() {
    sed -E 's/[[:space:]]+\/\/.*$//' | \
        grep -Ec '^[[:space:]]*@([A-Za-z0-9_.]+\.)?Test([[:space:]]*\(|[[:space:]]|$)' || true
}

count_disabled() {
    sed -E 's/[[:space:]]+\/\/.*$//' | \
        grep -Ec '^[[:space:]]*@([A-Za-z0-9_.]+\.)?(Ignore|Disabled)([[:space:]]*\(|[[:space:]]|$)' || true
}

compare_test_file() {
    local base_path="$1"
    local current_path="$2"
    local label="$3"
    local before after before_disabled after_disabled

    before="$(git show "$BASE_REF:$base_path" 2>/dev/null | count_tests)"
    after="$(git show "$CURRENT_REF:$current_path" 2>/dev/null | count_tests)"
    before=${before:-0}
    after=${after:-0}
    before_disabled="$(git show "$BASE_REF:$base_path" 2>/dev/null | count_disabled)"
    after_disabled="$(git show "$CURRENT_REF:$current_path" 2>/dev/null | count_disabled)"
    before_disabled=${before_disabled:-0}
    after_disabled=${after_disabled:-0}

    if [ "$after" -lt "$before" ]; then
        fail "$label: @Test count dropped from $before to $after"
    fi
    if [ "$after_disabled" -gt "$before_disabled" ]; then
        fail "$label: @Ignore/@Disabled count increased from $before_disabled to $after_disabled — a test was newly skipped"
    fi
}

# --- 1. Deleted and renamed test files --------------------------------------
while IFS=$'\t' read -r status old_path new_path; do
    [ -z "${status:-}" ] && continue
    kind="${status:0:1}"
    case "$kind" in
        D)
            if is_test_path "$old_path"; then
                fail "Test file deleted: $old_path (was present in $BASE_REF, gone in $CURRENT_REF)"
            fi
            ;;
        R)
            if is_test_path "$old_path" && is_test_path "${new_path:-}"; then
                compare_test_file "$old_path" "$new_path" "$new_path (renamed from $old_path)"
            elif is_test_path "$old_path"; then
                fail "Test file renamed outside the test tree: $old_path -> ${new_path:-<missing>}"
            fi
            ;;
    esac
done < <(git diff --name-status --find-renames=50% --diff-filter=DR "$BASE_REF" "$CURRENT_REF" -- '*.kt')

# --- 2/3. Modified test files: count and skip checks -------------------------
while IFS= read -r f; do
    [ -z "$f" ] && continue
    if is_test_path "$f"; then
        compare_test_file "$f" "$f" "$f"
    fi
done < <(git diff --name-only --diff-filter=AMT "$BASE_REF" "$CURRENT_REF" -- '*.kt')

# --- 4. Workflow weakening checks -------------------------------------------
while IFS= read -r f; do
    [ -z "$f" ] && continue
    before="$(git show "$BASE_REF:$f" 2>/dev/null || true)"
    after="$(git show "$CURRENT_REF:$f" 2>/dev/null || true)"
    for pattern in \
        'continue-on-error:[[:space:]]*true' \
        '(^|[[:space:]])\|\|[[:space:]]*true' \
        '^[[:space:]]*set[[:space:]]+\+e'; do
        before_count="$(printf '%s\n' "$before" | grep -Ec "$pattern" || true)"
        after_count="$(printf '%s\n' "$after" | grep -Ec "$pattern" || true)"
        if [ "$after_count" -gt "$before_count" ]; then
            fail "$f: workflow weakening pattern increased: $pattern"
        fi
    done
done < <(git diff --name-only "$BASE_REF" "$CURRENT_REF" -- '.github/workflows/*.yml' '.github/workflows/*.yaml')

# --- 5. Required WP-2 workflow structure ------------------------------------
WP2_WORKFLOW=".github/workflows/wp-2-build-gates.yml"
workflow="$(git show "$CURRENT_REF:$WP2_WORKFLOW" 2>/dev/null || true)"
if [ -z "$workflow" ]; then
    fail "$WP2_WORKFLOW: required workflow is missing"
else
    for required in \
        'pull_request:' \
        'runs-on: ubuntu-24.04' \
        'check-architecture-drift.sh' \
        'check-ledger-updated.sh' \
        'check-test-integrity.sh' \
        'git diff --check' \
        'testPulseDebugUnitTest' \
        'assemblePulseDebug' \
        'expected = {"tests": 138, "failures": 0, "errors": 0, "skipped": 0}'; do
        if ! printf '%s\n' "$workflow" | grep -Fq "$required"; then
            fail "$WP2_WORKFLOW: required blocking-gate content missing: $required"
        fi
    done

    if printf '%s\n' "$workflow" | grep -Eq 'continue-on-error:[[:space:]]*true|(^|[[:space:]])\|\|[[:space:]]*true|^[[:space:]]*set[[:space:]]+\+e'; then
        fail "$WP2_WORKFLOW: fail-open shell/workflow construct detected"
    fi
fi

if [ "$FAILED" -eq 1 ]; then
    echo "::error::Test integrity check failed. Intentional test or gate changes require explicit human sign-off in PROJECT_LEDGER.md."
    exit 1
fi

echo "Test integrity check passed: recursive test paths, rename handling, test counts, skip annotations, workflow weakening, and WP-2 gate structure are intact."
