# Amin TV development status

Updated **27 November 2026**. Branch `dev/cloud-series-interface`, baseline `8d37208` (`main`). **No production release or physical-device installation for this checkpoint without new owner approval.**

Review: [pull request #2](https://github.com/Skyspiriit/Amin-Live-TV/pull/2), open and unmerged. The latest branch/PR Actions runs attach installable QA APKs and reports; use the source commit in each artifact's `verification.json`. Owner review is the next step.

## Production baseline

3.9.2 / code 18; package/certificate in [architecture](ARCHITECTURE.md). Catalogue revision 12: 38 series / 1,134 episodes. Keep the catalogue unchanged.

Physical Xiaomi update on 26 November used the app's automatic download and Android confirmation. Installed APK hash, saved theme/continuations and Sports menu were verified. New Sports playback was not tested. Historical 3.9.1 preview resume tests are recorded separately; they do not verify this interface change.

## Audit

- Reviewed the tracked documentation and source/build workflows. Historical release/source reports are dated snapshots; current README/catalogue supersede earlier totals. The series inventory mixes current counts with older 3.6/3.7 findings and public/embed-only permissions; current additions need stronger authorization evidence.
- No open GitHub issues returned. `origin/feature/official-free-news` and an unrelated local VPN commit are unmerged historical work, excluded here. No VPN is implemented on `main`.
- Existing debug artifacts share production identity unless a test-catalogue flag is passed. This branch makes all previews separate and suppresses production update offers in ordinary previews.
- Existing releases publish on tag push. A manual owner-approved flow will be proposed. **No GitHub signing secrets existed** during the audit; no secret values were read or transferred.
- The private **Amin-Live-TV** Codex environment is published and the environment API reports `runnable: true`. Its preparation built the baseline, passed 22 JVM tests and lint (0 errors, 17 warnings). A software-only emulator did not boot within five minutes; no cloud-emulator playback claim. GitHub-hosted builds also verify the versioned setup independently of the owner's PC.
- Partial lists omit unavailable numbers. UI placeholders will expose known gaps without changing saved IDs/positions. Unknown final counts need future verified metadata.

## Milestones

1. **Audit/cloud preparation passed.** Commit `931121e`; [fresh Linux SDK/bootstrap build](https://github.com/Skyspiriit/Amin-Live-TV/actions/runs/46057270060) passed, including 22 JVM tests, catalogue preservation and both lint variants. Codex environment published. The original Windows generated-file cache had a permissions failure; a clean source copy built successfully, with no permission/data changes. The release workflow proposal permits only an explicit owner-approved dispatch from `main`, checks the original certificate and requires a newer version code. No production job was run.
2. **Persian interface passed.** Commit `a703296`; [GitHub build and Android TV navigation](https://github.com/Skyspiriit/Amin-Live-TV/actions/runs/46059211041) both green. Compact adaptive RTL cards, visible disabled gaps, progress/watched labels, one-click timestamped continuation, restored title/card focus and smaller library thumbnails. Player, subtitle policy and progress formats remain unchanged. Optional verified `episodeCounts` can expose trailing gaps later; catalogue unchanged. All **29 JVM tests / 7 suites**, both lint variants (0 errors) and two instrumentation tests on GitHub API 30 TV passed. The same two tests passed locally on API 34 TV. [Detailed results and screenshots](series-interface-checkpoint.md).
3. **Catalogue research documented.** [Nine requested titles](catalogue-research-2026-11-27.md), stable public IDs/listings/sample restrictions recorded. Several numbered runs found, but no newly independently authorized complete run certified. First uploads for لحظه گرگ و میش / قهوه تلخ / Green Square پاورچین block embedding. Current metadata for ساختمان پزشکان 3/4 now allows embedding; existing approved gaps remain until authorization and actual playback review. Zero data changes/additions.
4. **Stable review checkpoint prepared.** Isolated installable CI preview and reports available, private Codex environment published/runnable, documentation and UI evidence ready for PR. No production release or physical installation. Do not begin catalogue implementation before interface acceptance.

## Actual playback evidence

API 34 Android TV **emulator**, existing هزارتو episode 1: pause/save at 137s; Back/reopen sampled 137.641491s. After process restart, exact fractional bookmark survived and home Continue Watching reopened the same ID at start=137 (sample 138.508998s). Completion saved watched state and focused next; next episode began at 0.360295s. App caption/translation mode stayed off on open, resume and next; no subtitle implementation changed. Details/limits are in the interface report. **No Xiaomi testing** for this checkpoint.

## Changes and remaining limitations

- Environment/build: `AGENTS.md`, `.codex/android-env.sh`, `.codex/setup.sh`, `.gitattributes`, wrapper checksum, `scripts/check.sh`, `scripts/check-tv.sh`, `scripts/verify-preview.cjs`, both Actions workflows and preview configuration in `app/build.gradle.kts` / `ReleaseCheck.kt`.
- Interface: `IranianSeriesActivity.kt`, `IranianSeries.kt`, new `EpisodeCards.kt`, Persian strings, seven JVM tests and two Android TV instrumentation tests. Production player/progress preferences unchanged.
- Continuation/research: README, architecture/roadmap/status/cloud guides, interface report/screenshots and nine-title research with public metadata evidence.
- Remaining lint warnings concern older SDK/dependencies, icons and pre-existing unused resources. Dependency upgrades are a separate milestone. Original Windows generated cache was unusable; clean-copy and independent Linux builds passed.
- Unknown finale/season lengths cannot be guessed. External YouTube playback has no accurate in-app tracking. No exhaustive live/documentary/network-outage regression or geographic certification in this checkpoint.
- Cloud preview/development needs no owner PC. **Production cloud signing is not yet provisioned**: no signing secrets existed; specific owner approval is needed to place the existing key in protected Actions secrets for a later approved release. Never replace the production certificate.

## Resume work

Read this file, architecture and roadmap. Continue the current milestone, record exact test/commit/workflow results, and stop at a stable tested PR before implementing new catalogue titles or another major phase.

## Physical checks still required for the new UI

Normal-distance text, RTL/numbering, unavailable entries, focus/scroll restoration, D-pad-only operation, exact resume/pause/Back after restart, captions off and next-episode/fallback. These are **not tested on Xiaomi for this checkpoint**.
