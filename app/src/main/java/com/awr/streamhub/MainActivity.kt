package com.awr.streamhub

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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

enum class Tab(val label: String) {
    Anime("Anime"), Movies("Movies"), Drama("K-Drama"), Search("Search"), Favorites("Favorites"), History("History")
}

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
    val videoUrl: String = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
)

data class LocalLibrary(
    val favorites: Set<String> = emptySet(),
    val history: List<String> = emptyList(),
    val progress: Map<String, Int> = emptyMap()
)

private val catalog = listOf(
    HubItem("anime-aurora", "Blade Aurora", "Anime", "9.7", "2026", listOf("Action", "Shounen", "Adventure"), "A young guardian discovers a forbidden aurora blade and enters a tournament that decides the fate of two worlds.", 24, "Jikan ready", Coral, Color(0xFF7C5CFF)),
    HubItem("anime-classroom", "Starlit Classroom", "Anime", "9.0", "2025", listOf("School", "Fantasy", "Drama"), "A classroom appears only after midnight, where students learn from future versions of themselves.", 12, "Jikan ready", Accent, Cyan),
    HubItem("anime-zero", "Zero Kingdom", "Anime", "9.2", "2026", listOf("Isekai", "Magic", "War"), "A strategist wakes inside a kingdom that resets after every defeat and must win before memory fades.", 25, "Jikan ready", Color(0xFF8E6BFF), Color(0xFFFF904D)),
    HubItem("movie-crimson", "Crimson Horizon", "Movie", "9.1", "2026", listOf("Action", "Sci-Fi", "Thriller"), "A betrayed pilot crosses a burning skyline to stop a global satellite war.", 1, "Consumet route", Coral, Color(0xFFFF9F43)),
    HubItem("movie-signal", "North Signal", "Movie", "8.6", "2024", listOf("Survival", "Mystery"), "A frozen signal brings a rescue team to a place that should not exist.", 1, "Consumet route", Cyan, Color(0xFF355CFF)),
    HubItem("movie-crown", "Silent Crown", "Movie", "8.8", "2025", listOf("Drama", "Mystery"), "A royal secret turns into a worldwide chase after a journalist finds a missing archive.", 1, "Consumet route", Color(0xFFB778FF), Green),
    HubItem("drama-seoul", "Neon Seoul", "K-Drama", "9.4", "2026", listOf("Romance", "Crime", "Thriller"), "A hacker and a prosecutor uncover a city built on erased memories.", 16, "Consumet route", Cyan, Color(0xFF8F65FF)),
    HubItem("drama-moon", "Moon Contract", "K-Drama", "8.9", "2025", listOf("Fantasy", "Romance"), "A mysterious contract links two souls across time and rewrites one night every full moon.", 12, "Consumet route", Color(0xFFFF9A4B), Accent)
)

