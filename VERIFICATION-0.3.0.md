# Amin Live TV 0.3.0 verification

Checked 17 November 2026 from a home connection in Portugal. Every playback result below comes from the Android TV emulator unless the row says otherwise. Nothing in this release was tested on the physical Xiaomi box.

## What changed

- **Four documentary groups on the home screen** under the existing four categories: مستند (documentaries), حیات وحش (wildlife), طبیعت (nature) and علم و فضا (science and space). Movies & Series is unchanged.
- **Official YouTube playback inside the app.** Tiles marked «یوتیوب رسمی» play the broadcaster's own YouTube live broadcast or playlist in YouTube's embedded player (IFrame API). One OK press starts playback. OK pauses or resumes, Up and Down change programme, and Back returns to the grid.
- **Hand-off to the official YouTube app** when the owner doesn't allow embedding (YouTube error 150). The app opens that exact live broadcast, or the playlist's newest video, in `com.google.android.youtube.tv`, never YouTube's home screen. If the YouTube app is missing or the link can't be found, the tile shows «این برنامه الان در دسترس نیست.»
- **Native first.** Persiana Docs plays natively with Media3 in the Documentaries group. It references the existing catalogue record, so the Film entry is untouched.
- **Catalogue revision 5** adds `youtubeChannel` to the international news records and a new top-level `programmes` list. Apps up to 0.2.2 ignore both, so they keep receiving catalogue updates (tested below).

## Channel-by-channel results

**Native** = Media3 playback of a stream in the app. **In-app official** = YouTube's embedded player inside Amin Live TV. **External official** = the official YouTube TV app opened at a specific broadcast or playlist.

### News

| Channel | Result | Source | Evidence and reason |
| --- | --- | --- | --- |
| BBC Persian | External official | BBC News Persian YouTube live (`UCHZk9MrT3DGWmVqdsj5y0EA`) | Live and playable from Portugal. BBC blocks embedding (error 150). The emulator launched the YouTube app with `watch?v=bLAJZTSl2_Y`. The emulator's YouTube app refuses to run on emulators, so playback after the hand-off is **untested**. |
| Euronews English | In-app official | `@euronews` live | Video played on the emulator. |
| DW English | In-app official | `@dwnews` live | Video played. OK paused and resumed it, and Down moved to the next channel. |
| France 24 English | In-app official | `@France24_en` live | Video played. |
| Al Jazeera English | In-app official | `@aljazeeraenglish` live | Video played. |
| Sky News | External official | `@SkyNews` live | Embedding blocked. Hand-off to the current live video launched. Playback after the hand-off is untested. |
| ABC News Live | External official | `@ABCNews` live | Embedding blocked. Hand-off launched. Playback untested. |
| CBS News | In-app official | `@CBSNews` live | Video played. CBS has no 24/7 stream on YouTube, only its current live coverage, so the tile is now named «سی‌بی‌اس نیوز». When CBS isn't live, the tile reports that the programme is unavailable. |
| CNN Headlines | External official | `@CNN` live «CNN Headlines: 24/7 Live News» | Free on CNN's official YouTube channel and playable from Portugal. Embedding blocked. Hand-off launched. Playback untested. Hidden in 0.2.2 and earlier. |
| CNN International | Excluded | — | `@CNNInternational` had no live broadcast. No free, legal live access without a TV subscription was found. |

The Iranian and Persian-language news channels (IRINN, IRINN 2, Iran International, VOA Persian, Afghanistan International) are unchanged.

### Documentaries, wildlife, nature and science

| Group | Tile | Result | Source | Evidence |
| --- | --- | --- | --- | --- |
| مستند | پرشیانا مستند | Native | Persiana Docs HLS (existing verified record) | Video played. |
| مستند | آرته · گزارش | In-app official | ARTE.tv Documentary «Arte Reportage» playlist | Video played. |
| مستند | ارث تاچ · مستند کامل | In-app official | Earth Touch «Full-length Documentaries» playlist | Video played. |
| مستند | ان‌اچ‌کی · جهان | External official | NHK WORLD-JAPAN «Stories from Around the Globe» | Embedding blocked. Hand-off to `watch?v=59u_T52YLG4&list=…` launched. Playback untested. |
| مستند | ان‌اچ‌کی · قطارهای ژاپن | External official | NHK WORLD-JAPAN «Trains of Japan» | Embedding blocked. Hand-off launched. Playback untested. |
| حیات وحش | لاو نیچر · زنده | External official | Love Nature 24/7 live, with a playlist fallback | Embedding blocked. Hand-off to the live `watch?v=QhahoVG0BfI` launched. Playback untested. |
| حیات وحش | وایلد ارث · سافاری | External official | WildEarth SafariLIVE, with the «Masai Mara» playlist fallback | The safari was offline at test time, so the app correctly chose the playlist and handed it off. Playback untested. |
| حیات وحش | ارث تاچ · گربه‌سانان | In-app official | Earth Touch «Big Cats» playlist | Video played. |
| حیات وحش | لاو نیچر · حیات دریایی | External official | Love Nature «Ocean Wildlife» playlist | Embedding blocked. Hand-off launched. Playback untested. |
| طبیعت | نیچراسکیپ · مناظر | In-app official | Stingray Naturescape uploads | Video played. |
| طبیعت | نیچراسکیپ · صدای آب | In-app official | Stingray Naturescape «Calming Water Sounds» | Video played. |
| طبیعت | آرته · طبیعت | In-app official | ARTE.tv Documentary «Nature» playlist | Video played. |
| طبیعت | لاو نیچر · زیستگاه‌ها | External official | Love Nature «Earth's Ecosystems» playlist | Embedding blocked. Hand-off launched. Playback untested. |
| علم و فضا | ناسا · زنده از فضا | External official | NASA live (ISS views), with the James Webb playlist fallback | Embedding blocked. Hand-off to the live `watch?v=ROboUHfPUrU` launched. Playback untested. |
| علم و فضا | ناسا · تلسکوپ جیمز وب | In-app official | NASA «James Webb Space Telescope» playlist | Video played. |
| علم و فضا | ناسا · مریخ‌نورد | In-app official | NASA «Perseverance Mars Rover» playlist | Video played. |
| علم و فضا | ان‌اچ‌کی · تغییر اقلیم | External official | NHK WORLD-JAPAN «IMPACTS: Climates Change the World» | Embedding blocked. Hand-off launched. Playback untested. |

