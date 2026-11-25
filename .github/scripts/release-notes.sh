#!/usr/bin/env bash
# Prints the CHANGELOG.md section for a version, for use as release notes.
# Fails if the version differs from mod.version or has no changelog section.
#
# Usage: release-notes.sh <version>      e.g. release-notes.sh 1.2.0
set -euo pipefail

version="${1:?usage: release-notes.sh <version>}"
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

declared="$(sed -n 's/^mod\.version *= *"\(.*\)".*/\1/p' "$root/stonecutter.properties.toml")"
if [[ "$declared" != "$version" ]]; then
    echo "error: requested version '$version' does not match mod.version '$declared'" >&2
    exit 1
fi

# Lines between the "## [<version>]" heading and the next "## [" heading,
# without the link definitions at the bottom of the file and without leading blank lines.
notes="$(awk -v v="$version" '
    /^## \[/        { printing = (index($0, "## [" v "]") == 1); next }
    /^\[[^]]+\]: /  { next }
    printing        { print }
' "$root/CHANGELOG.md" | sed '/./,$!d')"

if [[ -z "$notes" ]]; then
    echo "error: CHANGELOG.md has no entries under '## [$version]'" >&2
    exit 1
fi

printf '%s\n' "$notes"
