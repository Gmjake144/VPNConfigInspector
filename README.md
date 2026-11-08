# VPN Config Inspector (Android)

Offline Android starter app for inspecting VPN configuration files selected by the user.

## Format recognition
- HTTP Custom `.hc` — recognizes the extension; proprietary payloads are not decoded.
- HTTP Injector `.ehi` — recognizes the extension; proprietary payloads are not decoded.
- HA Tunnel Plus `.hat` — recognizes the extension; proprietary payloads are not decoded.
- OpenVPN `.ovpn`
- WireGuard `.conf`
- Shadowsocks, VMess, VLESS, Trojan URI/config text
- JSON and plain-text configuration files

## Features
- Android system file picker (`OpenDocument`)
- Local file inspection; no upload to a server
- Heuristic format and setting-marker detection
- Credential-marker count without displaying secret values
- Optional Base64 text decoding

## Limitations and safety
“Support all VPN files” cannot be guaranteed. Some apps use proprietary formats, encryption, compression, or version-specific schemas. This app identifies `.hc`, `.ehi`, and `.hat` by extension only; it does not decrypt or reverse-engineer protected payloads. Base64 is encoding, not encryption. Actual encrypted data requires the correct key and authorization.

The app does not crack credentials, extract private keys, bypass provider restrictions, intercept/decrypt live VPN traffic, or connect to VPN servers. Treat VPN profiles as secrets because they may contain passwords, tokens, or private keys.

## Build APK
1. Open this project in Android Studio.
2. Install Android SDK 35 and allow Gradle sync.
3. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Android Studio displays the APK output location.
