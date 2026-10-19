package com.animeblack.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.animeblack.app.auth.AchievementItem
import com.animeblack.app.auth.AnimeItem
import com.animeblack.app.auth.AuthException
import com.animeblack.app.auth.AuthUser
import com.animeblack.app.auth.ChatMessage
import com.animeblack.app.auth.ChatSummary
import com.animeblack.app.auth.CommunityItem
import com.animeblack.app.auth.FrameItem
import com.animeblack.app.auth.ReelItem
import com.animeblack.app.auth.SocialComment
import com.animeblack.app.auth.SocialPost
import com.animeblack.app.auth.SocialRepository
import com.animeblack.app.auth.SocialSearchResults
import com.animeblack.app.auth.StoryItem
import com.animeblack.app.auth.UserNotification
import com.animeblack.app.auth.UserStats
import com.animeblack.app.auth.WatchEntry
import com.animeblack.app.auth.timeAgo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Black = Color(0xFF050608)
private val Canvas = Color(0xFF090B12)
private val Panel = Color(0xFF10131D)
private val Card = Color(0xFF131622)
private val Raised = Color(0xFF1A1E2E)
private val Neon = Color(0xFF00E5FF)
private val Violet = Color(0xFF7C4DFF)
private val Gold = Color(0xFFFFB300)
private val White = Color(0xFFF8FAFC)
private val Secondary = Color(0xFF94A3B8)
private val Tertiary = Color(0xFF64748B)
private val Danger = Color(0xFFFF5368)
private val Good = Color(0xFF00D889)

private enum class MainTab { HOME, COMMUNITY, CHAT, REELS, MORE }
private enum class SocialPage {
    HOME, COMMUNITY, CHAT, REELS, MORE, CREATE_POST, SEARCH, NOTIFICATIONS,
    COMMUNITY_CREATE, COMMUNITY_DETAIL, CHAT_NEW, CHAT_ROOM, POST_COMMENTS,
    REEL_COMMENTS, REEL_CREATE, STORE, WATCHLIST, ACHIEVEMENTS, PRIVACY,
    STORY_VIEWER, PUBLIC_PROFILE
}

