# NEXUS Ultimate — Independent Source Review and Patch Report

**Review date:** 2026-10-16  
**Input:** `NEXUS-Ultimate-source-updated.zip`, `NEXUS_SESSION_REPORT.md`, and `nexus-session.patch` supplied in this conversation.

## Verification boundary

This is a source review only. No Gradle compilation, Android lint, unit tests, APK build, dependency resolution, CVE scan, APK inspection, emulator run, or device/network test was completed. The supplied session report documents that the environment has JDK 21 but lacks Android SDK/adb/emulator and cannot resolve required Gradle/Maven hosts. See `NEXUS_SESSION_REPORT.md` for the reported environment findings.

## Changes made in this review

1. **Disabled the custom Discord Gateway Rich Presence implementation.** The reviewed source sent a Discord OAuth access token into a custom Gateway IDENTIFY flow while claiming official Discord Android client properties. This is not a supported/authenticated design that can be safely endorsed from source review alone. `DiscordSocialPresenceClient` now fails closed, and `DiscordRPC.updateSong` stops before asset resolution or any presence request. The unused custom Gateway implementation and its supporting model/default/super-properties files were removed. Discord Rich Presence will not work until replaced with a documented supported integration and tested.
2. **Removed stale-token fallback in `DiscordPresenceManager`.** It no longer falls back to the raw token passed by UI/service code when `getValidAccessToken()` returns null. It now fails closed and asks for reauthorization rather than reusing an expired/invalid token.
3. **Fixed token Authorization header formatting in `DiscordAssetRegistrar`.** It always uses `Authorization: Bearer <token>` rather than sometimes sending the raw token. Error logging no longer includes the remote response body.
4. **Treats missing expiry metadata as expired.** `getValidAccessToken()` now only returns the current token when its recorded expiry is sufficiently in the future; expiry `0` no longer means “valid forever.” It attempts refresh when expiry is missing/near/elapsed and returns null if refresh cannot be completed.
5. **Makes local logout happen before best-effort remote revocation.** `clearSession()` snapshots tokens if possible, clears local credentials/profile values first, then attempts revocation. A remote timeout/failure no longer keeps credentials in local storage while logout waits on the server.

## Existing changes reviewed in the supplied source

- Discord WebView localStorage token scraping was not found in the inspected source.
- OAuth authorization-code + PKCE flow is present.
- Callback activity checks redirect scheme and host; repository validates the callback target and state.
- `allowBackup=false` and cleartext HTTP disabled are present in the project configuration.
- Smart Discovery source wiring to search results and the playback queue is present, but has not been executed.
- Automatic audio profile behavior still selects a profile label; it does not apply a real EQ curve.

## Known risks still open

- **Discord token storage remains unencrypted** in Preferences DataStore. This review did not attempt a broad storage migration because the UI, repository, and playback service all read the preference directly; this needs a carefully scoped change and tests.
- Other WebViews still use JavaScript bridges and local-file access for upstream player scripts. Origin/navigation policy and bridge exposure need dedicated review.
- A Google API-key-shaped value remains in source; ownership, restrictions, and quota were not verified. Do not treat an APK-embedded key as secret.
- Permissions in the merged manifests have not been verified against actual feature use.
- The community FFmpegKit fork's provenance/checksum remains unverified; there are no dependency lockfiles or Gradle verification metadata.
- Discord Rich Presence is intentionally disabled by this patch; use a documented supported integration if one is available, otherwise leave it disabled.
- Other NEXUS features and privacy controls remain untested. This patch does not add the unimplemented recommendation engine or true automatic EQ.

## Commands to run on a connected build machine

Use JDK 21 and a compatible Android SDK with repository access:

```bash
./gradlew --version
./gradlew :app:tasks --all
./gradlew :app:assembleUniversalFossDebug
./gradlew :app:testUniversalFossDebugUnitTest
./gradlew :app:lintUniversalFossDebug
./gradlew :app:dependencies --configuration universalFossDebugRuntimeClasspath
```

Then generate a CycloneDX SBOM, scan the resolved graph with OSV-Scanner or another SCA tool, review merged manifests for each flavor, and inspect the resulting APK. On a test device, verify OAuth login/refresh/logout, Smart Discovery search/queue/play/add-to-queue, privacy settings, and confirm no Discord Gateway connection or token transmission occurs. Capture network traffic in a controlled test environment and check all destinations/data categories.

## Result

This patch is **not compiled or tested**. The output is a source-review patch and should be treated as a candidate for review, not a release build. GPL-3.0 project licensing is retained.
