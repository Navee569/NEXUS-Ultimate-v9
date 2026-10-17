package echo.music.iad1tya.discord

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Discord Rich Presence is intentionally disabled in this private fork.
 *
 * The previous implementation sent an OAuth access token to Discord Gateway IDENTIFY using
 * user-client-style properties. OAuth access tokens are not documented Gateway credentials,
 * and imitating the official client is not an acceptable authentication strategy. Keep this
 * adapter fail-closed until a supported Discord presence integration is implemented and tested.
 */
object DiscordSocialPresenceClient {
  private val mutex = Mutex()

  val isStarted: Boolean
    get() = false

  suspend fun updatePresence(
    accessToken: String,
    activity: DiscordPresenceActivity,
  ): Result<Unit> =
    Result.failure(
      UnsupportedOperationException(
        "Discord Rich Presence is disabled until a supported authentication method is available."
      )
    )

  /** Clear any local connection state; never attempt to authenticate to the Gateway. */
  suspend fun clearPresence(accessToken: String? = null): Result<Unit> = close()

  suspend fun close(): Result<Unit> =
    mutex.withLock {
      // No Gateway connection is created by this implementation.
      Result.success(Unit)
    }
}
