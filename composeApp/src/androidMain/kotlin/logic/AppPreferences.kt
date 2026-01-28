package com.organize.photos.logic

import android.content.Context
import com.organize.photos.model.ThumbnailSize

/**
 * Android 用設定保存（SharedPreferences を使用）
 * Context が必要なため、初期化が必要
 */
actual object AppPreferences {
    private const val PREFS_NAME = "organize_photos_settings"
    private const val KEY_THUMBNAIL_SIZE = "thumbnail_size"
    
    private var context: Context? = null
    
    fun init(ctx: Context) {
        context = ctx.applicationContext
    }
    
    actual fun getThumbnailSize(): ThumbnailSize {
        val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ordinal = prefs?.getInt(KEY_THUMBNAIL_SIZE, ThumbnailSize.DEFAULT.ordinal) 
            ?: ThumbnailSize.DEFAULT.ordinal
        return ThumbnailSize.fromOrdinal(ordinal)
    }
    
    actual fun setThumbnailSize(size: ThumbnailSize) {
        val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs?.edit()?.putInt(KEY_THUMBNAIL_SIZE, size.ordinal)?.apply()
    }
}
