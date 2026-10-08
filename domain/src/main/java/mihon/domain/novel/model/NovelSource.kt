package mihon.domain.novel.model

/**
 * Repository-declared text source. It intentionally lives beside extension-store metadata so
 * repositories can expose novels without changing the existing Mihon source APK contract.
 *
 * The app never executes repository-provided code for this path: it only fetches HTML and
 * extracts text using declared CSS selectors.
 */
data class NovelSource(
    val id: String,
    val name: String,
    val lang: String,
    val homeUrl: String,
    val searchUrl: String,
    val searchItemSelector: String,
    val titleSelector: String,
    val urlSelector: String,
    val chapterListSelector: String,
    val chapterNameSelector: String,
    val chapterUrlSelector: String,
    val contentSelector: String,
    val contentWarning: String = "SAFE",
)

object NovelSourceRegistry {
    private val sources = LinkedHashMap<String, List<NovelSource>>()

    @Synchronized
    fun replace(repositoryUrl: String, values: List<NovelSource>) {
        sources[repositoryUrl] = values
    }

    @Synchronized
    fun all(): List<NovelSource> = sources.values.flatten().filter { it.contentWarning.equals("SAFE", ignoreCase = true) }.distinctBy { it.id }

    @Synchronized
    fun clear(repositoryUrl: String) {
        sources.remove(repositoryUrl)
    }
}
