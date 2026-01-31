package com.organize.photos.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.organize.photos.logic.PhotoScanner
import com.organize.photos.logic.ThumbnailGenerator
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.application
import com.organize.photos.ui.App
import javax.swing.JFileChooser
import java.awt.Desktop
import java.io.File

private fun openFileWithDefaultApp(filePath: String) {
    try {
        val file = File(filePath)
        if (file.exists()) {
            if (Desktop.isDesktopSupported()) {
                val desktop = Desktop.getDesktop()
                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(file)
                } else {
                    // Fallback: try platform-specific commands
                    val os = System.getProperty("os.name").lowercase()
                    when {
                        os.contains("win") -> Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", "\"\"", filePath))
                        os.contains("mac") -> Runtime.getRuntime().exec(arrayOf("open", filePath))
                        else -> Runtime.getRuntime().exec(arrayOf("xdg-open", filePath))
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun isWindows(): Boolean = System.getProperty("os.name").lowercase().contains("win")

private fun pickDirectory(ownerWindow: java.awt.Window?): String? {
    // Windows 標準ダイアログを使用（Windows判定あり）
    if (isWindows()) {
        val lastPath = FolderPathStore.getLastFolderPath()
        val initialDir = if (lastPath != null && File(lastPath).exists()) lastPath else null
        val selectedPath = WindowsFolderPicker.selectFolder("フォルダを選択", initialDir, ownerWindow)
        if (selectedPath != null) {
            FolderPathStore.saveFolderPath(selectedPath)
        }
        return selectedPath
    }
    
    // Linux/macOS: JFileChooser を使用
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isMultiSelectionEnabled = true // Allow multiple folder selection
        // Set initial directory to last opened folder if available
        val lastPath = FolderPathStore.getLastFolderPath()
        if (lastPath != null && File(lastPath).exists()) {
            currentDirectory = File(lastPath)
        }
    }
    val result = chooser.showOpenDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) {
        val selected = chooser.selectedFiles
        if (selected.isNotEmpty()) {
            val firstPath = selected[0].absolutePath
            FolderPathStore.saveFolderPath(firstPath)
            // Return comma-separated paths for multiple folders
            selected.joinToString(",") { it.absolutePath }
        } else {
            val selectedPath = chooser.selectedFile?.absolutePath
            if (selectedPath != null) {
                FolderPathStore.saveFolderPath(selectedPath)
            }
            selectedPath
        }
    } else {
        null
    }
}

@Composable
private fun DesktopApp() {
    val scanner = remember { PhotoScanner() }
    val thumbs = remember { ThumbnailGenerator() }
    App(
        photoScanner = scanner, 
        openFolderPicker = { pickDirectory(null) }, 
        thumbnailGenerator = thumbs,
        openWithDefaultApp = ::openFileWithDefaultApp,
        enableControlsCollapse = true
    )
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Organize Photos",
        state = rememberWindowState(width = 1400.dp, height = 900.dp)
    ) {
        val ownerWindow = this.window
        val scanner = remember { PhotoScanner() }
        val thumbs = remember { ThumbnailGenerator() }
        App(
            photoScanner = scanner,
            openFolderPicker = { pickDirectory(ownerWindow) },
            thumbnailGenerator = thumbs,
            openWithDefaultApp = ::openFileWithDefaultApp,
            enableControlsCollapse = true
        )
    }
}
