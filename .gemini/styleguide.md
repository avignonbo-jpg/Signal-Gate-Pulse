# SignalGate Multi-Port style guide

## Purpose
This repository is an Android app focused on call protection, call screening, overlays, and related UI/telephony flows. Reviews should prioritize correctness, consistency and security with existing app architecture, and minimal safe changes.

## Review priorities
1. Catch syntax, resource, and build-breaking issues.
2. Flag mismatches with nearby files, naming, or architectural patterns.
3. Check that changes fit the existing app reference model.
4. Prefer small, targeted edits over broad refactors.
5. Keep behavior aligned with the current call-screening and overlay flow.

## Android-specific rules
- Verify manifest entries, permissions, receivers, services, and exported flags carefully.
- Check resource references, IDs, drawables, styles, themes, and layout names for consistency.
- Flag duplicate IDs, invalid XML attributes, invalid layout gravity usage, and broken resource links.
- Watch for foreground service, overlay, call screening, and telephony permission issues.
- If a change touches a view, service, receiver, or permission, compare it against related files in the repo.

## Code style
- Follow the existing naming conventions in the repository.
- Do not introduce new patterns unless they clearly improve the current design.
- Avoid rewriting working code unless the change is necessary.
- If changes are deemed necessary, create a PR detailing why and to attain permission from the owner prior to rewriting the code.
- Preserve public APIs and file names unless a rename is explicitly requested.

## Repetitive task behavior
When asked to search for patterns like DAO, TODO, debug logging, duplicate layout IDs, or repeated strings:
- search the repository broadly,
- report every relevant match,
- apply the requested change consistently,
- mention any files that were intentionally skipped.

## Review output expectations
- Be concise and actionable.
- Separate critical issues from suggestions.
- Explain why a change may break compatibility, build stability, or app behavior.
- If something is ambiguous, identify the affected files and ask for clarification instead of guessing.
