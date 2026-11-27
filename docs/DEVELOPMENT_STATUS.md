# Amin TV development status

Updated **27 November 2026**. Branch `dev/cloud-series-interface`, baseline `8d37208` (`main`). **No production release or physical-device installation for this checkpoint without new owner approval.**

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
2. **Persian interface implemented; final CI/evidence pending.** Compact adaptive RTL numbered cards, visible disabled gaps, progress/watched labels, one-click timestamped continuation, restored title/card focus and smaller library thumbnails. Player, subtitle policy and progress formats remain unchanged. Optional verified `episodeCounts` bounds can expose trailing gaps later; the current catalogue has no such additions. Local clean-copy build, all 29 JVM tests and both lint variants passed. Two Android TV API 34 emulator instrumentation tests passed for gaps, Left/Right/Down/Up, Back/title focus and exact stored bookmark across Activity restarts. Screenshot capture timing is being refined; actual YouTube playback is a separate check. No physical Xiaomi testing.
3. **Catalogue research: pending, documentation only.** Nine requested titles; no new playable entries here.
4. **Review checkpoint: pending.** Checks, preview artifact, UI evidence and PR; no production release.

## Resume work

Read this file, architecture and roadmap. Continue the current milestone, record exact test/commit/workflow results, and stop at a stable tested PR before implementing new catalogue titles or another major phase.

## Physical checks still required for the new UI

Normal-distance text, RTL/numbering, unavailable entries, focus/scroll restoration, D-pad-only operation, exact resume/pause/Back after restart, captions off and next-episode/fallback. These are **not tested on Xiaomi for this checkpoint**.
