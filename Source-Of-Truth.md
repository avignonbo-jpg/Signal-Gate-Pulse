# SignalGate Pulse — Source-of-Truth Branch Audit

> Authority notice (2026-10-25): This file is retained as the historical inventory artifact for this branch. The current authoritative source-of-truth document for `consumer-v1` is `SignalGatePulseAudit_20261025_SOURCE_OF_TRUTH (1).md`. All branch governance, audit status, and fact assertions should be read against that file first. This file remains useful as a secondary reference and historical record, but it is no longer the governing document.

**Audit scope.** This document records what exists in the attached branch only; it does not propose fixes or assess desired behavior. Paths are relative to the repository root `Signal-Gate-Pulse` unless otherwise noted.

## Inventory summary

| Area | Count / value |
|---|---:|
| Production Kotlin files (`android/app/src/main`) | 83 |
| JVM unit-test Kotlin files (`android/app/src/test`) | 17 |
| Instrumented-test Kotlin files (`android/app/src/androidTest`) | 8 |
| Total Kotlin files | 108 |
| Android module | `android/app` |
| Namespace | `com.signalgate.pulse` |
| Default application ID | `com.signalgate.multipoint` |
| `pulse` flavor application ID | `com.signalgate.multipoint.pulse` |


## 1. Kotlin files
