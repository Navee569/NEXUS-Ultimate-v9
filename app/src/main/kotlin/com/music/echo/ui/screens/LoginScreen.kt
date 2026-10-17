package echo.music.iad1tya.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.Uri
import org.json.JSONObject
import org.json.JSONTokener
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.music.innertube.YouTube
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.AccountChannelHandleKey
import echo.music.iad1tya.constants.AccountEmailKey
import echo.music.iad1tya.constants.AccountNameKey
import echo.music.iad1tya.constants.DataSyncIdKey
import echo.music.iad1tya.constants.InnerTubeCookieKey
import echo.music.iad1tya.constants.SavedAccountsKey
import echo.music.iad1tya.constants.VisitorDataKey
import echo.music.iad1tya.models.AccountData
import echo.music.iad1tya.ui.component.IconButton
import echo.music.iad1tya.ui.utils.backToMain
import echo.music.iad1tya.utils.rememberPreference
import echo.music.iad1tya.utils.reportException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun LoginScreen(
  navController: NavController,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var visitorData by rememberPreference(VisitorDataKey, "")
  var dataSyncId by rememberPreference(DataSyncIdKey, "")
  var innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
  var accountName by rememberPreference(AccountNameKey, "")
  var accountEmail by rememberPreference(AccountEmailKey, "")
  var accountChannelHandle by rememberPreference(AccountChannelHandleKey, "")
  var savedAccountsJson by rememberPreference(SavedAccountsKey, "[]")
  var hasCompletedLogin by remember { mutableStateOf(false) }

  var webView: WebView? = null

  AndroidView(
    modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current).fillMaxSize(),
    factory = { webViewContext ->
      WebView(webViewContext).apply {
        webViewClient =
          object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
              view: WebView,
              request: android.webkit.WebResourceRequest,
            ): Boolean {
              // Third-party subresources are needed by Google's sign-in/CAPTCHA pages. They
              // cannot reach a JavascriptInterface because this WebView has none. Restrict only
              // top-level navigation, where an arbitrary origin could impersonate the sign-in UI.
              if (!request.isForMainFrame) return false
              val uri = request.url
              val host = uri.host.orEmpty().lowercase()
              val trustedHost =
                uri.scheme == "https" &&
                  (host == "google.com" || host.endsWith(".google.com") ||
                    host == "youtube.com" || host.endsWith(".youtube.com"))
              return !trustedHost
            }

            override fun onPageFinished(view: WebView, url: String?) {
              val uri = runCatching { Uri.parse(url.orEmpty()) }.getOrNull() ?: return
              if (uri.scheme != "https" || uri.host != "music.youtube.com" || hasCompletedLogin) {
                return
              }

              // Retrieve page values via evaluateJavascript's result callback instead of a
              // JavascriptInterface. A JavascriptInterface is exposed to every frame, which
              // unnecessarily expands the attack surface of an authentication WebView.
              view.evaluateJavascript(
                """(function(){var c=(window.yt&&window.yt.config_)||{};return JSON.stringify({visitorData:c.VISITOR_DATA||"",dataSyncId:c.DATASYNC_ID||""});})()""",
              ) { rawResult ->
                if (hasCompletedLogin) return@evaluateJavascript
                val pageData =
                  runCatching {
                    val decoded = JSONTokener(rawResult ?: "null").nextValue() as? String
                      ?: return@runCatching null
                    JSONObject(decoded)
                  }.getOrNull() ?: return@evaluateJavascript

                visitorData = pageData.optString("visitorData")
                dataSyncId = pageData.optString("dataSyncId").substringBefore("||")
                innerTubeCookie = CookieManager.getInstance().getCookie(url).orEmpty()
                if (innerTubeCookie.isBlank()) return@evaluateJavascript
                hasCompletedLogin = true

                coroutineScope.launch {
                  delay(500)

                  YouTube.cookie = innerTubeCookie
                  YouTube.dataSyncId = dataSyncId
                  YouTube.visitorData = visitorData

                  Timber.d("Login: YouTube object initialized, validating...")

                  YouTube.accountInfo()
                    .onSuccess {
                      accountName = it.name
                      accountEmail = it.email.orEmpty()
                      accountChannelHandle = it.channelHandle.orEmpty()

                      val newAccount =
                        AccountData(
                          name = it.name,
                          email = it.email.orEmpty(),
                          channelHandle = it.channelHandle.orEmpty(),
                          cookie = innerTubeCookie,
                          visitorData = visitorData,
                          dataSyncId = dataSyncId,
                          avatarUrl = it.thumbnailUrl.orEmpty()
                        )
                      val accounts =
                        try {
                          Json.decodeFromString<List<AccountData>>(savedAccountsJson)
                        } catch (e: Exception) {
                          emptyList()
                        }.toMutableList()
                      accounts.removeAll { acc -> acc.name == newAccount.name }
                      accounts.add(newAccount)
                      savedAccountsJson = Json.encodeToString(accounts)

                      Timber.d("Login: Successfully logged in as ${it.name}, restarting app...")

                      view.apply {
                        stopLoading()
                        clearHistory()
                        clearCache(true)
                        clearFormData()
                      }

                      val intent =
                        context.packageManager.getLaunchIntentForPackage(context.packageName)
                      intent?.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                      )
                      context.startActivity(intent)
                      delay(500)
                      Runtime.getRuntime().exit(0)
                    }
                    .onFailure {
                      Timber.e(it, "Login: Authentication validation failed")
                      hasCompletedLogin = false
                      reportException(it)
                    }
                }
              }
            }
          }
        settings.apply {
          javaScriptEnabled = true
          allowFileAccess = false
          allowContentAccess = false
          mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
          setSupportZoom(true)
          builtInZoomControls = true
          displayZoomControls = false
        }
        webView = this

        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        loadUrl("https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com")
      }
    }
  )

  TopAppBar(
    title = { Text(stringResource(R.string.login)) },
    navigationIcon = {
      IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
      }
    }
  )

  BackHandler(enabled = webView?.canGoBack() == true) { webView?.goBack() }
}
