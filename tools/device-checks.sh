#!/usr/bin/env bash
# The device passes that cannot be automated into a unit test.
#
# Runs against whatever is attached. Most of it works by overriding the display size and
# density rather than booting a separate emulator for every form factor: a tablet is a phone
# with more pixels as far as the layout is concerned, and booting five virtual devices to
# learn that costs half an hour and a lot of somebody's laptop.
#
# The one thing this cannot fake is an older Android. See the notes at the end.
set -euo pipefail

ADB=${ADB:-adb}
PKG=com.sendoku.app

restore() {
  $ADB shell wm size reset >/dev/null 2>&1 || true
  $ADB shell wm density reset >/dev/null 2>&1 || true
  $ADB shell cmd uimode night no >/dev/null 2>&1 || true
  $ADB shell cmd connectivity airplane-mode disable >/dev/null 2>&1 || true
}
trap restore EXIT

shot() {
  mkdir -p "${OUT:-shots}"
  $ADB exec-out screencap -p > "${OUT:-shots}/$1.png"
  echo "  captured $1"
}

launch() {
  $ADB shell am force-stop $PKG
  $ADB shell am start -n $PKG/.MainActivity >/dev/null
  sleep 4
}

echo "Form factors"
for entry in "phone:1080x2400:420" "small:720x1280:320" "tablet:1600x2560:320" "foldout:2208x1840:420" "foldshut:1080x2092:420"; do
  name=${entry%%:*}; rest=${entry#*:}; size=${rest%%:*}; density=${rest##*:}
  $ADB shell wm size "$size" >/dev/null
  $ADB shell wm density "$density" >/dev/null
  launch
  shot "form-$name"
done
restore

echo "Dark and light"
for mode in yes no; do
  $ADB shell cmd uimode night $mode >/dev/null
  launch
  shot "night-$mode"
done

echo "Offline"
$ADB shell cmd connectivity airplane-mode enable >/dev/null
sleep 2
launch
shot "airplane"
$ADB shell cmd connectivity airplane-mode disable >/dev/null

echo "Interrupted by a call"
launch
$ADB emu gsm call 5551234 >/dev/null 2>&1 || echo "  (no modem on this device, skipping)"
sleep 3
shot "call"
$ADB emu gsm cancel 5551234 >/dev/null 2>&1 || true
sleep 2
shot "after-call"

echo "Killed for memory"
launch
$ADB shell am kill $PKG || true
sleep 2
$ADB shell am start -n $PKG/.MainActivity >/dev/null
sleep 4
shot "after-kill"

echo
echo "Not covered here:"
echo "  minSdk 26. Needs a system image for API 26, which is a separate download."
echo "    sdkmanager 'system-images;android-26;google_apis;x86_64'"
echo "  A real hour of play. Start a puzzle, leave the phone, come back."
