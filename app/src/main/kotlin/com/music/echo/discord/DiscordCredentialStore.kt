package echo.music.iad1tya.discord

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import echo.music.iad1tya.constants.DiscordRefreshTokenKey
import echo.music.iad1tya.constants.DiscordTokenKey
import echo.music.iad1tya.utils.dataStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.first

/**
 * Stores Discord OAuth credentials encrypted at rest with an Android Keystore AES-GCM key.
 * Legacy plaintext Preferences keys are migrated once and removed after successful encryption.
 * If migration/decryption fails, credentials are discarded and the user must sign in again.
 */
internal object DiscordCredentialStore {
  private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
  private const val KEY_ALIAS = "nexus_discord_oauth_aes_gcm_v1"
  private const val TRANSFORMATION = "AES/GCM/NoPadding"
  private const val TAG_BITS = 128
  private const val IV_BYTES = 12

  private val encryptedAccessTokenKey = stringPreferencesKey("discord_access_token_encrypted_v1")
  private val encryptedRefreshTokenKey = stringPreferencesKey("discord_refresh_token_encrypted_v1")

  data class Credentials(val accessToken: String?, val refreshToken: String?)

  suspend fun read(context: Context): Credentials {
    migrateLegacy(context)
    val prefs = context.applicationContext.dataStore.data.first()
    return try {
      Credentials(
        accessToken = prefs[encryptedAccessTokenKey]?.let(::decrypt),
        refreshToken = prefs[encryptedRefreshTokenKey]?.let(::decrypt),
      )
    } catch (_: Exception) {
      // Key invalidation or corrupt ciphertext must fail closed, not fall back to plaintext.
      clearTokenMaterial(context)
      Credentials(null, null)
    }
  }

  suspend fun write(context: Context, accessToken: String, refreshToken: String?) {
    val encryptedAccess = encrypt(accessToken)
    val encryptedRefresh = refreshToken?.takeIf { it.isNotBlank() }?.let(::encrypt)
    context.applicationContext.dataStore.edit { prefs ->
      prefs[encryptedAccessTokenKey] = encryptedAccess
      if (encryptedRefresh != null) {
        prefs[encryptedRefreshTokenKey] = encryptedRefresh
      } else {
        prefs.remove(encryptedRefreshTokenKey)
      }
      prefs.remove(DiscordTokenKey)
      prefs.remove(DiscordRefreshTokenKey)
    }
  }

  suspend fun clearTokenMaterial(context: Context) {
    context.applicationContext.dataStore.edit { prefs ->
      prefs.remove(encryptedAccessTokenKey)
      prefs.remove(encryptedRefreshTokenKey)
      prefs.remove(DiscordTokenKey)
      prefs.remove(DiscordRefreshTokenKey)
    }
  }

  private suspend fun migrateLegacy(context: Context) {
    try {
      context.applicationContext.dataStore.edit { prefs ->
        val legacyAccess = prefs[DiscordTokenKey]
        val legacyRefresh = prefs[DiscordRefreshTokenKey]
        if (prefs[encryptedAccessTokenKey].isNullOrBlank() && !legacyAccess.isNullOrBlank()) {
          prefs[encryptedAccessTokenKey] = encrypt(legacyAccess)
        }
        if (prefs[encryptedRefreshTokenKey].isNullOrBlank() && !legacyRefresh.isNullOrBlank()) {
          prefs[encryptedRefreshTokenKey] = encrypt(legacyRefresh)
        }
        // Remove plaintext only after encryption succeeded for the values being migrated.
        prefs.remove(DiscordTokenKey)
        prefs.remove(DiscordRefreshTokenKey)
      }
    } catch (error: Exception) {
      // Do not leave an unusable legacy plaintext credential behind if secure migration fails.
      runCatching { clearTokenMaterial(context) }
      throw IllegalStateException("Discord credentials could not be secured; please sign in again.", error)
    }
  }

  private fun encrypt(plaintext: String): String {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
    val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
    val combined = cipher.iv + ciphertext
    return Base64.encodeToString(combined, Base64.NO_WRAP)
  }

  private fun decrypt(encoded: String): String {
    val combined = Base64.decode(encoded, Base64.NO_WRAP)
    require(combined.size > IV_BYTES) { "Invalid encrypted Discord credential" }
    val iv = combined.copyOfRange(0, IV_BYTES)
    val ciphertext = combined.copyOfRange(IV_BYTES, combined.size)
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
    return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
  }

  @Synchronized
  private fun getOrCreateKey(): SecretKey {
    val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
    generator.init(
      KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(true)
        .setKeySize(256)
        .build(),
    )
    return generator.generateKey()
  }
}
