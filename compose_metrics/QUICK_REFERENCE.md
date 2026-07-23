# Compose Metrics Quick Reference

## One-Line Commands

### Run Analysis
```bash
bash scripts/analyze-compose-metrics.sh --report
```

### View Latest Report
```bash
cat compose_metrics/reports/metrics_*/analysis.json | tail -1
```

### Compare Two Reports
```bash
diff compose_metrics/reports/metrics_OLD/classes.txt compose_metrics/reports/metrics_NEW/classes.txt
```

## Three Critical Questions

### 1. How many composables are skippable?
```bash
grep -c "skippable: true" android/app/build/compose_metrics/composables.txt
# Target: > 90%
```

### 2. What classes are unstable?
```bash
grep "UNSTABLE class:" android/app/build/compose_metrics/classes.txt | head -10
# These are your biggest problems
```

### 3. Where is this class used?
```bash
# Search your codebase
grep -r "class YourUnstableClass" android/app/src --include="*.kt"
grep -r "YourUnstableClass" android/app/src --include="*.kt"
```

## Three-Step Fix Pattern

### For Each Unstable Class:

1. **Make it immutable**
   ```kotlin
   // Before
   data class MyState(var count: Int = 0)
   
   // After
   @Immutable
   data class MyState(val count: Int = 0)
   ```

2. **Extract before passing to composable**
   ```kotlin
   // Before
   @Composable
   fun Screen(viewModel: MyViewModel) { }
   
   // After
   @Composable
   fun Screen(viewModel: MyViewModel) {
       val state by viewModel.state.collectAsState()
       ScreenContent(state)
   }
   ```

3. **Use keys in lists**
   ```kotlin
   LazyColumn {
       items(items, key = { it.id }) { item ->
           ItemCard(item)
       }
   }
   ```

## Red Flags in Metrics

| Red Flag | Means | Fix |
|----------|-------|-----|
| UNSTABLE class with `var` | Mutable = always recomposes | Change to `val` |
| `skippable: false` | Can't skip | Check parameters for unstable classes |
| High UNSTABLE % | Many problems | Start with most-used classes |
| Composable not in list | Compiler issues | Check syntax or nested definitions |

## Before/After Checklist

### Before Optimization
- [ ] Run: `bash scripts/analyze-compose-metrics.sh --report`
- [ ] Note skippable %: ____%
- [ ] Note unstable classes: ______

### After Fixing 1-2 Issues
- [ ] Run: `bash scripts/analyze-compose-metrics.sh --report`
- [ ] Note new skippable %: ____%
- [ ] Check if improved

### Success Indicators
- ✅ Skippable % went up
- ✅ Fewer UNSTABLE classes
- ✅ App feels smoother

## Performance Impact by Fix

| Fix | Impact | Effort |
|-----|--------|--------|
| Make data class immutable | ⭐⭐⭐⭐⭐ | ⭐ |
| Extract ViewModel state | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| Add keys to LazyColumn | ⭐⭐⭐⭐ | ⭐ |
| Use remember for lambdas | ⭐⭐⭐ | ⭐⭐ |

## Python Analyzer Usage

```bash
# Basic analysis
python3 tools/metrics-analysis/analyze_metrics.py \
  android/app/build/compose_metrics/

# Save as JSON
python3 tools/metrics-analysis/analyze_metrics.py \
  android/app/build/compose_metrics/ \
  --json report.json

# Show only unstable classes
python3 tools/metrics-analysis/analyze_metrics.py \
  android/app/build/compose_metrics/ | grep -i unstable
```

## Git Workflow

### Save your analysis
```bash
# Before optimization
cp -r android/app/build/compose_metrics \
  compose_metrics/reports/baseline_$(date +%Y%m%d)

# After optimization
cp -r android/app/build/compose_metrics \
  compose_metrics/reports/optimized_$(date +%Y%m%d)
```

### Compare improvements
```bash
diff compose_metrics/reports/baseline_*/classes.txt \
     compose_metrics/reports/optimized_*/classes.txt
```

## Emergency Fixes (Quick Wins)

### In 5 Minutes
1. Find top unstable class
2. Add `@Immutable` annotation
3. Rebuild and re-run metrics
4. Check if improved

### In 15 Minutes
1. Find composable with `skippable: false`
2. Check its parameters
3. Extract unstable ones to parent
4. Pass only stable data

### In 30 Minutes
1. Profile composables with Layout Inspector
2. Extract most-recomposed ones
3. Make them skippable
4. Verify with metrics

## Remember

- 🎯 **Target >90% skippable** - That's excellent Compose performance
- 🏗️ **Build first, optimize second** - Get metrics baseline first
- 🔄 **Small iterations** - Fix one thing, measure, repeat
- 📊 **Track progress** - Save reports to see improvements
- ⚡ **Focus on hot paths** - Optimize what users see most

---

**For Signal-Gate-Multi-Path**
**Status: Ready to Optimize**
