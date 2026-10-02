#!/bin/sh
# Package Operation Phoenix as an LG webOS TV app (.ipk). Needs python3 and the webOS CLI:
#   npm install -g @webos-tools/cli
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
APP="$(dirname "$HERE")"
OUT="$HERE/build/app"
rm -rf "$HERE/build"; mkdir -p "$OUT" "$APP/dist"
python3 "$APP/tools/make-www.py" "$OUT"
cp "$HERE/appinfo.json" "$HERE/icon.png" "$HERE/largeIcon.png" "$HERE/splash.png" "$OUT/"
ARES="${ARES_PACKAGE:-$(command -v ares-package)}"
"$ARES" --no-minify "$OUT" -o "$APP/dist"
echo "Built $(ls "$APP"/dist/*.ipk)"