@Composable
fun AnimeBlackSocialApp(
    repository: SocialRepository,
    user: AuthUser,
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("anime_black_ui", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var page by rememberSaveable { mutableStateOf(SocialPage.HOME) }
    var tab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var feedFilter by rememberSaveable { mutableStateOf("all") }
    var communityFilter by rememberSaveable { mutableStateOf("all") }
    var communityQuery by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var watchSearch by rememberSaveable { mutableStateOf("") }
    var newChatQuery by rememberSaveable { mutableStateOf("") }
    var selectedCommunityId by rememberSaveable { mutableStateOf<String?>(null) }
    var postingCommunityId by rememberSaveable { mutableStateOf<String?>(null) }
    var createSessionId by rememberSaveable { mutableIntStateOf(0) }
    var draftPostType by rememberSaveable { mutableStateOf("general") }
    var draftPostSpoiler by rememberSaveable { mutableStateOf(false) }
    var selectedChatId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPost by remember { mutableStateOf<SocialPost?>(null) }
    var selectedReel by remember { mutableStateOf<ReelItem?>(null) }
    var selectedStory by remember { mutableStateOf<StoryItem?>(null) }
    var selectedProfile by remember { mutableStateOf<com.animeblack.app.auth.PublicProfileResult?>(null) }
    var profileFollowing by remember { mutableStateOf(false) }
    var feed by remember { mutableStateOf<List<SocialPost>>(emptyList()) }
    var stories by remember { mutableStateOf<List<StoryItem>>(emptyList()) }
    var communities by remember { mutableStateOf<List<CommunityItem>>(emptyList()) }
    var chats by remember { mutableStateOf<List<ChatSummary>>(emptyList()) }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var reels by remember { mutableStateOf<List<ReelItem>>(emptyList()) }
    var comments by remember { mutableStateOf<List<SocialComment>>(emptyList()) }
    var watchlist by remember { mutableStateOf<List<WatchEntry>>(emptyList()) }
    var animeChoices by remember { mutableStateOf<List<AnimeItem>>(emptyList()) }
    var stats by remember { mutableStateOf<UserStats?>(null) }
    var achievements by remember { mutableStateOf<List<AchievementItem>>(emptyList()) }
    var frames by remember { mutableStateOf<List<FrameItem>>(emptyList()) }
    var notifications by remember { mutableStateOf<List<UserNotification>>(emptyList()) }
    var searchResults by remember { mutableStateOf(SocialSearchResults(emptyList(), emptyList(), emptyList(), emptyList())) }
    var peopleResults by remember { mutableStateOf<List<com.animeblack.app.auth.PublicProfileResult>>(emptyList()) }
    var pageBusy by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var hideSpoilers by rememberSaveable { mutableStateOf(prefs.getBoolean("hide_spoilers", true)) }
    var autoPlayReels by rememberSaveable { mutableStateOf(prefs.getBoolean("autoplay_reels", false)) }
    var pureBlack by rememberSaveable { mutableStateOf(prefs.getBoolean("pure_black", false)) }
    var modalReasonPost by remember { mutableStateOf<SocialPost?>(null) }

    fun homePageForTab(value: MainTab): SocialPage = when (value) {
        MainTab.HOME -> SocialPage.HOME
        MainTab.COMMUNITY -> SocialPage.COMMUNITY
        MainTab.CHAT -> SocialPage.CHAT
        MainTab.REELS -> SocialPage.REELS
        MainTab.MORE -> SocialPage.MORE
    }
    fun navigateTab(value: MainTab) { tab = value; page = homePageForTab(value); notice = null }
    fun startCreatePost(type: String = "general", containsSpoiler: Boolean = false) {
        draftPostType = type
        draftPostSpoiler = containsSpoiler
        animeChoices = emptyList()
        createSessionId++
        page = SocialPage.CREATE_POST
    }
    fun runAction(action: suspend () -> Unit, after: () -> Unit = {}) {
        if (pageBusy) return
        pageBusy = true
        notice = null
        scope.launch {
            try {
                action()
                refresh++
                after()
            } catch (error: Exception) {
                notice = (error as? AuthException)?.message ?: error.message ?: "تعذّر تنفيذ الطلب. حاول مرة أخرى."
            } finally { pageBusy = false }
        }
    }

    BackHandler(enabled = page != homePageForTab(tab)) { page = homePageForTab(tab) }

    LaunchedEffect(page, tab, feedFilter, communityFilter, communityQuery, searchQuery, newChatQuery, watchSearch, selectedCommunityId, selectedChatId, refresh) {
        if ((page == SocialPage.SEARCH && searchQuery.isNotBlank()) ||
            (page == SocialPage.CHAT_NEW && newChatQuery.isNotBlank()) ||
            (page == SocialPage.COMMUNITY && communityQuery.isNotBlank()) ||
            (page == SocialPage.WATCHLIST && watchSearch.isNotBlank())) delay(350)
        loading = true
        notice = null
        try {
            when (page) {
                SocialPage.HOME -> {
                    feed = repository.loadFeed(user.id, feedFilter)
                    stories = repository.loadStories()
                    communities = repository.loadCommunities(user.id).take(12)
                    stats = repository.loadProfileStats(user.id)
                }
                SocialPage.COMMUNITY -> communities = repository.loadCommunities(user.id, communityFilter, communityQuery)
                SocialPage.COMMUNITY_DETAIL -> {
                    communities = repository.loadCommunities(user.id)
                    feed = repository.loadFeed(user.id, "all", selectedCommunityId)
                }
                SocialPage.CHAT -> chats = repository.loadConversations(user.id)
                SocialPage.CHAT_ROOM -> {
                    selectedChatId?.let {
                        messages = repository.loadMessages(it)
                        repository.markConversationRead(it)
                    }
                }
                SocialPage.REELS -> reels = repository.loadReels(user.id)
                SocialPage.MORE -> stats = repository.loadProfileStats(user.id)
                SocialPage.SEARCH -> searchResults = repository.searchAll(user.id, searchQuery)
                SocialPage.NOTIFICATIONS -> notifications = repository.loadNotifications(user.id)
                SocialPage.CHAT_NEW -> peopleResults = if (newChatQuery.isBlank()) emptyList() else repository.searchProfiles(newChatQuery)
                SocialPage.POST_COMMENTS -> selectedPost?.let { comments = repository.loadPostComments(it.id) }
                SocialPage.REEL_COMMENTS -> selectedReel?.let { comments = repository.loadReelComments(it.id) }
                SocialPage.STORE -> {
                    stats = repository.loadProfileStats(user.id)
                    val current = stats ?: UserStats(0, 1, "مبتدئ", 100, 0, 0, 0, 0, null)
                    frames = repository.loadFrames(user.id, current.level, current.currentFrameId)
                }
                SocialPage.WATCHLIST -> {
                    watchlist = repository.loadWatchlist(user.id)
                    animeChoices = repository.loadAnime(watchSearch).take(30)
                }
                SocialPage.ACHIEVEMENTS -> {
                    stats = repository.loadProfileStats(user.id)
                    achievements = repository.loadAchievements(user.id)
                }
                SocialPage.PUBLIC_PROFILE -> selectedProfile?.let { }
                else -> Unit
            }
        } catch (error: Exception) {
            notice = (error as? AuthException)?.message ?: "تعذّر تحميل البيانات. تحقق من الاتصال ثم أعد المحاولة."
        } finally { loading = false }
    }

    LaunchedEffect(page, selectedProfile?.id, user.id, refresh) {
        if (page == SocialPage.PUBLIC_PROFILE && selectedProfile != null) {
            profileFollowing = runCatching { repository.isFollowing(user.id, selectedProfile!!.id) }.getOrDefault(false)
        }
    }

    LaunchedEffect(page, selectedChatId, refresh) {
        if (page == SocialPage.CHAT_ROOM && selectedChatId != null) {
            while (true) {
                delay(8_000)
                runCatching { messages = repository.loadMessages(selectedChatId!!) }
            }
        }
    }

    val onBack = { page = homePageForTab(tab) }
    val background = if (pureBlack) Black else Canvas
    val bottomVisible = page in setOf(SocialPage.HOME, SocialPage.COMMUNITY, SocialPage.CHAT, SocialPage.REELS, SocialPage.MORE)

    Scaffold(
        containerColor = background,
        snackbarHost = { hostState -> SnackbarHost(hostState) },
        bottomBar = {
            if (bottomVisible) SocialBottomBar(tab, ::navigateTab)
        }
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
            Column(Modifier.fillMaxSize()) {
                notice?.let { NoticeBanner(it, onDismiss = { notice = null }) }
                when (page) {
                    SocialPage.HOME -> HomePage(
                        user, stats, stories, communities, feed, feedFilter, loading, pageBusy, hideSpoilers,
                        onSearch = { page = SocialPage.SEARCH },
                        onNotifications = { page = SocialPage.NOTIFICATIONS },
                        onProfile = { navigateTab(MainTab.MORE) },
                        onCreatePost = { type, spoiler -> postingCommunityId = null; startCreatePost(type, spoiler) },
                        onCreateStory = { selectedStory = null; page = SocialPage.STORY_VIEWER },
                        onViewStory = { selectedStory = it; page = SocialPage.STORY_VIEWER },
                        onCommunity = { selectedCommunityId = it.id; page = SocialPage.COMMUNITY_DETAIL },
                        onExploreCommunities = { navigateTab(MainTab.COMMUNITY) },
                        onFilter = { feedFilter = it },
                        onLike = { post -> runAction({ repository.togglePostLike(user.id, post.id, post.isLiked) }) },
                        onSave = { post -> runAction({ repository.togglePostSave(user.id, post.id, post.isSaved) }) },
                        onComments = { selectedPost = it; page = SocialPage.POST_COMMENTS },
                        onShare = { shareText(context, "Anime Black · ${it.content}") },
                        onMore = { modalReasonPost = it },
                        onVote = { post, option -> post.pollId?.let { poll -> runAction({ repository.votePoll(user.id, poll, option) }) } },
                        onAddAnime = { post -> post.animeId?.let { id -> runAction({ repository.saveTrackerEntry(user.id, id, "planning", 0, null) }, after = { notice = "أُضيف العمل إلى قائمة المتابعة." }) } },
                        onQuest = { postingCommunityId = null; startCreatePost() },
                        onRefresh = { refresh++ }
                    )
                    SocialPage.COMMUNITY -> CommunityPage(
                        communities, user.id, communityFilter, communityQuery, loading, pageBusy,
                        onQuery = { communityQuery = it }, onFilter = { communityFilter = it },
                        onCreate = { page = SocialPage.COMMUNITY_CREATE },
                        onOpen = { selectedCommunityId = it.id; page = SocialPage.COMMUNITY_DETAIL },
                        onJoin = { cm -> runAction({ repository.joinCommunity(user.id, cm.id, cm.isJoined) }) },
                        onRefresh = { refresh++ }
                    )
                    SocialPage.COMMUNITY_DETAIL -> {
                        val community = communities.firstOrNull { it.id == selectedCommunityId }
                        CommunityDetailPage(community, feed, user.id, hideSpoilers, loading, pageBusy,
                            onBack = onBack,
                            onJoin = { cm -> runAction({ repository.joinCommunity(user.id, cm.id, cm.isJoined) }) },
                            onCreatePost = { postingCommunityId = selectedCommunityId; startCreatePost() },
                            onPostComments = { selectedPost = it; page = SocialPage.POST_COMMENTS },
                            onPostLike = { post -> runAction({ repository.togglePostLike(user.id, post.id, post.isLiked) }) },
                            onPostSave = { post -> runAction({ repository.togglePostSave(user.id, post.id, post.isSaved) }) },
                            onShare = { shareText(context, "Anime Black · ${it.content}") },
                            onMore = { modalReasonPost = it },
                            onVote = { post, option -> post.pollId?.let { id -> runAction({ repository.votePoll(user.id, id, option) }) } }
                        )
                    }
                    SocialPage.CHAT -> ChatListPage(chats, loading, pageBusy,
                        onNewChat = { newChatQuery = ""; page = SocialPage.CHAT_NEW },
                        onOpen = { selectedChatId = it.id; page = SocialPage.CHAT_ROOM },
                        onRefresh = { refresh++ }
                    )
                    SocialPage.CHAT_NEW -> NewChatPage(peopleResults, newChatQuery, loading, pageBusy,
                        onBack = onBack, onQuery = { newChatQuery = it },
                        onPerson = { person ->
                            if (person.id == user.id) { selectedProfile = person; page = SocialPage.PUBLIC_PROFILE }
                            else runAction({ selectedChatId = repository.createDirectConversation(user.id, person.id) }, after = { page = SocialPage.CHAT_ROOM })
                        }
                    )
                    SocialPage.CHAT_ROOM -> ChatRoomPage(
                        chats.firstOrNull { it.id == selectedChatId }?.title ?: "محادثة Anime Black",
                        messages, user.id, loading, pageBusy,
                        onBack = onBack, onSend = { text -> runAction({ repository.sendMessage(user.id, selectedChatId ?: return@runAction, text) }) },
                        onRefresh = { refresh++ }
                    )
                    SocialPage.REELS -> ReelsPage(reels, loading, pageBusy, autoPlayReels,
                        onCreate = { page = SocialPage.REEL_CREATE },
                        onLike = { reel -> runAction({ repository.toggleReelLike(user.id, reel.id, reel.isLiked) }) },
                        onComments = { selectedReel = it; page = SocialPage.REEL_COMMENTS },
                        onShare = { shareText(context, "Anime Black · ${it.caption}\n${it.videoUrl}") },
                        onFollow = { reel -> runAction({ repository.toggleFollow(user.id, reel.authorId, reel.isFollowing) }) },
                        onRefresh = { refresh++ }
                    )
                    SocialPage.MORE -> MorePage(user, stats, pageBusy,
                        onEditProfile = onEditProfile,
                        onStore = { page = SocialPage.STORE },
                        onWatchlist = { watchSearch = ""; page = SocialPage.WATCHLIST },
                        onAchievements = { page = SocialPage.ACHIEVEMENTS },
                        onTheme = {
                            pureBlack = !pureBlack; prefs.edit().putBoolean("pure_black", pureBlack).apply()
                        },
                        pureBlack = pureBlack,
                        onPrivacy = { page = SocialPage.PRIVACY },
                        onSignOut = onSignOut
                    )
                    SocialPage.CREATE_POST -> CreatePostPage(
                        sessionKey = createSessionId,
                        initialType = draftPostType,
                        initialSpoiler = draftPostSpoiler,
                        animeChoices = animeChoices,
                        busy = pageBusy,
                        community = communities.firstOrNull { it.id == postingCommunityId },
                        onBack = onBack,
                        onLoadAnime = { query ->
                            if (query.length < 2) animeChoices = emptyList()
                            else runAction({ animeChoices = repository.loadAnime(query) })
                        },
                        onSubmit = { content, type, topic, imageUri, spoiler, spoilerLabel, animeId, rating, polls, communityId ->
                            runAction({
                                val imageUrl = imageUri?.let { repository.uploadImage(user.id, it) }
                                repository.publishPost(content, type, topic, imageUrl, spoiler = spoiler,
                                    spoilerLabel = spoilerLabel, animeId = animeId, rating = rating,
                                    pollOptions = polls, communityId = communityId)
                            }, after = { page = if (communityId != null) SocialPage.COMMUNITY_DETAIL else SocialPage.HOME; tab = if (communityId != null) MainTab.COMMUNITY else MainTab.HOME })
                        }
                    )
                    SocialPage.SEARCH -> SearchPage(searchQuery, searchResults, hideSpoilers, loading, pageBusy,
                        onBack = onBack, onQuery = { searchQuery = it },
                        onOpenPerson = { selectedProfile = it; page = SocialPage.PUBLIC_PROFILE },
                        onOpenCommunity = { selectedCommunityId = it.id; page = SocialPage.COMMUNITY_DETAIL },
                        onAddAnime = { anime -> runAction({ repository.saveTrackerEntry(user.id, anime.id, "planning", 0, null) }, after = { notice = "أُضيف ${anime.displayTitle} إلى قائمة المتابعة." }) },
                        onOpenPost = { selectedPost = it; page = SocialPage.POST_COMMENTS }
                    )
                    SocialPage.NOTIFICATIONS -> NotificationsPage(notifications, loading, pageBusy, onBack,
                        onOpen = { notification ->
                            runAction({
                                if (!notification.isRead) repository.markNotificationRead(notification.id)
                                if (notification.postId != null) {
                                    selectedPost = repository.loadFeed(user.id).firstOrNull { it.id == notification.postId }
                                }
                            }, after = {
                                if (notification.postId != null && selectedPost != null) page = SocialPage.POST_COMMENTS
                            })
                        }, onRefresh = { refresh++ })
                    SocialPage.POST_COMMENTS -> CommentsPage("التعليقات", selectedPost?.content.orEmpty(), hideSpoilers && selectedPost?.containsSpoiler == true, comments, user.id, loading, pageBusy,
                        onBack, onSend = { text -> selectedPost?.let { post -> runAction({ repository.addPostComment(user.id, post.id, text) }) } },
                        onRefresh = { refresh++ }
                    )
                    SocialPage.REEL_COMMENTS -> CommentsPage("تعليقات المقطع", selectedReel?.caption.orEmpty(), false, comments, user.id, loading, pageBusy,
                        onBack, onSend = { text -> selectedReel?.let { reel -> runAction({ repository.addReelComment(user.id, reel.id, text) }) } },
                        onRefresh = { refresh++ }
                    )
                    SocialPage.REEL_CREATE -> ReelCreatePage(pageBusy, onBack,
                        onSubmit = { uri, caption ->
                            runAction({ val url = repository.uploadVideo(user.id, uri); repository.createReel(user.id, url, caption) }, after = { page = SocialPage.REELS })
                        }
                    )
                    SocialPage.COMMUNITY_CREATE -> CommunityCreatePage(pageBusy, onBack,
                        onCreate = { name, description, category ->
                            runAction({ selectedCommunityId = repository.createCommunity(user.id, name, description, category) }, after = { page = SocialPage.COMMUNITY_DETAIL })
                        }
                    )
                    SocialPage.STORE -> StorePage(frames, stats, loading, pageBusy, onBack,
                        onEquip = { frame -> runAction({ repository.equipFrame(frame.id) }, after = { notice = "تم تحديث إطار الملف الشخصي." }) })
                    SocialPage.WATCHLIST -> WatchlistPage(watchlist, animeChoices, watchSearch, loading, pageBusy, onBack,
                        onSearch = { watchSearch = it },
                        onAdd = { anime -> runAction({ repository.saveTrackerEntry(user.id, anime.id, "planning", 0, null) }) },
                        onStatus = { entry, status -> runAction({ repository.saveTrackerEntry(user.id, entry.anime.id, status, entry.progress, entry.score) }) },
                        onProgress = { entry, delta -> runAction({ repository.saveTrackerEntry(user.id, entry.anime.id, entry.status, (entry.progress + delta).coerceAtLeast(0), entry.score) }) },
                        onDelete = { entry -> runAction({ repository.deleteTrackerEntry(user.id, entry.anime.id) }) }
                    )
                    SocialPage.ACHIEVEMENTS -> AchievementsPage(achievements, stats, loading, onBack)
                    SocialPage.PRIVACY -> PrivacyPage(hideSpoilers, autoPlayReels, pureBlack, onBack,
                        onHideSpoilers = { hideSpoilers = it; prefs.edit().putBoolean("hide_spoilers", it).apply() },
                        onAutoPlay = { autoPlayReels = it; prefs.edit().putBoolean("autoplay_reels", it).apply() },
                        onTheme = { pureBlack = it; prefs.edit().putBoolean("pure_black", it).apply() }
                    )
                    SocialPage.STORY_VIEWER -> StoryViewerPage(selectedStory, stories, pageBusy, onBack,
                        onPick = { uri, caption -> runAction({ val url = repository.uploadImage(user.id, uri); repository.createStory(user.id, url, caption) }, after = { page = SocialPage.HOME }) })
                    SocialPage.PUBLIC_PROFILE -> PublicProfilePage(selectedProfile, user.id, profileFollowing, pageBusy, onBack,
                        onFollow = { person -> runAction({ repository.toggleFollow(user.id, person.id, profileFollowing) }, after = { profileFollowing = !profileFollowing }) },
                        onMessage = { person -> runAction({ selectedChatId = repository.createDirectConversation(user.id, person.id) }, after = { page = SocialPage.CHAT_ROOM }) }
                    )
                }
            }
            if (loading && page in setOf(SocialPage.HOME, SocialPage.COMMUNITY, SocialPage.CHAT, SocialPage.REELS, SocialPage.MORE) &&
                feed.isEmpty() && communities.isEmpty() && chats.isEmpty() && reels.isEmpty()) {
                CircularProgressIndicator(color = Neon, modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    modalReasonPost?.let { post ->
        PostOptionsDialog(
            post = post,
            isMine = post.authorId == user.id,
            onDismiss = { modalReasonPost = null },
            onSave = { modalReasonPost = null; runAction({ repository.togglePostSave(user.id, post.id, post.isSaved) }) },
            onReport = { reason -> modalReasonPost = null; runAction({ repository.reportPost(user.id, post.id, reason) }, after = { notice = "تم إرسال البلاغ للمراجعة." }) },
            onDelete = { modalReasonPost = null; runAction({ repository.deletePost(user.id, post.id) }) }
        )
    }
}

@Composable
private fun SocialBottomBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    val items = listOf(
        Triple(MainTab.HOME, "الرئيسية", Icons.Outlined.Home),
        Triple(MainTab.COMMUNITY, "المجتمع", Icons.Outlined.Groups),
        Triple(MainTab.CHAT, "الدردشة", Icons.Outlined.ChatBubbleOutline),
        Triple(MainTab.REELS, "المقاطع", Icons.Outlined.Movie),
        Triple(MainTab.MORE, "المزيد", Icons.Outlined.MoreHoriz)
    )
    NavigationBar(containerColor = Color(0xF20D111B), tonalElevation = 0.dp) {
        items.forEach { (tab, title, icon) ->
            NavigationBarItem(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                icon = { Icon(icon, contentDescription = title) },
                label = { Text(title, fontSize = 10.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = Neon, selectedTextColor = Neon,
                    unselectedIconColor = Secondary, unselectedTextColor = Secondary, indicatorColor = Neon.copy(alpha = .12f))
            )
        }
    }
}

@Composable
private fun HomePage(
    user: AuthUser, stats: UserStats?, stories: List<StoryItem>, communities: List<CommunityItem>, posts: List<SocialPost>,
    filter: String, loading: Boolean, busy: Boolean, hideSpoilers: Boolean,
    onSearch: () -> Unit, onNotifications: () -> Unit, onProfile: () -> Unit, onCreatePost: (String, Boolean) -> Unit,
    onCreateStory: () -> Unit, onViewStory: (StoryItem) -> Unit, onCommunity: (CommunityItem) -> Unit,
    onExploreCommunities: () -> Unit, onFilter: (String) -> Unit, onLike: (SocialPost) -> Unit,
    onSave: (SocialPost) -> Unit, onComments: (SocialPost) -> Unit, onShare: (SocialPost) -> Unit,
    onMore: (SocialPost) -> Unit, onVote: (SocialPost, String) -> Unit, onAddAnime: (SocialPost) -> Unit,
    onQuest: () -> Unit, onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(painter = androidx.compose.ui.res.painterResource(R.drawable.anime_black_logo), contentDescription = "Anime Black",
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)))
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text("ANIME BLACK", color = White, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Text("مجتمع الأنمي الذي يشبهك", color = Secondary, fontSize = 11.sp)
                }
                IconAction(Icons.Outlined.Add, "إنشاء منشور", { onCreatePost("general", false) })
                IconAction(Icons.Outlined.Search, "بحث", onSearch)
                IconAction(Icons.Outlined.NotificationsNone, "الإشعارات", onNotifications)
                Box(Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onProfile), contentAlignment = Alignment.Center) {
                    Avatar(user.avatarUrl, user.displayName, Modifier.fillMaxSize())
                }
            }
        }
        item {
            Surface(Modifier.fillMaxWidth().clickable(onClick = onSearch), color = Panel, shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, Color(0x223C526A))) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Search, null, tint = Secondary, modifier = Modifier.size(18.dp))
                    Text("ابحث عن أنمي أو عضو أو مجتمع", color = Tertiary, fontSize = 13.sp, modifier = Modifier.padding(start = 9.dp))
                }
            }
        }
        item { StoriesStrip(user, stories, onCreateStory, onViewStory) }
        item { UserStatusCard(stats, onQuest, onRefresh) }
        item { CreatePostCard(user, onCreatePost) }
        item {
            SectionHeading("المجتمعات", "اكتشف مساحاتك المفضلة", "استكشف الكل", onExploreCommunities)
            Spacer(Modifier.height(9.dp))
            if (communities.isEmpty()) EmptyInline("لا توجد مجتمعات بعد. كن أول من ينشئ مساحة جديدة.", Icons.Outlined.Groups)
            else LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(communities, key = { it.id }) { CommunityMiniCard(it, onOpen = { onCommunity(it) }) }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filters = listOf("all" to "لك", "following" to "أتابعهم", "latest" to "الأحدث", "trending" to "الأكثر تفاعلًا", "reviews" to "المراجعات", "theories" to "النظريات")
                items(filters) { (key, label) -> FilterChip(selected = filter == key, onClick = { onFilter(key) }, label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.copy(alpha = .16f), selectedLabelColor = Neon,
                        containerColor = Panel, labelColor = Secondary), border = FilterChipDefaults.filterChipBorder(enabled = true, selected = filter == key,
                        borderColor = Color(0x333C526A), selectedBorderColor = Neon.copy(alpha = .55f))) }
            }
        }
        if (loading && posts.isEmpty()) item { LoadingPanel("جارٍ تحميل الخلاصة") }
        if (!loading && posts.isEmpty()) item { EmptyState("لا توجد منشورات في هذه الخلاصة", "ابدأ النقاش أو تابع المجتمعات ليظهر لك محتوى أكثر.", Icons.Outlined.Forum, { onCreatePost("general", false) }, "إنشاء أول منشور") }
        items(posts, key = { it.id }) { post ->
            PostCard(post, user.id, hideSpoilers, busy, onLike, onComments, onSave, onShare, onMore, onVote, onAddAnime)
        }
    }
}

