# Amin TV architecture

Audited 27 November 2026 at `8d37208` / production **3.9.2, code 18**. Continue the existing app. Read [development status](DEVELOPMENT_STATUS.md) first.

## Build and identity

One native Kotlin Android module, Activity/View widgets, landscape, Persian RTL and Leanback launcher. Java 17; Gradle 8.13; AGP 8.13.2; Kotlin 2.2.21; compile/target SDK 35; minimum SDK 26; Build Tools 35.0.0. Runtime dependencies: Media3 1.8.0 player, HLS, DASH and UI. JVM tests: JUnit 4.13.2 and org.json 20240303. No backend, account or database server.

Production package **`com.enigma.aminlivetv`** and certificate SHA-256 **`ce108de5ce0ebdb7e5d118dc60ac8a2a87899f2553cdb3e9eb6abea1dfe16b3d`** must remain compatible. Private keys never belong in source or development environments. Previews use `com.enigma.aminlivetv.qa`, a separate label and `-preview` suffix; they cannot replace production.

## Components

| Component | Responsibility |
|---|---|
| `MainActivity` | Home/menu, category grids, remote navigation, live continuation, update notice |
| `IranianSeriesActivity` | Titles/episodes, series controls, samples, checkpoints, Back/restart/next |
| `IranianSeries` / `SeriesProgress` | Season/episode ordering, exact-ID bookmark matching and continuation |
| `CatalogueStore` / `CatalogueParser` | Bundled/cache-first load, atomic cache, bounded HTTPS refresh/validation |
| `LivePlayer` | Native Media3 playback, last-good source and failover |
| `ChannelHistory`, `ViewingSession`, `ChannelMonitor`, `StreamProbe`, `MediaSample` | Local health/history, bounded probes and retry scheduling |
| `YouTubePlayer` | Official IFrame player, live/playlist/reviewed IDs, timeouts, episode samples, official YouTube handoff |
| `DocumentaryQueue` | Persistent 25-video rounds, position, shared watched/skipped history, refresh/cache fallback |
| `SubtitlePolicy` | Existing native/YouTube caption rules; Iranian episodes default off |
| `ReleaseCheck` / `AppUpdate` | GitHub discovery/download, package/version/certificate validation, Android installation confirmation |
| `AppTheme`, `IntroView`, `Logos` | Saved palettes, lifecycle-aware intro, cached imagery |

Amin Player is a visual reference only: warm gold accents, compact numbered episode grid, strong focus. Its WebView catalogue and VPN are not dependencies and are not modified.

## Catalogue and playback

`channels.json`: schema 1, revision 12; 204 channels, 32 `programmes`, 35 `programmes2`, **24 complete + 13 partial + one ongoing Iranian series / 1,134 episode records**. It is bundled and fetched from the public `main/channels.json` raw GitHub URL. Updates must preserve existing IDs. Catalogue checks run every 15 minutes while browsing; release checks every six hours after success and retry after 15 minutes on failure. Catalogue changes wait until leaving playback.

Native streams carry source attribution. Other tiles use official embedded YouTube channels/playlists or bounded reviewed IDs. Do not extract protected streams or bypass embedding, membership or geographic restrictions. External YouTube playback provides no accurate resume samples. Historical public/embed-only source decisions are not authorization proof for new additions under the current request.

Complete runs use `iranianSeries`; partial/ongoing runs use `iranianSeriesInProgress`. Parsing sorts season/episode numbers and rejects duplicates. Preserve original numbers and stable series/episode/video IDs. UI placeholders can represent gaps without changing `Episode` or saved progress. Unknown final counts must stay unknown until corroborated.

## Local data

| Store | Preserve |
|---|---|
| `viewing` | Last successful station, category/grid positions |
| `iranian-series-progress` | `seriesId/episodeId` → exact IDs, seconds, completion and timestamp |
| `channel-health` | Health, last-good source, weighted viewing history |
| `documentary-rounds` | Per-tile queues/positions and shared watched/skipped history |
| `appearance` | Theme, intro animation/sound |
| `update` | Pending verified installation |
| Internal `files/channels.json` | Last valid catalogue, written atomically |

No migration is needed for layout work. Series progress is sampled each second, checkpointed every five seconds and on state/lifecycle changes; Back samples before leaving. Matching all three IDs prevents resuming a replaced upload at an unrelated timestamp.

## Verification boundaries

[Cloud development](CLOUD_DEVELOPMENT.md) covers setup/artifacts. Tests cover catalogue, channel/update policy, documentary rounds, series identity/order and captions. Emulator and real Xiaomi results must be separate. On **26 November**, production 3.9.2 actually updated through the Xiaomi updater; saved theme/continuation and the Sports menu survived. That is not a test of the new interface.

GitHub signing secrets were absent in the 27 November audit. Production still uses the existing secure local key; future cloud signing needs specific owner-approved provisioning into protected Actions secrets. Development needs no key. Repository visibility remains public and unchanged.

Current `main` contains no VPN. An unrelated optional-VPN local commit and an unmerged historical news branch are outside this checkpoint. Keep native/full-screen playback, startup, subtitles, network recovery and documentary behaviour unchanged.
