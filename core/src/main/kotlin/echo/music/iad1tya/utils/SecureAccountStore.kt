package echo.music.iad1tya.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.first

/** Encrypts YouTube session credentials and account-switching records at rest. */
object SecureAccountStore {
  private const val KEY_ALIAS = "nexus_youtube_account_aes_gcm_v1"
  private const val PREFIX = "nexus-secure-v1:"
  private const val TRANSFORMATION = "AES/GCM/NoPadding"

  // These are legacy logical preference names. Their plaintext entries are migrated then removed.
  private val protectedNames = setOf(
    "innerTubeCookie", "savedAccounts", "visitorData", "dataSyncId",
    "accountName", "accountEmail", "accountChannelHandle"
  )

  fun isProtected(name: String): Boolean = name in protectedNames

  private fun storageKey(name: String) = stringPreferencesKey("__nexus_secure_${name}_v1")
  private fun legacyKey(name: String) = stringPreferencesKey(name)

  /** Reads the encrypted value. Legacy fallback exists only to avoid data loss before migration. */
  fun read(preferences: Preferences, name: String): String? {
    if (!isProtected(name)) return preferences[stringPreferencesKey(name)]
    val encrypted = preferences[storageKey(name)]
    if (encrypted != null) return runCatching { decrypt(encrypted, name) }.getOrNull()
    return preferences[legacyKey(name)]
  }

  suspend fun write(context: Context, name: String, value: String) {
    writeBatch(context, mapOf(name to value))
  }

  suspend fun writeBatch(context: Context, values: Map<String, String>) {
    require(values.keys.all(::isProtected)) { "Attempted to write an unprotected account preference" }
    val encryptedValues = values.mapValues { (name, value) -> encrypt(value, name) }
    context.dataStore.edit { prefs ->
      encryptedValues.forEach { (name, encrypted) ->
        prefs[storageKey(name)] = encrypted
        prefs.remove(legacyKey(name))
      }
    }
  }

  /** One-time migration; if encryption fails, credentials are discarded rather than retained in plaintext. */
  suspend fun migrate(context: Context) {
    val snapshot = context.dataStore.data.first()
    val toEncrypt = linkedMapOf<String, String>()
    val corrupt = mutableSetOf<String>()
    for (name in protectedNames) {
      val encrypted = snapshot[storageKey(name)]
      if (encrypted != null) {
        if (runCatching { decrypt(encrypted, name) }.isFailure) corrupt += name
      } else {
        snapshot[legacyKey(name)]?.let { toEncrypt[name] = it }
      }
    }
    // Encrypt outside DataStore's transaction; no plaintext is rewritten on encryption failure.
    val encryptedValues = try {
      toEncrypt.mapValues { (name, value) -> encrypt(value, name) }
    } catch (_: Exception) {
      // Fail closed: remove legacy secrets if the Keystore is unavailable.
      context.dataStore.edit { prefs ->
        protectedNames.forEach { name ->
          prefs.remove(legacyKey(name))
          if (name in corrupt) prefs.remove(storageKey(name))
        }
      }
      return
    }
    context.dataStore.edit { prefs ->
      encryptedValues.forEach { (name, encrypted) ->
        prefs[storageKey(name)] = encrypted
      }
      // Remove every legacy plaintext entry, including names that already had valid ciphertext.
      protectedNames.forEach { name -> prefs.remove(legacyKey(name)) }
      corrupt.forEach { name ->
        prefs.remove(storageKey(name))
        prefs.remove(legacyKey(name))
      }
    }
  }

  fun clearProtectedPreferences(prefs: MutablePreferences, includeSavedAccounts: Boolean = false) {
    protectedNames.filter { includeSavedAccounts || it != "savedAccounts" }.forEach { name ->
      prefs.remove(legacyKey(name))
      prefs.remove(storageKey(name))
    }
  }

  private fun encrypt(plainText: String, name: String): String {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
    cipher.updateAAD(name.toByteArray(StandardCharsets.UTF_8))
    val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
    val combined = cipher.iv + encrypted
    return PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
  }

  private fun decrypt(encoded: String, name: String): String {
    require(encoded.startsWith(PREFIX)) { "Unsupported encrypted account data version" }
    val combined = Base64.decode(encoded.removePrefix(PREFIX), Base64.NO_WRAP)
    require(combined.size > 12 + 16) { "Encrypted account data is truncated" }
    val iv = combined.copyOfRange(0, 12)
    val ciphertext = combined.copyOfRange(12, combined.size)
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, getExistingKey(), GCMParameterSpec(128, iv))
    cipher.updateAAD(name.toByteArray(StandardCharsets.UTF_8))
    return String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
  }

  @Synchronized
  private fun getOrCreateKey(): SecretKey {
    getExistingKeyOrNull()?.let { return it }
    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    generator.init(
      KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .build()
    )
    return generator.generateKey()
  }

  private fun getExistingKey(): SecretKey = getExistingKeyOrNull()
    ?: throw IllegalStateException("Account encryption key is unavailable")

  private fun getExistingKeyOrNull(): SecretKey? {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    return (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)
  }
}
