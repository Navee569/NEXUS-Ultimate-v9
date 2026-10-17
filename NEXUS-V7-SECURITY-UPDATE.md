# NEXUS Ultimate v7 candidate — Security update

Date: 2026-10-16
Base: NEXUS Ultimate reviewed source v6

## Changes in this continuation

1. Added `DiscordCredentialStore.kt`, which encrypts Discord access and refresh tokens using an AES-256-GCM key held in Android Keystore. The ciphertext is stored in Preferences DataStore; the key is not stored alongside it.
2. Added one-time migration from the old plaintext `DiscordTokenKey` and `DiscordRefreshTokenKey` preferences. On successful encryption, the plaintext keys are removed. If migration or decryption fails, the implementation fails closed, clears token material, and requires the user to sign in again rather than falling back to plaintext.
3. Updated `DiscordOAuthRepository` to read, write, refresh, revoke, and clear tokens through the encrypted credential store. Expiry and profile metadata remain in DataStore; these are not authentication secrets.
4. Updated Discord settings to load the token through the repository instead of observing a plaintext token preference directly.
5. Updated `MusicService.ensurePresenceManager()` to stop and not restart the disabled custom Discord Rich Presence path. It no longer reads the token from DataStore or passes it into the legacy presence implementation.
6. Disabled the Rich Presence toggle in the UI and added an explanation that the feature remains unavailable until a supported integration is implemented.

## Static checks performed

- Parsed 582 XML files: zero parsing errors.
- Confirmed direct references to the old plaintext token preference keys are confined to the credential-store migration/cleanup class.
- Confirmed the old Discord WebView `localStorage` token-scraping pattern was not found in Kotlin source.
- Confirmed the credential-store source uses Android Keystore AES/GCM and the service path is disabled.
- Created a machine-readable result file: `NEXUS-SECURITY-CHECKS-v7.json`.

## Security notes and limitations

- This is a source-level candidate, **not a verified release**.
- No Gradle compilation, unit tests, lint, dependency resolution, SBOM/CVE scan, APK build/analysis, emulator/device test, or live network capture was run. The prior session report documents that this environment lacks Android SDK/adb/emulator and cannot resolve the Gradle/Google Maven/Maven Central hosts.
- Android Keystore encryption protects token values at rest in the app's preferences, but does not protect against a compromised/rooted device or malicious code running inside the app process while the token is in memory.
- Migration intentionally fails closed: if the key cannot be created or old values cannot be encrypted, local Discord credentials are discarded and the user must reauthorize. This behaviour needs device testing.
- The OAuth revocation endpoint's compatibility and logout behaviour remain unverified.
- Discord Rich Presence remains disabled. Do not restore the old Gateway IDENTIFY flow; use a documented supported API/SDK if one is available and verify it before re-enabling.
- Other upstream WebViews/JavaScript bridges, the Google API-key-shaped constant, manifest permissions, FFmpegKit fork provenance, and dependency integrity remain open audit items.

## Required validation on a connected Android build machine

Use JDK 21 and an Android SDK with repository access:

```bash
./gradlew --version
./gradlew :app:assembleUniversalFossDebug
./gradlew :app:testUniversalFossDebugUnitTest
./gradlew :app:lintUniversalFossDebug
./gradlew :app:dependencies --configuration universalFossDebugRuntimeClasspath
```

Then:

1. Verify clean install, existing v6 upgrade, token migration, re-login, refresh, logout, app restart, process death, and device reboot.
2. Verify no plaintext access/refresh token remains in Preferences DataStore after migration.
3. Test Keystore unavailable/key invalidation behaviour; confirm the app fails closed and offers reauthorization without crashing.
4. Test OAuth callback state, PKCE verifier lifecycle, expiry, refresh failure, and revocation error handling.
5. Generate a CycloneDX SBOM and scan resolved transitive/native dependencies with a current SCA tool.
6. Inspect merged manifests for all variants, WebView bridge origins/navigation, and runtime network destinations/data.
7. Verify Smart Discovery search, de-duplication, play queue, add-to-queue, and provider/network failure UI.

## Feature status

The existing NEXUS features (MoodSync timer, Sonic Memory, Music Constellation, Smart Discovery queue wiring, saved audio-profile labels, and privacy shortcuts) are retained from v6. Their runtime behaviour is still not verified. The audio profile still selects a label rather than applying an actual equalizer curve; advanced recommendation logic and robust offline improvements remain unfinished.
