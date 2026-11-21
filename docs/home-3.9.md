# Home screen and content review (3.9.0)

## Navigation

### Before (3.8.0)

```
Header (theme, date, clock)
Series banner → سریال‌های ایرانی
ادامهٔ تماشا (last station)
تلویزیون زنده:      شبکه‌های داخلی · شبکه‌های خبری · شبکه‌های ورزشی · فیلم و سریال
سرگرمی و سلیقه‌ها:  مافیا و مسابقه · سینما و فانتزی · دنیای خودرو · شطرنج
کشف جهان:           مستند · حیات وحش · طبیعت · علم و فضا
آرشیو و آیین:        فیلم سینمایی · سریال کامل · آیین و مداحی
```

There were 15 category tiles and one series banner.

### After (3.9.0)

```
Header (theme, date, clock)
ادامهٔ تماشا:  پخش زنده · <last station>   سریال · <latest series>
پخش زنده:     شبکه‌های ایرانی · اخبار جهان · ورزش · فیلم و سرگرمی
فیلم و مستند:  فیلم سینمایی · مستند و طبیعت · علم و فضا · موضوعات دیگر
سریال‌ها:      سریال ایرانی · سریال خارجی
```

The home screen now has 10 tiles. The 5 grouped categories are still there, behind tabs:

- **مستند و طبیعت** opens a grid with tabs for مستند, حیات وحش and طبیعت. The grid opens on the tab used last.
- **موضوعات دیگر** opens a grid with tabs for مافیا و مسابقه, سینما و فانتزی, دنیای خودرو, شطرنج and آیین و مداحی.
- **سریال خارجی** opens «سایر سریال‌ها», the former «سریال کامل» programmes, directly. Once the catalogue has Turkish series, it first offers two large choices: «سریال ترکی» and «سایر سریال‌ها».

**Remote navigation**
- Up from the top row of a grid reaches the tabs. Left and Right move between tabs, and OK switches the grid.
- Back from a grid returns to the home tile that opened it. From «سایر سریال‌ها», Back returns to the series choice when it is shown.
- Back from the player returns to the grid, as before.
- A tile, tab or series choice appears only when it has something to play.

**Unchanged**
- The six themes, the artwork, the Persian date and the clock.
- Channel and programme IDs, the catalogue schema and the Persiana and GEM order (`filmOrder`).
- Health monitoring, the documentary rounds, saved grid positions and the update button.

**Continue watching**
- The live button replays the last station, as before.
- The series button opens the most recently watched Iranian or Turkish episode directly, using the existing saved positions.
- On first launch, focus starts on «ادامهٔ تماشا».

## Turkish series

The catalogue can now hold `turkishSeries` (complete, consecutive runs) and `turkishSeriesInProgress` (`"status": "partial"` or `"ongoing"`). They use exactly the same episode format as `iranianSeries`:

```json
"turkishSeries": [
  { "id": "kara-para-ask", "title": "کارا پارا عشق",
    "episodes": [ { "id": "s1e1", "season": 1, "number": 1, "videoId": "<11-character YouTube ID>" } ] }
]
```

- Series IDs must not repeat an Iranian series ID, because both libraries share the progress store.
- A YouTube video may appear in only one episode.
- Releases before 3.9 ignore both keys, so adding them doesn't break older TVs.

Turkish episodes play in the same in-app player, with the same list, «ادامهٔ تماشا», «قسمت بعد» and resume behaviour. Subtitles start on and use the Persian choice described below. «زیرنویس خاموش» turns them off.

The bundled catalogue (revision 11) has no Turkish series yet. See the next section for why.

## Source review

This release was prepared in a cloud session whose network allowed only GitHub and package registries. YouTube, the broadcasters' websites and their stream hosts were unreachable from it, and the web-fetching tool was refused for the same hosts. That means no candidate below was opened, played or checked from Portugal. Following the catalogue rules (`verified` only after successful playback), **no new channel, film, documentary or series was added.** Each candidate still needs the checks in the last column.

### Live TV