@Composable
private fun StoriesStrip(user: AuthUser, stories: List<StoryItem>, onCreate: () -> Unit, onOpen: (StoryItem) -> Unit) {
    Column {
        SectionHeading("القصص", "لقطات تختفي بعد 24 ساعة", null, null)
        Spacer(Modifier.height(9.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
            item {
                Column(Modifier.width(62.dp).clickable(onClick = onCreate), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                        Surface(color = Panel, shape = CircleShape, border = BorderStroke(1.dp, Neon.copy(alpha = .65f)), modifier = Modifier.fillMaxSize()) {
                            Box(contentAlignment = Alignment.Center) { Avatar(user.avatarUrl, user.displayName, Modifier.fillMaxSize()) }
                        }
                        Surface(color = Neon, shape = CircleShape, modifier = Modifier.align(Alignment.BottomEnd).size(21.dp), border = BorderStroke(2.dp, Canvas)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Add, null, tint = Black, modifier = Modifier.size(15.dp)) }
                        }
                    }
                    Text("قصتي", color = Secondary, fontSize = 10.sp, maxLines = 1, modifier = Modifier.padding(top = 5.dp))
                }
            }
            items(stories.filter { it.authorId != user.id }, key = { it.id }) { story ->
                Column(Modifier.width(62.dp).clickable { onOpen(story) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(58.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Neon, Violet))).padding(2.dp)) {
                        Avatar(story.avatarUrl, story.authorName, Modifier.fillMaxSize().clip(CircleShape))
                    }
                    Text(story.authorName, color = Secondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
                }
            }
        }
    }
}

@Composable
private fun UserStatusCard(stats: UserStats?, onQuest: () -> Unit, onRefresh: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    Surface(
        color = Panel,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color(0x553D61A3)),
        modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF14203B), Panel)), RoundedCornerShape(22.dp))
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("رتبتك الحالية", color = Secondary, fontSize = 11.sp)
                    Text("${stats?.levelTitle ?: "مستكشف"} · المستوى ${stats?.level ?: 1}", color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                IconAction(Icons.Outlined.Refresh, "تحديث الإحصائيات", onRefresh, small = true)
                IconAction(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, if (expanded) "طي الإحصائيات" else "عرض الإحصائيات", { expanded = !expanded }, small = true)
            }
            if (expanded) {
                val xp = stats?.xp ?: 0
                val next = stats?.nextLevelXp?.coerceAtLeast(xp + 1) ?: (xp + 100)
                val start = if ((stats?.level ?: 1) <= 1) 0 else (next - 250).coerceAtLeast(0)
                val progress = ((xp - start).toFloat() / (next - start).coerceAtLeast(1)).coerceIn(0f, 1f)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$xp XP", color = Neon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("$next XP للمستوى التالي", color = Secondary, fontSize = 11.sp)
                }
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = Neon, trackColor = Raised)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("الإنجازات", "${stats?.unlockedAchievements ?: 0}", Icons.Outlined.EmojiEvents, Modifier.weight(1f))
                    StatTile("المنشورات", "${stats?.postCount ?: 0}", Icons.Outlined.Article, Modifier.weight(1f))
                    StatTile("قائمتي", "${stats?.trackedCount ?: 0}", Icons.Outlined.LibraryBooks, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("مهمة المجتمع", color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("شارك منشورًا واكسب نقاط خبرة تلقائيًا", color = Secondary, fontSize = 10.sp)
                    }
                    TextButton(onClick = onQuest) { Text("اكتب منشورًا", color = Neon, fontSize = 11.sp) }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier, color = Color(0x66192032), shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, Color(0x223D526C))) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Neon, modifier = Modifier.size(17.dp))
            Text(value, color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
            Text(label, color = Secondary, fontSize = 9.sp)
        }
    }
}

