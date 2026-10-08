package eu.kanade.tachiyomi.ui.novel

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.edit
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
        val context = LocalContext.current
        val source = remember(sourceId) { NovelSourceRegistry.all().firstOrNull { it.id == sourceId } }
        var text by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }
        val paragraphs = remember(text) { text.split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() } }
        val progressKey = remember(sourceId, url) { "novel_progress_${sourceId}_${url.hashCode()}" }
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = context.getSharedPreferences("manga_heroes_novels", 0).getInt(progressKey, 0))

        androidx.compose.runtime.LaunchedEffect(sourceId, url) {
            if (source == null) return@LaunchedEffect
            val result = withContext(Dispatchers.IO) { runCatching { NovelSourceClient(network).read(source, url) } }
            text = result.getOrElse {
                error = it.message ?: "Unable to load this chapter from the source"
                ""
            }
        }
        androidx.compose.runtime.LaunchedEffect(listState.firstVisibleItemIndex, text) {
            context.getSharedPreferences("manga_heroes_novels", 0).edit { putInt(progressKey, listState.firstVisibleItemIndex) }
        }

        tachiyomi.presentation.core.components.material.Scaffold(
            topBar = { scrollBehavior -> AppBar(title = title, navigateUp = navigator::pop, scrollBehavior = scrollBehavior) },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
                state = listState,
            ) {
                if (error.isNotBlank()) {
                    item { Text("Chapter reading failed: $error", color = MaterialTheme.colorScheme.error) }
                } else if (paragraphs.isEmpty()) {
                    item { Text("Loading chapter…", style = MaterialTheme.typography.bodyLarge) }
                } else {
                    items(paragraphs) { paragraph ->
                        Text(
                            text = paragraph,
                            modifier = Modifier.padding(bottom = 16.dp),
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.55f),
                        )
                    }
                }
            }
        }
    }
}
