package com.animeblack.app.auth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private fun jsonArray(raw: String): JSONArray = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
private fun queryEncode(value: String): String = Uri.encode(value.trim())
private fun jsonObject(block: JSONObject.() -> Unit): JSONObject = JSONObject().apply(block)
private fun inClause(values: List<String>): String = "in.(${values.distinct().joinToString(",")})"

private data class PublicProfile(
    val id: String,
    val name: String,
    val username: String,
    val avatarUrl: String?
)

data class CharacterItem(
    val id: String,
    val nameArabic: String,
    val nameEnglish: String,
    val nameNative: String,
    val imageUrl: String?,
    val about: String
) {
    val displayName: String get() = nameArabic.ifBlank { nameEnglish }
}

data class CatalogEntry(
    val id: String,
    val kind: String,
    val titleArabic: String,
    val titleEnglish: String,
    val posterUrl: String?,
    val bannerUrl: String?,
    val synopsis: String,
    val format: String,
    val status: String,
    val count: Int,
    val score: Double,
    val popularity: Int,
    val season: String?,
    val year: Int?,
    val ageRating: String?,
    val isNsfw: Boolean,
    val trailerYoutubeId: String? = null,
    val officialSiteUrl: String? = null
) {
    val displayTitle: String get() = titleArabic.ifBlank { titleEnglish }
}

data class AnimeItem(
    val id: String,
    val titleArabic: String,
    val titleEnglish: String,
    val posterUrl: String?,
    val episodeCount: Int,
    val format: String,
    val status: String,
    val score: Double
) {
    val displayTitle: String get() = titleArabic.ifBlank { titleEnglish }
}

data class CommunityItem(
    val id: String,
    val name: String,
    val slug: String,
    val description: String,
    val coverUrl: String?,
    val category: String,
    val memberCount: Int,
    val isJoined: Boolean,
    val createdBy: String
)

data class PollOptionItem(val id: String, val text: String, val votes: Int, val isSelected: Boolean)

data class SocialPost(
    val id: String,
    val authorId: String,
    val authorName: String,
    val username: String,
    val avatarUrl: String?,
    val content: String,
    val createdAt: String,
    val kind: String,
    val topic: String,
    val imageUrl: String?,
    val videoUrl: String?,
    val containsSpoiler: Boolean,
    val spoilerLabel: String?,
    val animeId: String?,
    val animeTitle: String?,
    val rating: Int?,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean,
    val isSaved: Boolean,
    val pollId: String?,
    val pollOptions: List<PollOptionItem>,
    val pollEnded: Boolean
)

data class StoryItem(
    val id: String,
    val authorId: String,
    val authorName: String,
    val username: String,
    val avatarUrl: String?,
    val imageUrl: String,
    val caption: String,
    val createdAt: String
)

data class ChatSummary(
    val id: String,
    val title: String,
    val avatarUrl: String?,
    val lastMessage: String,
    val updatedAt: String,
    val unreadCount: Int,
    val otherUserId: String?
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val avatarUrl: String?,
    val body: String,
    val createdAt: String
)

data class ReelItem(
    val id: String,
    val authorId: String,
    val authorName: String,
    val username: String,
    val avatarUrl: String?,
    val videoUrl: String,
    val caption: String,
    val createdAt: String,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean,
    val isFollowing: Boolean
)

data class SocialComment(
    val id: String,
    val authorId: String,
    val authorName: String,
    val username: String,
    val avatarUrl: String?,
    val body: String,
    val createdAt: String
)

data class UserStats(
    val xp: Int,
    val level: Int,
    val levelTitle: String,
    val nextLevelXp: Int,
    val unlockedAchievements: Int,
    val totalAchievements: Int,
    val postCount: Int,
    val trackedCount: Int,
    val currentFrameId: String?
)

data class AchievementItem(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val xpReward: Int,
    val unlockedAt: String?
)

data class FrameItem(
    val id: String,
    val name: String,
    val nameEnglish: String,
    val rarity: String,
    val unlockType: String,
    val unlockLevel: Int,
    val imageUrl: String?,
    val isUnlocked: Boolean,
    val isActive: Boolean
)

data class AdminMetrics(val users: Int, val posts: Int, val communities: Int, val reels: Int, val pendingReports: Int)

data class AdminAuditEntry(val id: String, val actorName: String, val action: String, val targetType: String, val targetId: String?, val details: String, val createdAt: String)

data class AdminReport(
    val id: String,
    val postId: String,
    val reason: String,
    val reportedAt: String,
    val status: String,
    val moderatorNote: String?,
    val postContent: String?,
    val postAuthorName: String?
)

data class ReviewEntry(
    val id: String,
    val userId: String,
    val authorName: String,
    val authorAvatar: String?,
    val workTitle: String,
    val rating: Int,
    val title: String?,
    val body: String,
    val containsSpoiler: Boolean,
    val likeCount: Int,
    val createdAt: String,
    val isLiked: Boolean
)

data class QuoteEntry(
    val id: String,
    val text: String,
    val characterName: String,
    val sourceReference: String?,
    val tags: List<String>,
    val likeCount: Int,
    val createdAt: String,
    val isLiked: Boolean
)

data class AdminQuoteEntry(
    val id: String,
    val text: String,
    val characterName: String,
    val sourceReference: String?,
    val tags: List<String>,
    val submittedByName: String,
    val createdAt: String
)

data class UserNotification(
    val id: String,
    val actorId: String,
    val actorName: String,
    val actorAvatar: String?,
    val kind: String,
    val postId: String?,
    val createdAt: String,
    val isRead: Boolean
)

data class MangaTrackerEntry(
    val mangaId: String,
    val status: String,
    val progressChapters: Int,
    val progressVolumes: Int,
    val score: Int?
)

data class MangaWatchEntry(val manga: CatalogEntry, val status: String, val progressChapters: Int, val progressVolumes: Int, val score: Int?)

data class EpisodeScheduleEntry(val id: String, val anime: AnimeItem, val episodeNumber: Int, val airAt: String, val source: String?)

data class WatchEntry(
    val anime: AnimeItem,
    val status: String,
    val progress: Int,
    val score: Int?,
    val entryId: String
)

data class SocialSearchResults(
    val anime: List<AnimeItem>,
    val people: List<PublicProfileResult>,
    val communities: List<CommunityItem>,
    val posts: List<SocialPost>
)

data class PublicProfileResult(val id: String, val name: String, val username: String, val avatarUrl: String?, val bio: String)

class SocialRepository(private val auth: SupabaseAuthRepository, context: Context) {
    private val appContext = context.applicationContext

    suspend fun loadAnime(search: String = ""): List<AnimeItem> {
        val term = search.trim()
        val filter = if (term.isNotBlank()) "&or=(title_ar.ilike.*${queryEncode(term)}*,title_en.ilike.*${queryEncode(term)}*)" else ""
        val rows = jsonArray(auth.socialRest("anime?select=id,title_ar,title_en,poster_url,episodes_count,format,status,score&order=popularity.desc.nullslast&limit=100$filter"))
        return (0 until rows.length()).mapNotNull { i -> rows.optJSONObject(i)?.let(::animeFrom) }
    }

    suspend fun loadEpisodeSchedule(): List<EpisodeScheduleEntry> {
        val rows = jsonArray(auth.socialRest("episode_schedule?select=id,anime_id,episode_number,air_at,source&air_at=gte.${queryEncode(nowIso())}&order=air_at.asc&limit=100"))
        val raw = (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }
        if (raw.isEmpty()) return emptyList()
        val animeById = animeByIds(raw.map { it.optString("anime_id") }.distinct()).associateBy { it.id }
        return raw.mapNotNull { row ->
            val anime = animeById[row.optString("anime_id")] ?: return@mapNotNull null
            EpisodeScheduleEntry(row.optString("id"), anime, row.optInt("episode_number"), row.optString("air_at"), row.optString("source").takeIf(String::isNotBlank))
        }
    }