@Composable
private fun CreatePostCard(user: AuthUser, onOpen: (String, Boolean) -> Unit) {
    Surface(color = Panel, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color(0x223C526A)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(13.dp)) {
            Row(Modifier.fillMaxWidth().clickable { onOpen("general", false) }, verticalAlignment = Alignment.CenterVertically) {
                Avatar(user.avatarUrl, user.displayName, Modifier.size(42.dp))
                Surface(color = Color(0xFF0B0E16), shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Row(Modifier.padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("ما الذي تتابعه اليوم؟", color = Tertiary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.Outlined.Edit, null, tint = Neon, modifier = Modifier.size(17.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                MiniAction(Icons.Outlined.AddPhotoAlternate, "صورة", { onOpen("image", false) }, Modifier.weight(1f))
                MiniAction(Icons.Outlined.StarBorder, "مراجعة", { onOpen("review", false) }, Modifier.weight(1f))
                MiniAction(Icons.Outlined.Poll, "استطلاع", { onOpen("poll", false) }, Modifier.weight(1f))
                MiniAction(Icons.Outlined.VisibilityOff, "حرق", { onOpen("general", true) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiniAction(icon: ImageVector, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(horizontal = 3.dp, vertical = 4.dp)) {
        Icon(icon, null, tint = if (text == "مراجعة") Gold else Neon, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = Secondary, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String, action: String?, onAction: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (subtitle.isNotBlank()) Text(subtitle, color = Tertiary, fontSize = 10.sp)
        }
        if (action != null && onAction != null) TextButton(onClick = onAction, contentPadding = PaddingValues(4.dp)) {
            Text(action, color = Neon, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CommunityMiniCard(item: CommunityItem, onOpen: () -> Unit) {
    Surface(Modifier.width(235.dp).clickable(onClick = onOpen), color = Card, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color(0x223C526A))) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(38.dp), color = Violet.copy(alpha = .18f), shape = RoundedCornerShape(12.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Groups, null, tint = Neon, modifier = Modifier.size(20.dp)) }
                }
                Column(Modifier.weight(1f).padding(start = 9.dp)) {
                    Text(item.name, color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${item.memberCount} عضو", color = Secondary, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(item.description.ifBlank { "مساحة للنقاش حول الأنمي والمانجا." }, color = Secondary, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
            Spacer(Modifier.height(8.dp))
            Text(if (item.isJoined) "عضو في المجتمع" else "استكشف المجتمع", color = if (item.isJoined) Good else Neon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PostCard(
    post: SocialPost, userId: String, hideSpoilers: Boolean, busy: Boolean,
    onLike: (SocialPost) -> Unit, onComments: (SocialPost) -> Unit, onSave: (SocialPost) -> Unit,
    onShare: (SocialPost) -> Unit, onMore: (SocialPost) -> Unit, onVote: (SocialPost, String) -> Unit,
    onAddAnime: (SocialPost) -> Unit
) {
    var revealed by rememberSaveable(post.id) { mutableStateOf(false) }
    Surface(color = Card, shape = RoundedCornerShape(21.dp), border = BorderStroke(1.dp, Color(0x2236475B)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(post.avatarUrl, post.authorName, Modifier.size(42.dp))
                Column(Modifier.weight(1f).padding(start = 9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(post.authorName, color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (post.kind == "review") Text("مراجع", color = Gold, fontSize = 9.sp, modifier = Modifier.padding(start = 7.dp))
                    }
                    Text("@${post.username} · ${timeAgo(post.createdAt)} · ${topicLabel(post.topic, post.kind)}", color = Tertiary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { onMore(post) }, enabled = !busy) { Icon(Icons.Outlined.MoreVert, "خيارات المنشور", tint = Secondary) }
            }
            if (post.content.isNotBlank()) {
                if (post.containsSpoiler && hideSpoilers && !revealed) {
                    Text("تم إخفاء نص المنشور لتجنب حرق الأحداث.", color = Secondary, fontSize = 12.sp)
                } else {
                    Text(post.content, color = White, fontSize = 13.sp, lineHeight = 21.sp)
                }
            }
            if (post.containsSpoiler && (hideSpoilers && !revealed)) {
                Surface(color = Danger.copy(alpha = .1f), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Danger.copy(alpha = .35f)), modifier = Modifier.fillMaxWidth().clickable { revealed = true }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.VisibilityOff, null, tint = Danger, modifier = Modifier.size(19.dp))
                        Column(Modifier.weight(1f).padding(start = 9.dp)) {
                            Text(post.spoilerLabel ?: "محتوى يتضمن حرقًا", color = Danger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("اضغط لإظهار التفاصيل", color = Secondary, fontSize = 10.sp)
                        }
                        Icon(Icons.Outlined.ExpandMore, null, tint = Danger)
                    }
                }
            } else if (post.containsSpoiler && !hideSpoilers) {
                Text("يتضمن هذا المنشور حرقًا للأحداث", color = Danger, fontSize = 10.sp)
            }
            if (post.imageUrl != null && (!post.containsSpoiler || !hideSpoilers || revealed)) {
                AsyncImage(post.imageUrl, contentDescription = "صورة مرفقة", modifier = Modifier.fillMaxWidth().heightIn(max = 310.dp).clip(RoundedCornerShape(16.dp)))
            }
            if (post.videoUrl != null && (!post.containsSpoiler || !hideSpoilers || revealed)) InlineVideo(post.videoUrl, Modifier.fillMaxWidth().height(250.dp))
            if (post.kind == "poll" && post.pollOptions.isNotEmpty()) PollBlock(post, busy, onVote)
            if (post.kind == "review" && post.rating != null) RatingBadge(post.rating)
            if (post.animeTitle != null) {
                Surface(color = Panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Movie, null, tint = Violet, modifier = Modifier.size(22.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 9.dp)) {
                            Text(post.animeTitle, color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("عمل مرتبط بالمنشور", color = Tertiary, fontSize = 9.sp)
                        }
                        TextButton(onClick = { onAddAnime(post) }, enabled = !busy) { Text("أضف للقائمة", color = Neon, fontSize = 10.sp) }
                    }
                }
            }
            HorizontalDivider(color = Color(0x2236475B))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ActionCount(if (post.isLiked) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, "${post.likeCount}", if (post.isLiked) Danger else Secondary, !busy) { onLike(post) }
                ActionCount(Icons.Outlined.ChatBubbleOutline, "${post.commentCount}", Secondary, !busy) { onComments(post) }
                ActionCount(Icons.Outlined.Share, "مشاركة", Secondary, !busy) { onShare(post) }
                IconButton(onClick = { onSave(post) }, enabled = !busy) { Icon(if (post.isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, "حفظ المنشور", tint = if (post.isSaved) Neon else Secondary) }
            }
        }
    }
}

@Composable
private fun PollBlock(post: SocialPost, busy: Boolean, onVote: (SocialPost, String) -> Unit) {
    val total = post.pollOptions.sumOf { it.votes }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("استطلاع", color = Neon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        post.pollOptions.forEach { option ->
            val percent = (option.votes * 100) / total
            Surface(color = if (option.isSelected) Neon.copy(alpha = .13f) else Panel, shape = RoundedCornerShape(11.dp),
                border = BorderStroke(1.dp, if (option.isSelected) Neon.copy(alpha = .5f) else Color(0x2236475B)),
                modifier = Modifier.fillMaxWidth().clickable(enabled = !busy && !post.pollEnded && post.pollOptions.none { it.isSelected }) { onVote(post, option.id) }) {
                Column(Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(option.text, color = White, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Text("$percent% · ${option.votes}", color = Secondary, fontSize = 9.sp)
                    }
                    Spacer(Modifier.height(5.dp))
                    LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape), color = Neon, trackColor = Raised)
                }
            }
        }
        Text(if (post.pollEnded) "انتهى الاستطلاع" else "${post.pollOptions.sumOf { it.votes }} صوت · اختر خيارًا للتصويت", color = Tertiary, fontSize = 9.sp)
    }
}

@Composable
private fun RatingBadge(rating: Int) {
    Surface(color = Gold.copy(alpha = .12f), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Gold.copy(alpha = .35f))) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Star, null, tint = Gold, modifier = Modifier.size(15.dp))
            Text("$rating / 10", color = Gold, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(start = 5.dp))
        }
    }
}

@Composable
private fun ActionCount(icon: ImageVector, label: String, tint: Color, enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 5.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
        Text(label, color = tint, fontSize = 10.sp, modifier = Modifier.padding(start = 5.dp))
    }
}

@Composable
private fun CommunityPage(
    communities: List<CommunityItem>, userId: String, filter: String, query: String, loading: Boolean, busy: Boolean,
    onQuery: (String) -> Unit, onFilter: (String) -> Unit, onCreate: () -> Unit,
    onOpen: (CommunityItem) -> Unit, onJoin: (CommunityItem) -> Unit, onRefresh: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        PageTopBar("المجتمعات", null, Icons.Outlined.Refresh, onRefresh) {
            FilledIconButton(onClick = onCreate, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Neon, contentColor = Black)) { Icon(Icons.Outlined.Add, "إنشاء مجتمع") }
        }
        OutlinedSearch(query, onQuery, "ابحث عن مجتمع أو موضوع", Modifier.padding(horizontal = 16.dp, vertical = 7.dp))
        LazyRow(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(listOf("all" to "الكل", "joined" to "مجتمعاتي", "anime" to "أنمي", "studio" to "استوديوهات", "manga" to "مانجا", "shonen" to "شونين", "other" to "أخرى")) { (key, title) ->
                FilterChip(selected = filter == key, onClick = { onFilter(key) }, label = { Text(title) }, colors = chipColors())
            }
        }
        if (loading && communities.isEmpty()) LoadingPanel("جارٍ تحميل المجتمعات")
        if (!loading && communities.isEmpty()) EmptyState("لا توجد نتائج", "جرّب تغيير البحث أو أنشئ مجتمعًا جديدًا.", Icons.Outlined.Groups, onCreate, "إنشاء مجتمع")
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            items(communities, key = { it.id }) { cm ->
                Surface(color = Card, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color(0x2236475B)), modifier = Modifier.fillMaxWidth().clickable { onOpen(cm) }) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(49.dp), color = Violet.copy(alpha = .18f), shape = RoundedCornerShape(14.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Groups, null, tint = Neon) } }
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(cm.name, color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${cm.memberCount} عضو · ${categoryLabel(cm.category)}", color = Secondary, fontSize = 10.sp)
                            }
                            Button(onClick = { onJoin(cm) }, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = if (cm.isJoined) Raised else Neon, contentColor = if (cm.isJoined) White else Black), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 13.dp, vertical = 6.dp)) {
                                Text(if (cm.isJoined) "عضو" else "انضمام", fontSize = 10.sp)
                            }
                        }
                        Text(cm.description.ifBlank { "مجتمع لمشاركة الآراء والنقاشات حول ${cm.name}." }, color = Secondary, fontSize = 11.sp, lineHeight = 18.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text("فتح المجتمع", color = Neon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityDetailPage(
    community: CommunityItem?, posts: List<SocialPost>, userId: String, hideSpoilers: Boolean, loading: Boolean, busy: Boolean,
    onBack: () -> Unit, onJoin: (CommunityItem) -> Unit, onCreatePost: () -> Unit,
    onPostComments: (SocialPost) -> Unit, onPostLike: (SocialPost) -> Unit, onPostSave: (SocialPost) -> Unit,
    onShare: (SocialPost) -> Unit, onMore: (SocialPost) -> Unit, onVote: (SocialPost, String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        PageTopBar(community?.name ?: "المجتمع", onBack, null, null) {
            if (community != null) Button(onClick = { onJoin(community) }, enabled = !busy, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = if (community.isJoined) Raised else Neon, contentColor = if (community.isJoined) White else Black)) {
                Text(if (community.isJoined) "عضو" else "انضمام", fontSize = 11.sp)
            }
        }
        community?.let { cm ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 7.dp)) {
                Text("${cm.memberCount} عضو · ${categoryLabel(cm.category)}", color = Neon, fontSize = 11.sp)
                Text(cm.description.ifBlank { "مساحة لمجتمع ${cm.name}." }, color = Secondary, fontSize = 12.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 5.dp))
                Spacer(Modifier.height(10.dp))
                Button(onClick = onCreatePost, enabled = cm.isJoined, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Black)) {
                    Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text(if (cm.isJoined) "اكتب للمجتمع" else "انضم للنشر")
                }
            }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            if (loading && posts.isEmpty()) item { LoadingPanel("جارٍ تحميل النقاشات") }
            if (!loading && posts.isEmpty()) item { EmptyInline("لا توجد منشورات هنا بعد. ابدأ أول نقاش عند الانضمام.", Icons.Outlined.Forum) }
            items(posts, key = { it.id }) { post -> PostCard(post, userId, hideSpoilers, busy, onPostLike, onPostComments, onPostSave, onShare, onMore, onVote) { } }
        }
    }
}