@Composable
fun AwrStreamHubApp() {
    val context = LocalContext.current
    val library by remember {
        context.localStore.data.map { prefs ->
            LocalLibrary(
                favorites = prefs[favoritesKey].decodeSet(),
                history = prefs[historyKey].decodeList(),
                progress = prefs[progressKey].decodeProgress()
            )
        }
    }.collectAsState(initial = LocalLibrary())
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(Tab.Anime) }
    var screen by remember { mutableStateOf(Screen.Home) }
    var selected by remember { mutableStateOf(catalog.first()) }

    MaterialTheme(
        colorScheme = darkColorScheme(background = Bg, surface = Panel, primary = Accent, secondary = Coral, onBackground = Color.White, onSurface = Color.White, onPrimary = Color.Black)
    ) {
        Scaffold(
            containerColor = Bg,
            bottomBar = { if (screen != Screen.Player) BottomTabs(tab) { tab = it; screen = Screen.Home } }
        ) { padding ->
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF101522), Bg, Color.Black))).padding(padding)) {
                Crossfade(screen, label = "screen") { current ->
                    when (current) {
                        Screen.Home -> HomeRoute(tab, library) { selected = it; screen = Screen.Details }
                        Screen.Details -> DetailsScreen(
                            item = selected,
                            library = library,
                            onBack = { screen = Screen.Home },
                            onFavorite = { scope.launch { context.toggleFavorite(selected.id, library) } },
                            onWatch = { scope.launch { context.addHistory(selected.id, library) }; screen = Screen.Player }
                        )
                        Screen.Player -> PlayerScreen(
                            item = selected,
                            savedProgress = library.progress[selected.id] ?: 0,
                            onBack = { screen = Screen.Details },
                            onSaveProgress = { seconds -> scope.launch { context.saveProgress(selected.id, seconds, library) } },
                            onNext = { scope.launch { context.saveProgress(selected.id, 0, library); context.addHistory(selected.id, library) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeRoute(tab: Tab, library: LocalLibrary, onOpen: (HubItem) -> Unit) {
    when (tab) {
        Tab.Search -> SearchScreen(onOpen)
        Tab.Favorites -> GridScreen("Favorites", "Saved locally on this device", catalog.filter { it.id in library.favorites }, library, onOpen)
        Tab.History -> GridScreen("History", "Your latest watched titles", library.history.mapNotNull { id -> catalog.firstOrNull { it.id == id } }, library, onOpen)
        else -> HubHome(tab, library, onOpen)
    }
}

@Composable
private fun HubHome(tab: Tab, library: LocalLibrary, onOpen: (HubItem) -> Unit) {
    val primary = when (tab) {
        Tab.Anime -> catalog.filter { it.kind == "Anime" }
        Tab.Movies -> catalog.filter { it.kind == "Movie" }
        Tab.Drama -> catalog.filter { it.kind == "K-Drama" }
        else -> catalog
    }
    val continueItems = library.progress.entries.sortedByDescending { it.value }.mapNotNull { entry -> catalog.firstOrNull { it.id == entry.key } }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { AppHeader() }
        item { HeroBanner(primary.firstOrNull() ?: catalog.first(), library, onOpen) }
        if (continueItems.isNotEmpty()) {
            item { SectionTitle("Continue Watching", "Resume from your last local position") }
            item { MediaRail(continueItems, library, onOpen) }
        }
        item { SectionTitle("Trending", "HiTV-style top picks") }
        item { MediaRail(catalog.sortedByDescending { it.rating }, library, onOpen) }
        item { SectionTitle("Popular Anime", "Metadata path: Jikan") }
        item { MediaRail(catalog.filter { it.kind == "Anime" }, library, onOpen) }
        item { SectionTitle("Movies", "Streaming source path: Consumet") }
        item { MediaRail(catalog.filter { it.kind == "Movie" }, library, onOpen) }
        item { SectionTitle("K-Drama", "Episode-first browsing") }
        item { MediaRail(catalog.filter { it.kind == "K-Drama" }, library, onOpen) }
        item { SectionTitle("Recently Added", "Fresh local catalog entries") }
        item { MediaRail(catalog.reversed(), library, onOpen) }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun AppHeader() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("AWR Stream Hub", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
            Text("No account. Local favorites, history and progress.", color = Muted, fontSize = 12.sp)
        }
        Box(Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Accent, Coral))), contentAlignment = Alignment.Center) {
            Text("AWR", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun HeroBanner(item: HubItem, library: LocalLibrary, onOpen: (HubItem) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().height(310.dp).clickable { onOpen(item) }, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.Transparent), border = BorderStroke(1.dp, Color.White.copy(.08f))) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(item.accentA, item.accentB, Color.Black)))) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.9f)))))
            Column(Modifier.align(Alignment.BottomStart).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("Trending  |  " + item.kind, Accent, Color.Black)
                Text(item.title, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black, maxLines = 2)
                Text(item.story, color = Soft, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    PrimaryButton("Watch") { onOpen(item) }
                    Text("Rating " + item.rating + "  |  " + item.episodes + " episodes", color = Soft, fontSize = 12.sp)
                }
                ProgressLine(library.progress[item.id] ?: 0)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun MediaRail(items: List<HubItem>, library: LocalLibrary, onOpen: (HubItem) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(items) { item -> PosterCard(item, library, onOpen, Modifier.width(160.dp)) } }
}

@Composable
private fun PosterCard(item: HubItem, library: LocalLibrary, onOpen: (HubItem) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(252.dp).clickable { onOpen(item) }, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.Transparent), border = BorderStroke(1.dp, Color.White.copy(.08f))) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(item.accentA, item.accentB, Color.Black)))) {
            Text(item.kind.uppercase(), color = Color.White.copy(.32f), fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
            if (item.id in library.favorites) Pill("FAV", Accent, Color.Black, Modifier.align(Alignment.TopStart).padding(9.dp))
            Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(item.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.year + "  |  Rating " + item.rating, color = Soft, fontSize = 11.sp)
                ProgressLine(library.progress[item.id] ?: 0)
            }
        }
    }
}

