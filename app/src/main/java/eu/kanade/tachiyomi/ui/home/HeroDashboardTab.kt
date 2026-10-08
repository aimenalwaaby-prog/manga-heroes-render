package eu.kanade.tachiyomi.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.novel.NovelSourcesScreen
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Explore
import mihon.icons.materialsymbols.rounded.LocalLibrary
import mihon.icons.materialsymbols.rounded.QueryStats
import mihon.icons.materialsymbols.rounded.RocketLaunch
import mihon.icons.materialsymbols.rounded.Settings

/**
 * The Manga Heroes landing page deliberately avoids Mihon's standard five-tab shell.
 * It is a dashboard with quick actions and visual hierarchy, while the existing
 * library/source implementations remain the real underlying features.
 */
data object HeroDashboardTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 0u,
            title = stringResource(R.string.heroes_home_title),
            icon = MaterialSymbols.Rounded.Explore,
        )

    @Composable
    override fun Content() {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val isTablet = isTabletUi()
        val pulse = rememberInfiniteTransition(label = "heroPulse")
        val glow by pulse.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.02f,
            animationSpec = infiniteRepeatable(
                tween(1500, easing = FastOutSlowInEasing),
                RepeatMode.Reverse,
            ),
            label = "heroGlow",
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = if (isTablet) 28.dp else 16.dp,
                top = 22.dp,
                end = if (isTablet) 28.dp else 16.dp,
                bottom = 110.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(glow)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF4C6FFF),
                                    Color(0xFF7B3FF2),
                                    Color(0xFFFF4F9A),
                                ),
                            ),
                            RoundedCornerShape(30.dp),
                        )
                        .padding(22.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.heroes_home_greeting),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                        Text(
                            text = stringResource(R.string.heroes_home_title),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                        Text(
                            text = stringResource(R.string.heroes_home_subtitle),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.94f),
                        )
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            color = Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.clickable { tabNavigator.current = HeroAiTab },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(MaterialSymbols.Rounded.RocketLaunch, null, tint = Color.White)
                                Text(
                                    stringResource(R.string.heroes_home_ai_cta),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.heroes_quick_actions),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    HeroActionCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.heroes_action_library),
                        icon = MaterialSymbols.Rounded.LocalLibrary,
                        accent = Color(0xFF22C7A9),
                    ) { tabNavigator.current = LibraryTab }
                    HeroActionCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.heroes_action_updates),
                        icon = MaterialSymbols.Rounded.QueryStats,
                        accent = Color(0xFFFFA62B),
                    ) { tabNavigator.current = UpdatesTab }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    HeroActionCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.heroes_action_history),
                        icon = MaterialSymbols.Rounded.QueryStats,
                        accent = Color(0xFF8E7CFF),
                    ) { tabNavigator.current = HistoryTab }
                    HeroActionCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.heroes_action_profile),
                        icon = MaterialSymbols.Rounded.RocketLaunch,
                        accent = Color(0xFFFF5D8F),
                    ) { tabNavigator.current = HeroAiTab }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            stringResource(R.string.heroes_content_modes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(R.string.heroes_content_modes_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = true, onClick = {
                            }, label = { Text(stringResource(R.string.heroes_manga_mode)) })
                            FilterChip(selected = false, onClick = {
                                navigator.push(NovelSourcesScreen())
                            }, label = { Text(stringResource(R.string.heroes_novel_mode)) })
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.heroes_discover_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(stringResource(R.string.heroes_discover_hint), style = MaterialTheme.typography.bodyMedium)
                        Surface(
                            onClick = { tabNavigator.current = BrowseTab },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.tertiary,
                        ) {
                            Text(
                                stringResource(R.string.heroes_open_sources),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                                color = MaterialTheme.colorScheme.onTertiary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun HeroActionCard(
        modifier: Modifier,
        title: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        accent: Color,
        onClick: () -> Unit,
    ) {
        Card(
            modifier = modifier.clickable(onClick = onClick),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(color = accent.copy(alpha = 0.16f), shape = RoundedCornerShape(14.dp)) {
                    Icon(icon, null, tint = accent, modifier = Modifier.padding(10.dp).size(22.dp))
                }
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}
