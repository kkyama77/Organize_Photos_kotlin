package com.organize.photos.logic

import java.io.File

data class UserMetadata(
    val title: String = "",
    val tags: List<String> = emptyList(),
    val comment: String = ""
)

/**
 * ユーザー定義メタデータ（タイトル、タグ、コメント）を XMP サイドカーで管理。
 * 写真と同じ場所に `<filename>.xmp` を作成し、ファイル本体は変更しない。
 */
object UserMetadataManager {
    private val cache = mutableMapOf<String, UserMetadata>()

    fun clearCache() = cache.clear()

    /**
     * サイドカーから読み込み。なければ空メタデータ。
     */
    fun getUserMetadata(filePath: String): UserMetadata {
        cache[filePath]?.let { return it }
        val loaded = readSidecar(filePath) ?: UserMetadata()
        cache[filePath] = loaded
        return loaded
    }

    /**
     * サイドカーへ保存し、キャッシュも更新。
     */
    fun setUserMetadata(filePath: String, metadata: UserMetadata) {
        cache[filePath] = metadata
        writeSidecar(filePath, metadata)
    }

    private fun sidecarFile(path: String): File {
        val photoFile = File(path)
        val parentDir = photoFile.parentFile ?: return File(path + ".xmp")
        val xmpDir = File(parentDir, ".xmp")
        xmpDir.mkdirs()
        return File(xmpDir, photoFile.name + ".xmp")
    }

    private fun writeSidecar(path: String, meta: UserMetadata) {
        runCatching {
            val xml = buildXmp(meta)
            sidecarFile(path).writeText(xml)
        }.onFailure { it.printStackTrace() }
    }

    private fun readSidecar(path: String): UserMetadata? {
        val file = sidecarFile(path)
        if (!file.exists()) return null
        val text = runCatching { file.readText() }.getOrElse { return null }
        return parseXmp(text)
    }

    // --- XMP minimal builder / parser ---

    private fun escape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun buildXmp(meta: UserMetadata): String {
        val title = escape(meta.title)
        val comment = escape(meta.comment)
        val tagsXml = meta.tags.joinToString("\n") { "        <rdf:li>${escape(it)}</rdf:li>" }

        return """
<?xml version="1.0" encoding="UTF-8"?>
<x:xmpmeta xmlns:x="adobe:ns:meta/">
  <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:dc="http://purl.org/dc/elements/1.1/">
    <rdf:Description>
      <dc:title>
        <rdf:Alt>
          <rdf:li xml:lang="x-default">$title</rdf:li>
        </rdf:Alt>
      </dc:title>
      <dc:subject>
        <rdf:Bag>
$tagsXml
        </rdf:Bag>
      </dc:subject>
      <dc:description>
        <rdf:Alt>
          <rdf:li xml:lang="x-default">$comment</rdf:li>
        </rdf:Alt>
      </dc:description>
    </rdf:Description>
  </rdf:RDF>
</x:xmpmeta>
""".trimIndent()
    }

    private fun parseXmp(xml: String): UserMetadata? {
        val title = Regex("<dc:title>.*?<rdf:li[^>]*>(.*?)</rdf:li>.*?</dc:title>", RegexOption.DOT_MATCHES_ALL)
            .find(xml)?.groupValues?.getOrNull(1)?.trim().orEmpty()

        val comment = Regex("<dc:description>.*?<rdf:li[^>]*>(.*?)</rdf:li>.*?</dc:description>", RegexOption.DOT_MATCHES_ALL)
            .find(xml)?.groupValues?.getOrNull(1)?.trim().orEmpty()

        val bagBlock = Regex("<dc:subject>.*?<rdf:Bag>(.*?)</rdf:Bag>.*?</dc:subject>", RegexOption.DOT_MATCHES_ALL)
            .find(xml)?.groupValues?.getOrNull(1).orEmpty()
        val tags = Regex("<rdf:li[^>]*>(.*?)</rdf:li>")
            .findAll(bagBlock)
            .mapNotNull { it.groupValues.getOrNull(1)?.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        return UserMetadata(title = title, tags = tags, comment = comment)
    }
}