    suspend fun loadCatalog(kind: String, search: String = "", status: String = "all"): List<CatalogEntry> {
        require(kind == "anime" || kind == "manga") { "Unknown catalog type" }
        val table = kind
        val selectedColumns = if (kind == "anime") {
"id,title_ar,title_en,poster_url,banner_url,synopsis_ar,format,status,episodes_count,score,popularity,season,season_year,age_rating,is_nsfw,trailer_youtube_id,official_site_url"
        } else {
            "id,title_ar,title_en,poster_url,banner_url,synopsis_ar,format,status,chapters_count,score,popularity,is_nsfw"
        }
        val term = search.trim()
        val filters = buildList {
            add("is_nsfw=eq.false")
            if (term.isNotBlank()) add("or=(title_ar.ilike.*${queryEncode(term)}*,title_en.ilike.*${queryEncode(term)}*,title_romaji.ilike.*${queryEncode(term)}*)")
            if (status != "all") add("status=eq.${queryEncode(status)}")
        }.joinToString("&")
        val rows = jsonArray(auth.socialRest("$table?select=$selectedColumns&$filters&order=popularity.desc&limit=80"))
        return (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }.map { row -> catalogFrom(row, kind) }
    }

    suspend fun loadCatalogDetail(kind: String, itemId: String): CatalogEntry? {
        require(kind == "anime" || kind == "manga") { "Unknown catalog type" }
        val row = jsonArray(auth.socialRest("$kind?id=eq.$itemId&is_nsfw=eq.false&select=*&limit=1")).optJSONObject(0) ?: return null
        return catalogFrom(row, kind)
    }

    suspend fun loadMangaWatchlist(userId: String): List<MangaWatchEntry> {
        val raw = jsonArray(auth.socialRest("manga_tracker_entries?user_id=eq.$userId&select=manga_id,status,progress_chapters,progress_volumes,score&order=updated_at.desc&limit=500"))
        val entries = (0 until raw.length()).mapNotNull { raw.optJSONObject(it) }
        if (entries.isEmpty()) return emptyList()
        val ids = entries.map { it.optString("manga_id") }.filter(String::isNotBlank).distinct()
        if (ids.isEmpty()) return emptyList()
        val mangaRows = jsonArray(auth.socialRest("manga?id=${inClause(ids)}&is_nsfw=eq.false&select=*&limit=500"))
        val mangaById = (0 until mangaRows.length()).mapNotNull { mangaRows.optJSONObject(it) }
            .associate { row -> row.optString("id") to catalogFrom(row, "manga") }
        return entries.mapNotNull { row ->
            val manga = mangaById[row.optString("manga_id")] ?: return@mapNotNull null
            MangaWatchEntry(manga, row.optString("status"), row.optInt("progress_chapters"), row.optInt("progress_volumes"),
                if (row.isNull("score")) null else row.optInt("score"))
        }
    }

    suspend fun loadCharacters(search: String = ""): List<CharacterItem> {
        val term = search.trim()
        val filter = if (term.isBlank()) "" else "&or=(name_ar.ilike.*${queryEncode(term)}*,name_en.ilike.*${queryEncode(term)}*,name_native.ilike.*${queryEncode(term)}*)"
        val rows = jsonArray(auth.socialRest("characters?select=id,name_ar,name_en,name_native,image_url,about&order=name_ar.asc.nullslast&limit=100$filter"))
        return (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }.map(::characterFrom)
    }

    suspend fun loadCharacterDetail(characterId: String): CharacterItem? {
        val rows = jsonArray(auth.socialRest("characters?id=eq.$characterId&select=id,name_ar,name_en,name_native,image_url,about&limit=1"))
        return rows.optJSONObject(0)?.let(::characterFrom)
    }

    suspend fun loadCharacterWorks(characterId: String): List<CatalogEntry> {
        val animeLinks = jsonArray(auth.socialRest("anime_characters?character_id=eq.$characterId&select=anime_id&limit=100"))
        val mangaLinks = jsonArray(auth.socialRest("manga_characters?character_id=eq.$characterId&select=manga_id&limit=100"))
        val animeIds = (0 until animeLinks.length()).mapNotNull { animeLinks.optJSONObject(it)?.optString("anime_id")?.takeIf(String::isNotBlank) }
        val mangaIds = (0 until mangaLinks.length()).mapNotNull { mangaLinks.optJSONObject(it)?.optString("manga_id")?.takeIf(String::isNotBlank) }
        val anime = loadCatalogEntriesByIds("anime", animeIds)
        val manga = loadCatalogEntriesByIds("manga", mangaIds)
        return anime + manga
    }

    suspend fun loadFavoriteCharacterIds(userId: String): Set<String> {
        val rows = jsonArray(auth.socialRest("profile_favorite_characters?user_id=eq.$userId&select=character_id&order=rank.asc&limit=9"))
        return (0 until rows.length()).mapNotNull { rows.optJSONObject(it)?.optString("character_id")?.takeIf(String::isNotBlank) }.toSet()
    }

    suspend fun toggleFavoriteCharacter(userId: String, characterId: String, isFavorite: Boolean) {
        if (isFavorite) {
            auth.socialRest("profile_favorite_characters?user_id=eq.$userId&character_id=eq.$characterId", "DELETE")
            return
        }
        val current = jsonArray(auth.socialRest("profile_favorite_characters?user_id=eq.$userId&select=rank&order=rank.asc&limit=9"))
        val ranks = (0 until current.length()).mapNotNull { current.optJSONObject(it)?.optInt("rank") }.toSet()
        val nextRank = (1..9).firstOrNull { it !in ranks } ?: throw AuthException("وصلت إلى الحد الأقصى وهو 9 شخصيات مفضلة.")
        auth.socialRest("profile_favorite_characters", "POST", jsonObject {
            put("user_id", userId); put("character_id", characterId); put("rank", nextRank)
        }.toString())
    }

    private suspend fun loadCatalogEntriesByIds(kind: String, ids: List<String>): List<CatalogEntry> {
        if (ids.isEmpty()) return emptyList()
        require(kind == "anime" || kind == "manga")
        val columns = if (kind == "anime") "id,title_ar,title_en,poster_url,banner_url,synopsis_ar,format,status,episodes_count,score,popularity,season,season_year,age_rating,is_nsfw,trailer_youtube_id,official_site_url"
            else "id,title_ar,title_en,poster_url,banner_url,synopsis_ar,format,status,chapters_count,score,popularity,is_nsfw"
        val rows = jsonArray(auth.socialRest("$kind?id=${inClause(ids)}&is_nsfw=eq.false&select=$columns&limit=100"))
        return (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }.map { catalogFrom(it, kind) }
    }

    private fun characterFrom(row: JSONObject) = CharacterItem(
        id = row.optString("id"), nameArabic = row.optString("name_ar"), nameEnglish = row.optString("name_en"),
        nameNative = row.optString("name_native"), imageUrl = row.optString("image_url").takeIf(String::isNotBlank),
        about = row.optString("about")
    )

    suspend fun loadMangaTrackerEntry(userId: String, mangaId: String): MangaTrackerEntry? {
        val rows = jsonArray(auth.socialRest("manga_tracker_entries?user_id=eq.$userId&manga_id=eq.$mangaId&select=manga_id,status,progress_chapters,progress_volumes,score&limit=1"))
        val row = rows.optJSONObject(0) ?: return null
        return MangaTrackerEntry(row.optString("manga_id"), row.optString("status"), row.optInt("progress_chapters"),
            row.optInt("progress_volumes"), if (row.isNull("score")) null else row.optInt("score"))
    }

