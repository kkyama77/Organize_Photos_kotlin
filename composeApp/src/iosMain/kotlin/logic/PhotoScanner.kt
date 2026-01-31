package com.organize.photos.logic

import com.organize.photos.model.PhotoItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * iOS実装：スタブ（将来的にPhotosFrameworkを使用）
 */
actual class PhotoScanner(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    actual suspend fun scan(root: String, filters: ScanFilters): List<PhotoItem> = withContext(dispatcher) {
        // iOS実装はPhotosFrameworkを使用する必要がある
        // 現在はスタブ実装
        emptyList()
    }

    actual suspend fun renamePhoto(photo: PhotoItem, newFileName: String): PhotoItem? = withContext(dispatcher) {
        try {
            val oldFile = File(photo.absolutePath)
            if (!oldFile.exists() || !oldFile.isFile) return@withContext null
            
            // 拡張子を自動付上
            val fileNameWithExt = if (!newFileName.endsWith(".${photo.extension}")) {
                "$newFileName.${photo.extension}"
            } else {
                newFileName
            }
            
            val newFile = File(oldFile.parent, fileNameWithExt)
            val renamed = oldFile.renameTo(newFile)
            
            if (!renamed) return@withContext null
            
            // 新しいPhotoItemを作成して返す
            photo.copy(
                id = newFile.absolutePath,
                displayName = newFile.name,
                absolutePath = newFile.absolutePath
            )
        } catch (e: Exception) {
            null
        }
    }
}
