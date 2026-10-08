package eu.kanade.tachiyomi.ui.novel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import mihon.data.novel.NovelSearchResult
import mihon.data.novel.NovelSourceClient
import mihon.domain.novel.model.NovelSource
import mihon.domain.novel.model.NovelSourceRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy
import eu.kanade.tachiyomi.network.NetworkHelper

class NovelSourcesScreen : Screen() {
    private val network: NetworkHelper by injectLazy()

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val client = remember { NovelSourceClient(network) }
        var query by remember { mutableStateOf("") }
        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf("") }
        var results by remember { mutableStateOf<List<Pair<NovelSource, NovelSearchResult>>>(emptyList()) }
        val sources = remember { NovelSourceRegistry.all() }

        tachiyomi.presentation.core.components.material.Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = "Novel Universe",
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Novel sources (${sources.size})", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "These sources use the same repository layer, but text chapters get their own reader pipeline. Only repository entries explicitly marked SAFE are exposed here.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it.take(120) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Search novels") },
                                singleLine = true,
                            )
                            Button(
                                enabled = query.isNotBlank() && !loading && sources.isNotEmpty(),
                                onClick = {
                                    loading = true
                                    error = ""
                                    scope.launch {
                                        val fetched = withContext(Dispatchers.IO) {
                                            sources.map { source -> source to runCatching { client.search(source, query.trim()) } }
                                        }
                                        results = fetched.flatMap { (source, result) ->
                                            result.getOrElse {
                                                error = "${source.name}: ${it.message ?: "source request failed"}"
                                                emptyList()
                                            }.map { source to it }
                                        }
                                        loading = false
                                        if (sources.isEmpty()) error = "No compatible SAFE novel sources are registered. Refresh a repository that declares novelSources first."
                                    }
                                },
                            ) {
                                if (loading) CircularProgressIndicator() else Text("Search")
                            }
                            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (sources.isEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Text(
                                "Novel support is installed, but your current repositories do not declare compatible novel source definitions yet.",
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }

                items(results) { (source, result) ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(result.title, style = MaterialTheme.typography.titleMedium)
                            Text("${source.name} · ${source.lang}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { navigator.push(NovelChaptersScreen(result.title, result.url, source.id)) }) {
                                    Text("Chapters")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
