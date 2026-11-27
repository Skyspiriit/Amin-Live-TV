#!/usr/bin/env bash
set -euo pipefail
[[ "$(uname -s)" == Linux ]] || { echo 'Cloud setup targets Linux; use the existing SDK on Windows.' >&2; exit 1; }
amin_repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$amin_repo_root"
source .codex/android-env.sh
amin_packages=()
for amin_command in curl unzip node; do
  command -v "$amin_command" >/dev/null || case "$amin_command" in
    node) amin_packages+=(nodejs);;
    *) amin_packages+=("$amin_command");;
  esac
done
if [[ -z "${JAVA_HOME:-}" ]] || [[ ! -x "$JAVA_HOME/bin/javac" ]] || [[ "$("$JAVA_HOME/bin/javac" -version 2>&1)" != javac\ 17.* ]]; then
  amin_packages+=(openjdk-17-jdk-headless)
fi
if (( ${#amin_packages[@]} )); then
  amin_sudo=()
  if (( EUID != 0 )); then command -v sudo >/dev/null || { echo 'Install JDK 17, curl, unzip and Node 18+ first.' >&2; exit 1; }; amin_sudo=(sudo); fi
  "${amin_sudo[@]}" apt-get update -qq
  "${amin_sudo[@]}" apt-get install -y --no-install-recommends "${amin_packages[@]}" ca-certificates
  source .codex/android-env.sh
fi
[[ "$(javac -version 2>&1)" == javac\ 17.* ]] || { echo 'JDK 17 is required.' >&2; exit 1; }
node -e 'if (+process.versions.node.split(".")[0] < 18) throw Error("Node 18+ is required")'
# Google repository2-3.xml pins this Java-17-compatible CLI-tools 19.0 archive/checksum.
amin_sdkmanager="$ANDROID_HOME/cmdline-tools/19.0/bin/sdkmanager"
if [[ ! -x "$amin_sdkmanager" ]]; then
  amin_tmp="$(mktemp -d -t amin-tv-sdk-XXXXXXXX)"
  trap 'rm -rf -- "$amin_tmp"' EXIT
  curl --fail --location --retry 3 --output "$amin_tmp/tools.zip" \
    https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip
  printf '%s  %s\n' 5fdcc763663eefb86a5b8879697aa6088b041e70 "$amin_tmp/tools.zip" | sha1sum --check --status
  unzip -q "$amin_tmp/tools.zip" -d "$amin_tmp/unpacked"
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  mv "$amin_tmp/unpacked/cmdline-tools" "$ANDROID_HOME/cmdline-tools/19.0"
fi
# Preserve sdkmanager errors while allowing yes to end with SIGPIPE.
set +o pipefail
yes | "$amin_sdkmanager" --sdk_root="$ANDROID_HOME" --licenses >/dev/null
set -o pipefail
"$amin_sdkmanager" --sdk_root="$ANDROID_HOME" 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0'
bash scripts/check.sh
echo 'Cloud setup passed. Use bash scripts/check.sh in future sessions.'
