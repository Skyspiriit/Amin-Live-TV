# Cloud development and iPhone review

The Windows computer is not a build dependency. Codex Cloud and GitHub-hosted Ubuntu use the same repository setup; production signing is separate.

## Reusable Codex environment

Name **Amin TV — Android TV development**; repository `Skyspiriit/Amin-Live-TV`; branch `dev/cloud-series-interface` for review, `main` after approval/merge. Install script: **`bash .codex/setup.sh`**. Start instructions: read `AGENTS.md` and `docs/DEVELOPMENT_STATUS.md`, run `bash scripts/check.sh`, work in a branch, update status and prepare a PR. No production publish/install without explicit approval.

Use an owner-only environment. No production credentials, signing key, VPN, account or paid service is needed. Package downloads need `dl.google.com`, Maven Central, Gradle plugin/distribution hosts and GitHub download redirects. Content research needs separate publisher access; a cloud connection is not automatically Portugal.

Setup targets Linux x86_64, JDK 17 and Node 18+. It installs missing Ubuntu packages where privileged installation is available, verifies Google's CLI-tools 19 archive, installs Platform 35/Build Tools 35.0.0 and runs checks. Gradle 8.13 is checksum-pinned. A fresh shell can call `bash scripts/check.sh`, which restores SDK/Java paths. First setup accepts the normal Android SDK package licences as part of build preparation.

A saved configuration is not a **published runnable environment**. Do not claim publication until the cloud UI and environment picker confirm it. Current state is in [development status](DEVELOPMENT_STATUS.md). [Official Codex environment guide](https://learn.chatgpt.com/docs/environments/cloud-environments).

## Build/download from iPhone

GitHub → repository → **Actions → Build Android TV APK → Run workflow** → choose development branch. Push/PR events also build. The runner downloads its own SDK; no local file/server/USB/device is required.

After success download **Amin-TV-Preview-<commit>**. Its ZIP contains `Amin-TV-Preview.apk` plus `verification.json` (package, source commit, SHA-256). This is a separate preview, **not** a production update. Reports are attached separately. Artifact downloads may require GitHub sign-in, expire after 30 days, and can be rebuilt by rerunning the workflow.

Fresh cloud runners can have different debug keys. Later previews may require a fresh **preview-only** install; never uninstall production to resolve a preview signature conflict. Preview testing does not establish production signed-upgrade compatibility. Production data/key remain untouched.

Emulator UI jobs need KVM. Emulator Android version, network and playback results are not Xiaomi/Portugal evidence. Real provider restrictions/ads/outages need separate checks.

## Production approval/signing

The audit found no GitHub signing secrets. Keep the current key in secure local storage; do not copy it into Codex Cloud or generate a replacement. Preview builds require no key.

A later approved cloud release needs specific permission to provision the existing key into protected GitHub Actions secrets: `AMIN_LIVE_TV_KEYSTORE_BASE64`, `AMIN_LIVE_TV_STORE_PASSWORD`, `AMIN_LIVE_TV_KEY_ALIAS`, `AMIN_LIVE_TV_KEY_PASSWORD`. It then needs separate release approval and certificate comparison against the previous public APK. This first checkpoint performs neither provisioning nor production publishing. Repository visibility and paid-service configuration remain unchanged.
