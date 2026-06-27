package com.awr.streamhub

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import coil.compose.AsyncImage
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Context.localStore by preferencesDataStore("awr_stream_hub_local")

private val Bg = Color(0xFF05060A)
private val Panel = Color(0xFF10131B)
private val Panel2 = Color(0xFF171B25)
private val Accent = Color(0xFFFFD15C)
private val Coral = Color(0xFFFF5C7A)
private val Cyan = Color(0xFF45D7FF)
private val Green = Color(0xFF47E6A1)
private val Muted = Color(0xFF9AA3B8)
private val Soft = Color(0xFFE8ECF4)

private val favoritesKey = stringPreferencesKey("favorites")
private val historyKey = stringPreferencesKey("history")
private val progressKey = stringPreferencesKey("progress")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AwrStreamHubApp() }
    }
}

enum class Tab(val label: String) { Anime("Anime"), Movies("Movies"), Drama("K-Drama"), Search("Search"), Favorites("Favorites"), History("History") }
enum class Screen { Home, Details, Player }

data class HubItem(
    val id: String,
    val title: String,
    val kind: String,
    val rating: String,
    val year: String,
    val genres: List<String>,
    val story: String,
    val episodes: Int,
    val source: String,
    val accentA: Color,
    val accentB: Color,
    val imageUrl: String? = null,
    val videoUrl: String = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
)

data class LocalLibrary(val favorites: Set<String> = emptySet(), val history: List<String> = emptyList(), val progress: Map<String, Int> = emptyMap())
data class ApiState(val loading: Boolean = false, val message: String = "Local fallback ready", val remoteItems: List<HubItem> = emptyList())

private val localCatalog = listOf(
    HubItem("anime-aurora", "Blade Aurora", "Anime", "9.7", "2026", listOf("Action", "Shounen", "Adventure"), "A young guardian discovers a forbidden aurora blade and enters a tournament that decides the fate of two worlds.", 24, "Local fallback", Coral, Color(0xFF7C5CFF)),
    HubItem("anime-classroom", "Starlit Classroom", "Anime", "9.0", "2025", listOf("School", "Fantasy", "Drama"), "A classroom appears only after midnight, where students learn from future versions of themselves.", 12, "Local fallback", Accent, Cyan),
    HubItem("anime-zero", "Zero Kingdom", "Anime", "9.2", "2026", listOf("Isekai", "Magic", "War"), "A strategist wakes inside a kingdom that resets after every defeat and must win before memory fades.", 25, "Local fallback", Color(0xFF8E6BFF), Color(0xFFFF904D)),
    HubItem("movie-crimson", "Crimson Horizon", "Movie", "9.1", "2026", listOf("Action", "Sci-Fi", "Thriller"), "A betrayed pilot crosses a burning skyline to stop a global satellite war.", 1, "Local fallback", Coral, Color(0xFFFF9F43)),
    HubItem("movie-signal", "North Signal", "Movie", "8.6", "2024", listOf("Survival", "Mystery"), "A frozen signal brings a rescue team to a place that should not exist.", 1, "Local fallback", Cyan, Color(0xFF355CFF)),
    HubItem("movie-crown", "Silent Crown", "Movie", "8.8", "2025", listOf("Drama", "Mystery"), "A royal secret turns into a worldwide chase after a journalist finds a missing archive.", 1, "Local fallback", Color(0xFFB778FF), Green),
    HubItem("drama-seoul", "Neon Seoul", "K-Drama", "9.4", "2026", listOf("Romance", "Crime", "Thriller"), "A hacker and a prosecutor uncover a city built on erased memories.", 16, "Local fallback", Cyan, Color(0xFF8F65FF)),
    HubItem("drama-moon", "Moon Contract", "K-Drama", "8.9", "2025", listOf("Fantasy", "Romance"), "A mysterious contract links two souls across time and rewrites one night every full moon.", 12, "Local fallback", Color(0xFFFF9A4B), Accent)
)

object HubApi {
    private const val jikanBase = "https://api.jikan.moe/v4"
    private const val consumetBase = "https://api.consumet.org"

    suspend fun home(tab: Tab): Result<List<HubItem>> = runCatching {
        when (tab) {
            Tab.Anime -> jikanTopAnime()
            Tab.Movies -> consumetMovieSearch("movie")
            Tab.Drama -> consumetDramaSearch("korean drama")
            else -> jikanTopAnime() + consumetMovieSearch("movie").take(6) + consumetDramaSearch("korean drama").take(6)
        }
    }

