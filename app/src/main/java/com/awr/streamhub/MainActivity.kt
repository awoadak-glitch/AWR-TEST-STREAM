package com.awr.streamhub

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Page = Color(0xFFF7F7F8)
private val Ink = Color(0xFF18181B)
private val Muted = Color(0xFF77777F)
private val Brand = Color(0xFF8E173A)
private val BrandDark = Color(0xFF5B0D25)
private val Gold = Color(0xFFFFD400)
private val Live = Color(0xFFE11D48)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AnimeWitcherHybridApp() }
    }
}

private enum class Screen { Home, Details, Player }

@Composable
fun AnimeWitcherHybridApp() {
    val scheme = lightColorScheme(
        primary = Brand,
        secondary = Gold,
        background = Page,
        surface = Color.White,
        onPrimary = Color.White,
        onSurface = Ink,
        onBackground = Ink
    )
    MaterialTheme(colorScheme = scheme) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var tab by remember { mutableStateOf(MainTab.Anime) }
        var screen by remember { mutableStateOf(Screen.Home) }
        var sections by remember { mutableStateOf<List<HomeSection>>(emptyList()) }
        var loading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf("") }
        var selected by remember { mutableStateOf<MediaEntry?>(null) }
        var detail by remember { mutableStateOf<DetailBundle?>(null) }
        var detailLoading by remember { mutableStateOf(false) }
        var sources by remember { mutableStateOf<List<SourceEntry>>(emptyList()) }
        var sourceEpisode by remember { mutableStateOf<EpisodeEntry?>(null) }
        var sourceDialog by remember { mutableStateOf(false) }
        var sourceLoading by remember { mutableStateOf(false) }
        var playingUrl by remember { mutableStateOf("") }
        var playingTitle by remember { mutableStateOf("") }
        var searchOpen by remember { mutableStateOf(false) }
        var refreshKey by remember { mutableIntStateOf(0) }

        LaunchedEffect(tab, refreshKey) {
            loading = true
            error = ""
            sections = emptyList()
            RemoteApi.loadTab(tab).fold(
                onSuccess = { sections = it; loading = false },
                onFailure = { error = it.message ?: "تعذر جلب المحتوى"; loading = false }
            )
        }

        fun openItem(item: MediaEntry) {
            selected = item
            detail = null
            detailLoading = true
            screen = Screen.Details
            scope.launch {
                RemoteApi.loadDetails(item).fold(
                    onSuccess = { detail = it; selected = it.item; detailLoading = false },
                    onFailure = { detail = DetailBundle(item); detailLoading = false; error = it.message ?: "تعذر فتح التفاصيل" }
                )
            }
        }

        fun showSources(item: MediaEntry, ep: EpisodeEntry?) {
            sourceEpisode = ep
            sources = emptyList()
            sourceLoading = true
            sourceDialog = true
            scope.launch {
                RemoteApi.loadSources(item, ep).fold(
                    onSuccess = { sources = it; sourceLoading = false },
                    onFailure = { sourceLoading = false; error = it.message ?: "تعذر جلب السيرفرات" }
                )
            }
        }

        Scaffold(
            containerColor = Page,
            bottomBar = {
                if (screen == Screen.Home) BottomBar(tab) { newTab ->
                    tab = newTab
                    sections = emptyList()
                    error = ""
                }
            }
        ) { pad ->
            Box(Modifier.fillMaxSize().background(Page).padding(pad)) {
                Crossfade(screen, label = "screen") { s ->
                    when (s) {
                        Screen.Home -> HomeScreen(
                            tab = tab,
                            sections = sections,
                            loading = loading,
                            error = error,
                            onRefresh = { refreshKey++ },
                            onSearch = { searchOpen = true },
                            onOpen = ::openItem
                        )
                        Screen.Details -> DetailsScreen(
                            bundle = detail,
                            fallback = selected,
                            loading = detailLoading,
                            onBack = { screen = Screen.Home },
                            onPlay = { item, ep -> showSources(item, ep) }
                        )
                        Screen.Player -> PlayerScreen(playingUrl, playingTitle) {
                            playingUrl = ""
                            screen = Screen.Details
                        }
                    }
                }
            }
        }

        if (sourceDialog && selected != null) {
            SourcesDialog(
                item = selected!!,
                episode = sourceEpisode,
                loading = sourceLoading,
                sources = sources,
                onDismiss = { sourceDialog = false },
                onPlay = { src ->
                    sourceLoading = true
                    scope.launch {
                        RemoteApi.resolveSource(selected!!, src, sourceEpisode).fold(
                            onSuccess = { url ->
                                sourceLoading = false
                                sourceDialog = false
                                playingUrl = url
                                playingTitle = sourceEpisode?.title ?: selected!!.title
                                screen = Screen.Player
                            },
                            onFailure = { sourceLoading = false; error = it.message ?: "فشل تجهيز السيرفر" }
                        )
                    }
                },
                onDownload = { src ->
                    sourceLoading = true
                    scope.launch {
                        RemoteApi.resolveSource(selected!!, src, sourceEpisode).fold(
                            onSuccess = { url ->
                                sourceLoading = false
                                enqueueDownload(context, url, "${selected!!.title} ${sourceEpisode?.title.orEmpty()}")
                            },
                            onFailure = { sourceLoading = false; error = it.message ?: "فشل تجهيز التنزيل" }
                        )
                    }
                }
            )
        }

        if (searchOpen) {
            SearchDialog(
                tab = tab,
                onDismiss = { searchOpen = false },
                onResults = { result ->
                    sections = listOf(HomeSection("نتائج البحث", result))
                    searchOpen = false
                }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    tab: MainTab,
    sections: List<HomeSection>,
    loading: Boolean,
    error: String,
    onRefresh: () -> Unit,
    onSearch: () -> Unit,
    onOpen: (MediaEntry) -> Unit
) {
    val heroItems = sections.firstOrNull()?.items?.take(8).orEmpty()
    var heroIndex by remember(tab, heroItems.size) { mutableIntStateOf(0) }
    LaunchedEffect(tab, heroItems.size) {
        if (heroItems.size > 1) {
            while (true) {
                delay(4500)
                heroIndex = (heroIndex + 1) % heroItems.size
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item { TopHeader(tab.label, onSearch) }
        if (loading) item { LoadingBlock() }
        if (error.isNotBlank() && sections.isEmpty()) item { ErrorBlock(error, onRefresh) }
        if (heroItems.isNotEmpty()) item { HeroCard(heroItems[heroIndex], heroIndex, heroItems.size, onOpen) }
        items(sections.drop(if (heroItems.isNotEmpty()) 1 else 0)) { section -> SectionBlock(section, onOpen) }
        if (!loading && sections.isEmpty() && error.isBlank()) item { EmptyBlock("لا يوجد محتوى حالياً") }
    }
}

@Composable
private fun TopHeader(title: String, onSearch: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("☰", fontSize = 28.sp, color = Ink)
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ANIME WITCHER", color = Brand, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text(title, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.weight(1f))
        Surface(
            modifier = Modifier.size(42.dp).clickable(onClick = onSearch),
            shape = CircleShape,
            color = Color(0xFFF2F2F4)
        ) {
            Box(contentAlignment = Alignment.Center) { Text("⌕", fontSize = 25.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun HeroCard(item: MediaEntry, index: Int, count: Int, onOpen: (MediaEntry) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).aspectRatio(1.72f).clickable { onOpen(item) },
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = item.cover.ifBlank { item.poster },
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x18000000), Color(0xC0000000)))))
                Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                    if (item.kind == MediaKind.CHANNEL) Badge("LIVE", Live, Color.White)
                    Text(item.title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (item.subtitle.isNotBlank()) Text(item.subtitle, color = Color.White.copy(alpha = .82f), fontSize = 12.sp)
                }
            }
        }
        if (count > 1) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center) {
                repeat(count.coerceAtMost(8)) { i ->
                    Box(
                        Modifier.padding(horizontal = 3.dp)
                            .size(if (i == index) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (i == index) Brand else Color(0xFFD1D1D5))
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionBlock(section: HomeSection, onOpen: (MediaEntry) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("عرض المزيد", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text(section.title, color = Color(0xFF6A6A70), fontSize = 21.sp, fontWeight = FontWeight.Black)
        }
        LazyRow(
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(section.items) { item -> MediaCard(item, section.episodeStyle, onOpen) }
        }
    }
}

@Composable
private fun MediaCard(item: MediaEntry, episodeStyle: Boolean, onOpen: (MediaEntry) -> Unit) {
    val wide = item.kind == MediaKind.CHANNEL
    Column(Modifier.width(if (wide) 210.dp else 142.dp).clickable { onOpen(item) }) {
        Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(1.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(if (wide) 1.65f else .70f)) {
                AsyncImage(
                    model = item.poster.ifBlank { item.cover },
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (episodeStyle && item.subtitle.isNotBlank()) {
                    Surface(Modifier.align(Alignment.BottomEnd).padding(7.dp), color = Gold, shape = RoundedCornerShape(7.dp)) {
                        Text(item.subtitle, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                    }
                }
                if (item.kind == MediaKind.CHANNEL) {
                    Surface(Modifier.align(Alignment.TopStart).padding(7.dp), color = Live, shape = RoundedCornerShape(7.dp)) {
                        Text("● مباشر", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(item.title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val meta = item.subtitle.ifBlank { item.year }
        if (meta.isNotBlank()) Text(meta, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DetailsScreen(
    bundle: DetailBundle?,
    fallback: MediaEntry?,
    loading: Boolean,
    onBack: () -> Unit,
    onPlay: (MediaEntry, EpisodeEntry?) -> Unit
) {
    val item = bundle?.item ?: fallback
    if (item == null) {
        EmptyBlock("لا يوجد عمل محدد")
        return
    }
    LazyColumn(Modifier.fillMaxSize().background(Color.White), contentPadding = PaddingValues(bottom = 26.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(310.dp)) {
                AsyncImage(
                    model = item.cover.ifBlank { item.poster },
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33000000), Color.Transparent, Color.White))))
                Surface(
                    Modifier.padding(16.dp).size(44.dp).clickable(onClick = onBack),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = .92f)
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("←", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                }
                if (!loading && (item.kind == MediaKind.MOVIE || item.kind == MediaKind.CHANNEL)) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 2.dp).size(72.dp).clickable { onPlay(item, null) },
                        shape = RoundedCornerShape(23.dp),
                        color = Brand
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text("▶", color = Color.White, fontSize = 30.sp) }
                    }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Text(item.title, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (item.rating.isNotBlank()) Badge("★ ${item.rating}", Gold, Color.Black)
                    if (item.year.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Badge(item.year, Color(0xFFF0F0F2), Ink)
                    }
                    Spacer(Modifier.width(8.dp))
                    Badge(kindArabic(item.kind), Color(0xFFF0F0F2), Ink)
                }
                if (item.tags.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.End) {
                        item.tags.take(4).forEach { t ->
                            Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF3EDF0), modifier = Modifier.padding(start = 6.dp)) {
                                Text(t, color = BrandDark, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                            }
                        }
                    }
                }
                if (item.story.isNotBlank()) {
                    Spacer(Modifier.height(18.dp))
                    Text("القصة", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    Text(item.story, modifier = Modifier.fillMaxWidth().padding(top = 7.dp), textAlign = TextAlign.End, color = Color(0xFF55555B), fontSize = 14.sp, lineHeight = 22.sp)
                }
            }
        }
        if (loading) item { LoadingBlock() }
        if (!loading && bundle != null && bundle.episodes.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    Text("الحلقات", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }
            items(bundle.episodes.reversed()) { ep -> EpisodeRow(item, ep, onPlay) }
        }
        if (!loading && bundle != null && bundle.cast.isNotEmpty()) {
            item {
                Text("فريق العمل", modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), textAlign = TextAlign.End, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
                LazyRow(reverseLayout = true, contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(bundle.cast.take(12)) { actor ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(82.dp)) {
                            AsyncImage(model = actor.poster, contentDescription = actor.title, modifier = Modifier.size(72.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            Text(actor.title, color = Ink, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(item: MediaEntry, ep: EpisodeEntry, onPlay: (MediaEntry, EpisodeEntry?) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable { onPlay(item, ep) },
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F6F7)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(88.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.padding(start = 12.dp).size(48.dp), color = Brand, shape = CircleShape) {
                Box(contentAlignment = Alignment.Center) { Text("▶", color = Color.White, fontSize = 19.sp) }
            }
            Spacer(Modifier.weight(1f))
            Column(Modifier.padding(horizontal = 12.dp).weight(2.3f), horizontalAlignment = Alignment.End) {
                Text(ep.title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
                if (ep.subtitle.isNotBlank()) Text(ep.subtitle, color = Muted, fontSize = 12.sp, maxLines = 1)
            }
            AsyncImage(
                model = ep.thumb.ifBlank { item.poster },
                contentDescription = ep.title,
                modifier = Modifier.width(112.dp).fillMaxHeight().clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun SourcesDialog(
    item: MediaEntry,
    episode: EpisodeEntry?,
    loading: Boolean,
    sources: List<SourceEntry>,
    onDismiss: () -> Unit,
    onPlay: (SourceEntry) -> Unit,
    onDownload: (SourceEntry) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                Text("اختر السيرفر", fontWeight = FontWeight.Black)
                Text(episode?.title ?: item.title, fontSize = 12.sp, color = Muted)
            }
        },
        text = {
            if (loading) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Brand) }
            } else if (sources.isEmpty()) {
                Text("لم نجد سيرفرات متاحة الآن.", color = Muted)
            } else {
                LazyColumn(Modifier.height((sources.size.coerceAtMost(6) * 78).dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sources) { src ->
                        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF4F2F3)) {
                            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { onDownload(src) }) { Text("تنزيل", color = Brand) }
                                Button(onClick = { onPlay(src) }, colors = ButtonDefaults.buttonColors(containerColor = Brand), shape = RoundedCornerShape(10.dp)) { Text("تشغيل") }
                                Spacer(Modifier.weight(1f))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(src.name, color = Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                                    Text(src.quality, color = Muted, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق", color = Brand) } },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun SearchDialog(tab: MainTab, onDismiss: () -> Unit, onResults: (List<MediaEntry>) -> Unit) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("بحث في ${tab.label}", fontWeight = FontWeight.Black) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("اكتب ما تبحث عنه") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (busy) CircularProgressIndicator(Modifier.padding(top = 14.dp).size(24.dp), color = Brand)
                if (message.isNotBlank()) Text(message, color = Live, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = {
            Button(
                enabled = query.isNotBlank() && !busy,
                onClick = {
                    busy = true
                    message = ""
                    scope.launch {
                        RemoteApi.search(tab, query).fold(
                            onSuccess = { busy = false; onResults(it) },
                            onFailure = { busy = false; message = it.message ?: "تعذر البحث" }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Brand)
            ) { Text("بحث") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        containerColor = Color.White
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerScreen(url: String, title: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
            modifier = Modifier.fillMaxSize()
        )
        Surface(
            Modifier.padding(14.dp).size(44.dp).clickable(onClick = onBack),
            shape = CircleShape,
            color = Color.Black.copy(alpha = .55f)
        ) {
            Box(contentAlignment = Alignment.Center) { Text("←", color = Color.White, fontSize = 24.sp) }
        }
        Text(
            title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 26.dp, start = 64.dp, end = 64.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BottomBar(tab: MainTab, onChange: (MainTab) -> Unit) {
    NavigationBar(containerColor = Color.White, tonalElevation = 9.dp) {
        MainTab.entries.forEach { t ->
            val active = t == tab
            NavigationBarItem(
                selected = active,
                onClick = { onChange(t) },
                icon = {
                    Surface(shape = RoundedCornerShape(14.dp), color = if (active) Brand else Color.Transparent) {
                        Text(
                            t.glyph,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                            color = if (active) Color.White else Color(0xFF8B8B92),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                },
                label = { Text(t.label, fontSize = 11.sp, fontWeight = if (active) FontWeight.Black else FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor = Brand,
                    unselectedTextColor = Muted,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
private fun Badge(text: String, bg: Color, fg: Color) {
    Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
        Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun LoadingBlock() {
    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Brand) }
}

@Composable
private fun ErrorBlock(text: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, color = Muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text("إعادة المحاولة") }
    }
}

@Composable
private fun EmptyBlock(text: String) {
    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { Text(text, color = Muted) }
}

private fun kindArabic(k: MediaKind) = when (k) {
    MediaKind.ANIME -> "أنمي"
    MediaKind.SERIES -> "مسلسل"
    MediaKind.MOVIE -> "فيلم"
    MediaKind.CHANNEL -> "قناة مباشرة"
}

private fun enqueueDownload(context: Context, url: String, title: String) {
    try {
        val clean = title.replace(Regex("[^\\p{L}\\p{N}._ -]+"), "_").trim().ifBlank { "AnimeWitcher" }.take(80)
        val ext = when {
            url.contains(".m3u8", true) -> ".m3u8"
            url.contains(".webm", true) -> ".webm"
            else -> ".mp4"
        }
        val req = DownloadManager.Request(Uri.parse(url))
            .setTitle(clean)
            .setDescription("Anime Witcher")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "$clean$ext")
        (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
        Toast.makeText(context, "بدأ التنزيل", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر بدء التنزيل: ${e.message}", Toast.LENGTH_LONG).show()
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
