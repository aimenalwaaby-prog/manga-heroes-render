package mihon.data.novel

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import mihon.domain.novel.model.NovelSource
import org.jsoup.Jsoup
import java.net.URLEncoder

class NovelSourceClient(
    private val network: NetworkHelper,
) {
    suspend fun search(source: NovelSource, query: String): List<NovelSearchResult> {
        val url = source.searchUrl
            .replace("{query}", URLEncoder.encode(query, Charsets.UTF_8.name()))
            .replace("{page}", "1")
        val document = network.client.newCall(GET(url)).awaitSuccess().use { Jsoup.parse(it.body.string(), url) }
        return document.select(source.searchItemSelector).mapNotNull { item ->
            val title = item.select(source.titleSelector).firstOrNull()?.text()?.trim().orEmpty()
            val href = item.select(source.urlSelector).firstOrNull()?.absUrl("href").orEmpty()
            if (title.isBlank() || href.isBlank()) null else NovelSearchResult(title, href)
        }
    }

    suspend fun chapters(source: NovelSource, novelUrl: String): List<NovelChapter> {
        val document = network.client.newCall(GET(novelUrl)).awaitSuccess().use { Jsoup.parse(it.body.string(), novelUrl) }
        return document.select(source.chapterListSelector).mapNotNull { item ->
            val name = item.select(source.chapterNameSelector).firstOrNull()?.text()?.trim().orEmpty()
            val href = item.select(source.chapterUrlSelector).firstOrNull()?.absUrl("href").orEmpty()
            if (name.isBlank() || href.isBlank()) null else NovelChapter(name, href)
        }
    }

    suspend fun read(source: NovelSource, chapterUrl: String): String {
        val document = network.client.newCall(GET(chapterUrl)).awaitSuccess().use { Jsoup.parse(it.body.string(), chapterUrl) }
        return document.select(source.contentSelector).firstOrNull()
            ?.text()
            ?.trim()
            .orEmpty()
    }
}

data class NovelSearchResult(val title: String, val url: String)
data class NovelChapter(val name: String, val url: String)
