package com.organize.photos.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size
import com.organize.photos.model.PhotoItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Android 用 ThumbnailGenerator 実装
 * Coil を使用したサムネイル生成とキャッシュ
 */
actual class ThumbnailGenerator(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val imageLoader = ImageLoader.Builder(context)
        .crossfade(true)
        .build()
    
    actual suspend fun generate(item: PhotoItem, maxSize: Int): ByteArray? = withContext(dispatcher) {
        runCatching {
            val file = File(item.absolutePath)
            if (!file.exists()) return@withContext null
            
            // Coil でサムネイル読み込み
            val request = ImageRequest.Builder(context)
                .data(file)
                .size(Size(maxSize, maxSize))
                .allowHardware(false) // Bitmap取得のため
                .build()
            
            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                    ?: return@withContext null
                
                // Bitmap → ByteArray (JPEG)
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.toByteArray()
            } else {
                null
            }
        }.getOrNull()
    }
}
