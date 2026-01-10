package com.organize.photos.logic

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File

@Serializable
data class UserMetadata(
    val title: String = "",
    val tags: List<String> = emptyList(),
    val comment: String = ""
)

@Serializable
data class MetadataStorage(
    val photos: Map<String, UserMetadata> = emptyMap()
)

/**
 * ユーザー定義メタデータ（タイトル、タグ、コメント）の管理
 * フォルダルートに .organize_photos_metadata.json を作成・管理
 */
object UserMetadataManager {
    private val json = Json { prettyPrint = true }
    private var metadataStorage: MetadataStorage = MetadataStorage()
    private var storageFile: File? = null
    
    /**
     * フォルダ選択時に呼び出し、メタデータファイルをロード
     */
    fun initialize(folderPath: String) {
        val folder = File(folderPath)
        storageFile = File(folder, ".organize_photos_metadata.json")
        
        // 既存のメタデータがあれば読み込む
        if (storageFile?.exists() == true) {
            try {
                val content = storageFile?.readText() ?: "{\"photos\":{}}"
                metadataStorage = json.decodeFromString<MetadataStorage>(content)
            } catch (e: Exception) {
                e.printStackTrace()
                metadataStorage = MetadataStorage()
            }
        } else {
            metadataStorage = MetadataStorage()
        }
    }
    
    /**
     * ファイルパスに対応するユーザーメタデータを取得
     */
    fun getUserMetadata(filePath: String): UserMetadata {
        return metadataStorage.photos[filePath] ?: UserMetadata()
    }
    
    /**
     * ユーザーメタデータを設定して保存
     */
    fun setUserMetadata(filePath: String, metadata: UserMetadata) {
        val updated = metadataStorage.photos.toMutableMap()
        updated[filePath] = metadata
        metadataStorage = metadataStorage.copy(photos = updated)
        save()
    }
    
    /**
     * メタデータをJSONファイルに保存
     */
    fun save() {
        try {
            val jsonString = json.encodeToString<MetadataStorage>(metadataStorage)
            storageFile?.writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * メモリとファイルをクリア
     */
    fun clear() {
        metadataStorage = MetadataStorage()
        storageFile = null
    }
}
