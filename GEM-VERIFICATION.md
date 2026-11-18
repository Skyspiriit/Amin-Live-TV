# GEM channel audit

## 18 November 2026 recheck: no authorized, working GEM stream

Tested 18 November 2026, 02:12–02:30 UTC, from a home connection in Portugal on a development computer. Each candidate was checked for DNS, a fully verified HTTPS connection, the HLS manifest and a media segment, and then decoded with FFmpeg for 20 seconds. No candidate got as far as a media segment. Media3 playback and the Xiaomi box weren't tested, because there was no stream to play.

**Result: no GEM channel plays.** The catalogue is unchanged and no APK was published.

- **`mehrdad-dd/myiptv` (`core.m3u`, last changed 16 May 2026).** 21 GEM channels go through two third-party Cloudflare Workers to `ca-rt.onetv.app`. `wild-dawn-81ef…workers.dev` no longer exists (DNS fails) and `still-mud-3642…workers.dev` returns HTTP 403. The Workers are anonymous relays of another service's streams, so they aren't authorized sources and weren't considered for the app even if they had worked. GEM 24B and GEM TV+ point to a bare IP restream server (`66.102.120.18:8000`) that refused connections, and GEM Pixel points to the Tamashaland aggregator (HTTP 522).
- **`ca-rt.onetv.app` directly.** All 21 GEM addresses return HTTP 403 Forbidden over a valid HTTPS connection. The certificate for `onetv.app` is valid until 10 February 2027. OneTV is a private IPTV app service, so these streams are restricted to its subscribers. (A first Python test reported an expired certificate. That came from this computer's outdated Python trust store, not from the server.)
- **Historical GEM CloudFront list (`iptv-restream/mxtv`).** All 25 addresses use `d2e40kvaojifd6.cloudfront.net`, which no longer resolves.
- **`shayanline/iptv-iran`.** Its only GEM entry, GEM Sport, goes through another third-party Worker that returns HTTP 403.
- **Official GEM sources.** `gemgroup.tv` lists the channels and their satellite frequencies but has no live stream (`/en/live` returns 404). GEM Online (`gemonline.tv`) returns HTTP 521. GEM's official YouTube channel, linked from its site (`@GemSocialHub`), posts short entertainment clips only, with no live channels or series. I found no other official internet stream.

### Why GEM disappeared from the app

All 34 GEM records (25 `unverified`, 9 `unavailable`) have an empty stream list. `HealthRecord.canShow` only shows a channel that has at least one stream, so the tiles are absent because there is nothing to play. The health checker doesn't hide them, parsing doesn't fail, and Media3 never gets a stream to decode. The recovery path (a configured channel that fails stays in the catalogue and returns when a check succeeds) was verified on 17 November and is unchanged. The Film order (Persiana 1000s → GEM 2000s → others 3000s) is kept, so an authorized GEM source added later reappears in its place without a code change.

### What would restore GEM

A stream address that GEM Group publishes or licenses for this kind of use, such as a revived GEM Online, an official YouTube live channel, or a direct agreement. Add it to the existing record with `status: "verified"` after a playback test. Revision 5 of the catalogue reaches installed apps without a new APK.

### Candidate results

| Channel | Source | Manifest | Media segments | Playback 15–30 s | Media3 | Result |
| --- | --- | --- | --- | --- | --- | --- |
| GEM 24B | myiptv · raw IP `66.102.120.18:8000` | No connection | — | — | Not reached | **TEMPORARILY UNAVAILABLE** · Connection refused/timed out twice; unidentified restream server, not authorized |
| GEM Bollywood | myiptv · `wild-dawn-81ef…workers.dev` → onetv | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Worker deleted |
| GEM Bollywood | `ca-rt.onetv.app` direct | HTTP 403 | — | — | Not reached | **ACCESS RESTRICTED** · OneTV subscriber service; 403 Forbidden with verified HTTPS |
| GEM Classic | myiptv · `wild-dawn-81ef…workers.dev` → onetv | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Worker deleted |
| GEM Classic | `ca-rt.onetv.app` direct | HTTP 403 | — | — | Not reached | **ACCESS RESTRICTED** · OneTV subscriber service; 403 Forbidden with verified HTTPS |
| GEM Comedy | myiptv · `still-mud-3642…workers.dev` → onetv | HTTP 403 | — | — | Not reached | **VERIFIED FAILING** · Worker refuses; third-party proxy, not authorized |
| GEM Comedy | `ca-rt.onetv.app` direct | HTTP 403 | — | — | Not reached | **ACCESS RESTRICTED** · OneTV subscriber service; 403 Forbidden with verified HTTPS |
| GEM Drama | myiptv · `wild-dawn-81ef…workers.dev` → onetv | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Worker deleted |
| GEM Drama | `ca-rt.onetv.app` direct | HTTP 403 | — | — | Not reached | **ACCESS RESTRICTED** · OneTV subscriber service; 403 Forbidden with verified HTTPS |
| GEM Drama Plus | myiptv · `still-mud-3642…workers.dev` → onetv | HTTP 403 | — | — | Not reached | **VERIFIED FAILING** · Worker refuses; third-party proxy, not authorized |
| GEM Drama Plus | `ca-rt.onetv.app` direct | HTTP 403 | — | — | Not reached | **ACCESS RESTRICTED** · OneTV subscriber service; 403 Forbidden with verified HTTPS |
| GEM Film | myiptv · `wild-dawn-81ef…workers.dev` → onetv | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Worker deleted |
| GEM Film | `ca-rt.onetv.app` direct | HTTP 403 | — | — | Not reached | **ACCESS RESTRICTED** · OneTV subscriber service; 403 Forbidden with verified HTTPS |
| GEM Fit | myiptv · `still-mud-3642…workers.dev` → onetv | HTTP 403 | — | — | Not reached | **VERIFIED FAILING** · Worker refuses; third-party proxy, not authorized |
| GEM Fit | `ca-rt.onetv.app` direct | HTTP 403 | — | — | Not reached | **ACCESS RESTRICTED** · OneTV subscriber service; 403 Forbidden with verified HTTPS |
| GEM Food | myiptv · `wild-dawn-81ef…workers.dev` → onetv | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Worker deleted |
| GEM Food | `ca-rt.onetv.app` direct | HTTP 403 | — | — | Not reached | **ACCESS RESTRICTED** · OneTV subscriber service; 403 Forbidden with verified HTTPS |
| GEM Junior | myiptv · `wild-dawn-81ef…workers.dev` → onetv | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Worker deleted |
| GEM Junior | `ca-rt.onetv.app` direct | HTTP 403 | — | — | Not reached | **ACCESS RESTRICTED** · OneTV subscriber service; 403 Forbidden with verified HTTPS |
| GEM Kids | myiptv · `wild-dawn-81ef…workers.dev` → onetv | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Worker deleted |
| GEM Kids | `ca-rt.onetv.app` direct | HTTP 403 | — | — | Not reached | **ACCESS RESTRICTED** · OneTV subscriber service; 403 Forbidden with verified HTTPS |
| GEM Life | myiptv · `still-mud-3642…workers.dev` → onetv | HTTP 403 | — | — | Not reached | **VERIFIED FAILING** · Worker refuses; third-party proxy, not authorized |
| GEM Life | `ca-rt.onetv.app` direct | HTTP 403 | — | — | Not reached | **ACCESS RESTRICTED** · OneTV subscriber service; 403 Forbidden with verified HTTPS |
| GEM Mifa | myiptv · `wild-dawn-81ef…workers.dev` → onetv | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Worker deleted |
| GEM Mifa | `ca-rt.onetv.app` direct | HTTP 403 | — | — | Not reached | **ACCESS RESTRICTED** · OneTV subscriber service; 403 Forbidden with verified HTTPS |
| GEM Mifa Plus | myiptv · `still-mud-3642…workers.dev` → onetv | HTTP 403 | — | — | Not reached | **VERIFIED FAILING** · Worker refuses; third-party proxy, not authorized |
| GEM Mifa Plus | `ca-rt.onetv.app` direct | HTTP 403 | — | — | Not reached | **ACCESS RESTRICTED** · OneTV subscriber service; 403 Forbidden with verified HTTPS |
| GEM Nature | myiptv · `still-mud-3642…workers.dev` → onetv | HTTP 403 | — | — | Not reached | **VERIFIED FAILING** · Worker refuses; third-party proxy, not authorized |
| GEM Nature | `ca-rt.onetv.app` direct | HTTP 403 | — | — | Not reached | **ACCESS RESTRICTED** · OneTV subscriber service; 403 Forbidden with verified HTTPS |
| GEM Onyx | myiptv · `still-mud-3642…workers.dev` → onetv | HTTP 403 | — | — | Not reached | **VERIFIED FAILING** · Worker refuses; third-party proxy, not authorized |
| GEM Onyx | `ca-rt.onetv.app` direct | HTTP 403 | — | — | Not reached | **ACCESS RESTRICTED** · OneTV subscriber service; 403 Forbidden with verified HTTPS |
| GEM Pixel | myiptv · `glserver1.tamashaland.com` | HTTP 522 / timeout | — | — | Not reached | **TEMPORARILY UNAVAILABLE** · Aggregator restream, not authorized |
| GEM River | myiptv · `wild-dawn-81ef…workers.dev` → onetv | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Worker deleted |
| GEM River | `ca-rt.onetv.app` direct | HTTP 403 | — | — | Not reached | **ACCESS RESTRICTED** · OneTV subscriber service; 403 Forbidden with verified HTTPS |
| GEM River Plus | myiptv · `still-mud-3642…workers.dev` → onetv | HTTP 403 | — | — | Not reached | **VERIFIED FAILING** · Worker refuses; third-party proxy, not authorized |
| GEM River Plus | `ca-rt.onetv.app` direct | HTTP 403 | — | — | Not reached | **ACCESS RESTRICTED** · OneTV subscriber service; 403 Forbidden with verified HTTPS |
| GEM Rubix | myiptv · `still-mud-3642…workers.dev` → onetv | HTTP 403 | — | — | Not reached | **VERIFIED FAILING** · Worker refuses; third-party proxy, not authorized |
| GEM Rubix | `ca-rt.onetv.app` direct | HTTP 403 | — | — | Not reached | **ACCESS RESTRICTED** · OneTV subscriber service; 403 Forbidden with verified HTTPS |
| GEM Rubix Plus | myiptv · `still-mud-3642…workers.dev` → onetv | HTTP 403 | — | — | Not reached | **VERIFIED FAILING** · Worker refuses; third-party proxy, not authorized |
| GEM Rubix Plus | `ca-rt.onetv.app` direct | HTTP 403 | — | — | Not reached | **ACCESS RESTRICTED** · OneTV subscriber service; 403 Forbidden with verified HTTPS |
| GEM Series | myiptv · `wild-dawn-81ef…workers.dev` → onetv | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Worker deleted |
| GEM Series | `ca-rt.onetv.app` direct | HTTP 403 | — | — | Not reached | **ACCESS RESTRICTED** · OneTV subscriber service; 403 Forbidden with verified HTTPS |
| GEM Series Plus | myiptv · `still-mud-3642…workers.dev` → onetv | HTTP 403 | — | — | Not reached | **VERIFIED FAILING** · Worker refuses; third-party proxy, not authorized |
| GEM Series Plus | `ca-rt.onetv.app` direct | HTTP 403 | — | — | Not reached | **ACCESS RESTRICTED** · OneTV subscriber service; 403 Forbidden with verified HTTPS |
| GEM TV + | myiptv · raw IP `66.102.120.18:8000` | No connection | — | — | Not reached | **TEMPORARILY UNAVAILABLE** · Connection refused/timed out twice; unidentified restream server, not authorized |
| GEM 24B | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM Academy | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM Arabia | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM AZ | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM Bollywood | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM Classic | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM Drama | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM Fit | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM Food | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM Film | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM Junior | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM Kids | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM Latido | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM Life | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM MAX | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM Mifa | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM Modern Economy | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM Nature | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM Onyx | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM Property | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM River | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM Rubix | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM Series | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM Travel | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM TV | CloudFront `d2e40kvaojifd6` | DNS fails | — | — | Not reached | **VERIFIED FAILING** · Hostname no longer exists |
| GEM Sport | shayanline · `steep-wildflower…workers.dev` → `gg.hls2.xyz` | HTTP 403 | — | — | Not reached | **VERIFIED FAILING** · Third-party proxy, not authorized |

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