    suspend fun search(query: String): Result<List<HubItem>> = runCatching {
        if (query.isBlank()) emptyList() else jikanSearch(query) + consumetMovieSearch(query).take(8)
    }

    private suspend fun jikanTopAnime(): List<HubItem> = withContext(Dispatchers.IO) { parseJikan(get(jikanBase + "/top/anime?limit=18"), "Jikan top anime") }
    private suspend fun jikanSearch(query: String): List<HubItem> = withContext(Dispatchers.IO) { parseJikan(get(jikanBase + "/anime?q=" + query.url() + "&limit=18"), "Jikan search") }
    private suspend fun consumetMovieSearch(query: String): List<HubItem> = withContext(Dispatchers.IO) { parseConsumet(get(consumetBase + "/movies/flixhq/" + query.url()), "Movie", "Consumet FlixHQ") }
    private suspend fun consumetDramaSearch(query: String): List<HubItem> = withContext(Dispatchers.IO) { parseConsumet(get(consumetBase + "/movies/flixhq/" + query.url()), "K-Drama", "Consumet FlixHQ") }

    private fun get(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "AWR-Stream-Hub/1.0")
        }
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun parseJikan(raw: String, source: String): List<HubItem> {
        val data = JSONObject(raw).optJSONArray("data") ?: JSONArray()
        return (0 until data.length()).mapNotNull { index ->
            val item = data.optJSONObject(index) ?: return@mapNotNull null
            val title = item.optString("title_english").ifBlank { item.optString("title") }
            if (title.isBlank()) return@mapNotNull null
            val images = item.optJSONObject("images")?.optJSONObject("jpg")
            HubItem(
                id = "jikan-" + item.optInt("mal_id", index),
                title = title,
                kind = "Anime",
                rating = item.optDouble("score", 0.0).takeIf { it > 0.0 }?.toString() ?: "N/A",
                year = item.optInt("year", 0).takeIf { it > 0 }?.toString() ?: item.optString("status", "Anime"),
                genres = item.optJSONArray("genres").namesFromArray().ifEmpty { listOf("Anime") }.take(4),
                story = item.optString("synopsis", "Anime metadata loaded from Jikan."),
                episodes = item.optInt("episodes", 1).coerceAtLeast(1),
                source = source,
                accentA = Coral,
                accentB = Color(0xFF7C5CFF),
                imageUrl = images?.let { img -> img.optString("large_image_url").ifBlank { img.optString("image_url") } }
            )
        }
    }

    private fun parseConsumet(raw: String, kind: String, source: String): List<HubItem> {
        val root = JSONObject(raw)
        val data = root.optJSONArray("results") ?: root.optJSONArray("data") ?: JSONArray()
        return (0 until data.length()).mapNotNull { index ->
            val item = data.optJSONObject(index) ?: return@mapNotNull null
            val title = item.optString("title").ifBlank { item.optString("name") }
            if (title.isBlank()) return@mapNotNull null
            HubItem(
                id = "consumet-" + item.optString("id", kind + "-" + index),
                title = title,
                kind = kind,
                rating = item.optString("rating", item.optString("releaseDate", "N/A")),
                year = item.optString("releaseDate", item.optString("year", "New")),
                genres = listOf(kind, "Online"),
                story = item.optString("description", "Metadata loaded from Consumet. Connect a licensed stream source for playback."),
                episodes = 1,
                source = source,
                accentA = if (kind == "K-Drama") Cyan else Color(0xFFFF9A4B),
                accentB = if (kind == "K-Drama") Color(0xFF8F65FF) else Accent,
                imageUrl = item.optString("image").ifBlank { null }
            )
        }
    }
}

