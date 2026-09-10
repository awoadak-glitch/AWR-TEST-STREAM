package com.awr.streamhub

import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object RemoteApi {
    private const val ANIME_BASE = "https://awr-stream-web.vercel.app"
    private const val DRAMA_BASE = "https://awr-stream-web.vercel.app/api/drama"

    suspend fun loadTab(tab: MainTab): Result<List<HomeSection>> = runCatching {
        when (tab) {
            MainTab.Anime -> loadAnimeHome()
            MainTab.Series -> listOf(HomeSection("أحدث المسلسلات", parseDramaList(drama("series", mapOf("page" to "0")), MediaKind.SERIES)))
            MainTab.Movies -> listOf(HomeSection("أحدث الأفلام", parseDramaList(drama("movies", mapOf("page" to "0")), MediaKind.MOVIE)))
            MainTab.Channels -> listOf(HomeSection("القنوات المباشرة", parseDramaList(drama("channels", mapOf("page" to "0")), MediaKind.CHANNEL)))
        }
    }

    suspend fun loadDetails(item: MediaEntry): Result<DetailBundle> = runCatching {
        when (item.kind) {
            MediaKind.ANIME -> animeDetails(item)
            MediaKind.SERIES -> dramaDetails(item, true)
            MediaKind.MOVIE -> dramaDetails(item, false)
            MediaKind.CHANNEL -> dramaChannel(item)
        }
    }

    suspend fun loadSources(item: MediaEntry, episode: EpisodeEntry? = null): Result<List<SourceEntry>> = runCatching {
        when (item.kind) {
            MediaKind.ANIME -> {
                val ep = episode ?: error("اختر الحلقة أولاً")
                val root = JSONObject(get("$ANIME_BASE/api/anime/${url(item.id)}/episodes/${url(ep.id)}/servers"))
                val arr = root.optJSONArray("items") ?: JSONArray()
                (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    SourceEntry(i, o.str("id", i.toString()), o.str("name", "سيرفر ${i + 1}"), o.str("quality", "أخرى"), o.str("url"), o.toString())
                }
            }
            MediaKind.MOVIE -> parseSources(drama("movie-sources", mapOf("id" to item.id)))
            MediaKind.SERIES -> {
                val ep = episode ?: error("اختر الحلقة أولاً")
                parseSources(drama("episode-sources", mapOf("id" to ep.id)))
            }
            MediaKind.CHANNEL -> parseSources(drama("channel", mapOf("id" to item.id)))
        }
    }

    suspend fun resolveSource(item: MediaEntry, source: SourceEntry, episode: EpisodeEntry? = null): Result<String> = runCatching {
        if (item.kind == MediaKind.ANIME) {
            val ep = episode ?: error("الحلقة غير محددة")
            val root = JSONObject(get("$ANIME_BASE/api/anime/${url(item.id)}/episodes/${url(ep.id)}/servers?resolve=${source.index}"))
            root.optString("url").takeIf { it.startsWith("http") } ?: source.url.takeIf { it.startsWith("http") } ?: error(root.optString("error", "تعذر تجهيز السيرفر"))
        } else {
            val direct = source.url.takeIf { isDirect(it) }
            if (direct != null) direct else {
                val candidate = source.url.ifBlank { firstHttp(JSONObject(source.raw)) }
                if (candidate.isBlank()) error("لا يوجد رابط تشغيل صالح")
                val inspected = JSONObject(get("$DRAMA_BASE?action=inspect-source&url=${url(candidate)}"))
                val arr = inspected.optJSONArray("urls") ?: JSONArray()
                (0 until arr.length()).map { arr.optString(it) }.firstOrNull { isDirect(it) } ?: candidate
            }
        }
    }

    suspend fun search(tab: MainTab, query: String): Result<List<MediaEntry>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()
        when (tab) {
            MainTab.Anime -> parseAnimeSearch(JSONObject(get("$ANIME_BASE/api/search?q=${url(query)}")))
            else -> parseDramaList(drama("search", mapOf("query" to query, "page" to "0")), kindFor(tab))
        }
    }

    private suspend fun loadAnimeHome(): List<HomeSection> {
        val root = JSONObject(get("$ANIME_BASE/api/home"))
        return listOf(
            HomeSection("مختارات هذا الموسم", parseAnimeArray(root.optJSONArray("popular") ?: root.optJSONArray("hero") ?: JSONArray())),
            HomeSection("حلقات جديدة", parseAnimeArray(root.optJSONArray("recent") ?: JSONArray(), recent = true), episodeStyle = true),
            HomeSection("الأكثر شهرة هذا الموسم", parseAnimeArray(root.optJSONArray("popular") ?: JSONArray())),
            HomeSection("أفضل الأنميات عالمياً", parseAnimeArray(root.optJSONArray("bestMal") ?: JSONArray())),
            HomeSection("الأنميشن الأكثر مشاهدة", parseAnimeArray(root.optJSONArray("animations") ?: JSONArray())),
            HomeSection("آخر الأعمال المضافة", parseAnimeArray(root.optJSONArray("latest") ?: JSONArray()))
        ).filter { it.items.isNotEmpty() }
    }

    private suspend fun animeDetails(base: MediaEntry): DetailBundle {
        val o = JSONObject(get("$ANIME_BASE/api/anime/${url(base.id)}"))
        val details = o.optJSONObject("details")
        val item = base.copy(
            title = o.str("name", base.title), poster = o.str("poster_uri", base.poster), cover = o.str("cover_uri", base.cover),
            story = o.str("story", base.story), year = details?.str("year", base.year) ?: base.year,
            rating = o.optJSONObject("rating")?.optDouble("rate", 0.0)?.takeIf { it > 0 }?.let { String.format("%.1f", it) } ?: base.rating,
            tags = jsonStrings(o.optJSONArray("tags")).ifEmpty { base.tags }, raw = o.toString()
        )
        val eroot = JSONObject(get("$ANIME_BASE/api/anime/${url(base.id)}/episodes"))
        val arr = eroot.optJSONArray("items") ?: JSONArray()
        val episodes = (0 until arr.length()).mapNotNull { i ->
            val e = arr.optJSONObject(i) ?: return@mapNotNull null
            EpisodeEntry(e.str("doc_id", i.toString()), e.str("name", "الحلقة ${i + 1}"), e.str("thumb_uri", item.cover.ifBlank { item.poster }), e.str("title_translated"), e.toString())
        }
        return DetailBundle(item, episodes)
    }

    private suspend fun dramaDetails(base: MediaEntry, series: Boolean): DetailBundle {
        val data = drama("poster", mapOf("id" to base.id))
        val obj = firstObject(data) ?: JSONObject(base.raw.ifBlank { "{}" })
        val item = dramaItem(obj, if (series) MediaKind.SERIES else MediaKind.MOVIE, base)
        val castData = runCatching { drama("cast", mapOf("id" to base.id)) }.getOrNull()
        val cast = castData?.let { parseDramaList(it, MediaKind.MOVIE).take(15) } ?: emptyList()
        val episodes = if (series) parseEpisodes(drama("seasons", mapOf("id" to base.id))) else emptyList()
        return DetailBundle(item, episodes, cast)
    }

    private suspend fun dramaChannel(base: MediaEntry): DetailBundle {
        val data = drama("channel", mapOf("id" to base.id))
        val obj = firstObject(data) ?: JSONObject(base.raw.ifBlank { "{}" })
        return DetailBundle(dramaItem(obj, MediaKind.CHANNEL, base))
    }

    private suspend fun drama(action: String, params: Map<String, String> = emptyMap()): Any {
        val q = buildString {
            append("action=").append(url(action))
            params.forEach { (k, v) -> append('&').append(url(k)).append('=').append(url(v)) }
        }
        val root = JSONObject(get("$DRAMA_BASE?$q"))
        if (!root.optBoolean("ok", false)) error(root.optString("error", "تعذر جلب عالم الدراما"))
        return root.opt("data") ?: JSONArray()
    }

    private fun parseAnimeArray(arr: JSONArray, recent: Boolean = false): List<MediaEntry> = (0 until arr.length()).mapNotNull { i ->
        val o = arr.optJSONObject(i) ?: return@mapNotNull null
        val id = o.str(if (recent) "animeId" else "id", o.str("id"))
        val title = o.str("name", id)
        if (id.isBlank() || title.isBlank()) return@mapNotNull null
        MediaEntry(id, title, MediaKind.ANIME, o.str("poster"), o.str("cover", o.str("thumb")), if (recent) o.str("episodeName") else o.str("season"), year = o.str("year"), rating = o.opt("rate")?.toString()?.takeUnless { it == "null" } ?: "", tags = jsonStrings(o.optJSONArray("tags")), raw = o.toString())
    }

    private fun parseAnimeSearch(root: JSONObject): List<MediaEntry> {
        val arrays = listOf("items", "results", "hits", "data").mapNotNull { root.optJSONArray(it) }
        return arrays.firstOrNull()?.let { parseAnimeArray(it) } ?: emptyList()
    }

    private fun parseDramaList(data: Any, kind: MediaKind): List<MediaEntry> {
        val arr = bestArray(data) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let { dramaItem(it, kind, null) } }.filter { it.id.isNotBlank() && it.title.isNotBlank() }.distinctBy { it.id }
    }

    private fun dramaItem(o: JSONObject, kind: MediaKind, fallback: MediaEntry?): MediaEntry {
        val id = firstString(o, listOf("id", "poster_id", "movie_id", "serie_id", "channel_id", "post_id")).ifBlank { fallback?.id ?: "" }
        val title = firstString(o, listOf("name", "title", "poster_name", "movie_name", "serie_name", "channel_name")).ifBlank { fallback?.title ?: id }
        val poster = firstString(o, listOf("poster", "poster_uri", "image", "image_url", "photo", "thumbnail", "thumb", "logo")).ifBlank { fallback?.poster ?: "" }
        val cover = firstString(o, listOf("cover", "cover_uri", "backdrop", "background", "banner", "wallpaper")).ifBlank { fallback?.cover ?: poster }
        val story = firstString(o, listOf("story", "description", "overview", "content", "plot")).ifBlank { fallback?.story ?: "" }
        val year = firstString(o, listOf("year", "release_year", "date", "created_at")).ifBlank { fallback?.year ?: "" }
        val rating = firstString(o, listOf("rating", "rate", "imdb", "score")).ifBlank { fallback?.rating ?: "" }
        val subtitle = firstString(o, listOf("category", "type", "country", "quality", "episode_name")).ifBlank { fallback?.subtitle ?: "" }
        return MediaEntry(id, title, kind, poster, cover, subtitle, story, year, rating, extractTags(o).ifEmpty { fallback?.tags ?: emptyList() }, o.toString())
    }

    private fun parseEpisodes(data: Any): List<EpisodeEntry> {
        val out = mutableListOf<EpisodeEntry>()
        fun walk(v: Any?) {
            when (v) {
                is JSONArray -> for (i in 0 until v.length()) walk(v.opt(i))
                is JSONObject -> {
                    val id = firstString(v, listOf("episode_id", "id", "ep_id"))
                    val title = firstString(v, listOf("episode_name", "name", "title"))
                    val looksEpisode = id.isNotBlank() && (title.contains("حلقة") || title.contains("episode", true) || v.has("episode_id") || v.has("ep_num") || v.has("episode_number"))
                    if (looksEpisode) out += EpisodeEntry(id, title.ifBlank { "حلقة" }, firstString(v, listOf("image", "thumb", "thumbnail", "poster")), firstString(v, listOf("quality", "description")), v.toString())
                    val keys = v.keys(); while (keys.hasNext()) walk(v.opt(keys.next()))
                }
            }
        }
        walk(data)
        return out.distinctBy { it.id }
    }

    private fun parseSources(data: Any): List<SourceEntry> {
        val arr = bestArray(data)
        if (arr != null) {
            val out = (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val link = firstHttp(o)
                SourceEntry(i, firstString(o, listOf("id", "source_id")).ifBlank { i.toString() }, firstString(o, listOf("name", "server", "source_name", "title")).ifBlank { "سيرفر ${i + 1}" }, firstString(o, listOf("quality", "label", "resolution")).ifBlank { "تشغيل سريع" }, link, o.toString())
            }
            if (out.isNotEmpty()) return out
        }
        val obj = firstObject(data)
        return if (obj != null) listOf(SourceEntry(0, "0", "السيرفر الرئيسي", "مباشر", firstHttp(obj), obj.toString())) else emptyList()
    }

    private fun bestArray(data: Any?): JSONArray? {
        if (data is JSONArray) return data
        if (data !is JSONObject) return null
        val preferred = listOf("data", "results", "items", "posters", "movies", "series", "channels", "seasons", "episodes", "sources")
        preferred.forEach { k -> data.optJSONArray(k)?.let { if (it.length() > 0) return it } }
        val keys = data.keys(); while (keys.hasNext()) { val v = data.opt(keys.next()); if (v is JSONArray && v.length() > 0) return v; if (v is JSONObject) bestArray(v)?.let { return it } }
        return null
    }

    private fun firstObject(data: Any?): JSONObject? {
        if (data is JSONObject) { listOf("data", "item", "poster", "movie", "serie", "channel").forEach { data.optJSONObject(it)?.let { return it } }; return data }
        if (data is JSONArray) { for (i in 0 until data.length()) data.optJSONObject(i)?.let { return it } }
        return null
    }

    private fun extractTags(o: JSONObject): List<String> {
        listOf("tags", "genres", "genre", "categories").forEach { key -> o.optJSONArray(key)?.let { return jsonStrings(it).take(6) } }
        return emptyList()
    }

    private fun jsonStrings(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { i -> when (val v = arr.opt(i)) { is String -> v.takeIf { it.isNotBlank() }; is JSONObject -> firstString(v, listOf("name", "title", "label")).takeIf { it.isNotBlank() }; else -> null } }
    }

    private fun firstString(o: JSONObject, keys: List<String>): String {
        keys.forEach { k -> when (val v = o.opt(k)) { is String -> if (v.isNotBlank() && v != "null") return v; is Number -> return v.toString() } }
        return ""
    }

    private fun firstHttp(o: JSONObject): String {
        val priority = listOf("video_url", "video_uri", "direct_url", "download_url", "url", "link", "source", "file", "stream", "m3u8")
        priority.forEach { k -> val v = o.optString(k); if (v.startsWith("http")) return v }
        val keys = o.keys(); while (keys.hasNext()) {
            when (val v = o.opt(keys.next())) {
                is String -> if (v.startsWith("http")) return v
                is JSONObject -> firstHttp(v).takeIf { it.isNotBlank() }?.let { return it }
                is JSONArray -> for (i in 0 until v.length()) if (v.opt(i) is JSONObject) firstHttp(v.optJSONObject(i)).takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        return ""
    }

    private fun isDirect(v: String) = v.startsWith("http") && (Regex("\\.(mp4|m3u8|webm)([?#].*)?$", RegexOption.IGNORE_CASE).containsMatchIn(v) || v.contains("/api/file/"))
    private fun kindFor(tab: MainTab) = when (tab) { MainTab.Series -> MediaKind.SERIES; MainTab.Movies -> MediaKind.MOVIE; MainTab.Channels -> MediaKind.CHANNEL; else -> MediaKind.ANIME }
    private fun JSONObject.str(k: String, fallback: String = "") = optString(k).takeUnless { it.isBlank() || it == "null" } ?: fallback
    private fun url(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 18000; readTimeout = 24000
            setRequestProperty("Accept", "application/json, text/plain, */*")
            setRequestProperty("Accept-Language", "ar-YE,ar;q=0.9,en;q=0.6")
            setRequestProperty("User-Agent", "Anime-Witcher-Android/2.0")
        }
        val code = c.responseCode
        val body = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) error("HTTP $code: ${body.take(160)}")
        body
    }
}
