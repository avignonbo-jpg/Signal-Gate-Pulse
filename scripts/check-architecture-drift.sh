#!/bin/bash
##############################################################################
# SignalGate Pulse — Architecture Drift Detection
#
# Enforces the layer-boundary rules from Architecture Contract Section 4
# (Class Ownership Map) and Section 6 (Security Boundary Meaning) that
# neither the Kotlin compiler nor Android Lint catch on their own: which
# layers are allowed to import which other layers.
#
# NOTE (2026-07-15): rule labels below were relabeled to match the current
# 7-layer OSI-style contract (Layer 1 Platform/Edge ... Layer 7 UI). Prior
# versions of this script used a different, incompatible numbering scheme
# (e.g. called SanitizationEngine "L4" when the current contract places it
# in Layer 2 — Security/Parsing). If you find a rule label here that doesn't
# match the contract's Section 4 table, that's a bug — fix the label, not
# the contract.
#
# Run in CI on every push/PR (see .github/workflows/pulse-ci.yml) and can be
# run locally the exact same way:
#
#   ./scripts/check-architecture-drift.sh
#
# Rules enforced:
#   1. UI layer (ui/**, Layer 7)           must not import database.daos.*
#      directly — Composables and their ViewModels go through a Repository,
#      never a DAO.
#   2. Layer 4 decision logic               must not import Notification
#      classes (CallScreeningEngine, CallRiskEvaluator) — building/showing
#      notifications is a Service/UI concern, not a scoring concern.
#   3. Layer 2 Security/Parsing (data/security/**) must not import Room —
#      these are meant to be pure functions with no persistence dependency.
#   4. Layer 5 Application (sync/orchestration engines) must not import Room
#      directly (DataSyncEngine, ReliableSourceManager) — they hand parsed
#      data to a Repository; they don't touch the database themselves.
#   5. runBlocking                         is banned everywhere except
#      MainApplication.kt — anywhere else it risks blocking the calling
#      thread (main thread, a Binder thread in CallScreeningService, a
#      WorkManager thread, etc).
#   6. Cross-cutting purity (data/models/**, utils/**, ui/theme/**) must not
#      import Room, DAOs, or android.content.Context — these are meant to be
#      plain data carriers / stateless functions callable from any layer.
#      (ui/theme/** added 2026-08-13 — see Contract §9.6.)
#   7. No orphaned XML layouts             res/layout/*.xml files with zero
#      references anywhere in src/main/java are drift — this app is
#      Compose-first; legacy View XML should not silently accumulate.
#
# Note on package-to-layer mapping: the `logic/` package holds files from
# two different architecture layers (Layer 4 decision engines and Layer 5
# application/orchestration engines) — it is not a 1:1 package-to-layer
# mapping. Rules 2 and 4 are therefore scoped to specific files, not the
# whole `logic/` directory.
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
# Rule 1: UI layer (Layer 7) -> DAO import
# ---------------------------------------------------------------------------
RULE="Layer7-UI -> DAO"
if [ -d "$SRC/ui" ]; then
    while IFS=: read -r file _line content; do
        [ -z "$file" ] && continue
        fail "$RULE" "${file#$PROJECT_ROOT/}" "$content"
    done < <(grep -rn "^import com\.signalgate\.pulse\.database\.daos" "$SRC/ui" --include="*.kt" 2>/dev/null)
fi

# ---------------------------------------------------------------------------
# Rule 2: Layer 4 decision logic -> Notification import
# ---------------------------------------------------------------------------
RULE="Layer4-Domain -> Notification"
L4_FILES=(
    "$SRC/logic/CallScreeningEngine.kt"
    "$SRC/logic/CallRiskEvaluator.kt"
)
for f in "${L4_FILES[@]}"; do
    [ -f "$f" ] || continue
    while IFS=: read -r _line content; do
        [ -z "$content" ] && continue
        fail "$RULE" "${f#$PROJECT_ROOT/}" "$content"
    done < <(grep -n "^import \(android\.app\.Notification\|androidx\.core\.app\.Notification\)" "$f")
done

# ---------------------------------------------------------------------------
# Rule 3: Layer 2 Security/Parsing -> Room import
# ---------------------------------------------------------------------------
RULE="Layer2-Security -> Room"
if [ -d "$SRC/data/security" ]; then
    while IFS=: read -r file _line content; do
        [ -z "$file" ] && continue
        fail "$RULE" "${file#$PROJECT_ROOT/}" "$content"
    done < <(grep -rn "^import androidx\.room" "$SRC/data/security" --include="*.kt" 2>/dev/null)
fi

# ---------------------------------------------------------------------------
# Rule 4: Layer 5 Application (sync/orchestration) -> Room import
# ---------------------------------------------------------------------------
RULE="Layer5-Application -> Room"
L5_FILES=(
    "$SRC/logic/DataSyncEngine.kt"
    "$SRC/logic/ReliableSourceManager.kt"
)
for f in "${L5_FILES[@]}"; do
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

# ---------------------------------------------------------------------------
# Rule 6: Cross-cutting (data/models, utils, ui/theme) must stay pure —
#         no Room, no DAOs, no Context
#
# NOTE (2026-08-13): ui/theme/ was added to this rule's scope. Contract §4's
# Cross-Cutting list has always claimed Color/Theme/SignalGateTheme/Effects
# as pure, but this rule only ever scanned data/models and utils — the claim
# was unenforced. See Contract §9.6.
# ---------------------------------------------------------------------------
RULE="Cross-cutting purity"
CROSS_CUTTING_DIRS=(
    "$SRC/data/models"
    "$SRC/utils"
    "$SRC/ui/theme"
)
for d in "${CROSS_CUTTING_DIRS[@]}"; do
    [ -d "$d" ] || continue
    while IFS=: read -r file _line content; do
        [ -z "$file" ] && continue
        fail "$RULE" "${file#$PROJECT_ROOT/}" "$content"
    done < <(grep -rn "^import \(androidx\.room\|com\.signalgate\.multipoint\.database\.daos\|android\.content\.Context\)" "$d" --include="*.kt" 2>/dev/null)
done

# ---------------------------------------------------------------------------
# Rule 7: Orphaned XML layouts — res/layout/*.xml with zero references
#         anywhere in src/main/java (Compose-first architecture, Section 1)
# ---------------------------------------------------------------------------
RULE="Orphaned XML layout"
RES_LAYOUT="$PROJECT_ROOT/android/app/src/main/res/layout"
if [ -d "$RES_LAYOUT" ]; then
    for layout_file in "$RES_LAYOUT"/*.xml; do
        [ -f "$layout_file" ] || continue
        layout_name="$(basename "$layout_file" .xml)"
        if ! grep -rq "$layout_name" "$SRC" --include="*.kt" 2>/dev/null; then
            fail "$RULE" "${layout_file#$PROJECT_ROOT/}" "no reference to '$layout_name' found anywhere in src/main/java"
        fi
    done
fi

echo
if [ "$VIOLATIONS" -eq 0 ]; then
    echo -e "${GREEN}✓ No architecture drift detected.${NC}"
    exit 0
else
    echo -e "${RED}✗ $VIOLATIONS architecture drift violation(s) found.${NC}"
    echo "See Architecture-Contract.md Section 4 (Class Ownership Map) for the layer-boundary rules being enforced."
    exit 1
fi
