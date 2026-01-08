package com.organize.photos.desktop

import com.organize.photos.logic.ThumbnailGenerator
import com.organize.photos.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import javax.imageio.stream.FileImageInputStream

class DesktopThumbnailGenerator : ThumbnailGenerator {
    override suspend fun generate(item: PhotoItem, maxSize: Int): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(item.absolutePath)
            if (!file.exists()) return@withContext null

            // Read image - try standard ImageIO.read first, fallback to explicit readers
            val src: BufferedImage = ImageIO.read(file) ?: run {
                // If standard read fails, try explicit reader (especially for TIFF)
                if (file.extension.lowercase() == "tiff" || file.extension.lowercase() == "tif") {
                    val readers = ImageIO.getImageReadersByFormatName("TIFF")
                    if (readers.hasNext()) {
                        val reader = readers.next()
                        var result: BufferedImage? = null
                        FileImageInputStream(file).use { stream ->
                            reader.input = stream
                            result = reader.read(0)
                            reader.dispose()
                        }
                        result ?: return@withContext null
                    } else {
                        return@withContext null
                    }
                } else {
                    return@withContext null
                }
            }

            val (w, h) = src.width to src.height
            if (w <= 0 || h <= 0) return@withContext null

            val scale = if (w > h) maxSize.toDouble() / w else maxSize.toDouble() / h
            val dstW = maxOf(1, (w * scale).toInt())
            val dstH = maxOf(1, (h * scale).toInt())

            val scaled = BufferedImage(dstW, dstH, BufferedImage.TYPE_INT_RGB)
            val g = scaled.createGraphics()
            g.drawImage(src.getScaledInstance(dstW, dstH, Image.SCALE_SMOOTH), 0, 0, null)
            g.dispose()

            val out = ByteArrayOutputStream()
            ImageIO.write(scaled, "jpg", out)
            out.toByteArray()
        }.getOrNull()
    }
}
