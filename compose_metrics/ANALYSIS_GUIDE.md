# Compose Metrics Analysis Guide

Complete guide for analyzing Jetpack Compose performance metrics in **Signal-Gate-Multi-Path**.

## 📊 What Are Compose Metrics?

The Jetpack Compose Compiler generates three key reports that tell you:

1. **Which classes are unstable** (cause unnecessary recompositions)
2. **Which composables can't be skipped** (even when parameters haven't changed)
3. **Performance statistics** (how expensive each composable is)

These live in: `android/app/build/compose_metrics/`

## 🚀 Getting Started (Your First Analysis)

### Step 1: Run a Release Build
```bash
cd android
./gradlew :app:assembleRelease
```

### Step 2: Find Your Reports
```
android/app/build/compose_metrics/
├── classes.txt              ← Class stability analysis
├── composables.txt          ← Which composables can skip
└── composables-metrics.txt  ← Performance stats
```

### Step 3: Use the Analysis Script
```bash
bash scripts/analyze-compose-metrics.sh --report
```

This generates a nice summary showing:
- How many composables are skippable (target: >90%)
- How many classes are unstable
- Top 5 problem areas to fix

## 📋 Reading the Reports

### `classes.txt` - Find Unstable Classes

**Example output:**
```
STABLE class: SignalData
  properties:
    - frequency: Int
    - amplitude: Double

UNSTABLE class: SignalViewModel
  mutable properties:
    - _uiState: MutableState<SignalUiState>
    - selectedSignals: MutableList<Signal>
```

**What this means:**
- ✅ `SignalData` is immutable → Can be optimized
- ❌ `SignalViewModel` has mutable state → Always causes recompositions

**Action items:**
1. Search your codebase for each UNSTABLE class
2. Check where they're used in composables
3. If used as a parameter, that composable will never skip!

### `composables.txt` - Find Non-Skippable Composables

**Example output:**
```
composable: SignalScreen
  restartable: true
  skippable: false
  parameters:
    - viewModel: SignalViewModel (UNSTABLE)

composable: FrequencyDisplay
  restartable: true
  skippable: true
  parameters:
    - frequency: Int (STABLE)
```

**What this means:**
- `SignalScreen` **cannot be skipped** because it takes an unstable ViewModel
- `FrequencyDisplay` **can be skipped** because it only takes a stable Int

**Target state:**
- ✅ `restartable: true` - Composable can be interrupted and resumed
- ✅ `skippable: true` - Composable can skip if parameters haven't changed

## 🔧 Common Problems & Solutions

### Problem 1: Data Class with `var` (Mutable)

**Current code:**
```kotlin
// In SignalGateApp or similar
data class SignalState(
    var isRunning: Boolean = false,
    var selectedSignalId: String? = null,
    var signals: MutableList<Signal> = mutableListOf()
)

@Composable
fun SignalScreen(state: SignalState) { } // ❌ Unstable parameter = always recomposes
```

**Why it's a problem:**
- `var` properties are mutable
- Compose can't guarantee the object won't change
- So it recomposes every time, even if nothing changed

**Fix:**
```kotlin
// ✅ Use immutable data class
@Immutable
data class SignalState(
    val isRunning: Boolean = false,
    val selectedSignalId: String? = null,
    val signals: List<Signal> = emptyList()  // List, not MutableList
)

@Composable
fun SignalScreen(state: SignalState) { } // ✅ Now stable and skippable
```

### Problem 2: Passing Entire ViewModel

**Current code:**
```kotlin
@Composable
fun MainScreen(viewModel: MainViewModel) {
    // Using viewModel.uiState, viewModel.signals, etc.
    SignalList(viewModel)  // ❌ All composables non-skippable
}

@Composable
fun SignalList(viewModel: MainViewModel) { }
```

**Why it's a problem:**
- ViewModels always have mutable state
- Any composable using it will never skip
- Every state change causes all child composables to recompose

**Fix:**
```kotlin
@Composable
fun MainScreen(viewModel: MainViewModel) {
    // Extract only the data you need
    val signals by viewModel.signals.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Pass immutable data to child composables
    SignalList(signals, isLoading)
}

@Composable
fun SignalList(
    signals: List<Signal>,
    isLoading: Boolean
) { } // ✅ Now skippable
```

