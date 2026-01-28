package com.organize.photos.logic

import com.organize.photos.model.ThumbnailSize
import java.util.prefs.Preferences

/**
 * Desktop 用設定保存（java.util.prefs を使用）
 */
actual object AppPreferences {
    private val prefs = Preferences.userNodeForPackage(AppPreferences::class.java)
    private const val KEY_THUMBNAIL_SIZE = "thumbnail_size"
    
    actual fun getThumbnailSize(): ThumbnailSize {
        val ordinal = prefs.getInt(KEY_THUMBNAIL_SIZE, ThumbnailSize.DEFAULT.ordinal)
        return ThumbnailSize.fromOrdinal(ordinal)
    }
    
    actual fun setThumbnailSize(size: ThumbnailSize) {
        prefs.putInt(KEY_THUMBNAIL_SIZE, size.ordinal)
        prefs.flush()
    }
}
