# NEXUS Source-Level Static Audit

**Scope:** Repository source/configuration only; not APK, runtime traffic, or resolved dependency graph.

Declared version-catalog aliases reviewed: **75**

## Checks

| Check | Result | Detail |
|---|---|---|
| Android manifest XML | **PASS** | Parsed successfully; 18 uses-permission declarations. |
| Permission absent: android.permission.REQUEST_INSTALL_PACKAGES | **PASS** | Not declared in the main manifest. |
| Permission absent: android.permission.READ_CALL_LOG | **PASS** | Not declared in the main manifest. |
| Permission absent: android.permission.READ_SMS | **PASS** | Not declared in the main manifest. |
| Automatic backup | **PASS** | android:allowBackup=false |
| Cleartext traffic | **PASS** | android:usesCleartextTraffic=default false |
| Gradle version catalog | **PASS** | Parsed 50 version keys and 75 library aliases; actual transitive resolution unavailable without Gradle. |
| FFmpegKit dependency maintenance | **PASS** | Declared coordinate: dev.ffmpegkit-maintained:ffmpeg-kit-audio:8.1.7 |
| Protobuf version floor | **PASS** | Declared protobuf-javalite: 4.36.2 |
| Discord WebView token extraction pattern | **PASS** | No matching pattern found in scanned source/config files. |
| Legacy retired FFmpegKit coordinate | **PASS** | No matching pattern found in scanned source/config files. |
| Shell execution APIs | **PASS** | No matching pattern found in scanned source/config files. |
| Dynamic DEX/class loading APIs | **PASS** | No matching pattern found in scanned source/config files. |
| Hardcoded Last.fm secret BuildConfig field | **PASS** | LASTFM_SECRET is an empty placeholder; no embedded Last.fm secret found. |
| Smart Discovery queue wiring | **PASS** | Search, deduplicate, playQueue and addToQueue paths present. |
| Potential embedded credential literals | **REVIEW** | 1 heuristic matches; a Google API-key-shaped value is present in PoTokenWebView. It may be a client key rather than a secret, but its API restrictions and quotas should be verified. Values are not emitted by this report. |

## Dependency notes

- The legacy `com.arthenica:ffmpeg-kit-audio:6.0-2` coordinate was retired and its artifacts removed from Maven Central. The project now declares the community-maintained `dev.ffmpegkit-maintained:ffmpeg-kit-audio:8.1.7` coordinate. The maintainer describes it as a drop-in continuation, but it is a third-party fork; validate maintainer trust, artifact checksums, transitive graph, licence, and runtime behaviour before release.
- `protobuf-javalite` is declared at `4.36.2`, above the fixed versions listed for CVE-2024-7254. This observation applies only to the declared direct version; confirm the resolved graph has no older duplicate/transitive protobuf artifacts.
- This is not a full vulnerability scan. No resolved dependency graph was available.

## Limitations

- Gradle could not download its distribution in this environment because DNS resolution for services.gradle.org failed.
- No APK was compiled or executed, and no runtime network capture was performed.
- The dependency list is from the version catalog, not the resolved transitive dependency graph.
- A complete CVE scan requires resolving all variants and checking the resolved graph against a current advisory database.
