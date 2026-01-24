package com.organize.photos.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.organize.photos.logic.PhotoScanner
import com.organize.photos.logic.ThumbnailGenerator
import com.organize.photos.ui.App

/**
 * Android用フォルダピッカー + App フレーム
 */
@Composable
fun AndroidAppFrame(
    context: Context,
    photoScanner: PhotoScanner,
    thumbnailGenerator: ThumbnailGenerator
) {
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                // パーミッション保持
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                // URI を保存して App に渡す
                AndroidFolderStore.saveFolderUri(context, uri)
            }
        }
    )
    
    fun pickFolder() {
        folderPickerLauncher.launch(null)
    }
    
    // 保存済みフォルダを復元
    val savedUri = remember { AndroidFolderStore.getSavedFolderUri(context) }
    
    if (savedUri == null) {
        // フォルダが未選択の場合はピッカー表示
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text("写真フォルダを選択してください")
                Button(
                    onClick = { pickFolder() },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("フォルダを選択")
                }
            }
        }
    } else {
        // フォルダが選択済みの場合は App を表示
        App(
            photoScanner = photoScanner,
            openFolderPicker = { 
                pickFolder()
                null // Android は非同期なので URI を返さない
            },
            thumbnailGenerator = thumbnailGenerator
        )
    }
}
