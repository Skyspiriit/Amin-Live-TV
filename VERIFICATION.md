# Channel discovery and verification

Checked on 17 November 2026. The bundled catalogue contains **189 distinct channel records**, of which **81 have verified video**. It includes **97 separately tested playback sources** across those 81 channels. Sixteen channels have two sources, and 65 have one.

| Home category | Listed | Video verified |
| --- | ---: | ---: |
| شبکه‌های داخلی | 65 | 56 |
| شبکه‌های خبری | 15 | 5 |
| شبکه‌های ورزشی | 11 | 6 |
| فیلم و سریال | 104 | 19 |

Category totals overlap where a channel belongs to more than one category.

| Status | Count | Basis |
| --- | ---: | --- |
| Verified | 81 | Sampled video decoded from at least one included source. |
| Unverified | 101 | The channel was identified and a playable source suitable for the app is still needed or untested. |
| Unavailable | 5 | A tested source failed or supplied test bars at the time of the check. |
| Restricted | 2 | Playback through this native client would require permission or subscriber access. |

## What was checked

- Visited SalinTV’s Persian archive, live archive, gallery and the channel categories exposed by its sitemap and gallery links. Followed the site’s pagination, including the terminal empty pages its links returned. Across 43 archive pages, 27 returned channel cards. The archives and gallery linked to 96 distinct channel pages, all of which were fetched. Queried three additional GEM footer links, which returned HTTP 404. Merged repeated High Vision, Maah TV and Novin TV listings. Excluded a radio-only listing and pages for individual broadcasts. [Persian archive](https://salintv.com/categories/live/farsi/) · [Live gallery](https://salintv.com/live-tv-streams/) · [Category sitemap](https://salintv.com/categories-sitemap.xml)
- Reviewed individual station titles, available artwork and player markup. Found HLS references on 85 channel pages and iframe-based players on some pages, including the Khabar page. Verified Khabar through Telewebion’s broadcast CDN. Some SalinTV JW Player or iframe pages reference transient sources or streams on other services. Those references alone were insufficient for native integration. [SalinTV terms](https://salintv.com/terms/) · [Khabar listing](https://salintv.com/video/khabar-tv-live/)
- Added Iran’s national and provincial stations from public Telewebion lineups, the broadcaster’s Persiana lineup and GEM’s published channels. Included Dari stations found in the SalinTV live listings. Supplemented news with official Iran International, Afghanistan International and VOA pages. Channel artwork is referenced by URL, with a generic play icon when artwork is unavailable. [Telewebion](https://telewebion.ir/live) · [Telewebion lineup references](https://github.com/shayanline/iptv-iran/blob/main/playlists/iran.m3u) · [Persiana](https://persianagroup.tv/live.html) · [GEM](https://gemgroup.tv/en/channels) · [Iran International](https://www.iranintl.com/live) · [Afghanistan International](https://www.afintl.com/live) · [VOA Persian](https://ir.voanews.com/live/video/416)
- Tested 107 candidate HTTPS HLS endpoints from the available test network. FFmpeg decoded sample frames from 102 endpoints. Sampled video was reviewed for station identity, test images and off-air slates. The catalogue carries the 97 retained sources and their individual provenance and test dates. The app requests streams from broadcaster or distribution hosts directly. It stores station metadata locally and uses no media relay. Sources that require access tokens, authentication, DRM, subscription access or a geographic workaround were excluded.
- Fifteen two-source channels have distinct Telewebion URLs, typically a multivariant playlist and a separate edge manifest. Both paths were tested and may fail together during a provider outage. Iran International has a broadcaster-player feed and a separate Akamai delivery endpoint. Each was tested independently.
- The Pooya / Nahal stream showed test bars. iFilm 2, Tapesh, Rahe Nejat and Your Time did not yield video from their tested candidates. iFilm Persian played through Telewebion. The BBC Persian candidate decoded, and the BBC’s conditions for third-party use limited native integration, so the channel is listed as restricted. Persiana+ is restricted because the broadcaster reserves premium channels for subscribers. Persiana’s site also ships demonstration streams under several labels. The verified Persiana listings use sampled broadcaster-branded broadcast endpoints. [BBC terms](https://downloads.bbc.co.uk/usingthebbc/bbc_terms_of_use_19September2022english.pdf) · [Persiana terms](https://persianagroup.tv/legal.html?p=terms)

## Android TV checks

The signed APK was built with Android SDK 35. The Android TV test image is Android 14 on x86. The package ID is `com.enigma.aminlivetv`. Android build checks passed with zero lint errors.

| Check | Result |
| --- | --- |
| Installation and launch | The signed app installed alongside the existing Amin Player package and opened with the four Persian categories and version 0.1.0. |
| D-pad navigation | Opened every category with the remote keys. Checked channel grid focus, OK to play, Up and Down to change channels, the channel strip, and Back to return to the grid and home. |
| Fullscreen live video | Android Media3 rendered video from IRIB TV1, TV2, TV3, Varzesh, iFilm Persian, Iran International, VOA Persian and Afghanistan International. |
| Last channel | Varzesh resumed after the app was stopped and started again. The home screen also restored the last category. |
| Repeated switching | The final APK restored Varzesh after 20 rapid Up and Down channel changes. The grid and home screen remained responsive. |
| Automatic fallback | A separate test build received HTTP 404 from its priority-one source, loaded a local HLS test stream from priority two, and persisted the successful channel. |
| Catalogue recovery | The test build accepted a valid newer JSON revision, rejected malformed and incomplete revisions, kept the saved revision after the test endpoint was removed, and started with the bundled revision after clearing its test data. |

The catalogue tests used a local HTTP fixture in a separate test build. The supplied APK uses the configured GitHub HTTPS endpoint and carries the same catalogue validation and cache logic.

The signed distribution APK has SHA-256 digest `acb6c6839aa5a8977d5d4bf15cd0db70e86ca9e6db85cec30b1d503c56d40baf`.

## Known limitations

Playback probing took place on the development network. Test streams may differ by programme, provider and location. The physical Xiaomi device and a Portuguese household connection still need testing. Verification is a snapshot, and recurring expiry or week-long stability have not been measured. The 81 figure is the count of channels verified by video decoding and includes stations outside the subset tested in Android TV.

GEM, Manoto, several Persiana stations and other discoveries appear without playable sources where a suitable video source was unavailable or remains unverified. The app lists these after the stations with playable sources and displays a Persian availability message on selection. Broadcaster-inserted advertising may still appear in the video.

The app checks the [public GitHub catalogue](https://raw.githubusercontent.com/Skyspiriit/Amin-Live-TV/main/channels.json) on startup and ships with a bundled copy. The signing key and signing password are kept outside the repository, source archive and release assets.