### Providers that were checked and excluded

| Provider | Reason |
| --- | --- |
| wedotv (time2Rlx Wildlife, Nature Walks) | The website works from Portugal, but playback needs the wedotv app or a browser on the TV. I found no official YouTube channel and no deep link that starts a programme. |
| Plex free channels | Needs the Plex app, which isn't on the box. I couldn't confirm a deep link that starts a specific channel. |
| Rakuten TV free channels | Needs the Rakuten TV app. I couldn't confirm a free documentary channel that starts with one press. |
| RTP Play | Portuguese-language, and needs the RTP Play app or a browser. I found no verified deep link to a specific programme. |
| NASA+ native stream | NASA publishes no documented feed for third-party apps, and the NASA+ YouTube channel has no videos. NASA's own YouTube channel is used instead. |
| Love Nature, Stingray Naturescape and Earth Touch FAST streams | Their FAST HLS addresses aren't licensed for third-party apps. Each broadcaster's official YouTube channel is used instead. |

### Persiana Sports

| Channel | Result | Reason |
| --- | --- | --- |
| Persiana Sports 1 (`spstr.abntv.live/hls/psstream.m3u8`) | Excluded | The connection timed out from Portugal. |
| Persiana Sports 2 (`pepsi.abntv.live/hls/4spstream.m3u8`) | Excluded | HTTPS timed out. Plain HTTP only redirected back to HTTPS. |
| Persiana Sports 3 (`pepsi3.abntv.live/hls/psp3.m3u8`) | Excluded | HTTPS timed out. |
| Persiana Sports 4 (`pepsi4.abntv.live/hls/psp4.m3u8`) | Excluded | The connection timed out. |

None of the four candidates returned a playlist from Portugal, so I couldn't verify video or channel identity. `abntv.live` isn't named as a Persiana source on any official page I could reach. `persianasports.com` didn't respond, and the official Persiana Sports YouTube channel has no videos, so there is no official one-press fallback. The four records stay `unavailable` and hidden. Persiana Fight still plays natively in Sports.

## Tests on the Android TV emulator

Google TV image, Android 11 (API 30, like the box), 1920×1080 at 320 dpi.

- **Navigation.** D-pad moved between the four main tiles and the four new group tiles, and OK opened each group. Back went from the player to the grid and from the grid to the home screen.
- **Playback.** Every tile in the tables above was opened in turn. The results are listed per row.
- **Recovery.** With the emulator's network off, a YouTube tile showed «در حال اتصال…» and then «این برنامه الان در دسترس نیست.» after 25 seconds. Back returned to the grid.
- **Live fallback.** WildEarth's offline safari fell back to its playlist automatically.
- **Regression.** IRIB TV1 played natively and Down switched channels. Persiana Family played natively in Movies & Series, whose grid and order are unchanged. In Sports, IRIB TV3 and Varzesh failed because Telewebion returns HTTP 403 for their video segments from Portugal; TV1's segments load normally. This is an upstream restriction, not a change in this release, and the existing monitor hides those channels.
- **Older apps.** The unchanged 0.2.2 code, built as the `.qa` test variant, downloaded the revision 5 catalogue from a local server, logged `Activated revision 5`, and showed the News grid exactly as before.
- **Automated checks.** 9 unit tests passed, including a new one for the documentary groups. Lint found 0 errors. Its 11 warnings all predate this release.

## Not tested

- **Anything on the physical Xiaomi box.**
- **Playback after a hand-off to the YouTube app.** The emulator's YouTube app shows «YouTube is not supported on this device». The tests confirm the app launches with the correct link, and returning to Amin Live TV lands on the grid.
- **An in-place upgrade from the signed 0.2.2.** Debug builds use a different key. The release workflow refuses to publish unless the new APK's certificate matches 0.2.2's (`ce108de5ce0ebdb7e5d118dc60ac8a2a87899f2553cdb3e9eb6abea1dfe16b3d`).

## Screenshots

These are unedited emulator captures in [`docs/verification/v0.3.0`](docs/verification/v0.3.0). `03-persiana-docs-native.png` was captured with an earlier 0.3.0 test build that used the same native playback path. `14-bbc-persian-handoff-youtube-app-emulator.png` shows the emulator's YouTube app refusing to play after a correct hand-off.
