# Content added in 0.3.2

Checked 18 November 2026 from Portugal. For each playlist, the first five videos were checked for length and for whether they play without signing in and can be embedded. The results come from the Android TV emulator (Google TV, Android 11, 1080p). The Xiaomi box was not used for these tests.

| Source | Playlist | Group | Emulator result |
| --- | --- | --- | --- |
| Free Documentary | Earth Untold | مستند | in-app |
| Free Documentary – History | Legends of the Pharaohs | مستند | not opened in the app; 5/5 first videos play |
| FD Adventure | Spiky Gold Hunters | مستند | not opened in the app; 5/5 play |
| ENDEVR | Europe – Best Docs | مستند | not opened in the app; 5/5 play |
| DW Documentary | Train Ride Documentaries | مستند | not opened in the app; 5/5 play |
| Timeline | Myth Hunters | مستند | not opened in the app; 5/5 play |
| GEDEON DOC | History & Civilizations | مستند | in-app |
| Real Wild | Wildlife Documentaries To Fall Asleep To | حیات وحش | in-app |
| Free Documentary – Nature | Continents: Wild Europe | حیات وحش | in-app |
| Free Documentary – Ocean | Best Ocean Documentaries | طبیعت | YouTube app (embedding blocked) |
| Free Documentary – Space | Best Space Documentaries | علم و فضا | in-app |
| The Royal Institution | Science History lectures | علم و فضا | in-app |
| Mosfilm | Full movies with English subtitles | فیلم سینمایی | in-app |
| FilmRise Movies | Based on True Stories | فیلم سینمایی | YouTube app |
| FilmRise Movies | Comedy | فیلم سینمایی | YouTube app |
| Movie Central | Christmas Movies | فیلم سینمایی | YouTube app |
| Shout! Studios | Overly Dramatic Drama | فیلم سینمایی | in-app |
| Shout! Studios | B&W / Film Noir | فیلم سینمایی | YouTube app |
| FilmRise Television | Paul O'Grady: For the Love of Dogs | سریال کامل | YouTube app |
| FilmRise Television | Greatest Prison Escapes | سریال کامل | in-app |
| Unsolved Mysteries – Full Episodes (FilmRise) | Fan Favorites | سریال کامل | in-app |

"YouTube app" means the owner blocks embedding. The app then opens that exact video in the official YouTube TV app. On the emulator, the hand-off opened YouTube with the right video, but the emulator's YouTube app doesn't play. On the Xiaomi box, the same hand-off played BBC Persian on 18 November.

## Rejected

- **Korean Classic Film (KOFA).** It's official, but 3 of the first 5 films in two playlists require signing in (age-restricted), so they won't play on the TV.
- **"NEW Unsolved Mysteries" (@OfficialUnsolvedMysteries).** It's mostly 2–3 minute clips and podcast episodes. The full episodes come from FilmRise's Unsolved Mysteries channel instead.
- **Free Documentary – Engineering.** I found no such channel.
- **FilmRise Television's other playlists.** These are *Z Nation* (zombies), *Cheaters* and comedy roasts. They're full length but unsuitable, so I picked the gentler shows.
- **Unsolved Mysteries "Haunted Cases".** Its first five episodes include one that requires signing in. I used "Fan Favorites" instead.

## Compatibility

The 12 documentary-group playlists are in `programmes`. The unchanged 0.3.1 code accepted catalogue revision 6 ("Activated revision 6") and showed them. Movies and series are in `programmes2`, which 0.3.1 ignores. On the emulator, TV1, a Persiana Film channel and DW News played as before, and the Film grid is unchanged.
