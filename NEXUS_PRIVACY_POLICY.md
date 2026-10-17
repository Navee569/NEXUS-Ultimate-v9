# NEXUS Music — Privacy Notes

This is a personal fork of Echo Music. The changes described here apply to this source tree; a compiled APK must be checked separately.

## Defaults in this fork

- Android automatic backup is disabled.
- Firebase Analytics and Crashlytics dependencies/configuration have been removed from this fork.
- The legacy Discord WebView token-extraction implementation has been removed; Discord authorization uses OAuth authorization-code flow with PKCE in the system browser.
- Cleartext HTTP is blocked by Android network security configuration.
- The app no longer requests the Android permission that allows requesting APK installations.
- The Last.fm shared secret is not embedded in BuildConfig. Last.fm API features requiring signatures may be unavailable.
- The Private Session switch pauses new local listening and search history together. It does not erase prior history; use the separate clear-history actions for that.

## Information that can still leave the device

This app still needs network access to retrieve online music, artwork, metadata, and optional integrations. Those remote services can observe requests and associated network metadata. If you explicitly use Discord OAuth, Discord receives the authentication request. If you use an optional AI or weather integration, data necessary for that feature may be sent to its configured provider. The source-level changes here do not guarantee that every third-party library or remote service is privacy-preserving.

## Limits

NEXUS is not invisible to Android, device administrators, network operators, or people who can inspect the device. The app does not attempt to bypass monitoring or conceal itself from security tools. Use Android's screen lock and, where available, Secure Folder/private app features for device-level privacy.

## Before distributing

- Audit dependencies and their licenses.
- Inspect network traffic on a test device.
- Verify OAuth redirect registration and the Discord app configuration.
- Test that the Private Session switch stops history writes.
- Build from this source and compare the resulting APK to the source revision.
- Do not add real API secrets to client-side source or BuildConfig.