@Composable
private fun GridScreen(title: String, subtitle: String, items: List<HubItem>, library: LocalLibrary, onOpen: (HubItem) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { AppHeader(); Spacer(Modifier.height(18.dp)); SectionTitle(title, subtitle) }
        if (items.isEmpty()) {
            item { EmptyState("Nothing here yet. Open a title and start watching or add it to favorites.") }
        } else {
            item {
                LazyVerticalGrid(columns = GridCells.Adaptive(142.dp), modifier = Modifier.height(720.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items) { PosterCard(it, library, onOpen, Modifier.fillMaxWidth()) }
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(onOpen: (HubItem) -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) {
        if (query.isBlank()) catalog else catalog.filter { it.title.contains(query, true) || it.kind.contains(query, true) || it.genres.any { genre -> genre.contains(query, true) } }
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { AppHeader() }
        item { OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search anime, movies, K-Drama") }, shape = RoundedCornerShape(8.dp)) }
        item { ApiNote() }
        items(results) { item -> RowResult(item, onOpen) }
    }
}

@Composable
private fun RowResult(item: HubItem, onOpen: (HubItem) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onOpen(item) }, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Panel), border = BorderStroke(1.dp, Color.White.copy(.08f))) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(78.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(listOf(item.accentA, item.accentB))))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(item.story, color = Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.kind + "  |  " + item.source, color = Accent, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun DetailsScreen(item: HubItem, library: LocalLibrary, onBack: () -> Unit, onFavorite: () -> Unit, onWatch: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onBack, shape = RoundedCornerShape(8.dp)) { Text("Back") }
                OutlinedButton(onClick = onFavorite, shape = RoundedCornerShape(8.dp)) { Text(if (item.id in library.favorites) "Saved" else "Favorite") }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().aspectRatio(.72f).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(listOf(item.accentA, item.accentB, Color.Black)))) {
                Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                    Pill(item.kind, Accent, Color.Black)
                    Spacer(Modifier.height(8.dp))
                    Text(item.title, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Text(item.year + "  |  Rating " + item.rating + "  |  " + item.episodes + " episodes", color = Soft, fontSize = 13.sp)
                }
            }
        }
        item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { item.genres.forEach { Pill(it, Panel2, Soft) }; Pill(item.source, Panel2, Accent) } }
        item { Text("Story", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(item.story, color = Soft, fontSize = 15.sp, lineHeight = 22.sp) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { PrimaryButton("Watch") { onWatch() }; OutlinedButton(onClick = onWatch, shape = RoundedCornerShape(8.dp)) { Text("Resume " + formatSeconds(library.progress[item.id] ?: 0)) } } }
        item { AiTranslationCard() }
        item { Text("Episodes", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); EpisodeList(item.episodes, onWatch) }
    }
}

@Composable
private fun EpisodeList(count: Int, onWatch: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..count).forEach { episode ->
            Card(Modifier.fillMaxWidth().clickable { onWatch() }, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Panel), border = BorderStroke(1.dp, Color.White.copy(.07f))) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Episode " + episode, color = Color.White, fontWeight = FontWeight.Bold); Text("Watch", color = Accent, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun PlayerScreen(item: HubItem, savedProgress: Int, onBack: () -> Unit, onSaveProgress: (Int) -> Unit, onNext: () -> Unit) {
    var subtitle by remember { mutableStateOf("Arabic") }
    var currentPosition by remember { mutableStateOf(savedProgress) }
    var videoView by remember { mutableStateOf<VideoView?>(null) }
    LaunchedEffect(videoView) {
        while (true) {
            videoView?.let { currentPosition = it.currentPosition / 1000 }
            delay(1000)
        }
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { onSaveProgress(currentPosition); onBack() }, shape = RoundedCornerShape(8.dp)) { Text("Back") }
            Text(item.title, color = Color.White, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black),
            factory = { context ->
                VideoView(context).apply {
                    videoView = this
                    setVideoURI(Uri.parse(item.videoUrl))
                    setMediaController(MediaController(context))
                    setOnPreparedListener { mediaPlayer -> if (savedProgress > 0) seekTo(savedProgress * 1000); mediaPlayer.start() }
                    setOnCompletionListener { onSaveProgress(0); onNext() }
                }
            }
        )
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text("Resume point: " + formatSeconds(savedProgress), color = Soft); ProgressLine(savedProgress) }
            item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Arabic", "English", "Off").forEach { option -> FilterChip(selected = subtitle == option, onClick = { subtitle = option }, label = { Text(option) }) } } }
            item {
                Card(colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Accent.copy(.25f))) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("AI Translation", color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp)
                        Text("Audio -> Grok ASR -> OpenAI Translation -> SRT -> in-app subtitle playback.", color = Muted, fontSize = 13.sp)
                        PrimaryButton("Generate AI SRT") { onSaveProgress(currentPosition) }
                    }
                }
            }
            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { PrimaryButton("Next Episode") { currentPosition = 0; onNext() }; OutlinedButton(onClick = { onSaveProgress(currentPosition) }, shape = RoundedCornerShape(8.dp)) { Text("Save Position") } } }
        }
    }
}

