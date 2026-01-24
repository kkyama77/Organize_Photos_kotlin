package com.organize.photos.android

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

/**
 * Android の SharedPreferences を使用してフォルダ URI を永続化
 */
object AndroidFolderStore {
    private const val PREFS_NAME = "organize_photos_prefs"
    private const val KEY_FOLDER_URI = "selected_folder_uri"
    
    fun saveFolderUri(context: Context, uri: Uri) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FOLDER_URI, uri.toString()).apply()
    }
    
    fun getSavedFolderUri(context: Context): Uri? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriString = prefs.getString(KEY_FOLDER_URI, null) ?: return null
        return runCatching { Uri.parse(uriString) }.getOrNull()
    }
    
    fun clearFolderUri(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_FOLDER_URI).apply()
    }
}
