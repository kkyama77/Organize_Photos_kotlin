package com.organize.photos.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent

/**
 * Desktop版: マウス右クリック検出
 */
@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.rightClickable(onRightClick: (Offset) -> Unit): Modifier = this.onPointerEvent(PointerEventType.Press) { event ->
    val position = event.changes.first().position
    // 左クリックでない場合（右クリックまたは中クリック）
    if (!event.buttons.isPrimaryPressed) {
        onRightClick(position)
    }
}
