#!/usr/bin/env bash
# Upgrade and navigation check on an Android TV emulator. Expects old.apk (3.8.0 from main) and
# new.apk (this branch), both debug builds signed with the same runner key.
set -u
ui() { python3 .github/tv-check/ui.py "$@"; }
export OUT=tv-check-output
mkdir -p $OUT
adb wait-for-device
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0

echo "== 3.8.0: create viewing data"
adb install old.apk
ui launch; ui shot 00-old-home; ui texts
ui tap "شبکه‌های داخلی"; ui shot 01-old-domestic
ui key DPAD_CENTER; sleep 45; ui shot 02-old-playing
ui key BACK; ui key BACK
ui tap "انتخاب سریال"; ui tap "هزارتو"; ui texts
ui tap "قسمت ۱"; sleep 60; ui shot 03-old-episode
ui key BACK; ui key BACK; ui key BACK
adb shell run-as com.enigma.aminlivetv ls shared_prefs | tee -a $OUT/report.txt

echo "== 3.9.0: upgrade in place"
adb install -r new.apk
adb shell dumpsys package com.enigma.aminlivetv | grep -E "versionName|versionCode" | tee -a $OUT/report.txt
ui launch; ui shot 10-home; ui texts
ui order "ادامهٔ تماشا" "پخش زنده" "فیلم و مستند" "سریال‌ها"
ui expect-text "پخش زنده · "
ui expect-text "سریال · هزارتو"
ui expect-focus "پخش زنده · "
for t in "شبکه‌های ایرانی" "اخبار جهان" "ورزش" "فیلم و سرگرمی" "فیلم سینمایی" "مستند و طبیعت" "علم و فضا" "موضوعات دیگر" "سریال ایرانی" "سریال خارجی"; do ui expect-text "$t"; done
for t in "مافیا و مسابقه" "حیات وحش" "شطرنج" "آیین و مداحی" "سریال ترکی"; do ui expect-no-text "$t"; done

echo "== D-pad order down the home screen"
ui key DPAD_DOWN; ui key DPAD_DOWN; ui key DPAD_LEFT 3; ui key DPAD_DOWN; ui key DPAD_DOWN; ui shot 11-home-bottom
ui key DPAD_UP 4; ui focus

echo "== Documentaries hub and tabs"
ui launch; ui tap "مستند و طبیعت"; ui shot 20-documentaries
ui expect-text "حیات وحش"; ui expect-text "طبیعت"
ui key DPAD_UP; ui expect-focus "مستند"
ui key DPAD_LEFT; ui expect-focus "حیات وحش"
ui key DPAD_CENTER; ui shot 21-wildlife-tab
ui key BACK; ui expect-focus "مستند و طبیعت"
ui tap "مستند و طبیعت"; ui key DPAD_UP; ui focus; ui shot 22-documentaries-reopened
ui key BACK

echo "== Other topics"
ui tap "موضوعات دیگر"; ui shot 30-other-topics
for t in "مافیا و مسابقه" "سینما و فانتزی" "دنیای خودرو" "شطرنج" "آیین و مداحی"; do ui expect-text "$t"; done
ui key BACK; ui expect-focus "موضوعات دیگر"

echo "== Series"
ui tap "سریال خارجی"; ui shot 40-foreign; ui expect-text "سایر سریال‌ها"
ui key BACK; ui expect-focus "سریال خارجی"
ui tap "سریال ایرانی"; ui shot 41-iranian-library; ui expect-text "ادامهٔ تماشا · هزارتو"
ui key BACK; ui expect-focus "سریال ایرانی"
ui tap "سریال · هزارتو"; sleep 30; ui shot 42-series-resumed; ui texts
ui key BACK; ui key BACK; ui key BACK; ui focus

echo "== Live TV and resume"
ui launch; ui tap "اخبار جهان"; ui shot 50-news; ui key BACK; ui expect-focus "اخبار جهان"
ui tap "فیلم و سرگرمی"; ui shot 51-film; ui texts; ui key BACK
ui tap "پخش زنده · "; sleep 30; ui shot 52-resumed-live
ui key BACK; ui focus; ui key BACK; ui focus

echo "== Theme persists"
ui tap "رنگ‌ها"; ui key DPAD_LEFT 2; ui key DPAD_CENTER; ui shot 60-theme
ui launch; ui shot 61-theme-after-restart
adb shell run-as com.enigma.aminlivetv cat shared_prefs/appearance.xml | tee -a $OUT/report.txt
adb logcat -d -s AminMonitor AminYouTube AminSeries AminCatalogue AndroidRuntime > $OUT/logcat.txt
grep -c "^FAIL" $OUT/report.txt || true
