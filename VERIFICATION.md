# Amin Live TV 0.2.0 verification

Checked 17 November 2026. The catalogue contains **193 unique records**. Of these, **181** belong to at least one visible category before availability filtering. **91** have video playback evidence, and **15** have two verified source URLs. Twelve international news-only records are retained for maintenance.

| Category | Records | Initially visible | Video verified |
| --- | ---: | ---: | ---: |
| شبکه‌های داخلی | 65 | 64 | 56 |
| شبکه‌های خبری | 3 | 3 | 2 |
| شبکه‌های ورزشی | 11 | 7 | 6 |
| فیلم و سریال | 108 | 101 | 32 |

Totals overlap across categories. Among the 181 eligible records, 91 are verified, 77 are unchecked, 11 are unavailable and 2 are restricted. Twelve tiles are withheld on a fresh installation because the catalogue has no usable source and records them as unavailable or restricted. BBC Persian remains visible. A fresh installation has zero device-confirmed failures. Further hiding and recovery depend on the local playback and health cache. Fifteen multi-source stations use distinct tested Telewebion routes, which can share upstream outages.

## Sources and repairs

- **SalinTV and coverage.** Reopened the Persian archive, live gallery and Persiana directory, and retained the previous investigation of 43 archive pages and 96 individual channel pages. Existing domestic, sports and entertainment coverage was preserved. The News grid contains IRINN, IRINN 2 and BBC Persian. [SalinTV Persian archive](https://salintv.com/categories/live/farsi/) · [Live gallery](https://salintv.com/live-tv-streams/) · [Persiana archive](https://salintv.com/categories/live/farsi/mahvareh/persiana-group/)
- **Persiana.** Reviewed the current broadcaster player and decoded several seconds of video from 23 published Persiana CDN candidates. Configured 22 free broadcast feeds with visible programme or channel evidence. The 13 additions to the playable set include Türkiye, Medical, Docs, Junior, Nostalgia, Folk, SetMix, Travel, 4 Kurd, China, Rap, Podcast and Voyage. Cinema, Series, Family, Iranian, Comedy, Korea, Reality, Music and Fight were rechecked. The official site ships demonstration streams and uses a separate player flow. Sports 1–3 and Women Sports remain unavailable in this app. The broadcaster’s published Sports site returned HTTP 523 on one address and had an expired TLS certificate on the other address. Persiana+ is restricted to subscribers. The historical HD and Science listings have no separate verified feed. [Persiana live](https://persianagroup.tv/live.html) · [Persiana terms](https://persianagroup.tv/legal.html?p=terms) · [Official Sports viewing link](https://t.me/s/Sportspersiana/40570)
- **BBC Persian.** Checked the BBC’s official live page, the public Akamai HLS reference, documented DASH options and the verified YouTube channel. The Akamai candidate rendered video in a direct diagnostic test. The BBC provides its own live player and individual YouTube broadcasts. A YouTube broadcast has an official embed, but I found no dependable, embeddable 24-hour channel to use in this app. The BBC’s terms restrict taking media out of its player. I found no authorized native HLS or DASH integration, so BBC Persian stays in News with the Persian availability message. [BBC Persian live](https://www.bbc.com/persian/watch/bbc_persian_tv/live) · [BBC Persian on YouTube](https://www.youtube.com/@BBCPersian/streams) · [BBC terms](https://downloads.bbc.co.uk/usingthebbc/bbc_terms_of_use_19September2022english.pdf)

## Availability and viewing history

The home screen uses the bundled or last valid cached catalogue. A single background worker checks stream manifests and bounded samples of referenced video. Ambiguous failures, network changes and offline periods leave the last confirmed unavailable status intact and never mark a channel newly unavailable. Confirmed failures and recovered streams are reflected when categories reopen. The last source that rendered video gets first attempt on the next tune. Only 30 seconds or more of active playback contributes to viewing preferences. The scheduler decays the weight of older viewing, uses faster backoff for watched channels and reserves every third due check for a least-recently-checked channel. All viewing data stays on the TV.

The APK retains package `com.enigma.aminlivetv` and the existing release certificate. The app checks the [public GitHub catalogue](https://raw.githubusercontent.com/Skyspiriit/Amin-Live-TV/main/channels.json) asynchronously and preserves local preferences across catalogue revisions and signed upgrades.

## Acceptance tests

- **Android TV emulator.** Upgraded signed v0.1.0 to v0.2.0 in place and resumed IRIB TV1 from the previous install. One measured launch reached the home activity in 714 ms. Opened all four grids using the D-pad. Verified the exact three News entries and the BBC availability message.
- **Native playback.** Media3 rendered Persiana Family, Series, Cinema and Türkiye on the emulator. Türkiye played again after 20 rapid Up and Down channel changes. Amin Player’s package was still installed alongside the upgrade.
- **Availability and failover fixture.** Confirmed one hidden channel after an HTTP failure. A working backup and an unchecked tile stayed visible. The focused grid stayed stable, and the failed channel reappeared on reopening after its stream recovered. Media3 played the backup HLS feed and a DASH fixture. The monitor fetched bounded video samples for both formats.
- **Local persistence and outage fixture.** The monitor paused during playback. A failed tune added zero viewing sessions, and a 33-second successful tune saved one session and its source. After restarting, the last good source was attempted first. When all fixture sources and the catalogue endpoint became unreachable, cached channels stayed visible.
- **Local policy tests.** Six automated tests passed for news filtering, stale and uncertain results, source changes, recent viewing priority, scheduling fairness, qualified viewing time and video sample identification. Android lint passed with zero errors.
- **Physical Xiaomi box.** The signed APK upgraded the existing installation to 0.2.0 and the TV launcher resolved the updated app. Playback on that box was left untested because another app was on screen. Feed availability on the household connection in Portugal still needs live confirmation.

APK SHA-256 `9e06a9ebf4332596362c8affbee9446fb41b98c451abcfc3445c36c68b28027e`.
