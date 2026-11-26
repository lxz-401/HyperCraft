#!/usr/bin/env bash
# Host test for the authenticated preference endpoint, including its snapshot
# and hot-reload collaborators. No device is required.
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/../.." && pwd)"
classes_dir="$(mktemp -d -t home-layout-endpoint.XXXXXX)"
trap 'rm -r "$classes_dir"' EXIT
sources=()
while IFS= read -r -d '' source; do
    sources+=("$source")
done < <(find "$project_dir/tests/home-layout-native/stubs" \
    "$project_dir/tests/home-dock-window/stubs" -name '*.java' -print0)
libhook="$project_dir/library/libhook/src/main/java/com/sevtinge/hypercraft/libhook"
javac -encoding UTF-8 -d "$classes_dir" "${sources[@]}" \
    "$libhook/base/FrameworkHookGate.java" \
    "$libhook/provider/HomeLayoutPrefsSnapshot.java" \
    "$libhook/provider/HomeGesturePrefsSnapshot.java" \
    "$libhook/rules/home/dock/HomeLayoutNativeEndpointOS4.java" \
    "$project_dir/tests/home-layout-native/HomeLayoutNativeEndpointOS4Test.java"
java -ea -cp "$classes_dir" com.sevtinge.hypercraft.libhook.rules.home.dock.HomeLayoutNativeEndpointOS4Test
