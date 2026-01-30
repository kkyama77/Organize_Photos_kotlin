package com.organize.photos.android

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
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
    // フォルダ選択トリガー用の State
    var folderSelectionTrigger by remember { mutableStateOf(0) }
    
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
                // トリガーを更新してスキャンを開始
                folderSelectionTrigger++
            }
        }
    )
    
    val openFolderPicker: () -> String? = {
        folderPickerLauncher.launch(null)
        // Android は非同期なので null を返す（選択結果は onResult で処理）
        null
    }
    
    // フォルダ選択時に自動スキャンをトリガー
    LaunchedEffect(folderSelectionTrigger) {
        if (folderSelectionTrigger > 0 && currentFolderUri != null) {
            // フォルダが選択されたことを App に通知（initialItems で渡すため再コンポーズ）
        }
    }
    
    // 画像を外部アプリで開く
    val openWithDefaultApp: (String) -> Unit = { path ->
        val uri = resolveImageUri(context, path)
        if (uri != null) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
        }
    }

    // 常に App を表示
    App(
        photoScanner = photoScanner,
        openFolderPicker = openFolderPicker,
        thumbnailGenerator = thumbnailGenerator,
        // 初期パスとして URI を渡す（Android では使用しないが、デスクトップ版との互換性のため）
        initialItems = emptyList(),
        openWithDefaultApp = openWithDefaultApp,
        enableControlsCollapse = true
    )
}

private fun resolveImageUri(context: Context, path: String): Uri? {
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val selection = "${MediaStore.Images.Media.DATA} = ?"
    val selectionArgs = arrayOf(path)
    val resolver = context.contentResolver

    return runCatching {
        resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            } else {
                null
            }
        }
    }.getOrNull()
}
