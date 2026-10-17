package echo.music.iad1tya.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.nexus.designsystem.NexusInfoCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import echo.music.iad1tya.ui.component.IconButton
import echo.music.iad1tya.ui.component.Material3SettingsGroup
import echo.music.iad1tya.ui.component.Material3SettingsItem
import echo.music.iad1tya.ui.utils.backToMain
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import echo.music.iad1tya.R

private data class LicenseEntry(
  val name: String,
  val license: String,
  val url: String,
)

private val licenseEntries = listOf(
  LicenseEntry("Echo Music / NEXUS base", "GNU GPL-3.0-or-later", "https://github.com/EchoMusicApp/Echo-Music"),
  LicenseEntry("AndroidX / Jetpack Compose / Media3 / Room / WorkManager", "Apache-2.0", "https://github.com/androidx/androidx"),
  LicenseEntry("Kotlin / kotlinx.coroutines", "Apache-2.0", "https://github.com/JetBrains/kotlin"),
  LicenseEntry("Hilt / Dagger", "Apache-2.0", "https://github.com/google/dagger"),
  LicenseEntry("Coil", "Apache-2.0", "https://github.com/coil-kt/coil"),
  LicenseEntry("Ktor", "Apache-2.0", "https://github.com/ktorio/ktor"),
  LicenseEntry("Retrofit", "Apache-2.0", "https://github.com/square/retrofit"),
  LicenseEntry("OkHttp", "Apache-2.0", "https://github.com/square/okhttp"),
  LicenseEntry("Guava", "Apache-2.0", "https://github.com/google/guava"),
  LicenseEntry("Protocol Buffers", "BSD-3-Clause", "https://github.com/protocolbuffers/protobuf"),
  LicenseEntry("JUnit", "EPL-1.0", "https://github.com/junit-team/junit4"),
  LicenseEntry("Robolectric", "MIT", "https://github.com/robolectric/robolectric"),
  LicenseEntry("Timber", "Apache-2.0", "https://github.com/JakeWharton/timber"),
  LicenseEntry("Lottie", "Apache-2.0", "https://github.com/airbnb/lottie-android"),
  LicenseEntry("NewPipe Extractor", "GPL-3.0", "https://github.com/TeamNewPipe/NewPipeExtractor"),
  LicenseEntry("FFmpeg Kit (audio)", "See bundled FFmpeg build license notices", "https://github.com/arthenica/ffmpeg-kit"),
  LicenseEntry("uCrop", "Apache-2.0", "https://github.com/Yalantis/uCrop"),
  LicenseEntry("MaterialKolor", "Apache-2.0", "https://github.com/jordond/MaterialKolor"),
  LicenseEntry("Smooth Corner Compose", "See upstream repository license", "https://github.com/racra/smooth-corner-rect-android-compose"),
  LicenseEntry("Shimmer for Compose", "Apache-2.0", "https://github.com/valentinilk/compose-shimmer"),
)

@Composable
fun OpenSourceLicensesScreen(navController: NavController) {
  val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.surface,
    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    topBar = {
      TopAppBar(
        title = { Text("Open-source licenses", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = { navController.navigateUp() }, onLongClick = navController::backToMain) {
            Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
      )
    },
  ) { insets ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
      contentPadding = PaddingValues(top = insets.calculateTopPadding() + 12.dp, bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        NexusInfoCard(
          title = "NEXUS · Open-source licenses",
          body = "NEXUS is a GPL-3.0 fork. Echo Music is credited as upstream. Existing upstream copyright notices and the repository LICENSE are retained. This first screen lists principal declared dependencies; complete transitive notices still require a resolved dependency-license report.",
        )
      }
      items(licenseEntries, key = { it.name }) { entry ->
        Material3SettingsGroup(
          title = entry.name,
          items = listOf(
            Material3SettingsItem(
              icon = painterResource(R.drawable.github),
              title = { Text(entry.license) },
              description = { Text(entry.url) },
              onClick = { uriHandler.openUri(entry.url) },
            )
          ),
        )
      }
    }
  }
}
