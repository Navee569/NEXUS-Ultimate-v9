# NEXUS Ultimate — Handoff for Gemini

**Handoff date:** 2026-10-16  
**Project:** NEXUS Ultimate Private Edition, a personal fork/customization of Echo Music for Android.  
**Starting project:** User-provided `Echo-Music-main.zip`.  
**Important:** Treat the source tree as an unverified development snapshot, not a release-ready or security-certified app.

## What the user wants

Continue developing a genuinely useful, highly personalized Android music app for the user's Samsung Galaxy S23 5G. Priorities:

1. Implement as many practical features as can be completed reliably.
2. Improve privacy and security; investigate the earlier Discord token extraction concern and other credential/network risks.
3. Deliver working source code and, once successfully built and tested, an installable APK.
4. Be precise about what is implemented versus merely planned. Do not claim builds/tests passed unless they were run and the results recorded.
5. The user wants personal/exclusive use and low unnecessary exposure. Do not promise invisibility: Android, device administrators, network operators, and providers can observe some app/network activity. Do not add stealth, security-control bypass, or covert surveillance features.

## Source and project layout

The source is in this ZIP's `Echo-Music-main/` directory. It is an Android Studio/Gradle Kotlin project based on Echo Music. Inspect `settings.gradle*`, root Gradle files, `gradle/libs.versions.toml`, `app/build.gradle.kts`, app manifests, and source code before making changes. The project contains upstream features and tests as well as NEXUS additions and audit reports.

Useful files included in this project:

- `NEXUS_CHANGELOG.md` — summary of customizations.
- `NEXUS_FEATURES.md` — feature status and limitations.
- `NEXUS_PRIVATE_EDITION.md` — privacy/project notes.
- `NEXUS_PRIVACY_POLICY.md` — NEXUS privacy notes.
- `NEXUS_SECURITY_AND_FEATURE_STATUS.md` — status summary.
- `NEXUS_BUILD_AND_TEST_STATUS.md` — previous build blocker and required checks.
- `NEXUS-LOCAL-AUDIT-REPORT.md` — offline heuristic audit results.
- `NEXUS-LOCAL-AUDIT-RESULTS.json` — machine-readable scan results; sensitive key-like snippets are redacted.
- `NEXUS_DEPENDENCY_INVENTORY.md` — declared dependency inventory, not a resolved SBOM.
- `scripts/nexus_static_audit.py` and `tools/nexus_offline_audit.py` — source-level heuristic checks, not malware/CVE scanners.

## Work already represented in this source snapshot

### NEXUS identity and user-facing features

- Separate Android application ID is intended to be `com.nexus.music.private`; launcher branding is NEXUS. Verify actual Gradle variants and merged manifests.
- NEXUS Studio / MoodSync: six presets (Deep Focus, Beast Mode, Unwind, Night Drive, Discovery, Power Nap), durations, timer/progress, pause/resume/reset, and local per-preset session counts.
- Sonic Memory: local personal notes, tags, search/filter, deletion, clear-all confirmation, and explicit share/export.
- Music Constellation: visualization of Sonic Memory entries. It is a visual grouping of saved memories, **not** a validated music-similarity graph.
- Smart Discovery: preferences for mood/energy, vocal style, language, duration; builds a query against existing music search.
- Source inspection in the latest status report indicates Smart Discovery queue wiring: query existing YouTube music search, filter `SongItem`, discard blank IDs, deduplicate, cap at 30, convert to playback `MediaItem`s, call `PlayerConnection.playQueue(ListQueue(...))` or `PlayerConnection.addToQueue(...)`. This is **source-level only**; compilation/runtime behaviour is unverified. Duration influences query text and does not guarantee exact mix length.
- Audio profile labels are saved locally and can open the existing equalizer. Applying the actual EQ profile remains manual; automatic Bluetooth/device-aware EQ is **not** implemented.
- Privacy quick controls link to existing listening/search history preferences and screenshot-protection preference. Verify the semantics of the existing preferences before claiming they stop all collection.
- Existing upstream supported search, playback, playlists, lyrics, Music Flow, equalizer and other features are intended to remain. Provider/build-variant limitations may apply.

### Security/privacy changes already attempted in the fork

- Removed Discord WebView JavaScript that reads `window.localStorage.getItem('token')` and passes a session token to Android.
- Replaced the old approach with a Discord OAuth authorization-code + PKCE flow. **Must audit it thoroughly**: state validation, redirect URI validation, PKCE verifier/challenge, callback parsing, CSRF/replay protection, token storage/refresh/revocation and whether the flow is actually supported by Discord. Do not assume it is secure merely because PKCE is mentioned.
- Removed the hardcoded Last.fm shared secret from BuildConfig; signed Last.fm functionality may no longer work. Never embed a real client secret in an APK.
- Removed Firebase Analytics/Crashlytics runtime dependencies and upstream Google Services config in this fork. Verify merged manifests and transitive dependencies and ensure the app does not still initialize telemetry elsewhere.
- Disabled Android automatic backups.
- Removed `REQUEST_INSTALL_PACKAGES` from the main app manifest; check every build variant/manifest merge.
- Disabled cleartext HTTP by default; audit exceptions, `usesCleartextTraffic`, network security config, WebViews and all URL paths.
- Changed app identity/branding to NEXUS.
- Updated the retired FFmpegKit dependency coordinate from `com.arthenica:ffmpeg-kit-audio:6.0-2` to `dev.ffmpegkit-maintained:ffmpeg-kit-audio:8.1.7`. This is a community-maintained fork/supply-chain decision; build compatibility and artifact provenance have not been verified.
- Declared protobuf-javalite version is `4.36.2`; still resolve the full graph and verify there are no vulnerable older transitive/duplicate versions.

