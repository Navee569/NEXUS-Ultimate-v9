package echo.music.iad1tya.discord

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import echo.music.iad1tya.BuildConfig
import echo.music.iad1tya.constants.DiscordAvatarUrlKey
import echo.music.iad1tya.constants.DiscordNameKey
import echo.music.iad1tya.constants.DiscordTokenExpiresAtKey
import echo.music.iad1tya.constants.DiscordUsernameKey
import echo.music.iad1tya.utils.dataStore
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class DiscordAuthorizationSession(
  val state: String,
  val codeVerifier: String,
  val authorizationUri: Uri,
)

data class DiscordAccount(
  val id: String,
  val username: String,
  val displayName: String,
  val avatarUrl: String?,
)

data class DiscordAuthSession(
  val accessToken: String,
  val refreshToken: String?,
  val expiresAtMillis: Long,
  val account: DiscordAccount?,
)

object DiscordAuthCoordinator {
  val redirects =
    MutableSharedFlow<Uri>(
      replay = 1,
      extraBufferCapacity = 1,
    )

  fun emit(uri: Uri) {
    redirects.tryEmit(uri)
  }

  /** Drop the replayed redirect so an authorization code can never be processed twice. */
  fun consume() {
    redirects.resetReplayCache()
  }
}

object DiscordOAuthRepository {
  private const val AUTHORIZATION_ENDPOINT = "https://discord.com/oauth2/authorize"
  private const val TOKEN_ENDPOINT = "https://discord.com/api/oauth2/token"
  private const val REVOKE_ENDPOINT = "https://discord.com/api/oauth2/token/revoke"
  private const val CURRENT_USER_ENDPOINT = "https://discord.com/api/v10/users/@me"
  private const val REQUEST_TIMEOUT_MS = 12_000
  private const val EXPIRY_SKEW_MS = 60_000L

  private val json = Json { ignoreUnknownKeys = true }
  private val secureRandom = SecureRandom()

  val applicationId: Long
    get() = BuildConfig.DISCORD_APPLICATION_ID_LONG

  val redirectUri: String
    get() = "${BuildConfig.DISCORD_REDIRECT_SCHEME}://authorize/callback"

  fun createAuthorizationSession(): DiscordAuthorizationSession {
    val state = randomUrlSafeString(byteCount = 32)
    val verifier = randomUrlSafeString(byteCount = 64)
    val challenge = sha256Base64Url(verifier)
    val scopes =
      listOf(
          "openid",
          "identify",
        )
        .joinToString(separator = " ")

    val uri =
      Uri.parse(AUTHORIZATION_ENDPOINT)
        .buildUpon()
        .appendQueryParameter("client_id", BuildConfig.DISCORD_APPLICATION_ID)
        .appendQueryParameter("response_type", "code")
        .appendQueryParameter("redirect_uri", redirectUri)
        .appendQueryParameter("scope", scopes)
        .appendQueryParameter("state", state)
        .appendQueryParameter("code_challenge", challenge)
        .appendQueryParameter("code_challenge_method", "S256")
        .build()

    return DiscordAuthorizationSession(
      state = state,
      codeVerifier = verifier,
      authorizationUri = uri,
    )
  }

  suspend fun completeAuthorization(
    context: Context,
    session: DiscordAuthorizationSession,
    redirect: Uri,
  ): Result<DiscordAuthSession> =
    withContext(Dispatchers.IO) {
      DiscordAuthCoordinator.consume()
      runCatching {
        require(redirect.scheme == BuildConfig.DISCORD_REDIRECT_SCHEME) {
          "Unexpected Discord redirect scheme"
        }
        require(
          redirect.path == "/authorize/callback" ||
            (redirect.host == "authorize" && redirect.path == "/callback")
        ) {
          "Unexpected Discord redirect target"
        }
        require(constantTimeEquals(redirect.getQueryParameter("state"), session.state)) {
          "Discord authorization state mismatch"
        }

        redirect.getQueryParameter("error")?.let { error ->
          val description = redirect.getQueryParameter("error_description")
          throw IllegalStateException(description ?: error)
        }

        val code =
          requireNotNull(redirect.getQueryParameter("code")) {
            "Discord authorization code is missing"
          }

        val token = exchangeAuthorizationCode(code, session.codeVerifier)
        val account = runCatching { fetchAccount(token.accessToken) }.getOrNull()
        val authSession = token.toAuthSession(account)
        storeSession(context, authSession)
        authSession
      }
    }

