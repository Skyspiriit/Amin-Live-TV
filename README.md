# Amin Live TV

Amin Live TV 3.9.0 is a standalone Persian-language live television app for Android TV 8.0 and later. It runs under `com.enigma.aminlivetv`, upgrades earlier signed releases in place and can be installed alongside Amin Player.

**Home screen (3.9.0).** Below the header, «ادامهٔ تماشا» resumes the last station and the latest series episode. Three sections follow, always in this order:

- **پخش زنده**: شبکه‌های ایرانی، اخبار جهان، ورزش، فیلم و سرگرمی.
- **فیلم و مستند**: فیلم سینمایی، مستند و طبیعت، علم و فضا، موضوعات دیگر.
- **سریال‌ها**: سریال ایرانی، سریال خارجی.

«مستند و طبیعت» and «موضوعات دیگر» open a grid with a tab for each group (مستند، حیات وحش، طبیعت, and مافیا و مسابقه، سینما و فانتزی، دنیای خودرو، شطرنج، آیین و مداحی). Press Up from the grid to reach the tabs. Tiles without anything to play are hidden.

Press «رنگ‌ها» to preview the six fantasy themes. The selected setting and colours are saved on the TV. See the [3.9 home screen and source review](docs/home-3.9.md), [earlier content checks](docs/entertainment-sources.md) and [artwork and design notes](docs/home-artwork.md).

## Install on a Xiaomi Android TV Box

1. Copy `Amin-Live-TV-v3.9.0.apk` to a USB drive and plug it into the box.
2. Open the drive from the box’s file manager and select the APK. If Android asks, allow the file manager to install unknown apps, then return to the APK and select **Install**. The setting’s name varies by model and firmware.
3. Open **Amin Live TV** from **Apps**. Choose a tile under **پخش زنده**, **فیلم و مستند** or **سریال‌ها**, and press OK.
4. While watching, press Up for the previous station and Down for the next one. Press OK to show or hide the channel strip. Left and Right switch stations while the strip is visible. Back returns to the grid. **ادامهٔ تماشا** on the home screen resumes the last station that played successfully when it is still available.

