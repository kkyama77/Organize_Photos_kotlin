package com.organize.photos.android

import android.content.Context
import com.organize.photos.logic.PhotoScanner
import com.organize.photos.logic.ThumbnailGenerator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Android 用 ThumbnailGenerator 実装
 * 画像の EXIF 情報から回転を考慮したサムネイル生成
 */
actual class ThumbnailGenerator(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    // TODO: 実装予定 - Compose Image Loader または Coil を使用
}
