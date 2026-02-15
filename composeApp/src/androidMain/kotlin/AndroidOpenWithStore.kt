package com.organize.photos.android

import android.content.Context

/**
 * Android の SharedPreferences を使用して、画像の既定アプリを保持
 */
object AndroidOpenWithStore {
    private const val PREFS_NAME = "organize_photos_prefs"
    private const val KEY_IMAGE_APP_PACKAGE = "preferred_image_app_package"

    fun savePreferredImageApp(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_IMAGE_APP_PACKAGE, packageName).apply()
    }

    fun getPreferredImageApp(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_IMAGE_APP_PACKAGE, null)
    }

    fun clearPreferredImageApp(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_IMAGE_APP_PACKAGE).apply()
    }
}
