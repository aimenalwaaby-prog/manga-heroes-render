package eu.kanade.tachiyomi.ui.novel

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.data.novel.NovelSourceClient
import mihon.domain.novel.model.NovelSourceRegistry
import uy.kohesive.injekt.injectLazy

class NovelReaderScreen(
    private val title: String,
    private val url: String,
    private val sourceId: String,
) : Screen() {
    private val network: NetworkHelper by injectLazy()

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val source = remember(sourceId) { NovelSourceRegistry.all().firstOrNull { it.id == sourceId } }
        var text by remember { mutableStateOf("") }

        androidx.compose.runtime.LaunchedEffect(sourceId, url) {
            if (source == null) return@LaunchedEffect
            text = withContext(Dispatchers.IO) { runCatching { NovelSourceClient(network).read(source, url) }.getOrDefault("") }
        }

        tachiyomi.presentation.core.components.material.Scaffold(
            topBar = { scrollBehavior -> AppBar(title = title, navigateUp = navigator::pop, scrollBehavior = scrollBehavior) },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
            ) {
                item {
                    Text(
                        text = text.ifBlank { "Loading chapter…" },
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.55f),
                    )
                }
            }
        }
    }
}
