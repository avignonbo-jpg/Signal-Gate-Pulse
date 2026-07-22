# Jetpack Compose Metrics Analysis Guide

## Overview
This guide helps you understand and analyze Compose Compiler Metrics to identify and fix UI performance issues. The metrics are automatically generated during the build process and stored in `build/compose_metrics/`.

## Quick Start

### 1. Generate Metrics
Run a release build to generate metrics:
```bash
./gradlew assembleRelease
```

The compiler will generate metrics files in `android/app/build/compose_metrics/`

### 2. Analyze Reports
Three key files will be generated:

#### `classes.txt` - Class Stability Report
Lists all classes and their stability status:
- **STABLE**: Class is immutable and can be reliably skipped
- **UNSTABLE**: Class is mutable; composables using it will always recompose

**What to look for:**
```
STABLE class: MyDataClass
  immutable properties: id, name
  
UNSTABLE class: UserViewModel
  mutable property: _uiState (type: MutableState)
```

**Fix unstable classes** by:
- Making data classes immutable (val instead of var)
- Using `@Immutable` annotation from `androidx.compose.runtime`
- Wrapping mutable state in `State<>` objects

#### `composables.txt` - Composable Report
Shows which composables can be optimized:

```
composable: MyScreen(
  restartable: true
  skippable: true
  parameters:
    - id: String (stable)
    - viewModel: ViewModel (unstable)
)
```

**Optimal state**: `restartable=true, skippable=true`
- **Restartable**: Can resume from where it left off
- **Skippable**: Can skip recomposition if parameters haven't changed

#### `composables-metrics.txt` - Detailed Statistics
Contains aggregate performance data and call counts.

## Understanding the Metrics

### Recomposition Flow
1. State changes → Parent recomposes
2. Parent passes new parameters → Child evaluates
3. If parameters are STABLE and unchanged → Child is skipped ✅
4. If parameters are UNSTABLE → Child always recomposes ❌

### Common Issues & Fixes

#### Issue 1: Unstable Parameter in Composable
```kotlin
// ❌ UNSTABLE - Mutable class
data class UiState(
    var count: Int = 0,
    var items: MutableList<String> = mutableListOf()
)

@Composable
fun MyScreen(state: UiState) { } // Always recomposes
```

**Fix:**
```kotlin
// ✅ STABLE - Immutable class
data class UiState(
    val count: Int = 0,
    val items: List<String> = emptyList()
)

@Composable
fun MyScreen(state: UiState) { } // Can be skipped
```

#### Issue 2: Non-Skippable Composable
```kotlin
// ❌ NOT SKIPPABLE
@Composable
fun Counter(onIncrement: () -> Unit) { // Lambda captured
    Button(onClick = onIncrement) { }
}
```

**Fix:**
```kotlin
// ✅ SKIPPABLE
@Composable
fun Counter(
    count: Int,
    onIncrement: () -> Unit
) {
    Button(onClick = onIncrement) {
        Text("$count")
    }
}
// Use rememberUpdatedReference for callbacks in stable composables
```

#### Issue 3: ViewModel Always Unstable
```kotlin
// ❌ Always recomposes because ViewModel has mutable state
@Composable
fun MyScreen(viewModel: MyViewModel) { }
```

**Partial Fix:**
```kotlin
// ✅ Better - Extract only needed data
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    MyScreenContent(uiState) // Pass immutable data
}

@Composable
fun MyScreenContent(uiState: UiState) { } // Can be skipped
```

## Using the Analysis Scripts

### Python Script: `analyze_metrics.py`
Generates a summary report from raw metrics:

```bash
python3 tools/metrics-analysis/analyze_metrics.py \
  android/app/build/compose_metrics/
```

**Output includes:**
- Total composables analyzed
- Percentage skippable/restartable
- List of problematic unstable classes
- Recommendations for optimization

### Bash Script: `analyze-compose-metrics.sh`
Automates the full metrics generation pipeline:

```bash
bash scripts/analyze-compose-metrics.sh --report
```

**Does:**
1. Cleans previous metrics
2. Runs release build
3. Copies metrics to `compose_metrics/reports/`
4. Runs Python analysis
5. Shows formatted summary

## Performance Tips

### 1. Make Data Classes Immutable
```kotlin
// ✅ Good
@Immutable
data class MenuItem(
    val id: String,
    val label: String
)

// ❌ Bad - uses mutable defaults
data class MenuItem(
    val id: String,
    val label: String,
    val isSelected: Boolean = false
)
```

### 2. Extract Expensive Composables
```kotlin
// ❌ Recomposes entire list on any state change
@Composable
fun ListScreen(items: List<Item>) {
    LazyColumn {
        items(items) { item ->
            ExpensiveItemCard(item) // Recomposes all
        }
    }
}

// ✅ Each item can skip independently
@Composable
fun ListScreen(items: List<Item>) {
    LazyColumn {
        items(items, key = { it.id }) { item ->
            ListItem(item)
        }
    }
}

@Composable
fun ListItem(item: Item) { // Skippable!
    ExpensiveItemCard(item)
}
```

### 3. Use remember for Lambdas
```kotlin
@Composable
fun MyButton(onClick: (String) -> Unit) {
    val memoizedOnClick = remember(onClick) { onClick }
    Button(onClick = { memoizedOnClick("clicked") }) { }
}
```

### 4. Stabilize Collections
```kotlin
// ❌ Each recomposition creates new list (unstable)
@Composable
fun Screen() {
    val items = listOf("a", "b", "c")
    ItemList(items)
}

// ✅ Stable, doesn't recreate
@Composable
fun Screen() {
    val items = remember { listOf("a", "b", "c") }
    ItemList(items)
}
```

## Monitoring Over Time

Save metrics after each optimization:
```bash
mkdir -p compose_metrics/reports/$(date +%Y-%m-%d)
cp android/app/build/compose_metrics/* compose_metrics/reports/$(date +%Y-%m-%d)/
```

Compare reports to measure improvement:
```bash
diff compose_metrics/reports/2026-05-25/classes.txt \
    compose_metrics/reports/2026-05-26/classes.txt
```

## Next Steps

1. **Run the build** and check `android/app/build/compose_metrics/`
2. **Review `classes.txt`** - Find unstable classes
3. **Review `composables.txt`** - Find non-skippable composables
4. **Fix high-impact issues** - Start with frequently recomposed items
5. **Re-run metrics** - Verify improvements

## Resources

- [Jetpack Compose Performance](https://developer.android.com/jetpack/compose/performance)
- [Compose Compiler Metrics](https://developer.android.com/jetpack/compose/performance/compose-compiler-metrics)
- [Layout Inspector Guide](https://developer.android.com/studio/debug/layout-inspector)

---

**Last Updated**: 2026-05-26
**For Your Project**: Signal-Gate-Multi-Path
