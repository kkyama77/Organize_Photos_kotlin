package com.organize.photos.logic

import com.organize.photos.model.PhotoItem

/**
 * 詳細検索用フィールド定義
 * EXIF メタデータをカテゴリ別に整理
 */
enum class SearchFieldCategory {
    CAMERA,      // カメラ情報（メーカー、モデル）
    LENS,        // レンズ情報
    EXPOSURE,    // 撮影設定（ISO、絞り、シャッタースピード）
    FOCUS,       // フォーカス関連
    GPS,         // GPS情報
    DATE,        // 撮影日時
    IMAGE_INFO,  // 画像情報（サイズ、拡張子）
    SOFTWARE,    // ソフトウェア情報
}

data class SearchField(
    val key: String,                    // メタデータキー（例："CameraMake"）
    val displayName: String,            // 表示名（例："カメラメーカー"）
    val category: SearchFieldCategory,  // カテゴリ
    val keywords: List<String> = emptyList()  // 日英同義語（例：["camera", "カメラ", "make"]）
)

data class FieldFilter(
    val field: SearchField,
    val selectedValues: Set<String> = emptySet(),  // 選択された値
    val searchMode: SearchService.SearchMode = SearchService.SearchMode.OR  // フィールド内のAND/OR
)

/**
 * 詳細検索の設定
 */
data class AdvancedSearchConfig(
    val fieldFilters: Map<String, FieldFilter> = emptyMap(),  // フィールドキー -> フィルタ設定
    val matchMode: SearchService.SearchMode = SearchService.SearchMode.AND  // フィールド間のAND/OR
)

/**
 * メタデータから検索フィールドを抽出・集計
 */
object FieldAnalyzer {
    
    /**
     * 定義済みの検索フィールド（日英同義語対応）
     */
    private val DEFINED_FIELDS = mapOf(
        // カメラ情報
        "CameraMake" to SearchField(
            key = "CameraMake",
            displayName = "カメラメーカー",
            category = SearchFieldCategory.CAMERA,
            keywords = listOf("make", "manufacturer", "メーカー", "製造元")
        ),
        "CameraModel" to SearchField(
            key = "CameraModel",
            displayName = "カメラモデル",
            category = SearchFieldCategory.CAMERA,
            keywords = listOf("model", "camera", "機種", "カメラ")
        ),
        
        // レンズ情報
        "LensModel" to SearchField(
            key = "LensModel",
            displayName = "レンズモデル",
            category = SearchFieldCategory.LENS,
            keywords = listOf("lens", "レンズ")
        ),
        
        // 撮影設定
        "ISO" to SearchField(
            key = "ISO",
            displayName = "ISO感度",
            category = SearchFieldCategory.EXPOSURE,
            keywords = listOf("iso", "sensitivity", "感度")
        ),
        "FNumber" to SearchField(
            key = "FNumber",
            displayName = "絞り値 (F値)",
            category = SearchFieldCategory.EXPOSURE,
            keywords = listOf("aperture", "f-number", "fnumber", "絞り")
        ),
        "ExposureTime" to SearchField(
            key = "ExposureTime",
            displayName = "シャッタースピード",
            category = SearchFieldCategory.EXPOSURE,
            keywords = listOf("exposure", "shutter", "速度", "露出")
        ),
        "FocalLength" to SearchField(
            key = "FocalLength",
            displayName = "焦点距離",
            category = SearchFieldCategory.EXPOSURE,
            keywords = listOf("focal", "length", "焦点", "距離")
        ),
        
        // GPS
        "GPSLatitude" to SearchField(
            key = "GPSLatitude",
            displayName = "GPS 緯度",
            category = SearchFieldCategory.GPS,
            keywords = listOf("gps", "latitude", "緯度", "位置")
        ),
        "GPSLongitude" to SearchField(
            key = "GPSLongitude",
            displayName = "GPS 経度",
            category = SearchFieldCategory.GPS,
            keywords = listOf("gps", "longitude", "経度", "位置")
        ),
        
        // ソフトウェア
        "Software" to SearchField(
            key = "Software",
            displayName = "ソフトウェア",
            category = SearchFieldCategory.SOFTWARE,
            keywords = listOf("software", "app", "ソフト", "アプリ")
        ),
    )
    
    /**
     * 複数の写真から全フィールドの一意な値を抽出
     */
    fun extractAvailableValues(photos: List<PhotoItem>): Map<String, Set<String>> {
        val values = mutableMapOf<String, MutableSet<String>>()
        
        for (photo in photos) {
            DEFINED_FIELDS.keys.forEach { fieldKey ->
                photo.metadata[fieldKey]?.let { value ->
                    values.getOrPut(fieldKey) { mutableSetOf() }.add(value)
                }
            }
        }
        
        return values
    }
    
    /**
     * 定義済みフィールド一覧を取得
     */
    fun getDefinedFields(): List<SearchField> = DEFINED_FIELDS.values.toList()
    
    /**
     * フィールド定義を取得
     */
    fun getField(key: String): SearchField? = DEFINED_FIELDS[key]
}

/**
 * 詳細検索エンジン
 */
class AdvancedSearchEngine {
    
    fun filter(
        items: List<PhotoItem>,
        config: AdvancedSearchConfig
    ): List<PhotoItem> {
        if (config.fieldFilters.isEmpty()) {
            return items
        }
        
        return items.filter { photo ->
            // フィールド間のマッチング（AND/OR）
            val fieldMatches = config.fieldFilters.values.map { filter ->
                matchesField(photo, filter)
            }
            
            when (config.matchMode) {
                SearchService.SearchMode.OR -> fieldMatches.any { it }
                SearchService.SearchMode.AND -> fieldMatches.all { it }
            }
        }
    }
    
    private fun matchesField(photo: PhotoItem, filter: FieldFilter): Boolean {
        if (filter.selectedValues.isEmpty()) {
            return true
        }
        
        val photoValue = photo.metadata[filter.field.key] ?: return false
        
        // フィールド内のマッチング（AND/OR）
        val matches = filter.selectedValues.map { selectedValue ->
            photoValue.contains(selectedValue, ignoreCase = true)
        }
        
        return when (filter.searchMode) {
            SearchService.SearchMode.OR -> matches.any { it }
            SearchService.SearchMode.AND -> matches.all { it }
        }
    }
}
