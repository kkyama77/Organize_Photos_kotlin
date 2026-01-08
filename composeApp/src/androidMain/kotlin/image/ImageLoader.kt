package com.organize.photos.image

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap {
    return try {
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (bmp != null) bmp.asImageBitmap() else ImageBitmap(1, 1)
    } catch (e: Exception) {
        ImageBitmap(1, 1)
    }
}
