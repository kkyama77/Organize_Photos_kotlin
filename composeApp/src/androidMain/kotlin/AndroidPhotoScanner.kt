package com.organize.photos.android

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.organize.photos.logic.PhotoScanner
import com.organize.photos.logic.ScanFilters
import com.organize.photos.logic.UserMetadataManager
import com.organize.photos.model.PhotoItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import java.io.File

/**
 * Android実装：MediaStore経由で写真をスキャン
 * ContentResolver を使用して外部ストレージの画像を取得
 */
actual class PhotoScanner(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val contentResolver: ContentResolver = context.contentResolver

    actual suspend fun scan(root: String, filters: ScanFilters): List<PhotoItem> = withContext(dispatcher) {
        val results = mutableListOf<PhotoItem>()
        
        val normalizedExt = filters.extensions.map { it.lowercase() }.toSet()
        
        // MediaStore.Images.Media.EXTERNAL_CONTENT_URI から画像を取得
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE
        )
        
        val selection = "${MediaStore.Images.Media.DATE_TAKEN} IS NOT NULL"
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        
        runCatching {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                
                while (cursor.moveToNext()) {
                    runCatching {
                        val id = cursor.getLong(idColumn)
                        val displayName = cursor.getString(nameColumn)
                        val data = cursor.getString(dataColumn)
                        val dateTaken = cursor.getLong(dateTakenColumn)
                        val dateModified = cursor.getLong(dateModifiedColumn)
                        val width = cursor.getInt(widthColumn)
                        val height = cursor.getInt(heightColumn)
                        val size = cursor.getLong(sizeColumn)
                        
                        // 拡張子フィルター
                        val ext = displayName.substringAfterLast(".").lowercase()
                        if (normalizedExt.isNotEmpty() && !normalizedExt.contains(ext)) {
                            return@runCatching
                        }
                        
                        // ファイルチェック
                        val file = File(data)
                        if (!file.exists() || !file.isFile) {
                            return@runCatching
                        }
                        
                        val capturedAt = if (dateTaken > 0) {
                            Instant.fromEpochMilliseconds(dateTaken)
                        } else {
                            null
                        }
                        
                        val modifiedAt = if (dateModified > 0) {
                            Instant.fromEpochMilliseconds(dateModified * 1000)
                        } else {
                            null
                        }
                        
                        // ユーザーメタデータ（XMPサイドカー）
                        val userMeta = UserMetadataManager.getUserMetadata(file.absolutePath)
                        
                        val item = PhotoItem(
                            id = id.toString(),
                            displayName = displayName,
                            absolutePath = file.absolutePath,
                            capturedAt = capturedAt,
                            width = if (width > 0) width else null,
                            height = if (height > 0) height else null,
                            sizeBytes = size,
                            extension = ext,
                            metadata = emptyMap(), // TODO: EXIF抽出実装予定
                            thumbnail = null,
                            title = userMeta.title,
                            tags = userMeta.tags,
                            comment = userMeta.comment,
                            modifiedAt = modifiedAt
                        )
                        results += item
                    }
                }
            }
        }
        
        results
    }
}