@Composable
fun AwrStreamHubApp() {
    val context = LocalContext.current
    val library by remember { context.localStore.data.map { prefs -> LocalLibrary(prefs[favoritesKey].decodeSet(), prefs[historyKey].decodeList(), prefs[progressKey].decodeProgress()) } }.collectAsState(initial = LocalLibrary())
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(Tab.Anime) }
    var screen by remember { mutableStateOf(Screen.Home) }
    var selected by remember { mutableStateOf(localCatalog.first()) }
    var apiState by remember { mutableStateOf(ApiState()) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(tab, refreshKey) {
        if (tab == Tab.Search || tab == Tab.Favorites || tab == Tab.History) return@LaunchedEffect
        apiState = apiState.copy(loading = true, message = "Loading APIs...")
        HubApi.home(tab).fold(
            onSuccess = { items -> apiState = ApiState(false, "Live APIs connected", items) },
            onFailure = { error -> apiState = ApiState(false, "API fallback: " + (error.message ?: "source unavailable"), emptyList()) }
        )
    }

    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Panel, primary = Accent, secondary = Coral, onBackground = Color.White, onSurface = Color.White, onPrimary = Color.Black)) {
        Scaffold(containerColor = Bg, bottomBar = { if (screen != Screen.Player) BottomTabs(tab) { tab = it; screen = Screen.Home } }) { padding ->
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF101522), Bg, Color.Black))).padding(padding)) {
                Crossfade(screen, label = "screen") { current ->
                    when (current) {
                        Screen.Home -> HomeRoute(tab, library, apiState, onRefresh = { refreshKey++ }) { selected = it; screen = Screen.Details }
                        Screen.Details -> DetailsScreen(selected, library, onBack = { screen = Screen.Home }, onFavorite = { scope.launch { context.toggleFavorite(selected.id, library) } }, onWatch = { scope.launch { context.addHistory(selected.id, library) }; screen = Screen.Player })
                        Screen.Player -> PlayerScreen(selected, library.progress[selected.id] ?: 0, onBack = { screen = Screen.Details }, onSaveProgress = { seconds -> scope.launch { context.saveProgress(selected.id, seconds, library) } }, onNext = { scope.launch { context.saveProgress(selected.id, 0, library); context.addHistory(selected.id, library) } })
                    }
                }
            }
        }
    }
}

private fun visibleCatalog(apiState: ApiState): List<HubItem> = (apiState.remoteItems + localCatalog).distinctBy { it.id }

@Composable
private fun HomeRoute(tab: Tab, library: LocalLibrary, apiState: ApiState, onRefresh: () -> Unit, onOpen: (HubItem) -> Unit) {
    val all = visibleCatalog(apiState)
    when (tab) {
        Tab.Search -> SearchScreen(onOpen)
        Tab.Favorites -> GridScreen("Favorites", "Saved locally on this device", all.filter { it.id in library.favorites }, library, onOpen)
        Tab.History -> GridScreen("History", "Your latest watched titles", library.history.mapNotNull { id -> all.firstOrNull { it.id == id } }, library, onOpen)
        else -> HubHome(tab, library, apiState, onRefresh, onOpen)
    }
}

