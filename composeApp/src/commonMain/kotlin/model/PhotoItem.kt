package com.organize.photos.model

import kotlinx.datetime.Instant

data class PhotoItem(
    val id: String,
    val displayName: String,
    val absolutePath: String,
    val capturedAt: Instant?,
    val width: Int?,
    val height: Int?,
    val sizeBytes: Long?,
    val extension: String,
    val metadata: Map<String, String> = emptyMap(),
    val thumbnail: ByteArray? = null,
    // ✨ ユーザー定義メタデータ
    val title: String = "",
    val tags: List<String> = emptyList(),
    val comment: String = "",
    // ✨ ファイル作成日時（並べ替え用）
    val modifiedAt: Instant?,
)
