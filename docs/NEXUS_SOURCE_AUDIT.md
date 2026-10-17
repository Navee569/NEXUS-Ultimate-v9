# NEXUS — Source Audit and Feature Gap (2026-10-17)

## Chosen mode: A — GPL-3.0 fork
NEXUS currently lives in a repository whose Gradle project name, namespace, manifest resources, widgets, and README still contain Echo/Metrolist identifiers. The inspected app build file and manifest demonstrate that this is a derivative codebase, not a clean-room implementation. Fork mode is the accurate and legally safer choice: retain GPL-3.0, preserve all existing headers/copyright notices and the upstream LICENSE, and maintain an explicit upstream attribution/third-party notices screen. This document does not claim all attribution work is already complete.

## Scope and evidence
Audit based on the repository's current default-branch files inspected through GitHub: `settings.gradle.kts`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `gradle/libs.versions.toml`, `.github/workflows/android-build.yml`, and `README.md`. No device profiler trace, source-wide static scan, or local build was run as part of this document. Therefore performance root causes beyond direct source evidence are hypotheses to validate, not measured findings.

## Repository and architecture inventory
- Gradle modules declared: app, innertube, multiple lyrics providers, streaming/service integrations, canvas/artist-video, core, playback, USB audio, metadata, DSP core/audio DSP, domain.
- UI: Jetpack Compose/Material 3 and adaptive libraries are declared; Coil 3 is declared for image loading; Hilt, Room, DataStore and coroutines are declared.
- Playback: Media3 ExoPlayer, MediaSession, HLS, UI, OkHttp and Cronet datasource artifacts are declared; separate playback/DSP/USB-audio modules exist.
- Local persistence: Room runtime/ktx and DataStore are declared. This inventory does not prove all data access is offline-first or incremental.
- FOSS/GMS product flavors and ABI flavors exist. Existing CI invokes `:app:assembleUniversalFossDebug`.
- Existing app targets SDK 36 and compiles against SDK 37. Build uses JDK 21 and NDK 28.2.13676358.
- Project identity is mixed: root project is `echomusic`, Android namespace is `echo.music.iad1tya`, applicationId is `com.nexus.music.private`, theme names are Echo-specific, manifest contains Echo URL schemes/hosts and widget actions, and README is still Echo-branded. This is a confirmed cleanup and deep-link compatibility risk.
- Manifest declares location, microphone, boot receiver, wake lock and multiple foreground-service permissions. Each should be permission-gated by the feature that needs it; remove only after tracing usages to avoid regressions.
- `android:largeHeap="true"` is set. This can mask allocation pressure rather than fix it and must be benchmarked before changing.
- Last.fm API key is embedded in BuildConfig, while shared secret is blank with a source comment warning against shipping it. Public client IDs are not secrets; any true secret in an APK is recoverable. Keystore storage cannot make a third-party client secret safe when it must be sent to the provider.
- Workflow uploads APKs as artifacts, but has no test/lint/benchmark steps and uses broad APK globbing. It triggers on pushes to main and manual dispatch. It does not currently trigger on the audit branch.

## Update-flow finding
The checked-in README says upstream Echo removed in-app OTA updates and asks users to update manually. The repo README also remains Echo-branded. The exact click handler that launches Echo has not been located from the file excerpts inspected so far, so its URL cannot be asserted from evidence. The next code task must search for update/release URL constants, updater dialog call sites, browser intents and all Echo release URLs; disable/remove the inherited update entry point or bind it exclusively to an explicit NEXUS release URL. Add a regression test for the no-upstream-redirect behavior. Do not invent a NEXUS release URL before a release channel exists.

## Performance audit — confirmed risks vs. hypotheses
Confirmed risks from configuration:
- CI builds but does not run unit tests, lint, macrobenchmarks, startup profiles, or dependency/security review.
- A large multi-module app and numerous networking, lyrics, streaming, image, animation and native multimedia dependencies increase APK and runtime footprint; quantify rather than assume.
- `largeHeap` is enabled.
- No performance targets are verified in this audit: no S23 physical-device trace or battery measurement was supplied.
Hypotheses to profile:
- Main-thread work during library scanning, search, artwork decoding or Compose state updates.
- Unstable/recreated list state and expensive nested scroll/animation work.
- Redundant network requests/polling or large native library residency.
- Player transition stalls from source resolution or queue preparation rather than the Compose UI alone.
Do not claim cold-start, jank, memory, or battery targets until measured on a named device/build.

