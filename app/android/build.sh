#!/bin/sh
# Build the Operation Phoenix APK with the plain Android command-line tools (no Gradle needed).
# Needs: python3, curl, a JDK, and aapt, dalvik-exchange (dx), zipalign, apksigner plus an android.jar.
# On Ubuntu/Debian: sudo apt install aapt dalvik-exchange zipalign apksigner android-sdk-platform-23
#
# The app targets API 36 (Android 16, and installs on 17). Resources are compiled by aapt against the API 23
# platform (the manifest only needs the target level as a number). The Java code is compiled against an API 36
# framework jar: $COMPILE_JAR, the SDK's android-36 platform if it is installed, or the matching Robolectric
# "android-all" jar from Maven Central, checked against a pinned SHA-256.
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
APP="$(dirname "$HERE")"
SDK="${ANDROID_SDK:-/usr/lib/android-sdk}"
JAR="${ANDROID_JAR:-$SDK/platforms/android-23/android.jar}"
DX="$(command -v dalvik-exchange || command -v dx)"
OUT="$HERE/build"
CACHE="$HERE/.cache"
KEY="${KEYSTORE:-$HERE/release.keystore}"

AA_VER=16-robolectric-13921718
AA_SHA=8b74a0a137330658d2f33f0dc715d42734f74ba8b2d7014fc2e95aa40d3f682d
if [ -n "$COMPILE_JAR" ]; then API36="$COMPILE_JAR"
elif [ -f "$SDK/platforms/android-36/android.jar" ]; then API36="$SDK/platforms/android-36/android.jar"
else
  API36="$CACHE/android-all-$AA_VER.jar"
  if [ ! -f "$API36" ]; then
    mkdir -p "$CACHE"
    curl -fL -o "$API36.part" "https://repo1.maven.org/maven2/org/robolectric/android-all/$AA_VER/android-all-$AA_VER.jar"
    mv "$API36.part" "$API36"
  fi
  echo "$AA_SHA  $API36" | sha256sum -c - >/dev/null || { echo "android-all jar checksum mismatch"; rm -f "$API36"; exit 1; }
fi

rm -rf "$OUT"; mkdir -p "$OUT/gen" "$OUT/obj" "$OUT/bin" "$OUT/assets"
python3 "$APP/tools/make-www.py" "$OUT/assets/www"

# resources -> R.java, then compile and convert to dex
aapt package -f -m -J "$OUT/gen" -M "$HERE/AndroidManifest.xml" -S "$HERE/res" -I "$JAR"
javac -nowarn -Xlint:-options --release 8 -encoding UTF-8 -classpath "$API36" -d "$OUT/obj" \
  "$OUT"/gen/com/operationphoenix/game/R.java "$HERE"/src/com/operationphoenix/game/*.java
"$DX" --dex --min-sdk-version=24 --output="$OUT/bin/classes.dex" "$OUT/obj"

# package (resources.arsc stored uncompressed, as API 30+ requires), align, sign
aapt package -f -0 arsc -M "$HERE/AndroidManifest.xml" -S "$HERE/res" -A "$OUT/assets" -I "$JAR" -F "$OUT/unsigned.apk" "$OUT/bin"
zipalign -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
if [ ! -f "$KEY" ]; then
  keytool -genkeypair -keystore "$KEY" -storepass phoenix -keypass phoenix -alias phoenix -keyalg RSA -keysize 2048 \
    -validity 10000 -dname "CN=Operation Phoenix" >/dev/null 2>&1
fi
mkdir -p "$APP/dist"
apksigner sign --v4-signing-enabled false --min-sdk-version 24 --ks "$KEY" --ks-pass pass:phoenix --key-pass pass:phoenix \
  --out "$APP/dist/OperationPhoenix.apk" "$OUT/aligned.apk"
apksigner verify --min-sdk-version 24 "$APP/dist/OperationPhoenix.apk"
zipalign -c -p 4 "$APP/dist/OperationPhoenix.apk"
echo "Built $APP/dist/OperationPhoenix.apk"
