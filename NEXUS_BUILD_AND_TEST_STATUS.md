# NEXUS Build, Queue, and Dependency Audit Status

Audit date: 2026-10-16

## Build attempt

Command attempted:

```bash
./gradlew :app:assembleUniversalFossDebug --no-daemon
```

Result: **BLOCKED before Gradle starts**. The wrapper could not resolve `services.gradle.org` (`java.net.UnknownHostException`). This is an environment/network blocker, not a compilation result. No APK was produced, and the Kotlin sources were not compiled.

## Smart Discovery queue wiring

Static source inspection confirms the NEXUS Studio discovery action:

1. Builds a search query from the selected mood, vocal style, language, and duration.
2. Calls the existing YouTube music search API.
3. Filters results to `SongItem`, deduplicates by ID, rejects blank IDs, and caps the generated list at 30.
4. Converts results to playback `MediaItem`s.
5. Calls the existing `PlayerConnection.playQueue(ListQueue(...))` for Play mix and `PlayerConnection.addToQueue(...)` for Add to queue.

This establishes source-level wiring only. Runtime queue behaviour, error handling under network failures, and playback on a physical device remain unverified. The chosen session duration currently affects the search phrase; it does not guarantee an exact-duration mix.

## Dependency audit scope and findings

The version catalog contains 75 declared library aliases and 50 version keys. This is a declared dependency inventory, not the resolved transitive dependency graph.

### Fixed in this source revision

- Migrated the FFmpegKit audio dependency from the retired `com.arthenica:ffmpeg-kit-audio:6.0-2` coordinate to `dev.ffmpegkit-maintained:ffmpeg-kit-audio:8.1.7`. The original FFmpegKit artifacts were retired/removed from Maven Central; the new artifact is a community-maintained continuation and therefore adds a maintainer/supply-chain trust decision. Verify checksums and resolved dependencies in a connected build environment before release.
- The declared `protobuf-javalite` version is `4.36.2`, above the fixed versions listed for CVE-2024-7254. Still check the resolved graph for older duplicate/transitive protobuf versions.

### Manual review item

- `PoTokenWebView.kt` contains a Google API-key-shaped constant used as the `x-goog-api-key` request header. The value is not repeated in this report. This can be a public client key rather than a password, but it should be verified against the key owner's Google Cloud API restrictions, allowed APIs, application restrictions, and quota. Do not assume that moving a client key into a local properties file makes it secret after it is packaged into an APK.

### Not completed

- Full CVE scan of the resolved dependency graph (blocked by Gradle distribution/network access).
- Native-library SBOM and vulnerability scan.
- Dependency checksum verification.
- APK static analysis and signing-certificate review.
- Runtime traffic capture for playback, search, OAuth, and background operation.
- Instrumented/unit tests and physical-device playback verification.

## Reproducible source checks

Run:

```bash
python3 scripts/nexus_static_audit.py
```

This writes `NEXUS_STATIC_AUDIT.md` and `NEXUS_STATIC_AUDIT.json`. It performs source/configuration sanity checks only; it is not a malware scanner or CVE scanner.

## Recommended local build/test commands

Use JDK 21 and an Android SDK matching the project configuration. From the repository root:

```bash
./gradlew :app:assembleUniversalFossDebug
./gradlew :app:testUniversalFossDebugUnitTest
```

If these task names differ in a given checkout, inspect `./gradlew :app:tasks --all` and use the exact variant name present there.
