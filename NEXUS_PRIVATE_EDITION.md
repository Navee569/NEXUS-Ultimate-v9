# NEXUS Music — Private Edition

This is a personal fork of Echo Music, based on the supplied source archive. It preserves the upstream GPL-3.0 license and notices.

## Privacy and security changes in this fork

- Replaced the Discord WebView token-scraping flow with the existing OAuth 2.0 authorization-code + PKCE flow in the system browser.
- Removed the old `DiscordTokenWebView.kt` implementation that read Discord's `localStorage` token.
- Removed the hardcoded Last.fm shared secret from distributable BuildConfig. A secret embedded in an APK is not secret; Last.fm features requiring request signing may be unavailable until implemented through a trusted server-side design.
- Disabled Android app backup (`allowBackup=false`) to reduce unintended cloud/device-transfer copies of private app data.
- Removed `REQUEST_INSTALL_PACKAGES` from the main manifest because this music player should not request permission to install APKs.
- Disabled cleartext HTTP traffic by default. Features that depend on unencrypted HTTP endpoints will fail until migrated to HTTPS/WSS.
- Removed the upstream `google-services.json` from this fork so the personal build does not inherit the upstream Firebase project configuration. This also disables Google Services integration unless you deliberately configure your own project.
- Changed the installed app label to `NEXUS Music` and application ID to `com.nexus.music.private` to separate this build from the upstream app identity.

## Privacy limits (important)

NEXUS is designed to minimize unnecessary exposure, not to make its use literally undetectable. Android will still show the installed app to someone with access to the device/app list; network providers and music/authentication services can observe connections they serve; Android system logs and device-management software may also reveal activity. This fork does not attempt to hide itself from device owners, administrators, or security tools.

For a more private setup:

1. Disable listening and search history in the app's Privacy settings when desired.
2. Deny optional microphone/location permissions unless you use those features.
3. Avoid Discord Rich Presence unless needed, and review the OAuth consent screen before authorizing.
4. Use a device screen lock and Android's app-lock/private-space features if available on your device.
5. Review the source and build locally. Do not trust a prebuilt APK merely because the source is available.

## Planned/retained feature direction

The base project already contains a broad music-player feature set. The personal edition is intended to build on it with local-first mood sessions, musical-history insights, private notes attached to songs, device-specific audio profiles, and a network-activity/privacy dashboard. These features are not all implemented by this initial hardening patch; do not assume a feature is present until it appears in the app and has been tested.

## Build

Open this project in Android Studio and build the `foss` variant. A compatible Android SDK/NDK and the dependencies required by the project must be available. The source archive was not yet verified to produce a successful APK in this environment.