## Competitive feature-gap matrix
Legend: “documented” means found in public project feature documentation, not independently device-tested. “Unknown” means this source audit has not verified NEXUS behavior.

| Capability | NEXUS/Echo source inventory | Spotify / YouTube Music / Apple Music | Poweramp / Symfonium / Musicolet | Namida / Metrolist / Oto |
|---|---|---|---|---|
| Streaming catalogue/account | YouTube Music/Innertube-oriented integrations declared | First-party streaming catalogues/accounts | Mainly local/server libraries; exact feature varies | Namida and Metrolist document YouTube integrations; Oto is local-focused |
| Local library, folders, tag editing | Room and metadata modules exist; exact parity unknown | Not equivalent to power-user local-library tooling | Strong local playback/library feature focus varies by app | Namida documents indexing/tag editing/duplicate prevention; Oto is local-library oriented |
| Gapless, crossfade, loudness | Crossfade advertised in source README; true gapless/ReplayGain unverified | Service-specific behavior; not directly comparable | Varies; Poweramp has DSP/effects, Symfonium supports advanced playback | Namida documents gapless, crossfade, ReplayGain; Metrolist documents crossfade/audio normalization |
| Parametric EQ / AutoEQ / USB DAC | DSP and USB-audio modules/dependencies exist; capability and device support unverified | Platform/service-specific; not a common user-controlled parametric EQ feature | Poweramp is known for advanced DSP; Symfonium supports audio customization | Namida documents parametric EQ/AutoEQ and recent bit-perfect/USB-DAC features; verify exact release |
| Synced lyrics / scrobbling | Many lyric providers and Last.fm configuration are present; current correctness unknown | Lyrics availability varies; first-party integrations differ | Varies by app | Namida documents synced/word-synced lyrics and Last.fm/ListenBrainz; Metrolist documents synced lyrics and Last.fm |
| On-device recommendations/AI | Echo Brain is described in README; local-only/privacy properties require code audit | Personalized cloud recommendation systems | Usually library/player focused | Namida documents smart playlists/recommendations; implementations differ |
| Android Auto / Wear OS / widgets | Manifest advertises automotive capability and several widgets; actual UX tests unknown | Strong platform integrations vary by product | Varies by product | Namida documents Android Auto and Wear OS; Metrolist documents widgets |
| Privacy / local-only mode | Permissions and remote integrations are broad; data inventory/consent audit needed | Account/cloud dependent | Varies | FOSS projects differ; review each provider and network path |
| Performance evidence | No reproducible benchmark results captured here | Vendor internals generally unavailable | Third-party app benchmark data not normalized | Public release notes exist; not comparable without same-device tests |

Products are not one-to-one substitutes. Spotify, YouTube Music and Apple Music are primarily streaming services; local players emphasize local libraries, codecs, DSP and device integration. Public feature pages do not establish parity or measured performance.

## Research references
- Namida feature list and release notes: https://github.com/namidaco/namida and https://github.com/namidaco/namida/releases
- Metrolist features and maintenance notice: https://github.com/MetrolistGroup/Metrolist
- Echo upstream README / GPL context: https://github.com/EchoMusicApp/Echo-Music
- Android performance guidance: https://developer.android.com/topic/performance
- Android Macrobenchmark: https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview
- Android Baseline Profiles: https://developer.android.com/topic/performance/baselineprofiles
- Media3: https://developer.android.com/media/media3

## Immediate implementation priorities
P0: trace and remove the erroneous update redirect; add a regression test.
P0: inventory all network endpoints, analytics/crash reports, permissions, WebViews, cookies, JS bridges, exported components, deep links and persisted credentials.
P1: add CI gates for unit tests, lint and dependency/license/security reports before any APK is labelled verified.
P1: instrument startup, scroll and player transitions; profile a physical Galaxy S23 5G before optimizing.
P2: measure APK size, process PSS while playing, and one-hour battery drain screen-on/off.
P3: implement advanced DSP/AI features one at a time with design notes, opt-in controls, test coverage, and explicit resource/battery budgets.