### Problem 3: Recreating Collections in Composables

**Current code:**
```kotlin
@Composable
fun SignalListScreen(viewModel: MainViewModel) {
    val signals by viewModel.signals.collectAsState()
    
    LazyColumn {
        items(signals) { signal ->
            SignalCard(signal, onSelect = { viewModel.selectSignal(it) })
        }
    }
}

@Composable
fun SignalCard(signal: Signal, onSelect: (String) -> Unit) { }
```

**Why it's a problem:**
- `onSelect` lambda is recreated each time parent recomposes
- `signal` is stable, but lambda is unstable
- So `SignalCard` can't skip even though signal is same

**Fix:**
```kotlin
@Composable
fun SignalListScreen(viewModel: MainViewModel) {
    val signals by viewModel.signals.collectAsState()
    val onSelect = remember { { id: String -> viewModel.selectSignal(id) } }
    
    LazyColumn {
        items(signals, key = { it.id }) { signal ->
            SignalCard(signal, onSelect)  // ✅ Now skippable
        }
    }
}

@Composable
fun SignalCard(signal: Signal, onSelect: (String) -> Unit) { }
```

## 📈 Optimization Checklist

- [ ] Run `bash scripts/analyze-compose-metrics.sh --report`
- [ ] Check top 3 unstable classes
- [ ] Find where these are used in composables
- [ ] Make data classes immutable (change `var` to `val`)
- [ ] Extract ViewModel state before passing to composables
- [ ] Add `key` parameter to `items()` in LazyColumn/LazyRow
- [ ] Use `remember` for lambdas and collections
- [ ] Re-run metrics to verify improvement
- [ ] Compare before/after percentages

## 🎯 Target Metrics

| Metric | Target | Current |
|--------|--------|---------|
| Skippable composables | >90% | ? |
| Stable classes | >95% | ? |
| Restartable composables | >95% | ? |

## 🔍 Deep Dive: Using Layout Inspector

Complement metrics with Android Studio's Layout Inspector:

1. **Run your app on emulator/device**
2. **Tools → Layout Inspector**
3. **Enable "Show Recompose Counts"**
4. **Interact with UI and watch recomposition counts**
5. **Compare with metrics analysis**

High recomposition counts for composables that should be skippable = Need optimization!

## 📚 Files in Your Project

```
Signal-Gate-Multi-Path/
├── tools/metrics-analysis/
│   ├── analyze_metrics.py       # Python analyzer
│   └── README.md                # Technical docs
├── scripts/
│   └── analyze-compose-metrics.sh # Automation
└── compose_metrics/
    ├── ANALYSIS_GUIDE.md        # This file
    └── reports/
        └── metrics_YYYY-MM-DD_HH-MM-SS/
            ├── classes.txt
            ├── composables.txt
            └── analysis.json
```

## 💡 Pro Tips

1. **Focus on hot paths** - Optimize composables that recompose most often, not everything
2. **Fix the root, not symptoms** - Usually one unstable class causes many issues
3. **Make small changes** - After each fix, re-run metrics to verify
4. **Use `@Immutable`** - Mark your data classes with this annotation from `androidx.compose.runtime`
5. **Extract early** - Extract complex state in parent screens before passing to children

## 🆘 Troubleshooting

### "Metrics directory not found"
```bash
# Make sure build completed successfully
cd android
./gradlew clean :app:assembleRelease

# Check if files exist
ls -la android/app/build/compose_metrics/
```

### "Python3 not found"
The script will still work but won't generate JSON reports. Install with:
```bash
# macOS
brew install python3

# Ubuntu/Debian
sudo apt install python3
```

### "No metrics generated"
Verify `android/app/build.gradle` has this configuration (should already be there):

```gradle
tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configureEach {
    kotlinOptions {
        freeCompilerArgs += [
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" + project.buildDir.absolutePath + "/compose_metrics",
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" + project.buildDir.absolutePath + "/compose_metrics"
        ]
    }
}
```

## 📖 Next Steps

1. **Run your first analysis**
2. **Read `classes.txt` and identify top 3 unstable classes**
3. **Find usage in your composables**
4. **Apply fixes from this guide**
5. **Re-run metrics and see improvement**

---

**Created For**: Signal-Gate-Multi-Path
**Status**: Ready to use
**Last Updated**: 2026-05-30
