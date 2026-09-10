package com.awr.streamhub

enum class MainTab(val label: String, val glyph: String) {
    Anime("أنمي", "✦"),
    Series("مسلسلات", "▣"),
    Movies("أفلام", "▶"),
    Channels("قنوات", "◉")
}

enum class MediaKind { ANIME, SERIES, MOVIE, CHANNEL }

data class MediaEntry(
    val id: String,
    val title: String,
    val kind: MediaKind,
    val poster: String = "",
    val cover: String = "",
    val subtitle: String = "",
    val story: String = "",
    val year: String = "",
    val rating: String = "",
    val tags: List<String> = emptyList(),
    val raw: String = ""
)

data class HomeSection(val title: String, val items: List<MediaEntry>, val episodeStyle: Boolean = false)

data class EpisodeEntry(
    val id: String,
    val title: String,
    val thumb: String = "",
    val subtitle: String = "",
    val raw: String = ""
)

data class SourceEntry(
    val index: Int,
    val id: String,
    val name: String,
    val quality: String,
    val url: String = "",
    val raw: String = ""
)

data class DetailBundle(
    val item: MediaEntry,
    val episodes: List<EpisodeEntry> = emptyList(),
    val cast: List<MediaEntry> = emptyList()
)
