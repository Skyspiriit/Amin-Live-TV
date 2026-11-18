# Persian subtitles (0.3.3)

Checked on 18 November 2026 from Portugal.

## Approach

YouTube's embedded player (IFrame API), which the app already uses, can translate a video's captions itself. The app turns on the player's captions module and selects one track:

1. the video's own Persian track, if it has one;
2. otherwise YouTube's automatic translation of the English track into Persian (`translationLanguage: fa`), with human-written English preferred over automatic English;
3. otherwise no subtitles.

YouTube draws the subtitles inside its own player using the original cue timing, right to left. The app downloads no captions, calls no translation service and needs no key or account. It keeps the official player, so the owner's settings still apply. The selection runs again for each video in a playlist. It only runs for the documentary, wildlife, nature, science, movies and series groups. Live news and TV channels are unchanged.

**Approaches not used.** Fetching captions with youtube-transcript-api and translating them with a free service would mean a second request path to YouTube outside the player, a free translation endpoint with no service guarantee, and the app's own subtitle renderer timed against the embed. The player's built-in translation gives the same result with none of that, so the fallback wasn't needed.

## Tested video

| | |
|---|---|
| Video | *By train to Mount Etna*, DW Documentary (43 min), `uWSuGqi_Lo8`, desktop browser |
| Caption source | Human-written English captions, translated by YouTube |
| Sync | Same cue, same moment, in both languages: at 5:07, English "But now the 110 kilometer Ferrovia Circumetnea", Persian «اما اکنون…». At 20:03, "Those who do not share Marco's passion for farming often move elsewhere" became «کسانی که اشتیاق مارکو به کشاورزی را ندارند، اغلب به جای دیگری نقل مکان می‌کنند» |
| Quality | Correct, natural sentences. Place names sometimes come out awkwardly (Ferrovia Circumetnea) |

In the app on the Android TV emulator (Google TV, Android 11, 1080p, WebView 90), one OK press:

| Tile | Video | Caption source | Result |
|---|---|---|---|
| زمین ناگفته (Free Documentary) | *Earth Untold: Tunisia* | automatic English | Persian, in sync with the narration («پشتِ نماهای سفیدِ غرق در آفتاب…») |
| ناسا · مریخ‌نورد | Mars Sample Return | human-written English (en-US) | Persian line «آزمایش بازگشت نمونه از مریخ» appears with the on-screen title "TESTING MARS SAMPLE RETURN" |
| اندِوِر · اروپا, شکارچیان افسانه, تاریخ و تمدن | ENDEVR, Timeline, GEDEON first videos | automatic English | Persian translation selected (app log; not checked on screen) |
| سفر با قطار | DW *Through Kazakhstan by train* | Russian only | No subtitles, as intended |
| آرته · گزارش | ARTE | Hindi automatic only | No subtitles, as intended |
| افسانه فراعنه, جویندگان طلا | FD History, FD Adventure | automatic English on desktop | **No subtitles in the emulator.** That player received no caption track for these two videos, while desktop Chrome (also with an Android user agent) did. Probably the emulator's old WebView 90. Her box has WebView 157, so they probably work there, but this hasn't been verified on the box |
| دویچه‌وله انگلیسی (news) | DW News live | — | Unchanged: no captions requested |

OK still pauses (the subtitle stays on screen), Up and Down change programme and each new video gets its own subtitles, Back returns to the grid. 9 unit tests pass; lint has no errors.

**Real device:** not tested. At the time of testing the Xiaomi box was on and playing Amin Live TV, so it was left alone.

## Coverage

The first video of each subgroup playlist, checked from the watch page:

- **Persian or English captions (subtitles expected):** NASA ISS, Webb and Mars, Shout! drama, *Greatest Prison Escapes* and *Unsolved Mysteries* (human-written English); Earth Touch, WildEarth, NHK climate, Free Documentary and its History, Nature and Space channels, FD Adventure, ENDEVR, Timeline, GEDEON, Real Wild, Royal Institution (automatic English).
- **No usable captions:** most ambient nature and wildlife streams, NHK World, ARTE, DW trains (Russian), Mosfilm (Russian), FilmRise films, Movie Central, Shout! noir, Paul O'Grady, FD Ocean. Most of the films in this list open in the YouTube app anyway, where the viewer can turn on captions and auto-translate themselves.

## Limitations

- Translation is YouTube's machine translation. Automatic English captions add their own errors, and they appear word by word in rolling lines rather than as whole sentences.
- It depends on YouTube's player options for captions (`getOption`/`setOption('captions', …)`). These are part of the embedded player but not formally documented, and YouTube could change them. If that happens, videos still play, just without subtitles.
- Programmes opened in the YouTube app (owners who block embedding) don't get automatic subtitles.
- Live broadcasts get none.
- Subtitle size is YouTube's size step 1 (one step above normal).
