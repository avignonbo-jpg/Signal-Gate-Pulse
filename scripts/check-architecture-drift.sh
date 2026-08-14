#!/bin/bash
##############################################################################
# SignalGate Pulse — Architecture Drift Detection
#
# Enforces the layer-boundary rules from Architecture Contract §7.3 that
# neither the Kotlin compiler nor Android Lint catch on their own: which
# layers are allowed to import which other layers.
#
# Run in CI on every push/PR (see .github/workflows/pulse-ci.yml) and can be
# run locally the exact same way:
#
#   ./scripts/check-architecture-drift.sh
#
# Rules enforced (Roadmap Step 0.4):
#   1. UI layer (ui/**)                    must not import database.daos.*
#      directly — Composables and their ViewModels go through a Repository,
#      never a DAO.
#   2. L6 decision logic                   must not import Notification
#      classes (CallScreeningEngine, CallRiskEvaluator) — building/showing
#      notifications is a Service/UI concern, not a scoring concern.
#   3. L4 sanitization (data/security/**)  must not import Room — these are
#      meant to be pure functions with no persistence dependency.
#   4. L2 transport/sync engines           must not import Room directly
#      (DataSyncEngine, ReliableSourceManager) — they hand parsed data to a
#      Repository; they don't touch the database themselves.
#   5. runBlocking                         is banned everywhere except
#      MainApplication.kt — anywhere else it risks blocking the calling
#      thread (main thread, a Binder thread in CallScreeningService, a
#      WorkManager thread, etc).
#
# Note on L2/L6 classification: the `logic/` package holds files from two
# different architecture layers (L2 transport engines and L6 decision
# engines) — it is not a 1:1 package-to-layer mapping. Rules 2 and 4 are
# therefore scoped to specific files, not the whole `logic/` directory.
#
# This is intentionally a set of grep-based structural checks, not a real
# lint/detekt plugin — good enough to catch drift, cheap to maintain, and
# fails fast in CI before the expensive Gradle build step runs.
##############################################################################

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SRC="$PROJECT_ROOT/android/app/src/main/java/com/signalgate/pulse"

VIOLATIONS=0

# Colors (matches scripts/analyze-compose-metrics.sh conventions)
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

# $1 = rule name, $2 = file (relative), $3 = offending line
fail() {
    echo -e "${RED}✗ [$1]${NC} $2"
    echo "    $3"
    echo "::error file=$2::Architecture drift ($1): $3"
    VIOLATIONS=$((VIOLATIONS + 1))
}

if [ ! -d "$SRC" ]; then
    echo "::error::Expected source root not found: $SRC"
    exit 1
fi

echo "Running architecture drift checks against: ${SRC#$PROJECT_ROOT/}"
echo

# ---------------------------------------------------------------------------
# Rule 1: UI layer -> DAO import
# ---------------------------------------------------------------------------
RULE="UI -> DAO"
if [ -d "$SRC/ui" ]; then
    while IFS=: read -r file _line content; do
        [ -z "$file" ] && continue
        fail "$RULE" "${file#$PROJECT_ROOT/}" "$content"
    done < <(grep -rn "^import com\.signalgate\.pulse\.database\.daos" "$SRC/ui" --include="*.kt" 2>/dev/null)
fi

# ---------------------------------------------------------------------------
# Rule 2: L6 decision logic -> Notification import
# ---------------------------------------------------------------------------
RULE="L6 -> Notification"
L6_FILES=(
    "$SRC/logic/CallScreeningEngine.kt"
    "$SRC/logic/CallRiskEvaluator.kt"
)
for f in "${L6_FILES[@]}"; do
    [ -f "$f" ] || continue
    while IFS=: read -r _line content; do
        [ -z "$content" ] && continue
        fail "$RULE" "${f#$PROJECT_ROOT/}" "$content"
    done < <(grep -n "^import \(android\.app\.Notification\|androidx\.core\.app\.Notification\)" "$f")
done

# ---------------------------------------------------------------------------
# Rule 3: L4 sanitization -> Room import
# ---------------------------------------------------------------------------
RULE="L4 -> Room"
if [ -d "$SRC/data/security" ]; then
    while IFS=: read -r file _line content; do
        [ -z "$file" ] && continue
        fail "$RULE" "${file#$PROJECT_ROOT/}" "$content"
    done < <(grep -rn "^import androidx\.room" "$SRC/data/security" --include="*.kt" 2>/dev/null)
fi

# ---------------------------------------------------------------------------
# Rule 4: L2 transport/sync -> Room import
# ---------------------------------------------------------------------------
RULE="L2 -> Room"
L2_FILES=(
    "$SRC/logic/DataSyncEngine.kt"
    "$SRC/logic/ReliableSourceManager.kt"
)
for f in "${L2_FILES[@]}"; do
    [ -f "$f" ] || continue
    while IFS=: read -r _line content; do
        [ -z "$content" ] && continue
        fail "$RULE" "${f#$PROJECT_ROOT/}" "$content"
    done < <(grep -n "^import androidx\.room" "$f")
done

# ---------------------------------------------------------------------------
# Rule 5: runBlocking outside MainApplication.kt
# ---------------------------------------------------------------------------
RULE="runBlocking outside MainApplication"
while IFS=: read -r file _line content; do
    [ -z "$file" ] && continue
    case "$file" in
        */MainApplication.kt) continue ;;
    esac
    fail "$RULE" "${file#$PROJECT_ROOT/}" "$content"
done < <(grep -rn "runBlocking[[:space:]]*[({]" "$SRC" --include="*.kt" 2>/dev/null)

echo
if [ "$VIOLATIONS" -eq 0 ]; then
    echo -e "${GREEN}✓ No architecture drift detected.${NC}"
    exit 0
else
    echo -e "${RED}✗ $VIOLATIONS architecture drift violation(s) found.${NC}"
    echo "See Architecture Contract §7.3 for the layer-boundary rules being enforced."
    exit 1
fi
