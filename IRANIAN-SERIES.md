# Persian series sources and verification

Checked 18 November 2026. All 119 selected videos came from the [official SHEYDA channel](https://www.youtube.com/channel/UC3_wFuKt0PT2Fj7gunthcYw), whose description states it licenses its catalogue. Its rights-management partner confirms distribution on YouTube and specifically identifies the official SHEYDA channel for Shahrzad. [ZigZag partnership](https://t.me/zigzagnetworkfa/1311) · [Shahrzad rights announcement](https://t.me/s/zigzagnetworkfa?q=%23Copyright)

| Title | Full episodes | Episode length | Publisher’s YouTube archive |
| --- | ---: | --- | --- |
| هزارتو | 13 | 44–54 min | [Complete series](https://www.youtube.com/playlist?list=PLHY2hywUNdRaI3GG8eumgQtRrqY1ZyTQt) |
| شهرزاد | 59 across three seasons (28 + 15 + 16) | 52–93 min | [Season 1](https://www.youtube.com/playlist?list=PLHY2hywUNdRZbMPoJp5Gc3MYmmyrSkjw8) · [Season 2](https://www.youtube.com/playlist?list=PLHY2hywUNdRYWcdGCMBlO83siMXN47bFK) · [Season 3](https://www.youtube.com/playlist?list=PLHY2hywUNdRbSZz3HFvKxr7QR-ZgRE2Ag) |
| شاهگوش | 28 | 47–73 min | [Complete series](https://www.youtube.com/playlist?list=PLHY2hywUNdRaLZvd3SNjZXdJEPmr5ARnk) |
| شکارگاه | 9 | 42–58 min | [Complete series](https://www.youtube.com/playlist?list=PLHY2hywUNdRa2XqOVI5HnSlsZkSbI8TPV) · [Filimo finale announcement](https://t.me/s/filimo?q=%23%D8%B4%DA%A9%D8%A7%D8%B1%DA%AF%D8%A7%D9%87) |
| گل‌سنگ | 10 | 39–54 min | [Complete series](https://www.youtube.com/playlist?list=PLHY2hywUNdRaXaLGjqdDfWTxjFNv1Nih6) · [Distributor announcement](https://t.me/s/sheyda_vod?before=1876) |

The audit checked each numbered episode against its official YouTube watch page. All 119 used distinct video IDs, belonged to the same publisher, reported public playback, permitted embedding and had full-episode runtimes. The library sorts by season and episode number, including lists whose publisher page opens in reverse order. Watch-page metadata and starts in the emulator establish availability from the test environment. YouTube may still enforce regional restrictions or change an upload later.

DigiStudio, Shahre Farang, Ghasedak and NetFilma all publish candidate archives. I found no title-level rights confirmation for the candidate classics from those accounts. Shahre Farang’s streaming site links a different YouTube account from the candidate archive. IMVBox confirms its licensing and links its YouTube channel, but I could not verify a qualifying complete series on that channel. [DigiStudio](https://www.youtube.com/@DigiStudio/playlists) · [Shahre Farang archive](https://www.youtube.com/@ShahreFarangs/playlists) · [Shahre Farang streaming site](https://shahrefarang.live/) · [Ghasedak](https://www.youtube.com/@GhasedakChannel/playlists) · [NetFilma](https://www.youtube.com/@NetFilmaChannel/playlists) · [IMVBox licensing and channel](https://www.imvbox.com/en/faq)

## Playback and resume

The Android TV emulator played the first full episode of هزارتو through the official YouTube IFrame player. The player saved the exact series, episode and video IDs and a position of 140.23 seconds. After force-stopping and reopening the app, the same video resumed at 140.53 seconds. Restart played from the beginning. An end event from the real player recorded episode completion and offered the next episode. Selecting it started the second video.

The emulator also started episode one of each of the other four series. Replacing the test APK preserved the episode bookmark, and a test catalogue with reversed episode arrays displayed in the correct order. The signed v3.5.0 APK upgraded v3.4.0 in place on the emulator, retained the live TV resume action and played an episode in the new library. Twelve unit tests and debug and release lint passed. No playback or install test was performed on the physical Xiaomi box.

Amin Live TV stores progress only for its own embedded player. If a publisher blocks embedding, the viewer can open the exact video in the official YouTube TV app. Playback there does not update the app’s saved position.
