package com.organize.photos.logic

import com.organize.photos.model.ThumbnailSize

/**
 * アプリ設定の保存・読み込みインターフェース
 * プラットフォームごとに expect/actual で実装
 */
expect object AppPreferences {
    fun getThumbnailSize(): ThumbnailSize
    fun setThumbnailSize(size: ThumbnailSize)
}
