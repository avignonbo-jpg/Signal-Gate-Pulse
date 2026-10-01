# Signal-Gate Pulse — Consumer v1

**The source of truth for the Signal-Gate Pulse consumer-grade call protection app.**

---

## What is Signal-Gate Pulse?

**Pulse** is a native Android application (Kotlin) that provides real-time call screening and blocking for everyday users who want safety without managing complex rules.

### Mission
Turn call protection from a reactive utility into a convenient, always-on consumer experience.

### Core Purpose
- **Real-time call screening** — Intercepts incoming calls instantly
- **Pattern learning** — Improves filtering over time
- **Low friction** — Set-and-forget protection with minimal user management
- **Smart routing** — Blocks, screens, or quietly routes suspicious calls

### Use Cases
- Spam defense
- Scam reduction
- Nuisance-call suppression
- Personal call management without technical overhead

---

## Project Overview

**Repository**: `avignonbo-jpg/Signal-Gate-Pulse`  
**Active Branch**: `consumer-v1` (default)  
**Language**: Kotlin  
**Platform**: Android (native)  
**Build System**: Gradle  
**Last Updated**: 2026-09-30

### Key Technologies
- **Kotlin** — Modern Android language
- **Room Database** — Local data persistence
- **Jetpack Compose** — Modern UI framework (where applicable)
- **CallScreeningService** — Android call interception API
- **KSP** — Kotlin Symbol Processing for compile-time optimization

---

## Project Structure

```
Signal-Gate-Pulse/
├── android/
│   ├── app/
│   │   ├── build.gradle              # App-level config (dependencies, signing)
│   │   ├── src/main/
│   │   │   ├── AndroidManifest.xml   # App permissions and components
│   │   │   ├── java/com/signalgate/multipoint/
│   │   │   │   ├── CallScreeningService.kt       # Core call interception logic
│   │   │   │   ├── MainActivity.kt               # App entry point
│   │   │   │   ├── MainApplication.kt            # App initialization
│   │   │   │   ├── database/
│   │   │   │   │   ├── SignalGateDatabase.kt     # Room Database definition
│   │   │   │   │   ├── entities/                 # Data models (UnifiedEntryEntity, etc.)
│   │   │   │   │   └── daos/                     # Data Access Objects
│   │   │   │   └── ui/                           # UI screens and components
│   │   │   └── res/                  # Android resources (icons, strings, layouts, themes)
│   ├── build.gradle                  # Project-level config
│   └── gradle.properties              # Gradle build settings
├── tools/
│   └── metrics-analysis/             # Performance analysis tools (Compose metrics)
├── docs/                             # Documentation (if present)
├── README.md                         # This file
└── .gitignore

```

### Key Files Explained

| File | Purpose |
|------|---------|
| `CallScreeningService.kt` | The engine — intercepts calls, applies rules, routes handling |
| `MainActivity.kt` | User-facing UI entry point |
| `SignalGateDatabase.kt` | Local data store for blocked calls, user settings, patterns |
| `AndroidManifest.xml` | Declares permissions (READ_PHONE_STATE, CALL_LOG, etc.) |
| `build.gradle` (app) | Dependencies, compilation targets, plugin config |

---

## Getting Started

### Prerequisites
- **Android Studio** (latest version recommended)
- **JDK 11+** (bundled with Android Studio)
- **Android SDK** (API 24+, though target is higher for modern features)
- **Git** (for cloning and version control)

### Clone the Repository

```bash
# Clone the consumer-v1 branch
git clone --branch consumer-v1 \
  https://github.com/avignonbo-jpg/Signal-Gate-Pulse.git

cd Signal-Gate-Pulse
```

Or if you already cloned the full repo:

```bash
git clone https://github.com/avignonbo-jpg/Signal-Gate-Pulse.git
cd Signal-Gate-Pulse
git checkout consumer-v1
```

### Setup in Android Studio

1. **Open the project**: `File → Open` → select `Signal-Gate-Pulse` folder
2. **Wait for Gradle sync** (first time takes 2–5 minutes)
3. **Check SDK versions**: `File → Project Structure → Modules → app`
   - Target SDK should be current (API 35+)
   - Min SDK is API 24+
4. **Resolve any dependency issues**: Let Android Studio download missing SDKs/tools

### Build the App

```bash
# From the repo root
cd android
./gradlew build

# Or from Android Studio
Build → Make Project
```

### Run on Device or Emulator

```bash
# From Android Studio
Run → Run 'app'

# Or command line
./gradlew installDebug
```

---

## Development Workflow

### Branch Strategy

- **`consumer-v1`** (default, this branch)
  - Stable consumer-facing code
  - All features must be tested and reviewed
  - Direct commits to this branch are restricted (use pull requests)

- **Feature branches** (when contributing)
  - Format: `feature/description` or `fix/description`
  - Branch from `consumer-v1`
  - Submit pull request for review before merging

### Making Changes

```bash
# 1. Create a feature branch
git checkout -b feature/my-feature

# 2. Make your changes
# (edit files, test locally)

# 3. Commit with clear messages
git add .
git commit -m "Add feature: description of what changed"

# 4. Push to GitHub
git push origin feature/my-feature

# 5. Create a Pull Request on GitHub
# → Go to https://github.com/avignonbo-jpg/Signal-Gate-Pulse
# → Click "New Pull Request"
# → Base: consumer-v1, Compare: feature/my-feature
```

### Code Style & Standards

