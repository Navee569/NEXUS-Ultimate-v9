# NEXUS Ultimate v8 candidate — WebView and sensitive-log hardening

Date: 2026-10-16  
Base: NEXUS Ultimate v7 candidate — Keystore credential storage

## Changes in this continuation

1. **Removed the JavaScript bridge from the Google/YouTube sign-in WebView.** Page metadata is now retrieved through `evaluateJavascript`'s result callback. The WebView navigation callback only allows HTTPS Google/YouTube hosts, and file/content access is disabled with mixed content blocked. This reduces exposure of the account-bearing WebView to arbitrary origins and iframes.
2. **Redacted sensitive PoToken/BotGuard output from logs.** Logs now record response/token lengths rather than raw BotGuard responses, integrity-token prefixes, or generated proof-of-origin tokens.
3. **Made the PoToken request body JSON-safe.** The GenerateIT payload is built with `JSONArray`, instead of interpolating a server response into a JSON string.
4. **Escaped the PoToken identifier before embedding it into JavaScript.** The identifier is now encoded with `JSONObject.quote`, avoiding a JavaScript-string injection path if an unexpected identifier reaches this method.
5. **Removed authentication data from logout logs.** Logout no longer prints the first part of the YouTube cookie or visitor/data-sync identifiers, and no longer logs the cleared values.
6. **Reduced manifest permissions.** Removed `WRITE_SETTINGS`, which had no direct use in the app source, and limited legacy `READ_EXTERNAL_STORAGE` to API 32 and below. Microphone and location permissions were retained because source references show features that use them.

## Static checks performed

- Parsed all project XML files with Python's XML parser.
- Checked that the sign-in screen no longer calls `addJavascriptInterface`.
- Checked that the PoToken source no longer logs raw generated token values or response bodies.
- Checked that the logout source no longer interpolates cookie, visitor-data, or data-sync values into log messages.
- Checked that `WRITE_SETTINGS` is absent and legacy storage permission is capped at API 32.
- Rechecked the archive after packaging.

## Important limitations

This remains a **source-only candidate, not a verified release**. The Gradle wrapper cannot download Gradle in this environment because DNS resolution for `services.gradle.org` fails. No Kotlin compilation, unit tests, lint, dependency resolution, SBOM/CVE scan, APK build/analysis, device testing, or live network capture was completed.

The Google/YouTube login flow is sensitive to provider redirects and page structure. The host allowlist and `evaluateJavascript` extraction need real-device login testing. The Google API-key-shaped constant in `PoTokenWebView.kt` remains unverified for ownership/restrictions; an embedded client API key is not a secret and should be restricted by its owner. The local JavaScript bridges used by the cipher/EJS/PoToken WebViews remain an architectural audit item, although their content is loaded from app-generated/local HTML and the relevant WebViews block network loads. YouTube account cookies and the saved-account JSON are still stored in ordinary app preferences; encrypting those values safely requires a broader migration across all account-management call sites and is a high-priority remaining task.

Other outstanding work: verify the FFmpegKit fork's provenance/checksum; resolve and scan dependencies; review merged manifests for every build variant; run OAuth/Keystore migration and account-login tests; and implement/test actual EQ curves and the advanced recommendation engine.

## Validation commands for a connected Android build machine

```bash
./gradlew --version
./gradlew :app:assembleUniversalFossDebug
./gradlew :app:testUniversalFossDebugUnitTest
./gradlew :app:lintUniversalFossDebug
./gradlew :app:dependencies --configuration universalFossDebugRuntimeClasspath
```

After successful dependency resolution, generate an SBOM and scan both Java/Kotlin and native dependencies before producing a release build.
