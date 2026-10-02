# Operation Phoenix apps

Ready-to-install builds are in `dist/`:

| File | For |
|---|---|
| `OperationPhoenix.apk` | Android phones, tablets and Android TV / Google TV |
| `com.operationphoenix.game_1.1.0_all.ipk` | LG TVs (webOS) |

Both are the full game, bundled offline. The only permission the Android app asks for is internet access, and it uses it only when you play online with a friend. The game picks its controls automatically: touch on phones, remote or controller on a TV, mouse and keyboard on a computer. The buttons for whatever you're using are shown in the corner.

## Install on Android or Android TV
The APK targets Android 16 (API 36) and installs on Android 7 through Android 17, including Google TV. Version 1.1 installs over 1.0 and keeps your saves, as long as both were signed with the same key.

- **Phone or tablet:** copy the APK over, open it, and allow "Install unknown apps" for your file manager when asked.
- **Android TV / Google TV:** turn on Developer options (Settings → System → About → click Build 7 times), allow unknown sources, then either send the APK with an app like *Send Files to TV* or use `adb connect <tv-ip>` and `adb install OperationPhoenix.apk`.

## Install on an LG TV (webOS)
LG only allows side-loaded apps through its free **Developer Mode** app:
1. Make a free account at webostv.developer.lge.com, install **Developer Mode** from the LG Content Store on the TV, sign in and turn Dev Mode on.
2. On a computer: `npm install -g @webos-tools/cli`, then `ares-setup-device` to add the TV (its IP and the passphrase shown in the Developer Mode app).
3. `ares-install --device <name> dist/com.operationphoenix.game_1.1.0_all.ipk`

Developer Mode sessions expire after a few days unless you press "Extend" in the Developer Mode app.

## Controllers
Pair Xbox (or any standard) controllers over Bluetooth. In Co-op the first controller plays **Alpha** and the second plays **Bravo**. You can also play one controller and one keyboard. In Versus, controller 1 defends and controller 2 attacks.

**Settings → Controllers** shows what's connected, lights up every button as you press it, tests rumble, and lets you rebind any button (handy for controllers that report a non-standard layout). Rebinding swaps buttons, so nothing is ever left unbound.

## Playing online with a friend
Open **Battle modes → Co-op → 🌐 Play online**, or the same button on the Versus setup screen. One of you hosts and the other joins:
- **Room code** (on the claude.ai link): the host gets a 5-letter code and the friend types it in. Your friend needs a claude.ai account, and you share the game with them as a *Contributor* or above.
- **Invite code** (anywhere: the HTML file, the APK, the LG app): the host copies an invite code to the friend (over Discord, text, anything). The friend pastes it and sends back a reply code, and the host pastes that. It connects directly between your two computers with no server in the middle. Strict school or work networks can block this; home internet almost always works.

The host runs the battle and picks the mission. Both players use their whole keyboard or any controller.

## Rebuilding
After changing `../operation-phoenix.html`:
- **Android:** `android/build.sh` (needs a JDK, python3, curl, and on Ubuntu/Debian `apt install aapt dalvik-exchange zipalign apksigner android-sdk-platform-23`). Resources compile against API 23, and the Java code against an API 36 framework jar: your SDK's `platforms/android-36/android.jar` if it's installed, otherwise the build downloads Robolectric's `android-all` jar for Android 16 from Maven Central once (about 190 MB, checked against a pinned SHA-256) into `android/.cache/`.
- **LG webOS:** `webos/build.sh` (needs `npm install -g @webos-tools/cli`).

`tools/make-www.py` turns the web build into the offline page both apps use. It inlines Tailwind (MIT licence in `vendor/`), drops web fonts, and sets a content policy that blocks every web request (fetches, images, sockets). Online play uses only a direct peer-to-peer connection, and only when you start one.

**Signing key:** `android/build.sh` signs with `android/release.keystore` and creates one if it's missing. The key is deliberately not in git because this repo is public. Android only installs an update over an existing install when both are signed with the same key. A new key means uninstalling first, and that wipes your saves, so keep your keystore somewhere safe.
