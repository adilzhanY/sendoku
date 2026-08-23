#!/usr/bin/env bash
# Cold start, jank, and the baseline profile. All three come from the benchmark module.
#
# Numbers from an emulator are noisy and are worth reading as a trend rather than as truth.
# Run it on a real phone before believing a regression.
set -euo pipefail

cd "$(dirname "$0")/.."
export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk}

echo "Generating the baseline profile"
./gradlew :app:generateBaselineProfile

echo
echo "Measuring cold start and jank"
./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest

echo
echo "Results:"
find benchmark/build/outputs -name "*-benchmarkData.json" -newermt "-10 minutes" | while read -r file; do
  echo "  $file"
  python3 - "$file" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
for bench in data.get("benchmarks", []):
    name = bench["name"]
    for metric, values in sorted(bench.get("metrics", {}).items()):
        median = values.get("median")
        if median is not None:
            print(f"    {name} {metric}: {median:.1f}")
PY
done
