package eu.kanade.tachiyomi.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.heroes.MangaHeroesScreen
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.RocketLaunch
import tachiyomi.presentation.core.i18n.stringResource

/** AI is a first-class destination in Manga Heroes, rather than a buried utility page. */
data object HeroAiTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 4u,
            title = stringResource(R.string.heroes_ai_short_title),
            icon = rememberVectorPainter(MaterialSymbols.Rounded.RocketLaunch),
        )

    @Composable
    override fun Content() {
        MangaHeroesScreen().Content()
    }
}