@Composable
private fun ChatListPage(chats: List<ChatSummary>, loading: Boolean, busy: Boolean, onNewChat: () -> Unit, onOpen: (ChatSummary) -> Unit, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PageTopBar("الدردشات", null, Icons.Outlined.Refresh, onRefresh) {
            FilledIconButton(onClick = onNewChat, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Neon, contentColor = Black)) { Icon(Icons.Outlined.Add, "بدء محادثة") }
        }
        if (loading && chats.isEmpty()) LoadingPanel("جارٍ تحميل محادثاتك")
        if (!loading && chats.isEmpty()) EmptyState("لا توجد محادثات بعد", "ابحث عن أحد أعضاء المجتمع وابدأ محادثة خاصة.", Icons.Outlined.ChatBubbleOutline, onNewChat, "محادثة جديدة")
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(chats, key = { it.id }) { chat ->
                Surface(Modifier.fillMaxWidth().clickable(enabled = !busy) { onOpen(chat) }, color = Panel, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0x2236475B))) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(chat.avatarUrl, chat.title, Modifier.size(48.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(chat.title, color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (chat.updatedAt.isBlank()) "" else timeAgo(chat.updatedAt), color = Tertiary, fontSize = 9.sp)
                            }
                            Text(chat.lastMessage.ifBlank { "ابدأ المحادثة برسالة قصيرة." }, color = if (chat.unreadCount > 0) White else Secondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                        }
                        if (chat.unreadCount > 0) Surface(color = Neon, shape = CircleShape) { Text(chat.unreadCount.toString(), color = Black, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewChatPage(
    people: List<com.animeblack.app.auth.PublicProfileResult>, query: String, loading: Boolean, busy: Boolean,
    onBack: () -> Unit, onQuery: (String) -> Unit, onPerson: (com.animeblack.app.auth.PublicProfileResult) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        PageTopBar("محادثة جديدة", onBack, null, null)
        OutlinedSearch(query, onQuery, "ابحث بالاسم أو اسم المستخدم", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        if (query.isBlank()) EmptyInline("اكتب اسم عضو للبحث عنه وبدء محادثة خاصة.", Icons.Outlined.PersonSearch)
        if (loading) LoadingPanel("جارٍ البحث")
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp)) {
            items(people, key = { it.id }) { person ->
                PersonRow(person.name, person.username, person.avatarUrl, busy, "مراسلة") { onPerson(person) }
            }
        }
    }
}

@Composable
private fun ChatRoomPage(
    title: String, messages: List<ChatMessage>, userId: String, loading: Boolean, busy: Boolean,
    onBack: () -> Unit, onSend: (String) -> Unit, onRefresh: () -> Unit
) {
    var draft by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        PageTopBar(title, onBack, Icons.Outlined.Refresh, onRefresh)
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), reverseLayout = false, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (loading && messages.isEmpty()) item { LoadingPanel("جارٍ تحميل الرسائل") }
            if (!loading && messages.isEmpty()) item { EmptyInline("ابدأ المحادثة برسالة محترمة وواضحة.", Icons.Outlined.ChatBubbleOutline) }
            items(messages, key = { it.id }) { message ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.senderId == userId) Arrangement.Start else Arrangement.End) {
                    Surface(color = if (message.senderId == userId) Color(0xFF17334A) else Panel, shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, Color(0x2236475B)), modifier = Modifier.widthIn(max = 300.dp)) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            if (message.senderId != userId) Text(message.senderName, color = Neon, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(message.body, color = White, fontSize = 12.sp, lineHeight = 18.sp)
                            Text(timeAgo(message.createdAt), color = Tertiary, fontSize = 8.sp, modifier = Modifier.align(Alignment.End).padding(top = 3.dp))
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(Panel).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(draft, { draft = it.take(2000) }, modifier = Modifier.weight(1f), placeholder = { Text("اكتب رسالتك") }, maxLines = 4, shape = RoundedCornerShape(18.dp), colors = fieldColors())
            IconButton(onClick = { if (draft.isNotBlank()) { onSend(draft.trim()); draft = "" } }, enabled = !busy && draft.isNotBlank()) { Icon(Icons.Outlined.Send, "إرسال", tint = Neon) }
        }
    }
}

@Composable
private fun ReelsPage(
    reels: List<ReelItem>, loading: Boolean, busy: Boolean, autoPlay: Boolean,
    onCreate: () -> Unit, onLike: (ReelItem) -> Unit, onComments: (ReelItem) -> Unit,
    onShare: (ReelItem) -> Unit, onFollow: (ReelItem) -> Unit, onRefresh: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        PageTopBar("المقاطع القصيرة", null, Icons.Outlined.Refresh, onRefresh) {
            FilledTonalButton(onClick = onCreate, colors = ButtonDefaults.filledTonalButtonColors(containerColor = Neon.copy(alpha = .15f), contentColor = Neon)) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("رفع مقطع", fontSize = 11.sp)
            }
        }
        if (loading && reels.isEmpty()) LoadingPanel("جارٍ تحميل المقاطع")
        if (!loading && reels.isEmpty()) EmptyState("لا توجد مقاطع بعد", "ارفع أول مقطع قصير إلى مجتمع Anime Black.", Icons.Outlined.Movie, onCreate, "رفع أول مقطع")
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 13.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(reels, key = { it.id }) { reel ->
                Surface(color = Card, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Color(0x2236475B)), modifier = Modifier.fillMaxWidth()) {
                    Column {
                        InlineVideo(reel.videoUrl, Modifier.fillMaxWidth().height(410.dp), autoPlay)
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(reel.avatarUrl, reel.authorName, Modifier.size(37.dp))
                            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                Text(reel.authorName, color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("@${reel.username} · ${timeAgo(reel.createdAt)}", color = Tertiary, fontSize = 9.sp)
                            }
                            TextButton(onClick = { onFollow(reel) }, enabled = !busy) { Text(if (reel.isFollowing) "تتابعه" else "متابعة", color = Neon, fontSize = 10.sp) }
                        }
                        if (reel.caption.isNotBlank()) Text(reel.caption, color = White, fontSize = 12.sp, lineHeight = 19.sp, modifier = Modifier.padding(horizontal = 13.dp).padding(bottom = 9.dp))
                        Row(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 3.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            ActionCount(if (reel.isLiked) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, reel.likeCount.toString(), if (reel.isLiked) Danger else Secondary, !busy) { onLike(reel) }
                            ActionCount(Icons.Outlined.ChatBubbleOutline, reel.commentCount.toString(), Secondary, !busy) { onComments(reel) }
                            ActionCount(Icons.Outlined.Share, "مشاركة", Secondary, !busy) { onShare(reel) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MorePage(
    user: AuthUser, stats: UserStats?, busy: Boolean, onEditProfile: () -> Unit, onStore: () -> Unit,
    onWatchlist: () -> Unit, onAchievements: () -> Unit, onTheme: () -> Unit, pureBlack: Boolean,
    onPrivacy: () -> Unit, onSignOut: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageTopBar("ملفي ومركز الأوتاكو", null, Icons.Outlined.Settings, onPrivacy) }
        item {
            Surface(color = Panel, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Color(0x333D6A97)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(17.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar(user.avatarUrl, user.displayName, Modifier.size(80.dp))
                    Text(user.displayName, color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                    Text("@${user.username.ifBlank { user.email.substringBefore('@') }}", color = Neon, fontSize = 11.sp)
                    Text(user.email, color = Secondary, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
                    Text("${stats?.levelTitle ?: "مستكشف"} · المستوى ${stats?.level ?: 1}", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 9.dp))
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        MiniStat("${stats?.xp ?: 0}", "XP")
                        MiniStat("${stats?.postCount ?: 0}", "منشور")
                        MiniStat("${stats?.trackedCount ?: 0}", "عمل")
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onEditProfile, enabled = !busy, shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Black), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("تعديل الملف الشخصي")
                    }
                }
            }
        }
        item { ActionSetting(Icons.Outlined.Storefront, "إطارات الملف الشخصي", "افتح الإطارات المتاحة بحسب مستواك", onStore) }
        item { ActionSetting(Icons.Outlined.LibraryBooks, "قائمة المتابعة", "أعمالك وحالة المشاهدة والتقدم", onWatchlist) }
        item { ActionSetting(Icons.Outlined.EmojiEvents, "الإنجازات", "إنجازاتك وتقدمك في المجتمع", onAchievements) }
        item { ActionSetting(Icons.Outlined.Settings, "الخصوصية والتفضيلات", "الحرق، تشغيل المقاطع، والمظهر", onPrivacy) }
        item {
            Surface(Modifier.fillMaxWidth().clickable(onClick = onTheme), color = Panel, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0x2236475B))) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.DarkMode, null, tint = Neon)
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) { Text("الوضع الأسود", color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(if (pureBlack) "أسود عميق مفعّل" else "ليلي نيون مفعّل", color = Secondary, fontSize = 10.sp) }
                    Switch(checked = pureBlack, onCheckedChange = { onTheme() })
                }
            }
        }
        item { OutlinedButton(onClick = onSignOut, enabled = !busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)) { Icon(Icons.Outlined.Logout, null); Spacer(Modifier.width(7.dp)); Text("تسجيل الخروج") } }
    }
}

@Composable
private fun MiniStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(label, color = Secondary, fontSize = 10.sp) }
}

@Composable
private fun ActionSetting(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), color = Panel, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0x2236475B))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Neon, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f).padding(horizontal = 11.dp)) { Text(title, color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text(subtitle, color = Secondary, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp)) }
            Icon(Icons.Outlined.ChevronLeft, null, tint = Tertiary)
        }
    }
}

@Composable
private fun CreatePostPage(
    sessionKey: Int, initialType: String, initialSpoiler: Boolean,
    animeChoices: List<AnimeItem>, busy: Boolean, community: CommunityItem?, onBack: () -> Unit,
    onLoadAnime: (String) -> Unit,
    onSubmit: (String, String, String, Uri?, Boolean, String?, String?, Int?, List<String>, String?) -> Unit
) {
    var type by rememberSaveable(sessionKey) { mutableStateOf(initialType) }
    var content by rememberSaveable(sessionKey) { mutableStateOf("") }
    var spoiler by rememberSaveable(sessionKey) { mutableStateOf(initialSpoiler) }
    var spoilerLabel by rememberSaveable(sessionKey) { mutableStateOf("") }
    var rating by rememberSaveable(sessionKey) { mutableFloatStateOf(8.0f) }
    var selectedAnime by rememberSaveable(sessionKey) { mutableStateOf<String?>(null) }
    var animeSearch by rememberSaveable(sessionKey) { mutableStateOf("") }
    var pollOptions by rememberSaveable(sessionKey) { mutableStateOf(listOf("", "")) }
    var imageUri by rememberSaveable(sessionKey) { mutableStateOf<Uri?>(null) }
    var validation by rememberSaveable(sessionKey) { mutableStateOf<String?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { imageUri = it }
    LaunchedEffect(animeSearch, type) {
        if (type == "review" && animeSearch.length >= 2) {
            delay(350)
            onLoadAnime(animeSearch)
        }
    }
    Column(Modifier.fillMaxSize()) {
        PageTopBar("إنشاء منشور", onBack, null, null) {
            Button(onClick = {
                validation = when {
                    content.isBlank() && imageUri == null -> "اكتب نصًا أو أرفق صورة قبل النشر."
                    type == "image" && imageUri == null -> "أرفق صورة لهذا المنشور قبل النشر."
                    content.length > 2000 -> "الحد الأقصى 2000 حرف."
                    type == "review" && selectedAnime == null -> "اختر عملًا لربط المراجعة به."
                    type == "poll" && pollOptions.count { it.isNotBlank() } < 2 -> "أضف خيارين على الأقل للاستطلاع."
                    else -> null
                }
                if (validation == null) onSubmit(content.trim(), if (type == "review") "review" else if (type == "poll") "poll" else if (type == "image" || imageUri != null) "image" else "text",
                    if (type == "theory") "theory" else "general", imageUri, spoiler, spoilerLabel.ifBlank { if (spoiler) "يتضمن هذا المنشور حرقًا" else null },
                    selectedAnime, if (type == "review") rating.toInt() else null, if (type == "poll") pollOptions.filter(String::isNotBlank) else emptyList(), community?.id)
            }, enabled = !busy, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Black), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) { Text(if (busy) "جارٍ النشر" else "نشر", fontSize = 11.sp) }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                if (community != null) InfoCard("النشر داخل ${community.name}", "سيظهر منشورك لأعضاء المجتمع.", Icons.Outlined.Groups)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(listOf("general" to "عام", "image" to "صورة", "review" to "مراجعة", "poll" to "استطلاع", "theory" to "نظرية")) { (key, title) ->
                        FilterChip(selected = type == key, onClick = { type = key }, label = { Text(title) }, colors = chipColors())
                    }
                }
            }
            item {
                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(content, { content = it.take(2100); validation = null }, modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp), label = { Text("ماذا تريد أن تشارك؟") }, placeholder = { Text("اكتب رأيك أو نقاشك باللغة التي تناسبك") }, maxLines = 9, colors = fieldColors(), shape = RoundedCornerShape(18.dp))
                    Text("${content.length} / 2000", color = if (content.length > 2000) Danger else Tertiary, fontSize = 10.sp, modifier = Modifier.align(Alignment.End).padding(top = 5.dp))
                }
            }
            item {
                Text("وسوم سريعة", color = Secondary, fontSize = 10.sp)
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("#نظريات_أنمي", "#تسريبات", "#مراجعة")) { tag -> AssistChip(onClick = { content = (content.trimEnd() + " " + tag).trim(); validation = null }, label = { Text(tag, fontSize = 10.sp) }, colors = AssistChipDefaults.assistChipColors(containerColor = Panel, labelColor = Secondary)) }
                }
            }
            if (type == "review") item {
                InfoCard("تقييم العمل", "اختر عملًا ثم قيّمه من 1 إلى 10.", Icons.Outlined.Star)
                Text("التقييم: ${rating.toInt()} / 10", color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Slider(value = rating, onValueChange = { rating = it }, valueRange = 1f..10f, steps = 8, colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold))
            }
            if (type == "poll") item {
                Text("خيارات الاستطلاع", color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                pollOptions.forEachIndexed { index, option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(option, { value -> pollOptions = pollOptions.toMutableList().also { it[index] = value.take(80) } }, modifier = Modifier.weight(1f), label = { Text("الخيار ${index + 1}") }, singleLine = true, colors = fieldColors(), shape = RoundedCornerShape(13.dp))
                        if (pollOptions.size > 2) IconButton(onClick = { pollOptions = pollOptions.toMutableList().also { it.removeAt(index) } }) { Icon(Icons.Outlined.Close, "حذف الخيار", tint = Danger) }
                    }
                }
                TextButton(onClick = { if (pollOptions.size < 6) pollOptions = pollOptions + "" }, enabled = pollOptions.size < 6) { Icon(Icons.Outlined.Add, null); Text("إضافة خيار") }
            }
            item {
                Text("إرفاق عمل", color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                OutlinedSearch(animeSearch, { animeSearch = it }, "ابحث عن أنمي أو مانجا", Modifier.fillMaxWidth().padding(top = 5.dp))
                if (animeChoices.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                        items(animeChoices.take(12), key = { it.id }) { anime ->
                            FilterChip(selected = selectedAnime == anime.id, onClick = { selectedAnime = if (selectedAnime == anime.id) null else anime.id }, label = { Text(anime.displayTitle, maxLines = 1) }, colors = chipColors())
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.VisibilityOff, null, tint = Danger)
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) { Text("يتضمن حرقًا للأحداث", color = White, fontSize = 12.sp); Text("سيُخفى المحتوى حتى يختار القارئ إظهاره.", color = Secondary, fontSize = 10.sp) }
                    Switch(checked = spoiler, onCheckedChange = { spoiler = it })
                }
                if (spoiler) OutlinedTextField(spoilerLabel, { spoilerLabel = it.take(100) }, modifier = Modifier.fillMaxWidth(), label = { Text("وصف التحذير") }, placeholder = { Text("مثل: حرق الحلقة الأخيرة") }, singleLine = true, colors = fieldColors(), shape = RoundedCornerShape(13.dp))
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("صورة مرفقة", color = White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { imagePicker.launch("image/*") }, shape = RoundedCornerShape(12.dp)) { Icon(Icons.Outlined.AddPhotoAlternate, null); Text("اختيار صورة", modifier = Modifier.padding(start = 5.dp)) }
                }
                imageUri?.let { uri -> AsyncImage(uri, contentDescription = "معاينة الصورة", modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).padding(top = 8.dp).clip(RoundedCornerShape(14.dp))) }
                if (imageUri != null) TextButton(onClick = { imageUri = null }) { Icon(Icons.Outlined.DeleteOutline, null, tint = Danger); Text("إزالة الصورة", color = Danger) }
            }
            validation?.let { item { NoticeBanner(it) {} } }
            item { Text("يجب أن تلتزم المشاركات بإرشادات المجتمع. يمكنك الإبلاغ عن المحتوى المخالف.", color = Tertiary, fontSize = 10.sp, lineHeight = 17.sp) }
        }
    }
}

@Composable
private fun SearchPage(
    query: String, results: SocialSearchResults, hideSpoilers: Boolean, loading: Boolean, busy: Boolean, onBack: () -> Unit,
    onQuery: (String) -> Unit, onOpenPerson: (com.animeblack.app.auth.PublicProfileResult) -> Unit,
    onOpenCommunity: (CommunityItem) -> Unit, onAddAnime: (AnimeItem) -> Unit, onOpenPost: (SocialPost) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        PageTopBar("بحث شامل", onBack, null, null)
        OutlinedSearch(query, onQuery, "أنمي، أعضاء، مجتمعات، منشورات", Modifier.padding(horizontal = 15.dp, vertical = 7.dp))
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (query.isBlank()) item { EmptyInline("اكتب كلمة للبحث في الأنمي والأعضاء والمجتمعات والمنشورات.", Icons.Outlined.Search) }
            if (loading && query.isNotBlank()) item { LoadingPanel("جارٍ البحث") }
            if (results.anime.isNotEmpty()) item { SearchSectionTitle("الأنمي") }
            items(results.anime, key = { "anime-${it.id}" }) { anime -> AnimeSearchRow(anime, busy, onAddAnime) }
            if (results.people.isNotEmpty()) item { SearchSectionTitle("الأعضاء") }
            items(results.people, key = { "person-${it.id}" }) { person -> PersonRow(person.name, "@${person.username}", person.avatarUrl, busy, "عرض") { onOpenPerson(person) } }
            if (results.communities.isNotEmpty()) item { SearchSectionTitle("المجتمعات") }
            items(results.communities, key = { "community-${it.id}" }) { CommunitySearchRow(it) { onOpenCommunity(it) } }
            if (results.posts.isNotEmpty()) item { SearchSectionTitle("منشورات ذات صلة") }
            items(results.posts, key = { "post-${it.id}" }) { post -> Surface(Modifier.fillMaxWidth().clickable { onOpenPost(post) }, color = Card, shape = RoundedCornerShape(14.dp)) { Text(if (hideSpoilers && post.containsSpoiler) "منشور يتضمن حرقًا للأحداث — المحتوى مخفي في نتائج البحث" else post.content, color = White, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(12.dp)) } }
            if (query.isNotBlank() && !loading && results.anime.isEmpty() && results.people.isEmpty() && results.communities.isEmpty() && results.posts.isEmpty()) item { EmptyInline("لم نعثر على نتائج مطابقة. جرّب كلمة أخرى.", Icons.Outlined.Search) }
        }
    }
}

@Composable
private fun NotificationsPage(items: List<UserNotification>, loading: Boolean, busy: Boolean, onBack: () -> Unit, onOpen: (UserNotification) -> Unit, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PageTopBar("الإشعارات", onBack, Icons.Outlined.Refresh, onRefresh)
        if (loading && items.isEmpty()) LoadingPanel("جارٍ تحميل الإشعارات")
        if (!loading && items.isEmpty()) EmptyInline("لا توجد إشعارات جديدة الآن.", Icons.Outlined.NotificationsNone)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id }) { item ->
                Surface(Modifier.fillMaxWidth().clickable(enabled = !busy) { onOpen(item) }, color = if (item.isRead) Panel else Color(0xFF132234), shape = RoundedCornerShape(15.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(item.actorAvatar, item.actorName, Modifier.size(43.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(notificationText(item.kind, item.actorName), color = White, fontSize = 11.sp, lineHeight = 18.sp)
                            Text(timeAgo(item.createdAt), color = Tertiary, fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                        if (!item.isRead) Surface(Modifier.size(8.dp), color = Neon, shape = CircleShape) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentsPage(title: String, postText: String, spoilerHidden: Boolean, comments: List<SocialComment>, userId: String, loading: Boolean, busy: Boolean, onBack: () -> Unit, onSend: (String) -> Unit, onRefresh: () -> Unit) {
    var draft by rememberSaveable { mutableStateOf("") }
    var revealedSpoiler by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        PageTopBar(title, onBack, Icons.Outlined.Refresh, onRefresh)
        if (postText.isNotBlank()) Surface(color = Panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp)) {
            Text(if (spoilerHidden) "تم إخفاء المنشور لتجنب حرق الأحداث." else postText, color = Secondary, fontSize = 11.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(12.dp))
        }
        if (loading && comments.isEmpty()) LoadingPanel("جارٍ تحميل التعليقات")
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (spoilerHidden && !revealedSpoiler) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                        InfoCard("التعليقات مخفية", "قد تتضمن التعليقات حرقًا للأحداث.", Icons.Outlined.VisibilityOff)
                        OutlinedButton(onClick = { revealedSpoiler = true }, shape = RoundedCornerShape(12.dp)) { Text("عرض على مسؤوليتي", color = Danger) }
                    }
                }
            } else {
                if (!loading && comments.isEmpty()) item { EmptyInline("كن أول من يشارك رأيه باحترام.", Icons.Outlined.ChatBubbleOutline) }
                items(comments, key = { it.id }) { comment ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Avatar(comment.avatarUrl, comment.authorName, Modifier.size(35.dp))
                        Column(Modifier.padding(start = 9.dp)) {
                            Surface(color = Panel, shape = RoundedCornerShape(15.dp)) {
                                Column(Modifier.padding(10.dp)) {
                                    Text(comment.authorName, color = Neon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(comment.body, color = White, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                            Text(timeAgo(comment.createdAt), color = Tertiary, fontSize = 8.sp, modifier = Modifier.padding(start = 7.dp, top = 3.dp))
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(Panel).padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(draft, { draft = it.take(1000) }, modifier = Modifier.weight(1f), placeholder = { Text("اكتب تعليقًا") }, maxLines = 3, colors = fieldColors(), shape = RoundedCornerShape(18.dp))
            IconButton(onClick = { if (draft.isNotBlank()) { onSend(draft.trim()); draft = "" } }, enabled = !busy && draft.isNotBlank()) { Icon(Icons.Outlined.Send, "إرسال تعليق", tint = Neon) }
        }
    }
}

@Composable
private fun ReelCreatePage(busy: Boolean, onBack: () -> Unit, onSubmit: (Uri, String) -> Unit) {
    var caption by rememberSaveable { mutableStateOf("") }
    var video by rememberSaveable { mutableStateOf<Uri?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { video = it }
    Column(Modifier.fillMaxSize()) {
        PageTopBar("رفع مقطع", onBack, null, null) {
            Button(onClick = { if (video == null) error = "اختر مقطع فيديو أولًا." else onSubmit(video!!, caption) }, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Black), shape = RoundedCornerShape(12.dp)) { Text(if (busy) "جارٍ الرفع" else "نشر المقطع") }
        }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            InfoCard("المقاطع القصيرة", "ارفع ملف MP4 أو WebM أو MOV بحد أقصى 50 ميغابايت.", Icons.Outlined.Movie)
            OutlinedButton(onClick = { picker.launch("video/*") }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Outlined.VideoLibrary, null); Text(if (video == null) "اختيار فيديو" else "تم اختيار الفيديو", modifier = Modifier.padding(start = 7.dp)) }
            OutlinedTextField(caption, { caption = it.take(300) }, modifier = Modifier.fillMaxWidth(), label = { Text("وصف المقطع") }, maxLines = 4, colors = fieldColors(), shape = RoundedCornerShape(15.dp))
            error?.let { NoticeBanner(it) { error = null } }
        }
    }
}

@Composable
private fun CommunityCreatePage(busy: Boolean, onBack: () -> Unit, onCreate: (String, String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("anime") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        PageTopBar("إنشاء مجتمع", onBack, null, null) {
            Button(onClick = {
                error = when { name.trim().length < 3 -> "اكتب اسمًا من 3 أحرف على الأقل."; description.length > 500 -> "الوصف يجب أن يكون أقصر من 500 حرف."; else -> null }
                if (error == null) onCreate(name.trim(), description.trim(), category)
            }, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Black), shape = RoundedCornerShape(12.dp)) { Text(if (busy) "جارٍ الإنشاء" else "إنشاء") }
        }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoCard("مساحة لها هوية", "اختر اسمًا واضحًا وفئة مناسبة. ستصبح مشرفًا على المجتمع الذي تنشئه.", Icons.Outlined.Groups)
            OutlinedTextField(name, { name = it.take(80); error = null }, modifier = Modifier.fillMaxWidth(), label = { Text("اسم المجتمع") }, singleLine = true, colors = fieldColors(), shape = RoundedCornerShape(15.dp))
            OutlinedTextField(description, { description = it.take(500) }, modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp), label = { Text("وصف المجتمع") }, maxLines = 5, colors = fieldColors(), shape = RoundedCornerShape(15.dp))
            Text("الفئة", color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(listOf("anime" to "أنمي", "studio" to "استوديوهات", "manga" to "مانجا", "shonen" to "شونين", "general" to "عام", "other" to "أخرى")) { (key, label) -> FilterChip(selected = category == key, onClick = { category = key }, label = { Text(label) }, colors = chipColors()) }
            }
            error?.let { NoticeBanner(it) { error = null } }
        }
    }
}

@Composable
private fun StorePage(frames: List<FrameItem>, stats: UserStats?, loading: Boolean, busy: Boolean, onBack: () -> Unit, onEquip: (FrameItem) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PageTopBar("معرض الإطارات", onBack, null, null)
        InfoCard("إطارات تُكتسب بالتقدم", "لا توجد عملات وهمية أو مشتريات. افتح الإطارات برفع مستواك ثم استخدم أحد ما أتيح لك.", Icons.Outlined.Storefront)
        Text("مستواك: ${stats?.level ?: 1} · ${stats?.xp ?: 0} XP", color = Neon, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        if (loading && frames.isEmpty()) LoadingPanel("جارٍ تحميل الإطارات")
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(frames, key = { it.id }) { frame ->
                Surface(color = Card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, if (frame.isActive) Neon.copy(alpha = .6f) else Color(0x2236475B))) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(46.dp), color = rarityColor(frame.rarity).copy(alpha = .15f), shape = CircleShape, border = BorderStroke(1.dp, rarityColor(frame.rarity))) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Person, null, tint = rarityColor(frame.rarity)) }
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(frame.name, color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${frame.nameEnglish} · ${frame.rarity}", color = Secondary, fontSize = 9.sp)
                            Text(if (frame.isUnlocked) "متاح للاستخدام" else "يفتح عند المستوى ${frame.unlockLevel}", color = if (frame.isUnlocked) Good else Tertiary, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
                        }
                        if (frame.isUnlocked) Button(onClick = { onEquip(frame) }, enabled = !busy && !frame.isActive, colors = ButtonDefaults.buttonColors(containerColor = if (frame.isActive) Raised else Neon, contentColor = if (frame.isActive) White else Black), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) { Text(if (frame.isActive) "مستخدم" else "استخدام", fontSize = 9.sp) }
                    }
                }
            }
            if (!loading && frames.isEmpty()) item { EmptyInline("لا توجد إطارات متاحة حاليًا.", Icons.Outlined.Person) }
        }
    }
}

@Composable
private fun WatchlistPage(
    items: List<WatchEntry>, anime: List<AnimeItem>, query: String, loading: Boolean, busy: Boolean, onBack: () -> Unit,
    onSearch: (String) -> Unit, onAdd: (AnimeItem) -> Unit, onStatus: (WatchEntry, String) -> Unit,
    onProgress: (WatchEntry, Int) -> Unit, onDelete: (WatchEntry) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        PageTopBar("قائمة المتابعة", onBack, null, null)
        OutlinedSearch(query, onSearch, "ابحث عن عمل لإضافته", Modifier.padding(horizontal = 14.dp, vertical = 7.dp))
        if (query.isNotBlank()) {
            LazyRow(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(anime, key = { "add-${it.id}" }) { item -> FilterChip(selected = false, onClick = { onAdd(item) }, label = { Text("إضافة ${item.displayTitle}", maxLines = 1) }, colors = chipColors()) }
            }
        }
        if (loading && items.isEmpty()) LoadingPanel("جارٍ تحميل قائمتك")
        if (!loading && items.isEmpty()) EmptyInline("قائمتك فارغة. ابحث عن أنمي أعجبك وأضفه للمتابعة.", Icons.Outlined.LibraryBooks)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(items, key = { it.entryId }) { entry ->
                var menuOpen by remember { mutableStateOf(false) }
                Surface(color = Card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0x2236475B))) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AnimePoster(entry.anime, Modifier.size(width = 54.dp, height = 70.dp))
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(entry.anime.displayTitle, color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${entry.anime.score} / 10 · ${entry.anime.episodeCount} حلقة", color = Secondary, fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                            Box {
                                TextButton(onClick = { menuOpen = true }) { Text(statusLabel(entry.status), color = Neon, fontSize = 10.sp); Icon(Icons.Outlined.ExpandMore, null, tint = Neon) }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    listOf("planning", "watching", "completed", "paused", "dropped").forEach { status -> DropdownMenuItem(text = { Text(statusLabel(status)) }, onClick = { menuOpen = false; onStatus(entry, status) }) }
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("التقدم", color = Secondary, fontSize = 10.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onProgress(entry, -1) }, enabled = !busy && entry.progress > 0) { Text("−", color = Neon, fontSize = 19.sp) }
                            Text("${entry.progress} / ${entry.anime.episodeCount.coerceAtLeast(0)}", color = White, fontSize = 11.sp)
                            IconButton(onClick = { onProgress(entry, 1) }, enabled = !busy && (entry.anime.episodeCount == 0 || entry.progress < entry.anime.episodeCount)) { Icon(Icons.Outlined.Add, "زيادة التقدم", tint = Neon) }
                            IconButton(onClick = { onDelete(entry) }, enabled = !busy) { Icon(Icons.Outlined.DeleteOutline, "إزالة من القائمة", tint = Danger) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementsPage(items: List<AchievementItem>, stats: UserStats?, loading: Boolean, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PageTopBar("الإنجازات", onBack, null, null)
        InfoCard("${stats?.unlockedAchievements ?: 0} من ${stats?.totalAchievements ?: items.size} إنجاز", "كل إنجاز يُفتح تلقائيًا عندما تحقق شرطه داخل التطبيق.", Icons.Outlined.EmojiEvents)
        if (loading && items.isEmpty()) LoadingPanel("جارٍ تحميل الإنجازات")
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id }) { item ->
                Surface(color = if (item.unlockedAt != null) Color(0xFF171B24) else Panel, shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, if (item.unlockedAt != null) Gold.copy(alpha = .32f) else Color(0x2236475B))) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (item.unlockedAt != null) Icons.Outlined.EmojiEvents else Icons.Outlined.Lock, null, tint = if (item.unlockedAt != null) Gold else Tertiary, modifier = Modifier.size(25.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(item.name, color = if (item.unlockedAt != null) White else Secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(item.description.ifBlank { "أنجز المزيد من النشاطات لفتح هذا الإنجاز." }, color = Tertiary, fontSize = 9.sp, lineHeight = 15.sp)
                        }
                        Text(if (item.unlockedAt != null) "مفتوح" else "+${item.xpReward} XP", color = if (item.unlockedAt != null) Good else Neon, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyPage(
    hideSpoilers: Boolean, autoPlay: Boolean, pureBlack: Boolean, onBack: () -> Unit,
    onHideSpoilers: (Boolean) -> Unit, onAutoPlay: (Boolean) -> Unit, onTheme: (Boolean) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        PageTopBar("الخصوصية والتفضيلات", onBack, null, null)
        Text("تتحكم هذه الخيارات بطريقة عرض التطبيق على هذا الجهاز.", color = Secondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        SettingToggle(Icons.Outlined.VisibilityOff, "إخفاء الحرق تلقائيًا", "اعرض تحذيرًا قبل كشف أحداث القصة.", hideSpoilers, onHideSpoilers)
        SettingToggle(Icons.Outlined.PlayArrow, "تشغيل المقاطع تلقائيًا", "استخدم البيانات لتشغيل المقاطع عند فتحها.", autoPlay, onAutoPlay)
        SettingToggle(Icons.Outlined.DarkMode, "خلفية سوداء عميقة", "تغيير درجة خلفية الوضع الليلي.", pureBlack, onTheme)
        InfoCard("البيانات الشخصية", "لا نعرض البريد الإلكتروني ضمن المنشورات. استخدم تعديل الملف لتغيير الاسم أو الصورة أو النبذة.", Icons.Outlined.Lock)
    }
}

@Composable
private fun SettingToggle(icon: ImageVector, title: String, subtitle: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Surface(color = Panel, shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, Color(0x2236475B)), modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Neon)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) { Text(title, color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = Secondary, fontSize = 9.sp, lineHeight = 14.sp) }
            Switch(value, onChange)
        }
    }
}

@Composable
private fun StoryViewerPage(story: StoryItem?, stories: List<StoryItem>, busy: Boolean, onBack: () -> Unit, onPick: (Uri, String) -> Unit) {
    var caption by rememberSaveable { mutableStateOf("") }
    var createMode by rememberSaveable(story?.id) { mutableStateOf(story == null) }
    var newUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { newUri = it }
    Box(Modifier.fillMaxSize().background(Black)) {
        if (createMode) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PageTopBar("إضافة قصة", onBack, null, null)
                InfoCard("قصتك تختفي بعد 24 ساعة", "اختر صورة واكتب وصفًا قصيرًا.", Icons.Outlined.AddPhotoAlternate)
                OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(13.dp)) { Icon(Icons.Outlined.AddPhotoAlternate, null); Text(if (newUri == null) "اختيار صورة" else "تغيير الصورة", modifier = Modifier.padding(start = 6.dp)) }
                newUri?.let { AsyncImage(it, contentDescription = "معاينة القصة", modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp).clip(RoundedCornerShape(18.dp))) }
                OutlinedTextField(caption, { caption = it.take(200) }, modifier = Modifier.fillMaxWidth(), label = { Text("وصف القصة") }, colors = fieldColors(), shape = RoundedCornerShape(14.dp))
                Button(onClick = { if (newUri == null) error = "اختر صورة للقصة." else onPick(newUri!!, caption) }, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Black), shape = RoundedCornerShape(13.dp)) { Text(if (busy) "جارٍ النشر" else "نشر القصة") }
                error?.let { NoticeBanner(it) { error = null } }
            }
        } else if (story != null) {
            AsyncImage(story.imageUrl, contentDescription = "قصة ${story.authorName}", modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xAA000000), Color.Transparent, Color(0xCC000000)))))
            Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(story.avatarUrl, story.authorName, Modifier.size(38.dp))
                Column(Modifier.weight(1f).padding(horizontal = 9.dp)) { Text(story.authorName, color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(timeAgo(story.createdAt), color = Secondary, fontSize = 9.sp) }
                IconButton(onClick = onBack) { Icon(Icons.Outlined.Close, "إغلاق القصة", tint = White) }
            }
            Text(story.caption, color = White, fontSize = 14.sp, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(20.dp))
        }
    }
}

@Composable
private fun PublicProfilePage(
    profile: com.animeblack.app.auth.PublicProfileResult?, currentUserId: String, following: Boolean, busy: Boolean, onBack: () -> Unit,
    onFollow: (com.animeblack.app.auth.PublicProfileResult) -> Unit, onMessage: (com.animeblack.app.auth.PublicProfileResult) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        PageTopBar("الملف العام", onBack, null, null)
        profile?.let { person ->
            Surface(Modifier.fillMaxWidth().padding(14.dp), color = Panel, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color(0x2236475B))) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar(person.avatarUrl, person.name, Modifier.size(80.dp))
                    Text(person.name, color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                    Text("@${person.username}", color = Neon, fontSize = 11.sp)
                    Text(person.bio.ifBlank { "عضو في مجتمع Anime Black." }, color = Secondary, fontSize = 12.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 12.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onFollow(person) }, enabled = !busy && person.id != currentUserId, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Icon(if (following) Icons.Outlined.PersonRemove else Icons.Outlined.PersonAddAlt1, null); Text(if (following) "إلغاء المتابعة" else "متابعة", modifier = Modifier.padding(start = 5.dp)) }
                        Button(onClick = { onMessage(person) }, enabled = !busy && person.id != currentUserId, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Black)) { Icon(Icons.Outlined.ChatBubbleOutline, null); Text("مراسلة", modifier = Modifier.padding(start = 5.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostOptionsDialog(post: SocialPost, isMine: Boolean, onDismiss: () -> Unit, onSave: () -> Unit, onReport: (String) -> Unit, onDelete: () -> Unit) {
    var reportMode by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text(if (reportMode) "الإبلاغ عن المنشور" else "خيارات المنشور", color = White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!reportMode) {
                    TextButton(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Icon(if (post.isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, null); Text(if (post.isSaved) "إزالة من المحفوظات" else "حفظ المنشور", modifier = Modifier.padding(start = 8.dp)) }
                    if (isMine) TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.DeleteOutline, null, tint = Danger); Text("حذف منشوري", color = Danger, modifier = Modifier.padding(start = 8.dp)) }
                    else TextButton(onClick = { reportMode = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.ReportProblem, null, tint = Danger); Text("إبلاغ عن مخالفة", color = Danger, modifier = Modifier.padding(start = 8.dp)) }
                } else listOf("محتوى مزعج أو ترويجي", "إساءة أو تحرش", "حرق أحداث دون تحذير", "معلومات مضللة", "سبب آخر").forEach { reason ->
                    TextButton(onClick = { onReport(reason) }, modifier = Modifier.fillMaxWidth()) { Text(reason, color = White) }
                }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق", color = Neon) } }
    )
}

@Composable
private fun PageTopBar(title: String, onBack: (() -> Unit)?, actionIcon: ImageVector?, onAction: (() -> Unit)?, trailing: (@Composable RowScope.() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().background(Canvas).padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "رجوع", tint = White) }
        Column(Modifier.weight(1f)) { Text(title, color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        if (actionIcon != null && onAction != null) IconButton(onClick = onAction) { Icon(actionIcon, "تحديث", tint = Neon) }
        trailing?.invoke(this)
    }
}

@Composable
private fun OutlinedSearch(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(value, onValueChange, modifier = modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(placeholder, color = Tertiary, fontSize = 11.sp) }, leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Secondary) }, colors = fieldColors(), shape = RoundedCornerShape(15.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = White, unfocusedTextColor = White, focusedBorderColor = Neon, unfocusedBorderColor = Color(0xFF344052),
    focusedLabelColor = Neon, unfocusedLabelColor = Secondary, cursorColor = Neon,
    focusedContainerColor = Panel, unfocusedContainerColor = Panel
)

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.copy(alpha = .16f), selectedLabelColor = Neon, containerColor = Panel, labelColor = Secondary)

@Composable
private fun IconAction(icon: ImageVector, description: String, onClick: () -> Unit, small: Boolean = false) {
    IconButton(onClick = onClick, modifier = Modifier.size(if (small) 34.dp else 38.dp)) { Icon(icon, description, tint = Secondary, modifier = Modifier.size(if (small) 18.dp else 20.dp)) }
}

@Composable
private fun Avatar(url: String?, label: String, modifier: Modifier = Modifier) {
    if (!url.isNullOrBlank()) AsyncImage(url, contentDescription = label, modifier = modifier.clip(CircleShape), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
    else Surface(modifier = modifier.clip(CircleShape), color = BrushColor(label)) {
        Box(contentAlignment = Alignment.Center) { Text(label.firstOrNull()?.uppercase() ?: "A", color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
    }
}

private fun BrushColor(label: String): Color {
    val color = when ((label.hashCode() ushr 1) % 4) { 0 -> Color(0xFF14566A); 1 -> Color(0xFF483278); 2 -> Color(0xFF39523F); else -> Color(0xFF604032) }
    return color
}

@Composable
private fun AnimePoster(anime: AnimeItem, modifier: Modifier = Modifier) {
    if (anime.posterUrl != null) AsyncImage(anime.posterUrl, anime.displayTitle, modifier.clip(RoundedCornerShape(10.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
    else Surface(modifier, color = Violet.copy(alpha = .13f), shape = RoundedCornerShape(10.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Movie, null, tint = Neon) } }
}

@Composable
private fun AnimeSearchRow(anime: AnimeItem, busy: Boolean, onAdd: (AnimeItem) -> Unit) {
    Surface(color = Panel, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color(0x2236475B))) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AnimePoster(anime, Modifier.size(width = 42.dp, height = 56.dp))
            Column(Modifier.weight(1f).padding(horizontal = 9.dp)) { Text(anime.displayTitle, color = White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis); Text("${anime.format} · ${anime.score} / 10", color = Secondary, fontSize = 9.sp) }
            IconButton(onClick = { onAdd(anime) }, enabled = !busy) { Icon(Icons.Outlined.Add, "أضف إلى قائمة المتابعة", tint = Neon) }
        }
    }
}

@Composable
private fun PersonRow(name: String, username: String, avatar: String?, busy: Boolean, actionLabel: String, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(enabled = !busy, onClick = onClick), color = Panel, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(avatar, name, Modifier.size(40.dp))
            Column(Modifier.weight(1f).padding(horizontal = 9.dp)) { Text(name, color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text(username, color = Secondary, fontSize = 9.sp) }
            Text(actionLabel, color = Neon, fontSize = 10.sp)
        }
    }
}

@Composable
private fun CommunitySearchRow(item: CommunityItem, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), color = Panel, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Groups, null, tint = Neon)
            Column(Modifier.weight(1f).padding(horizontal = 9.dp)) { Text(item.name, color = White, fontWeight = FontWeight.Bold, fontSize = 11.sp); Text("${item.memberCount} عضو", color = Secondary, fontSize = 9.sp) }
            Icon(Icons.Outlined.ChevronLeft, null, tint = Tertiary)
        }
    }
}

@Composable
private fun SearchSectionTitle(title: String) {
    Text(title, color = Neon, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun PageTitle(title: String, subtitle: String = "") {
    Column(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 8.dp)) {
        Text(title, color = White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        if (subtitle.isNotBlank()) Text(subtitle, color = Secondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun InfoCard(title: String, body: String, icon: ImageVector) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp), color = Panel, shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, Color(0x2236475B))) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Neon, modifier = Modifier.size(23.dp))
            Column(Modifier.padding(start = 10.dp)) { Text(title, color = White, fontWeight = FontWeight.Bold, fontSize = 11.sp); Text(body, color = Secondary, fontSize = 9.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 4.dp)) }
        }
    }
}

@Composable
private fun EmptyInline(message: String, icon: ImageVector) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Tertiary, modifier = Modifier.size(20.dp))
        Text(message, color = Secondary, fontSize = 10.sp, lineHeight = 16.sp, modifier = Modifier.padding(start = 9.dp))
    }
}

@Composable
private fun EmptyState(title: String, body: String, icon: ImageVector, onAction: () -> Unit, action: String) {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(Modifier.size(60.dp), color = Neon.copy(alpha = .1f), shape = CircleShape) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Neon, modifier = Modifier.size(27.dp)) } }
        Text(title, color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
        Text(body, color = Secondary, fontSize = 11.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 6.dp))
        Button(onClick = onAction, modifier = Modifier.padding(top = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Black), shape = RoundedCornerShape(12.dp)) { Text(action, fontSize = 11.sp) }
    }
}

@Composable
private fun LoadingPanel(label: String) {
    Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(color = Neon, modifier = Modifier.size(17.dp), strokeWidth = 2.dp)
        Text(label, color = Secondary, fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun NoticeBanner(message: String, onDismiss: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp), color = Danger.copy(alpha = .13f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Danger.copy(alpha = .35f))) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, null, tint = Danger, modifier = Modifier.size(17.dp))
            Text(message, color = White, fontSize = 10.sp, modifier = Modifier.weight(1f).padding(horizontal = 7.dp))
            IconButton(onClick = onDismiss, modifier = Modifier.size(25.dp)) { Icon(Icons.Outlined.Close, "إخفاء", tint = Secondary, modifier = Modifier.size(15.dp)) }
        }
    }
}

@Composable
private fun IconActionRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(10.dp)) { Icon(icon, null); Text(title, modifier = Modifier.padding(start = 8.dp)) }
}

@Composable
private fun InlineVideo(url: String, modifier: Modifier = Modifier, autoPlay: Boolean = false) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            playWhenReady = autoPlay
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        factory = { viewContext -> PlayerView(viewContext).apply { this.player = player; useController = true; setShowNextButton(false); setShowPreviousButton(false) } },
        modifier = modifier.clip(RoundedCornerShape(15.dp)),
        update = { it.player = player }
    )
}

private fun topicLabel(topic: String, kind: String): String = when {
    kind == "review" -> "مراجعة"
    kind == "poll" -> "استطلاع"
    topic == "theory" -> "نظرية"
    topic == "question" -> "سؤال"
    else -> "نقاش"
}

private fun categoryLabel(category: String): String = when (category) {
    "anime" -> "أنمي"
    "studio" -> "استوديوهات"
    "manga" -> "مانجا"
    "shonen" -> "شونين"
    "other" -> "أخرى"
    else -> "عام"
}

private fun statusLabel(status: String): String = when (status) {
    "watching" -> "أتابعه"
    "planning" -> "مخطط له"
    "completed" -> "مكتمل"
    "paused" -> "متوقف مؤقتًا"
    "dropped" -> "متروك"
    else -> status
}

private fun notificationText(kind: String, actorName: String): String = when (kind) {
    "comment" -> "$actorName علّق على منشورك."
    "like" -> "$actorName أعجب بمنشورك."
    "follow" -> "$actorName بدأ بمتابعتك."
    else -> "$actorName تفاعل معك في Anime Black."
}

private fun rarityColor(rarity: String): Color = when (rarity) {
    "rare" -> Neon
    "epic" -> Violet
    "legendary" -> Gold
    "mythic" -> Color(0xFFFF4DCE)
    else -> Secondary
}

private fun shareText(context: Context, text: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
        context.startActivity(Intent.createChooser(intent, "مشاركة عبر"))
    }.onFailure {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Anime Black", text))
    }
}
