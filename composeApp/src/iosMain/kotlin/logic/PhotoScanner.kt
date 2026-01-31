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

    actual suspend fun renamePhoto(photo: PhotoItem, newFileName: String): Boolean = withContext(dispatcher) {
        return try {
            val oldFile = File(photo.absolutePath)
            if (!oldFile.exists() || !oldFile.isFile) return@withContext false
            
            val newFile = File(oldFile.parent, newFileName)
            oldFile.renameTo(newFile)
        } catch (e: Exception) {
            false
        }
    }
}
