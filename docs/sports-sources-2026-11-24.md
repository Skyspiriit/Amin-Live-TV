# Sports source review — 24 November 2026

The baseline is main commit `09b8d6b`, catalogue revision 11. Revision 12 adds **14 official replay collections containing 49 individually checked videos**, and moves the existing **شطرنج فارسی** playlist into Sports. Everything stays in one existing Sports grid, with native live channels first. No new verified 24/7 live-TV stream was found; no recording is labelled as a new live channel.

## Counts

| Measure | Before | After |
|---|---:|---:|
| Sports records, including existing unavailable placeholders | 11 | 26 |
| Entries with configured playback sources | 6 | 21 |
| Entries with actual Portugal emulator playback confirmed | 4 | 19 |

The last row counts working tiles, not videos: it includes the four retained live streams, new replay collections and the moved Persian Chess tile. Five existing source-less placeholders and the two existing feeds that failed the current Portugal check are preserved. Their existing health monitor still controls visibility. A catalogue status is not substituted for actual playback evidence.

## Accepted sources

| Tile | Checked full matches/events | Official publisher |
|---|---:|---|
| فوتبال · مسابقات کامل جام حذفی انگلیس | 4 | [Publisher](https://www.thefa.com/competitions/thefacup) · [YouTube](https://www.youtube.com/channel/UCChcWqwYXCEs657MQ00qVWA) |
| فوتبال · مسابقات کامل لیگ ژاپن | 3 | [Publisher](https://www.jleague.jp/corporate/en/pressrelease/article/16008) · [YouTube](https://www.youtube.com/channel/UCmQp6ZaAejJKKkXc_Y_lh1A) |
| بسکتبال سه‌نفره · فیبا | 4 | [Publisher](https://fiba3x3.com/) · [YouTube](https://www.youtube.com/channel/UC7LpyJP5fupiJu2CdzRQheg) |
| بسکتبال · مسابقات کامل فیبا | 4 | [Publisher](https://www.fiba.basketball/) · [YouTube](https://www.youtube.com/channel/UCtInrnU3QbWqFGsdKT1GZtg) |
| والیبال · مسابقات کامل | 4 | [Publisher](https://www.volleyballworld.com/) · [YouTube](https://www.youtube.com/channel/UCNMg6XDhRZI2QzL4pWOvP_w) |
| بدمینتون · مسابقات کامل | 4 | [Publisher](https://bwfbadminton.com/) · [YouTube](https://www.youtube.com/channel/UChh-akEbUM8_6ghGVnJd6cQ) |
| تنیس روی میز · مسابقات کامل | 3 | [Publisher](https://www.worldtabletennis.com/) · [YouTube](https://www.youtube.com/channel/UC9ckyA_A3MfXUa0ttxMoIZw) |
| تنیس · آرشیو اوپن استرالیا | 4 | [Publisher](https://ausopen.com/) · [YouTube](https://www.youtube.com/channel/UCeTKJSW1NTAkf27nNmjWt5A) |
| تنیس · مسابقات کامل یو اس اوپن | 4 | [Publisher](https://www.usopen.org/) · [YouTube](https://www.youtube.com/channel/UCXbboag48Qlr78zzz6SkzkQ) |
| اتومبیل‌رانی · مسابقات کامل جی‌تی | 3 | [Publisher](https://www.gt-world-challenge-europe.com/live) · [YouTube](https://www.youtube.com/channel/UC-yHapH6mW1ceZ_5PDUf1_g) |
| فرمول E · بازپخش مسابقات کامل | 4 | [Publisher](https://www.fiaformulae.com/en/ways-to-watch) · [YouTube](https://www.youtube.com/channel/UC-DuRqsBQOEk_5o1q4Ze-Fg) |
| ورزش‌های رزمی · وان چمپیونشیپ | 3 | [Publisher](https://www.onefc.com/) · [YouTube](https://www.youtube.com/channel/UCiormkBf3jm6mfb7k0yPbKA) |
| تکواندو · مسابقات کامل | 3 | [Publisher](https://www.worldtaekwondo.org/) · [YouTube](https://www.youtube.com/channel/UCHp-A--zKubjgaa5ZQClk9g) |
| شطرنج · مسابقات رسمی فیده | 2 | [Publisher](https://www.fide.com/) · [YouTube](https://www.youtube.com/channel/UC9B47GnzCRFHTT1BIBWvStQ) |

[Persian Chess](https://www.youtube.com/@PersianChess) retains ID `persian-chess` and playlist `UU4wXiMrci9gCRLOuLGqHa2Q`. The full list of exact video IDs, titles, official owner IDs, durations and decoded-frame measurements is in [the machine-readable record](sports-verification-2026-11-24.json). Full matches include FIBA 3x3 games that can be shorter than conventional basketball games; no ten-minute minimum is represented as a definition of a full match.

## Rejected or unverified

- **Dubai Sports ([official page](https://www.dubaiplus.net/web/)):** Official redirects lead to Dubai+. Anonymous Portugal browser did not provide a stable, confirmed free live stream for the native or embedded player. Not added; no guessed legacy CDN links.
- **Abu Dhabi Sports ([official page](https://starzplay.com/ar/admn)):** Official ADTV redirects lead to Starzplay. Anonymous Portugal browser showed account/subscription surfaces; discovered homepage media were trailers, not verified free sports live channels.
- **FIFA+ ([official page](https://www.fifa.com/pt/dazn)):** Free-to-view FIFA+ is now on DAZN; no stable compatible direct playback integration was verified. Not labelled paid or unavailable in Portugal.
- **FIFA official YouTube ([official page](https://www.youtube.com/@fifa)):** All three selected full matches were blocked by the content owner in Amin TV (embedded-player error 150/auth), despite watch-page metadata reporting embedding allowed.
- **Unfiltered uploads, short highlights, paid/member-only and unconfirmed live events:** Excluded by design. No YouTube channel-discovery tile is used for the new collections. Formula E is archival replay only; no claim of free current live rights.

All actual playback failures are also retained in the verification JSON. Watch-page `playableInEmbed` alone was insufficient: FIFA passed that check and failed inside the app. Only IDs that passed both ownership/metadata checks and actual embedded playback remain in the new catalogue.

## Portugal playback verification

Tests used an Android TV API 34 emulator through the host's Portugal connection. YouTube's player reported `gl=PT`; an independent connection-country check also reported PT. No geographic override, VPN, account, copied playback token or extracted YouTube stream was used. Each accepted video rendered non-ad frames in the official embedded player, after seeking into the match. Resolution and frame counts are recorded per video. This proves sampled playback on that connection and date, not all future rights or every minute of each recording.

Existing native streams: **sport1, sport2, sport3 and persiana-fight passed first-frame decoding**. **tv3 and varzesh failed both configured sources with HTTP playback errors**. All six original records and URLs are unchanged. No unrelated functioning channel was removed.

The physical Xiaomi box in Portugal was actively playing SalinTV. It was left uninterrupted. No APK, preview build, catalogue, settings or data were installed/updated on that device. New Sports playback, the physical remote and Android 11/WebView behaviour remain physical-device checks for the owner. Emulator results must not be read as Xiaomi results.

## Compatibility and checks

The existing app could not parse YouTube programmes into Sports. A small 3.9.2 reader/player extension therefore accompanies the catalogue: bounded `youtubeVideos` arrays are played with [YouTube's official loadPlaylist API](https://developers.google.com/youtube/iframe_api_reference#loadPlaylist). This preserves reviewed video IDs without exposing a mixed uploads feed. Future approved selections can be changed remotely. No new dependencies or artwork were added.

Older 3.9.1 was actually run against revision 12 and activated it successfully. The optional `displayGroup: sports` keeps Chess's original `group: chess` for old readers; they safely skip the new Sports collections. New readers route Chess into Sports. The current cache validation and schema version remain intact. Existing channel IDs, sources, relative order and both series libraries are protected by the baseline fingerprint check. No viewing/progress storage schema is changed.

- `node scripts/validate-sports-catalogue.cjs`: validates source preservation and actual Portugal playback evidence for every new video.
- Gradle: debug/release builds, unit tests and debug/release lint.
- Navigation, legacy activation and signed-upgrade results are recorded with the release verification.

The moved chess playlist and new VOD collections use the existing Persian-caption policy. Native live subtitles, documentary rounds, Iranian-series captions-off/resume, home screen, VPN and Amin Player are unchanged.

## Final verification results

- **Passed:** 22 JVM tests; debug and release builds; debug and release lint; preservation of all 257 existing channel/programme records and both series libraries (38 titles). Only Chess's optional display routing is added to an existing record.
- **Passed in the Android TV emulator:** all 49 new videos decoded non-ad frames from Portugal; the original Persian Chess playlist decoded 1080p video; Back returned Chess to Sports. Four retained native live feeds rendered video. Two retained feeds failed as documented above.
- **Passed with the remote key path:** the four-video FIBA 3x3 queue exactly matched its reviewed IDs; OK resumed and paused without changing position; natural completion advanced to the next match; Back preserved the selected Sports tile; reopening started the first reviewed match; Up selected the preceding tile and Down the following tile. The existing Persian VOD subtitle setting remained enabled. Persian speech in Chess retained its existing no-subtitles-needed behaviour.
- **Passed:** the final revision 12 catalogue activated in the 3.9.1 preview build. New Sports groups were safely ignored by that older reader and the original Chess group remained readable.
- **Passed:** production-signed 3.9.1 → 3.9.2 upgrade on the emulator, retaining the saved theme and both native-channel and Iranian-episode Continue Watching actions. Both retained actions still played. Exact episode resume seconds were not remeasured in this Sports-only release; the resume code and data formats are unchanged.
- **Passed:** APK package `com.enigma.aminlivetv`, version code 18, version 3.9.2, non-debuggable, certificate matching the previous public APK, and bundled catalogue byte-for-byte matching the reviewed JSON. APK size is 5,219,472 bytes, 2,336 bytes above 3.9.1. SHA-256: `a71f6265b19217d2bf432ce6e4a2984f30ff177031bd32d0b28486909c19c4e0`.
- **Not tested:** the new catalogue/player path on the physical Xiaomi. No device installation or update was performed.

## Additional official references

[J.League's international distribution notice](https://www.jleague.jp/corporate/en/pressrelease/article/16008) identifies its official YouTube match service, with rights-based territory exclusions. [GT World's coverage notice](https://www.gt-world-challenge-europe.com/news/2751/another-season-of-comprehensive-live-free-to-air-coverage-confirmed-across-four-continents?filter_meeting_id=19&filter_team_news=0) documents free official coverage. [Australian Open's archive announcement](https://ausopen.com/articles/news/iconic-australian-open-archive-matches-debut-on-ao-youtube-channel) identifies its full-match archive. These establish publisher provenance; actual per-ID Portugal playback checks determined acceptance.

[Formula E's viewing guide](https://www.fiaformulae.com/en/ways-to-watch) concerns current live rights, which are not promised by the archive tile. [FIFA's DAZN page](https://www.fifa.com/pt/dazn) describes the FIFA+ platform; this release does not add a DAZN integration. The new collections retain the official embedded player and its restrictions.
