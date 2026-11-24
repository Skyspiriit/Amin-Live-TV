# Amin Live TV 3.9.1 verification

Prepared on 24 November 2026 from main commit `88db689`, finishing the existing home reorganisation. The unrelated local VPN branch was kept separate.

## Build and compatibility

- Debug and signed release APKs built with JDK 17 and Android SDK 35.
- All 18 unit tests passed: channel/update policies (10), documentary rounds (3), series parsing (2), bundled series inventory (1), and subtitle policies (2).
- Both debug and release lint passed with no reported issues.
- Production package remains `com.enigma.aminlivetv`; version name 3.9.1, version code 17.
- Signing certificate SHA-256 remains `ce108de5ce0ebdb7e5d118dc60ac8a2a87899f2553cdb3e9eb6abea1dfe16b3d`, matching the published 3.8.0 APK.

## Physical Xiaomi checks

The Xiaomi MiTV_AFKR0 runs Android 11. Its main installation was version 3.8.0 (code 15), with installation permission already allowed. Initial checks opened that installation and its channel grid. The production app was never replaced during this work.

New-code tests use `com.enigma.aminlivetv.qa`, labelled **Amin TV Preview**, installed separately for user 0. It uses the full bundled catalogue through a local test endpoint. Its preferences are separate from the production app.

The D-pad check covered the fixed menu, the films section, documentary/wildlife tab switching, Back to the originating home tile, remembering the selected tab, the Iranian-series section, the episode list, and Back from the series screens. All passed. Episode labels 1 and 2 retained their expected order.

Real YouTube episode playback also passed in the Xiaomi preview. The check used the stable `hezartou/s1e1` catalogue entry and video `D_VP8L4z0X0`. It exercised restart from the beginning, seeking, periodic checkpoints, pausing, Back, process termination and reopening through the home Continue Watching action. Back saved **147.729898 seconds**; after the process restart the same episode requested that position and the first playing sample was **148.73532191226195 seconds** (about 1.01 seconds later). Pausing left the measured position unchanged. Both the initial and resumed embedded player showed no visible caption segments; the resumed iframe started at second 147. YouTube retained an unused automatic-Persian track in its internal metadata, which is not evidence that subtitles were displayed. The app's existing episode caption suppression was left intact.

The first intro prototype exhausted graphics resources after repeated launches on the physical box (`EGL_BAD_ALLOC`). The final intro uses a normal native View within the existing activity renderer, without a second SurfaceView renderer. The box's system animation preference is off; its ordinary startup uses the reduced-motion path. Temporary test changes to that preference are restored afterward.

The final animated intro rendered 42 frames in 2.077 seconds on the Xiaomi box, with an audio-output timestamp reported by Android. Normal completion returned to a focused home action. Back skipped the intro while keeping the app open. Home during startup, followed by reopening, returned to the home screen without a stale overlay. These checks used D-pad mode; touch-driven setup was ended before checking remote focus.

## Emulator checks

Android TV API 34 on x86 is used for emulator checks. Emulator results do not constitute testing a production upgrade on the Xiaomi box.

The **published, production-signed 3.8.0 APK** was installed on the emulator. A real episode was played and paused, creating a series bookmark, and the saffron theme was selected. The new **production-signed 3.9.1 APK** then upgraded it in place with `install -r`, without clearing data. The home continuation and saffron theme survived. Settings showed version 3.9.1, and the upgraded player played the continued episode. This verifies the actual release signing and upgrade path on the emulator; exact resume timing was measured separately in the physical preview as described above.

## Playback limits

YouTube decides video availability, embedding permission, ads and regional restrictions. Playback handed off to the official YouTube app has no exact-position tracking in Amin Live TV. Iranian episodes retain the existing captions-off default; documentary subtitle behaviour and saved queue formats were not changed.

## Design references

The owner's Amin Player 1.37 source supplied the intro's gold-pour drawing and sound timing. The drawing was adapted for this app's mark and activity lifecycle. The fixed menu and distinct focus borders follow Android's [TV focus guidance](https://developer.android.com/design/ui/tv/guides/styles/focus-system) and [navigation components](https://developer.android.com/design/ui/tv/guides/components).