@Composable
private fun HubHome(tab: Tab, library: LocalLibrary, apiState: ApiState, onRefresh: () -> Unit, onOpen: (HubItem) -> Unit) {
    val all = visibleCatalog(apiState)
    val categoryItems = when (tab) {
        Tab.Anime -> all.filter { it.kind == "Anime" }
        Tab.Movies -> all.filter { it.kind == "Movie" }
        Tab.Drama -> all.filter { it.kind == "K-Drama" }
        else -> all
    }
    val safeItems = if (categoryItems.isNotEmpty()) categoryItems else localCatalog.filter { fallback ->
        when (tab) {
            Tab.Anime -> fallback.kind == "Anime"
            Tab.Movies -> fallback.kind == "Movie"
            Tab.Drama -> fallback.kind == "K-Drama"
            else -> true
        }
    }
    val continueItems = library.progress.entries
        .sortedByDescending { it.value }
        .mapNotNull { entry -> safeItems.firstOrNull { item -> item.id == entry.key } }
    val screenName = when (tab) { Tab.Anime -> "Anime"; Tab.Movies -> "Movies"; Tab.Drama -> "K-Drama"; else -> "Catalog" }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { AppHeader(apiState, onRefresh) }
        item { HeroBanner(safeItems.first(), library, onOpen) }
        if (continueItems.isNotEmpty()) {
            item { SectionTitle("Continue Watching", "Only " + screenName + " items you already started") }
            item { MediaRail(continueItems, library, onOpen) }
        }
        item { SectionTitle("Trending " + screenName, "No mixed categories here") }
        item { MediaRail(safeItems.sortedByDescending { it.rating }, library, onOpen) }
        item { SectionTitle("Popular " + screenName, "Dedicated results for this tab") }
        item { MediaRail(safeItems.drop(1).ifEmpty { safeItems }, library, onOpen) }
        item { SectionTitle("Recently Added " + screenName, "Newest items from API plus local fallback") }
        item { MediaRail(safeItems.reversed(), library, onOpen) }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun AppHeader(apiState: ApiState = ApiState(), onRefresh: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text("AWR Stream Hub", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black); Text(if (apiState.loading) "Loading Jikan / Consumet..." else apiState.message, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        if (onRefresh != null) OutlinedButton(onClick = onRefresh, shape = RoundedCornerShape(8.dp)) { Text("Refresh") }
        Spacer(Modifier.width(8.dp)); Box(Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Accent, Coral))), contentAlignment = Alignment.Center) { Text("AWR", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun HeroBanner(item: HubItem, library: LocalLibrary, onOpen: (HubItem) -> Unit) {
    Card(Modifier.fillMaxWidth().height(336.dp).clickable { onOpen(item) }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.Transparent), border = BorderStroke(1.dp, Accent.copy(.28f))) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(item.accentA, item.accentB, Color.Black)))) {
            item.imageUrl?.let { AsyncImage(model = it, contentDescription = item.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.92f)))))
            Column(Modifier.align(Alignment.BottomStart).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Pill("Trending | " + item.kind, Accent, Color.Black); Text(item.title, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black, maxLines = 2); Text(item.story, color = Soft, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { PrimaryButton("Watch") { onOpen(item) }; Text("Rating " + item.rating + " | " + item.episodes + " episodes", color = Soft, fontSize = 12.sp) }; ProgressLine(library.progress[item.id] ?: 0) }
        }
    }
}

@Composable private fun SectionTitle(title: String, subtitle: String) { Column { Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, fontSize = 12.sp) } }
@Composable private fun MediaRail(items: List<HubItem>, library: LocalLibrary, onOpen: (HubItem) -> Unit) { LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(items) { PosterCard(it, library, onOpen, Modifier.width(160.dp)) } } }

@Composable
private fun PosterCard(item: HubItem, library: LocalLibrary, onOpen: (HubItem) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.height(268.dp).clickable { onOpen(item) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(Color.Transparent), border = BorderStroke(1.dp, Color.White.copy(.14f))) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(item.accentA, item.accentB, Color.Black)))) {
            item.imageUrl?.let { AsyncImage(model = it, contentDescription = item.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.78f)))))
            Text(item.kind.uppercase(), color = Color.White.copy(.65f), fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
            if (item.id in library.favorites) Pill("FAV", Accent, Color.Black, Modifier.align(Alignment.TopStart).padding(9.dp))
            Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) { Text(item.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(item.year + " | Rating " + item.rating, color = Soft, fontSize = 11.sp); ProgressLine(library.progress[item.id] ?: 0) }
        }
    }
}

@Composable
private fun GridScreen(title: String, subtitle: String, items: List<HubItem>, library: LocalLibrary, onOpen: (HubItem) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { item { AppHeader(); Spacer(Modifier.height(18.dp)); SectionTitle(title, subtitle) }; if (items.isEmpty()) item { EmptyState("Nothing here yet. Open a title and start watching or add it to favorites.") } else item { LazyVerticalGrid(columns = GridCells.Adaptive(142.dp), modifier = Modifier.height(720.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(items) { PosterCard(it, library, onOpen, Modifier.fillMaxWidth()) } } } }
}

@Composable
private fun SearchScreen(onOpen: (HubItem) -> Unit) {
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Search Jikan and Consumet") }
    var remote by remember { mutableStateOf<List<HubItem>>(emptyList()) }
    LaunchedEffect(query) {
        if (query.length < 2) { remote = emptyList(); message = "Type at least 2 letters"; return@LaunchedEffect }
        loading = true; delay(350)
        HubApi.search(query).fold(onSuccess = { remote = it; message = "Live search results" }, onFailure = { message = "API fallback: " + (it.message ?: "search unavailable"); remote = emptyList() })
        loading = false
    }
    val local = if (query.isBlank()) localCatalog else localCatalog.filter { it.title.contains(query, true) || it.kind.contains(query, true) || it.genres.any { genre -> genre.contains(query, true) } }
    val results = (remote + local).distinctBy { it.id }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { AppHeader(ApiState(loading, message, remote)) }; item { OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search anime, movies, K-Drama") }, shape = RoundedCornerShape(8.dp)) }; item { ApiNote() }; items(results) { RowResult(it, onOpen) } }
}

@Composable
private fun RowResult(item: HubItem, onOpen: (HubItem) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onOpen(item) }, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Panel), border = BorderStroke(1.dp, Color.White.copy(.08f))) { Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(78.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(listOf(item.accentA, item.accentB)))) { item.imageUrl?.let { AsyncImage(model = it, contentDescription = item.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(item.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black); Text(item.story, color = Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(item.kind + " | " + item.source, color = Accent, fontSize = 11.sp) } } }
}

