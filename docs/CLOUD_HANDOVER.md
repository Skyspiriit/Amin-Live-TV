# Amin TV — Codex Cloud handover

**28 November 2026. Scope: Amin TV only.** This handover changes documentation only. Do not inspect, modify or push Morphent, segmentation projects or other applications. Do not implement a new feature, merge, publish, provision signing credentials or install on a device merely because this handover is available.

## Where to continue

Repository: [Skyspiriit/Amin-Live-TV](https://github.com/Skyspiriit/Amin-Live-TV), public, visibility unchanged. The application is **Amin Live TV**, separate from Amin Player. Continue the existing app; do not rebuild it.

| Source | Status |
|---|---|
| `main`, `8d37208` | Published production **3.9.2 / versionCode 18** |
| `dev/cloud-series-interface`, `79ef32e` | Tested compact Persian-series UI and cloud build preparation; [PR #2](https://github.com/Skyspiriit/Amin-Live-TV/pull/2), open and unmerged when handed over |
| `docs/cloud-handover-20261128` | This handover, based on that tested development branch; application source/catalogue identical to `79ef32e` |

Read this file, [development status](DEVELOPMENT_STATUS.md), [architecture](ARCHITECTURE.md), [roadmap](ROADMAP.md), [cloud instructions](CLOUD_DEVELOPMENT.md), and `AGENTS.md`. Then inspect current GitHub commits/PR state, since the owner may accept a review later. Historical release/source reports are dated evidence, not proof of today's provider availability.

## Cloud environment and iPhone workflow

The private owner-only Codex environment **Amin-Live-TV** is published and reports **runnable**. It mounts the repository at `/workspace/Amin-Live-TV`; no production credentials are configured. Its default repository ref is still `main`, so **select the handover/development branch explicitly** for the current prepared code. Old main lacks the new scripts and isolated preview configuration until PR #2 is approved and merged.

From the iPhone, open Codex Cloud, choose **Amin-Live-TV**, select this handover branch, and ask the session to read this file before changes. No owner computer, USB connection, local server or Xiaomi connection is required for ordinary source development and GitHub builds. This handover does not create another chat or change environment permissions.

Prepared cloud toolchain, in each fresh shell:

```bash
cd /workspace/Amin-Live-TV
source /workspace/amin-android-toolchain/activate.sh
bash scripts/check.sh
```

For a fresh Linux machine outside that prepared environment:

```bash
bash .codex/setup.sh
```

The versioned setup prepares Java 17 and SDK 35, pins Google's CLI archive and Gradle checksum, and runs the normal checks. Network access is needed for Google Maven/SDK, Maven Central, Gradle and GitHub downloads. Content research needs publisher access separately; a cloud connection does not prove availability in Portugal.

GitHub from iPhone: **Actions → Build Android TV APK → Run workflow → choose development/handover branch**. Download `Amin-TV-Preview-<commit>` after success; its ZIP has the APK and `verification.json`. Reports are separate artifacts. APK retention is 30 days, reports 14 days; rerun an approved development build if they expire. A GitHub source push or review artifact is not a production release.

## Architecture, dependencies and content

One native Kotlin Android module, Activity/View interface, landscape, Persian RTL and Leanback. JDK 17, Gradle 8.13, AGP 8.13.2, Kotlin 2.2.21; compile/target SDK 35, minimum 26, Build Tools 35.0.0. Runtime dependencies are Media3 1.8.0 player/HLS/DASH/UI. Tests use JUnit, real JVM `org.json`, and AndroidX instrumentation. There is no app-account backend, database service or implemented VPN in this codebase.

| Component | Responsibility |
|---|---|
| `MainActivity`, `AppTheme`, `IntroView`, `Logos` | Home/category grids, large remote-friendly controls, Continue Watching, saved themes and intro |
| `CatalogueStore`, `CatalogueParser` | Bundled/cache-first catalogue, atomic disk cache, bounded remote refresh and compatibility validation |
| `LivePlayer`, channel health/history/probes | Native Media3 playback, source failover and last-good continuation |
| `IranianSeriesActivity`, `EpisodeCards` | Series library, compact numbered RTL cards, visible disabled gaps, focus restoration, player controls |
| `IranianSeries`, `SeriesProgress` | Stable identities, season/episode ordering, exact-ID local bookmarks and continuation |
| `YouTubePlayer`, `SubtitlePolicy` | Official embedded playback, samples, error handling and official YouTube-app handoff; existing caption policies |
| `DocumentaryQueue` | Persistent shuffled rounds of up to 25 distinct approved videos, shared watched/skipped history, next/Back/rollover behavior |
| `ReleaseCheck`, `AppUpdate` | GitHub update discovery/download and package/version/certificate validation before Android installation confirmation |

Catalogue `channels.json`: schema 1, revision 12; **204 channels, 32 programmes, 35 programmes2; 38 Iranian series / 1,134 playable episode records** (24 complete, 13 partial, one ongoing). The app refreshes the public raw `main/channels.json`. Current changes are research only: **do not modify the catalogue** before interface acceptance and source verification. Keep existing IDs and catalogue compatibility. Source-less/unplayable entries are not a reason to restore dead or unofficial streams.

## Completed features to preserve

- Persian home with live, films/documentaries, series and settings; saved fantasy/gold themes and skippable intro. Large D-pad focus and simple remote navigation.
- Live TV playback/failover, successful-channel history and Continue Watching. During viewing, Up selects the previous channel and Down the next.
- Official YouTube content, approved documentary queues with saved round position, skipped/watched tracking, automatic next on completion and «مستند بعدی». WildEarth live stays separate from its on-demand fallback. Documentary subtitle behavior is unchanged.
- Series episode selection and locally retained playback progress; periodic checkpoints, pause, Back, restart, next episode and home continuation. Dedicated Iranian episodes default to captions/automatic translation off.
- Prepared compact episode cards: prominent Persian numbers, two–four responsive columns, progress/watched labels, one-click saved continuation, disabled unavailable numbers and Back focus restoration. Missing numbers are neither hidden nor renumbered. Unknown season/finale counts are not guessed.
- Compatible catalogue updates and production GitHub updater. Ordinary QA previews suppress production APK update offers.

## Identity, user data and signing

Production package **`com.enigma.aminlivetv`** and certificate SHA-256 **`ce108de5ce0ebdb7e5d118dc60ac8a2a87899f2553cdb3e9eb6abea1dfe16b3d`** must stay unchanged. Never generate a replacement production key or clear the app to make an update work.

Preserve preferences `viewing`, `iranian-series-progress`, `channel-health`, `documentary-rounds`, `appearance`, `update`, and internal `files/channels.json`. Series bookmarks retain series ID, episode ID, exact video ID, fractional seconds, completion and update time. Replaced uploads must not inherit an unrelated timestamp. No data migration is required for the current UI/documentation.

Previews use **`com.enigma.aminlivetv.qa`**, label **Amin TV Preview**, and a debug key. They cannot replace production. Fresh cloud runners may have different debug keys; do not uninstall production to fix a preview signature conflict. Do not upload credentials, keystores, private keys, local configuration, app data or raw personal/device logs.

Production cloud signing was **not provisioned** in the audit. Later owner-approved setup would use the existing key in protected Actions secrets, followed by separate release approval and comparison with the last signed APK. The development environment needs no key. The proposed release workflow is manual/owner-approved; it must not be invoked in this handover.

## Verification and known limitations

| Evidence | Result and boundary |
|---|---|
| [Final prepared-code CI run 46061095907](https://github.com/Skyspiriit/Amin-Live-TV/actions/runs/46061095907) | Fresh Linux SDK/build, **29 JVM tests**, both lint variants with zero errors, and **two API 30 Android TV instrumentation tests passed**. Source head `79ef32e`, GitHub test-merge tree `c711da10` |
| Local API 34 TV emulator | Same two navigation tests passed. Actual official YouTube playback: 137s saved bookmark reopened at 137.64s; process restart retained continuation; natural completion offered next; episode two began near zero with app caption mode off |
| Codex environment preparation | Baseline build, 22 JVM tests, lint 0 errors/17 warnings passed. Its software-only TV emulator did not boot within five minutes; **not** an emulator playback pass. Use the separate GitHub KVM job |
| Physical Xiaomi | Production 3.9.2 updater/package/hash/theme/continuation/Sports menu verified on 26 November. **No physical test of the new series UI**, and no new device activity during this handover |

Full scope, screenshots and omissions: [series checkpoint](series-interface-checkpoint.md). These are recorded tests from the preceding implementation checkpoint, not new application tests by this documentation task.

Known limits / unresolved work:

- Embedded start parameters use whole seconds; stored bookmarks retain fractions. Resume observations include playback elapsed before sampling. Official external YouTube-app playback cannot report accurate in-app resume.
- Unknown finales/whole missing seasons lack verified bounds; placeholders reflect the approved catalogue, not permanent absence from YouTube. Optional `episodeCounts` support is present but no bounds were added to the catalogue.
- The nine requested classics were [researched](catalogue-research-2026-11-27.md), not added. Authorization, complete original-episode mapping and every-video playback remain unverified for candidates. Some first videos block embedding; long combined editions must not be silently mapped to original episode numbers.
- Remaining lint warnings involve SDK/dependency/icon/unused-resource advice. No dependency upgrade is part of this checkpoint. The old Windows generated cache had a permissions failure; a clean copy and independent Linux builds passed.
- No exhaustive retest of every live station, documentary rollover, network outage/recovery or external-app handoff; cloud metadata is not geographic/physical playback proof. No live subtitle or VPN development.
- Historical news/VPN work is excluded. Do not merge an unrelated branch because it exists locally.

Normal checks: `bash scripts/check.sh`. Emulator UI checks: explicitly select a booted emulator, then `ANDROID_SERIAL=emulator-5554 bash scripts/check-tv.sh`. The script refuses physical serials; never choose the first attached device. App tests are not permission to install on the owner's TV.

## Owner preferences and next roadmap

The owner mainly works from an iPhone. Preserve elderly-friendly Persian text, RTL, obvious focus, minimal clicks, exact stable episode identities and Continue Watching. Make small, reviewable milestones. Report emulator and real Xiaomi results separately. No paid services, repository visibility changes, production releases or device installs/updates without explicit approval. **Work only on Amin TV; all other applications, especially Morphent/segmentation, are excluded.**

1. Owner reviews the prepared UI/PR #2 and this handover. Stop at that checkpoint; do not start a new major phase automatically.
2. With new device permission, test seating-distance text/overscan, first/last rows and season gaps, D-pad/Back restoration, pause/restart/resume, captions, next and YouTube fallback; sample live/documentary regressions on Xiaomi.
3. After interface acceptance, verify title-specific authorization, episode boundaries/order/counts and every stable video ID before any catalogue addition. مدار صفر درجه is a bounded 30-part research lead, **not an accepted addition**.
4. After separately approved merge, version/signing provisioning and release approval, produce a correctly signed compatible update. Do not reuse a preview key for production.

At every future stable milestone update `DEVELOPMENT_STATUS.md`, architecture/roadmap when appropriate, record source/test/artifact provenance, commit a focused branch and prepare review. Do not depend on local chat history or the owner's PC to reconstruct context.
