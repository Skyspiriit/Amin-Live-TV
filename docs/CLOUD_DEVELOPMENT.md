# Cloud development and iPhone review

The Windows computer is not a build dependency. A published Codex Cloud environment and GitHub-hosted Ubuntu builds are available; production signing is separate.

## Reusable Codex environment

Published name **Amin-Live-TV**; repository `Skyspiriit/Amin-Live-TV`; select branch `dev/cloud-series-interface` for this review, `main` after approval/merge. The saved environment mounts the repository at `/workspace/Amin-Live-TV`. Its verified installation script prepares JDK 17 and SDK 35 in `/workspace/amin-android-toolchain`; each new shell must source `/workspace/amin-android-toolchain/activate.sh`. This also configures Java for the cloud machine's proxy and trusted CA store without embedding credentials. No environment secrets are configured.

Start instructions read `AGENTS.md` and the three continuation documents, run `bash scripts/check.sh` on this branch, update status and prepare a PR. No production publish/install without explicit approval. Older `main` lacks the new scripts and preview isolation until this PR is approved; the saved guidance identifies that difference. Do not distribute its old debug build as an isolated preview.

Outside that prepared environment, the versioned portable Linux install command is **`bash .codex/setup.sh`**. GitHub Actions verified this from a fresh SDK directory in [run 46057270060](https://github.com/Skyspiriit/Amin-Live-TV/actions/runs/46057270060).

Use an owner-only environment. No production credentials, signing key, VPN, account or paid service is needed. Package downloads need `dl.google.com`, Maven Central, Gradle plugin/distribution hosts and GitHub download redirects. Content research needs separate publisher access; a cloud connection is not automatically Portugal.

Setup targets Linux x86_64, JDK 17 and Node 18+. It installs missing Ubuntu packages where privileged installation is available, verifies Google's CLI-tools 19 archive, installs Platform 35/Build Tools 35.0.0 and runs checks. Gradle 8.13 is checksum-pinned. A fresh shell can call `bash scripts/check.sh`, which restores SDK/Java paths. First setup accepts the normal Android SDK package licences as part of build preparation.

Published on 27 November 2026; API configuration ID `8ca60d4e-72bb-4100-a816-b2f40ba02fa6~asenvcfg_c6143a7445ac8191949c63d352963138`, `runnable: true`. Preparation built baseline `main`, passed 22 JVM tests and lint (0 errors, 17 warnings). Its software-only TV emulator did not boot in five minutes. Use the separate GitHub KVM emulator job for automated UI checks; build success alone is not playback verification. [Official Codex environment guide](https://learn.chatgpt.com/docs/environments/cloud-environments).

## Build/download from iPhone

GitHub → repository → **Actions → Build Android TV APK → Run workflow** → choose development branch. Push/PR events also build. The runner downloads its own SDK; no local file/server/USB/device is required.

After success download **Amin-TV-Preview-<commit>**. Its ZIP contains `Amin-TV-Preview.apk` plus `verification.json` (package, source commit, SHA-256). This is a separate preview, **not** a production update. Reports are attached separately. Artifact downloads may require GitHub sign-in, expire after 30 days, and can be rebuilt by rerunning the workflow.

Fresh cloud runners can have different debug keys. Later previews may require a fresh **preview-only** install; never uninstall production to resolve a preview signature conflict. Preview testing does not establish production signed-upgrade compatibility. Production data/key remain untouched.

Emulator UI jobs need KVM. Emulator Android version, network and playback results are not Xiaomi/Portugal evidence. Real provider restrictions/ads/outages need separate checks.

The **Android TV remote navigation** CI job runs API 30 TV instrumentation and saves reports/screenshots for 14 days. Locally select an emulator explicitly, then run `ANDROID_SERIAL=emulator-5554 bash scripts/check-tv.sh`. The script refuses a physical serial. Tests assert the isolated QA identity before touching QA bookmarks. They exercise real bundled series, RTL card numbering, disabled missing episodes, D-pad traversal, Back focus and persisted 137.25-second bookmarks across Activity restarts. They do not assert online YouTube playback.

## Production approval/signing

The audit found no GitHub signing secrets. Keep the current key in secure local storage; do not copy it into Codex Cloud or generate a replacement. Preview builds require no key.

A later approved cloud release needs specific permission to provision the existing key into protected GitHub Actions secrets: `AMIN_LIVE_TV_KEYSTORE_BASE64`, `AMIN_LIVE_TV_STORE_PASSWORD`, `AMIN_LIVE_TV_KEY_ALIAS`, `AMIN_LIVE_TV_KEY_PASSWORD`. It then needs separate release approval and certificate comparison against the previous public APK. This first checkpoint performs neither provisioning nor production publishing. Repository visibility and paid-service configuration remain unchanged.
