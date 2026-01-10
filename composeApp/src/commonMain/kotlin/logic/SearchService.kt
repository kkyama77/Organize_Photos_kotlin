package com.organize.photos.logic

import com.organize.photos.model.PhotoItem
import kotlinx.datetime.Instant

class SearchService {
    enum class SearchMode {
        OR,  // いずれかのキーワードにマッチ
        AND  // すべてのキーワードにマッチ
    }
    
    fun filter(
        items: List<PhotoItem>,
        query: String,
        extensions: Set<String>,
        dateRange: ClosedRange<Instant>? = null,
        searchMode: SearchMode = SearchMode.OR,
    ): List<PhotoItem> {
        // キーワードを複数に分割（カンマ区切り）
        val keywords = if (query.isBlank()) {
            emptyList()
        } else {
            query.split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
        }
        
        return items.asSequence()
            .filter {
                if (keywords.isEmpty()) true else {
                    // 検索対象テキストを統合（ディレクトリパス除外、ファイル名のみ）
                    val metaText = it.metadata.entries.joinToString(" ") { (k, v) -> "$k $v" }
                    val resolutionText = listOfNotNull(it.width, it.height)
                        .takeIf { lst -> lst.size == 2 }
                        ?.joinToString("x") ?: ""
                    val sizeText = it.sizeBytes?.toString() ?: ""
                    val dateText = it.capturedAt?.toString() ?: ""
                    
                    // ✨ ユーザーメタデータ（title, tags, comment）も検索対象に追加
                    val userMetaText = buildString {
                        append(it.title).append(" ")
                        append(it.tags.joinToString(" ")).append(" ")
                        append(it.comment).append(" ")
                    }
                    
                    val haystack = (it.displayName + " " +
                            it.extension + " " +
                            metaText + " " +
                            resolutionText + " " +
                            sizeText + " " +
                            dateText + " " +
                            userMetaText).lowercase()
                    
                    // 検索モードに応じて AND/OR 検索を切り替え
                    when (searchMode) {
                        SearchMode.OR -> keywords.any { keyword -> haystack.contains(keyword) }
                        SearchMode.AND -> keywords.all { keyword -> haystack.contains(keyword) }
                    }
                }
            }
            .filter { extensions.isEmpty() || extensions.contains(it.extension.lowercase()) }
            .filter {
                dateRange?.let { range ->
                    val captured = it.capturedAt
                    captured == null || (captured >= range.start && captured <= range.endInclusive)
                } ?: true
            }
            .toList()
    }
}
