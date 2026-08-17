#!/usr/bin/env python3
"""
SignalGate Pulse — Jetpack Compose Compiler Metrics Analyzer
High-Precision Performance & Stability Analysis for Security UI

This tool is critical for maintaining smooth, reliable, and secure UI behavior
in the Call Screening Overlay and Operational Dashboard.
"""

import json
import sys
import re
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Any


class ComposeMetricsAnalyzer:
    """High-precision analyzer focused on UI stability for security applications."""

    def __init__(self, metrics_dir: str):
        self.metrics_dir = Path(metrics_dir)
        # Compose compiler 1.5.x generates prefixed filenames (e.g. app_debug-classes.txt).
        # Resolve the actual file paths by searching for the suffix pattern first,
        # then falling back to the legacy unprefixed names.
        self.classes_file = self._resolve_file("classes.txt")
        self.composables_file = self._resolve_file("composables.txt")
        self.metrics_file = self._resolve_file("composables-metrics.txt")
        
        # Security-relevant UI components to watch closely.
        # These match confirmed Composable names in the Pulse consumer branch.
        # "CallOverlay" removed — no such Composable exists in the current codebase.
        # Update this set whenever a new security-surface screen is added.
        self.critical_components = {
            "Shield", "Dashboard", "DigestScreen", "PendingCard",
            "OnboardingWizard", "PermissionSettings", "RiskIndicator"
        }

    def _resolve_file(self, suffix: str) -> Path:
        """Return the first file in metrics_dir whose name ends with the given suffix.

        Compose compiler 1.5.x prefixes output files with the module/variant name
        (e.g. ``app_debug-classes.txt``).  This helper locates the file regardless
        of the prefix so the analyzer works with both naming conventions.
        """
        # Exact match (legacy / unprefixed)
        exact = self.metrics_dir / suffix
        if exact.exists():
            return exact
        # Prefix-agnostic glob: any file ending with "-<suffix>" or exactly "<suffix>"
        stem = Path(suffix).stem   # e.g. "classes"
        ext  = Path(suffix).suffix  # e.g. ".txt"
        candidates = list(self.metrics_dir.glob(f"*-{stem}{ext}"))
        if candidates:
            return candidates[0]
        # Return the expected path even if missing so validate() can report it
        return exact

    def validate(self) -> bool:
        """Validate metrics directory and files."""
        if not self.metrics_dir.exists():
            print(f"❌ [ERROR] Metrics directory not found: {self.metrics_dir}")
            return False

        missing = [f for f in [self.classes_file, self.composables_file] if not f.exists()]
        if missing:
            # Distinguish between a genuinely empty directory (compiler flags not applied)
            # and a directory that exists but is simply missing specific expected files.
            # Both are build configuration failures, not stability findings — fail loudly
            # rather than reporting 0% skippable as if it were a real stability result.
            dir_contents = list(self.metrics_dir.iterdir()) if self.metrics_dir.exists() else []
            if not dir_contents:
                print("❌ [CONFIG ERROR] Metrics directory is empty.")
                print("   The Compose compiler did not emit any output files.")
                print("   This is a build configuration problem, not a stability finding.")
                print("   Fix: pass -PcomposeCompilerReports=true -PcomposeCompilerMetrics=true")
                print("   to Gradle, or run: scripts/analyze-compose-metrics.sh --pulse")
                print("   Do NOT interpret 0% skippable as a real result — no data was collected.")
            else:
                print(f"❌ [CONFIG ERROR] Expected metrics files not found in {self.metrics_dir}")
                print(f"   Directory contains: {[f.name for f in dir_contents]}")
                print(f"   Missing: {[f.name for f in missing]}")
                print("   This usually means the Compose compiler version changed its output")
                print("   filename format. Check _resolve_file() and update the suffix patterns.")
            return False

        print(f"✅ Metrics directory validated: {self.metrics_dir}")
        return True

    def parse_classes(self) -> Dict[str, Any]:
        """Parse class stability with focus on security-related classes."""
        result: Dict[str, Any] = {"stable": [], "unstable": [], "total": 0, "critical_unstable": []}

        if not self.classes_file.exists():
            return result

        with open(self.classes_file, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if not line or "class:" not in line:
                    continue

                result["total"] += 1

                if "UNSTABLE class:" in line:
                    class_name = line.split("UNSTABLE class:")[-1].strip().split()[0]
                    result["unstable"].append(class_name)
                    
                    # Flag critical security UI components
                    if any(crit in class_name for crit in self.critical_components):
                        result["critical_unstable"].append(class_name)

                elif "STABLE class:" in line:
                    class_name = line.split("STABLE class:")[-1].strip().split()[0]
                    result["stable"].append(class_name)

        return result

    def parse_composables(self) -> Dict[str, Any]:
        """Parse composables with emphasis on recomposition risks."""
        result: Dict[str, Any] = {
            "skippable": [],
            "not_skippable": [],
            "total": 0,
            "critical_not_skippable": []
        }

        if not self.composables_file.exists():
            return result

        with open(self.composables_file, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if not line or "composable:" not in line.lower():
                    continue

                result["total"] += 1

                # Extract composable name
                match = re.search(r'composable:\s*([^,]+)', line)
                name = match.group(1).strip() if match else "Unknown"

                if "skippable: false" in line:
                    result["not_skippable"].append(name)
                    if any(crit in name for crit in self.critical_components):
                        result["critical_not_skippable"].append(name)
                else:
                    result["skippable"].append(name)

        return result

    def calculate_metrics(self, classes: Dict, composables: Dict) -> Dict[str, Any]:
        """Calculate key performance and stability metrics."""
        total_comp = composables["total"]
        skippable = len(composables["skippable"])

        return {
            "total_composables": total_comp,
            "skippable_composables": skippable,
            "skippable_percentage": round((skippable / total_comp * 100), 2) if total_comp > 0 else 0.0,
            "not_skippable_composables": len(composables["not_skippable"]),
            "total_classes": classes["total"],
            "unstable_classes": len(classes["unstable"]),
            "critical_unstable": len(classes.get("critical_unstable", [])),
            "critical_not_skippable": len(composables.get("critical_not_skippable", [])),
            "stability_percentage": round(((classes["total"] - len(classes["unstable"])) / classes["total"] * 100), 2) 
                                   if classes["total"] > 0 else 0.0,
        }

    def _get_recommendations(self, classes: Dict, composables: Dict, metrics: Dict) -> List[str]:
        """Generate high-priority, security-aware recommendations."""
        recs = []

        # Only fire skippability warnings when real data was collected.
        # A zero result here always means the compiler flags were not applied —
        # validate() already caught and reported that case.
        if metrics["total_composables"] == 0:
            recs.append("⚠️  No composables found — see config error above. No stability conclusions can be drawn.")
            return recs

        if metrics["skippable_percentage"] < 92:
            recs.append(
                f"🔴 CRITICAL: Skippable composables only {metrics['skippable_percentage']}% "
                f"(Target: ≥92% for smooth screening performance)"
            )

        if metrics["critical_not_skippable"]:
            recs.append(
                f"🛡️  SECURITY UI RISK: {len(metrics['critical_not_skippable'])} "
                f"critical composables are not skippable — recomposition during a call decision is high-risk"
            )

        if metrics["critical_unstable"]:
            recs.append(
                f"⚠️  HIGH IMPACT: {len(metrics['critical_unstable'])} unstable classes "
                f"in security-critical UI ({', '.join(classes['critical_unstable'][:3])})"
            )

        if metrics["unstable_classes"] > 8:
            recs.append(
                "💡 Consider @Stable/@Immutable on data models and remember() for expensive objects"
            )

        if not recs:
            recs.append("✅ No critical stability issues found. Keep monitoring on each release.")

        return recs

    def print_report(self, analysis: Dict):
        """Print detailed, professional report."""
        if not analysis:
            return

        m = analysis["metrics"]
        print("\n" + "="*80)
        print("🔐 SIGNALGATE PULSE — COMPOSE METRICS ANALYSIS")
        print("="*80)

        print(f"\n📊 COMPOSABLES STATS")
        print(f"   Total Composables      : {m['total_composables']}")
        print(f"   Skippable              : {m['skippable_composables']} ({m['skippable_percentage']}%)")
        print(f"   Not Skippable          : {m['not_skippable_composables']}")

        print(f"\n🔧 CLASS STABILITY")
        print(f"   Total Classes          : {m['total_classes']}")
        print(f"   Unstable Classes       : {m['unstable_classes']}")
        print(f"   Critical Unstable      : {m.get('critical_unstable', 0)}")

        if analysis.get("recommendations"):
            print(f"\n🚨 RECOMMENDATIONS")
            for rec in analysis["recommendations"]:
                print(f"   {rec}")

        print("\n" + "="*80 + "\n")

    def save_json(self, analysis: Dict, output_path: str):
        output_file = Path(output_path)
        output_file.parent.mkdir(parents=True, exist_ok=True)
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(analysis, f, indent=2, ensure_ascii=False)
        print(f"💾 JSON report saved: {output_file}")

    def analyze(self) -> Dict[str, Any]:
        """Perform full analysis of the metrics."""
        if not self.validate():
            # Return a sentinel so main() can exit with a non-zero code, making
            # a config failure visible in CI rather than silently green with 0% data.
            return {"_config_error": True}

        classes = self.parse_classes()
        composables = self.parse_composables()
        metrics = self.calculate_metrics(classes, composables)
        recommendations = self._get_recommendations(classes, composables, metrics)

        return {
            "timestamp": datetime.now().isoformat(),
            "metrics": metrics,
            "classes": classes,
            "composables": composables,
            "recommendations": recommendations
        }


def main():
    if len(sys.argv) < 2:
        print("Usage: python3 analyze_metrics.py <metrics_dir> [--json output.json]")
        sys.exit(1)

    metrics_dir = sys.argv[1]
    output_json = None

    if "--json" in sys.argv:
        idx = sys.argv.index("--json")
        if idx + 1 < len(sys.argv):
            output_json = sys.argv[idx + 1]

    analyzer = ComposeMetricsAnalyzer(metrics_dir)
    analysis = analyzer.analyze()

    if analysis.get("_config_error"):
        # validate() already printed the actionable error — just exit non-zero
        # so the CI step fails visibly instead of reporting green with zero data.
        sys.exit(1)
    elif analysis:
        analyzer.print_report(analysis)
        if output_json:
            analyzer.save_json(analysis, output_json)
    else:
        print("❌ Analysis failed — no output produced")
        sys.exit(1)


if __name__ == "__main__":
    main()
