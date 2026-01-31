package com.organize.photos.logic

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.GpsDirectory
import com.drew.metadata.jpeg.JpegDirectory
import com.organize.photos.logic.PhotoScanner
import com.organize.photos.logic.ScanFilters
import com.organize.photos.logic.UserMetadataManager
import com.organize.photos.model.PhotoItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.io.path.isDirectory

actual class PhotoScanner(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    actual suspend fun scan(root: String, filters: ScanFilters): List<PhotoItem> = withContext(dispatcher) {
        val results = mutableListOf<PhotoItem>()
        
        // Support comma-separated multiple folder paths
        val paths = root.split(",").map { it.trim() }.filter { it.isNotBlank() }
        
        for (pathStr in paths) {
            val rootPath = Paths.get(pathStr)
            if (!Files.exists(rootPath) || !rootPath.isDirectory()) continue
            
            val normalizedExt = filters.extensions.map { it.lowercase() }.toSet()
            Files.walk(rootPath).use { stream ->
                stream.filter { Files.isRegularFile(it) }
                    .forEach { path ->
                        runCatching {
                            val file = path.toFile()
                            if (!matchesExtension(file, normalizedExt)) return@runCatching
                            val item = toPhotoItem(file)
                            if (!matchesDate(item.capturedAt, filters.dateRange)) return@runCatching
                            results += item
                        }
                    }
            }
        }
        results
    }

    actual suspend fun renamePhoto(photo: PhotoItem, newFileName: String): Boolean = withContext(dispatcher) {
        return try {
            val oldFile = File(photo.absolutePath)
            if (!oldFile.exists()) return@withContext false
            
            val newFile = File(oldFile.parent, newFileName)
            oldFile.renameTo(newFile)
        } catch (e: Exception) {
            false
        }
    }

    private fun matchesExtension(file: File, allowed: Set<String>): Boolean {
        if (allowed.isEmpty()) return true
        val ext = file.extension.lowercase()
        return allowed.contains(ext)
    }

    private fun matchesDate(capturedAt: Instant?, range: ClosedRange<Instant>?): Boolean {
        range ?: return true
        capturedAt ?: return true
        return capturedAt >= range.start && capturedAt <= range.endInclusive
    }

    private fun toPhotoItem(file: File): PhotoItem {
        var width: Int? = null
        var height: Int? = null
        var date: Instant? = null
        var modifiedDate: Instant? = null
        val meta = mutableMapOf<String, String>()

        // ファイルの更新日時を取得
        runCatching {
            val lastModified = Files.getLastModifiedTime(file.toPath()).toInstant().toKotlinInstant()
            modifiedDate = lastModified
        }

        // Try metadata-extractor first
        runCatching {
            val metadata = ImageMetadataReader.readMetadata(file)
            val exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
            val ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory::class.java)
            val jpeg = metadata.getFirstDirectoryOfType(JpegDirectory::class.java)
            val gps = metadata.getFirstDirectoryOfType(GpsDirectory::class.java)

            date = exif?.dateOriginal?.toInstant()?.toKotlinInstant()
                ?: exif?.dateDigitized?.toInstant()?.toKotlinInstant()

            // Collect common EXIF fields
            ifd0?.getString(ExifIFD0Directory.TAG_MAKE)?.let { meta["CameraMake"] = it }
            ifd0?.getString(ExifIFD0Directory.TAG_MODEL)?.let { meta["CameraModel"] = it }
            exif?.getString(ExifSubIFDDirectory.TAG_LENS_MODEL)?.let { meta["LensModel"] = it }
            exif?.getInteger(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)?.let { meta["ISO"] = it.toString() }
            exif?.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)?.let { meta["ExposureTime"] = it }
            exif?.getString(ExifSubIFDDirectory.TAG_FNUMBER)?.let { meta["FNumber"] = it }
            exif?.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)?.let { meta["FocalLength"] = it }
            ifd0?.getString(ExifIFD0Directory.TAG_ORIENTATION)?.let { meta["Orientation"] = it }
            ifd0?.getString(ExifIFD0Directory.TAG_SOFTWARE)?.let { meta["Software"] = it }
            exif?.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)?.let { meta["DateOriginalRaw"] = it }
            gps?.geoLocation?.let { geo ->
                meta["GPSLatitude"] = geo.latitude.toString()
                meta["GPSLongitude"] = geo.longitude.toString()
            }

            width = exif?.getInteger(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH)
                ?: jpeg?.getInteger(JpegDirectory.TAG_IMAGE_WIDTH)
                ?: ifd0?.getInteger(ExifIFD0Directory.TAG_IMAGE_WIDTH)

            height = exif?.getInteger(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT)
                ?: jpeg?.getInteger(JpegDirectory.TAG_IMAGE_HEIGHT)
                ?: ifd0?.getInteger(ExifIFD0Directory.TAG_IMAGE_HEIGHT)

            // 追加: すべてのディレクトリ・タグを走査して格納（人間可読の説明）
            metadata.directories.forEach { dir ->
                dir.tags.forEach { tag ->
                    val key = "${dir.name}:${tag.tagName}"
                    val value = tag.description ?: dir.getDescription(tag.tagType) ?: ""
                    if (value.isNotBlank()) meta[key] = value
                }
            }
        }

        // Fallback to ImageIO if dimensions not found (especially for TIFF)
        if (width == null || height == null) {
            runCatching {
                val img = ImageIO.read(file)
                if (img != null) {
                    width = img.width
                    height = img.height
                }
            }
        }

        val userMeta = UserMetadataManager.getUserMetadata(file.absolutePath)

        return PhotoItem(
            id = file.absolutePath,
            displayName = file.name,
            absolutePath = file.absolutePath,
            capturedAt = date,
            width = width,
            height = height,
            sizeBytes = file.length(),
            extension = file.extension.ifBlank { "" }.lowercase(),
            metadata = meta,
            thumbnail = null,
            // ✨ ユーザーメタデータを統合（XMP サイドカー読み込み）
            title = userMeta.title,
            tags = userMeta.tags,
            comment = userMeta.comment,
            // ✨ ファイル更新日時
            modifiedAt = modifiedDate,
        )
    }
}
