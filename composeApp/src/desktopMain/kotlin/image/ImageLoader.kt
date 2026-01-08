package com.organize.photos.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image as SkiaImage

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap {
    return try {
        val skiaImage = SkiaImage.makeFromEncoded(bytes)
        skiaImage.toComposeImageBitmap()
    } catch (e: Exception) {
        // Return a 1x1 placeholder if decoding fails
        ImageBitmap(1, 1)
    }
}
