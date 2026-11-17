# GEM channel audit

## v0.2.2 recheck

Rechecked 17 November 2026. The eight supplied CloudFront URLs still did not resolve. Of the 28 directory endpoints, 17 returned HTTP 403 and 11 returned HTTP 404. No test retrieved a playable GEM stream. The broadcaster’s “Watch us LIVE” page returned 404 and GEM Online returned 521. The public satellite roster and social page did not publish a usable native HLS or DASH source. [GEM Group live page](https://gemgroup.tv/en/live/gem/gemtv) · [GEM satellite frequencies](https://gemgroup.tv/en/satellite-frequencies) · [GEM Online](https://gemonline.tv/)

All 34 GEM records remain in the catalogue. As of this recheck, none has a trusted playback link, so v0.2.2 shows none in the menu. On the Android TV emulator, synthetic HLS feeds verified that a failed channel disappears from an open grid and a recovered one returns in its assigned position. [Channel catalogue](channels.json)

## Original v0.2.1 audit

Checked 17 November 2026. The catalogue contains **34 GEM channels**. No channel had a verified playable feed or multiple verified sources. **Nine** are marked unavailable and retained in the catalogue. The other **25** are unverified. Of the 32 GEM entries in Film & Series, 23 remain visible. GEM Sport stays in Sports. GEM Modern Economy is outside the News selection.

The eight supplied CloudFront HLS URLs were requested individually. Their shared hostname returned no address or alias in A, AAAA or CNAME checks, so none produced an HTTP response. The 28 distinct alternative HLS routes exposed by the directory listings returned 15 HTTP 403 responses and 13 HTTP 404 responses. No playlist or video was retrieved from those routes. Each was used for discovery and testing. No stream URLs from those directories were integrated. [Parsa TV GEM Series](https://www.parsatv.com/name=GEM-Series) · [MRTV GEM TV](https://mrtv.me/gem-tv/) · [MRTV GEM Series](https://mrtv.me/gem-series/)

GEM Group documents its satellite channels and frequencies. Its public channel pages and linked social channel did not provide a stable native HLS or DASH feed for this app. GEM Online returned HTTP 521 from this network. The BabakTV pages checked returned HTTP 403. GLWiZ describes its service and subscription access. [GEM channel roster](https://gemgroup.tv/en/channels) · [GEM satellite frequencies](https://gemgroup.tv/en/satellite-frequencies) · [GEM Social Hub](https://www.youtube.com/@GemSocialHub/streams) · [GEM Online](https://gemonline.tv/) · [BabakTV GEM Series](https://babaktv.com/gem-series/) · [GLWiZ FAQ](https://glwiz.com/Pages/FAQ/FAQ.aspx)

GEM Travel has a broadcaster listing and a reported satellite departure in July 2026. It is now recorded as unavailable. GEM Music and GEM Fashion are listed on the satellite lineup and are now recorded as unverified. GEM MAX appears in a historical Yahlive list. Its current carriage could not be verified, so it was omitted. [GEM Travel](https://www.gemgroup.tv/en/channel-details/gem-travel) · [Travel satellite history](https://www.satfrq.com/EN/Channel/GEM-Travel) · [GEM Music](https://www.lyngsat.com/tvchannels/tr/GEM-Music.html) · [Fashion satellite lineup](https://www.lyngsat.com/muxes/Al-Yah-1_East_12034-V.html) · [Yahlive 2020 line-up](https://www.yahlive.com/sites/yahlive/files/2020-03/Channel%20List%2018-03-2020.pdf)

Every row below was reviewed on 17 November. The candidate format was HLS wherever a route was found. A dash indicates no native candidate was found for that column. No row has a verified primary or backup.

| Channel | Supplied HLS | Directory HLS | Catalogue status |
| --- | --- | --- | --- |
| GEM TV | DNS | 403 | Unavailable |
| GEM TV+ | — | 404 | Unverified |
| GEM Rubix | — | 403 | Unverified |
| GEM Rubix+ | — | 404 | Unverified |
| GEM Series | DNS | 403, 404 | Unavailable |
| GEM Series+ | — | 404 | Unverified |
| GEM River | — | 403 | Unverified |
| GEM River+ | — | 404 | Unverified |
| GEM Drama | DNS | 403 | Unavailable |
| GEM Drama+ | — | 404 | Unverified |
| GEM Film | DNS | 403 | Unavailable |
| GEM Onyx | — | 403 | Unverified |
| GEM 24B | — | 404 | Unverified |
| GEM Latino | — | — | Unverified |
| GEM Mifa | — | — | Unverified |
| GEM Mifa+ | — | — | Unverified |
| GEM AZ | — | 404 | Unverified |
| GEM Kids | DNS | 403 | Unavailable |
| GEM Junior | — | 403 | Unverified |
| GEM Bollywood | DNS | 403 | Unavailable |
| GEM Entertainment | — | 403 | Unverified |
| GEM Food | DNS | 404 | Unavailable |
| GEM Classic | DNS | 403 | Unavailable |
| GEM Comedy | — | 403 | Unverified |
| GEM Documentary | — | 404 | Unverified |
| GEM Life | — | 404 | Unverified |
| GEM Modern Economy | — | — | Unverified |
| GEM Nature | — | 403 | Unverified |
| GEM Pixel | — | 404 | Unverified |
| GEM Sport | — | — | Unverified |
| GEM Fit | — | 403 | Unverified |
| GEM Travel | — | — | Unavailable |
| GEM Music | — | 404 | Unverified |
| GEM Fashion | — | — | Unverified |

## Visibility and recovery

The 31 original GEM entries had empty source lists, so the scanner did not test or hide their Film tiles. The old source-first ordering put those tiles near the end of the catalogue. Revision 3 assigns permanent Film positions and groups Persiana, GEM and other channels in that order.

The scanner could classify a malformed child playlist as a failure of the entire HLS master. It now tries up to two renditions within the existing deadline and leaves the result inconclusive when neither yields a playable sample. A configured channel still gets background retries after a confirmed failure. Entries without an authorized URL can gain a working source through a later catalogue revision and retain their original position.

## App verification

- On the Android TV emulator, signed v0.2.1 upgraded v0.2.0 in place and preserved the resume option. The home screen opened in 632 ms in the measured launch. All four categories appeared. News displayed IRINN, IRINN 2 and BBC Persian. Persiana Family and Persiana Series rendered live video in Media3 and switched with the D-pad.
- With controlled HLS test video on the emulator, the scanner accepted a good second rendition after a malformed first one. Media3 played a backup after an HTTP 404 on the primary. A confirmed failure hid only the affected tile when the grid reopened. A background check restored it in its original GEM position. Two shuffled catalogue revisions yielded the same group order. These tests used synthetic GEM feeds.
- Seven unit tests and both debug and release Android lint checks passed.
- The Xiaomi MiTV-AFKR0 accepted the signed in-place upgrade to 0.2.1. The installed package matched the APK hash and resolved to the TV launcher. Playback and navigation were exercised on the emulator. Amin Player was already on the Xiaomi screen, so Amin Live TV was not opened there.
