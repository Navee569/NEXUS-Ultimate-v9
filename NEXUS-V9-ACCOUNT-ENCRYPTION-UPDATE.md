# NEXUS Ultimate v9 — YouTube Account Encryption

## Scope

This source update adds Android Keystore-backed encryption for YouTube authentication cookies and saved-account records, following the v7 Discord credential-hardening pattern.

## Implemented

- Added `SecureAccountStore` in the shared `core` module. It uses AES-256-GCM with a non-exportable Android Keystore key.
- Authenticated additional data binds each ciphertext to its logical preference name, preventing encrypted values from being silently swapped between protected preference fields.
- Encrypts the YouTube cookie, saved-account JSON (which contains saved cookies), visitor data, data-sync ID, and account name/email/channel handle.
- Adds a one-time migration from legacy plaintext Preferences DataStore entries to separate encrypted backing keys, then removes legacy plaintext entries.
- On decryption/authentication failure, returns no credential and removes corrupt encrypted entries during migration; it does not fall back to a legacy plaintext value when ciphertext exists.
- Updates Compose preference reads/writes and direct account-related DataStore access paths to use the secure store.
- Updates account switching and visitor-data refresh writes to store protected values encrypted.
- Logout removes active encrypted account material while preserving the saved-account list, matching the app's existing “logout but keep saved accounts” behavior.

## Migration and failure behavior

Migration runs during application startup before account observers are started. Existing account records are preserved if the Keystore key can be created and legacy values can be encrypted. If encryption cannot be initialized, protected legacy values are removed rather than left on disk in plaintext. If ciphertext cannot be authenticated/decrypted, that credential is unavailable and sign-in may be required. This is intentionally fail-closed and should be tested with real upgrade installs before release.

## Static checks

The v9 packaging process checks XML parseability, archive integrity, use of the encrypted store, and remaining direct plaintext reads/writes to protected logical preference keys.

## Not verified

- Gradle compilation, Kotlin compilation, unit tests, lint, dependency resolution, APK build, or device/emulator execution.
- Upgrade migration on an installed v8 build, including multiple saved accounts and logout/account switching.
- Keystore behavior across OS upgrades, backup/restore, app data clearing, and hardware-backed versus software-backed devices.
- Full security audit, dependency CVE scan, SBOM generation, network capture, and runtime verification.

The build attempt could not download Gradle 9.3.1 because `services.gradle.org` was not resolvable in the build environment. This is a source candidate, not a verified release APK.
