package mihon.data.extension.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.protobuf.ProtoNumber
import mihon.data.extension.model.NetworkExtensionStore.ContentWarning
import mihon.data.extension.model.NetworkExtensionStore.ExtensionList
import mihon.domain.extension.model.ExtensionStore
import mihon.domain.novel.model.NovelSource
import mihon.domain.novel.model.NovelSourceRegistry
import eu.kanade.tachiyomi.extension.model.Extension as TachiyomiExtension
import mihon.domain.extension.model.ContentWarning as DomainContentWarning

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class NetworkExtensionStore(
    @ProtoNumber(1) val name: String,
    @ProtoNumber(2) val badgeLabel: String,
    @ProtoNumber(3) val signingKey: String,
    @ProtoNumber(4) val contact: Contact,
    @ProtoNumber(101) val extensionList: ExtensionList?,
    @ProtoNumber(102) val extensionListUrl: String?,
    // JSON-only optional field. Protobuf stores remain fully backward compatible.
    @ProtoNumber(103) val novelSources: List<NovelSourceDefinition> = emptyList(),
) : BaseNetworkExtensionStore {
    @Serializable
    data class Contact(
        @ProtoNumber(1) val website: String,
        @ProtoNumber(2) val discord: String?,
    )

    @Serializable
    data class ExtensionList(@ProtoNumber(1) val extensions: List<Extension>)

    @Serializable
    data class Extension(
        @ProtoNumber(1) val name: String,
        @ProtoNumber(2) val packageName: String,
        @ProtoNumber(3) val resources: Resources,
        @ProtoNumber(4) val extensionLib: String,
        @ProtoNumber(5) val versionCode: Long,
        @ProtoNumber(6) val versionName: String,
        @ProtoNumber(7) val contentWarning: ContentWarning,
        @ProtoNumber(8) val sources: List<Source>,
    )

    @Serializable
    data class NovelSourceDefinition(
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
    ) {
        fun toDomain(): NovelSource = NovelSource(
            id = id,
            name = name,
            lang = lang,
            homeUrl = homeUrl,
            searchUrl = searchUrl,
            searchItemSelector = searchItemSelector,
            titleSelector = titleSelector,
            urlSelector = urlSelector,
            chapterListSelector = chapterListSelector,
            chapterNameSelector = chapterNameSelector,
            chapterUrlSelector = chapterUrlSelector,
            contentSelector = contentSelector,
            contentWarning = contentWarning,
        )
    }

    @Serializable
    data class Resources(
        @ProtoNumber(1) val apkUrl: String,
        @ProtoNumber(2) val iconUrl: String,
    )

    @Serializable
    data class Source(
        @ProtoNumber(1) val id: Long,
        @ProtoNumber(2) val name: String,
        @ProtoNumber(3) val language: String,
        @ProtoNumber(4) val homeUrl: String = "",
        @ProtoNumber(5) val mirrorUrls: List<String> = emptyList(),
        // @ProtoNumber(6) val contentWarning: ContentWarning = ContentWarning.SAFE,
        @ProtoNumber(7) val message: String? = null,
    )

    @Suppress("Unused")
    enum class ContentWarning {
        @ProtoNumber(0)
        @JsonNames("CONTENT_WARNING_UNSPECIFIED")
        UNSPECIFIED,

        @ProtoNumber(1)
        @JsonNames("CONTENT_WARNING_SAFE")
        SAFE,

        @ProtoNumber(2)
        @JsonNames("CONTENT_WARNING_MIXED")
        MIXED,

        @ProtoNumber(3)
        @JsonNames("CONTENT_WARNING_NSFW")
        NSFW,
    }

    override fun toExtensionStore(indexUrl: String): ExtensionStore {
        return ExtensionStore(
            indexUrl = indexUrl,
            name = name,
            badgeLabel = badgeLabel,
            signingKey = signingKey,
            contact = ExtensionStore.Contact(
                website = contact.website,
                discord = contact.discord,
            ),
            isLegacy = false,
            extensionListUrl = extensionListUrl,
        )
    }
}

fun ExtensionList.toAvailableExtensions(store: ExtensionStore): List<TachiyomiExtension.Available> {
    return extensions.map { extension ->
        val lang = extension.sources.map { it.language }.toSet()
        TachiyomiExtension.Available(
            name = extension.name,
            pkgName = extension.packageName,
            apkUrl = extension.resources.apkUrl,
            iconUrl = extension.resources.iconUrl,
            libVersion = extension.extensionLib.toDouble(),
            versionCode = extension.versionCode,
            versionName = extension.versionName,
            lang = if (lang.size == 1) lang.first() else "all",
            contentWarning = when (extension.contentWarning) {
                ContentWarning.SAFE -> DomainContentWarning.SAFE
                ContentWarning.MIXED -> DomainContentWarning.MIXED
                ContentWarning.NSFW -> DomainContentWarning.NSFW
                else -> DomainContentWarning.SAFE
            },
            sources = extension.sources.map { source ->
                TachiyomiExtension.Available.Source(
                    id = source.id,
                    name = source.name,
                    lang = source.language,
                    baseUrl = source.homeUrl,
                )
            },
            store = store,
        )
    }
}
