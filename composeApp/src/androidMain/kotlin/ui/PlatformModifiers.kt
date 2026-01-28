package com.organize.photos.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

/**
 * Android版: 右クリックは存在しないため空実装
 */
actual fun Modifier.rightClickable(onRightClick: (Offset) -> Unit): Modifier = this
