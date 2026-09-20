#!/bin/bash
##############################################################################
# SignalGate Pulse — Production Change Ledger Gate
#
# Usage: ./scripts/check-ledger-updated.sh <base-ref>
#
# Production Kotlin changes must be accompanied by an append-only ledger update
# in the same push or pull request. Test Kotlin, documentation, and other
# non-production changes do not require a ledger update.
##############################################################################

set -euo pipefail

BASE_REF="${1:-}"
if [ -z "$BASE_REF" ]; then
    echo "::error::Usage: $0 <base-ref>"
    exit 2
fi

if ! git rev-parse --verify "$BASE_REF^{commit}" >/dev/null 2>&1; then
    echo "::error::Base ref '$BASE_REF' is not available locally"
    exit 2
fi

CHANGED_FILES="$(git diff --name-only "$BASE_REF"...HEAD)"
PRODUCTION_CHANGED=0
LEDGER_CHANGED=0

while IFS= read -r path; do
    [ -z "$path" ] && continue
    if [[ "$path" == android/app/src/main/* && "$path" == *.kt ]]; then
        PRODUCTION_CHANGED=1
    fi
    if [ "$path" = "PROJECT_LEDGER.md" ]; then
        LEDGER_CHANGED=1
    fi
done <<< "$CHANGED_FILES"

if [ "$PRODUCTION_CHANGED" -eq 0 ]; then
    echo "✓ No production Kotlin changes detected; ledger update not required."
    exit 0
fi

if [ "$LEDGER_CHANGED" -eq 0 ]; then
    echo "::error::Production Kotlin files changed without a PROJECT_LEDGER.md update."
    echo "Production changes must append a ledger entry in the same push or pull request."
    exit 1
fi

BASE_LEDGER_LINES=0
if git cat-file -e "$BASE_REF:PROJECT_LEDGER.md" 2>/dev/null; then
    BASE_LEDGER_LINES="$(git show "$BASE_REF:PROJECT_LEDGER.md" | wc -l)"
fi
CURRENT_LEDGER_LINES="$(wc -l < PROJECT_LEDGER.md)"

if [ "$CURRENT_LEDGER_LINES" -le "$BASE_LEDGER_LINES" ]; then
    echo "::error::PROJECT_LEDGER.md changed but did not grow (base: $BASE_LEDGER_LINES lines; current: $CURRENT_LEDGER_LINES lines)."
    echo "Append a new ledger entry for production Kotlin changes."
    exit 1
fi

echo "✓ Production Kotlin changes are accompanied by a growing PROJECT_LEDGER.md ($BASE_LEDGER_LINES -> $CURRENT_LEDGER_LINES lines)."
