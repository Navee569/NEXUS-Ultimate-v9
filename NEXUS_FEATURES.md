# NEXUS Private Edition — Feature Status

## Implemented in this source fork
- Separate application ID and NEXUS branding.
- NEXUS Studio with local MoodSync session timers and local session counts.
- Sonic Memory: local notes, tags, search, deletion and explicit text export.
- Smart Discovery: user-controlled mood/energy, vocal style, language and duration preferences; creates a search query and opens the existing music search. Preferences are stored locally.
- Music Constellation: a visual graph of saved Sonic Memory entries, with memory list and tags. Connections are a visual grouping only, not a claim of measured musical similarity.
- Audio profile labels saved locally; user can open the existing equalizer to apply changes manually. Automatic Bluetooth switching is not implemented.
- Privacy controls for existing listening/search history toggles and screenshot preference.
- Security hardening in the fork: removed Discord WebView token extraction, removed embedded Last.fm shared secret, removed Firebase Analytics/Crashlytics runtime dependencies, disabled automatic backups, removed REQUEST_INSTALL_PACKAGES from the main manifest, and disabled cleartext HTTP by default (verify every build flavor/manifest merge).

## Existing upstream capabilities retained
- Existing supported music search, playback, playlists, lyrics, Music Flow and equalizer functionality. Availability depends on build variant and provider/service support.

## Not implemented / not verified
- No guarantee that the app is undiscoverable. Android, administrators, network operators and providers can observe some app/network activity.
- No packet-level network monitor or proof of all network data flows.
- No automatic AI mood classification, automatic playback queue generation from the new Smart Discovery form, or automatic Bluetooth EQ switching.
- Music Constellation currently visualizes Sonic Memories only.
- No successful Gradle build or on-device test has been completed in this environment because the Gradle distribution could not be downloaded (DNS/network unavailable).
- The source has not undergone a complete dependency/SBOM audit, dynamic analysis, APK signing verification or penetration test.
