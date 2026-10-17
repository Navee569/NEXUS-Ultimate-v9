# NEXUS — Architecture and Original Design Specification

## Status
Design proposal for the existing GPL-3.0 fork. This is a target architecture and design contract, not a claim that all modules/features have already been implemented. Preserve upstream copyright/license notices for all inherited code and keep the GPL-3.0 LICENSE. New original NEXUS UI work should use newly authored Compose components, icons, strings and assets; record upstream code/assets and licenses in an Open-source licenses screen.

## Architecture principles
- Kotlin, Jetpack Compose, Material 3, Kotlin Flow and structured concurrency.
- Media3 ExoPlayer/MediaSession for standard playback; use existing playback and DSP modules after behavior and thread-safety audits.
- Hilt dependency injection; Room for durable library/index/playlist/history metadata; DataStore for non-sensitive preferences.
- Android Keystore for locally stored encryption keys and tokens where appropriate. Keystore protects keys at rest; it does not make secrets safe to embed in the APK or protect data once compromised in process memory.
- Offline-first local library. MediaStore observer/change generations trigger incremental rescans; run DB writes in transactions and keep metadata extraction off the main thread. Use WorkManager only for deferrable work, not playback.
- Network clients use explicit timeouts, bounded retries, caching, cancellation, TLS validation and clear opt-in controls. Never log tokens, lyrics/search history, media URLs containing credentials, or user identifiers.
- WebViews are avoided unless necessary. If required: HTTPS allowlist, Safe Browsing where available, no JS bridge for untrusted content, disable file/content access and mixed content, block untrusted navigation, clear/partition cookies deliberately, and use strict CSP on owned HTML.
- No security downgrade to chase speed. Avoid cleartext traffic, permissive trust managers, exported components without a concrete need, and disabled certificate validation.

## Target module boundaries
- :app — application composition, navigation, feature entry points only.
- :design-system — NEXUS tokens, typography, shapes, original icons, reusable controls, previews and screenshot tests.
- :domain — entities, use cases and repository interfaces; Android-free where feasible.
- :data — Room, MediaStore, tag parsing, network implementations and repository bindings.
- :playback — Media3 player/session, queue transitions, audio focus, routing, notification/lock-screen integration.
- :audio-dsp — EQ/ReplayGain/crossfade abstractions, device capability detection and per-device presets.
- :feature:home, :feature:library, :feature:search, :feature:player, :feature:queue, :feature:lyrics, :feature:settings — isolated Compose feature modules.
- :benchmark — Macrobenchmark and baseline profile generation.
- Existing source modules should be migrated only after dependency graph inspection; avoid a wholesale rewrite that risks playback regressions. Hilt modules bind interfaces to implementations.

## Data and threading rules
- UI consumes immutable StateFlow UI state; events flow to ViewModels/use cases.
- No disk, MediaStore query, network, tag parsing, model inference or blocking lock on main thread.
- Use stable item keys and contentType for lazy lists. Derive state only where it reduces recomposition; defer state reads to the latest possible layout/draw phase.
- Use bounded image sizes and Coil memory/disk caching; cancel off-screen work. Visualizer sampling is lifecycle-aware and throttled in Battery Saver.
- All background work is cancellable, unique where appropriate, and idempotent.
- Add StrictMode in debug only, and surface violations during tests.
- Feature flags must default to safe/off for microphone, location, AI inference, scrobbling, social listening and any remote provider.

