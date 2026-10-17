package echo.music.iad1tya.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.navigation.NavController
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import echo.music.iad1tya.utils.rememberPreference
import echo.music.iad1tya.LocalPlayerConnection
import echo.music.iad1tya.extensions.toMediaItem
import echo.music.iad1tya.playback.queues.ListQueue
import echo.music.iad1tya.constants.DisableScreenshotKey
import echo.music.iad1tya.constants.PauseListenHistoryKey
import echo.music.iad1tya.constants.PauseSearchHistoryKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private val NexusMemoriesKey = stringPreferencesKey("nexus_sonic_memories_v1")
private val NexusMoodCountsKey = stringPreferencesKey("nexus_mood_sessions_v1")
private val NexusAudioProfileKey = stringPreferencesKey("nexus_audio_profile_v1")
private val NexusDiscoveryPrefsKey = stringPreferencesKey("nexus_discovery_prefs_v1")
private val NexusPrivateNotesKey = stringPreferencesKey("nexus_private_notes_v1")
private val NexusAutoAudioProfileKey = booleanPreferencesKey("nexus_auto_audio_profile_v1")
private val NexusDeviceProfileMapKey = stringPreferencesKey("nexus_device_profile_map_v1")

private data class SonicMemory(
    val id: Long,
    val title: String,
    val note: String,
    val tag: String,
    val createdAt: Long,
)

private fun decodeMemories(raw: String): List<SonicMemory> = try {
    val array = JSONArray(raw)
    (0 until array.length()).mapNotNull { i ->
        val o = array.optJSONObject(i) ?: return@mapNotNull null
        SonicMemory(
            id = o.optLong("id"), title = o.optString("title"),
            note = o.optString("note"), tag = o.optString("tag"),
            createdAt = o.optLong("createdAt")
        )
    }
} catch (_: Exception) { emptyList() }

private fun encodeMemories(memories: List<SonicMemory>): String = JSONArray().apply {
    memories.forEach { m -> put(JSONObject().apply {
        put("id", m.id); put("title", m.title); put("note", m.note)
        put("tag", m.tag); put("createdAt", m.createdAt)
    }) }
}.toString()

private data class MoodPreset(val name: String, val description: String, val minutes: Int, val emoji: String)

