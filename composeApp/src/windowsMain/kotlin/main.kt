package com.organize.photos.desktop

import com.sun.jna.platform.WindowUtils
import java.io.File

/**
 * Windows プラットフォーム専用の pickDirectory 実装
 */
actual fun pickDirectory(ownerWindow: java.awt.Window?): String? {
    val lastPath = FolderPathStore.getLastFolderPath()
    val initialDir = if (lastPath != null && File(lastPath).exists()) lastPath else null
    val ownerHwnd = ownerWindow?.let { WindowUtils.getHWND(it) }
    val selectedPath = WindowsFolderPicker.selectFolder("フォルダを選択", initialDir, ownerHwnd)
    if (selectedPath != null) {
        FolderPathStore.saveFolderPath(selectedPath)
    }
    return selectedPath
}