  suspend fun getValidAccessToken(context: Context): String? =
    withContext(Dispatchers.IO) {
      val prefs = context.dataStore.data.first()
      val credentials = runCatching { DiscordCredentialStore.read(context) }.getOrNull()
      val currentToken = credentials?.accessToken?.trim().orEmpty()
      if (currentToken.isBlank()) {
        return@withContext null
      }

      val expiresAt = prefs[DiscordTokenExpiresAtKey] ?: 0L
      if (expiresAt > System.currentTimeMillis() + EXPIRY_SKEW_MS) {
        return@withContext currentToken
      }

      // Missing expiry metadata is treated as expired, not as an indefinitely valid token.
      val refreshToken = credentials?.refreshToken?.trim().orEmpty()
      if (refreshToken.isBlank()) {
        return@withContext null
      }

      // Never hand an expired token to callers: if refresh fails the caller must re-authorize.
      refreshAccessToken(context, refreshToken).getOrNull()?.accessToken
    }

  suspend fun getStoredAccessToken(context: Context): String? =
    withContext(Dispatchers.IO) {
      runCatching { DiscordCredentialStore.read(context).accessToken }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }
    }

  suspend fun fetchAccount(accessToken: String): DiscordAccount =
    withContext(Dispatchers.IO) {
      val response =
        getJson(
          url = CURRENT_USER_ENDPOINT,
          bearerToken = accessToken,
        )
      val userInfo = json.decodeFromString<UserInfoResponse>(response)
      val userId = userInfo.id ?: userInfo.sub ?: ""
      val username = userInfo.preferredUsername ?: userInfo.username ?: userId
      val displayName = userInfo.nickname ?: userInfo.globalName ?: userInfo.name ?: username

      DiscordAccount(
        id = userId,
        username = username,
        displayName = displayName,
        avatarUrl =
          userInfo.picture?.takeIf { it.isNotBlank() }
            ?: buildAvatarUrl(
              userId = userId,
              avatarHash = userInfo.avatar,
              discriminator = userInfo.discriminator,
            ),
      )
    }

  suspend fun clearSession(context: Context) {
    withContext(Dispatchers.IO) {
      // Read decrypted credentials for best-effort revocation, then clear local state first.
      // Logout must not depend on a network request succeeding or timing out.
      val credentials = runCatching { DiscordCredentialStore.read(context) }.getOrNull()
      context.dataStore.edit { prefs ->
        prefs.remove(DiscordTokenExpiresAtKey)
        prefs.remove(DiscordUsernameKey)
        prefs.remove(DiscordNameKey)
        prefs.remove(DiscordAvatarUrlKey)
      }
      DiscordCredentialStore.clearTokenMaterial(context)

      listOf(
          credentials?.refreshToken to "refresh_token",
          credentials?.accessToken to "access_token",
        )
        .forEach { (token, hint) ->
          if (!token.isNullOrBlank()) {
            runCatching {
              postForm(
                url = REVOKE_ENDPOINT,
                params =
                  mapOf(
                    "client_id" to BuildConfig.DISCORD_APPLICATION_ID,
                    "token" to token,
                    "token_type_hint" to hint,
                  ),
              )
            }
          }
        }
    }
  }

  private suspend fun refreshAccessToken(
    context: Context,
    refreshToken: String,
  ): Result<DiscordAuthSession> =
    withContext(Dispatchers.IO) {
      runCatching {
        val token =
          postForm(
              url = TOKEN_ENDPOINT,
              params =
                mapOf(
                  "client_id" to BuildConfig.DISCORD_APPLICATION_ID,
                  "grant_type" to "refresh_token",
                  "refresh_token" to refreshToken,
                ),
            )
            .let { json.decodeFromString<TokenResponse>(it) }

        val account = runCatching { fetchAccount(token.accessToken) }.getOrNull()
        val session = token.toAuthSession(account, fallbackRefreshToken = refreshToken)
        storeSession(context, session)
        session
      }
    }

  private fun exchangeAuthorizationCode(
    code: String,
    codeVerifier: String,
  ): TokenResponse =
    postForm(
        url = TOKEN_ENDPOINT,
        params =
          mapOf(
            "client_id" to BuildConfig.DISCORD_APPLICATION_ID,
            "grant_type" to "authorization_code",
            "code" to code,
            "redirect_uri" to redirectUri,
            "code_verifier" to codeVerifier,
          ),
      )
      .let { json.decodeFromString(it) }

  private suspend fun storeSession(
    context: Context,
    session: DiscordAuthSession,
  ) {
    // Credentials are encrypted with an Android Keystore AES-GCM key; only metadata remains
    // in ordinary Preferences DataStore.
    DiscordCredentialStore.write(context, session.accessToken, session.refreshToken)
    context.dataStore.edit { prefs ->
      prefs[DiscordTokenExpiresAtKey] = session.expiresAtMillis
      session.account?.let { account ->
        prefs[DiscordUsernameKey] = account.username
        prefs[DiscordNameKey] = account.displayName
        prefs[DiscordAvatarUrlKey] = account.avatarUrl.orEmpty()
      }
    }
  }

  private fun buildAvatarUrl(
    userId: String,
    avatarHash: String?,
    discriminator: String?,
  ): String? {
    if (userId.isBlank()) {
      return null
    }

    val normalizedAvatarHash = avatarHash?.takeIf { it.isNotBlank() }
    if (normalizedAvatarHash != null) {
      val extension = if (normalizedAvatarHash.startsWith("a_")) "gif" else "png"
      return "https://cdn.discordapp.com/avatars/$userId/$normalizedAvatarHash.$extension?size=256"
    }

    val defaultIndex =
      discriminator?.toIntOrNull()?.takeIf { it > 0 }?.rem(5)
        ?: userId.toLongOrNull()?.let { ((it shr 22) % 6L).toInt() }
        ?: 0
    return "https://cdn.discordapp.com/embed/avatars/$defaultIndex.png"
  }

  private fun TokenResponse.toAuthSession(
    account: DiscordAccount?,
    fallbackRefreshToken: String? = null,
  ): DiscordAuthSession {
    val expiresInMillis = expiresInSeconds.coerceAtLeast(0L) * 1000L
    val expiresAt =
      if (expiresInMillis > 0L) {
        System.currentTimeMillis() + expiresInMillis
      } else {
        0L
      }

    return DiscordAuthSession(
      accessToken = accessToken,
      refreshToken = refreshToken ?: fallbackRefreshToken,
      expiresAtMillis = expiresAt,
      account = account,
    )
  }

  private fun postForm(
    url: String,
    params: Map<String, String>,
  ): String {
    val body =
      params.entries.joinToString(separator = "&") { (key, value) ->
        "${key.urlEncode()}=${value.urlEncode()}"
      }
    val connection =
      (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = REQUEST_TIMEOUT_MS
        readTimeout = REQUEST_TIMEOUT_MS
        doOutput = true
        setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        setRequestProperty("Accept", "application/json")
      }

    connection.outputStream.use { output -> output.write(body.toByteArray(Charsets.UTF_8)) }

    return connection.readResponse()
  }

  private fun getJson(
    url: String,
    bearerToken: String,
  ): String {
    val connection =
      (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = REQUEST_TIMEOUT_MS
        readTimeout = REQUEST_TIMEOUT_MS
        setRequestProperty("Authorization", "Bearer $bearerToken")
        setRequestProperty("Accept", "application/json")
      }

    return connection.readResponse()
  }

  private fun HttpURLConnection.readResponse(): String {
    val status = responseCode
    val stream = if (status in 200..299) inputStream else errorStream
    val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    disconnect()

    if (status !in 200..299) {
      // Do not echo the response body: it may contain codes/tokens and ends up in UI/log messages.
      throw IOException("Discord OAuth request failed with HTTP $status")
    }

    return body
  }

  private fun randomUrlSafeString(byteCount: Int): String {
    val bytes = ByteArray(byteCount)
    secureRandom.nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
  }

  private fun sha256Base64Url(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.US_ASCII))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
  }

  private fun constantTimeEquals(a: String?, b: String): Boolean =
    a != null && MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))

  private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

  @Serializable
  private data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresInSeconds: Long = 0L,
  )

  @Serializable
  private data class UserInfoResponse(
    @SerialName("id") val id: String? = null,
    @SerialName("sub") val sub: String? = null,
    @SerialName("avatar") val avatar: String? = null,
    @SerialName("picture") val picture: String? = null,
    @SerialName("discriminator") val discriminator: String? = null,
    @SerialName("preferred_username") val preferredUsername: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("nickname") val nickname: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("global_name") val globalName: String? = null,
  )
}