private val moodPresets = listOf(
    MoodPreset("Deep Focus", "Low-distraction listening for focused work", 25, "🧠"),
    MoodPreset("Beast Mode", "High-energy workout session", 45, "⚡"),
    MoodPreset("Unwind", "A softer landing after a long day", 20, "🌙"),
    MoodPreset("Night Drive", "Atmospheric listening; keep attention on the road", 30, "🌌"),
    MoodPreset("Discovery", "Explore something outside your usual rotation", 30, "🧭"),
    MoodPreset("Power Nap", "A short, quiet break", 15, "☁️"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusStudioScreen(navController: NavController) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val coroutineScope = rememberCoroutineScope()
    val (rawMemories, setRawMemories) = rememberPreference(NexusMemoriesKey, "[]")
    val (rawMoodCounts, setRawMoodCounts) = rememberPreference(NexusMoodCountsKey, "{}")
    val (pauseListenHistory, setPauseListenHistory) = rememberPreference(PauseListenHistoryKey, false)
    val (pauseSearchHistory, setPauseSearchHistory) = rememberPreference(PauseSearchHistoryKey, false)
    val (disableScreenshots, setDisableScreenshots) = rememberPreference(DisableScreenshotKey, false)
    val memories = remember(rawMemories) { decodeMemories(rawMemories) }
    val moodCounts = remember(rawMoodCounts) { try { JSONObject(rawMoodCounts) } catch (_: Exception) { JSONObject() } }

    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("Personal") }
    var memorySearch by remember { mutableStateOf("") }
    var selectedMemoryTag by remember { mutableStateOf("All") }
    var selectedMood by remember { mutableStateOf(moodPresets.first()) }
    var selectedMinutes by remember { mutableIntStateOf(selectedMood.minutes) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var sessionTotal by remember { mutableIntStateOf(0) }
    var sessionRunning by remember { mutableStateOf(false) }
    var showDeleteAll by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("Discover") }
    val (savedAudioProfile, setSavedAudioProfile) = rememberPreference(NexusAudioProfileKey, "Studio")
    val (discoveryPrefs, setDiscoveryPrefs) = rememberPreference(NexusDiscoveryPrefsKey, "Instrumental|Any language|Balanced|30")
    val (privateNote, setPrivateNote) = rememberPreference(NexusPrivateNotesKey, "")
    val (autoAudioProfile, setAutoAudioProfile) = rememberPreference(NexusAutoAudioProfileKey, true)
    val (deviceProfileMapRaw, setDeviceProfileMapRaw) = rememberPreference(
        NexusDeviceProfileMapKey,
        "{\"bluetooth\":\"Vocal Clarity\",\"wired\":\"Studio\",\"usb\":\"Studio\",\"speaker\":\"Night Listening\",\"other\":\"Studio\"}"
    )
    var currentOutputLabel by remember { mutableStateOf("Detecting audio output…") }
    var currentOutputKey by remember { mutableStateOf("other") }
    var networkSummary by remember { mutableStateOf("Not checked yet") }
    var networkCheckedAt by remember { mutableStateOf("") }
    var discoveryStyle by remember { mutableStateOf(discoveryPrefs.split("|").getOrElse(2) { "Balanced" }) }
    var discoveryVocals by remember { mutableStateOf(discoveryPrefs.split("|").getOrElse(0) { "Instrumental" }) }
    var discoveryLanguage by remember { mutableStateOf(discoveryPrefs.split("|").getOrElse(1) { "Any language" }) }
    var discoveryDuration by remember { mutableStateOf(discoveryPrefs.split("|").getOrElse(3) { "30" }) }
    var generatedSongs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var isBuildingMix by remember { mutableStateOf(false) }
    var discoveryStatus by remember { mutableStateOf("Your mix is built on demand; tracks are fetched from the selected music service.") }

    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    DisposableEffect(audioManager, autoAudioProfile, deviceProfileMapRaw) {
        fun refreshOutput() {
            val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
            val chosen = outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLE_HEADSET || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
                ?: outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET }
                ?: outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_USB_HEADSET || it.type == AudioDeviceInfo.TYPE_USB_DEVICE }
                ?: outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                ?: outputs.firstOrNull()
            val keyAndLabel = when (chosen?.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET -> "bluetooth" to "Bluetooth audio${chosen?.productName?.toString()?.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty()}"
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "wired" to "Wired headphones${chosen?.productName?.toString()?.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty()}"
                AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> "usb" to "USB audio${chosen?.productName?.toString()?.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty()}"
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "speaker" to "Phone speaker"
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "speaker" to "Phone earpiece"
                null -> "other" to "No output device detected"
                else -> "other" to (chosen?.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Other audio output")
            }
            currentOutputKey = keyAndLabel.first
            currentOutputLabel = keyAndLabel.second
            if (autoAudioProfile) {
                val mappings = try { JSONObject(deviceProfileMapRaw) } catch (_: Exception) { JSONObject() }
                val mapped = mappings.optString(keyAndLabel.first, "Studio")
                if (mapped.isNotBlank() && mapped != savedAudioProfile) setSavedAudioProfile(mapped)
            }
        }
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = refreshOutput()
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = refreshOutput()
        }
        audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        refreshOutput()
        onDispose { audioManager.unregisterAudioDeviceCallback(callback) }
    }

    fun refreshNetworkSummary() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        networkSummary = when {
            caps == null -> "No active network"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Connected · Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Connected · Mobile data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Connected · Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "Connected · VPN transport"
            else -> "Connected · Other transport"
        } + if (caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true) " · Internet validated" else " · Internet not verified"
        networkCheckedAt = DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date())
    }
    LaunchedEffect(Unit) { refreshNetworkSummary() }

    LaunchedEffect(sessionRunning, secondsLeft) {
        if (sessionRunning && secondsLeft > 0) {
            delay(1_000)
            secondsLeft -= 1
        } else if (sessionRunning && secondsLeft <= 0) {
            sessionRunning = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NEXUS Studio") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val exportText = memories.joinToString("\n\n") {
                            "${it.title} [${it.tag}]\n${it.note}\n${DateFormat.getDateTimeInstance().format(Date(it.createdAt))}"
                        }.ifBlank { "No Sonic Memories saved." }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, "NEXUS Sonic Memories")
                            putExtra(Intent.EXTRA_TEXT, exportText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Export memories"))
                    }) { Icon(Icons.Default.Share, contentDescription = "Export memories") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Your private music command centre", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Sessions, music memories and privacy-first personal tools. Notes and session counts are stored locally in app preferences; no NEXUS account is required.", style = MaterialTheme.typography.bodyMedium)
                        val totalSessions = (0 until moodCounts.length()).sumOf { moodCounts.optInt(moodCounts.names()?.optString(it) ?: "", 0) }
                        Text("Your snapshot · ${memories.size} memories · $totalSessions sessions", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Discover", "Sessions", "Constellation", "Memories", "Audio", "Network", "Privacy").forEach { tab ->
                                FilterChip(selected = selectedTab == tab, onClick = { selectedTab = tab }, label = { Text(tab) })
                            }
                        }
                    }
                }
            }

            if (selectedTab == "Discover") {
                item {
                    Text("NEXUS Smart Discovery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Build a search tailored to your current intent. Preferences stay on this device; when you launch a search, the query is sent to the selected music service.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    Card {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("What should it feel like?", style = MaterialTheme.typography.titleMedium)
                            Text("Energy / atmosphere")
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Calm", "Balanced", "High energy", "Dark / cinematic", "Dreamy", "Upbeat").forEach { value ->
                                    FilterChip(selected = discoveryStyle == value, onClick = { discoveryStyle = value }, label = { Text(value) })
                                }
                            }
                            Text("Vocal style")
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Instrumental", "Vocals", "Any").forEach { value ->
                                    FilterChip(selected = discoveryVocals == value, onClick = { discoveryVocals = value }, label = { Text(value) })
                                }
                            }
                            Text("Language")
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Any language", "English", "Hindi", "Kannada", "Korean", "Japanese", "Tamil").forEach { value ->
                                    FilterChip(selected = discoveryLanguage == value, onClick = { discoveryLanguage = value }, label = { Text(value) })
                                }
                            }
                            Text("Session length")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("15", "30", "45", "60", "90").forEach { value ->
                                    FilterChip(selected = discoveryDuration == value, onClick = { discoveryDuration = value }, label = { Text("${value} min") })
                                }
                            }
                            val query = buildString {
                                append(when (discoveryStyle) {
                                    "Calm" -> "calm relaxing"
                                    "High energy" -> "high energy workout"
                                    "Dark / cinematic" -> "dark cinematic soundtrack"
                                    "Dreamy" -> "dreamy atmospheric"
                                    "Upbeat" -> "upbeat feel good"
                                    else -> "balanced focus"
                                })
                                append(if (discoveryVocals == "Instrumental") " instrumental" else if (discoveryVocals == "Vocals") " vocal songs" else " music")
                                if (discoveryLanguage != "Any language") append(" ${discoveryLanguage} music")
                                append(" ${discoveryDuration} minute mix")
                            }
                            Button(onClick = {
                                setDiscoveryPrefs("$discoveryVocals|$discoveryLanguage|$discoveryStyle|$discoveryDuration")
                                navController.navigate("search/${Uri.encode(query)}")
                            }, modifier = Modifier.fillMaxWidth()) { Text("Find my mix") }
                            Button(
                                enabled = !isBuildingMix,
                                onClick = {
                                    setDiscoveryPrefs("$discoveryVocals|$discoveryLanguage|$discoveryStyle|$discoveryDuration")
                                    isBuildingMix = true
                                    generatedSongs = emptyList()
                                    discoveryStatus = "Searching the music catalogue…"
                                    coroutineScope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            runCatching {
                                                val first = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                                                    .getOrThrow().items.filterIsInstance<SongItem>()
                                                val second = if (first.size < 8) {
                                                    val alternate = listOf(
                                                        when (discoveryStyle) {
                                                            "Calm" -> "peaceful ambient music"
                                                            "High energy" -> "energetic workout songs"
                                                            "Dark / cinematic" -> "epic dark soundtrack"
                                                            "Dreamy" -> "dream pop atmospheric songs"
                                                            "Upbeat" -> "happy upbeat songs"
                                                            else -> "focus background music"
                                                        },
                                                        if (discoveryVocals == "Instrumental") "instrumental" else if (discoveryVocals == "Vocals") "with vocals" else "music",
                                                        if (discoveryLanguage != "Any language") discoveryLanguage else ""
                                                    ).filter { it.isNotBlank() }.joinToString(" ")
                                                    YouTube.search(alternate, YouTube.SearchFilter.FILTER_SONG)
                                                        .getOrNull()?.items.orEmpty().filterIsInstance<SongItem>()
                                                } else emptyList()
                                                (first + second).distinctBy { it.id }
                                                    .filter { it.id.isNotBlank() }
                                                    .take(30)
                                            }
                                        }
                                        isBuildingMix = false
                                        result.onSuccess { songs ->
                                            generatedSongs = songs
                                            discoveryStatus = if (songs.isEmpty()) {
                                                "No tracks were returned. Try another mood or use Find my mix to search manually."
                                            } else {
                                                "Built a ${songs.size}-track mix. Choose Play mix or Add to queue below."
                                            }
                                        }.onFailure { error ->
                                            discoveryStatus = "Couldn't build the mix: ${error.message ?: "music service unavailable"}"
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isBuildingMix) "Building mix…" else "Build a playable mix")
                            }
                            if (generatedSongs.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        enabled = playerConnection != null,
                                        onClick = {
                                            val items = generatedSongs.map { it.toMediaItem() }
                                            playerConnection?.playQueue(ListQueue(title = "NEXUS · $discoveryStyle", items = items))
                                            discoveryStatus = "Playing your ${generatedSongs.size}-track NEXUS mix."
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Play mix") }
                                    Button(
                                        enabled = playerConnection != null,
                                        onClick = {
                                            playerConnection?.addToQueue(generatedSongs.map { it.toMediaItem() })
                                            discoveryStatus = "Added ${generatedSongs.size} tracks to your current queue."
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Add to queue") }
                                }
                                Text("Preview: ${generatedSongs.take(5).joinToString { it.title }}${if (generatedSongs.size > 5) "…" else ""}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(discoveryStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Search preview: $query", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    Card {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Offline-first recommendation lab", style = MaterialTheme.typography.titleMedium)
                            Text("The app already includes a recommendation engine and Music Flow. Use Music Flow for continuous related-track discovery; this panel creates a controllable search recipe without sending your profile to a separate NEXUS server.")
                            Button(onClick = { navController.navigate("music_flow") }, modifier = Modifier.fillMaxWidth()) { Text("Open Music Flow") }
                        }
                    }
                }
            }

            if (selectedTab == "Constellation") {
                item {
                    Text("Music Constellation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("A private visual map of your Sonic Memories. Nodes represent saved moments; connections are a visual grouping, not an AI claim about musical similarity.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    Card {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (memories.isEmpty()) {
                                Text("Your constellation is waiting for its first star. Add memories in Sonic Memory to populate it.")
                            } else {
                                val nodes = memories.take(24)
                                val primaryNodeColor = MaterialTheme.colorScheme.primary
                                val tertiaryNodeColor = MaterialTheme.colorScheme.tertiary
                                val secondaryNodeColor = MaterialTheme.colorScheme.secondary
                                Canvas(modifier = Modifier.fillMaxWidth().height(190.dp)) {
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    val radius = (size.minDimension * 0.36f).coerceAtLeast(24f)
                                    val points = nodes.indices.map { i ->
                                        val angle = (2.0 * Math.PI * i / nodes.size) - Math.PI / 2.0
                                        Offset(cx + radius * kotlin.math.cos(angle).toFloat(), cy + radius * kotlin.math.sin(angle).toFloat())
                                    }
                                    points.forEachIndexed { i, point ->
                                        val next = points[(i + 1) % points.size]
                                        drawLine(primaryNodeColor.copy(alpha = 0.30f), point, next, strokeWidth = 2f, cap = StrokeCap.Round)
                                        if (nodes.size > 3 && i % 2 == 0) drawLine(tertiaryNodeColor.copy(alpha = 0.18f), point, points[(i + 2) % points.size], strokeWidth = 1f)
                                    }
                                    points.forEachIndexed { i, point ->
                                        drawCircle(color = if (i % 3 == 0) tertiaryNodeColor else primaryNodeColor, radius = if (i == 0) 8f else 5f, center = point)
                                        drawCircle(color = primaryNodeColor.copy(alpha = 0.12f), radius = 12f, center = point, style = Stroke(width = 2f))
                                    }
                                    drawCircle(color = secondaryNodeColor, radius = 6f, center = Offset(cx, cy))
                                }
                                nodes.forEachIndexed { index, memory ->
                                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("✦", color = MaterialTheme.colorScheme.primary)
                                        Column {
                                            Text(memory.title, fontWeight = FontWeight.SemiBold)
                                            Text("${memory.tag} · ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(memory.createdAt))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                if (memories.size > 24) Text("Showing 24 of ${memories.size} memories.", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(onClick = { selectedTab = "Memories" }, modifier = Modifier.fillMaxWidth()) { Text("Add a star") }
                        }
                    }
                }
            }

            if (selectedTab == "Audio") {
                item {
                    Text("Adaptive Audio Profiles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("NEXUS detects the active output route and can automatically select a saved profile for Bluetooth, wired, USB, and phone-speaker routes. Applying actual EQ bands still depends on the existing playback/equalizer engine.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    Card {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Current output", fontWeight = FontWeight.SemiBold)
                            Text(currentOutputLabel)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text("Auto-select profile", fontWeight = FontWeight.SemiBold)
                                    Text("Choose a profile when the output device changes.", style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(checked = autoAudioProfile, onCheckedChange = setAutoAudioProfile)
                            }
                            val mappings = try { JSONObject(deviceProfileMapRaw) } catch (_: Exception) { JSONObject() }
                            Text("Profile for this device type", style = MaterialTheme.typography.labelLarge)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Studio", "Vocal Clarity", "Bass Impact", "Night Listening", "Cinema", "Podcast", "Custom").forEach { mappedProfile ->
                                    FilterChip(
                                        selected = mappings.optString(currentOutputKey, "Studio") == mappedProfile,
                                        onClick = {
                                            mappings.put(currentOutputKey, mappedProfile)
                                            setDeviceProfileMapRaw(mappings.toString())
                                            setSavedAudioProfile(mappedProfile)
                                        },
                                        label = { Text(mappedProfile) }
                                    )
                                }
                            }
                        }
                    }
                }
                items(listOf("Studio", "Vocal Clarity", "Bass Impact", "Night Listening", "Cinema", "Podcast", "Custom")) { profile ->
                    Card(onClick = { setSavedAudioProfile(profile) }) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(profile, fontWeight = FontWeight.SemiBold)
                                Text(when (profile) {
                                    "Studio" -> "Neutral reference; minimal processing"
                                    "Vocal Clarity" -> "Prioritize intelligible vocals"
                                    "Bass Impact" -> "More perceived low-end energy"
                                    "Night Listening" -> "Comfort-focused lower-volume listening"
                                    "Cinema" -> "Immersive, spacious playback"
                                    "Podcast" -> "Speech-focused listening"
                                    else -> "Your own equalizer configuration"
                                }, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(if (savedAudioProfile == profile) "Selected ✓" else "Select", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                item {
                    Card {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Current saved profile: $savedAudioProfile", fontWeight = FontWeight.SemiBold)
                            Button(onClick = { navController.navigate("settings/equalizer") }, modifier = Modifier.fillMaxWidth()) { Text("Open equalizer to apply") }
                        }
                    }
                }
                item {
                    Card {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Private listening note", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(value = privateNote, onValueChange = setPrivateNote, label = { Text("Note for this audio setup") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            Text("Stored locally as a preference. Avoid placing passwords or sensitive credentials here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (selectedTab == "Sessions") {
                item {
                    Text("MoodSync sessions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Choose a listening intention and run a focus timer. This timer does not automatically control the music queue yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(moodPresets) { preset ->
                    Card(onClick = { selectedMood = preset; selectedMinutes = preset.minutes; if (!sessionRunning) secondsLeft = 0 }) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(preset.emoji, style = MaterialTheme.typography.headlineMedium)
                            Column(Modifier.weight(1f)) {
                                Text(preset.name, fontWeight = FontWeight.SemiBold)
                                Text(preset.description, style = MaterialTheme.typography.bodySmall)
                            }
                            Text("${preset.minutes} min", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                item {
                    Card {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("${selectedMood.emoji} ${selectedMood.name}", style = MaterialTheme.typography.titleLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(15, 25, 45, 60, 90).forEach { minutes ->
                                    FilterChip(selected = selectedMinutes == minutes, onClick = { selectedMinutes = minutes }, label = { Text("${minutes}m") })
                                }
                            }
                            if (sessionTotal > 0) {
                                val progress = (secondsLeft.toFloat() / sessionTotal).coerceIn(0f, 1f)
                                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                                Text("${secondsLeft / 60}:${(secondsLeft % 60).toString().padStart(2, '0')} remaining", style = MaterialTheme.typography.titleMedium)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    if (sessionRunning) sessionRunning = false else {
                                        if (secondsLeft <= 0) {
                                            secondsLeft = selectedMinutes * 60
                                            sessionTotal = secondsLeft
                                            val updated = try { JSONObject(rawMoodCounts) } catch (_: Exception) { JSONObject() }
                                            updated.put(selectedMood.name, updated.optInt(selectedMood.name) + 1)
                                            setRawMoodCounts(updated.toString())
                                        }
                                        sessionRunning = true
                                    }
                                }) { Text(if (sessionRunning) "Pause session" else if (secondsLeft > 0) "Resume session" else "Start session") }
                                TextButton(onClick = { sessionRunning = false; secondsLeft = 0; sessionTotal = 0 }) { Text("Reset") }
                            }
                            Text("Sessions started: ${moodCounts.optInt(selectedMood.name)}", style = MaterialTheme.typography.bodySmall)
                            Button(onClick = {
                                val moodQuery = when (selectedMood.name) {
                                    "Deep Focus" -> "deep focus instrumental concentration music"
                                    "Beast Mode" -> "high energy workout music intense training"
                                    "Unwind" -> "calm relaxing evening chill music"
                                    "Night Drive" -> "atmospheric night drive synthwave music"
                                    "Discovery" -> "discover new alternative music hidden gems"
                                    "Power Nap" -> "soft ambient sleep meditation music"
                                    else -> "balanced music mix"
                                } + " ${selectedMinutes} minute mix"
                                setDiscoveryPrefs("${discoveryVocals}|${discoveryLanguage}|${discoveryStyle}|$selectedMinutes")
                                navController.navigate("search/${Uri.encode(moodQuery)}")
                            }, modifier = Modifier.fillMaxWidth()) { Text("Find a mix for this session") }
                        }
                    }
                }
                item {
                    Card {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Audio profile ideas", style = MaterialTheme.typography.titleMedium)
                            Text("Studio · Vocal Clarity · Bass Impact · Night Listening · Cinema", style = MaterialTheme.typography.bodyMedium)
                            Text("Device-aware selection is enabled above. Profiles are stored per output category; use the equalizer screen to apply or fine-tune the actual sound curve supported by your player and device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { navController.navigate("settings/equalizer") }) { Text("Open equalizer") }
                        }
                    }
                }
            }

            if (selectedTab == "Memories") {
                item {
                    Text("Sonic Memory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Attach a private note to a song, artist, moment or playlist. Memories remain on this device unless you explicitly export them.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    OutlinedTextField(
                        value = memorySearch,
                        onValueChange = { memorySearch = it },
                        label = { Text("Search your memories") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    val tags = listOf("All") + memories.map { it.tag }.distinct().sorted()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.forEach { memoryTag ->
                            FilterChip(
                                selected = selectedMemoryTag == memoryTag,
                                onClick = { selectedMemoryTag = memoryTag },
                                label = { Text(memoryTag) }
                            )
                        }
                    }
                }
                item {
                    Card {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Song / moment title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Your memory") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                            OutlinedTextField(value = tag, onValueChange = { tag = it }, label = { Text("Tag (e.g. Work, Travel, Personal)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Button(enabled = title.isNotBlank() && note.isNotBlank(), onClick = {
                                val memory = SonicMemory(System.currentTimeMillis(), title.trim(), note.trim(), tag.trim().ifBlank { "Personal" }, System.currentTimeMillis())
                                setRawMemories(encodeMemories(listOf(memory) + memories))
                                title = ""; note = ""; tag = "Personal"
                            }, modifier = Modifier.fillMaxWidth()) { Text("Save private memory") }
                        }
                    }
                }
                val filteredMemories = memories.filter { memory ->
                    (selectedMemoryTag == "All" || memory.tag == selectedMemoryTag) &&
                        (memorySearch.isBlank() || memory.title.contains(memorySearch, true) || memory.note.contains(memorySearch, true) || memory.tag.contains(memorySearch, true))
                }
                if (memories.isEmpty()) item { Text("No memories yet. Save the first one above.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                else if (filteredMemories.isEmpty()) item { Text("No memories match this search or tag.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(filteredMemories, key = { it.id }) { memory ->
                    Card {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(memory.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                IconButton(onClick = { setRawMemories(encodeMemories(memories.filterNot { it.id == memory.id })) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete memory")
                                }
                            }
                            Text(memory.note)
                            Text("${memory.tag} · ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault()).format(Date(memory.createdAt))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (memories.isNotEmpty()) item { TextButton(onClick = { showDeleteAll = true }) { Text("Delete all memories") } }
            }

            if (selectedTab == "Network") {
                item {
                    Text("Network Exposure Dashboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("This dashboard reports Android connectivity and documents known integration categories. It does not perform packet capture or claim to see every live request.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    Card {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Device connectivity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(networkSummary)
                            Text("Last checked: ${networkCheckedAt.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { refreshNetworkSummary() }, modifier = Modifier.fillMaxWidth()) { Text("Refresh connection status") }
                        }
                    }
                }
                val endpoints = listOf(
                    Triple("Music provider", "Online search, playback metadata and streams", "Required for online playback; the provider can observe requests and network metadata."),
                    Triple("GitHub raw content", "Remote configuration or project-hosted resources where used", "A fetched configuration can change the endpoint used by that feature."),
                    Triple("Discord", "Optional account authorization and Rich Presence", "Use only if needed; authorization shares data with Discord under its own policies."),
                    Triple("Last.fm", "Optional scrobbling / metadata integration", "May transmit listening or account-related data when enabled."),
                    Triple("Listen Together WebSocket", "Shared listening rooms and synchronization", "The server operator may observe data sent through the shared session."),
                    Triple("AI providers", "Optional AI-assisted features", "Prompts and supplied context may leave the device when those features are used.")
                )
                items(endpoints) { entry ->
                    Card {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(entry.first, fontWeight = FontWeight.SemiBold)
                            Text(entry.second, style = MaterialTheme.typography.bodyMedium)
                            Text(entry.third, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    Card {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("What this can and cannot prove", fontWeight = FontWeight.SemiBold)
                            Text("NEXUS does not currently capture all DNS lookups, TLS hosts, bytes per app request, or traffic initiated by SDKs. For a true runtime audit, use an Android VPN-based capture tool or a controlled test proxy and inspect requests during playback, login, search, and background operation. Never upload a capture containing tokens or personal data without redaction.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (selectedTab == "Privacy") {
                item {
                    Text("Privacy Fortress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("NEXUS uses a local-first approach for the custom tools on this screen. Online playback and integrations still contact the services they require.", style = MaterialTheme.typography.bodyMedium)
                }
                item {
                    Card {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text("Private Session", fontWeight = FontWeight.SemiBold)
                                    Text("Pause new listening and search history writes together.", style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(
                                    checked = pauseListenHistory && pauseSearchHistory,
                                    onCheckedChange = { enabled ->
                                        setPauseListenHistory(enabled)
                                        setPauseSearchHistory(enabled)
                                    }
                                )
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text("Screenshot protection", fontWeight = FontWeight.SemiBold)
                                    Text("Use the app's screenshot-blocking preference where supported.", style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(checked = disableScreenshots, onCheckedChange = setDisableScreenshots)
                            }
                        }
                    }
                }
                val privacyItems = listOf(
                    "Local Sonic Memory" to "Stored in app preferences; not synced by NEXUS itself.",
                    "No NEXUS account" to "The custom tools do not require a NEXUS account.",
                    "Private Session" to "Use Settings → Privacy to pause the app's existing listening and search history.",
                    "Network transparency" to "The Network tab shows connectivity and known integration categories; it is not a packet-level monitor and cannot prove which requests occurred.",
                    "External services" to "Online search and playback contact the configured music provider. Listen Together uses its configured WebSocket service. Optional integrations such as Discord or Last.fm can send data required by their features. Avoid integrations you do not need.",
                    "Device privacy" to "Use Android screen lock / Secure Folder if available. The app does not hide itself from Android or device administrators.",
                    "Credentials" to "The previous Discord WebView token scraping was removed. Review OAuth consent before authorizing external integrations."
                )
                items(privacyItems) { (titleText, description) ->
                    Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(titleText, fontWeight = FontWeight.SemiBold); Text(description, style = MaterialTheme.typography.bodySmall) } }
                }
                item { Button(onClick = { navController.navigate("settings/privacy") }, modifier = Modifier.fillMaxWidth()) { Text("Open privacy settings") } }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = { showDeleteAll = false },
            title = { Text("Delete all Sonic Memories?") },
            text = { Text("This permanently removes all memories stored by NEXUS on this device. Export them first if you want a copy.") },
            confirmButton = { TextButton(onClick = { setRawMemories("[]"); showDeleteAll = false }) { Text("Delete all") } },
            dismissButton = { TextButton(onClick = { showDeleteAll = false }) { Text("Cancel") } }
        )
    }
}
