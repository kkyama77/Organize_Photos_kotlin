package com.organize.photos.logic

import com.organize.photos.model.PhotoItem

/**
 * プラットフォーム別のサムネイル生成実装
 * - Desktop: ImageIO + BufferedImage
 * - Android: MediaStore Thumbnail / Glide
 */
expect class ThumbnailGenerator {
    suspend fun generate(item: PhotoItem, maxSize: Int): ByteArray?
}
