# NEXUS Ultimate — Implementation and Security Status

Audit date: 2026-10-16

## Newly implemented: mood-driven playable queue

The NEXUS Smart Discovery panel now has a **Build a playable mix** action. It:

1. Builds a search query from the selected mood/energy, vocal preference, language and session length.
2. Searches the existing YouTube Music catalogue integration for song results.
3. Runs a secondary query when the first result set is small.
4. Deduplicates tracks by ID and caps the generated queue at 30 tracks.
5. Lets the user explicitly choose **Play mix** or **Add to queue**. It does not start playback merely because a mood was selected.
6. Shows loading, result-count and error states, plus a short track preview.

This requires a network connection and uses the existing third-party music service. Search terms are sent to that service when the user builds the mix. The duration preference shapes the query but does not guarantee an exact-duration queue because track durations and search results vary.

## Existing NEXUS additions retained

- MoodSync session timer and local session counts.
- Sonic Memory notes, tags, search and export.
- Music Constellation visualization of saved memories.
- Saved audio-profile preferences and output-category detection.
- Network exposure information panel.
- Local privacy controls and separate NEXUS application ID (`com.nexus.music.private`).

Some features remain partial: audio profile selection does not automatically apply a custom equalizer curve; the network panel is not a packet capture tool; and the constellation links memories visually rather than claiming AI-detected musical similarity.

## Source-level security checks performed

- Removed the prior Discord WebView `localStorage` token-extraction implementation from the inspected project; Discord authentication uses the authorization-code + PKCE path in `DiscordOAuthRepository`.
- Removed the hardcoded Last.fm shared secret from `BuildConfig`; signed Last.fm actions requiring that secret may not work.
- Removed the unused GitHub OAuth client ID/secret BuildConfig fields so a confidential GitHub client secret cannot accidentally be embedded through those fields.
- Firebase Analytics/Crashlytics runtime dependencies are not declared in the app module. The root build still includes the Crashlytics Gradle plugin classpath; that alone does not establish that runtime analytics is active.
- Automatic Android backup is disabled in the app manifest.
- Cleartext HTTP is disabled by default in the app manifest/network security configuration.
- `REQUEST_INSTALL_PACKAGES` is not declared in the current main app manifest.
- Static source search did not find the usual direct indicators checked for `DexClassLoader`, `PathClassLoader`, shell execution, SMS/call-log permissions, contact harvesting or the old Discord WebView token extraction.

## Remaining security concerns and limitations

- Discord access/refresh tokens are stored in app-private DataStore preferences. Android's app sandbox protects app-private files from ordinary apps, but this is not equivalent to hardware-backed or encrypted token storage. Review whether Discord persistence is needed; consider Android Keystore-backed encryption before enabling it for sensitive accounts.
- This app depends on external music, lyrics, artwork, and optional integration services. The source-level review does not prove that no data is transmitted. Search queries are sent to the selected service when searching; Listen Together uses a configured external WebSocket service when that feature is used.
- A public Last.fm API key is still in the source. Public client API keys are not secrets, but should be restricted where the provider supports restrictions.
- The repository uses external Maven repositories including Maven Central, Google Maven, JitPack and an Aliyun mirror. Supply-chain review should include provenance and checksums for artifacts obtained from all repositories.
- This was not a full dependency vulnerability audit: the dependency graph could not be resolved, and no live CVE/advisory database scan could be run in this environment. Version catalog entries are inventoried in `gradle/libs.versions.toml`, but direct and transitive artifacts still need scanning after a successful Gradle resolution.
- Static review cannot rule out malicious behaviour hidden in third-party libraries, native code, build plugins, remote services, or a tampered APK.

## Build and verification

Attempted command:

```text
./gradlew --offline :app:compileFossUniversalDebugKotlin
```

Build could not start because the Gradle wrapper tried to download Gradle 9.3.1 from `services.gradle.org` and DNS resolution failed (`UnknownHostException`). This is an environment/network limitation, not a successful compile. No APK was produced or device-tested. XML syntax checks do not validate Kotlin compilation.

## Recommended verification on a connected development machine

1. Run `./gradlew :app:compileFossUniversalDebugKotlin` and fix any compiler issues.
2. Run `./gradlew :app:dependencies` and `./gradlew :app:dependencyInsight --dependency <artifact>` as needed.
3. Run OWASP Dependency-Check or another current SCA scanner against the resolved dependency graph.
4. Build an APK, inspect its manifest and signing certificate, and compare it with this source tree.
5. Test Smart Discovery on device, including empty results, network failure, queue playback and append-to-queue.
6. Capture test-device network traffic to verify actual destinations and payloads. Do not treat the in-app Network panel as a packet monitor.

## Bottom line

The Smart Discovery flow now fetches real song results and offers actual playback-queue actions in source code. However, compilation and device execution remain unverified. No confirmed malware was identified by the limited static checks, but the project is **not certified malware-free**.
