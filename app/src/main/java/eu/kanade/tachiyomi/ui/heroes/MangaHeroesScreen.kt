package eu.kanade.tachiyomi.ui.heroes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.stats.StatsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mihon.app.di.appGraph
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import tachiyomi.domain.library.model.LibraryDisplayMode
import java.time.LocalDate

/** A small, fully offline profile and habit layer that complements Mihon's existing reader and statistics. */
class MangaHeroesScreen : Screen() {
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val appLanguage = LocalConfiguration.current.locales[0].language
        val aiEmptyMessage = stringResource(R.string.heroes_ai_empty)
        val aiNotConfiguredMessage = stringResource(R.string.heroes_ai_not_configured)
        val aiErrorMessage = stringResource(R.string.heroes_ai_error)
        val navigator = LocalNavigator.currentOrThrow
        val prefs =
            remember { context.getSharedPreferences("manga_heroes_profile", android.content.Context.MODE_PRIVATE) }
        var username by remember { mutableStateOf(prefs.getString("username", "") ?: "") }
        var savedName by remember { mutableStateOf(prefs.getString("username", "") ?: "") }
        var favoriteGenres by remember {
            mutableStateOf(prefs.getStringSet("favorite_genres", emptySet())?.toSet() ?: emptySet())
        }
        val libraryDisplayPreference = remember { context.appGraph.libraryPreferences.displayMode }
        val libraryDisplayMode by libraryDisplayPreference.changes().collectAsState(
            initial = libraryDisplayPreference.get(),
        )
        var showLocalGuide by remember { mutableStateOf(false) }
        var aiPrompt by remember { mutableStateOf("") }
        var aiAnswer by remember { mutableStateOf("") }
        var aiLoading by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val httpClient = remember { OkHttpClient() }
        val today = LocalDate.now().toString()
        val missionKeys = listOf("read_one", "read_three", "discover", "favorite")
        val missionLabels = listOf(
            R.string.heroes_mission_one,
            R.string.heroes_mission_three,
            R.string.heroes_mission_discover,
            R.string.heroes_mission_favorite,
        )
        var completed by remember(today) {
            mutableStateOf(missionKeys.filter { prefs.getBoolean("mission_${today}_$it", false) }.toSet())
        }
        val genres = listOf(
            R.string.heroes_genre_action to "action",
            R.string.heroes_genre_adventure to "adventure",
            R.string.heroes_genre_fantasy to "fantasy",
            R.string.heroes_genre_comedy to "comedy",
            R.string.heroes_genre_drama to "drama",
            R.string.heroes_genre_romance to "romance",
            R.string.heroes_genre_mystery to "mystery",
            R.string.heroes_genre_scifi to "scifi",
        )

