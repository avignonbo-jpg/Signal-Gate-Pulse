#!/bin/bash
##############################################################################
# SignalGate Pulse — Test Integrity Check
#
# Detects mechanically-checkable attempts to weaken verification between a
# base ref and the current ref:
#   1. A tracked test file (src/test or src/androidTest) was deleted.
#   2. A surviving test file's @Test method count went down.
#   3. @Ignore / @Disabled was newly added to a test file (wasn't there in
#      the base ref, is there now).
#   4. continue-on-error: true was newly added to any GitHub Actions
#      workflow (wasn't there in the base ref, is there now).
#
# Deliberately NOT covered: assertion-count diffing. Too many legitimate
# refactors (extracting a helper, consolidating asserts) reduce raw
# assertion counts without weakening coverage — a heuristic gate on that
# would be noisy enough that people learn to ignore it, which defeats the
# purpose. This script only fails on things that are unambiguous.
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

# --- 1. Deleted test files -------------------------------------------------
while IFS= read -r f; do
    [ -z "$f" ] && continue
    case "$f" in
        android/app/src/test/*.kt|android/app/src/androidTest/*.kt)
            fail "Test file deleted: $f (was present in $BASE_REF, gone in $CURRENT_REF)"
            ;;
    esac
done < <(git diff --name-only --diff-filter=D "$BASE_REF" "$CURRENT_REF" -- '*.kt')

# --- 2. Decreased @Test count per surviving test file ----------------------
while IFS= read -r f; do
    [ -z "$f" ] && continue
    case "$f" in
        android/app/src/test/*.kt|android/app/src/androidTest/*.kt) ;;
        *) continue ;;
    esac

    before=$(git show "$BASE_REF:$f" 2>/dev/null | grep -c '^\s*@Test\b' || true)
    after=$(git show "$CURRENT_REF:$f" 2>/dev/null | grep -c '^\s*@Test\b' || true)
    before=${before:-0}
    after=${after:-0}

    if [ "$after" -lt "$before" ]; then
        fail "$f: @Test count dropped from $before to $after"
    fi
done < <(git diff --name-only --diff-filter=M "$BASE_REF" "$CURRENT_REF" -- '*.kt')

# --- 3. Newly-added @Ignore / @Disabled -------------------------------------
while IFS= read -r f; do
    [ -z "$f" ] && continue
    case "$f" in
        android/app/src/test/*.kt|android/app/src/androidTest/*.kt) ;;
        *) continue ;;
    esac

    before_ignored=$(git show "$BASE_REF:$f" 2>/dev/null | grep -c '@Ignore\|@Disabled' || true)
    after_ignored=$(git show "$CURRENT_REF:$f" 2>/dev/null | grep -c '@Ignore\|@Disabled' || true)
    before_ignored=${before_ignored:-0}
    after_ignored=${after_ignored:-0}

    if [ "$after_ignored" -gt "$before_ignored" ]; then
        fail "$f: @Ignore/@Disabled count increased from $before_ignored to $after_ignored — a test was newly skipped"
    fi
done < <(git diff --name-only --diff-filter=M "$BASE_REF" "$CURRENT_REF" -- '*.kt')

# --- 4. Newly-added continue-on-error in any workflow -----------------------
while IFS= read -r f; do
    [ -z "$f" ] && continue

    before_coe=$(git show "$BASE_REF:$f" 2>/dev/null | grep -c 'continue-on-error:\s*true' || true)
    after_coe=$(git show "$CURRENT_REF:$f" 2>/dev/null | grep -c 'continue-on-error:\s*true' || true)
    before_coe=${before_coe:-0}
    after_coe=${after_coe:-0}

    if [ "$after_coe" -gt "$before_coe" ]; then
        fail "$f: continue-on-error: true count increased from $before_coe to $after_coe — a required check may have been made advisory"
    fi
done < <(git diff --name-only "$BASE_REF" "$CURRENT_REF" -- '.github/workflows/*.yml')

if [ "$FAILED" -eq 1 ]; then
    echo "::error::Test integrity check failed. If any of the above is intentional (e.g. a genuinely obsolete test being removed), it needs explicit human sign-off recorded in PROJECT_LEDGER.md, not just to pass silently."
    exit 1
fi

echo "Test integrity check passed: no deleted tests, no reduced @Test counts, no newly-skipped tests, no newly-added continue-on-error."
