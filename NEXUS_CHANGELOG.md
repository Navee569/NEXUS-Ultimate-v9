# NEXUS Private Edition — Change Log

## Personal feature pack

- Added NEXUS Studio, linked from Privacy settings.
- Added local MoodSync focus sessions with six presets, configurable durations, pause/resume/reset, progress display, and per-preset session counters.
- Added Sonic Memory for local personal notes with tags, search, tag filters, individual deletion, clear-all confirmation, and explicit share-sheet export.
- Added a local snapshot of saved memories and session counts.
- Added quick controls for the existing Private Session history preferences and screenshot-protection preference.
- Added privacy explanations that distinguish local-only custom data from online playback/provider requests.

## Security hardening carried forward

- Removed Discord WebView localStorage token scraping; uses OAuth authorization-code + PKCE.
- Removed hardcoded Last.fm shared secret from BuildConfig (signed requests may be unavailable).
- Removed Firebase Analytics/Crashlytics runtime dependencies and upstream Google Services config.
- Disabled Android automatic backup, removed APK installation permission from the main manifest, and blocked cleartext HTTP by default.
- Assigned a separate NEXUS application ID and launcher name.

## Verification

- XML/configuration and source presence checks performed.
- A full Gradle build could not be run because this environment could not resolve `services.gradle.org` to download Gradle 9.3.1.
- No runtime traffic capture or on-device test has been completed. Do not treat the source review as a guarantee of malware-free behaviour.

## Expanded NEXUS Studio
- Added Smart Discovery query builder with local preferences for mood/energy, vocal style, language and session duration.
- Added Music Constellation visualization for Sonic Memory entries.
- Added saved audio profile selection and a direct link to the existing equalizer. Applying a profile remains manual; automatic device-aware EQ is not implemented.
- Expanded privacy information to clarify external-service and network-monitoring limitations.
- Build remains unverified because Gradle distribution download failed due unavailable DNS/network.

## 2026-10-16 — NEXUS private audit follow-up

- Migrated the retired FFmpegKit audio coordinate to the community-maintained `dev.ffmpegkit-maintained:ffmpeg-kit-audio:8.1.7` coordinate after identifying that the original distribution was retired. This is a supply-chain change and remains unverified by a successful build in this environment.
- Added reproducible source-level checks at `scripts/nexus_static_audit.py` and reports at `NEXUS_STATIC_AUDIT.md` / `NEXUS_STATIC_AUDIT.json`.
- Added `NEXUS_BUILD_AND_TEST_STATUS.md` documenting Smart Discovery queue wiring, the build blocker, dependency-review findings, and outstanding test requirements.
- Revalidated 582 XML files and the Gradle version-catalog TOML; zero XML parse failures. These are syntax checks, not Android compilation.
