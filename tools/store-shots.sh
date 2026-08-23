#!/usr/bin/env bash
# The eight store screenshots, at the three sizes Play asks for.
#
# It renders them on the device with an instrumented test rather than tapping through the
# app, so the same picture comes out every time and the tablet shots are the same screens as
# the phone ones rather than whatever the tapping happened to reach.
#
#   ./gradlew :app:installDebug :app:installDebugAndroidTest
#   ./tools/store-shots.sh
set -euo pipefail

cd "$(dirname "$0")/.."
ADB=${ADB:-adb}
PKG=com.sendoku.app
OUT=${OUT:-branding/screenshots}
REMOTE=/sdcard/Android/data/$PKG/files/store

restore() {
  $ADB shell wm size reset >/dev/null 2>&1 || true
  $ADB shell wm density reset >/dev/null 2>&1 || true
}
trap restore EXIT

# name : resolution : density. The three buckets Play sorts screenshots into.
for entry in "phone:1080x2400:420" "tablet7:1200x1920:240" "tablet10:1600x2560:320"; do
  name=${entry%%:*}; rest=${entry#*:}; size=${rest%%:*}; density=${rest##*:}
  echo "$name $size at $density dpi"
  $ADB shell wm size "$size" >/dev/null
  $ADB shell wm density "$density" >/dev/null
  $ADB shell rm -rf "$REMOTE" >/dev/null 2>&1 || true

  $ADB shell am instrument -w \
    -e annotation com.sendoku.app.StoreShot \
    -e class com.sendoku.app.ui.StoreShotsTest \
    $PKG.test/androidx.test.runner.AndroidJUnitRunner | tail -4

  mkdir -p "$OUT/$name"
  $ADB pull "$REMOTE/." "$OUT/$name" >/dev/null
  echo "  pulled $(ls "$OUT/$name" | wc -l) files"
done

restore
echo
echo "Checking for metadata"
python3 - "$OUT" <<'PY'
import os, struct, sys
allowed = {"IHDR", "PLTE", "bKGD", "IDAT", "IEND", "tRNS", "sRGB", "gAMA", "pHYs"}
bad = []
for root, _, files in os.walk(sys.argv[1]):
    for name in files:
        if not name.endswith(".png"):
            continue
        path = os.path.join(root, name)
        data = open(path, "rb").read()
        offset, chunks = 8, []
        while offset < len(data):
            length = struct.unpack(">I", data[offset:offset + 4])[0]
            chunks.append(data[offset + 4:offset + 8].decode("latin1"))
            offset += 12 + length
        extra = [c for c in chunks if c not in allowed]
        if extra:
            bad.append((path, extra))
for path, extra in bad:
    print("  METADATA", path, extra)
print("  clean" if not bad else "  FAILED")
sys.exit(1 if bad else 0)
PY
