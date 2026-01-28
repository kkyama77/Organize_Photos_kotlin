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
import androidx.compose.runtime.*
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
    // 現在選択されているフォルダ URI を State で管理
    var currentFolderUri by remember { mutableStateOf<Uri?>(AndroidFolderStore.getSavedFolderUri(context)) }
    
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                // パーミッション保持
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                // URI を保存
                AndroidFolderStore.saveFolderUri(context, uri)
                // State を更新して再描画
                currentFolderUri = uri
            }
        }
    )
    
    val openFolderPicker: () -> String? = {
        folderPickerLauncher.launch(null)
        // Android は非同期なので、選択結果は onResult で処理される
        // ここでは選択済みの URI を返す（再スキャン用）
        currentFolderUri?.toString()
    }
    
    if (currentFolderUri == null) {
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
                    onClick = { folderPickerLauncher.launch(null) },
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
            openFolderPicker = openFolderPicker,
            thumbnailGenerator = thumbnailGenerator
        )
    }
}
