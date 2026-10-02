# Operation Phoenix apps

Ready-to-install builds are in `dist/`:

| File | For |
|---|---|
| `OperationPhoenix.apk` | Android phones, tablets and Android TV / Google TV |
| `com.operationphoenix.game_1.0.0_all.ipk` | LG TVs (webOS) |

Both are the full game, fully offline. The Android app asks for **no permissions at all**, not even internet access. The game picks its controls automatically: touch on phones, remote or controller on a TV, mouse and keyboard on a computer. The buttons for whatever you're using are shown in the corner.

## Install on Android or Android TV
- **Phone or tablet:** copy the APK over, open it, and allow "Install unknown apps" for your file manager when asked.
- **Android TV / Google TV:** turn on Developer options (Settings → System → About → click Build 7 times), allow unknown sources, then either send the APK with an app like *Send Files to TV* or use `adb connect <tv-ip>` and `adb install OperationPhoenix.apk`.

## Install on an LG TV (webOS)
LG only allows side-loaded apps through its free **Developer Mode** app:
1. Make a free account at webostv.developer.lge.com, install **Developer Mode** from the LG Content Store on the TV, sign in and turn Dev Mode on.
2. On a computer: `npm install -g @webos-tools/cli`, then `ares-setup-device` to add the TV (its IP and the passphrase shown in the Developer Mode app).
3. `ares-install --device <name> dist/com.operationphoenix.game_1.0.0_all.ipk`

Developer Mode sessions expire after a few days unless you press "Extend" in the Developer Mode app.

## Controllers
Pair Xbox (or any standard) controllers over Bluetooth. In Co-op the first controller plays **Alpha** and the second plays **Bravo**. You can also play one controller and one keyboard. In Versus, controller 1 defends and controller 2 attacks.

## Rebuilding
After changing `../operation-phoenix.html`:
- **Android:** `android/build.sh` (needs a JDK, python3, and on Ubuntu/Debian `apt install aapt dalvik-exchange zipalign apksigner android-sdk-platform-23`).
- **LG webOS:** `webos/build.sh` (needs `npm install -g @webos-tools/cli`).

`tools/make-www.py` turns the web build into the offline page both apps use. It inlines Tailwind (MIT licence in `vendor/`), drops web fonts, and sets a content policy that blocks every network request.

**Signing key:** `android/build.sh` signs with `android/release.keystore` and creates one if it's missing. The key is deliberately not in git because this repo is public. Android only installs an update over an existing install when both are signed with the same key. A new key means uninstalling first, and that wipes your saves, so keep your keystore somewhere safe.