If the box cannot open the USB file, install with [Android Platform Tools](https://developer.android.com/tools/releases/platform-tools) and [Xiaomi’s developer-options instructions](https://www.mi.com/global/support/article/KA-06513/). Connect only to your own TV on your local network. Confirm the pairing prompt on the TV.

```sh
adb devices
adb -s <tv-device-id> install -r --user 0 Amin-Live-TV-v3.9.0.apk
```

Always pass `--user 0`. Without it, ADB installs the app into every Android user, including the hidden work profile on Xiaomi boxes. That second copy then hides the main copy's switch in **Install unknown apps**, and in-app updates can't be allowed. See [UPDATE-PERMISSION-3.3.md](UPDATE-PERMISSION-3.3.md).

See [Xiaomi’s APK installation guide](https://www.mi.com/uk/support/article/KA-06405/).

## Persian series

Open **سریال ایرانی** on the home screen and select a title and episode. **ادامهٔ تماشا** opens the saved episode. Choose **از ابتدا** to restart, **فهرست قسمت‌ها** to choose a different episode, or **قسمت بعد** when an episode ends. **قسمت‌های موجود** marks titles with some episodes unavailable. Amin Live TV stores positions from the embedded YouTube player on the device. If YouTube blocks embedding, **تماشا در یوتیوب** opens the episode in the official YouTube TV app, where Amin Live TV cannot track position.

The remote catalogue lists complete, consecutive runs under `iranianSeries`. Each entry has a stable `id`, a Persian `title` and an `episodes` array. Each episode has a stable `id`, a numeric `season` and `number`, and the exact YouTube `videoId`. Use `iranianSeriesInProgress` for a free, verified subset of a finished series (`"status": "partial"`) or an ongoing series (`"status": "ongoing"`). The app keeps the original episode numbers for those titles even when some are missing. Move a title to `iranianSeries` once the complete run is verified. Keep IDs stable when editing or moving a series. A replaced YouTube video starts from the beginning. [Series inventory and sources](docs/persian-series-inventory.md)

**Turkish series (3.9 and later).** `turkishSeries` and `turkishSeriesInProgress` use the same format and rules. Their IDs must not repeat an Iranian series ID. They appear under **سریال خارجی** › **سریال ترکی**, in the same episode screens, with Persian subtitles requested from YouTube's player. Older releases ignore them. The bundled catalogue has none yet. See the [source review](docs/home-3.9.md#turkish-series).

## App updates

The app checks the repository’s published GitHub releases in the background at startup. While browsing, it rechecks every six hours after a successful check and retries a failed check after 15 minutes. From 0.3.1, when a newer numbered release (tags `vX.Y.Z`; from 3.3 also `vX.Y`) has an APK, the app downloads it in the background. It installs the APK only if it is this app, newer, and signed with the same certificate. The home screen button then says «نسخهٔ … آماده است · برای نصب OK را بزنید». OK opens Android's install confirmation. Android puts the focus on **Cancel**, so press Right, then OK on **Install**. The first time only, the app opens Android's **Install unknown apps** page for itself (from 3.3). Turn on **Amin Live TV** and press Back. Android restarts the app after that change, and the install confirmation follows by itself. If the switch stays off, the app says so. Version 0.3.3 and earlier don't check first: Android shows its own prompt, choose **Settings**, turn it on, then reopen Amin Live TV. After an update installs, Android 11 returns to the TV's home screen. Open Amin Live TV again from there. Android doesn't let an app reopen itself. If a download fails, the button retries it. [GitHub releases](https://github.com/Skyspiriit/Amin-Live-TV/releases)

## Build

Open this folder in Android Studio with JDK 17, Android SDK Platform 35, and Android Build Tools 35.0.0.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

On Windows, use `gradlew.bat`. The APK is in `app/build/outputs/apk/debug/`. The GitHub Actions workflow produces a development APK.

`Catalogue.kt` validates and stores the catalogue. `YouTubePlayer.kt` plays official YouTube content and hands blocked embeds to the YouTube app. `AppUpdate.kt` downloads, checks and installs new releases. `ChannelMonitor.kt` and `StreamProbe.kt` check channel health. `ChannelHistory.kt` controls visibility, scan priority and local history. `LivePlayer.kt` owns Media3 playback and failover, and `MainActivity.kt` owns the TV interface and resume state.

**Signed releases.** Pushing a tag that matches `versionName`, such as `v0.3.0`, runs `.github/workflows/release.yml`. It runs the unit tests and lint, signs the APK with the release key, checks that its certificate matches the previous release, and publishes the release with the notes in `docs/releases/<tag>.md`. It needs four repository secrets: `AMIN_LIVE_TV_KEYSTORE_BASE64` (the keystore file in base64), `AMIN_LIVE_TV_STORE_PASSWORD`, `AMIN_LIVE_TV_KEY_ALIAS` and `AMIN_LIVE_TV_KEY_PASSWORD`.

The supplied experimental APK uses an app-specific signing key. Use that same private key with a higher `versionCode` for in-place upgrades. Keep the key and its password in private storage. For a signed release build, set `AMIN_LIVE_TV_KEYSTORE`, `AMIN_LIVE_TV_STORE_PASSWORD`, `AMIN_LIVE_TV_KEY_ALIAS` and `AMIN_LIVE_TV_KEY_PASSWORD`, then run `./gradlew assembleRelease`. Android requires matching signatures for upgrades, so development APKs signed by a different key need a separate emulator or a fresh installation.

## Maintain the catalogue

[`channels.json`](channels.json) is the build input and the remote update file. The bundled revision is always available. The app checks the following HTTPS location asynchronously at startup and every 15 minutes while browsing. It saves only fully validated newer revisions and uses the last valid cache when that location is unreachable. An open grid updates when a revision arrives. During playback, a new revision takes effect when the viewer returns to the grid.

```
https://raw.githubusercontent.com/Skyspiriit/Amin-Live-TV/main/channels.json
```

The public GitHub endpoint requires no credentials on the TV.

**Official YouTube playback.** Tiles marked «یوتیوب رسمی» play a broadcaster's own YouTube live broadcast or playlist in YouTube's embedded player inside the app. OK pauses, Up and Down change programme, and Back returns to the grid. When the owner doesn't allow embedding, the app opens that exact broadcast, or the playlist's newest video, in the official YouTube TV app instead. Back there returns to Amin Live TV. Tiles marked «سایت رسمی» open the broadcaster's live page in a browser. This only happens in apps older than 0.3.0, which don't read the YouTube fields. Unmarked tiles are native Media3 streams. No rehosted stream or copied token is used. See [VERIFICATION-0.3.0.md](VERIFICATION-0.3.0.md).

**Persian subtitles (0.3.3 and later).** Tiles in the documentary, wildlife, nature, science, movies and series groups turn on subtitles automatically. The app uses the video's own Persian captions if it has them. Otherwise it asks YouTube's player to translate the video's captions into Persian, preferring human-written English, then other human captions, then automatic captions. From 3.4 it also asks for automatic English captions directly, because the player doesn't switch those on by itself for most videos. Persian speech gets no subtitles. YouTube's player translates and draws the subtitles with the original timing, so nothing is downloaded or translated by the app. A video without captions plays without subtitles. Live news is unchanged: YouTube doesn't translate live captions. See [SUBTITLES-0.3.3.md](SUBTITLES-0.3.3.md) and [SUBTITLES-3.4.md](SUBTITLES-3.4.md).

- Increment `revision` for each published edit. Keep `schemaVersion` at 1 for this app.
- Keep existing IDs stable. Updates must retain every existing channel ID. To withdraw a channel with an expired feed, give it `unavailable` status, clear its stream list and record the check date.
- A single record may appear in multiple categories. The four category IDs are `domestic`, `news`, `sports` and `film`.
- A news record may add `youtubeChannel` (a `UC…` channel ID) to play that channel's current live broadcast. Keep `officialLiveUrl` for apps older than 0.3.0.
- The documentary groups live in the top-level `programmes` list, which apps older than 0.3.0 ignore. Each programme has an `id`, a Persian `name`, a `group` (`documentary`, `wildlife`, `nature` or `science`) and `sourceAttribution`. It also has either `channelRef` (the ID of a catalogue channel to play natively) or YouTube fields: `youtubeChannel` for a live broadcast and/or `youtubePlaylist` (a `PL…` or `UU…` ID). With both, the playlist is the fallback when nothing is live. Never put a group key in a channel's `categories`, because older apps would reject the whole catalogue.
- The `movies` and `series` groups (0.3.2 and later) go in a separate top-level `programmes2` list, using the same fields. Apps up to 0.3.1 ignore `programmes2`, but they reject the whole catalogue if `programmes` contains a group they don't know. From 0.3.2, an unknown group in either list is skipped. Before adding a playlist, check that its videos are full-length and play without signing in: age-restricted videos won't play on the TV. Also play it in the app itself: some owners allow embedding on websites but not in apps (error 150), and titles blocked in Portugal can move to the top of a playlist over time.
- From revision 3 onward, every `film` record needs a unique, nonnegative integer `filmOrder`. Use ranks in the 1000s for Persiana, 2000s for GEM and 3000s for other channels. Keep an existing channel's rank when its availability changes so it returns to the same place.
- Set `origin` to `domestic` for Iranian domestic services or `satellite` for other services. Only domestic news records and BBC Persian are eligible for the News grid. Like other stations, they need a trusted playable source to appear.
- Use clear, broadcaster-authorized HTTPS streams with the formats `hls`, `dash` or `progressive`. List each separately checked source with its attribution, ISO date and a unique priority. Lower numbers are tried first.
- Set a channel to `verified` only after successful video playback. Leave it `unverified` when listed or when playable integration remains untested. Use `unavailable` for a tested feed that fails and `restricted` when access or permission is required.

The player tries the last source that rendered video on that device first. It switches sources after a fatal error, a 16-second startup allowance, or 24 seconds without playback progress. It waits for internet recovery and retries the interrupted source when Android reports a device-wide outage.

The app records a viewing session after 30 seconds of actual playback. Recent viewing carries more weight than older viewing when scheduling checks. All viewing and health data stays on the TV. One bounded check runs at a time, with up to six channel checks in ten minutes. Every third check gives an eligible least-recently-checked channel a turn. Checks pause during playback and while the app is in the background or offline.

The menu and player’s channel strip show stations with a catalogue-verified stream or a locally confirmed playable stream. A confirmed failure across the configured sources removes the station from the menu. The monitor continues checking that station and restores it in its original order when a playable stream returns. An inconclusive check or connection outage keeps the previous visibility. A station without a configured source stays out of the menu until a catalogue refresh supplies one and it is verified.

[0.3.0 verification](VERIFICATION-0.3.0.md) · [GEM stream audit](GEM-VERIFICATION.md) · [Earlier discovery and verification](VERIFICATION.md)
