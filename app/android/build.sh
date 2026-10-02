#!/bin/sh
# Build the Operation Phoenix APK with the plain Android command-line tools (no Gradle needed).
# Needs: python3, a JDK, and aapt, dalvik-exchange (dx), zipalign, apksigner plus an android.jar.
# On Ubuntu/Debian: sudo apt install aapt dalvik-exchange zipalign apksigner android-sdk-platform-23
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
APP="$(dirname "$HERE")"
SDK="${ANDROID_SDK:-/usr/lib/android-sdk}"
JAR="${ANDROID_JAR:-$SDK/platforms/android-23/android.jar}"
DX="$(command -v dalvik-exchange || command -v dx)"
OUT="$HERE/build"
KEY="${KEYSTORE:-$HERE/release.keystore}"

rm -rf "$OUT"; mkdir -p "$OUT/gen" "$OUT/obj" "$OUT/bin" "$OUT/assets"
python3 "$APP/tools/make-www.py" "$OUT/assets/www"

# resources -> R.java, then compile and convert to dex
aapt package -f -m -J "$OUT/gen" -M "$HERE/AndroidManifest.xml" -S "$HERE/res" -I "$JAR"
javac -nowarn -Xlint:-options -source 8 -target 8 -encoding UTF-8 -bootclasspath "$JAR" -classpath "$JAR" -d "$OUT/obj" \
  "$OUT"/gen/com/operationphoenix/game/R.java "$HERE"/src/com/operationphoenix/game/*.java
"$DX" --dex --output="$OUT/bin/classes.dex" "$OUT/obj"

# package, align, sign
aapt package -f -M "$HERE/AndroidManifest.xml" -S "$HERE/res" -A "$OUT/assets" -I "$JAR" -F "$OUT/unsigned.apk" "$OUT/bin"
zipalign -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
if [ ! -f "$KEY" ]; then
  keytool -genkeypair -keystore "$KEY" -storepass phoenix -keypass phoenix -alias phoenix -keyalg RSA -keysize 2048 \
    -validity 10000 -dname "CN=Operation Phoenix" >/dev/null 2>&1
fi
mkdir -p "$APP/dist"
apksigner sign --v4-signing-enabled false --ks "$KEY" --ks-pass pass:phoenix --key-pass pass:phoenix --out "$APP/dist/OperationPhoenix.apk" "$OUT/aligned.apk"
apksigner verify "$APP/dist/OperationPhoenix.apk"
echo "Built $APP/dist/OperationPhoenix.apk"