@Composable
private fun DetailsScreen(item: HubItem, library: LocalLibrary, onBack: () -> Unit, onFavorite: () -> Unit, onWatch: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { OutlinedButton(onClick = onBack, shape = RoundedCornerShape(8.dp)) { Text("Back") }; OutlinedButton(onClick = onFavorite, shape = RoundedCornerShape(8.dp)) { Text(if (item.id in library.favorites) "Saved" else "Favorite") } } }; item { Box(Modifier.fillMaxWidth().aspectRatio(.72f).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(listOf(item.accentA, item.accentB, Color.Black)))) { item.imageUrl?.let { AsyncImage(model = it, contentDescription = item.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }; Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.9f))))); Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) { Pill(item.kind, Accent, Color.Black); Spacer(Modifier.height(8.dp)); Text(item.title, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black); Text(item.year + " | Rating " + item.rating + " | " + item.episodes + " episodes", color = Soft, fontSize = 13.sp) } } }; item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { item.genres.forEach { Pill(it, Panel2, Soft) }; Pill(item.source, Panel2, Accent) } }; item { Text("Story", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(item.story, color = Soft, fontSize = 15.sp, lineHeight = 22.sp) }; item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { PrimaryButton("Watch") { onWatch() }; OutlinedButton(onClick = onWatch, shape = RoundedCornerShape(8.dp)) { Text("Resume " + formatSeconds(library.progress[item.id] ?: 0)) } } }; item { AiTranslationCard() }; item { Text("Episodes", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); EpisodeList(item.episodes, onWatch) } }
}

@Composable private fun EpisodeList(count: Int, onWatch: () -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { (1..count).forEach { episode -> Card(Modifier.fillMaxWidth().clickable { onWatch() }, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Panel), border = BorderStroke(1.dp, Color.White.copy(.07f))) { Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Episode " + episode, color = Color.White, fontWeight = FontWeight.Bold); Text("Watch", color = Accent, fontSize = 12.sp) } } } } }

@OptIn(UnstableApi::class)
@Composable
private fun PlayerScreen(item: HubItem, savedProgress: Int, onBack: () -> Unit, onSaveProgress: (Int) -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current
    var subtitle by remember { mutableStateOf("Arabic") }
    var currentPosition by remember { mutableStateOf(savedProgress) }
    var isReady by remember { mutableStateOf(false) }
    val player = remember(item.id) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(PlayerMediaItem.fromUri(item.videoUrl))
            prepare()
            if (savedProgress > 0) seekTo(savedProgress * 1000L)
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose {
            onSaveProgress((player.currentPosition / 1000L).toInt())
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            currentPosition = (player.currentPosition / 1000L).toInt()
            isReady = player.playbackState != 1
            delay(1000)
        }
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { onSaveProgress(currentPosition); onBack() }, shape = RoundedCornerShape(10.dp)) { Text("Back") }
            Text(item.title, color = Color.White, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext -> PlayerView(viewContext).apply { this.player = player; useController = true } },
                update = { it.player = player }
            )
            if (!isReady) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(.54f)), contentAlignment = Alignment.Center) {
                    Text("Loading player...", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text("Saved position: " + formatSeconds(savedProgress), color = Soft); ProgressLine(currentPosition) }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Arabic", "English", "Off").forEach { option -> FilterChip(selected = subtitle == option, onClick = { subtitle = option }, label = { Text(option) }) }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Accent.copy(.3f))) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("AI Translation", color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp)
                        Text("Audio -> Grok ASR -> OpenAI Translation -> SRT -> in-app subtitle playback.", color = Muted, fontSize = 13.sp)
                        PrimaryButton("Generate AI SRT") { onSaveProgress(currentPosition) }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton("Next Episode") { player.seekTo(0); currentPosition = 0; onSaveProgress(0); onNext() }
                    OutlinedButton(onClick = { onSaveProgress(currentPosition) }, shape = RoundedCornerShape(10.dp)) { Text("Save Position") }
                }
            }
        }
    }
}

