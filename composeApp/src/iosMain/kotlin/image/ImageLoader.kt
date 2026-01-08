package com.organize.photos.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image as SkiaImage

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap {
    return try {
        val skiaImage = SkiaImage.makeFromEncoded(bytes)
        skiaImage.toComposeImageBitmap()
    } catch (e: Exception) {
        ImageBitmap(1, 1)
    }
}
