#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
: "${HITOHI_KEYSTORE:?Set the private keystore path}"
: "${HITOHI_PASSWORD_FILE:?Set the private password-file path}"
: "${ANDROID_HOME:?Set Android SDK path}"
: "${GRADLE_BIN:=./gradlew}"
sh scripts/test-timeline.sh
"$GRADLE_BIN" :app:assembleRelease :app:lintRelease --no-daemon
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
