#!/usr/bin/env bash
# Puts Sendoku on a device or emulator and opens it, for testing it by hand.
#
#   ./tools/run-app.sh            # build, install, launch
#   ./tools/run-app.sh --release  # the shrunk build instead, if it is signed
#
# This exists because the app does not stay installed. Every test task that talks to a
# device, `connectedDebugAndroidTest` above all, installs the app, runs, and uninstalls it
# again, so a run of the suite empties the app drawer. That looks exactly like a build
# failure from the drawer and is not one, and the fix is always the same three commands.
#
# It boots the emulator if nothing is attached, so it can be the only thing you run.
set -euo pipefail

cd "$(dirname "$0")/.."
SDK=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}
ADB=${ADB:-$SDK/platform-tools/adb}
EMULATOR=${EMULATOR:-$SDK/emulator/emulator}
AVD=${AVD:-sendoku}
PKG=com.sendoku.app
VARIANT=debug
[ "${1:-}" = "--release" ] && VARIANT=release

# JDK 26 is the system default on this machine and Gradle will not use it.
export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk}

if [ -z "$($ADB devices | sed -n '2p')" ]; then
  echo "no device attached, starting the $AVD emulator"
  "$EMULATOR" -avd "$AVD" -no-boot-anim >/dev/null 2>&1 &
  $ADB wait-for-device
fi

# wait-for-device returns as soon as adb can talk to it, which is long before the launcher
# exists. Installing into a half booted device fails in ways that read as a broken APK.
until [ "$($ADB shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
  sleep 1
done

./gradlew ":app:install${VARIANT^}" -q

# An instrumentation APK left over from a test run shadows nothing, but it does clutter the
# drawer with a second entry on some launchers.
$ADB uninstall "$PKG.test" >/dev/null 2>&1 || true

$ADB shell am force-stop "$PKG" >/dev/null 2>&1 || true
# Named rather than resolved by intent filter. LeakCanary registers a launcher activity of
# its own in the debug build, and letting the launcher pick opens that instead of the game.
$ADB shell am start -n "$PKG/.MainActivity" >/dev/null

until $ADB shell dumpsys activity activities 2>/dev/null | grep -q "topResumedActivity.*$PKG/.MainActivity"; do
  sleep 1
done
echo "Sendoku is installed and open ($VARIANT). It stays there until a device test run removes it."
