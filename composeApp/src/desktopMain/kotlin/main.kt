package com.organize.photos.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.application
import com.organize.photos.logic.PhotoScanner
import com.organize.photos.ui.App
import javax.swing.JFileChooser
import com.organize.photos.desktop.DesktopThumbnailGenerator
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

private fun pickDirectory(): String? {
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
    val thumbs = remember { DesktopThumbnailGenerator() }
    App(
        photoScanner = scanner, 
        openFolderPicker = ::pickDirectory, 
        thumbnailGenerator = thumbs,
        openWithDefaultApp = ::openFileWithDefaultApp
    )
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Organize Photos",
        state = rememberWindowState(width = 1400.dp, height = 900.dp)
    ) {
        DesktopApp()
    }
}
