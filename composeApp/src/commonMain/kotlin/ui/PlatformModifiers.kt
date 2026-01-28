package com.organize.photos.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

/**
 * 右クリック検出用のModifier拡張関数（プラットフォーム固有実装）
 * Desktop: マウス右クリック
 * Android: 実装なし（長押しのみ）
 */
expect fun Modifier.rightClickable(onRightClick: (Offset) -> Unit): Modifier