@Composable private fun ApiNote() { Card(colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.White.copy(.08f))) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("API paths active", color = Color.White, fontWeight = FontWeight.Black); Text("Anime: Jikan v4. Movies/K-Drama: Consumet FlixHQ route. Local DataStore keeps favorites, history and progress.", color = Muted, fontSize = 12.sp) } } }
@Composable private fun AiTranslationCard() { Card(colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Accent.copy(.25f))) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("AI Translation", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black); Text("Button flow: audio -> Grok ASR -> OpenAI Translation -> SRT -> video subtitle track.", color = Muted, fontSize = 13.sp); PrimaryButton("AI Translation") {} } } }
@Composable private fun EmptyState(text: String) { Card(colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.White.copy(.08f))) { Text(text, color = Muted, modifier = Modifier.padding(18.dp)) } }
@Composable private fun BottomTabs(active: Tab, onSelect: (Tab) -> Unit) { Surface(color = Color.Transparent, modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) { NavigationBar(containerColor = Color(0xEE080A10), tonalElevation = 0.dp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Color.White.copy(.08f), RoundedCornerShape(8.dp))) { Tab.entries.forEach { tab -> NavigationBarItem(selected = active == tab, onClick = { onSelect(tab) }, icon = { Text(tab.label.take(1), fontWeight = FontWeight.Black) }, label = { Text(tab.label, fontSize = 10.sp, maxLines = 1) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Black, selectedTextColor = Accent, indicatorColor = Accent, unselectedIconColor = Muted, unselectedTextColor = Muted)) } } } }
@Composable private fun PrimaryButton(text: String, onClick: () -> Unit) { Button(onClick = onClick, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black)) { Text(text, fontWeight = FontWeight.Black) } }
@Composable private fun Pill(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier) { Box(modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 5.dp)) { Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) } }
@Composable private fun ProgressLine(seconds: Int) { if (seconds <= 0) Divider(color = Color.White.copy(.08f), thickness = 4.dp, modifier = Modifier.clip(CircleShape)) else Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(.12f))) { Box(Modifier.fillMaxWidth((seconds.coerceAtMost(1800) / 1800f).coerceAtLeast(.04f)).height(4.dp).background(Accent)) } }

private suspend fun Context.toggleFavorite(id: String, library: LocalLibrary) { val next = library.favorites.toMutableSet().also { if (!it.add(id)) it.remove(id) }; localStore.edit { prefs -> prefs[favoritesKey] = next.joinToString("|") } }
private suspend fun Context.addHistory(id: String, library: LocalLibrary) { val next = (listOf(id) + library.history.filterNot { it == id }).take(40); localStore.edit { prefs -> prefs[historyKey] = next.joinToString("|") } }
private suspend fun Context.saveProgress(id: String, seconds: Int, library: LocalLibrary) { val next = library.progress.toMutableMap(); if (seconds <= 0) next.remove(id) else next[id] = seconds; localStore.edit { prefs -> prefs[progressKey] = next.entries.joinToString("|") { it.key + ":" + it.value } } }
private fun String?.decodeSet(): Set<String> = this?.split("|")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
private fun String?.decodeList(): List<String> = this?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
private fun String?.decodeProgress(): Map<String, Int> = this?.split("|")?.mapNotNull { entry -> val parts = entry.split(":"); if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null }?.filter { it.second > 0 }?.toMap() ?: emptyMap()
private fun String.url(): String = URLEncoder.encode(this, "UTF-8")
private fun JSONArray?.namesFromArray(): List<String> = if (this == null) emptyList() else (0 until length()).mapNotNull { index -> optJSONObject(index)?.optString("name")?.takeIf { it.isNotBlank() } }
private fun formatSeconds(seconds: Int): String { val minutes = seconds / 60; val remainder = seconds % 60; return "%02d:%02d".format(minutes, remainder) }
