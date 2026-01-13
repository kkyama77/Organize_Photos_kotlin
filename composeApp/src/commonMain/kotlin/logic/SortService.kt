package com.organize.photos.logic

import com.organize.photos.model.PhotoItem
import kotlin.math.absoluteValue

enum class SortOrder(val displayName: String) {
    // ファイル名順
    FILE_NAME_ASC("ファイル名（昇順）"),
    FILE_NAME_DESC("ファイル名（降順）"),
    
    // 撮影日順
    DATE_TAKEN_NEW("撮影日（新しい順）"),
    DATE_TAKEN_OLD("撮影日（古い順）"),
    
    // 作成日時順
    DATE_MODIFIED_NEW("作成日時（新しい順）"),
    DATE_MODIFIED_OLD("作成日時（古い順）"),
    
    // カメラ機種順
    CAMERA_MODEL("カメラ機種"),
    
    // 焦点距離順
    FOCAL_LENGTH_LONG("焦点距離（長い順）"),
    FOCAL_LENGTH_SHORT("焦点距離（短い順）"),
    
    // F値順
    APERTURE_LARGE("F値（大きい順）"),
    APERTURE_SMALL("F値（小さい順）"),
    
    // ISO感度順
    ISO_HIGH("ISO感度（高い順）"),
    ISO_LOW("ISO感度（低い順）"),
    
    // ファイル種類順
    FILE_TYPE("ファイル種類"),
    
    // 向き順
    ORIENTATION_PORTRAIT("縦向き優先"),
    ORIENTATION_LANDSCAPE("横向き優先"),
}

object SortService {
    
    fun sort(photos: List<PhotoItem>, order: SortOrder): List<PhotoItem> {
        return when (order) {
            SortOrder.FILE_NAME_ASC -> photos.sortedBy { it.displayName }
            SortOrder.FILE_NAME_DESC -> photos.sortedByDescending { it.displayName }
            
            SortOrder.DATE_TAKEN_NEW -> photos.sortedByDescending { it.capturedAt }
            SortOrder.DATE_TAKEN_OLD -> photos.sortedBy { it.capturedAt }
            
            SortOrder.DATE_MODIFIED_NEW -> photos.sortedByDescending { it.modifiedAt }
            SortOrder.DATE_MODIFIED_OLD -> photos.sortedBy { it.modifiedAt }
            
            SortOrder.CAMERA_MODEL -> {
                photos.sortedWith(compareBy(
                    { getCameraModel(it).ifEmpty { "Z_Unknown" } },
                    { it.displayName }
                ))
            }
            
            SortOrder.FOCAL_LENGTH_LONG -> {
                photos.sortedByDescending { extractFocalLength(it.metadata["Exif SubIFD:Focal Length"]) }
            }
            SortOrder.FOCAL_LENGTH_SHORT -> {
                photos.sortedBy { extractFocalLength(it.metadata["Exif SubIFD:Focal Length"]) }
            }
            
            SortOrder.APERTURE_LARGE -> {
                photos.sortedByDescending { extractAperture(it.metadata["Exif SubIFD:F-Number"]) }
            }
            SortOrder.APERTURE_SMALL -> {
                photos.sortedBy { extractAperture(it.metadata["Exif SubIFD:F-Number"]) }
            }
            
            SortOrder.ISO_HIGH -> {
                photos.sortedByDescending { extractISO(it.metadata["Exif SubIFD:ISO Speed Ratings"]) }
            }
            SortOrder.ISO_LOW -> {
                photos.sortedBy { extractISO(it.metadata["Exif SubIFD:ISO Speed Ratings"]) }
            }
            
            SortOrder.FILE_TYPE -> {
                photos.sortedWith(compareBy(
                    { it.extension.lowercase() },
                    { it.displayName }
                ))
            }
            
            SortOrder.ORIENTATION_PORTRAIT -> {
                photos.sortedWith(
                    compareBy<PhotoItem> { 
                        val isPortrait = isPortraitOrientation(it.width, it.height)
                        !isPortrait  // 縦向きを優先（true が先）
                    }.thenBy { it.displayName }
                )
            }
            SortOrder.ORIENTATION_LANDSCAPE -> {
                photos.sortedWith(
                    compareBy<PhotoItem> { 
                        val isPortrait = isPortraitOrientation(it.width, it.height)
                        isPortrait  // 横向きを優先（false が先）
                    }.thenBy { it.displayName }
                )
            }
        }
    }
    
    /**
     * カメラ機種を抽出（Make + Model）
     */
    private fun getCameraModel(photo: PhotoItem): String {
        val make = photo.metadata["Exif IFD0:Make"]?.trim() ?: ""
        val model = photo.metadata["Exif IFD0:Model"]?.trim() ?: ""
        return when {
            make.isNotEmpty() && model.isNotEmpty() -> "$make $model"
            model.isNotEmpty() -> model
            make.isNotEmpty() -> make
            else -> ""
        }
    }
    
    /**
     * 焦点距離を数値で抽出
     * "50.0 mm" → 50.0
     */
    private fun extractFocalLength(value: String?): Float {
        if (value.isNullOrBlank()) return 0f
        val numStr = value.replace(Regex("[^0-9.]"), "")
        return numStr.toFloatOrNull() ?: 0f
    }
    
    /**
     * F値を数値で抽出
     * "f/2.8" または "2.8" → 2.8
     */
    private fun extractAperture(value: String?): Float {
        if (value.isNullOrBlank()) return Float.MAX_VALUE
        val numStr = value.replace(Regex("[^0-9.]"), "")
        return numStr.toFloatOrNull() ?: Float.MAX_VALUE
    }
    
    /**
     * ISO感度を数値で抽出
     * "400" → 400
     */
    private fun extractISO(value: String?): Int {
        if (value.isNullOrBlank()) return 0
        val numStr = value.replace(Regex("[^0-9]"), "")
        return numStr.toIntOrNull() ?: 0
    }
    
    /**
     * 縦向きか判定
     * 高さ >= 幅 なら縦向き
     */
    private fun isPortraitOrientation(width: Int?, height: Int?): Boolean {
        if (width == null || height == null) return false
        return height >= width
    }
}
