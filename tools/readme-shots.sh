#!/usr/bin/env bash
# The README screenshots, rendered on a device rather than tapped out by hand.
#
#   ./gradlew :app:installDebug :app:installDebugAndroidTest
#   ./tools/readme-shots.sh
#   python3 tools/frame-shots.py
#
# One size only, 1080x2400 at 420 dpi, which is the phone Play sorts screenshots into and
# the shape the framed artwork is cut for. The store set has its own script because it is
# shot at three sizes and answers to different rules; see tools/store-shots.sh.
set -euo pipefail

cd "$(dirname "$0")/.."
ADB=${ADB:-adb}
PKG=com.sendoku.app
OUT=${OUT:-docs/shots}
REMOTE=/sdcard/Android/data/$PKG/files/readme

restore() {
  $ADB shell wm size reset >/dev/null 2>&1 || true
  $ADB shell wm density reset >/dev/null 2>&1 || true
}
trap restore EXIT

$ADB shell wm size 1080x2400 >/dev/null
$ADB shell wm density 420 >/dev/null
$ADB shell rm -rf "$REMOTE" >/dev/null 2>&1 || true

$ADB shell am instrument -w \
  -e annotation com.sendoku.app.StoreShot \
  -e class com.sendoku.app.ui.ReadmeShotsTest \
  $PKG.test/androidx.test.runner.AndroidJUnitRunner | tail -4

mkdir -p "$OUT"
$ADB pull "$REMOTE/." "$OUT" >/dev/null
echo "pulled $(ls "$OUT"/*.png | wc -l) shots into $OUT"