- **Kotlin style**: Follow [Kotlin official conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **Naming**: Use descriptive names (no single letters except loop vars)
- **Comments**: Document complex logic, not obvious code
- **Testing**: Write unit tests for business logic (database, filtering, etc.)

### Testing

```bash
# Run all unit tests
./gradlew test

# Run instrumented tests (on device/emulator)
./gradlew connectedAndroidTest

# Run specific test
./gradlew test --tests "com.signalgate.multipoint.CallScreeningTest"
```

---

## Build & Deployment

### Debug Build

```bash
./gradlew assembleDebug
# Output: android/app/build/outputs/apk/debug/app-debug.apk
```

### Release Build

```bash
./gradlew assembleRelease
# Output: android/app/build/outputs/apk/release/app-release.apk
```

**Note**: Release builds require signing configuration (see `build.gradle` for signing config).

### Generating Metrics

For performance analysis (Jetpack Compose optimization):

```bash
./gradlew assembleRelease
# Metrics generated to: android/app/build/compose_metrics/

# Analyze with provided script
python3 tools/metrics-analysis/analyze_metrics.py \
  android/app/build/compose_metrics/
```

See `tools/metrics-analysis/README.md` for detailed performance tuning.

---

## Core Architecture

### Call Interception Flow

```
Incoming Call
    ↓
CallScreeningService.onScreenCall()
    ↓
Fetch rules from SignalGateDatabase
    ↓
Analyze caller (pattern matching, blocklist, ML scoring)
    ↓
Decide: Block | Screen | Allow
    ↓
Route call (block, silent notification, normal routing)
```

### Database Schema

The app uses **Room Database** for persistence:

- **UnifiedEntryEntity** — Stores blocked/screened calls with timestamps, caller info
- **BlockedNumberEntity** (if separate) — User-maintained blocklist
- **RuleEntity** — Filtering rules and patterns
- **SettingsEntity** — User preferences

Query DAOs via:

```kotlin
val database = SignalGateDatabase.getInstance(context)
val entries = database.unifiedEntryDao().getAllEntries()
```

### Permissions Required

(Declared in `AndroidManifest.xml`)

```xml
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.CALL_LOG" />
<uses-permission android:name="android.permission.ANSWER_PHONE_CALLS" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.INTERNET" /> <!-- for pattern syncing -->
```

---

## Key Files & Their Responsibilities

| Class | Responsibility |
|-------|-----------------|
| `CallScreeningService` | Intercepts calls, applies rules, routes them |
| `MainActivity` | Main UI; shows blocked calls, settings, dashboard |
| `MainApplication` | Initializes database, app-wide configuration |
| `SignalGateDatabase` | Room database singleton; DAO access point |
| Various `*Entity` classes | Data models (Kotlin data classes mapped to database tables) |
| Various `*Dao` interfaces | Database queries and mutations (inserts, updates, deletes) |

---

## Troubleshooting

### Build Fails with "Gradle Sync Error"
- Invalidate cache: `File → Invalidate Caches`
- Update Gradle: `gradle/wrapper/gradle-wrapper.properties` → latest version
- Check SDK versions in `build.gradle`

### App Crashes on Launch
- Check logcat: `Logcat` panel in Android Studio
- Ensure permissions are granted on device (Settings → Apps → Pulse → Permissions)
- Verify database is initialized: check `MainApplication.kt`

### Call Screening Not Working
- Confirm `CallScreeningService` is enabled in system settings
- Check that permissions are granted
- Review logs in `CallScreeningService.onScreenCall()`
- Verify database has rules/settings (empty database = no filtering)

### Performance Issues (Slow UI)
- Run Compose metrics: `./gradlew assembleRelease`
- Check for unstable composables: `android/app/build/compose_metrics/composables.txt`
- See `tools/metrics-analysis/README.md` for optimization tips

---

## Contributing

1. **Fork or branch** from `consumer-v1`
2. **Make your changes** in a feature branch
3. **Test thoroughly** (local device/emulator + unit tests)
4. **Push to GitHub**
5. **Create a Pull Request** with clear description
6. **Wait for review** before merging

All commits to `consumer-v1` require:
- ✅ Code review
- ✅ Tests passing
- ✅ No merge conflicts

---

## Resources & Documentation

- **Android Developer Docs**: https://developer.android.com
- **Kotlin Language**: https://kotlinlang.org
- **Room Database Guide**: https://developer.android.com/training/data-storage/room
- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **CallScreeningService API**: https://developer.android.com/reference/android/telecom/CallScreeningService

---

## Support & Issues

- **Report bugs**: Open an issue on GitHub
- **Suggest features**: Discussions tab on GitHub
- **Ask questions**: Check existing issues first, then open a new discussion

---

## License & Ownership

**Owner**: avignonbo-jpg  
**Repository**: https://github.com/avignonbo-jpg/Signal-Gate-Pulse  
**Visibility**: Public  

See repository settings for license details.

---

**Last Updated**: October 1, 2026  
**Branch**: `consumer-v1` (default)  
**Maintained By**: avignonbo-jpg

---

## Quick Reference

| Need | Command |
|------|---------|
| Clone | `git clone --branch consumer-v1 https://github.com/avignonbo-jpg/Signal-Gate-Pulse.git` |
| Switch to consumer-v1 | `git checkout consumer-v1` |
| Build debug APK | `./gradlew assembleDebug` |
| Run app | `./gradlew installDebug` (then open from Android Studio) |
| Run tests | `./gradlew test` |
| Check git history | `git log --oneline` |
| Push changes | `git push origin feature/my-feature` |
| View remote branches | `git branch -r` |
