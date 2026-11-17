# Amin Live TV

Amin Live TV 0.2.0 is a standalone Persian-language live television app for Android TV 8.0 and later. It runs under `com.enigma.aminlivetv`, upgrades the signed 0.1.0 release in place and can be installed alongside Amin Player.

## Install on a Xiaomi Android TV Box

1. Copy `Amin-Live-TV-v0.2.0.apk` to a USB drive and plug it into the box.
2. Open the drive from the box’s file manager and select the APK. If Android asks, allow the file manager to install unknown apps, then return to the APK and select **Install**. The setting’s name varies by model and firmware.
3. Open **Amin Live TV** from **Apps**. Choose **شبکه‌های داخلی**, **شبکه‌های خبری**, **شبکه‌های ورزشی** or **فیلم و سریال** and press OK on a station.
4. While watching, press Up for the next station and Down for the previous one. Press OK to show or hide the channel strip. Left and Right switch stations while the strip is visible. Back returns to the grid. **ادامهٔ تماشا** on the home screen resumes the last station that played successfully.

If the box cannot open the USB file, install with [Android Platform Tools](https://developer.android.com/tools/releases/platform-tools) and [Xiaomi’s developer-options instructions](https://www.mi.com/global/support/article/KA-06513/). Connect only to your own TV on your local network. Confirm the pairing prompt on the TV.

```sh
adb devices
adb -s <tv-device-id> install -r Amin-Live-TV-v0.2.0.apk
```

See [Xiaomi’s APK installation guide](https://www.mi.com/uk/support/article/KA-06405/).

## Build

Open this folder in Android Studio with JDK 17, Android SDK Platform 35, and Android Build Tools 35.0.0.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

On Windows, use `gradlew.bat`. The APK is in `app/build/outputs/apk/debug/`. The GitHub Actions workflow produces a development APK.

`Catalogue.kt` validates and stores the catalogue. `ChannelMonitor.kt` and `StreamProbe.kt` check channel health. `ChannelHistory.kt` controls visibility, scan priority and local history. `LivePlayer.kt` owns Media3 playback and failover, and `MainActivity.kt` owns the TV interface and resume state.

The supplied experimental APK uses an app-specific signing key. Use that same private key with a higher `versionCode` for in-place upgrades. Keep the key and its password in private storage. For a signed release build, set `AMIN_LIVE_TV_KEYSTORE`, `AMIN_LIVE_TV_STORE_PASSWORD`, `AMIN_LIVE_TV_KEY_ALIAS` and `AMIN_LIVE_TV_KEY_PASSWORD`, then run `./gradlew assembleRelease`. Android requires matching signatures for upgrades, so development APKs signed by a different key need a separate emulator or a fresh installation.

## Maintain the catalogue

[`channels.json`](channels.json) is the build input and the remote update file. The bundled revision is always available. On launch the app checks the following HTTPS location asynchronously and saves only a fully validated newer revision. The last valid cache is used when that location is unreachable. If a refresh arrives while browsing or watching, the refreshed list appears when the viewer returns home.

```
https://raw.githubusercontent.com/Skyspiriit/Amin-Live-TV/main/channels.json
```

The public GitHub endpoint requires no credentials on the TV. A first launch without a remote response uses the bundled catalogue.

- Increment `revision` for each published edit. Keep `schemaVersion` at 1 for this app.
- Keep existing IDs stable. Updates must retain every existing channel ID. To withdraw a channel with an expired feed, give it `unavailable` status, clear its stream list and record the check date.
- A single record may appear in multiple categories. The four category IDs are `domestic`, `news`, `sports` and `film`.
- Set `origin` to `domestic` for Iranian domestic services or `satellite` for other services. The News grid displays the domestic news records and BBC Persian. International news records stay in the JSON for maintenance.
- Use clear, broadcaster-authorized HTTPS streams with the formats `hls`, `dash` or `progressive`. List each separately checked source with its attribution, ISO date and a unique priority. Lower numbers are tried first.
- Set a channel to `verified` only after successful video playback. Leave it `unverified` when listed or when playable integration remains untested. Use `unavailable` for a tested feed that fails and `restricted` when access or permission is required.

The player tries the last source that rendered video on that device first. It switches sources after a fatal error, a 16-second startup allowance, or 24 seconds without playback progress. It waits for internet recovery and retries the interrupted source when Android reports a device-wide outage.

The app records a viewing session after 30 seconds of actual playback. Recent viewing carries more weight than older viewing when scheduling checks. All viewing and health data stays on the TV. One bounded check runs at a time, with up to six channel checks in ten minutes. Every third check gives an eligible least-recently-checked channel a turn. Checks pause during playback and while the app is in the background or offline. Confirmed failures and recovered channels appear when the category is next opened, so the grid stays steady while someone navigates.

[Discovery and verification report](VERIFICATION.md)