## Checks already performed and their limits

- 582 XML files parsed with zero XML parsing errors in the latest recorded audit. This is XML syntax validation, not Android build verification.
- Gradle version catalog was parsed; inventory lists 75 library aliases and 50 version keys.
- A heuristic scan covered about 1,558 source/config files. It found zero matches for the specific shell-execution patterns checked, zero for the checked dynamic DEX-loading patterns, and zero for the old Discord `localStorage` token-extraction pattern.
- The scan found four JavaScript-interface declaration locations, 11 cleartext-HTTP string matches, three reflection/class-loading matches (may include normal framework/test use), and one credential-like assignment: a Google API-key-shaped value associated with `PoTokenWebView.kt` / `x-goog-api-key`. The key was redacted from reports. Verify whether it is a public client key, owner, API restrictions, allowed APIs, app restrictions and quota. Do not publish the key.
- The scan extracted URL/WebSocket literals; these are potential endpoints in source/config, not proof all are contacted at runtime.
- 59 unit-test source files were found; no instrumentation-test source files were found by the audit scan. Test task execution did not occur.
- No Gradle dependency lockfiles were found at audit time.
- ZIP integrity and XML parsing passed.

## Tests NOT performed / blockers

A previous attempt to run:

```bash
./gradlew :app:assembleUniversalFossDebug --no-daemon
```

and a separate `./gradlew test --no-daemon --stacktrace` attempt failed **before Gradle started** because DNS could not resolve `services.gradle.org` (`java.net.UnknownHostException`). Therefore:

- No Kotlin/Java compilation has been verified.
- No APK/AAB was produced from this snapshot.
- No unit tests have run in this environment.
- No instrumentation tests or physical-device tests have run.
- No Smart Discovery playback/queue runtime test has run.
- No complete Gradle-resolved dependency graph, SBOM, or CVE scan has been completed.
- No dependency checksum/signature verification has been completed.
- No compiled APK static analysis or signing-certificate review has been completed.
- No live network/packet capture has been done.
- No OAuth flow test or penetration test has been done.
- No verification that the distributed APK corresponds to this source has been done.
- No full review of native libraries or all third-party code has been completed.

The prior environment had Java 21 but no configured Android SDK/`adb` available for device testing. Confirm current environment rather than assuming this is still the case.

## Required work — recommended order

### Phase 1: Establish reproducible build

1. Inspect the whole project and Git history/diff if available. Do not blindly trust earlier assistant summaries; verify the actual files.
2. Check Gradle wrapper version, AGP/Kotlin compatibility, JDK version, Android SDK requirements, flavor names and task names.
3. In an environment with reliable network access, run `./gradlew --version`, `./gradlew :app:tasks --all`, then build the exact FOSS/universal debug variant. Example from earlier report: `./gradlew :app:assembleUniversalFossDebug`; verify task exists first.
4. Fix all compile/resource/manifest errors, including possible incompatibility from the FFmpegKit replacement. Do not remove functionality just to silence errors without documenting the trade-off.
5. Run all unit tests and Android lint. Record exact commands, exit codes, tool versions and logs. Add tests for new NEXUS code.
6. If possible, build on a clean machine/CI and record hashes of source archive and APK.

### Phase 2: Correctness and feature tests

- MoodSync timer: preset selection, duration boundaries, pause/resume/reset, process death, lifecycle/background behaviour, time-zone/clock edge cases, accessibility and local counter persistence.
- Sonic Memory: create/edit/delete, tags, search/filter, clear-all confirmation, export, malformed/large text, process restart, local persistence and data deletion.
- Music Constellation: empty/single/many memory states, graph rendering, accessibility, performance and whether links are labelled honestly as memory groupings.
- Smart Discovery: all moods/languages/vocal settings, empty query, no network, provider errors, cancellation, loading state, duplicate IDs, blank IDs, >30 results, `SongItem` filtering, play-mix vs add-to-queue semantics, existing queue preservation, queue order, repeat/shuffle interaction, playback errors, exact provider/build variant, duration caveat.
- Audio profiles: profile persistence, EQ UI link, accurate statement that profile label selection does not yet apply actual curve; test supported device types. Do not mark automatic Bluetooth EQ done unless implemented and device-tested.
- Privacy settings: verify each toggle actually changes behaviour; ensure private mode does not imply data is withheld from external music providers if requests still happen.
- Regression tests for all upstream playback, search, playlist, lyrics, widgets, offline and provider-specific features.

### Phase 3: Security review and fixes

