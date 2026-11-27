#!/usr/bin/env bash
# Source in each fresh shell; no machine paths or secrets are persisted.
export ANDROID_HOME="${AMIN_ANDROID_HOME:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/.local/share/amin-tv/android-sdk}}}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
if [[ -z "${JAVA_HOME:-}" ]] || [[ ! -x "$JAVA_HOME/bin/javac" ]] || [[ "$("$JAVA_HOME/bin/javac" -version 2>&1)" != javac\ 17.* ]]; then
  for amin_java_candidate in /usr/lib/jvm/java-17-openjdk-* /usr/lib/jvm/temurin-17-* /opt/java/openjdk; do
    if [[ -x "$amin_java_candidate/bin/javac" ]] && [[ "$("$amin_java_candidate/bin/javac" -version 2>&1)" == javac\ 17.* ]]; then
      export JAVA_HOME="$amin_java_candidate"
      break
    fi
  done
fi
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
export PATH="$ANDROID_HOME/cmdline-tools/19.0/bin:$ANDROID_HOME/platform-tools:$PATH"
