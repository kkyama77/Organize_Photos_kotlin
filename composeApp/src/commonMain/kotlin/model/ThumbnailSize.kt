package com.organize.photos.model

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * サムネイルサイズの選択肢
 */
enum class ThumbnailSize(val displayName: String, val sizeDp: Dp) {
    SMALL("小", 120.dp),
    MEDIUM("中", 180.dp),  // デフォルト
    LARGE("大", 240.dp);
    
    companion object {
        val DEFAULT = MEDIUM
        
        fun fromOrdinal(ordinal: Int): ThumbnailSize {
            return values().getOrNull(ordinal) ?: DEFAULT
        }
    }
}
