# Persian subtitle coverage (3.4 and catalogue revision 7)

Checked on 18 November 2026 from Portugal (YouTube reports country PT).

## Why most programmes showed no Persian subtitles

**1. The player never switched on automatic English captions.** Most YouTube documentaries only have YouTube's *automatic* English captions. YouTube does send that track to the app's embedded player: inspecting the player in the app's WebView showed `en:asr:a.en` for *Legends of the Pharaohs*. But the player leaves it off and doesn't list it, so 0.3.3/3.3 found "no captions". The track only came on by itself for videos with dubbed audio (*Earth Untold*) or human-written captions. This was reproduced on WebView 90 (Android 11) and WebView 113 (Android 14). Her box runs WebView 157.

**2. Some tiles couldn't show subtitles at all.** Some tiles hand off to the YouTube app, where the owner blocks embedding: NHK. Others now start with videos blocked in Portugal: FilmRise, Movie Central, Paul O'Grady, Love Nature and most of FD Nature *Wild Europe*. The YouTube player reports "The uploader has not made this video available in your country". Playlists change over time, so blocked titles reached the top.

**3. Some videos have no captions in any language.** Examples are the ambient Naturescape streams (no speech, no subtitles needed), ARTE uploads, and Mosfilm films with English subtitles burned into the picture.

## Fix in the app (3.4)

The app now picks, for each video in the playlist:

1. the video's own Persian track;
2. YouTube's Persian translation of human English captions;
3. YouTube's Persian translation of other human captions;
4. YouTube's Persian translation of the automatic captions the player shows by itself;
5. YouTube's Persian translation of automatic English captions, requested explicitly as `{languageCode: 'en', kind: 'asr', translationLanguage: fa}`. This is what was missing.

Track lists can arrive a few seconds late, so a better choice replaces a worse one during the first 20 seconds of playback. Asking for a language the video doesn't have leaves the current track alone, which I tested. When the player shows automatic Persian captions, the speech is Persian, so subtitles are switched off.

Everything is still drawn by YouTube's own player with the original timing. The app observes which caption file the player loads, logging lines like "Captions loading for … en (asr) as fa", so results can be confirmed from the TV's log. It doesn't read, change or re-request that file.

## Catalogue changes (revision 7)

Same tile IDs (the app refuses catalogues that drop an ID), new sources. Every new playlist was played in the app's own embedded player with the app's origin, not just checked on its watch page.

| Tile | Was | Now | Captions |
|---|---|---|---|
| doc-arte-reportage | ARTE (no captions) | DW Documentary · Editors' Choice | human English |
| doc-nhk-globe | NHK (blocks embedding) | DW Documentary · Travel | human English |
| doc-nhk-trains | NHK (blocks embedding) | DW Documentary · Culture & Arts | automatic English |
| wild-love-nature-live | Love Nature (blocked in PT) | Free Documentary Nature · Wild Africa | automatic English |
| wild-love-nature-ocean | Love Nature (blocked in PT) | Free Documentary Nature · The Big Blue | automatic English |
| wild-fd-nature-europe | FD Nature · Wild Europe (5 of 6 blocked in PT) | Free Documentary Nature · Wild Far North | automatic English |
| nature-arte | ARTE (no captions) | Netflix · *Our Planet* (full episodes, YouTube Learning playlist) | human English + 16 languages |
| nature-love-nature-habitats | Love Nature (blocked in PT) | DW Documentary · Environment & Nature | human English |
| nature-fd-ocean | FD Ocean (first video blocked) | Free Documentary Ocean · Ocean Wildlife | automatic English |
| science-nhk-climate | NHK (blocks embedding) | DW Documentary · Climate Change | human/automatic English |
| movie-filmrise-comedy | FilmRise (blocked in PT) | Shout! · Kenny Rogers TV films | human English |
| movie-central-christmas | Movie Central (blocked in PT) | Shout! · Documentaries (feature length) | human English |
| series-filmrise-dogs | FilmRise TV (blocked in PT) | Bob Ross · *The Joy of Painting* (full episodes) | human English |

New Persian-language documentary tiles (no subtitles needed): **بی‌بی‌سی · مستند** (BBC Persian documentary films), **من و تو · مستند** (Manoto documentaries) and **من و تو · تونل زمان** (Manoto *Time Tunnel* archive history). DW Persian was considered but its playlists are 1–4 minute news clips. Iran International's documentary list was left out because it is mostly about killings and repression.

Rejected after testing in the app: Rick Steves' Europe (error 150, the owner blocks embedding in apps), Shout! Comedy, For Families and Romance (first films blocked in PT), BBC Earth playlists (3–4 minute clips), and Paramount Movies (trailers only).

## Verification

**In the app, one OK press, Android TV emulator** (Android 14 with WebView 113, and Android 11 with WebView 90):

| Tile | Result |
|---|---|
| افسانه فراعنه (FD History) | was none; now Persian from automatic English, on both WebViews |
| جویندگان طلا (FD Adventure) | was none; now Persian from automatic English |
| آفریقای وحشی (new) | Persian from automatic English |
| نتفلیکس · سیاره ما (new) | Persian from human English (en-US) |
| وسترن · کنی راجرز (new) | Persian from human English, on screen |
| دویچه‌وله · برگزیده (new) | Persian from human English (en-GB) |
| باب راس · نقاشی (new) | Persian from human English |
| فرار از زندان | Persian from human English (unchanged) |
| بی‌بی‌سی · مستند, من و تو (new) | Persian speech, no subtitles (correct) |
| DW News live | unchanged |

Unit tests: 10 pass. Lint: no errors.

**Caption audit** (first six videos of every playlist tile, from their watch pages): videos with captions YouTube can translate went from about 28% (what 0.3.3/3.3 could use) to 58% with the 3.4 player, before the source swaps. Most of the rest were the tiles swapped above.

**Her box:** v3.3 and WebView 157. It was in use during the work, so the new player hasn't been tested there yet.

## Not possible, and why

- **Live news (DW, Al Jazeera, France 24…):** YouTube has live English captions for some channels, but marks them `is_translateable: false`. The player keeps them in English even when Persian is requested. This was tested on the TV emulator and in desktop Chrome.
- **Programmes that open in the YouTube app:** the app can't control subtitles there.
- **Fetching captions and translating them ourselves:** YouTube's terms forbid automated access to its content outside the player, and reading the player's own caption traffic would mean intercepting it. Speech recognition isn't practical either: the box has 2 GB of memory, and separating YouTube's audio isn't allowed.
- **Quality:** translations are YouTube's machine translation. Automatic captions add their own errors and roll in line by line.
