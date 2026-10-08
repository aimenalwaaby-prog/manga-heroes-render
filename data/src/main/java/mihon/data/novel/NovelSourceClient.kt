package mihon.data.novel

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import mihon.domain.novel.model.NovelSource
import org.jsoup.Jsoup
import java.net.URI
import java.net.URLEncoder

/** Real network client for repository-declared novel sources. It never executes repository code. */
class NovelSourceClient(
    private val network: NetworkHelper,
) {
    suspend fun search(source: NovelSource, query: String): List<NovelSearchResult> {
        requireValidUrl(source.searchUrl, source.homeUrl)
        requireSelector(source.searchItemSelector, "searchItemSelector")
        val url = source.searchUrl
            .replace("{query}", URLEncoder.encode(query, Charsets.UTF_8.name()))
            .replace("{page}", "1")
        val document = network.client.newCall(GET(url)).awaitSuccess().use { response ->
            Jsoup.parse(response.body.string(), url)
        }
        return document.select(source.searchItemSelector).mapNotNull { item ->
            val title = item.select(source.titleSelector).firstOrNull()?.text()?.trim().orEmpty()
            val href = item.select(source.urlSelector).firstOrNull()?.absUrl("href").orEmpty()
            if (title.isBlank() || href.isBlank() ||
                !isHttpUrl(href)
            ) {
                null
            } else {
                NovelSearchResult(source.id, title, href)
            }
        }
    }

    suspend fun chapters(source: NovelSource, novelUrl: String): List<NovelChapter> {
        requireValidUrl(novelUrl, source.homeUrl)
        requireSelector(source.chapterListSelector, "chapterListSelector")
        val document = network.client.newCall(GET(novelUrl)).awaitSuccess().use { response ->
            Jsoup.parse(response.body.string(), novelUrl)
        }
        return document.select(source.chapterListSelector).mapNotNull { item ->
            val name = item.select(source.chapterNameSelector).firstOrNull()?.text()?.trim().orEmpty()
            val href = item.select(source.chapterUrlSelector).firstOrNull()?.absUrl("href").orEmpty()
            if (name.isBlank() || href.isBlank() || !isHttpUrl(href)) null else NovelChapter(name, href)
        }
    }

    suspend fun read(source: NovelSource, chapterUrl: String): String {
        requireValidUrl(chapterUrl, source.homeUrl)
        requireSelector(source.contentSelector, "contentSelector")
        val document = network.client.newCall(GET(chapterUrl)).awaitSuccess().use { response ->
            Jsoup.parse(response.body.string(), chapterUrl)
        }
        val content = document.select(source.contentSelector).firstOrNull()
            ?: throw IllegalStateException("The source returned no matching chapter content")
        val paragraphs = content.select("p, br").map { it.text().trim() }.filter { it.isNotBlank() }
        return (if (paragraphs.size >= 2) paragraphs.joinToString("\n\n") else content.text())
            .trim()
            .ifBlank { throw IllegalStateException("The source returned an empty chapter") }
    }

    private fun requireValidUrl(value: String, fallback: String) {
        val candidate = value.replace("{query}", "query").replace("{page}", "1")
        if (!isHttpUrl(candidate) || !isHttpUrl(fallback)) {
            throw IllegalArgumentException("Source contains an invalid HTTP(S) URL")
        }
    }

    private fun requireSelector(selector: String, name: String) {
        if (selector.isBlank() || selector.length > 512) throw IllegalArgumentException("Invalid $name")
    }

    private fun isHttpUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        (uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) && !uri.host.isNullOrBlank()
    }.getOrDefault(false)
}

data class NovelSearchResult(val sourceId: String, val title: String, val url: String)
data class NovelChapter(val name: String, val url: String)
