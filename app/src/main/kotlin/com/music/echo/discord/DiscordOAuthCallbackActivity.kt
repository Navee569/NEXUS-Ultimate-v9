package echo.music.iad1tya.discord

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import echo.music.iad1tya.BuildConfig

class DiscordOAuthCallbackActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleIntent(intent)
    finish()
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIntent(intent)
    finish()
  }

  private fun handleIntent(intent: Intent?) {
    // Exported custom-scheme endpoint: ignore anything that is not our redirect scheme.
    intent?.data
      ?.takeIf { it.scheme == BuildConfig.DISCORD_REDIRECT_SCHEME && it.host == "authorize" }
      ?.let(DiscordAuthCoordinator::emit)
  }
}
