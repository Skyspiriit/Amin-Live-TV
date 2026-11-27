#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source .codex/android-env.sh
unset AMIN_LIVE_TV_KEYSTORE AMIN_LIVE_TV_STORE_PASSWORD AMIN_LIVE_TV_KEY_PASSWORD AMIN_LIVE_TV_KEY_ALIAS
# A physical TV must never be selected accidentally by a development check.
amin_tv_serial="${ANDROID_SERIAL:-emulator-5554}"
[[ "$amin_tv_serial" == emulator-* ]] || { echo "Select an emulator explicitly." >&2; exit 1; }
export ANDROID_SERIAL="$amin_tv_serial"
[[ "$(adb -s "$ANDROID_SERIAL" shell getprop sys.boot_completed | tr -d '\r')" == 1 ]]
mkdir -p app/build/outputs/tv-checks
trap 'adb -s "$ANDROID_SERIAL" logcat -d -t 1500 > app/build/outputs/tv-checks/logcat.txt || true; adb -s "$ANDROID_SERIAL" pull /sdcard/Android/data/com.enigma.aminlivetv.qa/files/screenshots app/build/outputs/tv-checks/ >/dev/null 2>&1 || true' EXIT
bash ./gradlew --no-daemon --max-workers=4 connectedDebugAndroidTest
