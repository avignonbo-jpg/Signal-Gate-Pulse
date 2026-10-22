#!/bin/bash
##############################################################################
# SignalGate Pulse — Production Change Ledger Gate
#
# Usage: ./scripts/check-ledger-updated.sh <base-ref>
#
# Production Kotlin changes must be accompanied by an append-only ledger update
# in the same push or pull request. This gate also validates append-only ledger
# history whenever PROJECT_LEDGER.md changes, including documentation-only WP-2.
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

CHANGED_FILES="$(git diff --name-only "$BASE_REF" HEAD)"
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

if [ "$LEDGER_CHANGED" -eq 1 ]; then
    python3 - "$BASE_REF" <<'PY'
import re
import subprocess
import sys
from pathlib import Path

base_ref = sys.argv[1]
base = subprocess.check_output(
    ["git", "show", f"{base_ref}:PROJECT_LEDGER.md"], text=True
)
current = Path("PROJECT_LEDGER.md").read_text()

entry_re = re.compile(r"(?m)^\d{4}-\d{2}-\d{2} — .+$")
base_match = entry_re.search(base)
current_match = entry_re.search(current)
if not base_match or not current_match:
    raise SystemExit("::error::Could not identify the first dated ledger entry in base/current ledger")

base_prefix = base[:base_match.start()]
old_entries = base[base_match.start():]
if not current.startswith(base_prefix):
    raise SystemExit("::error::Ledger preamble changed; PROJECT_LEDGER.md must be append-only")
if not current.endswith(old_entries):
    raise SystemExit("::error::Existing ledger entries changed or were reordered; append only a new newest entry")

inserted = current[len(base_prefix):len(current) - len(old_entries)]
if not inserted.strip():
    raise SystemExit("::error::PROJECT_LEDGER.md changed without a new entry block")
if not entry_re.search(inserted):
    raise SystemExit("::error::New ledger content does not begin with a dated newest entry")

if "WP-2" in inserted:
    required = ["sandbox-only", "CI pending", "no device run"]
    missing = [item for item in required if item not in inserted]
    if missing:
        raise SystemExit("::error::WP-2 ledger entry missing required qualifiers: " + ", ".join(missing))
    if "pre-WP-2 README" not in inserted or "script comments were stale and are now corrected" not in inserted:
        raise SystemExit("::error::WP-2 ledger entry must state that pre-WP-2 README and script comments were stale and are now corrected")

print("✓ Ledger is append-only: preamble and all prior entries are byte-for-byte preserved; a newest entry was added.")
PY
fi

if [ "$PRODUCTION_CHANGED" -eq 0 ]; then
    if [ "$LEDGER_CHANGED" -eq 0 ]; then
        echo "✓ No production Kotlin changes and no ledger change detected; ledger update not required."
    else
        echo "✓ Documentation/validation change has a valid append-only ledger update."
    fi
    exit 0
fi

if [ "$LEDGER_CHANGED" -eq 0 ]; then
    echo "::error::Production Kotlin files changed without a PROJECT_LEDGER.md update."
    echo "Production changes must append a ledger entry in the same push or pull request."
    exit 1
fi

echo "✓ Production Kotlin changes are accompanied by an append-only PROJECT_LEDGER.md update."
