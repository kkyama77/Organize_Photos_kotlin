package com.organize.photos.desktop

import java.io.File

object FolderPathStore {
    private val configDir = File(System.getProperty("user.home"), ".organize_photos")
    private val pathFile = File(configDir, "last_folder_path.txt")

    init {
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
    }

    fun getLastFolderPath(): String? {
        return if (pathFile.exists()) {
            pathFile.readText().trim().takeIf { it.isNotBlank() }
        } else {
            null
        }
    }

    fun saveFolderPath(path: String) {
        pathFile.writeText(path)
    }
}