@Composable
private fun ApiNote() {
    Card(colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.White.copy(.08f))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("API paths ready", color = Color.White, fontWeight = FontWeight.Black)
            Text("Anime metadata: Jikan. Streaming/search routes: Consumet. Local state: DataStore.", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AiTranslationCard() {
    Card(colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Accent.copy(.25f))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("AI Translation", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text("Button flow: audio -> Grok ASR -> OpenAI Translation -> SRT -> video subtitle track.", color = Muted, fontSize = 13.sp)
            PrimaryButton("AI Translation") {}
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Card(colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.White.copy(.08f))) { Text(text, color = Muted, modifier = Modifier.padding(18.dp)) }
}

@Composable
private fun BottomTabs(active: Tab, onSelect: (Tab) -> Unit) {
    Surface(color = Color.Transparent, modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
        NavigationBar(containerColor = Color(0xEE080A10), tonalElevation = 0.dp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Color.White.copy(.08f), RoundedCornerShape(8.dp))) {
            Tab.entries.forEach { tab ->
                NavigationBarItem(selected = active == tab, onClick = { onSelect(tab) }, icon = { Text(tab.label.take(1), fontWeight = FontWeight.Black) }, label = { Text(tab.label, fontSize = 10.sp, maxLines = 1) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Black, selectedTextColor = Accent, indicatorColor = Accent, unselectedIconColor = Muted, unselectedTextColor = Muted))
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black)) { Text(text, fontWeight = FontWeight.Black) }
}

@Composable
private fun Pill(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Box(modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 5.dp)) { Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
}

@Composable
private fun ProgressLine(seconds: Int) {
    if (seconds <= 0) {
        Divider(color = Color.White.copy(.08f), thickness = 4.dp, modifier = Modifier.clip(CircleShape))
    } else {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(.12f))) {
            Box(Modifier.fillMaxWidth((seconds.coerceAtMost(1800) / 1800f).coerceAtLeast(.04f)).height(4.dp).background(Accent))
        }
    }
}

private suspend fun Context.toggleFavorite(id: String, library: LocalLibrary) {
    val next = library.favorites.toMutableSet().also { if (!it.add(id)) it.remove(id) }
    localStore.edit { prefs -> prefs[favoritesKey] = next.joinToString("|") }
}

private suspend fun Context.addHistory(id: String, library: LocalLibrary) {
    val next = (listOf(id) + library.history.filterNot { it == id }).take(40)
    localStore.edit { prefs -> prefs[historyKey] = next.joinToString("|") }
}

private suspend fun Context.saveProgress(id: String, seconds: Int, library: LocalLibrary) {
    val next = library.progress.toMutableMap()
    if (seconds <= 0) next.remove(id) else next[id] = seconds
    localStore.edit { prefs -> prefs[progressKey] = next.entries.joinToString("|") { it.key + ":" + it.value } }
}

private fun String?.decodeSet(): Set<String> = this?.split("|")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
private fun String?.decodeList(): List<String> = this?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
private fun String?.decodeProgress(): Map<String, Int> = this?.split("|")?.mapNotNull { entry -> val parts = entry.split(":"); if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null }?.filter { it.second > 0 }?.toMap() ?: emptyMap()

private fun formatSeconds(seconds: Int): String {
    val minutes = seconds / 60
    val remainder = seconds % 60
    return "%02d:%02d".format(minutes, remainder)
}
