#!/usr/bin/env bash
set -euo pipefail
amin_repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$amin_repo_root"
source .codex/android-env.sh
# Development checks cannot consume the production signing key.
unset AMIN_LIVE_TV_KEYSTORE AMIN_LIVE_TV_STORE_PASSWORD AMIN_LIVE_TV_KEY_PASSWORD AMIN_LIVE_TV_KEY_ALIAS
node scripts/validate-sports-catalogue.cjs
bash ./gradlew --no-daemon assembleDebug testDebugUnitTest lintDebug lintRelease
node scripts/verify-preview.cjs
