#!/usr/bin/env bash
# Checks that a reinstall keeps a player's history.
#
# Needs a device or an emulator attached. Auto backup cannot be tested any other way: it is
# a platform service, and none of it runs in a unit test.
#
# Usage: tools/verify-backup.sh
set -euo pipefail

PACKAGE=com.sendoku.app

echo "Enabling the backup manager"
adb shell bmgr enable true
# The Google transport needs a signed in account. The local one needs nothing and exercises
# the same include rules, which is what is actually being tested here.
adb shell bmgr transport com.android.localtransport/.LocalTransport

echo
echo "Play a puzzle to the end and change a setting, then press enter."
read -r

echo "Backing up $PACKAGE"
adb shell bmgr backupnow "$PACKAGE"

echo "Uninstalling"
adb uninstall "$PACKAGE"

echo "Reinstalling"
./gradlew :app:installDebug

# Android restores automatically as part of the install when a backup exists, which is the
# path a real player takes when they set up a new phone. No explicit restore is needed.

echo
echo "Open the app. The finished puzzle should still be in the history and the setting"
echo "should still be changed. If either is gone, the include paths in"
echo "res/xml/backup_rules.xml and res/xml/data_extraction_rules.xml are wrong."