| Source | Owner | Current state in the app | What to check before adding |
| --- | --- | --- | --- |
| DW English | Deutsche Welle | `dw-english`: official YouTube live tile | Whether dw.com/en/live-tv exposes a public HLS URL. If it does, add it to `dw-english` as an `unverified` stream. The TV's monitor then shows it only after it plays. |
| France 24 English | France Médias Monde | `france24-english`: official YouTube live tile | Same check on france24.com/en/live. |
| France 24 Français, Arabic, Español | France Médias Monde | Not in the catalogue | The official YouTube channel ID for each language (from the France 24 site), then the same embed checks as the English tile. |
| NASA+ | NASA | NASA YouTube live and playlist tiles in علم و فضا | NASA's pages describe NASA+ as free, with no subscription. Search found no published HLS address. Keep the YouTube tiles. A NASA+ tile would need a documented stream or a handoff to the NASA app. |
| Euronews, Al Jazeera, Sky, ABC, CBS, CNN | Each broadcaster | Existing YouTube live tiles | No change |

### Documentaries and films

| Source | Current state | What to check |
| --- | --- | --- |
| ARTE | The existing DW and ARTE-labelled documentary playlists remain | Whether arte.tv's free programmes play without an account in Portugal, and whether ARTE permits embedding or only its own app |
| TV5MONDEplus | Not in the catalogue | It is a free platform with an account. Whether it plays in Portugal and offers a TV app handoff. There is no known public stream, so it would be an external tile at most. |

### Turkish series

| Candidate | What to confirm |
| --- | --- |
| Kara Para Aşk (ATV) | Whether the official channel's full-episode playlist matches the broadcast numbering. Sources disagree on the episode count (54 or 67), so confirm the numbering from the playlist itself. |
| Hercai (ATV) | Whether an official full-episode playlist exists and is free, embeddable and available in Portugal |
| Karadayı (ATV) | Same checks |
| Kardeşlerim (ATV) | Same checks |
| Kanal D, Show TV, TRT, tabii archives | Whether full episodes are free from Portugal. Broadcaster websites and tabii use their own players; only YouTube video IDs fit the series screens. |

For each Turkish candidate, record the official channel and full-episode playlist. Check every video's ID, number, public playback, embedding (not error 150) and Portugal availability, using the same per-video checks as the 3.8 series. Exclude trailers (fragman), scenes (sahne) and highlights. Use `turkishSeriesInProgress` with `partial` when episodes are missing.

## Subtitle compatibility

| Source | Player | Available captions | Persian subtitles | Difficulty of a future translation module |
| --- | --- | --- | --- | --- |
| YouTube documentaries, films, topic and «سایر سریال‌ها» tiles | YouTube embed | Video's own tracks, human or automatic | Yes: own Persian track, else YouTube's translation (unchanged since 3.4) | None needed |
| Turkish series on YouTube (when added) | YouTube embed, series player | Varies by video. Many official Turkish uploads have automatic Turkish captions and some have human English or Arabic. | Requested automatically: YouTube's translation when a track exists | None needed |
| Iranian series | YouTube embed, series player | Usually automatic Persian | Not needed. Captions remain optional, as before. | — |
| YouTube live news | YouTube embed | Live captions on some channels | No: YouTube doesn't translate live captions | High: needs live speech recognition |
| Native Media3 streams (Persiana, GEM, IRIB, …) | Media3 | Embedded WebVTT or CEA-608 tracks, if the stream carries them | Only if a stream carries Persian | Medium: a module could add a translated text track through `SubtitlePolicy` |
| Broadcaster websites or apps (tabii, TV5MONDEplus, ARTE) | External | Decided by the provider's player | Not controllable by this app | Not possible from this app |

`Subtitles.kt` holds the decision for each player type: `SubtitlePolicy.forChannel` and `SubtitlePolicy.forEpisode`. A later translation module can add its own `SubtitleMode` there without changing the playback code. No speech recognition or dubbing is included.

## Remaining limitations

- No new channels, films, documentaries or series in this release, for the reasons above.
- There was no Android SDK or emulator in this session. The GitHub Actions workflow ran the build, unit tests and lint. The UI and D-pad navigation weren't exercised on an emulator or the Xiaomi box. See the release notes for exactly what was checked.
- Playback in the official YouTube app (an embed-blocked fallback) still saves no position.