    suspend fun saveMangaTrackerEntry(userId: String, mangaId: String, status: String, progressChapters: Int, progressVolumes: Int, score: Int?) {
        require(status in setOf("reading", "planning", "completed", "paused", "dropped")) { "حالة قراءة غير صالحة." }
        require(progressChapters >= 0 && progressVolumes >= 0) { "التقدم لا يمكن أن يكون سالبًا." }
        require(score == null || score in 1..10) { "التقييم يجب أن يكون بين 1 و10." }
        val payload = jsonObject {
            put("user_id", userId); put("manga_id", mangaId); put("status", status)
            put("progress_chapters", progressChapters); put("progress_volumes", progressVolumes)
            put("score", score ?: JSONObject.NULL)
        }
        auth.socialRest("manga_tracker_entries?on_conflict=user_id,manga_id", "POST", payload.toString(), prefer = "resolution=merge-duplicates,return=minimal")
    }

    suspend fun deleteMangaTrackerEntry(userId: String, mangaId: String) {
        auth.socialRest("manga_tracker_entries?user_id=eq.$userId&manga_id=eq.$mangaId", "DELETE")
    }

    private fun catalogFrom(row: JSONObject, kind: String): CatalogEntry {
        val countColumn = if (kind == "anime") "episodes_count" else "chapters_count"
        return CatalogEntry(
            id = row.optString("id"), kind = kind,
            titleArabic = row.optString("title_ar"), titleEnglish = row.optString("title_en"),
            posterUrl = row.optString("poster_url").takeIf(String::isNotBlank),
            bannerUrl = row.optString("banner_url").takeIf(String::isNotBlank),
            synopsis = row.optString("synopsis_ar").ifBlank { row.optString("synopsis_en") },
            format = row.optString("format"), status = row.optString("status"),
            count = row.optInt(countColumn), score = row.optDouble("score"), popularity = row.optInt("popularity"),
            season = row.optString("season").takeIf(String::isNotBlank),
            year = row.optInt("season_year").takeIf { !row.isNull("season_year") && it > 0 },
            ageRating = row.optString("age_rating").takeIf(String::isNotBlank),
            isNsfw = row.optBoolean("is_nsfw"),
            trailerYoutubeId = row.optString("trailer_youtube_id").takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) },
            officialSiteUrl = row.optString("official_site_url").takeIf { it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true) }
        )
    }

    suspend fun loadFavoriteAnimeIds(userId: String): Set<String> {
        val rows = jsonArray(auth.socialRest("profile_favorite_anime?user_id=eq.$userId&select=anime_id&order=rank.asc&limit=20"))
        return (0 until rows.length()).mapNotNull { rows.optJSONObject(it)?.optString("anime_id")?.takeIf(String::isNotBlank) }.toSet()
    }

    suspend fun loadFavoriteAnime(userId: String): List<CatalogEntry> {
        val rows = jsonArray(auth.socialRest("profile_favorite_anime?user_id=eq.$userId&select=anime_id&order=rank.asc&limit=10"))
        val ids = (0 until rows.length()).mapNotNull { rows.optJSONObject(it)?.optString("anime_id")?.takeIf(String::isNotBlank) }
        val byId = loadCatalogEntriesByIds("anime", ids).associateBy { it.id }
        return ids.mapNotNull(byId::get)
    }

    suspend fun loadFavoriteCharacters(userId: String): List<CharacterItem> {
        val rows = jsonArray(auth.socialRest("profile_favorite_characters?user_id=eq.$userId&select=character_id&order=rank.asc&limit=9"))
        val ids = (0 until rows.length()).mapNotNull { rows.optJSONObject(it)?.optString("character_id")?.takeIf(String::isNotBlank) }
        if (ids.isEmpty()) return emptyList()
        val characterRows = jsonArray(auth.socialRest("characters?id=${inClause(ids)}&select=id,name_ar,name_en,name_native,image_url,about&limit=20"))
        val byId = (0 until characterRows.length()).mapNotNull { characterRows.optJSONObject(it) }.associate { it.optString("id") to characterFrom(it) }
        return ids.mapNotNull(byId::get)
    }

    suspend fun toggleFavoriteAnime(userId: String, animeId: String, isFavorite: Boolean) {
        if (isFavorite) {
            auth.socialRest("profile_favorite_anime?user_id=eq.$userId&anime_id=eq.$animeId", "DELETE")
            return
        }
        val current = jsonArray(auth.socialRest("profile_favorite_anime?user_id=eq.$userId&select=rank&order=rank.asc&limit=10"))
        val usedRanks = (0 until current.length()).mapNotNull { current.optJSONObject(it)?.optInt("rank") }.toSet()
        val nextRank = (1..10).firstOrNull { it !in usedRanks }
            ?: throw AuthException("يمكنك اختيار 10 أعمال مفضلة كحد أقصى.")
        auth.socialRest("profile_favorite_anime", "POST", jsonObject {
            put("user_id", userId); put("anime_id", animeId); put("rank", nextRank)
        }.toString())
    }

    suspend fun loadSavedPosts(userId: String): List<SocialPost> {
        val rows = jsonArray(auth.socialRest("post_saves?user_id=eq.$userId&select=post_id&limit=200"))
        val ids = (0 until rows.length()).mapNotNull { rows.optJSONObject(it)?.optString("post_id")?.takeIf(String::isNotBlank) }.distinct()
        if (ids.isEmpty()) return emptyList()
        return loadFeed(userId, filter = "all", postIdsFilter = ids)
    }

    suspend fun loadFeed(userId: String, filter: String = "all", communityId: String? = null, postIdsFilter: List<String>? = null, authorIdFilter: String? = null): List<SocialPost> {
        var authorIds: List<String>? = null
        if (filter == "following") {
            val follows = jsonArray(auth.socialRest("follows?follower_id=eq.$userId&select=following_id&limit=500"))
            authorIds = (0 until follows.length()).mapNotNull { follows.optJSONObject(it)?.optString("following_id")?.takeIf(String::isNotBlank) }
            if (authorIds.isEmpty()) return emptyList()
        }
        val filters = buildList {
            if (communityId != null) add("community_id=eq.$communityId")
            if (postIdsFilter != null) add("id=${inClause(postIdsFilter)}")
            if (authorIdFilter != null) add("author_id=eq.$authorIdFilter")
            if (authorIds != null) add("author_id=${inClause(authorIds)}")
            when (filter) {
                "reviews" -> add("kind=eq.review")
                "theories" -> add("topic=eq.theory")
            }
        }.joinToString("&").let { if (it.isBlank()) "" else "&$it" }
        val rowLimit = when {
            postIdsFilter != null -> 200
            authorIdFilter != null -> 20
            else -> 40
        }
        val rows = jsonArray(auth.socialRest("posts?select=id,author_id,community_id,content,image_url,video_url,created_at,kind,topic,contains_spoiler,spoiler_label,related_anime_id&order=created_at.desc&limit=$rowLimit$filters"))
        val rawPosts = (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }
        if (rawPosts.isEmpty()) return emptyList()
        val postIds = rawPosts.map { it.optString("id") }
        val authorIdsFromPosts = rawPosts.map { it.optString("author_id") }.distinct()
        val profiles = profilesById(authorIdsFromPosts)
        val likes = jsonArray(auth.socialRest("post_likes?post_id=${inClause(postIds)}&select=post_id,user_id&limit=5000"))
        val comments = jsonArray(auth.socialRest("post_comments?post_id=${inClause(postIds)}&select=post_id&limit=5000"))
        val saves = jsonArray(auth.socialRest("post_saves?user_id=eq.$userId&post_id=${inClause(postIds)}&select=post_id&limit=500"))
        val likeCounts = mutableMapOf<String, Int>()
        val liked = mutableSetOf<String>()
        for (i in 0 until likes.length()) likes.optJSONObject(i)?.let { row ->
            val id = row.optString("post_id")
            likeCounts[id] = (likeCounts[id] ?: 0) + 1
            if (row.optString("user_id") == userId) liked += id
        }
        val commentCounts = mutableMapOf<String, Int>()
        for (i in 0 until comments.length()) comments.optJSONObject(i)?.optString("post_id")?.let { commentCounts[it] = (commentCounts[it] ?: 0) + 1 }
        val saved = (0 until saves.length()).mapNotNull { saves.optJSONObject(it)?.optString("post_id") }.toSet()

        val animeIds = rawPosts.mapNotNull { it.optString("related_anime_id").takeIf(String::isNotBlank) }.distinct()
        val animeMap = if (animeIds.isEmpty()) emptyMap() else animeByIds(animeIds).associateBy { it.id }
        val pollRows = jsonArray(auth.socialRest("polls?post_id=${inClause(postIds)}&select=id,post_id,ends_at&limit=100"))
        val pollByPost = (0 until pollRows.length()).mapNotNull { pollRows.optJSONObject(it) }.associateBy { it.optString("post_id") }
        val pollIds = pollByPost.values.map { it.optString("id") }.distinct()
        val optionRows = if (pollIds.isEmpty()) JSONArray() else jsonArray(auth.socialRest("poll_options?poll_id=${inClause(pollIds)}&select=id,poll_id,text,position&order=position.asc&limit=600"))
        val voteRows = if (pollIds.isEmpty()) JSONArray() else jsonArray(auth.socialRest("poll_votes?poll_id=${inClause(pollIds)}&select=poll_id,option_id,user_id&limit=5000"))
        val voteCounts = mutableMapOf<String, Int>()
        val votedOption = mutableMapOf<String, String>()
        for (i in 0 until voteRows.length()) voteRows.optJSONObject(i)?.let { vote ->
            val poll = vote.optString("poll_id")
            val option = vote.optString("option_id")
            voteCounts[option] = (voteCounts[option] ?: 0) + 1
            if (vote.optString("user_id") == userId) votedOption[poll] = option
        }
        val optionsByPoll = mutableMapOf<String, MutableList<PollOptionItem>>()
        for (i in 0 until optionRows.length()) optionRows.optJSONObject(i)?.let { option ->
            val poll = option.optString("poll_id")
            optionsByPoll.getOrPut(poll) { mutableListOf() } += PollOptionItem(
                id = option.optString("id"), text = option.optString("text"),
                votes = voteCounts[option.optString("id")] ?: 0,
                isSelected = votedOption[poll] == option.optString("id")
            )
        }
        val posts = rawPosts.map { row ->
            val id = row.optString("id")
            val authorId = row.optString("author_id")
            val author = profiles[authorId]
            val animeId = row.optString("related_anime_id").takeIf(String::isNotBlank)
            val poll = pollByPost[id]
            SocialPost(
                id = id, authorId = authorId, authorName = author?.name ?: "عضو Anime Black",
                username = author?.username.orEmpty(), avatarUrl = author?.avatarUrl,
                content = row.optString("content"), createdAt = row.optString("created_at"),
                kind = row.optString("kind"), topic = row.optString("topic", "general"),
                imageUrl = row.optString("image_url").takeIf(String::isNotBlank),
                videoUrl = row.optString("video_url").takeIf(String::isNotBlank),
                containsSpoiler = row.optBoolean("contains_spoiler"),
                spoilerLabel = row.optString("spoiler_label").takeIf(String::isNotBlank),
                animeId = animeId, animeTitle = animeId?.let { animeMap[it]?.displayTitle },
                rating = null, likeCount = likeCounts[id] ?: 0,
                commentCount = commentCounts[id] ?: 0, isLiked = id in liked, isSaved = id in saved,
                pollId = poll?.optString("id"), pollOptions = poll?.optString("id")?.let { optionsByPoll[it]?.toList() }.orEmpty(),
                pollEnded = poll?.optString("ends_at")?.let { it.isNotBlank() && it < nowIso() } ?: false
            )
        }
        return if (filter == "trending") posts.sortedByDescending { it.likeCount + it.commentCount * 2 } else posts
    }

    suspend fun publishPost(
        content: String,
        kind: String,
        topic: String,
        imageUrl: String? = null,
        videoUrl: String? = null,
        spoiler: Boolean = false,
        spoilerLabel: String? = null,
        animeId: String? = null,
        rating: Int? = null,
        pollOptions: List<String> = emptyList(),
        communityId: String? = null
    ): String {
        val payload = jsonObject {
            put("p_content", content.trim())
            put("p_kind", kind)
            put("p_topic", topic)
            put("p_image_url", imageUrl ?: JSONObject.NULL)
            put("p_video_url", videoUrl ?: JSONObject.NULL)
            put("p_contains_spoiler", spoiler)
            put("p_spoiler_label", spoilerLabel ?: JSONObject.NULL)
            put("p_related_anime_id", animeId ?: JSONObject.NULL)
            put("p_rating", rating ?: JSONObject.NULL)
            put("p_poll_options", JSONArray(pollOptions))
            put("p_community_id", communityId ?: JSONObject.NULL)
        }
        val response = auth.socialRest("rpc/publish_social_post", "POST", payload.toString())
        return response.trim().trim('"')
    }

    suspend fun togglePostLike(userId: String, postId: String, isLiked: Boolean) {
        if (isLiked) auth.socialRest("post_likes?post_id=eq.$postId&user_id=eq.$userId", "DELETE")
        else auth.socialRest("post_likes", "POST", jsonObject { put("post_id", postId); put("user_id", userId) }.toString())
    }

    suspend fun togglePostSave(userId: String, postId: String, isSaved: Boolean) {
        if (isSaved) auth.socialRest("post_saves?post_id=eq.$postId&user_id=eq.$userId", "DELETE")
        else auth.socialRest("post_saves", "POST", jsonObject { put("post_id", postId); put("user_id", userId) }.toString())
    }

    suspend fun loadPostComments(postId: String): List<SocialComment> = loadComments("post_comments", "post_id", postId, "author_id")

    suspend fun addPostComment(userId: String, postId: String, body: String) {
        auth.socialRest("post_comments", "POST", jsonObject { put("post_id", postId); put("author_id", userId); put("body", body.trim()) }.toString())
    }

    suspend fun loadReviews(userId: String): List<ReviewEntry> {
        val rows = jsonArray(auth.socialRest("reviews?select=id,user_id,anime_id,manga_id,rating,title,body,contains_spoiler,likes_count,created_at&order=created_at.desc&limit=100"))
        val raw = (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }
        if (raw.isEmpty()) return emptyList()
        val animeIds = raw.mapNotNull { it.optString("anime_id").takeIf(String::isNotBlank) }.distinct()
        val mangaIds = raw.mapNotNull { it.optString("manga_id").takeIf(String::isNotBlank) }.distinct()
        val animeTitles = animeByIds(animeIds).associate { it.id to it.displayTitle }
        val mangaTitles = loadCatalogEntriesByIds("manga", mangaIds).associate { it.id to it.displayTitle }
        val profiles = profilesById(raw.map { it.optString("user_id") }.distinct())
        val likedRows = jsonArray(auth.socialRest("review_likes?user_id=eq.$userId&select=review_id&limit=500"))
        val likedIds = (0 until likedRows.length()).mapNotNull { likedRows.optJSONObject(it)?.optString("review_id")?.takeIf(String::isNotBlank) }.toSet()
        return raw.mapNotNull { row ->
            val authorId = row.optString("user_id")
            val profile = profiles[authorId]
            val workId = row.optString("anime_id").takeIf(String::isNotBlank) ?: row.optString("manga_id")
            val workTitle = animeTitles[workId] ?: mangaTitles[workId] ?: "عنوان غير متاح"
            ReviewEntry(row.optString("id"), authorId, profile?.name ?: "عضو", profile?.avatarUrl, workTitle,
                row.optInt("rating"), row.optString("title").takeIf(String::isNotBlank), row.optString("body"),
                row.optBoolean("contains_spoiler"), row.optInt("likes_count"), row.optString("created_at"), row.optString("id") in likedIds)
        }
    }

    suspend fun toggleReviewLike(userId: String, reviewId: String, isLiked: Boolean) {
        if (isLiked) auth.socialRest("review_likes?review_id=eq.$reviewId&user_id=eq.$userId", "DELETE")
        else auth.socialRest("review_likes", "POST", jsonObject { put("review_id", reviewId); put("user_id", userId) }.toString())
    }

    suspend fun loadApprovedQuotes(userId: String): List<QuoteEntry> {
        val rows = jsonArray(auth.socialRest("quotes?status=eq.approved&select=id,text_ar,character_name,source_reference,tags,likes_count,created_at&order=created_at.desc&limit=100"))
        val raw = (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }
        if (raw.isEmpty()) return emptyList()
        val likedRows = jsonArray(auth.socialRest("quote_likes?user_id=eq.$userId&select=quote_id&limit=500"))
        val likedIds = (0 until likedRows.length()).mapNotNull { likedRows.optJSONObject(it)?.optString("quote_id")?.takeIf(String::isNotBlank) }.toSet()
        return raw.map { row ->
            val tagsJson = row.optJSONArray("tags")
            val tags = if (tagsJson == null) emptyList() else (0 until tagsJson.length()).mapNotNull { tagsJson.optString(it).takeIf(String::isNotBlank) }
            QuoteEntry(row.optString("id"), row.optString("text_ar"), row.optString("character_name"),
                row.optString("source_reference").takeIf(String::isNotBlank), tags, row.optInt("likes_count"), row.optString("created_at"),
                row.optString("id") in likedIds)
        }
    }

    suspend fun submitQuoteSuggestion(userId: String, text: String, characterName: String, sourceReference: String, tags: List<String>) {
        val normalizedText = text.trim()
        if (normalizedText.length !in 3..800) throw AuthException("يجب أن يتراوح الاقتباس بين 3 و800 حرف.")
        val normalizedTags = tags.map(String::trim).filter(String::isNotBlank).distinct().take(10)
        if (normalizedTags.any { it.length > 40 }) throw AuthException("يجب ألا يتجاوز كل وسم 40 حرفًا.")
        val payload = jsonObject {
            put("text_ar", normalizedText)
            val character = characterName.trim().take(100)
            val source = sourceReference.trim().take(160)
            put("character_name", if (character.isBlank()) JSONObject.NULL else character)
            put("source_reference", if (source.isBlank()) JSONObject.NULL else source)
            put("tags", JSONArray(normalizedTags))
            put("status", "pending")
            put("submitted_by", userId)
        }
        auth.socialRest("quotes", "POST", payload.toString())
    }

    suspend fun loadPendingQuotes(): List<AdminQuoteEntry> {
        val rows = jsonArray(auth.socialRest("quotes?status=eq.pending&select=id,text_ar,character_name,source_reference,tags,submitted_by,created_at&order=created_at.asc&limit=100"))
        val raw = (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }
        if (raw.isEmpty()) return emptyList()
        val profiles = profilesById(raw.map { it.optString("submitted_by") }.distinct())
        return raw.map { row ->
            val tagsJson = row.optJSONArray("tags")
            val tags = if (tagsJson == null) emptyList() else (0 until tagsJson.length()).mapNotNull { tagsJson.optString(it).takeIf(String::isNotBlank) }
            val submitter = profiles[row.optString("submitted_by")]
            AdminQuoteEntry(row.optString("id"), row.optString("text_ar"), row.optString("character_name"),
                row.optString("source_reference").takeIf(String::isNotBlank), tags, submitter?.name ?: "عضو", row.optString("created_at"))
        }
    }

    suspend fun reviewQuoteSubmission(quoteId: String, status: String) {
        require(status == "approved" || status == "rejected")
        auth.socialRest("rpc/review_quote_submission", "POST", jsonObject {
            put("p_quote_id", quoteId); put("p_status", status)
        }.toString())
    }

    suspend fun toggleQuoteLike(userId: String, quoteId: String, isLiked: Boolean) {
        if (isLiked) auth.socialRest("quote_likes?quote_id=eq.$quoteId&user_id=eq.$userId", "DELETE")
        else auth.socialRest("quote_likes", "POST", jsonObject { put("quote_id", quoteId); put("user_id", userId) }.toString())
    }

    suspend fun loadProfileRole(userId: String): String {
        val rows = jsonArray(auth.socialRest("profiles?id=eq.$userId&select=role&limit=1"))
        return rows.optJSONObject(0)?.optString("role")?.lowercase(Locale.ROOT) ?: "member"
    }

    suspend fun loadAdminAuditLog(): List<AdminAuditEntry> {
        val rows = jsonArray(auth.socialRest("admin_audit_log?select=id,actor_id,action,target_type,target_id,details,created_at&order=created_at.desc&limit=200"))
        val raw = (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }
        if (raw.isEmpty()) return emptyList()
        val profiles = profilesById(raw.map { it.optString("actor_id") }.distinct())
        return raw.map { row ->
            val actorId = row.optString("actor_id")
            AdminAuditEntry(row.optString("id"), profiles[actorId]?.name ?: "مشرف", row.optString("action"),
                row.optString("target_type"), row.optString("target_id").takeIf(String::isNotBlank),
                row.optJSONObject("details")?.toString().orEmpty(), row.optString("created_at"))
        }
    }

    suspend fun loadAdminMetrics(): AdminMetrics {
        val result = JSONObject(auth.socialRest("rpc/admin_dashboard_metrics", "POST", "{}"))
        return AdminMetrics(result.optInt("users"), result.optInt("posts"), result.optInt("communities"),
            result.optInt("reels"), result.optInt("pending_reports"))
    }

    suspend fun loadAdminReports(pendingOnly: Boolean = true): List<AdminReport> {
        val rows = jsonArray(auth.socialRest("rpc/admin_list_reports", "POST", jsonObject { put("p_pending_only", pendingOnly) }.toString()))
        return (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }.map { row ->
            AdminReport(
                id = row.optString("report_id"), postId = row.optString("post_id"),
                reason = row.optString("reason"), reportedAt = row.optString("reported_at"),
                status = row.optString("status"), moderatorNote = row.optString("moderator_note").takeIf(String::isNotBlank),
                postContent = row.optString("post_content").takeIf(String::isNotBlank),
                postAuthorName = row.optString("post_author_name").takeIf(String::isNotBlank)
            )
        }
    }

    suspend fun reviewPostReport(reportId: String, status: String, note: String? = null) {
        require(status == "resolved" || status == "dismissed") { "حالة البلاغ غير صالحة." }
        auth.socialRest("rpc/review_post_report", "POST", jsonObject {
            put("p_report_id", reportId); put("p_status", status); put("p_note", note ?: JSONObject.NULL)
        }.toString())
    }

    suspend fun reportPost(userId: String, postId: String, reason: String) {
        auth.socialRest("post_reports", "POST", jsonObject { put("post_id", postId); put("reporter_id", userId); put("reason", reason) }.toString())
    }

    suspend fun deletePost(userId: String, postId: String) {
        auth.socialRest("posts?id=eq.$postId&author_id=eq.$userId", "DELETE")
    }

    suspend fun votePoll(userId: String, pollId: String, optionId: String) {
        auth.socialRest("poll_votes", "POST", jsonObject { put("poll_id", pollId); put("option_id", optionId); put("user_id", userId) }.toString())
    }

    suspend fun loadStories(): List<StoryItem> {
        val rows = jsonArray(auth.socialRest("stories?select=id,author_id,image_url,caption,created_at&expires_at=gt.${queryEncode(nowIso())}&order=created_at.desc&limit=60"))
        val raw = (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }
        val profiles = profilesById(raw.map { it.optString("author_id") }.distinct())
        return raw.map { row ->
            val profile = profiles[row.optString("author_id")]
            StoryItem(row.optString("id"), row.optString("author_id"), profile?.name ?: "عضو", profile?.username.orEmpty(), profile?.avatarUrl,
                row.optString("image_url"), row.optString("caption"), row.optString("created_at"))
        }
    }

    suspend fun createStory(userId: String, imageUrl: String, caption: String) {
        val expires = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).format(Date(System.currentTimeMillis() + 86_400_000L))
        val payload = jsonObject { put("author_id", userId); put("image_url", imageUrl); put("caption", caption.trim()); put("expires_at", expires) }
        auth.socialRest("stories", "POST", payload.toString())
    }

    suspend fun loadCommunities(userId: String, category: String = "all", query: String = ""): List<CommunityItem> {
        val filterParts = mutableListOf<String>()
        if (category == "joined") {
            val own = jsonArray(auth.socialRest("community_members?user_id=eq.$userId&select=community_id&limit=500"))
            val ids = (0 until own.length()).mapNotNull { own.optJSONObject(it)?.optString("community_id") }
            if (ids.isEmpty()) return emptyList()
            filterParts += "id=${inClause(ids)}"
        } else if (category != "all") filterParts += "category=eq.$category"
        if (query.isNotBlank()) filterParts += "or=(name.ilike.*${queryEncode(query)}*,description.ilike.*${queryEncode(query)}*)"
        val suffix = filterParts.joinToString("&").let { if (it.isBlank()) "" else "&$it" }
        val rows = jsonArray(auth.socialRest("communities?select=id,name,slug,description,cover_url,category,created_by&order=created_at.desc&limit=100$suffix"))
        val raw = (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }
        if (raw.isEmpty()) return emptyList()
        val ids = raw.map { it.optString("id") }
        val memberRows = jsonArray(auth.socialRest("community_members?community_id=${inClause(ids)}&select=community_id,user_id&limit=5000"))
        val counts = mutableMapOf<String, Int>()
        val joined = mutableSetOf<String>()
        for (i in 0 until memberRows.length()) memberRows.optJSONObject(i)?.let { member ->
            val cid = member.optString("community_id")
            counts[cid] = (counts[cid] ?: 0) + 1
            if (member.optString("user_id") == userId) joined += cid
        }
        return raw.map { row ->
            val id = row.optString("id")
            CommunityItem(id, row.optString("name"), row.optString("slug"), row.optString("description"),
                row.optString("cover_url").takeIf(String::isNotBlank), row.optString("category", "general"), counts[id] ?: 0,
                id in joined, row.optString("created_by"))
        }
    }

    suspend fun joinCommunity(userId: String, communityId: String, joined: Boolean) {
        if (joined) auth.socialRest("community_members?community_id=eq.$communityId&user_id=eq.$userId", "DELETE")
        else auth.socialRest("community_members", "POST", jsonObject { put("community_id", communityId); put("user_id", userId) }.toString())
    }

    suspend fun createCommunity(userId: String, name: String, description: String, category: String): String {
        val slug = name.trim().lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\u0600-\\u06ff]+"), "-").trim('-')
            .take(50).ifBlank { "community-${UUID.randomUUID().toString().take(8)}" }
        val body = jsonObject { put("name", name.trim()); put("slug", slug); put("description", description.trim()); put("category", category); put("created_by", userId) }
        val raw = jsonArray(auth.socialRest("communities", "POST", body.toString(), prefer = "return=representation"))
        val id = raw.optJSONObject(0)?.optString("id") ?: throw AuthException("تعذّر إنشاء المجتمع. حاول مرة ثانية.")
        joinCommunity(userId, id, false)
        return id
    }

    suspend fun loadProfileStats(userId: String): UserStats {
        val gamification = jsonArray(auth.socialRest("user_gamification?user_id=eq.$userId&select=xp,level,active_frame_id&limit=1")).optJSONObject(0) ?: JSONObject()
        val level = gamification.optInt("level", 1)
        val xp = gamification.optInt("xp", 0)
        val levelRows = jsonArray(auth.socialRest("levels?select=level,min_xp,title_ar&order=level.asc&limit=100"))
        var title = "مبتدئ"
        var nextXp = maxOf(xp + 100, 100)
        for (i in 0 until levelRows.length()) levelRows.optJSONObject(i)?.let { row ->
            if (row.optInt("level") == level) title = row.optString("title_ar", title)
            if (row.optInt("level") > level && (nextXp <= xp || row.optInt("min_xp") < nextXp)) nextXp = row.optInt("min_xp")
        }
        val achievements = jsonArray(auth.socialRest("user_achievements?user_id=eq.$userId&select=achievement_id&limit=500"))
        val postRows = jsonArray(auth.socialRest("posts?author_id=eq.$userId&select=id&limit=1000"))
        val trackerRows = jsonArray(auth.socialRest("tracker_entries?user_id=eq.$userId&select=id&limit=1000"))
        val allAchievements = jsonArray(auth.socialRest("achievements?select=id&limit=1000"))
        return UserStats(xp, level, title, nextXp, achievements.length(), allAchievements.length(), postRows.length(), trackerRows.length(),
            gamification.optString("active_frame_id").takeIf(String::isNotBlank))
    }

    suspend fun loadAchievements(userId: String): List<AchievementItem> {
        val all = jsonArray(auth.socialRest("achievements?select=id,name_ar,description_ar,category,xp_reward&order=category.asc&limit=200"))
        val mineRows = jsonArray(auth.socialRest("user_achievements?user_id=eq.$userId&select=achievement_id,unlocked_at&limit=500"))
        val mine = (0 until mineRows.length()).mapNotNull { mineRows.optJSONObject(it)?.let { row -> row.optString("achievement_id") to row.optString("unlocked_at") } }.toMap()
        return (0 until all.length()).mapNotNull { all.optJSONObject(it) }.map { row ->
            val id = row.optString("id")
            AchievementItem(id, row.optString("name_ar"), row.optString("description_ar"), row.optString("category"), row.optInt("xp_reward"), mine[id])
        }
    }

    suspend fun loadFrames(userId: String, level: Int, activeFrameId: String?): List<FrameItem> {
        val rows = jsonArray(auth.socialRest("avatar_frames?select=id,name_ar,name_en,rarity,unlock_type,unlock_value,image_url&order=unlock_value.asc.nullsfirst&limit=100"))
        val mineRows = jsonArray(auth.socialRest("user_frames?user_id=eq.$userId&select=frame_id&limit=200"))
        val mine = (0 until mineRows.length()).mapNotNull { mineRows.optJSONObject(it)?.optString("frame_id") }.toSet()
        return (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }.map { row ->
            val id = row.optString("id")
            val unlockType = row.optString("unlock_type")
            val unlocked = unlockType == "default" || id in mine || (unlockType == "level" && level >= row.optInt("unlock_value", Int.MAX_VALUE))
            FrameItem(id, row.optString("name_ar"), row.optString("name_en"), row.optString("rarity"), unlockType,
                row.optInt("unlock_value"), row.optString("image_url").takeIf(String::isNotBlank), unlocked, id == activeFrameId)
        }
    }

    suspend fun equipFrame(frameId: String) {
        auth.socialRest("rpc/equip_avatar_frame", "POST", jsonObject { put("p_frame_id", frameId) }.toString())
    }

    suspend fun loadWatchlist(userId: String): List<WatchEntry> {
        val rows = jsonArray(auth.socialRest("tracker_entries?user_id=eq.$userId&select=id,anime_id,status,progress,score&order=updated_at.desc&limit=500"))
        val raw = (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }
        if (raw.isEmpty()) return emptyList()
        val animes = animeByIds(raw.map { it.optString("anime_id") }.distinct()).associateBy { it.id }
        return raw.mapNotNull { row ->
            val anime = animes[row.optString("anime_id")] ?: return@mapNotNull null
            WatchEntry(anime, row.optString("status"), row.optInt("progress"), if (row.isNull("score")) null else row.optInt("score"), row.optString("id"))
        }
    }

    suspend fun saveTrackerEntry(userId: String, animeId: String, status: String, progress: Int, score: Int?) {
        val payload = jsonObject { put("user_id", userId); put("anime_id", animeId); put("status", status); put("progress", progress); put("score", score ?: JSONObject.NULL) }
        auth.socialRest("tracker_entries?on_conflict=user_id,anime_id", "POST", payload.toString(), prefer = "resolution=merge-duplicates,return=minimal")
    }

    suspend fun deleteTrackerEntry(userId: String, animeId: String) {
        auth.socialRest("tracker_entries?user_id=eq.$userId&anime_id=eq.$animeId", "DELETE")
    }

    suspend fun searchProfiles(query: String): List<PublicProfileResult> {
        if (query.isBlank()) return emptyList()
        val q = queryEncode(query)
        val rows = jsonArray(auth.socialRest("profiles?select=id,display_name,username,avatar_url,bio&or=(username.ilike.*$q*,display_name.ilike.*$q*)&limit=20"))
        return (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }.map { row ->
            PublicProfileResult(row.optString("id"), row.optString("display_name"), row.optString("username"), row.optString("avatar_url").takeIf(String::isNotBlank), row.optString("bio"))
        }
    }

    suspend fun loadPublicProfile(profileId: String): PublicProfileResult? {
        val row = jsonArray(auth.socialRest("profiles?id=eq.$profileId&select=id,display_name,username,avatar_url,bio&limit=1")).optJSONObject(0)
            ?: return null
        return PublicProfileResult(row.optString("id"), row.optString("display_name"), row.optString("username"),
            row.optString("avatar_url").takeIf(String::isNotBlank), row.optString("bio"))
    }

    suspend fun toggleFollow(userId: String, targetUserId: String, following: Boolean) {
        if (userId == targetUserId) return
        if (following) auth.socialRest("follows?follower_id=eq.$userId&following_id=eq.$targetUserId", "DELETE")
        else auth.socialRest("follows", "POST", jsonObject { put("follower_id", userId); put("following_id", targetUserId) }.toString())
    }

    suspend fun isFollowing(userId: String, targetUserId: String): Boolean {
        val rows = jsonArray(auth.socialRest("follows?follower_id=eq.$userId&following_id=eq.$targetUserId&select=follower_id&limit=1"))
        return rows.length() > 0
    }

    suspend fun loadNotifications(userId: String): List<UserNotification> {
        val rows = jsonArray(auth.socialRest("notifications?recipient_id=eq.$userId&select=id,actor_id,kind,post_id,created_at,read_at&order=created_at.desc&limit=100"))
        val raw = (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }
        val profiles = profilesById(raw.map { it.optString("actor_id") }.distinct())
        return raw.map { row ->
            val actor = profiles[row.optString("actor_id")]
            UserNotification(row.optString("id"), row.optString("actor_id"), actor?.name ?: "عضو", actor?.avatarUrl, row.optString("kind"),
                row.optString("post_id").takeIf(String::isNotBlank), row.optString("created_at"), !row.isNull("read_at"))
        }
    }

    suspend fun markNotificationRead(notificationId: String) {
        auth.socialRest("notifications?id=eq.$notificationId", "PATCH", jsonObject { put("read_at", nowIso()) }.toString())
    }

    suspend fun loadConversations(userId: String): List<ChatSummary> {
        val mineRows = jsonArray(auth.socialRest("conversation_members?user_id=eq.$userId&select=conversation_id,last_read_at&limit=500"))
        val mine = (0 until mineRows.length()).mapNotNull { mineRows.optJSONObject(it) }.associateBy { it.optString("conversation_id") }
        if (mine.isEmpty()) return emptyList()
        val ids = mine.keys.toList()
        val convRows = jsonArray(auth.socialRest("conversations?id=${inClause(ids)}&select=id,created_by&limit=500"))
        val memberRows = jsonArray(auth.socialRest("conversation_members?conversation_id=${inClause(ids)}&select=conversation_id,user_id&limit=2000"))
        val membersByConversation = mutableMapOf<String, MutableList<String>>()
        for (i in 0 until memberRows.length()) memberRows.optJSONObject(i)?.let { row ->
            membersByConversation.getOrPut(row.optString("conversation_id")) { mutableListOf() } += row.optString("user_id")
        }
        val allUserIds = membersByConversation.values.flatten().distinct()
        val profiles = profilesById(allUserIds)
        val messageRows = jsonArray(auth.socialRest("messages?conversation_id=${inClause(ids)}&select=id,conversation_id,sender_id,body,created_at&order=created_at.desc&limit=1000"))
        val messagesByConversation = mutableMapOf<String, MutableList<JSONObject>>()
        for (i in 0 until messageRows.length()) messageRows.optJSONObject(i)?.let { row ->
            messagesByConversation.getOrPut(row.optString("conversation_id")) { mutableListOf() } += row
        }
        return (0 until convRows.length()).mapNotNull { convRows.optJSONObject(it) }.map { conv ->
            val id = conv.optString("id")
            val members = membersByConversation[id].orEmpty()
            val others = members.filter { it != userId }
            val profileNames = others.mapNotNull { profiles[it]?.name }
            val lastRead = mine[id]?.optString("last_read_at").orEmpty()
            val messages = messagesByConversation[id].orEmpty()
            val last = messages.firstOrNull()
            val unread = messages.count { it.optString("sender_id") != userId && it.optString("created_at") > lastRead && lastRead.isNotBlank() }
            ChatSummary(id, profileNames.joinToString(", ").ifBlank { "محادثة جديدة" }, others.firstOrNull()?.let { profiles[it]?.avatarUrl },
                last?.optString("body").orEmpty(), last?.optString("created_at").orEmpty(), unread, others.firstOrNull())
        }.sortedByDescending { it.updatedAt }
    }

    suspend fun loadMessages(conversationId: String): List<ChatMessage> {
        val rows = jsonArray(auth.socialRest("messages?conversation_id=eq.$conversationId&select=id,sender_id,body,created_at&order=created_at.asc&limit=500"))
        val raw = (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }
        val profiles = profilesById(raw.map { it.optString("sender_id") }.distinct())
        return raw.map { row ->
            val sender = profiles[row.optString("sender_id")]
            ChatMessage(row.optString("id"), row.optString("sender_id"), sender?.name ?: "عضو", sender?.avatarUrl, row.optString("body"), row.optString("created_at"))
        }
    }

    suspend fun sendMessage(userId: String, conversationId: String, body: String) {
        auth.socialRest("messages", "POST", jsonObject { put("conversation_id", conversationId); put("sender_id", userId); put("body", body.trim()) }.toString())
    }

    suspend fun markConversationRead(conversationId: String) {
        auth.socialRest("rpc/mark_conversation_read", "POST", jsonObject { put("cid", conversationId) }.toString())
    }

    suspend fun createDirectConversation(userId: String, otherUserId: String): String {
        loadConversations(userId).firstOrNull { it.otherUserId == otherUserId }?.let { return it.id }
        val result = jsonArray(auth.socialRest("conversations", "POST", jsonObject { put("created_by", userId) }.toString(), prefer = "return=representation"))
        val conversationId = result.optJSONObject(0)?.optString("id") ?: throw AuthException("تعذّر بدء المحادثة.")
        val members = JSONArray().put(jsonObject { put("conversation_id", conversationId); put("user_id", userId) })
            .put(jsonObject { put("conversation_id", conversationId); put("user_id", otherUserId) })
        auth.socialRest("conversation_members", "POST", members.toString())
        return conversationId
    }

    suspend fun loadReels(userId: String): List<ReelItem> {
        val rows = jsonArray(auth.socialRest("reels?select=id,author_id,video_url,caption,created_at&order=created_at.desc&limit=50"))
        val raw = (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }
        if (raw.isEmpty()) return emptyList()
        val ids = raw.map { it.optString("id") }
        val authors = raw.map { it.optString("author_id") }.distinct()
        val profiles = profilesById(authors)
        val likes = jsonArray(auth.socialRest("reel_likes?reel_id=${inClause(ids)}&select=reel_id,user_id&limit=5000"))
        val comments = jsonArray(auth.socialRest("reel_comments?reel_id=${inClause(ids)}&select=reel_id&limit=5000"))
        val follows = if (authors.isEmpty()) JSONArray() else jsonArray(auth.socialRest("follows?follower_id=eq.$userId&following_id=${inClause(authors)}&select=following_id&limit=500"))
        val counts = mutableMapOf<String, Int>()
        val liked = mutableSetOf<String>()
        for (i in 0 until likes.length()) likes.optJSONObject(i)?.let { row ->
            val id = row.optString("reel_id"); counts[id] = (counts[id] ?: 0) + 1
            if (row.optString("user_id") == userId) liked += id
        }
        val commentCounts = mutableMapOf<String, Int>()
        for (i in 0 until comments.length()) comments.optJSONObject(i)?.optString("reel_id")?.let { commentCounts[it] = (commentCounts[it] ?: 0) + 1 }
        val following = (0 until follows.length()).mapNotNull { follows.optJSONObject(it)?.optString("following_id") }.toSet()
        return raw.map { row ->
            val id = row.optString("id"); val authorId = row.optString("author_id"); val profile = profiles[authorId]
            ReelItem(id, authorId, profile?.name ?: "عضو", profile?.username.orEmpty(), profile?.avatarUrl,
                row.optString("video_url"), row.optString("caption"), row.optString("created_at"), counts[id] ?: 0,
                commentCounts[id] ?: 0, id in liked, authorId in following)
        }
    }

    suspend fun createReel(userId: String, videoUrl: String, caption: String) {
        auth.socialRest("reels", "POST", jsonObject { put("author_id", userId); put("video_url", videoUrl); put("caption", caption.trim()) }.toString())
    }

    suspend fun toggleReelLike(userId: String, reelId: String, isLiked: Boolean) {
        if (isLiked) auth.socialRest("reel_likes?reel_id=eq.$reelId&user_id=eq.$userId", "DELETE")
        else auth.socialRest("reel_likes", "POST", jsonObject { put("reel_id", reelId); put("user_id", userId) }.toString())
    }

    suspend fun loadReelComments(reelId: String): List<SocialComment> = loadComments("reel_comments", "reel_id", reelId, "author_id")

    suspend fun addReelComment(userId: String, reelId: String, body: String) {
        auth.socialRest("reel_comments", "POST", jsonObject { put("reel_id", reelId); put("author_id", userId); put("body", body.trim()) }.toString())
    }

    suspend fun uploadImage(userId: String, uri: Uri, bucket: String = "post-images"): String {
        val bytes = withContext(Dispatchers.IO) { compressImage(uri) }
        if (bytes.size > 8 * 1024 * 1024) throw AuthException("حجم الصورة أكبر من المسموح بعد الضغط.")
        val path = "$userId/${UUID.randomUUID()}.jpg"
        return auth.uploadSocialObject(bucket, path, bytes, "image/jpeg")
    }

    suspend fun uploadVideo(userId: String, uri: Uri): String {
        val contentType = appContext.contentResolver.getType(uri)?.takeIf { it.startsWith("video/") }
            ?: throw AuthException("اختر ملف فيديو صالحًا.")
        val bytes = withContext(Dispatchers.IO) {
            appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw AuthException("تعذّر قراءة ملف الفيديو.")
        }
        if (bytes.size > 50 * 1024 * 1024) throw AuthException("الحد الأعلى لحجم المقطع 50 ميغابايت.")
        val ext = when (contentType.lowercase()) { "video/webm" -> "webm"; "video/quicktime" -> "mov"; else -> "mp4" }
        val path = "$userId/${UUID.randomUUID()}.$ext"
        return auth.uploadSocialObject("reel-videos", path, bytes, contentType)
    }

    suspend fun searchAll(userId: String, query: String): SocialSearchResults {
        val q = query.trim()
        if (q.isBlank()) return SocialSearchResults(emptyList(), emptyList(), emptyList(), emptyList())
        val anime = loadAnime(q).take(12)
        val people = searchProfiles(q)
        val communities = loadCommunities(userId, "all", q).take(12)
        val posts = loadFeed(userId).filter { it.content.contains(q, true) }.take(12)
        return SocialSearchResults(anime, people, communities, posts)
    }

    private suspend fun loadComments(table: String, filterName: String, id: String, authorField: String): List<SocialComment> {
        val rows = jsonArray(auth.socialRest("$table?$filterName=eq.$id&select=id,$authorField,body,created_at&order=created_at.asc&limit=500"))
        val raw = (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }
        val profiles = profilesById(raw.map { it.optString(authorField) }.distinct())
        return raw.map { row ->
            val authorId = row.optString(authorField)
            val profile = profiles[authorId]
            SocialComment(row.optString("id"), authorId, profile?.name ?: "عضو", profile?.username.orEmpty(), profile?.avatarUrl,
                row.optString("body"), row.optString("created_at"))
        }
    }

    private suspend fun profilesById(ids: List<String>): Map<String, PublicProfile> {
        if (ids.isEmpty()) return emptyMap()
        val rows = jsonArray(auth.socialRest("profiles?id=${inClause(ids)}&select=id,display_name,username,avatar_url&limit=500"))
        return (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }.associate { row ->
            val id = row.optString("id")
            id to PublicProfile(id, row.optString("display_name").ifBlank { "عضو" }, row.optString("username"), row.optString("avatar_url").takeIf(String::isNotBlank))
        }
    }

    private suspend fun animeByIds(ids: List<String>): List<AnimeItem> {
        if (ids.isEmpty()) return emptyList()
        val rows = jsonArray(auth.socialRest("anime?id=${inClause(ids)}&select=id,title_ar,title_en,poster_url,episodes_count,format,status,score&limit=200"))
        return (0 until rows.length()).mapNotNull { rows.optJSONObject(it)?.let(::animeFrom) }
    }

    private fun animeFrom(row: JSONObject) = AnimeItem(
        id = row.optString("id"), titleArabic = row.optString("title_ar"), titleEnglish = row.optString("title_en"),
        posterUrl = row.optString("poster_url").takeIf(String::isNotBlank), episodeCount = row.optInt("episodes_count"),
        format = row.optString("format"), status = row.optString("status"), score = row.optDouble("score", 0.0)
    )

    private fun compressImage(uri: Uri): ByteArray {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw AuthException("تعذّر قراءة الصورة.")
        var sample = 1
        while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
        val bitmap = resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
            ?: throw AuthException("تعذّر فتح الصورة.")
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 86, output)
        bitmap.recycle()
        return output.toByteArray()
    }

    private fun nowIso(): String = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).format(Date())
}

fun timeAgo(value: String): String {
    val patterns = listOf("yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    val date = patterns.firstNotNullOfOrNull { pattern -> runCatching { SimpleDateFormat(pattern, Locale.US).parse(value) }.getOrNull() } ?: return "حديثًا"
    val seconds = ((System.currentTimeMillis() - date.time) / 1000).coerceAtLeast(0)
    return when {
        seconds < 60 -> "الآن"
        seconds < 3600 -> "منذ ${seconds / 60} د"
        seconds < 86_400 -> "منذ ${seconds / 3600} س"
        else -> "منذ ${seconds / 86_400} ي"
    }
}
