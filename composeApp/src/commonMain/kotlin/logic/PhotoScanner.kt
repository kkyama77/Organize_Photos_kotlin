package com.organize.photos.logic

import com.organize.photos.model.PhotoItem

/**
 * プラットフォーム別の写真スキャン実装
 * - Desktop: Java NIO + metadata-extractor
 * - Android: ContentResolver + MediaStore
 */
expect class PhotoScanner {
    suspend fun scan(root: String, filters: ScanFilters = ScanFilters()): List<PhotoItem>
    suspend fun renamePhoto(photo: PhotoItem, newFileName: String): PhotoItem?
}