## NEXUS visual language (original design contract)
- Identity: quiet, precise, nocturnal, audio-instrument feel; avoid reproducing Echo or other players' signature layouts/assets.
- Palette (initial tokens): AMOLED background #000000; elevated surface #0B1014; primary ice-cyan #72E5F2; secondary blue-gray #9BB2C0; primary text #F1F7FA; secondary text #A0AFB8; destructive #FF6B78. Contrast-check each semantic pairing before shipping. Light theme uses near-white neutral surfaces with deep blue-gray text and cyan reserved for emphasis.
- Typography: system sans (Roboto) for UI; tabular numerals for time/progress; clear 3-level hierarchy, no custom font required for legibility.
- Shape: 12dp controls, 18dp cards, large artwork with restrained rounding; avoid excessive nested cards and translucent blur that raises GPU cost.
- Motion: 120–220 ms purposeful transitions; respect animator duration scale and reduced-motion setting; no perpetual off-screen animation. Use haptics only for meaningful actions and expose a disable toggle.
- Iconography: original vector icons or AndroidX Material symbols used consistently; no unlicensed icon pack.
- Themes: System light/dark, true-black AMOLED, optional dynamic colors. Dynamic color never overrides accessibility contrast or makes essential controls indistinguishable.
- Navigation: bottom navigation for Home, Library, Search; persistent mini-player above navigation; full player as a distinct destination; queue/lyrics available as player subpages/sheets. Adaptive navigation rail/two-pane layout on large screens; keyboard, rotary and D-pad navigation where platform requires.
- Screens: onboarding/permissions, home/discovery, library (songs/albums/artists/genres/folders/playlists), search, now playing, queue, lyrics, audio settings/EQ, downloads/offline, listening history/stats, privacy controls, integrations, app settings, open-source licenses, diagnostics/about.
- Components: artwork tile, track row, mini-player, transport controls, seek bar, queue item, lyric line, permission rationale, source/provider chip, empty/error/loading states, settings row, confirmation dialog and accessible snackbar.
- Accessibility: minimum 48dp touch targets; content descriptions for meaningful artwork/actions; decorative images excluded from TalkBack; text scaling without clipping; semantic state for play/pause/selected; contrast WCAG AA target; keyboard focus; reduced motion; never communicate state using color alone.

## Playback and audio feature sequencing
1. Correct queue/MediaSession behavior and test existing playback; verify gapless on supported local formats and devices before advertising it.
2. Crossfade only with a tested dual-player/mixing strategy; define behavior for streams, interruptions, route changes and DSP.
3. ReplayGain: track/album gain precedence, clipping prevention, fallback and user override.
4. Parametric EQ: validate Android audio-effect session lifecycle and per-route capability; do not claim bit-perfect while DSP is active.
5. Incremental MediaStore index and duplicate detection with deterministic fingerprints; keep expensive hashing off the main thread.
6. Lyrics providers and scrobbling: opt-in, retry/backoff, user-visible status and privacy controls.
7. Local recommendation/search models: quantized, on-device, opt-in, cancellable, defer to charging/idle if resource intensive.
8. Stem separation, transcription, head tracking, beat matching, Auracast and USB DAC exclusive output are research-level workstreams requiring codec/model/device/licensing review; do not promise support before prototype and measurements.

## Security and privacy checklist
- Audit each exported activity/service/receiver/provider and all intent filters; explicit component export and permission requirements.
- Audit app links and custom schemes. Do not claim an unverified host; use owned HTTPS domain + assetlinks only when controlled.
- Verify network security config, cleartext policy, TLS defaults, WebView hardening, dependency CVEs and native library provenance.
- Keystore-backed local secrets; no server/client confidential secret embedded in BuildConfig, resources, native libraries or obfuscated code.
- Document all outbound endpoints and purpose; user controls for location, mic, scrobbling and AI.
- FOSS build must not initialize proprietary analytics/crash-reporting SDKs or require Google Play Services.
- Preserve license texts, SPDX notices and attribution for all dependencies and inherited source.

## Verification plan and acceptance criteria
Targets are goals until measured; do not label as verified without recorded device/build evidence.
- Unit: use cases, indexing reconciliation, duplicate detection, gain math, queue transitions, updater URL policy, privacy settings.
- Instrumented: MediaSession actions, permission-denied paths, rotation/process recreation, deep links, offline playback.
- UI/screenshot tests: dark/AMOLED/light/dynamic, large text, TalkBack semantics, empty/loading/error states.
- Macrobenchmark: cold startup, library scroll, search typing, player open/close and queue transition; collect frame timing and startup metrics on physical Galaxy S23 5G.
- Perfetto/System Trace: main-thread blocking, binder, allocations, GC, image decode and network stalls.
- Battery: one-hour playback, screen-on and screen-off separately; report device, OS, brightness, codec, volume, route and baseline.
- Memory: process PSS/RSS while idle and playing; target <150 MB PSS is a goal to measure, not an assumed fact.
- Build: assemble Universal FOSS Debug, unit tests, lint, dependency/license scan, APK artifact SHA-256.
- CI should retain logs and artifact metadata and fail if the APK is missing. Release builds require separate signing and supply-chain review.
