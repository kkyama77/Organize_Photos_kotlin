package com.organize.photos.image

import androidx.compose.ui.graphics.ImageBitmap

expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap
