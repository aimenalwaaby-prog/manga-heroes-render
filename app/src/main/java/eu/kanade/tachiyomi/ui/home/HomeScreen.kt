package eu.kanade.tachiyomi.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Explore
import mihon.icons.materialsymbols.rounded.LocalLibrary
import mihon.icons.materialsymbols.rounded.NewReleases
import mihon.icons.materialsymbols.rounded.Person
import mihon.icons.materialsymbols.rounded.RocketLaunch
import mihon.icons.materialsymbols.rounded.Settings
import soup.compose.material.motion.animation.materialFadeThroughIn
import soup.compose.material.motion.animation.materialFadeThroughOut
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource as coreStringResource

object HomeScreen : Screen() {
    private val librarySearchEvent = Channel<String>()
    private val openTabEvent = Channel<Tab>()
    private val showBottomNavEvent = Channel<Boolean>()

    private val tabs = listOf(
        HeroDashboardTab,
        LibraryTab,
        UpdatesTab,
        BrowseTab,
        HeroAiTab,
        MoreTab,
    )

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        TabNavigator(tab = HeroDashboardTab, key = "MangaHeroesTabs") { tabNavigator ->
            CompositionLocalProvider(LocalNavigator provides navigator) {
                val state = tabNavigator.current
                Scaffold(
                    topBar = { scrollBehavior ->
                        if (state != HeroAiTab) {
                            HeroTopBar(tab = state, scrollBehavior = scrollBehavior)
                        }
                    },
                    bottomBar = {
                        HeroBottomNavigation(
                            tabs = tabs,
                            current = state,
                            onSelect = { tabNavigator.current = it },
                        )
                    },
                    contentWindowInsets = WindowInsets.navigationBars,
                ) { padding ->
                    AnimatedContent(
                        targetState = state,
                        transitionSpec = {
                            materialFadeThroughIn(initialScale = 0.97f, durationMillis = 240) togetherWith
                                materialFadeThroughOut(durationMillis = 180)
                        },
                        modifier = Modifier.padding(padding),
                        label = "heroTabContent",
                    ) { tab ->
                        tabNavigator.saveableState(key = "heroTab", tab) { tab.Content() }
                    }
                }
            }

            BackHandler(enabled = tabNavigator.current != HeroDashboardTab) {
                tabNavigator.current = HeroDashboardTab
            }

            LaunchedEffect(Unit) {
                launch {
                    librarySearchEvent.receiveAsFlow().collectLatest {
                        tabNavigator.current = LibraryTab
                        LibraryTab.search(it)
                    }
                }
                launch {
                    openTabEvent.receiveAsFlow().collectLatest { event ->
                        when (event) {
                            is Tab.Library -> {
                                tabNavigator.current = LibraryTab
                                if (event.mangaIdToOpen != null) navigator.push(MangaScreen(event.mangaIdToOpen))
                            }
                            Tab.Updates -> tabNavigator.current = UpdatesTab
                            is Tab.Browse -> {
                                if (event.toExtensions) BrowseTab.showExtension()
                                tabNavigator.current = BrowseTab
                            }
                            Tab.History -> tabNavigator.current = HistoryTab
                            is Tab.More -> {
                                tabNavigator.current = MoreTab
                                if (event.toDownloads) navigator.push(DownloadQueueScreen)
                            }
                            Tab.AI -> tabNavigator.current = HeroAiTab
                        }
                    }
                }
                launch {
                    showBottomNavEvent.receiveAsFlow().collectLatest { }
                }
            }
        }
    }

    @Composable
    private fun HeroTopBar(
        tab: cafe.adriel.voyager.navigator.tab.Tab,
        scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
    ) {
        val title = when (tab) {
            HeroDashboardTab -> stringResource(R.string.heroes_home_title)
            LibraryTab -> coreStringResource(MR.strings.label_library)
            UpdatesTab -> coreStringResource(MR.strings.label_recent_updates)
            BrowseTab -> coreStringResource(MR.strings.browse)
            HeroAiTab -> stringResource(R.string.heroes_ai_short_title)
            MoreTab -> coreStringResource(MR.strings.label_more)
            else -> "Manga Heroes"
        }
        TopAppBar(
            title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            scrollBehavior = scrollBehavior,
            actions = {
                if (tab == HeroDashboardTab) {
                    Surface(
                        modifier = Modifier.padding(end = 10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(MaterialSymbols.Rounded.RocketLaunch, null, modifier = Modifier.padding(1.dp))
                            Text(
                                stringResource(R.string.heroes_brand_pill),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            },
        )
    }

    @Composable
    private fun HeroBottomNavigation(
        tabs: List<cafe.adriel.voyager.navigator.tab.Tab>,
        current: cafe.adriel.voyager.navigator.tab.Tab,
        onSelect: (cafe.adriel.voyager.navigator.tab.Tab) -> Unit,
    ) {
        val context = LocalContext.current
        Surface(
            tonalElevation = 8.dp,
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF4C6FFF), Color(0xFFFF4F9A), Color(0xFFFFB52E), Color(0xFF22C7A9)),
                            ),
                        ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    tabs.fastForEach { tab ->
                        val selected = current.key == tab.key
                        val count by produceState(0, tab) {
                            val graph = context.appGraph
                            when (tab) {
                                is UpdatesTab -> combine(
                                    graph.libraryPreferences.newShowUpdatesCount.changes(),
                                    graph.libraryPreferences.newUpdatesCount.changes(),
                                ) { show, value -> if (show) value else 0 }.collectLatest { value = it }
                                is BrowseTab -> graph.sourcePreferences.extensionUpdatesCount.changes().collectLatest {
                                    value =
                                        it
                                }
                                else -> Unit
                            }
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = { onSelect(tab) },
                            icon = {
                                Box {
                                    Icon(tab.options.icon!!, contentDescription = tab.options.title)
                                    if (count > 0) {
                                        val chapterCountDescription = pluralStringResource(
                                            MR.plurals.notification_chapters_generic,
                                            count,
                                            count,
                                        )
                                        Badge(modifier = Modifier.align(Alignment.TopEnd)) {
                                            Text(
                                                count.toString(),
                                                modifier = Modifier.semantics {
                                                    contentDescription = chapterCountDescription
                                                },
                                            )
                                        }
                                    }
                                }
                            },
                            label = { Text(tab.options.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            }
        }
    }

    suspend fun search(query: String) = librarySearchEvent.send(query)

    suspend fun openTab(tab: Tab) = openTabEvent.send(tab)

    suspend fun showBottomNav(show: Boolean) = showBottomNavEvent.send(show)

    sealed interface Tab {
        data class Library(val mangaIdToOpen: Long? = null) : Tab
        data object Updates : Tab
        data object History : Tab
        data class Browse(val toExtensions: Boolean = false) : Tab
        data object AI : Tab
        data class More(val toDownloads: Boolean) : Tab
    }
}
