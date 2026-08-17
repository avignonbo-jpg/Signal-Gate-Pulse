# Crash Diagnostic log trace — 2026-08-17

## Source of the log

The attached log came from the GitHub Actions `Crash Diagnostic` workflow, not the user's physical device. The workflow runs on `ubuntu-latest`, boots an Android API 33 `google_apis` x86_64 emulator, installs the Pulse debug APK, captures full `adb logcat`, and then terminates the emulator. The attached text includes the workflow's `Terminate Emulator` section.

## Package identity

The debug flavor application ID is `com.signalgate.multipoint.pulse`; the activity class is `com.signalgate.pulse.MainActivity`. Seeing `com.signalgate.multipoint.pulse` in system-service messages therefore identifies the installed Pulse package, but does not by itself mean Pulse emitted the message or caused the warning.

## Ownership classification

- `ConfigFileUtils: Failed to read asset ChimeraModuleSetList.pb` is a Google/Google Play services Chimera-module-loader message. `ChimeraModuleSetList.pb` is not present in the Pulse source and is not referenced by the app.
- `AssistantConnector: Failed to query AGSA value` is a Google Assistant/Search subsystem message. AGSA commonly means Android Google Search App; the Pulse source contains no Assistant/App Actions/AGSA integration.
- `FontLog`, `fetchFonts`, and `Bugle` are Google/system font/message-stack activity. Pulse uses Compose `FontFamily.Default`, contains no downloadable-font or `FontsContract` code, and has no bundled custom font directory. The nearby `Bugle` logger is associated with the Google Messages stack, not Pulse.
- `BlockstoreStorage: Clearing Blockstore Data for package com.signalgate.multipoint.pulse` is a system service reacting to the package installation/replacement. Pulse contains no Blockstore API reference. It is not evidence that Pulse called Blockstore.
- `Package ... com.signalgate.multipoint.pulse has no metadata` is a system package/launcher or service observation. Pulse has normal application metadata such as label, theme, icon, and activity/service declarations; no app-attributable crash is shown.

## Conclusion

The successful Crash Diagnostic run shows that Pulse installed/launched and no app-attributable crash occurred. The listed Chimera, AGSA, FontLog/Bugle, and Blockstore messages are emulator/system or Google-app subsystem noise unless a future filtered, process-correlated log captures a Pulse stack trace. No source remediation is justified from this log alone.

## Recommended follow-up evidence

For a device-specific investigation, capture `adb logcat -v threadtime` while reproducing the event and correlate each line by PID/UID/package. Use `adb shell pidof com.signalgate.multipoint.pulse`, `adb shell dumpsys package com.google.android.gms`, `adb shell dumpsys package com.google.android.googlequicksearchbox`, and `adb shell pm path com.google.android.gms` / `adb shell pm path com.google.android.googlequicksearchbox`. Search the full log for `AndroidRuntime`, `FATAL EXCEPTION`, `Process: com.signalgate.multipoint.pulse`, and `Caused by:` rather than treating all `E` or `W` lines as app failures.

## Sources consulted

- `.github/workflows/crash-diagnostic.yml`
- `scripts/verify-launch-and-capture.sh`
- `android/app/build.gradle`
- `android/app/src/main/AndroidManifest.xml`
- live source grep for Blockstore, font-download, Assistant, AGSA, Chimera, and metadata references
- Google Assistant Help / AGSA terminology: https://support.google.com/assistant/thread/34746700/how-can-i-get-the-google-assistant-that-i-can-carry-a-conversation-with?hl=en
- Android font resource documentation: https://developer.android.com/develop/ui/views/text-and-emoji/fonts-in-xml
- Chimera module discussion (secondary source): https://xdaforums.com/t/what-are-chimera-modules-in-google-services.3409615/
