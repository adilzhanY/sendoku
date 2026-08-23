#!/usr/bin/env bash
# What the release artefact has to be true about itself before it goes anywhere.
#
# Size, native libraries and 16 KB page alignment. Google starts rejecting apps whose
# native libraries are not 16 KB aligned, and the two we have arrive through AndroidX
# rather than by choice, so this is worth re-checking whenever a dependency moves.
set -euo pipefail

cd "$(dirname "$0")/.."
APK=app/build/outputs/apk/release/app-release-unsigned.apk
AAB=app/build/outputs/bundle/release/app-release.aab
LIMIT_MB=10

[ -f "$APK" ] || { echo "No release APK. Run ./gradlew :app:assembleRelease first."; exit 1; }

apk_bytes=$(stat -c %s "$APK")
echo "APK $((apk_bytes / 1024)) KB"
[ -f "$AAB" ] && echo "AAB $(( $(stat -c %s "$AAB") / 1024 )) KB"
if [ "$apk_bytes" -gt $((LIMIT_MB * 1024 * 1024)) ]; then
  echo "FAIL: over ${LIMIT_MB} MB"
  exit 1
fi

echo
echo "Native libraries"
libs=$(unzip -l "$APK" | awk '/lib\/.*\.so$/ {print $4}')
if [ -z "$libs" ]; then
  echo "  none"
else
  work=$(mktemp -d)
  trap 'rm -rf "$work"' EXIT
  unzip -q "$APK" 'lib/*' -d "$work"
  fail=0
  for so in $(echo "$libs" | grep '^lib/arm64-v8a/'); do
    aligns=$(readelf -lW "$work/$so" | awk '/LOAD/ {print $NF}' | sort -u)
    ok=yes
    for a in $aligns; do
      [ $((a)) -ge 16384 ] || ok=no
    done
    echo "  $so LOAD alignment $(echo "$aligns" | tr '\n' ' ') $([ $ok = yes ] && echo OK || echo "TOO SMALL, needs 0x4000")"
    [ $ok = yes ] || fail=1
  done
  [ $fail -eq 0 ] || exit 1
fi

echo
zipalign=$(ls "${ANDROID_HOME:-$HOME/Android/Sdk}"/build-tools/*/zipalign | tail -1)
if "$zipalign" -c -P 16 -v 4 "$APK" >/dev/null 2>&1; then
  echo "Zip alignment: 16 KB OK"
else
  echo "Zip alignment: FAIL"
  exit 1
fi
