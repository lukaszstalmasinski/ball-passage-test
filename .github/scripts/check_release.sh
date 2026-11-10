#!/usr/bin/env bash
# Checks that a release tag matches the app version before anything is built or uploaded:
# the tag must be "v" + versionName, and every store language needs a changelog for versionCode
# (fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt, at most 500 characters).
#
# Usage: check_release.sh TAG        e.g. check_release.sh v1.0.3
set -euo pipefail

tag="${1:?usage: check_release.sh TAG}"
root="$(cd "$(dirname "$0")/../.." && pwd)"
gradle="$root/app/build.gradle.kts"

name="$(sed -nE 's/^[[:space:]]*versionName = "([^"]+)".*/\1/p' "$gradle")"
code="$(sed -nE 's/^[[:space:]]*versionCode = ([0-9]+).*/\1/p' "$gradle")"
if [ -z "$name" ] || [ -z "$code" ]; then
  echo "::error::versionName or versionCode not found in app/build.gradle.kts"
  exit 1
fi

errors=0
if [ "$tag" != "v$name" ]; then
  echo "::error::Tag $tag does not match versionName $name in app/build.gradle.kts; expected tag v$name."
  errors=1
fi

for dir in "$root"/fastlane/metadata/android/*/; do
  locale="$(basename "$dir")"
  file="$dir/changelogs/$code.txt"
  if [ ! -s "$file" ]; then
    echo "::error::Missing changelog for versionCode $code: fastlane/metadata/android/$locale/changelogs/$code.txt"
    errors=1
  elif [ "$(tr -d '\n' < "$file" | wc -m)" -gt 500 ]; then
    echo "::error::Changelog fastlane/metadata/android/$locale/changelogs/$code.txt is longer than Google Play's 500 characters."
    errors=1
  fi
done

if [ "$errors" -eq 0 ]; then
  echo "Release $tag: versionName $name, versionCode $code, changelogs present."
fi
exit "$errors"
