package eu.kanade.tachiyomi.ui.novel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mihon.data.novel.NovelChapter
import mihon.data.novel.NovelSourceClient
import mihon.domain.novel.model.NovelSourceRegistry
import uy.kohesive.injekt.injectLazy

class NovelChaptersScreen(
    private val title: String,
    private val url: String,
    private val sourceId: String,
) : Screen() {
    private val network: NetworkHelper by injectLazy()

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val source = remember(sourceId) { NovelSourceRegistry.all().firstOrNull { it.id == sourceId } }
        val client = remember { NovelSourceClient(network) }
        var loading by remember { mutableStateOf(source != null) }
        var chapters by remember { mutableStateOf<List<NovelChapter>>(emptyList()) }
        var error by remember { mutableStateOf("") }

        androidx.compose.runtime.LaunchedEffect(sourceId, url) {
            if (source == null) return@LaunchedEffect
            val result = withContext(Dispatchers.IO) { runCatching { client.chapters(source, url) } }
            chapters = result.getOrElse {
                error = it.message ?: "Unable to load chapters from this source"
                emptyList()
            }
            loading = false
        }

        tachiyomi.presentation.core.components.material.Scaffold(
            topBar = { scrollBehavior ->
                AppBar(title = title, navigateUp = navigator::pop, scrollBehavior = scrollBehavior)
            },
        ) { padding ->
            if (loading) {
                CircularProgressIndicator(Modifier.padding(padding).padding(24.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(chapters) { chapter ->
                        Card(Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { navigator.push(NovelReaderScreen(chapter.name, chapter.url, sourceId)) },
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                            ) {
                                Text(chapter.name)
                            }
                        }
                    }
                    if (error.isNotBlank()) {
                        item { Text("Chapter loading failed: $error", color = MaterialTheme.colorScheme.error) }
                    } else if (chapters.isEmpty()) {
                        item {
                            Text(
                                "No chapters were returned by this source.",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}
