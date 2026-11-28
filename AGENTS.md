# Amin TV development

Start with `docs/CLOUD_HANDOVER.md`, then read `docs/DEVELOPMENT_STATUS.md`, `docs/ARCHITECTURE.md` and `docs/ROADMAP.md` before changing code. Update status at every tested checkpoint.

- Continue this application; do not rewrite it or modify the adjacent Amin Player/VPN projects.
- Current handover scope is Amin TV only and documentation only. All other apps are skipped. Morphent and segmentation projects must not be inspected, edited or pushed.
- Preserve production package `com.enigma.aminlivetv`, release certificate, catalogue IDs and preference formats. Never clear production data to fix an update.
- Work in a dedicated branch and prepare a PR. Do not merge, tag or publish a production release without explicit owner approval.
- Previews use `com.enigma.aminlivetv.qa`. Do not install on the physical Xiaomi without specific permission. Emulator results are not Xiaomi results.
- Keep the official YouTube player and its restrictions. Iranian captions default off. Preserve documentary subtitles/queues and networking.
- Current checkpoint is research only: do not change `channels.json`. New titles need authorization, correctly numbered full episodes and actual playback evidence.
- No credentials/keys in source, logs, artifacts or Codex development environments. Production signing belongs in protected GitHub Actions secrets after owner approval.

## Commands

Fresh Linux cloud setup: `bash .codex/setup.sh`.

Normal verification: `bash scripts/check.sh` builds the preview, checks catalogue preservation, runs JVM tests and both lint variants, and verifies preview metadata. No release key is needed.

Emulator UI tests: `./gradlew --no-daemon connectedDebugAndroidTest`, with an explicitly selected emulator. See `docs/CLOUD_DEVELOPMENT.md`. Never select the first attached device blindly.

Stop at a tested review checkpoint before implementing catalogue additions or another major phase.