        tachiyomi.presentation.core.components.material.Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(R.string.heroes_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.heroes_welcome),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = savedName.ifBlank { stringResource(R.string.heroes_profile_hint) },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it.take(32) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.heroes_username)) },
                                singleLine = true,
                            )
                            Button(onClick = {
                                savedName = username.trim()
                                prefs.edit { putString("username", savedName) }
                            }) { Text(stringResource(R.string.heroes_save_profile)) }
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.heroes_preferences),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                stringResource(R.string.heroes_genres_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            genres.chunked(2).forEach { pair ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    pair.forEach { (label, key) ->
                                        FilterChip(
                                            selected = key in favoriteGenres,
                                            onClick = {
                                                favoriteGenres =
                                                    if (key in
                                                        favoriteGenres
                                                    ) {
                                                        favoriteGenres - key
                                                    } else {
                                                        favoriteGenres + key
                                                    }
                                                prefs.edit { putStringSet("favorite_genres", favoriteGenres) }
                                            },
                                            label = { Text(stringResource(label)) },
                                        )
                                    }
                                }
                            }
                            Text(
                                stringResource(R.string.heroes_layout_hint),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = libraryDisplayMode == LibraryDisplayMode.List, onClick = {
                                    libraryDisplayPreference.set(LibraryDisplayMode.List)
                                }, label = { Text(stringResource(R.string.heroes_vertical)) })
                                FilterChip(selected = libraryDisplayMode != LibraryDisplayMode.List, onClick = {
                                    libraryDisplayPreference.set(LibraryDisplayMode.ComfortableGrid)
                                }, label = { Text(stringResource(R.string.heroes_horizontal)) })
                            }
                        }
                    }
                }
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.heroes_daily_title),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                stringResource(R.string.heroes_daily_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            missionKeys.forEachIndexed { index, key ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Checkbox(
                                        checked = key in completed,
                                        onCheckedChange = { checked ->
                                            completed = if (checked) completed + key else completed - key
                                            prefs.edit { putBoolean("mission_${today}_$key", checked) }
                                        },
                                    )
                                    Text(
                                        stringResource(missionLabels[index]),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                            Text(
                                stringResource(R.string.heroes_challenge_progress, completed.size),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
                item {
                    Button(onClick = { navigator.push(StatsScreen()) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.heroes_open_stats))
                    }
                }
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(stringResource(R.string.heroes_ai_title), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.heroes_ai_description),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            OutlinedTextField(
                                value = aiPrompt,
                                onValueChange = { aiPrompt = it.take(600) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.heroes_ai_prompt)) },
                                minLines = 2,
                            )
                            Button(
                                enabled = !aiLoading,
                                onClick = {
                                    val baseUrl = BuildConfig.MANGA_HEROES_AI_BASE_URL.trimEnd('/')
                                    val token = BuildConfig.MANGA_HEROES_AI_TOKEN
                                    if (aiPrompt.isBlank()) {
                                        aiAnswer = aiEmptyMessage
                                    } else if (baseUrl.isBlank() || token.isBlank()) {
                                        aiAnswer = aiNotConfiguredMessage
                                    } else {
                                        aiLoading = true
                                        aiAnswer = ""
                                        val json = JSONObject()
                                            .put("prompt", aiPrompt.trim())
                                            .put("genres", JSONArray(favoriteGenres.toList()))
                                            .put("language", appLanguage)
                                        scope.launch {
                                            aiAnswer = try {
                                                withContext(Dispatchers.IO) {
                                                    val request = Request.Builder()
                                                        .url("$baseUrl/v1/recommend")
                                                        .header("Authorization", "Bearer $token")
                                                        .post(
                                                            json.toString().toRequestBody(
                                                                "application/json; charset=utf-8".toMediaType(),
                                                            ),
                                                        )
                                                        .build()
                                                    httpClient.newCall(request).execute().use { response ->
                                                        val body = response.body.string()
                                                        if (!response.isSuccessful) {
                                                            throw IllegalStateException(
                                                                "gateway",
                                                            )
                                                        }
                                                        JSONObject(body).optString("answer").ifBlank {
                                                            throw IllegalStateException("empty")
                                                        }
                                                    }
                                                }
                                            } catch (_: Exception) {
                                                aiErrorMessage
                                            } finally {
                                                aiLoading = false
                                            }
                                        }
                                    }
                                },
                            ) {
                                if (aiLoading) {
                                    CircularProgressIndicator()
                                } else {
                                    Text(stringResource(R.string.heroes_ai_send))
                                }
                            }
                            if (aiAnswer.isNotBlank()) Text(aiAnswer, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.heroes_local_title),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                stringResource(R.string.heroes_local_summary),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Button(onClick = { showLocalGuide = !showLocalGuide }) {
                                Text(
                                    stringResource(
                                        if (showLocalGuide) R.string.heroes_hide_guide else R.string.heroes_show_guide,
                                    ),
                                )
                            }
                            if (showLocalGuide) {
                                Text(
                                    stringResource(R.string.heroes_local_guide),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.heroes_privacy_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
