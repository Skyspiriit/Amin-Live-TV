#!/usr/bin/env bash
# Upgrade and navigation check on an Android TV emulator, driven only with D-pad keys. Expects
# old.apk (3.8.0 from main) and new.apk (this branch), debug builds signed with the same key.
set -u
ui() { python3 .github/tv-check/ui.py "$@"; }
export OUT=tv-check-output
PKG=com.enigma.aminlivetv
mkdir -p $OUT
adb wait-for-device

echo "== 3.8.0 with saved viewing data"
adb install old.apk
ui launch; ui shot 00-old-home
adb shell am force-stop $PKG
# Saved state as 3.8.0 writes it: last station, its category, and a bookmark at 5:00 in
# هزارتو episode 1. Seeded because stations geo-restricted to Iran don't play on this runner.
video=$(python3 -c "import json;d=json.load(open('channels.json'));print(next(s for s in d['iranianSeries'] if s['id']=='hezartou')['episodes'][0]['videoId'])")
cat > viewing.xml <<X
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map><string name="watched-channel">persiana-family</string><string name="watched-category">film</string><string name="category">film</string></map>
X
cat > progress.xml <<X
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map><string name="hezartou/s1e1">{&quot;seriesId&quot;:&quot;hezartou&quot;,&quot;episodeId&quot;:&quot;s1e1&quot;,&quot;videoId&quot;:&quot;$video&quot;,&quot;seconds&quot;:300.0,&quot;completed&quot;:false,&quot;updated&quot;:1763700000000}</string></map>
X
for f in viewing progress; do adb push $f.xml /data/local/tmp/$f.xml >/dev/null; adb shell chmod 644 /data/local/tmp/$f.xml; done
adb shell run-as $PKG mkdir -p shared_prefs
adb shell run-as $PKG cp /data/local/tmp/viewing.xml shared_prefs/viewing.xml
adb shell run-as $PKG cp /data/local/tmp/progress.xml shared_prefs/iranian-series-progress.xml
ui launch; ui shot 01-old-home-with-data; ui texts

echo "== Upgrade to 3.9.0 in place"
adb install -r new.apk
adb shell dumpsys package $PKG | grep -E "versionName|versionCode" | tee -a $OUT/report.txt
ui launch; ui shot 10-home; ui texts
ui order "ادامهٔ تماشا" "پخش زنده" "فیلم و مستند" "سریال‌ها"
ui expect-text "پخش زنده · پرشیانا خانواده"
ui expect-text "سریال · هزارتو"
ui expect-focus "پخش زنده · پرشیانا خانواده"
for t in "شبکه‌های ایرانی" "اخبار جهان" "ورزش" "فیلم و سرگرمی" "فیلم سینمایی" "مستند و طبیعت" "علم و فضا" "موضوعات دیگر" "سریال ایرانی" "سریال خارجی"; do ui expect-text "$t"; done
for t in "مافیا و مسابقه" "حیات وحش" "شطرنج" "آیین و مداحی" "سریال ترکی"; do ui expect-no-text "$t"; done

echo "== D-pad walk down the home screen"
ui key DPAD_DOWN; ui key DPAD_DOWN; ui key DPAD_DOWN; ui key DPAD_DOWN; ui key DPAD_DOWN; ui shot 11-home-bottom
ui key DPAD_UP 5; ui focus

echo "== Documentaries tile and tabs"
ui open "مستند و طبیعت"; ui shot 20-documentaries; ui texts
ui expect-text "حیات وحش"; ui expect-text "طبیعت"
ui key DPAD_UP; ui expect-focus "مستند"
ui key DPAD_LEFT; ui expect-focus "حیات وحش"
ui key DPAD_CENTER; ui shot 21-wildlife-tab; ui texts
ui key BACK; ui expect-focus "مستند و طبیعت"
ui open "مستند و طبیعت"; ui expect-text "آفریقای وحشی"; ui key DPAD_UP; ui expect-focus "حیات وحش"; ui shot 22-reopens-last-tab
ui key DPAD_DOWN; ui key DPAD_CENTER; sleep 25; ui shot 23-wildlife-playing; ui texts
ui key BACK; ui focus; ui key BACK; ui expect-focus "مستند و طبیعت"

echo "== Other topics"
ui open "موضوعات دیگر"; ui shot 30-other-topics
for t in "مافیا و مسابقه" "سینما و فانتزی" "دنیای خودرو" "شطرنج" "آیین و مداحی"; do ui expect-text "$t"; done
ui key BACK; ui expect-focus "موضوعات دیگر"

echo "== Series"
ui open "سریال خارجی"; ui shot 40-foreign; ui expect-text "سایر سریال‌ها"
ui key BACK; ui expect-focus "سریال خارجی"
ui open "سریال ایرانی"; ui shot 41-iranian-library; ui expect-text "ادامهٔ تماشا · هزارتو"
ui key BACK; ui expect-focus "سریال ایرانی"
ui open "سریال · هزارتو"; sleep 30; ui shot 42-series-resumed; ui texts
ui key BACK; ui texts; ui key BACK; ui texts; ui key BACK; ui focus

echo "== Live TV grids"
ui launch
ui open "اخبار جهان"; ui shot 50-news; ui texts; ui key BACK; ui expect-focus "اخبار جهان"
ui open "فیلم و سرگرمی"; ui shot 51-film-order; ui texts; ui key BACK; ui expect-focus "فیلم و سرگرمی"
ui open "پخش زنده · "; sleep 25; ui shot 52-resumed-live; ui texts
ui key BACK; ui focus; ui key BACK; ui focus

echo "== Theme persists"
ui launch; ui open "رنگ‌ها"; ui key DPAD_LEFT 2; ui key DPAD_CENTER; ui shot 60-theme
ui launch; ui shot 61-theme-after-restart
adb shell run-as $PKG cat shared_prefs/appearance.xml | tee -a $OUT/report.txt
echo "== Saved data after the run"
adb shell run-as $PKG cat shared_prefs/viewing.xml | tee -a $OUT/report.txt
adb shell run-as $PKG cat shared_prefs/iranian-series-progress.xml | tee -a $OUT/report.txt
adb logcat -d -s AminMonitor AminYouTube AminSeries AminCatalogue AndroidRuntime > $OUT/logcat.txt
echo "FAIL count: $(grep -c '^FAIL' $OUT/report.txt)"