1. **Discord OAuth:** remove any remaining token scraping; audit supported flow, state/PKCE, redirect validation, token lifetime/storage, logging, revocation, and error handling. Consider Android Keystore-backed encryption if tokens must persist. Do not write tokens to logs, crash reports or analytics. Verify provider terms/official API support.
2. **WebViews and JS bridges:** audit all four JS-interface locations; restrict navigation and allowed origins; avoid exposing privileged Android methods to arbitrary pages; disable unnecessary JavaScript/file/content access; verify safe browsing and SSL error handling; reject bad certificates rather than bypassing errors. Add tests for malicious navigation/redirects.
3. **API-key-shaped constant:** locate in `PoTokenWebView.kt`, do not reveal it; determine if it is public/restricted, check owner/allowed APIs/referrers/app restrictions/quota and rotate/restrict if the owner has authority. Do not claim client keys can be hidden in an APK.
4. **Cleartext/network:** contextualize all 11 HTTP literal matches; distinguish test fixtures/parser examples from active requests; check manifest merges for every flavor; enforce HTTPS where feasible; document unavoidable exceptions.
5. **Permissions:** inspect actual manifest merge for each variant. Justify and request location, microphone, Bluetooth, media/storage, `WRITE_SETTINGS` only when feature needs them. Test denial and revocation. No unnecessary permissions.
6. **Analytics/telemetry:** verify no analytics/Crashlytics runtime initialization remains, including transitive libraries and merged manifest; inspect crash logging and third-party SDKs.
7. **Local data and backup:** test backup/extraction flags for each variant; assess local DataStore/preferences and memory privacy; define export/delete behaviour. Encryption at rest may be appropriate for sensitive items but is not automatically required for every preference.
8. **Dependency/SBOM:** resolve every flavor's runtime/compile dependency graph, generate SBOM (CycloneDX or SPDX), run a current CVE/SCA scanner, inspect native `.so` libraries and licenses, verify checksums/signatures, review transitive protobuf versions and FFmpegKit maintained fork provenance. Upgrade/fix only with compatibility tests.
9. **Static APK review:** build APK; inspect with `apkanalyzer`, `aapt`, JADX/APKTool and MobSF or equivalent; inspect permissions, exported components, deep links, network config, embedded endpoints, secrets, obfuscation and native libraries. Verify APK signing certificate and hashes.
10. **Runtime network/privacy:** on a dedicated test device/emulator, observe DNS and connections during cold start, idle/background, search, playback, lyrics, Discord OAuth, Listen Together, location, AI features and crash conditions. Use an approved proxy/test network; note certificate pinning or TLS limitations. Record endpoint, purpose, payload categories and whether any identifiers/tokens are transmitted. Do not assert zero network transmission from static code alone.
11. **Malware analysis:** run reputable static and dynamic analysis on the built APK, but interpret detections manually; false positives are possible. Test network and behaviour without real personal credentials first.
12. **Privacy policy:** update policy to reflect actual tested behaviour, services, retention, telemetry and permissions; ensure the document matches code and app-store requirements.

### Phase 4: Galaxy S23 5G validation

- Install a debug build on Samsung Galaxy S23 5G / current Android version.
- Test cold start, playback, searching, queue, lyrics, playlists, background playback, lock screen, notification controls, headphones/Bluetooth, orientation, battery usage, network loss/reconnect, permission revocation, storage pressure and process death.
- Verify app ID/name/icon and that the app does not falsely claim to be invisible. Verify Play Protect / Android security warnings and signing identity.
- Check accessibility, text scaling, dark/AMOLED UI, crash logs and performance.
- Test uninstall/reinstall and ensure no unexpected retained data outside documented backups/export.

## Commands to start with

Run in the repository root in an environment with network, JDK, Android SDK and `adb`:

```bash
java -version
./gradlew --version
./gradlew :app:tasks --all
python3 scripts/nexus_static_audit.py
./gradlew :app:assembleUniversalFossDebug --no-daemon
./gradlew :app:testUniversalFossDebugUnitTest --no-daemon
./gradlew :app:lintUniversalFossDebug --no-daemon
```

Confirm the exact variant/task names from `:app:tasks --all`; the examples may need adjustment. Also build other distributed flavors. Generate a resolved dependency report and SBOM after Gradle can resolve dependencies.

## Instructions to Gemini

Please continue from the **actual uploaded source ZIP**, not from this summary alone. First inspect the source tree and verify every claimed change against code. Do not generate another ZIP that merely contains documentation or UI mockups. Prioritize reproducible compilation and real implementation. Make minimal, reviewable commits/patches and preserve upstream license notices (the original project is GPL-3.0; verify its exact license and comply with it). Do not silently remove existing functionality. Run tests and include logs/results. If you cannot access network/Android SDK, state exactly which tasks are blocked and provide reproducible commands for a developer machine. Do not claim malware-free, fully private, anonymous, or tested unless the evidence supports the claim. Return: (1) changed files and diff summary, (2) build/test commands with actual results, (3) dependency/SBOM/CVE results, (4) security findings with file/line evidence and severity, (5) unresolved risks, (6) final source ZIP, and (7) APK only if it was successfully built and its build/signing status is documented.
